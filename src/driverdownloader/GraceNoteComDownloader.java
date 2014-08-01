package driverdownloader;

public class GraceNoteComDownloader {

	private GraceNoteComDownloader() {
    }
    
    private static Download download = Download.get();

	public static void download(String url, String localPath) throws Exception {
		// The page has only one link
                DownloadedPage page = download.getPage(new PostableUrl(url));
                String content = page.getContent();
		String link = ParseUtils.findSubstringByHeaderString(content, "<a href=\"", "\">", false);
		download.downloadFile(link, localPath);
	}
}
