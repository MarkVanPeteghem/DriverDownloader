package driverdownloader;

import java.io.FileOutputStream;

/**
 * This is a simple class that contains a link and its content.
 * The link may be redirected. We need this redirected link to construct
 * a full link from a relative link, with some sites this is necessary.
 * 
 * @author Mark Van Peteghem
 */
public class DownloadedPage {
    private String content;
    private String url;

    public DownloadedPage(String content, String url) {
        this.content = content;
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public String getUrl() {
        return url;
    }
    
    public void saveTo(String filename) {
    	try {
			FileOutputStream out = new FileOutputStream(filename);
			out.write(content.getBytes());
			out.close();
    	} catch (Exception ex) {
    		System.err.println(""+ex);
    	}
    }
}
