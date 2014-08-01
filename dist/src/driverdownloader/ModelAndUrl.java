package driverdownloader;

import org.w3c.dom.Element;

public class ModelAndUrl {

	private String model;
	private String url;
	private Element element;
	
	public ModelAndUrl(String model, String url, Element element) {
		this.model = model;
		this.url = url;
		this.element = element;
	}

	public String getModel() {
		return model;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String newUrl) {
		element.setAttribute("url", newUrl);
	}
}
