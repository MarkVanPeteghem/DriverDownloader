package driverdownloader;

import java.util.Vector;

/**
 * Simple observable class to inform the GUI or other components
 * about the download progress.
 *   
 * @author Mark Van Peteghem
 */
public class DownloadObservable {
    private static DownloadObservable downloadObservable = new DownloadObservable();
    private Vector<DownloadObserver> listeners = new Vector<DownloadObserver>();
    private DownloadObservable() {
    }

    public static DownloadObservable get() {
        return downloadObservable;
    }

    public final void addListener(DownloadObserver listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public final void updateFilename(String filename) {
        for (DownloadObserver listener: listeners) {
            listener.updateFile(filename);
        }
    }
    public final void updateProgress(long bytes) {
        for (DownloadObserver listener: listeners) {
            listener.updateProgress(bytes);
        }
    }
}
