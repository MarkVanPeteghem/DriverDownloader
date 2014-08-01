package driverdownloader;

import java.util.GregorianCalendar;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class to download drivers from Toshiba
 * Note: the site uses JSON, which makes it easy to scrape the pages.
 * @author Mark Van Peteghem
 *
 */
public class ToshibaDriverDownloader extends DriverDownloader {
	
	private String homepage = "http://www.csd.toshiba.com/cgi-bin/tais/support/jsp/home.jsp?nav=Download";

	@Override
	public void download(PostableUrl url, String path) {
		try {
			DownloadedPage page1 = download.getPage(url);
			String content1 = page1.getContent();
			String folder = ParseUtils.getUrlFolder(page1.getUrl());
			String url2 = ParseUtils.findSubstringByHeaderString(content1, "tab_DL_0", "url=\"", "\"", false);
	
			DownloadedPage page2 = download.getPage(new PostableUrl(folder+url2));
			String content2 = page2.getContent();
			
			// searching for ";" is not safe because a string could contain ";",
			// so we look for "];" and add "]"
			String fileData = ParseUtils.findSubstringByHeaderString(content2, "var docListArr = ", "];", false)+"]";
			JSONArray files = new JSONArray(fileData);
			
			for (int f=0; f<files.length(); ++f) {
				JSONObject fileObject = files.getJSONObject(f);
				String oid = fileObject.getString("oid");
				long nowl = new GregorianCalendar().getTimeInMillis();
				String fileInfoLink = "http://www.csd.toshiba.com/cgi-bin/tais/support/jsp/detailView.jsp?title=Download&jsp=downloadDetail.jsp&soid="+oid+"&_="+nowl;
				DownloadedPage filePage = download.getPage(new PostableUrl(fileInfoLink));
				String filePageContent = filePage.getContent();
				String fileLink = ParseUtils.findSubstringByHeaderString(filePageContent, ">File:<", "href=\"", "\"", false);
				download.downloadFile(fileLink, path);
			}
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
	}

	@Override
	public String getName() {
		return "Toshiba";
	}

	@Override
	public String getDataFilename() {
		return "toshiba.xml";
	}

	@Override
	public void correct() {
	}

	@Override
    public void updateData(boolean updateOnlyCategory) {
		try {
			driverData.clear();
			
			DownloadedPage page = download.getPage(new PostableUrl(homepage));
			String content = page.getContent();
			
			String data = ParseUtils.findSubstringByHeaderString(content, "var modelList = eval(", ");", false);
			
			JSONObject modelsObject = new JSONObject(data);
	
			JSONArray products = modelsObject.getJSONArray("products");
			for (int p=0; p<products.length(); ++p) {
				JSONObject product = products.getJSONObject(p);
				
				String name = product.getString("pname");
				String productTypeId = product.getString("poid");
				driverData.addProductType(name);
				
				JSONArray families = product.getJSONArray("families");
				for (int f=0; f<families.length(); ++f) {
					JSONObject family = families.getJSONObject(f);
					String familyId = family.getString("foid");
					
					JSONArray models = family.getJSONArray("models");
					for (int m=0; m<models.length(); ++m) {
						JSONObject model = models.getJSONObject(m);
						String modelId = model.getString("moid");
						String modelName = model.getString("mname");
						String rpn = model.has("rpn") ? rpn = model.getString("rpn") : "undefined";

						String link = "http://www.csd.toshiba.com/cgi-bin/tais/support/jsp/modelContent.jsp?ct=DL&os=&category="+
							"&moid="+modelId+
							"&rpn="+rpn+
							"&modelFilter="+modelName+
							"&selCategory="+productTypeId+
							"&selFamily="+familyId;
						driverData.addModelNumber(modelName, link);
					}
				}
			}
			saveData();
			
		} catch (Exception ex) {
    		MessageHandler.addError(ex);
    		try {
    			loadData();
    		} catch (Exception ex2) {
    			
    		}
	    }
	}

	// This will not be called because we override updateData 
	@Override
	protected Map<String, PostableUrl> getCategories() throws Exception {
		return null;
	}

	// This will not be called because we override updateData 
	@Override
	protected void updateCategory(DownloadedPage page) throws Exception {

	}

}
