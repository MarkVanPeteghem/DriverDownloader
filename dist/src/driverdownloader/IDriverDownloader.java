package driverdownloader;

/**
 *
 * @author Mark Van Peteghem
 */
public interface IDriverDownloader {
    public void download(PostableUrl url, String Path);
    public DownloadObservable getDownloadObservable();
    public String getName();
    public String getDataFilename();
    public void loadData() throws Exception;
    public DriverDataOfSpecificManufacturer getData();
    public void updateData(boolean updateOnlyCategory) throws Exception;
    
    // Used to correct url data if it seems that it wasn't done correctly
    // in updataData() the previous time, and redoing it completely would
    // take too long.
    // This is called at startup time, so make the method empty if the
    // correction is done.
    public void correct();
}
