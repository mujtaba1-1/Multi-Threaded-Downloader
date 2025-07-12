import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class GUI extends JFrame implements DownloadProgressListener {

    private final JTextField urlField;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;

    private final JButton downloadButton;
    private final JButton pauseButton;
    private final JButton cancelButton;

    private final JPanel buttonPanel;

    private boolean isDownloading = false;
    private boolean isPaused = false;

    private final DownloadController dc;

    private final AtomicInteger totalBytesDownloaded = new AtomicInteger(0);
    private String fileName = "";

    public GUI() {
        super("Multi-Threaded File Downloader");

        dc = new DownloadController();

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("Multi-Threaded File Downloader", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                "Download Details", TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("File URL:"), gbc);

        urlField = new JTextField(30);
        gbc.gridx = 1;
        gbc.gridy = 0;
        formPanel.add(urlField, gbc);

        downloadButton = new JButton("Download");
        downloadButton.addActionListener(e -> toggleDownload());
        
        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> pauseDownload());

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancelDownload(urlField.getText().trim()));

        buttonPanel = new JPanel();
        buttonPanel.add(downloadButton);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(buttonPanel, gbc);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(progressBar, gbc);

        statusLabel = new JLabel("Status: Idle");
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        formPanel.add(statusLabel, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        add(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 300);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void toggleDownload() {
        String url = urlField.getText().trim();
        if (!isDownloading) {
            if (url.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a URL!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            startDownload(url);

        } else {
            cancelDownload(url);
        }
    }

    private void startDownload(String url) {
        isDownloading = true;
        isPaused = false;

        try {
            fileName = dc.initialiseFile(url);

            statusLabel.setText("Status: Downloading " + fileName);
            progressBar.setIndeterminate(true);

            buttonPanel.removeAll();
            buttonPanel.add(pauseButton);
            buttonPanel.add(cancelButton);
            buttonPanel.revalidate();
            buttonPanel.repaint();

        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, e, "Error", JOptionPane.ERROR_MESSAGE);
                cancelDownload(url);
            });
        }

        new Thread(() -> {
            try {
                totalBytesDownloaded.set(0);
                long start = System.currentTimeMillis();
                dc.downloadFile(url, this);
                long end = System.currentTimeMillis();

                System.out.println(start);
                System.out.println(end);

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Status: Completed | Time Elapsed: " + (end - start) / 1000 + " s");
                    resetVariables();
                });
            } 
            catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, e, "Error", JOptionPane.ERROR_MESSAGE);
                    cancelDownload(url);
                });
            }
        }).start();        
    }

    private void cancelDownload(String url) {
        isPaused = true;
        pauseDownload();
        dc.cancelDownload(url); 

        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Status: Cancelled");
            resetVariables();
        });
    }

    private void resetVariables() {
        pauseButton.setText("Pause");

        progressBar.setIndeterminate(false);
        progressBar.setValue(0);

        buttonPanel.removeAll();
        buttonPanel.add(downloadButton);
        buttonPanel.revalidate();
        buttonPanel.repaint();

        isDownloading = false;
        isPaused = false;
        fileName = "";
    }

    @Override
    public void updateProgress(int bytes, int contentLength) {
        int downloaded = totalBytesDownloaded.addAndGet(bytes);
        int progress = (int) Math.ceil((downloaded / (double) contentLength) * 100); 
        
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(progress);
        });

    }

    private void pauseDownload() {
        if (!isPaused) {
            System.out.println("Paused");
            pauseButton.setText("Resume");
            statusLabel.setText("Status: Paused");
            dc.pauseDownload();
        } else {
            System.out.println("Downlaoding again");
            pauseButton.setText("Pause");
            statusLabel.setText("Status: Downloading " + fileName);
            dc.resumeDownload();
        }

        progressBar.setIndeterminate(isPaused);
        isPaused = !isPaused;
    }
}
