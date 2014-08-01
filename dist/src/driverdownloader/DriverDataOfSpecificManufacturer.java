package driverdownloader;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.*;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import org.xml.sax.SAXException;

/**
 * This class stores the information of the urls where the main download page
 * for a type of computer or device can be found. It does this in a hierarchical
 * way: manufacturer -> product type -> model number. It has a notion of an active
 * manufacturer and an active product type, which makes it
 * easier for the UI to request the available options in the hierarchy below. It
 * has functionality to load and save these data in a XML file.
 * 
 * @author Mark Van Peteghem
 */

public class DriverDataOfSpecificManufacturer {

	// for simplicity we keep the data in the DOM tree
	Document doc;
	Element activeProductType;
	String activeURL;

	/**
	 * Reads the data from a XML stream
	 * 
	 * @param in
	 *            stream that provides the XML data
	 * @throws javax.xml.parsers.ParserConfigurationException
	 * @throws org.xml.sax.SAXException
	 * @throws java.io.IOException
	 */
	public void readData(String filename)
			throws javax.xml.parsers.ParserConfigurationException,
			org.xml.sax.SAXException, java.io.IOException {
		boolean ok = false;
		try {
			FileInputStream file = new FileInputStream(filename);
			readData(file);
			ok = true;
		} finally {
			if (!ok)
				makeEmptyRoot();
		}
	}

	/**
	 * Reads the data from a XML stream
	 * 
	 * @param in
	 *            stream that provides the XML data
	 * @throws javax.xml.parsers.ParserConfigurationException
	 * @throws org.xml.sax.SAXException
	 * @throws java.io.IOException
	 */
	public void readData(InputStream in)
			throws javax.xml.parsers.ParserConfigurationException,
			org.xml.sax.SAXException, java.io.IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		doc = builder.parse(in);

