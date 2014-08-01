package driverdownloader;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.swing.JOptionPane;

public class UpdateDataThread extends Thread {
    DriverManager driverManager;
    String manufacturer;
    boolean updateOnlyCategory;
    ThreadListener listener;

    UpdateDataThread(DriverManager driverManager, String manufacturer, boolean updateOnlyCategory, ThreadListener listener) {
        this.driverManager = driverManager;
        this.manufacturer = manufacturer;
        this.updateOnlyCategory = updateOnlyCategory;
        this.listener = listener;
    }

    @Override
    public void run() {
    	
    	listener.started();

        Interrupt.set(false);

        boolean updateOk = false;

        try {
            driverManager.updateData(manufacturer, updateOnlyCategory);
            updateOk = true;
        } catch (MalformedURLException ex) {
        	listener.reportMessage("URL is not well formed:\n"+ex,
                    "Incorrect URL", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
        	listener.reportMessage("There was an error while trying to load the file:\n"+ex,
                    "IO error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
        	listener.reportMessage(""+ex, "Error", JOptionPane.ERROR_MESSAGE);
        }
        finally {
        	listener.stopped();
        }

        if (Interrupt.get()) {
        	listener.reportMessage("Update was interrupted", "Interrupted", JOptionPane.INFORMATION_MESSAGE);
        }
        else if (updateOk) {
        	listener.reportMessage("Update finished", "Done", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
