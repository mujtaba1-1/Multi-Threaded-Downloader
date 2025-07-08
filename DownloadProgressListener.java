public interface DownloadProgressListener {
    void updateProgress(int bytes, int contentLength);
}