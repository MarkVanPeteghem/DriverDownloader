package driverdownloader;

/**
 *
 * @author Mark Van Peteghem
 */
public class RealComDownloader {
    private RealComDownloader() {
    }
    
    private static Download download = Download.get();

    public static void download(String url, String localPath) throws Exception {
        /*DownloadedPage page = download.getPage(new PostableUrl(url));
        String content = page.getContent();
        url = ParseUtils.findLastSubstringBySurroundingStrings(content, "clicking", "href=\"", "\"", false);
        page = download.getPage(new PostableUrl(url));
        content = page.getContent();
        url = ParseUtils.findLastSubstringBySurroundingStrings(content, "download_button.gif", "href=\"", "\"", false);
        download.downloadFile(url, localPath);*/
    }
}