		Element root = doc.getDocumentElement();
		NodeList productTypes = root.getElementsByTagName("product-type");
		for (int i = 0; i < productTypes.getLength(); ++i) {
			Node child = productTypes.item(i);
			if (child instanceof Element) {
				activeProductType = (Element) child;
				break;
			}
		}
	}

	private void makeEmptyRoot() throws ParserConfigurationException,
			SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		String docType = "<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n";
		String emptyData = docType + "\n<driver-data>\n</driver-data>";
		InputStream in = new ByteArrayInputStream(emptyData.getBytes());
		doc = builder.parse(in);
	}

	/**
	 * Stores the data in a XML file
	 * 
	 * @param out
	 * @throws javax.xml.transform.TransformerConfigurationException
	 * @throws javax.xml.transform.TransformerException
	 */
	public void saveData(String filename)
			throws javax.xml.transform.TransformerConfigurationException,
			javax.xml.transform.TransformerException, FileNotFoundException {
		FileOutputStream file = new FileOutputStream(filename);
		saveData(file);
	}

	/**
	 * Stores the data in a XML file
	 * 
	 * @param out
	 * @throws javax.xml.transform.TransformerConfigurationException
	 * @throws javax.xml.transform.TransformerException
	 */
	public void saveData(OutputStream out)
			throws javax.xml.transform.TransformerConfigurationException,
			javax.xml.transform.TransformerException {
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.transform(new DOMSource(doc), new StreamResult(out));
	}

	public void clear() {
		if (null == doc) {
			try {
				makeEmptyRoot();
			} catch (Exception ex) {
				// exception in makeEmptyRoot() is unlikely
			}
		} else {
			Element root = doc.getDocumentElement();
			while (true) {
				Node node = root.getFirstChild();
				if (null == node)
					break;
				root.removeChild(node);
			}
			doc.normalize();
		}
	}

	/**
	 * Select a different product type
	 * 
	 * @param idx
	 *            the index of the product type, zero-based
	 */
	public void selectProductType(int idx) {
		if (idx>=0) {
			Element root = doc.getDocumentElement();
			NodeList productTypes = root.getElementsByTagName("product-type");
			if (idx >= 0 && idx < productTypes.getLength()) {
				Node productType = productTypes.item(idx);
				if (productType instanceof Element) {
					activeProductType = (Element) productType;
				}
			}
		} else {
			activeProductType = null;
		}
	}

	/**
	 * Select a different product type
	 * 
	 * @param str
	 *            the name of the product type
	 * @throws Exception
	 */
	public void selectProductType(String str) throws Exception {
		if (str.equalsIgnoreCase("all")) {
			activeProductType = null;
		} else {
			Element root = doc.getDocumentElement();
			NodeList productTypes = root.getElementsByTagName("product-type");
			boolean found = false;
			for (int idx = 0; idx < productTypes.getLength(); ++idx) {
				Element elem = (Element) productTypes.item(idx);
				String elemName = elem.getAttribute("name");
				if (elemName.equals(str)) {
					activeProductType = elem;
					found = true;
					break;
				}
			}
			if (!found)
				throw new Exception("product type " + str + "was not found");
		}
	}

	/**
	 * Get a product type
	 * 
	 * @param str
	 *            the name of the product type
	 * @throws Exception
	 */
	public Element getProductType(String str) throws Exception {
		Element root = doc.getDocumentElement();
		NodeList productTypes = root.getElementsByTagName("product-type");
		for (int idx = 0; idx < productTypes.getLength(); ++idx) {
			Element elem = (Element) productTypes.item(idx);
			if (elem.getAttribute("name").equals(str)) {
				return elem;
			}
		}
		throw new Exception("product type " + str + "was not found");
	}

	/**
	 * Select a different model number
	 * It's possible that this method is no longer used
	 * 
	 * @param idx
	 *            the index of the model number, zero-based
	 */
	private Element getModelNumber(int idx) {
		Element productType = activeProductType;
		
		if (null == activeProductType) {
			// we have to search through all categories
			Element root = doc.getDocumentElement();
			NodeList productTypes = root.getElementsByTagName("product-type");
			for (int p= 0; p< productTypes.getLength(); ++p) {
				Element elem = (Element) productTypes.item(p);
				int count = getCount(elem, "model-number");
				if (count>idx) {
					productType = elem;
					break;
				} else {
					idx -= count;
				}
			}			
		}

		NodeList modelNumbers = productType.getElementsByTagName("model-number");
		if (idx >= 0 && idx < modelNumbers.getLength()) {
			Node modelNumber = modelNumbers.item(idx);
			if (modelNumber instanceof Element) {
				return (Element) modelNumber;
			}
		}
		return null;
	}

	/**
	 * Get a model
	 * 
	 * @param str
	 *            the name of the model
	 * @throws Exception
	 */
	private Element getModel(String str) throws Exception {
		if (null == activeProductType) {
			// we have to search through all categories
			Element root = doc.getDocumentElement();
			NodeList productTypes = root.getElementsByTagName("product-type");
			for (int p= 0; p< productTypes.getLength(); ++p) {
				Element prodTypeElem = (Element) productTypes.item(p);
				Element model = getModel(prodTypeElem, str);
				if (null!=model)
					return model;
			}			
			throw new Exception("model number " + str + "was not found");
		} else {
			// we only have to search in the active category
			Element elem = getModel(activeProductType, str);
			if (null==elem)
				throw new Exception("model number " + str + "was not found");
			return elem;
		}
	}

	private Element getModel(Element models, String str) throws Exception {
		NodeList modelNumbers = models.getElementsByTagName("model-number");
		for (int idx = 0; idx < modelNumbers.getLength(); ++idx) {
			Element elem = (Element) modelNumbers.item(idx);
			if (elem.getAttribute("name").equals(str)) {
				return elem;
			}
		}
		return null;
	}

	public void addProductType(String newName) {
		Element root = doc.getDocumentElement();
		NodeList productTypes = root.getElementsByTagName("product-type");
		Element newNode = doc.createElement("product-type");
		newNode.setAttribute("name", newName);
		Text textNode1 = doc.createTextNode("\n");
		newNode.appendChild(textNode1);

		activeProductType = (Element) newNode;

		boolean added = false;

		// search where to put it to keep it alphabetically
		for (int j = 0; j < productTypes.getLength(); ++j) {
			Node productType = productTypes.item(j);
			if (productType instanceof Element) {
				Element elem = (Element) productType;
				String name = elem.getAttribute("name");
				if (newName.equalsIgnoreCase(name)) {
					added = true;
					break;
                                }
				if (newName.compareToIgnoreCase(name) < 0) {
					root.insertBefore(newNode, productType);
					root.insertBefore(doc.createTextNode("\n"), productType);
					added = true;
					break;
				}
			}
		}
		if (!added) {
			root.appendChild(newNode);
			root.appendChild(doc.createTextNode("\n"));
		}
	}

	public void addModelNumber(String newName, String url) {
		NodeList modelNumbers = activeProductType
				.getElementsByTagName("model-number");
		Element newNode = doc.createElement("model-number");
		newNode.setAttribute("name", newName);
		newNode.setAttribute("url", url);
		Text textNode = doc.createTextNode("\n");

		// search where to put it to keep it alphabetically
		boolean added = false;
		for (int j = 0; j < modelNumbers.getLength(); ++j) {
			Node modelNumber = modelNumbers.item(j);
			if (modelNumber instanceof Element) {
				Element elem = (Element) modelNumber;
				String name = elem.getAttribute("name");
				if (newName.equals(name)) {
					String nodeUrl = elem.getAttribute("url");
					if (nodeUrl.equals(url)) {
						added = true;
						break;
					}
				}
				if (newName.compareToIgnoreCase(name) < 0) {
					activeProductType.insertBefore(newNode, modelNumber);
					activeProductType.insertBefore(textNode, modelNumber);
					added = true;
					break;
				}
			}
		}
		if (!added) {
			activeProductType.appendChild(newNode);
			activeProductType.appendChild(textNode);
		}
	}

	public String getActiveProductType() {

		return null==activeProductType ? "All" : getNameAttribute(activeProductType);
	}

	public String getModelNumberName(int idx) {

		return getNameAttribute(getModelNumber(idx));
	}

	private String getNameAttribute(Element element) {
		if (element == null) {
			return null;
		}
		if (!element.hasAttribute("name")) {
			return null;
		}
		return element.getAttribute("name");
	}

	public String getURL(int idx) {
		Element model = getModelNumber(idx);

		if (!model.hasAttribute("url")) {
			return null;
		}

		return model.getAttribute("url");
	}

	public String getURL(String name) throws Exception {
		Element model = getModel(name);

		if (!model.hasAttribute("url")) {
			return null;
		}

		return model.getAttribute("url");
	}

	public ArrayList<String> getProductTypes() {
		ArrayList<String> productTypes = new ArrayList<String>();
		productTypes.add("All");

		if (null != doc) {
			Element root = doc.getDocumentElement();
			if (root != null) {
				getNames(root, "product-type", productTypes);
			}
		}

		return productTypes;
	}

	public ArrayList<String> getModelNumbers() {
		ArrayList<String> models = new ArrayList<String>();

		if (activeProductType != null) {
			getNames(activeProductType, "model-number", models);
		} else {
			Element root = doc.getDocumentElement();
			NodeList productTypes = root.getElementsByTagName("product-type");
			for (int p=0; p<productTypes.getLength(); ++p) {
				Element productType = (Element)productTypes.item(p);
				getNames(productType, "model-number", models);
			}
		}

		return models;
	}

	public int getNrModelNumbers() {
		if (activeProductType != null) {
			return getCount(activeProductType, "model-number");
		} else {
			Element root = doc.getDocumentElement();
			NodeList productTypes = root.getElementsByTagName("product-type");
			int count = 0;
			for (int p=0; p<productTypes.getLength(); ++p) {
				Element productType = (Element)productTypes.item(0);
				count += getCount(productType, "model-number");
			}
			return count;
        }
	}

	private void getNames(Element element, String type,
			ArrayList<String> namesList) {
		NodeList children = element.getElementsByTagName(type);
		for (int j = 0; j < children.getLength(); ++j) {
			Node node = children.item(j);
			if (node instanceof Element) {
				Element elem = (Element) node;
				if (elem.hasAttribute("name")) {
					namesList.add(elem.getAttribute("name"));
				}
			}
		}
	}

	private int getCount(Element element, String type) {
            int count = 0;
		NodeList children = element.getElementsByTagName(type);
		for (int j = 0; j < children.getLength(); ++j) {
			Node node = children.item(j);
			if (node instanceof Element) {
				Element elem = (Element) node;
				if (elem.hasAttribute("name")) {
					++count;
				}
			}
		}
                return count;
	}

	public void clearProductType(String name) throws Exception {
		if (name.equalsIgnoreCase("all")) {
			Element root = doc.getDocumentElement();
			NodeList productTypes = root.getElementsByTagName("product-type");
			for (int p=0; p<productTypes.getLength(); ++p) {
				Element productType = (Element)productTypes.item(0);
				clearProductType(productType);
			}
		} else {
			selectProductType(name);
			clearProductType(activeProductType);
		}
		doc.normalize();
	}

	private void clearProductType(Element productType) throws DOMException {
		while (true) {
			Node node = productType.getFirstChild();
			if (null == node)
				break;
			productType.removeChild(node);
		}
	}

	private class CategoryIterator implements java.util.Iterator<String> {

		private NodeList nodes;
		private int index = 0;
		
		public CategoryIterator() {
			Element root = doc.getDocumentElement();
			nodes = root.getElementsByTagName("product-type");
		}
		
		@Override
		public boolean hasNext() {
			return index!=nodes.getLength();
		}

		@Override
		public String next() {
			Node node = nodes.item(index++);
			return ((Element)node).getAttribute("name");
		}

		@Override
		public void remove() {
			
		}
		
	}
	
	public Iterable<String> categories() {
		return new Iterable<String>() {

			@Override
			public Iterator<String> iterator() {
				return new CategoryIterator();
			}
		};
	}

	private class ModelIterator implements java.util.Iterator<ModelAndUrl> {

		private NodeList nodes;
		private int index = 0;
		
		public ModelIterator(String category) throws Exception {
			Element root = getProductType(category);
			nodes = root.getElementsByTagName("model-number");
		}
		
		@Override
		public boolean hasNext() {
			return index!=nodes.getLength();
		}

		@Override
		public ModelAndUrl next() {
			Node node = nodes.item(index++);
			Element elem = (Element)node;
			return new ModelAndUrl(elem.getAttribute("name"), elem.getAttribute("url"), elem);
		}

		@Override
		public void remove() {
			
		}
		
	}
	
	public Iterable<ModelAndUrl> models(final String category) {
		return new Iterable<ModelAndUrl>() {

			@Override
			public Iterator<ModelAndUrl> iterator() {
				try {
					return new ModelIterator(category);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		};
	}
}