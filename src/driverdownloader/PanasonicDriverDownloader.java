package driverdownloader;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class PanasonicDriverDownloader extends DriverDownloader {

	private final static String homepage = "http://pc-dl.panasonic.co.jp/dl/";
	private final static String domain = "http://pc-dl.panasonic.co.jp";

	@Override
	public void download(PostableUrl url, String path) {
		try {
			DownloadedPage page = download.getPage(url);
			
			String pageContent = page.getContent();
			if (pageContent.contains("There were no applicable downloads found")) {
				return;
			}
				
			downloadFiles(pageContent, path);
			
			if (pageContent.contains("class=\"pagination\"")) {
				String pagination = ParseUtils.findSubstringByHeaderString(pageContent, "class=\"pagination\"", "</div>", false);
				int pageNr = 2;
				while (pagination.contains("&amp;page="+pageNr+"&")) {
					downloadFiles(pageContent+"&page="+pageNr, path);
					++pageNr;
				}
			}
		} catch (Exception ex) {
			MessageHandler.addError(ex);
		}
	}

	private void downloadFiles(String OSPageContent, String path) throws Exception {
		String list = ParseUtils.findSubstringByHeaderString(OSPageContent, "\"doc_list\"", "</ol>", false);
		List<String> files = ParseUtils.getSubStrings(list, "<li", "</li>", false);
		for (String file: files) {
			if (!file.contains("href=\"")) {
				continue;
			}
			String filePageLink = domain+ParseUtils.findSubstringByHeaderString(file, "href=\"", "\"", false);
			if (filePageLink.endsWith(".pdf")) {
				download.downloadFile(filePageLink, path); // it is a link to the file itself (maybe not necessary)
			} else {
				DownloadedPage filePage = download.getPage(new PostableUrl(filePageLink));
				String filePageContent = filePage.getContent();
				// some pages don't have a download list
				if (filePageContent.contains("class=\"download_file_list\"")) {
					String linkSection = ParseUtils.findSubstringByHeaderString(filePageContent, "class=\"download_file_list\"", "</ol>", false);
					List<String> fileLinks = ParseUtils.getSubStrings(linkSection, "href=\"", "\"", false);
					for (String fileLink: fileLinks) {
						if (!fileLink.contains("/cart/")) {
							download.downloadFile(domain+fileLink, path);
						}
					}
				}
			}
		}
	}
	
	@Override
	public String getName() {
		return "Panasonic";
	}

	@Override
	public String getDataFilename() {
		return "panasonic.xml";
	}

	@Override
	public void correct() {
	}

    @Override
    protected Map<String, PostableUrl> getCategories() throws Exception {
        Map<String, PostableUrl> categories = new TreeMap<String, PostableUrl>();
		
		DownloadedPage page = download.getPage(new PostableUrl(homepage));
		String content = page.getContent();
		
		String form = ParseUtils.findSubstringByHeaderString(content, "Category:</label>", "</select>", false);
		String driversId = ParseUtils.findSubstringByFooterString(form, "<option value=\"", "\">All Driver", false);

		String categoryLink = "http://pc-dl.panasonic.co.jp/dl/products?dc%5B%5D="+driversId;
		DownloadedPage categoryPage = download.getPage(new PostableUrl(categoryLink));
		JSONArray products = new JSONArray(categoryPage.getContent());
		
		for (int p=0; p<products.length(); ++p) {
			JSONObject fileObject = products.getJSONObject(p);
			String productTypeId = fileObject.getString("value");
			String productType = fileObject.getString("text");
			String link = "http://pc-dl.panasonic.co.jp/dl/models?p="+productTypeId+"&dc%5B%5D=002001";
			categories.put(productType, new PostableUrl(link));
		}
        return categories;
    }

    @Override
    protected void updateCategory(DownloadedPage page) throws Exception {
    	String link = page.getUrl();
    	String productTypeId = ParseUtils.findSubstringByHeaderString(link, "models?p=", "&dc", false);
		JSONArray models = new JSONArray(page.getContent());
		
		for (int m=0; m<models.length(); ++m) {
			JSONObject modelObject = models.getJSONObject(m);
			String modelTypeId = modelObject.getString("value");
			String modelType = modelObject.getString("text");
			String modelLink = "http://pc-dl.panasonic.co.jp/dl/search?q=&button=&dc[]=002001&p1="+productTypeId+"&p2="+modelTypeId+"&oc=&lang=005&per_page=100";
			driverData.addModelNumber(modelType, modelLink);
		}
    }
}
