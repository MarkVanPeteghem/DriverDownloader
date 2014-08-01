package driverdownloader;

import java.io.FileNotFoundException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to download drivers from HP
 * Note: this site is straightforward (no AJAX or so), but inconsistent,
 *       which makes the code to scrape the website ugly.
 * @author Mark Van Peteghem
 *
 */
public class HPDriverDownloader extends DriverDownloader {
	private final String mainPageAddress = "http://welcome.hp.com/country/us/en/support_task.html";

	int modelsAdded = 0;

	@Override
	public void download(PostableUrl url, String path) {
		try {
			DownloadedPage page = download.getPage(url);
            if (Interrupt.get()) {
                return;
            }
			String content = page.getContent();
	
			if (content.contains("Downloads for this product are not available through this website")
					|| content.contains("Select your product"))
				return;
	
			String link = page.getUrl();
			int lastPos = ParseUtils.safeLastIndexOf(link, "/", true);
			String baseLink = link.substring(0, lastPos + 1);
	
			if (content.contains("Select your operating system")) {
				String OsSection = ParseUtils.findSubstringByHeaderString(content,
						"Select your operating system", "<select", "</select>",
						false);
				List<String> values = ParseUtils.getSubStrings(OsSection, "value=\"", "\"", false);
				for (String value : values) {
					if (!value.isEmpty() && !value.equals("-1")) {
						downloadForOs(link + "&os=" + value, path);
					}
					if (Interrupt.get())
						return;
				}
			} else if (content.contains("Select operating system")) {
				String linksSection = ParseUtils.findSubstringByHeaderString(
						content, "Select operating system", "</table>", false);
				List<String> links = ParseUtils.getSubStrings(linksSection, "href=\"", "\"", false);
				for (String newLink: links) {
					downloadForOs(baseLink + newLink, path);
					if (Interrupt.get())
						return;
				}
			}
			// if the page doesn't contain any of these strings, there are no drivers for the model
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
	}

	private void downloadForOs(String url, String path) {
		try {
			DownloadedPage osPage = download.getPage(new PostableUrl(url));
			if (Interrupt.get())
				return;
	
			// get baselink
			url = osPage.getUrl();
			URI baseURI = new URI(url);
			int lastPos = ParseUtils.safeLastIndexOf(url, "/", true);
			String baseLink = url.substring(0, lastPos + 1);
	
			// process content of webpage
			String osContent = osPage.getContent();
			String linksSection;
			if (osContent.contains("Select a download")
					&& osContent.contains("troubleshooting help"))
				linksSection = ParseUtils.findSubstringByHeaderString(osContent,
						"Select a download", "troubleshooting help", false);
			else if (osContent.contains("Select a download")
					&& osContent.contains("1.Which option did you select?"))
				linksSection = ParseUtils.findSubstringByHeaderString(osContent,
						"Select a download", "1.Which option did you select?",
						false);
			else if (osContent.contains("Select a download")
					&& osContent.contains(">Your product<"))
				linksSection = ParseUtils.findSubstringByHeaderString(osContent,
						"Select a download", ">Your product<",
						false);
			else
				linksSection = ParseUtils.findSubstringByHeaderString(osContent,
						">Description<", "Did not find what you're looking for",
						false);
			List<String> links = ParseUtils.getSubStrings(linksSection, "href=\"", "\"", false);
			String prevLink = "";
			for (String link : links) {
				if (!link.contains("/document") && !link.startsWith("#")
						//&& !link.contains("/softwareDownloadIndex?")
						&& !link.contains("/go/ssm")
						&& !link.startsWith("SoftwareDescription.jsp")
						&& !link.contains("previousVersions?")
						&& !link.startsWith("javascript")
						&& !link.equals(prevLink)) {
	
					URI linkUri = baseURI.resolve(link);
					downloadFile(linkUri.toString(), path);
					prevLink = link;
					if (Interrupt.get())
						return;
				}
			}
			List<String> actions = ParseUtils.getSubStrings(linksSection, "action=\"", "\"", false);
			for (String action : actions) {
				if (!action.contains("yesno/servlet/YesNo") && !action.contains("ewfrf/YesNo")) {
					String target = ParseUtils.findSubstringByHeaderString(action,
							"&targetPage=", "&", false);
					target = target.replaceAll("%3A", ":").replaceAll("%2F", "/");
					download.downloadFile(target, path);
					if (Interrupt.get())
						return;
				}
			}
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
	}
	
	private void downloadFile(String link, String path) throws Exception {
		DownloadedPage filePage = download.getPage(new PostableUrl(link));
		if (Interrupt.get())
			return;
		
		String content = filePage.getContent();
		
		if (content.contains("Invalid product.") // this can happen due to error on HP site
				|| content.contains("Please select the destination country where the product is to be shipped")) // CD to be ordered by mail
			return; 
		
		// Normally the next three parts give the same link.
		// For robustness against future changes we try all three.
		try {
			String part1 = ParseUtils.findSubstringByFooterString(content,
					"<a", "primButtonEnhanced", false);
			String link1 = ParseUtils.findSubstringByHeaderString(part1,
					"href=\"", "\"", false);
			download.downloadFile(link1, path);
			
			return;
		} catch (Exception e) {
			// ignore exception, try other method below
		}
		
		try {
			String link2 = ParseUtils.findSubstringByHeaderString(content, "location.href='", "'", false);
			download.downloadFile(link2, path);

			return;
		} catch (Exception e) {
			// ignore exception, try other method below
		}

		String part3 = ParseUtils.findSubstringByHeaderString(content,
				">Details and specifications<", ">Version:", false);
		String link3 = ParseUtils.findSubstringByHeaderString(part3,
				"href=\"", "\"", false);
		download.downloadFile(link3, path);
	}

	@Override
	public String getName() {
		return "HP";
	}

	@Override
	public String getDataFilename() {
		return "hp.xml";
	}

	@Override
	protected Map<String, PostableUrl> getCategories() throws Exception {

		Map<String, PostableUrl> categories = new HashMap<String, PostableUrl>();

		DownloadedPage page = download.getPage(new PostableUrl(mainPageAddress));
		String content = page.getContent();
		int startPos = ParseUtils.safeIndexOf(content, "product category", 0,
				false);
		int endPos = ParseUtils.safeIndexOf(content, "</table>", startPos,
				false);
		endPos = ParseUtils.safeIndexOf(content, "</table>", endPos + 5, false);
		String section = content.substring(startPos, endPos);
		List<String> linkParts = ParseUtils.getSubStrings(section, "<noscript>", "</noscript>", false);
		for (String linkPart : linkParts) {
			String link = ParseUtils.findSubstringByHeaderString(linkPart,
					"href=\"", "\"", false);
			String productType = ParseUtils.findSubstringByHeaderString(
					linkPart, "\">", "</a>", false);

			productType = productType.replaceAll("&amp;", "&");
			categories.put(productType, new PostableUrl(link));
			if (Interrupt.get())
				break;
		}
		return categories;
	}

	@Override
	protected void updateCategory(DownloadedPage page) throws Exception {
		String content = page.getContent();
		String link = page.getUrl();
		
		if (link.contains("/Error?"))
			return;

		int pos = link.indexOf("/", 8);
		String baseLink = page.getUrl().substring(0, pos);
		if (Interrupt.get())
			return;

		if (content.contains("Select your product")
				&& !content.contains("Select your product</option>")) {
			// It has sub categories

			String linkSection = ParseUtils.findSubstringByHeaderString(
					content, "Select your product", "Search products", false);
			List<String> linkParts = ParseUtils.getSubStrings(linkSection, "<a href", "</a>", false);

			for (String linkPart : linkParts) {
				String newLink = ParseUtils.findSubstringByHeaderString(
						linkPart, "=\"", "\">", false);
				DownloadedPage newPage;
				try {
					newPage = download.getPage(new PostableUrl(baseLink + newLink));
				} catch (FileNotFoundException ex) {
					System.err.println("Ignoring file: "+ex);
					continue;
				}
				updateCategory(newPage);
			}
		} else {
			// It is the page of a specific product
			String modelName;
			String newLink;
			if (content.contains("Downloads for this product are not available through this website")) {
				modelName = ParseUtils.findSubstringByHeaderString(content,
						"var page_english_prod_series_name=\"", "\"", false);
				newLink = link;
			} else if (content.contains("Software & Driver Downloads")) {
				modelName = ParseUtils.findSubstringByHeaderString(content,
						"<h1>", "</h1>", false);
				newLink = baseLink
						+ ParseUtils.findSubstringByFooterString(content,
								"href=\"",
								"\">Software & Driver Downloads</a>", false);
			} else if (content.contains("System maintenance")) {
				modelName = ParseUtils.findSubstringByHeaderString(content,
						"selected >", "</option>", false);
				newLink = ParseUtils.findSubstringByFooterString(content,
						"href=\"", "\">System maintenance", false);
			} else if (content.contains("Select operating system")) {
				// the present page is the page that we need
				modelName = ParseUtils.findSubstringByHeaderString(content,
						"var page_english_prod_series_name=\"", "\"", false);
				newLink = link;
			} else if (content.contains("Download drivers and software")) {
				modelName = ParseUtils.findSubstringByHeaderString(content,
						"var page_english_prod_series_name=\"", "\"", false);
				newLink = baseLink
						+ ParseUtils.findSubstringByFooterString(content,
								"href=\"",
								"\">Download drivers and software</a>", false);
			} else {
				modelName = ParseUtils.findSubstringByHeaderString(content,
						"<h1>", "</h1>", false);
				if (modelName.contains("<"))
					modelName = ParseUtils.findSubstringByHeaderString(content,
							"<h1>", "<h1>", "</h1>", false);
				newLink = link;
			}
			
			if (newLink.contains("&amp;"))
				newLink = newLink.replaceAll("&amp;", "&");
			
			if (newLink.contains("/ProductList.jsp?")) {
				// the current page looks like a page for a single product,
				// but the 'Drivers & Downloads' link takes us to a page where
				// we can select a product
				DownloadedPage newPage = download.getPage(new PostableUrl(newLink));
				updateCategory(newPage);
			} else if (!modelName.equalsIgnoreCase("IT Resource Center") && !modelName.equalsIgnoreCase("Networking")) {
				driverData.addModelNumber(modelName, newLink);
				++modelsAdded;
				System.out.println(modelsAdded+" models added");
				if (modelsAdded % 1000 == 0) {
					saveData();
				}
			}
		}
	}

	@Override
	public void correct() {
		/*FileInputStream file = new FileInputStream(getDataFilename());
		BufferedReader bf = new BufferedReader(new InputStreamReader(file));
		
		FileOutputStream fileout = new FileOutputStream("hpc.xml");
		OutputStreamWriter out = new OutputStreamWriter(fileout);
		
		String prevName = "";
		String line;
		String prevLine = "";
		int lineNr = 0;
		while ((line = bf.readLine()) != null) {
			if (line.contains("name=")) {

				String name = ParseUtils.findSubstringByHeaderString(line,
						"name=\"", "\"", false);
				if (name.equals(prevName)) {
					System.out.println("Duplicate name at line " + lineNr
							+ ": " + name);
				}
				prevName = name;
			}
			if (line.contains("url=")) {
				String url = ParseUtils.findSubstringByHeaderString(line,
						"url=\"", "\"", false);
				
			}
			if (!line.equals(prevLine)) {
				out.write(line.replaceAll("&amp;amp;", "&amp;")+"\n");
			}
			prevLine = line;
			
			++lineNr;
		}
		out.flush();
		out.close();
		fileout.flush();
		fileout.close();*/
		/*for (String category: driverData.categories()) {
			System.out.println(category);
			int count = 0, total = 0;
			for (ModelAndUrl model: driverData.models(category)) {
				if (model.getUrl().contains("/ProductList.jsp?")) {
					//System.out.println(model.getModel());
					++count;
				}
				++total;
			}
			System.out.println("Nr to correct: "+count+"/"+total);
		}*/
		/*try {
	    	Map<String, String> categories = getCategories();
	    	String strCat = categories.toString();
	    	String updateCat[] = new String [] { "Digital Entertainment & Audio", "Handheld & Calculators", "Monitors", "Networking",
	    			"Options & Accessories" };
	    	for (String category: updateCat) {
				driverData.clearProductType(category);
				String url = categories.get(category);
				if (null==url)
					System.out.println("Category "+category+" not found");
				else
					updateCategory(url);
	    	}
		} catch (Exception ex) {
			
		}*/
	}
}
