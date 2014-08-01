package driverdownloader;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class manages all the present objects that implement the
 * IDriverDownloader interface.
 * 
 * @author Mark Van Peteghem
 */
public class DriverManager {
    public IDriverDownloader createDriverDownloader(String name) throws Exception {
        IDriverDownloader driverDownloader = driverDownloaders.get(name);
        return driverDownloader;
    }

    static public class Exception extends java.lang.Exception {
        Exception(String name) {
            super("no driverdownloader functionality known for manufacturer "+name );
        }
    }

    public ArrayList<String> loadData() {
        addDriverDownloader(new GatewayDriverDownloader());
        addDriverDownloader(new SonyDriverDownloader());
        addDriverDownloader(new HPDriverDownloader());
        addDriverDownloader(new AsusDriverDownloader());
        addDriverDownloader(new AcerDriverDownloader());
        addDriverDownloader(new EMachinesDriverDownloader());
        addDriverDownloader(new ToshibaDriverDownloader());
        addDriverDownloader(new DellDriverDownloader());
        addDriverDownloader(new IbmLenovoDriverDownloader());
        addDriverDownloader(new PanasonicDriverDownloader());

        activeDriverDownloader = driverDownloaders.get("Gateway");

        ArrayList<String> errors = new ArrayList<String>();
        for (IDriverDownloader driverDownloader: driverDownloaders.values()) {
            try {
                driverDownloader.loadData();
                driverDownloader.correct();
            } catch (java.lang.Exception ex) {
                errors.add("Could not load file "+driverDownloader.getDataFilename()+": "+ex);
            }
        }
        return errors;
    }

    private void addDriverDownloader(IDriverDownloader driverDownloader) {
        driverDownloaders.put(driverDownloader.getName(), driverDownloader);
    }

    public ArrayList<String> getManufacturers() {
        ArrayList<String> manufacturersList = new ArrayList<String>(driverDownloaders.keySet());

        return manufacturersList;
    }

    void selectManufacturer(String str) {
        activeDriverDownloader = driverDownloaders.get(str);
    }

    void selectProductType(String str) throws java.lang.Exception {
        activeDriverDownloader.getData().selectProductType(str);
    }

    ArrayList<String> getProductTypes() {
        return activeDriverDownloader.getData().getProductTypes();
    }

    ArrayList<String> getModelNumbers() {
        return activeDriverDownloader.getData().getModelNumbers();
    }

    PostableUrl getURL(int idx) {
        return activeDriverDownloader.getData().getURL(idx);
    }

	public PostableUrl getURL(String name) throws java.lang.Exception {
		return activeDriverDownloader.getData().getURL(name);
	}

    void addListener(DownloadObserver iDriverDownloaderListener) {
        for (IDriverDownloader driverDownloader: driverDownloaders.values())
            driverDownloader.getDownloadObservable().addListener(iDriverDownloaderListener);
    }

    void updateData(String manufacturer, boolean updateOnlyCategory) throws java.lang.Exception {
        driverDownloaders.get(manufacturer).updateData(updateOnlyCategory);
    }

	Map<String, IDriverDownloader> driverDownloaders = new TreeMap<String, IDriverDownloader>();
    IDriverDownloader activeDriverDownloader;
}
