package driverdownloader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to download drivers from Asus
 * Note: The website contains some forms that use the POST method to
 *       submit, which makes it a little harder to scrape the pages.
 * @author Mark Van Peteghem
 *
 */
public class AsusDriverDownloader extends DriverDownloader {

	private String homepage = "http://support.asus.com/Select/ModelSelect.aspx?SLanguage=en&type=1&KeepThis=true&";
	
	@Override
	public String getName() {
		return "Asus";
	}

	@Override
	public String getDataFilename() {
		return "asus.xml";
	}

	@Override
	public void download(PostableUrl url, String path) {
		try {
			DownloadedPage page = download.getPage(url);
            if (Interrupt.get()) {
                return;
            }
			String content = page.getContent();
			
			Map<String, String> postData = ParseUtils.getInputs(content, 0);
	
			findAndAddPostBackParams(content, postData);
			
			String osSection = ParseUtils.findSubstringByHeaderString(content, "Select OS</option>", "</select>", false);
			List<String> values = ParseUtils.getSubStrings(osSection, "value=\"", "\"", false);
			for (String value: values) {
				postData.put("ddlOS", value);
				DownloadedPage osPage = download.getPage(new PostableUrl(url.getLink(), postData));
	            if (Interrupt.get()) {
	                break;
	            }

	            downloadForOs(osPage, path);
	            
				if (Interrupt.get())
	            	break;
			}
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
	}

	private void downloadForOs(DownloadedPage page, String path) {
		try {
			String content = page.getContent();
			
			Map<String, String> postData = ParseUtils.getInputs(content, 0);
	
			List<String> postBackParamSets = ParseUtils.getSubStrings(content, ":__doPostBack(", "\"", false);
			for (String postBackParams: postBackParamSets) {
				addPostBackParams(postData, postBackParams);
				DownloadedPage sectionPage = download.getPage(new PostableUrl(page.getUrl(), postData));
	            if (Interrupt.get()) {
	                break;
	            }
	            
				downloadFiles(sectionPage, path);
				if (Interrupt.get())
	            	break;
			}
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
	}

	private void downloadFiles(DownloadedPage page, String path) {
		try {
			String content = page.getContent();
			
			List<String> downloadCalls = ParseUtils.getSubStrings(content, "dlm_click(", ");\"", false);
			for (String downloadCall: downloadCalls) {
				List<String> params = ParseUtils.getSubStrings(downloadCall, "'", "'", false);
				if (params.size()<9)
					throw new UnexpectedFormatException();
				String baseLink;
				if (params.get(8).equals("1"))
					baseLink = "http://dlcdnas.asus.com/pub/ASUS/";
				else {
					String domain = ParseUtils.findSubstringByHeaderString(content, "name=\"hld_AkamaiURL\"", "value=\"", "\"", false);
					baseLink = "http://"+domain+"/pub/ASUS/";
				}
				String link = baseLink+params.get(1);
				download.downloadFile(link, path);
	
				if (Interrupt.get())
	            	break;
			}
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
	}
	
	private void findAndAddPostBackParams(String content, Map<String, String> postData)
			throws UnexpectedFormatException {
		String postBackCall = ParseUtils.findSubstringByHeaderString(content, "onchange=\"__doPostBack", "\"", false);
		addPostBackParams(postData, postBackCall);
	}

	private void addPostBackParams(Map<String, String> postData,
			String postBackCall) throws UnexpectedFormatException {
		List<String> params = ParseUtils.getSubStrings(postBackCall, "'", "'", false);
		if (2!=params.size())
			throw new UnexpectedFormatException();
		String eventTarget = params.get(0).replaceAll("\\$", ":");
		String eventArgument = params.get(1);
		postData.put("__EVENTTARGET", eventTarget);
		postData.put("__EVENTARGUMENT", eventArgument);
	}

	@Override
	protected Map<String, PostableUrl> getCategories() throws Exception {
		Map<String, PostableUrl> categories = new HashMap<String, PostableUrl>();
		
		DownloadedPage page = download.getPage(new PostableUrl(homepage));
        if (Interrupt.get()) {
            return null;
        }
		String content = page.getContent();
		
		// "/Select/ModelSelect.aspx?SLanguage=en&type=1&KeepThis=true&";
		
		
        List<String> inputs = ParseUtils.getSubStrings(content, "<a id=\"Repeater", "</a>", false);
        for (String input: inputs) {
        	String name = ParseUtils.findSubstringByHeaderString(input, ">", "", false).trim();
        	String eventTarget = ParseUtils.findSubstringByHeaderString(input, "&#39;", "&#39;", false).trim();

    		Map<String, String> post = new HashMap<String, String>();
    		post.put("__EVENTTARGET", eventTarget);
    		post.put("ScriptManager1", "ScriptManager1|"+eventTarget);
    		categories.put(name, new PostableUrl(homepage, post));
        }
        
        return categories;
	}

	@Override
	protected void updateCategory(DownloadedPage page) throws Exception {
		String link = page.getUrl();
		int slashPos = link.lastIndexOf("/");
		String baseLink = page.getUrl().substring(0, slashPos+1);

		String content = page.getContent();
		List<String> modelLinks = ParseUtils.getSubStrings(content, "<a id=\"Repeater", "</a>", false);
		for (String modelLink: modelLinks) {
			String eventTarget = ParseUtils.findSubstringByHeaderString(modelLink, "&#39;", "&#39;", false);
			String name = ParseUtils.findSubstringByHeaderString(modelLink, ">", "", false);
    		Map<String, String> post = new HashMap<String, String>();
    		post.put("__EVENTTARGET", eventTarget);
    		post.put("ScriptManager1", "ScriptManager1|"+eventTarget);
			driverData.addModelNumber(name, new PostableUrl(homepage, post));
		}
	}
	
	@Override
	public void correct() {
		
	}

	@Override
	public void CompleteUrl(PostableUrl url) {
		Map<String, String> post = url.getPostData();
		post.put("langNormal", "en");
		post.put("hd_l_series", "Series");
		post.put("hd_l_model", "Model");
		post.put("hd_l_os", "OS");
		post.put("hd_select_type", "1");
		post.put("__ASYNCPOST", "true");
		post.put("__EVENTARGUMENT", "");
		post.put("__VIEWSTATE", "/wEPDwUKMTAzOTQ0MDE1OQ8WCB4HbDFfbmFtZQVARWVlIEZhbWlseSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIB4FbDFfaWQFAjIwHgVsMl9pZGQeBm1fbmFtZWQWAgIDD2QWDAIDD2QWAmYPZBYGAgEPDxYCHgRUZXh0BQdQcm9kdWN0ZGQCAw8PFgIfBAUGU2VyaWVzZGQCBQ8PFgIfBAUFTW9kZWxkZAIFDxYCHgtfIUl0ZW1Db3VudAIKFhRmD2QWBGYPFQIlL2ltYWdlcy9wcm9kdWN0cy80OXg0OS80OXg0OWR3X25iLmpwZwBkAgEPFgIfBQIBFgJmD2QWBAIBDw8WAh8EBUBOb3RlYm9vayAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgFgQfAQUBMx8CBQEwZAIDDw8WAh4HVmlzaWJsZWhkZAIBD2QWBGYPFQImL2ltYWdlcy9wcm9kdWN0cy80OXg0OS80OXg0OWR3X2VwYy5qcGcAZAIBDxYCHwUCARYCZg9kFgQCAQ8PFgIfBAVARWVlIEZhbWlseSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIBYEHwEFAjIwHwIFATBkAgMPDxYCHwZoZGQCAg9kFgRmDxUCJS9pbWFnZXMvcHJvZHVjdHMvNDl4NDkvNDl4NDlkd19tYi5qcGcNUEMgQ29tcG9uZW50c2QCAQ8WAh8FAgYWDGYPZBYCAgEPDxYCHwQFQE1vdGhlcmJvYXJkICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAWBB8BBQExHwIFATBkAgEPZBYCAgEPDxYCHwQFQE9wdGljYWwgU3RvcmFnZSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAWBB8BBQEyHwIFATBkAgIPZBYCAgEPDxYCHwQFQEdyYXBoaWMgQ2FyZCAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAWBB8BBQE5HwIFATBkAgMPZBYCAgEPDxYCHwQFQFRoZXJtYWwgU29sdXRpb24gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAWBB8BBQIxNh8CBQEwZAIED2QWAgIBDw8WAh8EBUBNdWx0aW1lZGlhICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgFgQfAQUCMTkfAgUBMGQCBQ9kFgQCAQ8PFgIfBAVAQXVkaW8gQ2FyZHMgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIBYEHwEFAjIxHwIFATBkAgMPDxYCHwZoZGQCAw9kFgRmDxUCKi9pbWFnZXMvcHJvZHVjdHMvNDl4NDkvNDl4NDlkd19kZXNrdG9wLmpwZwdEZXNrdG9wZAIBDxYCHwUCAxYGZg9kFgICAQ8PFgIfBAVAQmFyZWJvbmUgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIBYEHwEFATgfAgUBMGQCAQ9kFgICAQ8PFgIfBAVARGVza3RvcCBQQyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIBYEHwEFAjE0HwIFATBkAgIPZBYEAgEPDxYCHwQFQEFsbC1pbi1vbmUgUENzICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAWBB8BBQIyNh8CBQEwZAIDDw8WAh8GaGRkAgQPZBYEZg8VAikvaW1hZ2VzL3Byb2R1Y3RzLzQ5eDQ5LzQ5eDQ5ZHdfc2VydmVyLmpwZwBkAgEPFgIfBQIBFgJmD2QWBAIBDw8WAh8EBUBTZXJ2ZXIgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgFgQfAQUBNR8CBQEwZAIDDw8WAh8GaGRkAgUPZBYEZg8VAigvaW1hZ2VzL3Byb2R1Y3RzLzQ5eDQ5LzQ5eDQ5ZHdfbGNkdHYuanBnB0Rpc3BsYXlkAgEPFgIfBQICFgRmD2QWAgIBDw8WAh8EBUBMQ0QgTW9uaXRvcnMgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgFgQfAQUCMTMfAgUBMGQCAQ9kFgQCAQ8PFgIfBAVATENEIFRWICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIBYEHwEFAjE3HwIFATBkAgMPDxYCHwZoZGQCBg9kFgRmDxUCKy9pbWFnZXMvcHJvZHVjdHMvNDl4NDkvNDl4NDlkd19oYW5kaGVsZC5qcGcWSGFuZGhlbGQgJiBOYXZpZ2F0aW9uIGQCAQ8WAh8FAgMWBmYPZBYCAgEPDxYCHwQFQFBEQSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAWBB8BBQE2HwIFATBkAgEPZBYCAgEPDxYCHwQFQE1vYmlsZSBQaG9uZSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAWBB8BBQIxMh8CBQEwZAICD2QWBAIBDw8WAh8EBUBQTkQgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgFgQfAQUCMjMfAgUBMGQCAw8PFgIfBmhkZAIHD2QWBGYPFQImL2ltYWdlcy9wcm9kdWN0cy80OXg0OS80OXg0OWR3X2xhbi5qcGcITmV0d29ya3NkAgEPFgIfBQICFgRmD2QWAgIBDw8WAh8EBUBOZXR3b3JraW5nICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgFgQfAQUCMTAfAgUBMGQCAQ9kFgQCAQ8PFgIfBAVAV2lyZWxlc3MgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIBYEHwEFAjExHwIFATBkAgMPDxYCHwZoZGQCCA9kFgRmDxUCKS9pbWFnZXMvcHJvZHVjdHMvNDl4NDkvNDl4NDlkd193ZWJjYW0uanBnAGQCAQ8WAh8FAgEWAmYPZBYEAgEPDxYCHwQFQFBlcmlwaGVyYWxzICYgQWNjZXNzb3JpZXMgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAWBB8BBQIyNR8CBQEwZAIDDw8WAh8GaGRkAgkPZBYEZg8VAiovaW1hZ2VzL3Byb2R1Y3RzLzQ5eDQ5LzQ5eDQ5ZHdfZGVza3RvcC5qcGcGT3RoZXJzZAIBDxYCHwUCBhYMZg9kFgICAQ8PFgIfBAVATGFuL1NDU0kgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIBYEHwEFATQfAgUBMGQCAQ9kFgICAQ8PFgIfBAVAQnJvYWRiYW5kICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIBYEHwEFATcfAgUBMGQCAg9kFgICAQ8PFgIfBAVAUEMgQ29tcG9uZW50cyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIBYEHwEFAjE1HwIFATBkAgMPZBYCAgEPDxYCHwQFQERpZ2l0YWwgSG9tZSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAWBB8BBQIxOB8CBQEwZAIED2QWAgIBDw8WAh8EBUBIU0RQQSBDYXJkcyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgFgQfAQUCMjIfAgUBMGQCBQ9kFgQCAQ8PFgIfBAVAV2ViY2FtICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIBYEHwEFAjI0HwIFATBkAgMPDxYCHwZoZGQCBw9kFgJmD2QWAgIBD2QWBAIBDw8WAh8EBSVTZWxlY3QgYW4gaXRlbSBpbiB0aGUgcHJldmlvdXMgY29sdW1uZGQCAw8WAh8FAgoWFGYPZBYCAgEPDxYCHwQFBkVlZSBBUBYEHwEFAjIwHwIFATNkAgEPZBYCAgEPDxYCHwQFB0VlZSBCb3gWBB8BBQIyMB8CBQEyZAICD2QWAgIBDw8WAh8EBQxFZWUgS2V5Ym9hcmQWBB8BBQIyMB8CBQE4ZAIDD2QWAgIBDw8WAh8EBQhFZWUgTm90ZRYEHwEFAjIwHwIFAjEyZAIED2QWAgIBDw8WAh8EBQdFZWUgUGFkFgQfAQUCMjAfAgUCMTZkAgUPZBYCAgEPDxYCHwQFBkVlZSBQQxYEHwEFAjIwHwIFATFkAgYPZBYCAgEPDxYCHwQFCkVlZSBSZWFkZXIWBB8BBQIyMB8CBQIxM2QCBw9kFgICAQ8PFgIfBAUJRWVlIFN0aWNrFgQfAQUCMjAfAgUBNGQCCA9kFgICAQ8PFgIfBAUORWVlIFZpZGVvcGhvbmUWBB8BBQIyMB8CBQE2ZAIJD2QWAgIBDw8WAh8EBQZXaUNhc3QWBB8BBQIyMB8CBQIxNWQCCQ9kFgJmD2QWAgIBD2QWBAIBDw8WAh8EBSVTZWxlY3QgYW4gaXRlbSBpbiB0aGUgcHJldmlvdXMgY29sdW1uZGQCAw8WAh8FZmQCCw9kFgJmD2QWAgIBD2QWAgIBDw8WAh8EBSVTZWxlY3QgYW4gaXRlbSBpbiB0aGUgcHJldmlvdXMgY29sdW1uZGQCDQ9kFgJmD2QWAgIBD2QWDgIBDw8WAh8EBRRQbGVhc2Ugc2VsZWN0IFNlcmllc2RkAgMPDxYCHwQFQEVlZSBGYW1pbHkgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBkZAIFDw8WAh8EZWRkAgcPDxYCHwRlZGQCCQ8PFgIfBGVkZAILD2QWAgIBDw8WAh8EBQdDb25maXJtZGQCDQ8PFgIfBAUKU3RhcnQgT3ZlcmRkZAPQ7Rj49gqNaEbIn4axTBXDlBwT");
		post.put("__EVENTVALIDATION", "/wEWKgL08e8TApLCgIQMAs2s5LALAvSx7C8Cr5zQ3A8CnrvvgA8C2cC8ywMCsNLJ2wwC67ytiAwC2rOWmwUClbnj5QkCyfitjA0C7qWwhggC7sjVlw8C/7SRxA8C+LOT+wECierR8AgC+sHa5QECu9KBzAECu5ug4QkCyqO+pggCxYb+xQgCwdmZ8wcC+eOJ5wYCmLqasAgC68n8xAoC8Jyypw4C9bTaqQkC35Sr1QkCzsODwg0Cw4rWpgwCrJ/mhA0Cv8/NvAkCuoGIggkCvYvpmQQCsMzH1Q8Cs9ao7QoC/uynsgsChbDovQcCh+K97w0C6+HavggCsbzEO0vCEosz/PLJ4orvNFQyxC5E2TGW");
    }
}
