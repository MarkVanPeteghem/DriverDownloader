package driverdownloader;

import java.util.Map;

public class PostableUrl {

	private String link;
	private Map<String, String> postData = null;
	
	public PostableUrl(String link, Map<String, String> postData) {
		this.link = link;
		this.postData = postData;
		normalize();
	}
	
	public PostableUrl(String link) {
		this.link = link;
		normalize();
	}

	public String getLink() {
		return link;
	}

	public Map<String, String> getPostData() {
		return postData;
	}
	
	private void normalize() {
        link = link.replaceAll("&amp;", "&");
        link = link.replaceAll(" ", "%20");
        link = link.replaceAll("\t", "");
	}
}
