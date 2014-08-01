package driverdownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Mark Van Peteghem
 */
public class SelectedDownload {
    IDriverDownloader driverDownloader;
    String model;
    PostableUrl url;
    File directory;
    enum Status { Waiting, Downloading, Processing, Error, Done }
    Status status;

    List<Listener> listeners = new ArrayList<Listener>();

    SelectedDownload(IDriverDownloader driverDownloader, String model, PostableUrl url, File directory)  {
        this.driverDownloader = driverDownloader;
        this.model = model;
        this.url = url;
        this.directory = directory;
        this.status = Status.Waiting;
    }

    void handle() throws Exception {
        status = Status.Downloading;
        updateStatus();

        try {
        	final String commandName = "start.exe";
            driverDownloader.download(url, directory.getAbsolutePath());
            String batchFilePath = directory+File.separator+commandName;
            File file = new File(batchFilePath);
            if (file.exists()) {
            	status = Status.Processing;
            	updateStatus();

            	// some versions of Windows have cmd.exe, others have command.com, so try both
            	try {
            		Process process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", batchFilePath}, null, directory);
            		MessageHandler.addMessage("running "+commandName);
            		//process.waitFor();
            	} catch (Exception ex) {
            		try {
            			Process process = Runtime.getRuntime().exec(new String[]{"command.com", "/c", batchFilePath}, null, directory);
            			MessageHandler.addMessage("running "+commandName);
                		//process.waitFor();
            		} catch (Exception ex2) {
            			MessageHandler.addError("error occurred while running "+commandName);
            		}
            	}
            } else {
            	MessageHandler.addMessage("no file called "+commandName+" found");
            }
        } catch (Exception ex) {
            status = Status.Error;
            updateStatus();
            throw ex;
        }

        if (Interrupt.get()) {
            status = Status.Waiting;
        } else {
            status = Status.Done;
        }
        updateStatus();
    }

    public void addStatusListener(Listener listener) {
        listeners.add(listener);
    }

    private void updateStatus() {
        for (Listener listener: listeners) {
            listener.update();
        }
    }

    public static interface Listener {
        public void update();
    }
}
