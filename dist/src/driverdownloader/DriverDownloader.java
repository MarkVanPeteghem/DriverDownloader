package driverdownloader;

import java.util.Map;

import javax.swing.JOptionPane;

/**
 *
 * @author Mark Van Peteghem
 */
public abstract class DriverDownloader implements IDriverDownloader {

    protected DriverDataOfSpecificManufacturer driverData = new DriverDataOfSpecificManufacturer();
    protected DownloadObservable downloadObservable = DownloadObservable.get();
    protected Download download = Download.get();

    public DriverDataOfSpecificManufacturer getData() {
        return driverData;
    }

    public void loadData() throws Exception {
        driverData.readData(getDataFilename());
    }

    public void saveData() throws Exception {
        driverData.saveData(getDataFilename());
    }

    public void updateData(boolean updateOnlyCategory) {
        try {
        	Map<String, PostableUrl> categories = getCategories();
        	if (null==categories)
        		return; // it was probably interrupted
        	if (updateOnlyCategory) {
        		String category = driverData.getActiveProductType();
        		driverData.clearProductType(category);
        		updateCategory(categories.get(category));
        	} else {
	            driverData.clear();
        		// first add all the categories, so that if the updating is stopped in the debugger
        		// for some reason, we can update per category later
        		for (Map.Entry<String, PostableUrl> pair: categories.entrySet()) {
        			driverData.addProductType(pair.getKey());
        		}
        		for (Map.Entry<String, PostableUrl> pair: categories.entrySet()) {
        			driverData.selectProductType(pair.getKey());
        			updateCategory(pair.getValue());
                    if (Interrupt.get())
                        break;
        		}
        	}
            if (Interrupt.get())
                loadData();
            else
                saveData();
        } catch (Exception ex) {
        	try {
        		loadData();
        	} catch (Exception ex2) {
        		MessageHandler.addError(ex2);
        	}
            MessageHandler.addError(ex);
        }
    }

    protected abstract Map<String, PostableUrl> getCategories() throws Exception;
    
    final protected void updateCategory(PostableUrl url) {
    	try {
	    	DownloadedPage page = download.getPage(url);
	    	if (!Interrupt.get())
	    		updateCategory(page);
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
    }
    
    protected abstract void updateCategory(DownloadedPage page) throws Exception;

    private String binaryExtensions[] = new String[] { ".exe", ".zip", ".dmg", ".7z", ".tar", ".gz", ".bz2", ".rar" };

    public void externalDownload(String url, String localPath) throws Exception {
        // first check if it is a binary file by looking at the extension
        int lastSlashPos = url.lastIndexOf("/");
        if (lastSlashPos<0)
            lastSlashPos = 0;
        int questionMarkPos = url.lastIndexOf("?");
        if (questionMarkPos<0)
            questionMarkPos = url.length();
        String filename = url.substring(lastSlashPos+1, questionMarkPos);
        for (String ext: binaryExtensions) {
            if (filename.endsWith(ext)) {
        	download.downloadFile(url, localPath);
                return;
            }
        }

        // otherwise suppose it is a html page, which we have to handle differently
        // for every website
        int doubleSlashPos = url.indexOf("//");
        if (doubleSlashPos<0)
            doubleSlashPos = 0; // htpp:// may be omitted
        int singleSlashPos = url.indexOf("/", doubleSlashPos+2);
        if (singleSlashPos<0)
            throw new Exception("Can't get domain from "+url);
        String domain = url.substring(doubleSlashPos+2, singleSlashPos);
        if (domain.contains("real.com")) {
            RealComDownloader.download(url, localPath);
        } else if (domain.contains("gracenote.com")) {
        	GraceNoteComDownloader.download(url, localPath);
        } else if (domain.contains("apple.com")) {
            // seemed to be broken links!
        } else {
            JOptionPane.showMessageDialog(null, "Don't know how to download from "+domain, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public DownloadObservable getDownloadObservable() {
        return downloadObservable;
    }
}
