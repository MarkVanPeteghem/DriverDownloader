package driverdownloader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import javax.swing.JOptionPane;

public class DownloadThread extends Thread {
    List<SelectedDownload> selectedDownloads;
    SelectedDownload activeDownload;
    
    ThreadListener listener;

    DownloadThread(List<SelectedDownload> selectedDownloads, ThreadListener listener) {
        this.selectedDownloads = selectedDownloads;
        this.listener = listener;
    }

    @Override
    public void run() {

    	listener.started();
        Interrupt.set(false);

        boolean allDownloadsOk = true;

        int idx = 0;
        while (idx<selectedDownloads.size()) {
        	SelectedDownload selectedDownload = selectedDownloads.get(idx);
        	listener.addMessage("Downloading files for "+selectedDownload.model);
            setActiveDownload(selectedDownload);
            boolean downloadOk = false;
            try {
                selectedDownload.handle();
                downloadOk = true;
            } catch (MalformedURLException ex) {
                String msg = "URL is not well formed:\n"+ex;
                listener.reportMessage(msg, "Incorrect URL", JOptionPane.ERROR_MESSAGE);
                System.out.println(msg);
            } catch (IOException ex) {
                String msg = "There was an error while trying to load the file:\n"+ex;
                listener.reportMessage(msg, "IO error", JOptionPane.ERROR_MESSAGE);
                System.out.println(msg);
            } catch (InterruptedException ex) {
            } catch (Exception ex) {
                String msg = ""+ex;
                listener.reportMessage(msg, "Error", JOptionPane.ERROR_MESSAGE);
            }
            finally {
                if (!downloadOk)
                    allDownloadsOk = false;
            }
            if (Interrupt.get())
                break;
            ++idx;
        }
        
        listener.stopped();

        if (Interrupt.get())
        	listener.reportMessage("Download was interrupted", "Interrupted", JOptionPane.INFORMATION_MESSAGE);
        else if (!allDownloadsOk)
        	listener.reportMessage("One or more errors occurred", "Error", JOptionPane.ERROR_MESSAGE);
        // no message to the user if all went fine
    }

    synchronized private void setActiveDownload(SelectedDownload activeDownload) {
        this.activeDownload = activeDownload;
    }

    synchronized private SelectedDownload getActiveDownload() {
        return activeDownload;
    }
}
