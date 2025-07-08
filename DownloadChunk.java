import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

public class DownloadChunk implements Runnable {

    private final String filePath;
    private final String fileURL;
    private final int startByte;
    private final int endByte;
    private final CountDownLatch latch;
    private final DownloadProgressListener listener;
    private final int contentLength;
    private final DownloadController dc;

    public DownloadChunk(String filePath,
                         String fileURL,
                         int startByte,
                         int endByte,
                         CountDownLatch latch,
                         DownloadProgressListener listener,
                         int contentLength,
                         DownloadController dc) {
        this.filePath = filePath;
        this.fileURL = fileURL;
        this.startByte = startByte;
        this.endByte = endByte;
        this.latch = latch;
        this.listener = listener;
        this.contentLength = contentLength;
        this.dc = dc;
    }

    @Override
    public void run() {
        HttpURLConnection conn = null;

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            if (dc.isCancelled()) {
                return;
            }

            URL url = URI.create(fileURL).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);
            conn.connect();

            try (BufferedInputStream bis = new BufferedInputStream(conn.getInputStream())) {
                raf.seek(startByte);
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = bis.read(buffer)) != -1) {
                    if (dc.isCancelled()) {
                        return;
                    }
                    raf.write(buffer, 0, bytesRead);
                    listener.updateProgress(bytesRead, contentLength);
                }
            }
        } catch (IOException e) {
            if (!dc.isCancelled()) {
                System.err.println("Error in chunk: " + startByte + "-" + endByte);
                e.printStackTrace();
            } else {
                System.out.println("IOException in cancelled chunk: " + startByte + "-" + endByte);
            }
        } finally {
            latch.countDown();
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
