
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;

public class DownloadController {

    private int totalThreads = 1;
    private ExecutorService executor;
    private CountDownLatch latch;
    private HttpURLConnection conn;

    private String fileName;
    private String filePath = "downloads/";
    private int contentLength;
    private RandomAccessFile raf;

    private volatile boolean isCancelled = false;
    private volatile boolean isPaused = false;
    private final Object pauseLock = new Object();

    public DownloadController() {}

    public String initialiseFile(String fileURL) throws IOException {
        if (fileURL != null && (fileURL.startsWith("http://") || fileURL.startsWith("https://"))) {
            try {
                isCancelled = false;

                URL url = URI.create(fileURL).toURL();
                if (fileURL.startsWith("https://")) {
                    conn = (HttpsURLConnection) url.openConnection();
                } else {
                    conn = (HttpURLConnection) url.openConnection();
                }

                readHttpData(conn, fileURL);

                return fileName;

            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException(e.getMessage());
            }
        } else {
            throw new IOException("Invalid URL: Must start with http:// or https://");
        }  
    } 

    public void downloadFile(String fileURL, DownloadProgressListener listener) throws IOException {
        try {
            executor = Executors.newFixedThreadPool(totalThreads);

            raf = new RandomAccessFile(filePath, "rw");
            raf.setLength(contentLength);
            raf.close();

            int chunkSize = contentLength / totalThreads;
            latch = new CountDownLatch(totalThreads);

            for (int i = 0; i < totalThreads; i++) {
                System.out.println("Thread: " + (i + 1));
                int startByte = i * chunkSize;
                int endByte = (i == totalThreads - 1) ? contentLength - 1 : (startByte + chunkSize - 1);
                executor.execute(new DownloadChunk(filePath, fileURL, startByte, endByte, latch, listener, contentLength, this));
            }

            latch.await();
            executor.shutdown();

            if (!isCancelled) {
                filePath = "downloads/";
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (executor != null) executor.shutdownNow();
            throw new IOException(e.getMessage());
        }
    }    

    private void readHttpData(HttpURLConnection conn, String fileURL) throws IOException {
        int rc = conn.getResponseCode();

        if (rc != HttpURLConnection.HTTP_OK) {
            throw new IOException("Server returned HTTP response code: " + rc + " for URL: " + fileURL);
        }

        contentLength = conn.getContentLength();

        if (contentLength < 0) {
            System.out.println("Failed to get file size");
        }

        fileName = getFileNameFromURL(conn, fileURL);
        filePath += handleDuplicateFiles(0);
        System.out.println(filePath);

        String acceptRanges = conn.getHeaderField("Accept-Ranges");

        if (acceptRanges != null && acceptRanges.toLowerCase().contains("bytes") && contentLength > 0) {
            totalThreads = 8;
        } 
    }

    private String handleDuplicateFiles(int count) {
        String newName = (count == 0) ? fileName : "(" + count + ") " + fileName;
        File file = new File(filePath + newName);

        if (file.exists()) { 
            newName = handleDuplicateFiles(count + 1);
        } 
        return newName;
    }

    private String getFileNameFromURL(HttpURLConnection conn, String fileURL) {
        String disposition = conn.getHeaderField("Content-Disposition");
        String name;

        if (disposition != null && disposition.contains("filename=")) {
            name = disposition.substring(disposition.indexOf("filename=") + 9).replace("\"", "").trim();
        } else {
            name = fileURL.substring(fileURL.lastIndexOf('/') + 1);
            String contentType = conn.getContentType();
            name += guessExtension(contentType);
        }

        return name;
    }

    private String guessExtension(String contentType) {
        if (contentType == null) return "";

        switch (contentType.toLowerCase()) {
            // Archives
            case "application/zip":
            case "application/x-zip-compressed":
            case "application/x-7z-compressed":
            case "application/x-rar-compressed":
            case "application/x-tar":
                return ".zip";
            case "application/x-gzip":
                return ".gz";
            case "application/java-archive":
            case "application/x-java-archive":
                return ".jar";

            // Java source code
            case "text/x-java-source":
            case "text/x-java":
            case "text/java":
                return ".java";

            // Documents
            case "application/pdf":
                return ".pdf";
            case "application/msword":
                return ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return ".docx";
            case "application/vnd.ms-excel":
                return ".xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                return ".xlsx";
            case "application/vnd.ms-powerpoint":
                return ".ppt";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
                return ".pptx";

            // Images
            case "image/jpeg":
            case "image/jpg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "image/bmp":
                return ".bmp";
            case "image/webp":
                return ".webp";
            case "image/svg+xml":
                return ".svg";

            // Audio
            case "audio/mpeg":
                return ".mp3";
            case "audio/wav":
                return ".wav";
            case "audio/ogg":
                return ".ogg";

            // Video
            case "video/mp4":
                return ".mp4";
            case "video/x-msvideo":
                return ".avi";
            case "video/x-matroska":
                return ".mkv";

            // Text files
            case "text/plain":
                return ".txt";
            case "text/html":
                return ".html";
            case "text/css":
                return ".css";
            case "application/javascript":
            case "text/javascript":
                return ".js";
            case "application/json":
                return ".json";
            case "application/xml":
            case "text/xml":
                return ".xml";

            // Others
            case "application/octet-stream":
                return ".bin";
            case "application/vnd.android.package-archive":
                return ".apk";
            case "application/x-sh":
                return ".sh";

            default:
                return "";
        }
    }

    public void pauseDownload() {
        if (!isPaused) {
            isPaused = true;
        }
    }

    public void resumeDownload() {
        if (isPaused) {
            synchronized (pauseLock) {
                isPaused = false;
                pauseLock.notifyAll();
            }
        }
    }

    public void cancelDownload(String fileURL) {
        System.out.println("Cancelling download...");

        isCancelled = true;
        isPaused = false;

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();  
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupted while waiting for executor termination");
                executor.shutdownNow();
                Thread.currentThread().interrupt();  
            }
        }

        if (latch != null) {
            while (latch.getCount() > 0) {
                latch.countDown();
            }
        }

        if (conn != null) {
            conn.disconnect();
        }

        if (raf != null) {
            try {
                raf.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println(filePath);
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists() && !filePath.equals("downloads/")) {
                boolean deleted = file.delete();
                System.out.println("File deleted: " + deleted);
            }
        }

        filePath = "downloads/";
        fileName = "";
        contentLength = 0;

        System.out.println("Download Cancelled.");
    }


    public boolean isCancelled() {
        return isCancelled;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public Object pauseLock() {
        return pauseLock;
    }
}