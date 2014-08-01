package driverdownloader;

/**
 *
 * @author Mark
 */
public interface DownloadObserver {

    public void updateFile(String filename);
    public void updateProgress(long bytes);
}
