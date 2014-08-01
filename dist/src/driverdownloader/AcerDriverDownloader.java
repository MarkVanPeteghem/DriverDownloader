package driverdownloader;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class to download drivers from Acer
 * Note: The website contains some forms that use the POST method to
 *       submit, which makes it a little harder to scrape the pages.
 * @author Mark Van Peteghem
 *
 */
public class AcerDriverDownloader extends DriverDownloader {

	// note that SC=PA_7& has to be included here, otherwise we get a page without subcategories
	private final String homepage = "http://global-download.acer.com/GDHome.aspx?SC=PA_7&LC=en";
	
	@Override
	public void download(PostableUrl url, String path) {
		try {
			DownloadedPage page = download.getPage(url);
            if (Interrupt.get()) {
                return;
            }
			String content = page.getContent();
			
			List<String> OSes = null;
			if (content.contains("Operating System")) {
				String osSection = ParseUtils.findSubstringByHeaderString(content, "Operating System", "</select>", false);
				OSes = ParseUtils.getSubStrings(osSection, "value=\"", "\"", false);
				if (OSes.size()==1 && OSes.get(0).startsWith("-"))
					OSes = null;
			}
			
			String categoryTable = ParseUtils.findSubstringByHeaderString(content, "<table id=\"CategoryTable\"", "</table>", false);
			List<String> lines = ParseUtils.getSubStrings(categoryTable, "<td", "</td>", false);
			List<String> categories = new ArrayList<String>();
			for (String line: lines) {
				String category = ParseUtils.findSubstringByHeaderString(line, "value=\"", "\"", false);
				categories.add(category);
			}
			
			for (String category: categories) {
				if (null==OSes) {
					// FS=O02 is necessary to download a page with all the files instead of just the latest
					String link = page.getUrl()+"&FS=O02&Category="+category;
					downloadFiles(link, path);				
	                if (Interrupt.get())
	                	break;
				} else {
					for (String os: OSes) {
						if (!os.startsWith("-")) {
							// FS=O02 is necessary to download a page with all the files instead of just the latest
							String link = page.getUrl()+"&OS="+os+"&FS=O02&Category="+category;
							downloadFiles(link, path);
		                    if (Interrupt.get())
		                    	break;
						}
					}
				}
			}
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
	}
	
	private void downloadFiles(String link, String path) {
		try {
			DownloadedPage page = download.getPage(new PostableUrl(link));
            if (Interrupt.get()) {
                return;
            }
			String content = page.getContent();
			String url = page.getUrl();
			int slashPos = url.indexOf('/', 8);
			String domain = url.substring(0, slashPos+1);
			
			String filesTable = ParseUtils.findSubstringByHeaderString(content, "id=\"FileTable\"", "</table>", false);
			List<String> fileLinks = ParseUtils.getSubStrings(filesTable, "href=\"", "\"", false);
			
			for (String fileLink: fileLinks) {
				if (!fileLink.equals("about:blank"))
					download.downloadFile(domain+fileLink.replaceAll(" ", "%20"), path);
	            if (Interrupt.get())
	            	break;
			}
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}			
	}

	@Override
	public String getName() {
		return "Acer";
	}

	@Override
	public String getDataFilename() {
		return "acer.xml";
	}

	@Override
	public void correct() {
	}

	@Override
	protected Map<String, PostableUrl> getCategories() throws Exception {
		Map<String, PostableUrl> categories = new TreeMap<String, PostableUrl>();
		
		DownloadedPage page = download.getPage(new PostableUrl(homepage));
		String content = page.getContent();
		
		Map<String, String> postData = ParseUtils.getInputs(content, 0);
		DownloadedPage page2 = download.getPage(new PostableUrl("http://global-download.acer.com/Step1.aspx", postData));
		String content2 = page2.getContent();
		String form = ParseUtils.findSubstringByHeaderString(content2, "<form", "</form>", false);
		List<String> inputs = ParseUtils.getSubStrings(form, "<input", ">", false);
		for (String input: inputs) {
			if (!input.contains("__VIEWSTATE")) {
				String name = ParseUtils.findSubstringByHeaderString(input, "value=\"", "\"", false);
				String categoryLink = "http://global-download.acer.com/Step2.aspx?Step1="+URLEncoder.encode(name, "UTF-8")+"&Step2=&Step3=&BC=Acer&SC=PA_7&LC=en";
				categories.put(name, new PostableUrl(categoryLink));
			}
		}
		
		return categories;
	}

	@Override
	protected void updateCategory(DownloadedPage page) throws Exception {
		// remove SC=PA_7& to get all the models
		String generalLink = page.getUrl().replace("Step2.aspx", "Step3.aspx").replace("SC=PA_7&", "");

		String form = ParseUtils.findSubstringByHeaderString(page.getContent(), "<form", "</form>", false);
		List<String> names= ParseUtils.getSubStrings(form, "<td>", "</td>", false);
		for (String name: names) {
			String link = generalLink.replace("Step2=", "Step2="+URLEncoder.encode(name.replaceAll("&quot;", "\""), "UTF-8"));
			updateProductLine(download.getPage(new PostableUrl(link)));
		}
	}

	protected void updateProductLine(DownloadedPage page) throws Exception {
		String generalLink = page.getUrl().replace("Step3.aspx", "Step5.aspx");

		String form = ParseUtils.findSubstringByHeaderString(page.getContent(), "<form", "</form>", false);
		List<String> names= ParseUtils.getSubStrings(form, "<td>", "</td>", false);
		for (String name: names) {
			String link = generalLink.replace("Step3=", "Step3="+URLEncoder.encode(name, "UTF-8"));
			driverData.addModelNumber(name, link);
		}
	}

}
