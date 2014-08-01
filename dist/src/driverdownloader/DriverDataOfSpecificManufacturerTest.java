package driverdownloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class DriverDataOfSpecificManufacturerTest {
    public DriverDataOfSpecificManufacturerTest() {
    }

    String docType = "<?xml version='1.0' encoding='UTF-8' standalone='no'?>";
    String emptyData = docType+"<driver-data></driver-data>";
    String oneProductType = docType+"<driver-data>"+
            "<product-type name='laptop'></product-type>"+
            "</driver-data>";
    String twoProductTypes = docType+"<driver-data>"+
            "<product-type name='laptop'></product-type>"+
            "<product-type name='desktop'></product-type>"+
            "</driver-data>";
    String oneModelNumber = docType+"<driver-data>"+
            "<product-type name='laptop'>"+
                "<model-number name='123456' url='http://www.man1.com/123456' />"+
            "</product-type>"+
            "</driver-data>";
    String oneModelNumberNoUrl = docType+"<driver-data>"+
            "<product-type name='laptop'>"+
                "<model-number name='123456' />"+
            "</product-type>"+
            "</driver-data>";
    String twoModelNumbers = docType+"<driver-data>"+
            "<product-type name='laptop'>"+
                "<model-number name='123456' url='http://www.man1.com/123456'/>"+
                "<model-number name='987654' url='http://www.man1.com/987654'/>"+
            "</product-type>"+
            "</driver-data>";
    String twoProductTypesWithModels = docType+"<driver-data>"+
	    "<product-type name='laptop'>"+
	        "<model-number name='123456' url='http://www.man1.com/123456'/>"+
	        "<model-number name='987654' url='http://www.man1.com/987654'/>"+
	    "</product-type>"+
	    "<product-type name='desktop'>" +
	        "<model-number name='123457' url='http://www.man1.com/123457'/>"+
	        "<model-number name='987653' url='http://www.man1.com/987653'/>"+
	    "</product-type>"+
	    "</driver-data>";

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testReadData_getProductTypes_EmptyFile() throws Exception {
        InputStream inStream = new ByteArrayInputStream(emptyData.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        ArrayList<String> productTypes = instance.getProductTypes();
        assertEquals(0, productTypes.size());
    }

    @Test
    public void testReadData_getProductTypes_One() throws Exception {
        InputStream inStream = new ByteArrayInputStream(oneProductType.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        ArrayList<String> productTypes = instance.getProductTypes();
        assertEquals(1, productTypes.size());
        assertEquals("laptop", productTypes.get(0));
    }

    @Test
    public void testReadData_getProductTypes_Two() throws Exception {
        InputStream inStream = new ByteArrayInputStream(twoProductTypes.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        ArrayList<String> productTypes = instance.getProductTypes();
        assertEquals(2, productTypes.size());
        assertEquals("laptop", productTypes.get(0));
        assertEquals("desktop", productTypes.get(1));
    }

    @Test
    public void testReadData_oneProductType_Clear() throws Exception {
        InputStream inStream = new ByteArrayInputStream(oneProductType.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        instance.clear();
        ArrayList<String> productTypes = instance.getProductTypes();
        assertEquals(0, productTypes.size());
    }

    @Test
    public void testReadData_twoProductTypes_Clear() throws Exception {
        InputStream inStream = new ByteArrayInputStream(twoProductTypes.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        instance.clear();
        ArrayList<String> productTypes = instance.getProductTypes();
        assertEquals(0, productTypes.size());
    }

    @Test
    public void testReadData_emptyFile_AddProductType() throws Exception {
        InputStream inStream = new ByteArrayInputStream(emptyData.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);

        instance.addProductType("laptop");
        ArrayList<String> productTypes = instance.getProductTypes();
        assertEquals(1, productTypes.size());
        assertEquals("laptop", productTypes.get(0));
    }

    @Test
    public void testReadData_emptyFile_AddTwoProductTypes_Alphabetically() throws Exception {
        InputStream inStream = new ByteArrayInputStream(emptyData.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);

        instance.addProductType("laptop");
        instance.addProductType("pc");
        ArrayList<String> productTypes = instance.getProductTypes();
        assertEquals(2, productTypes.size());
        assertEquals("laptop", productTypes.get(0));
        assertEquals("pc", productTypes.get(1));
    }

    @Test
    public void testReadData_emptyFile_AddTwoProductTypes_NotAlphabetically() throws Exception {
        InputStream inStream = new ByteArrayInputStream(emptyData.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);

        instance.addProductType("pc");
        instance.addProductType("laptop");
        ArrayList<String> productTypes = instance.getProductTypes();
        assertEquals(2, productTypes.size());
        assertEquals("laptop", productTypes.get(0));
        assertEquals("pc", productTypes.get(1));
    }

    @Test
    public void testReadData_GetActiveProductTypes() throws Exception {
        InputStream inStream = new ByteArrayInputStream(twoProductTypes.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        String seriesModel = instance.getActiveProductType();
        assertEquals("laptop", seriesModel);
    }

    @Test
    public void testReadData_GetActiveProductTypes_Second() throws Exception {
        InputStream inStream = new ByteArrayInputStream(twoProductTypes.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        instance.selectProductType(1);
        String seriesModel = instance.getActiveProductType();
        assertEquals("desktop", seriesModel);
    }

    @Test
    public void testReadData_GetActiveProductTypes_Second_ByString() throws Exception {
        InputStream inStream = new ByteArrayInputStream(twoProductTypes.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        instance.selectProductType("desktop");
        String seriesModel = instance.getActiveProductType();
        assertEquals("desktop", seriesModel);
    }

    @Test
    public void testReadData_GetModelNumbers_EmptyFile() throws Exception {
        InputStream inStream = new ByteArrayInputStream(emptyData.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        ArrayList<String> modelNumbers = instance.getModelNumbers();
        assertEquals(0, modelNumbers.size());
    }

    @Test
    public void testReadData_GetModelNumbers_One() throws Exception {
        InputStream inStream = new ByteArrayInputStream(oneModelNumber.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        ArrayList<String> modelNumbers = instance.getModelNumbers();
        assertEquals(1, modelNumbers.size());
        assertEquals("123456", modelNumbers.get(0));
    }

    @Test
    public void testReadData_GetModelNumbers_Two() throws Exception {
        InputStream inStream = new ByteArrayInputStream(twoModelNumbers.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        ArrayList<String> modelNumbers = instance.getModelNumbers();
        assertEquals(2, modelNumbers.size());
        assertEquals("123456", modelNumbers.get(0));
        assertEquals("987654", modelNumbers.get(1));
    }

    @Test
    public void testReadData_GetModelNumbersOfAllCategories() throws Exception {
        InputStream inStream = new ByteArrayInputStream(twoProductTypesWithModels.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        instance.selectProductType("All");
        ArrayList<String> modelNumbers = instance.getModelNumbers();
        assertEquals(4, modelNumbers.size());
        assertEquals("123456", modelNumbers.get(0));
        assertEquals("987654", modelNumbers.get(1));
        assertEquals("123457", modelNumbers.get(2));
        assertEquals("987653", modelNumbers.get(3));
    }

    @Test
    public void testReadData_GetActiveModelNumber() throws Exception {
        InputStream inStream = new ByteArrayInputStream(twoModelNumbers.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        String modelNumber = instance.getModelNumberName(0);
        assertEquals("123456", modelNumber);
    }

    @Test
    public void testReadData_GetActiveModelNumber_Second() throws Exception {
        InputStream inStream = new ByteArrayInputStream(twoModelNumbers.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        String modelNumber = instance.getModelNumberName(1);
        assertEquals("987654", modelNumber);
    }

    @Test
    public void testReadData_GetActiveUrl_NoFileRead() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        String url = instance.getURL(0);
        assertEquals(null, url);
    }

    @Test
    public void testReadData_GetActiveUrl_NoModelNumber() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(emptyData.getBytes());
        instance.readData(inStream);
        String url = instance.getURL(0);
        assertEquals(null, url);
    }

    @Test
    public void testReadData_GetActiveUrl_NoUrl() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(oneModelNumberNoUrl.getBytes());
        instance.readData(inStream);
        String url = instance.getURL(0);
        assertEquals(null, url);
    }

    @Test
    public void testReadData_GetActiveUrl_Present() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(oneModelNumber.getBytes());
        instance.readData(inStream);
        String url = instance.getURL(0);
        assertEquals("http://www.man1.com/123456", url);
    }

    @Test
    public void testReadData_GetActiveUrl_Second() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(twoModelNumbers.getBytes());
        instance.readData(inStream);
        String url = instance.getURL(1);
        assertEquals("http://www.man1.com/987654", url);
    }

    @Test
    public void testReadDataAndSave() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(twoModelNumbers.getBytes());
        instance.readData(inStream);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        instance.saveData(outStream);
        String result = new String(outStream.toByteArray());
        result = result.replace('"', '\'');
        assertEquals(result, twoModelNumbers);
    }

    @Test
    public void testReadData_AddProductType_Before() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(oneProductType.getBytes());
        instance.readData(inStream);
        instance.addProductType("desktop");
        ArrayList<String> productTypes = instance.getProductTypes();
        assertEquals(2, productTypes.size());
        assertEquals("desktop", productTypes.get(0));
        assertEquals("laptop", productTypes.get(1));
    }

    @Test
    public void testReadData_AddProductType_After() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(oneProductType.getBytes());
        instance.readData(inStream);
        instance.addProductType("towers");
        ArrayList<String> productTypes = instance.getProductTypes();
        assertEquals(2, productTypes.size());
        assertEquals("laptop", productTypes.get(0));
        assertEquals("towers", productTypes.get(1));
    }

    @Test
    public void testReadData_AddModelNumber_Before() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(oneModelNumber.getBytes());
        instance.readData(inStream);
        instance.addModelNumber("100000", "http://www.man1.com/100000");
        ArrayList<String> modelNumbers = instance.getModelNumbers();
        assertEquals(2, modelNumbers.size());
        assertEquals("100000", modelNumbers.get(0));
        assertEquals("123456", modelNumbers.get(1));
    }

    @Test
    public void testReadData_AddModelNumber_After() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(oneModelNumber.getBytes());
        instance.readData(inStream);
        instance.addModelNumber("200000", "http://www.man1.com/200000");
        ArrayList<String> modelNumbers = instance.getModelNumbers();
        assertEquals(2, modelNumbers.size());
        assertEquals("123456", modelNumbers.get(0));
        assertEquals("200000", modelNumbers.get(1));
    }

    @Test
    public void testReadData_AddExistingModelNumber() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(oneModelNumber.getBytes());
        instance.readData(inStream);
        instance.addModelNumber("123456", "http://www.man1.com/123456");
        ArrayList<String> modelNumbers = instance.getModelNumbers();
        assertEquals(1, modelNumbers.size());
        assertEquals("123456", modelNumbers.get(0));
    }

    @Test
    public void testReadData_AddExistingModelNumber_First() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(twoModelNumbers.getBytes());
        instance.readData(inStream);
        instance.addModelNumber("123456", "http://www.man1.com/123456");
        ArrayList<String> modelNumbers = instance.getModelNumbers();
        assertEquals(2, modelNumbers.size());
        assertEquals("123456", modelNumbers.get(0));
        assertEquals("987654", modelNumbers.get(1));
    }

    @Test
    public void testReadData_AddExistingModelNumber_Second() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(twoModelNumbers.getBytes());
        instance.readData(inStream);
        instance.addModelNumber("987654", "http://www.man1.com/987654");
        ArrayList<String> modelNumbers = instance.getModelNumbers();
        assertEquals(2, modelNumbers.size());
        assertEquals("123456", modelNumbers.get(0));
        assertEquals("987654", modelNumbers.get(1));
    }

    @Test
    public void testReadData_AddExistingModelNumber_First_DifferentUrl() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(twoModelNumbers.getBytes());
        instance.readData(inStream);
        instance.addModelNumber("123456", "http://www.man1.com/123456b");
        ArrayList<String> modelNumbers = instance.getModelNumbers();
        assertEquals(3, modelNumbers.size());
        assertEquals("123456", modelNumbers.get(0));
        assertEquals("123456", modelNumbers.get(1));
        assertEquals("987654", modelNumbers.get(2));
    }

    @Test
    public void testReadData_AddExistingModelNumber_Second_DifferentUrl() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(twoModelNumbers.getBytes());
        instance.readData(inStream);
        instance.addModelNumber("987654", "http://www.man1.com/987654b");
        ArrayList<String> modelNumbers = instance.getModelNumbers();
        assertEquals(3, modelNumbers.size());
        assertEquals("123456", modelNumbers.get(0));
        assertEquals("987654", modelNumbers.get(1));
        assertEquals("987654", modelNumbers.get(2));
    }

    @Test
    public void testReadData_ClearProductTypeLaptop() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(twoProductTypesWithModels.getBytes());
        instance.readData(inStream);

        instance.clearProductType("laptop");
        
        instance.selectProductType("laptop");
        ArrayList<String> modelNumbers = instance.getModelNumbers();
        assertEquals(0, modelNumbers.size());

        instance.selectProductType("desktop");
        modelNumbers = instance.getModelNumbers();
        assertEquals(2, modelNumbers.size());
    }

    @Test
    public void testReadData_ClearProductTypeDesktop() throws Exception {
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        InputStream inStream = new ByteArrayInputStream(twoProductTypesWithModels.getBytes());
        instance.readData(inStream);

        instance.clearProductType("desktop");
        
        instance.selectProductType("laptop");
        ArrayList<String> modelNumbers = instance.getModelNumbers();
        assertEquals(2, modelNumbers.size());

        instance.selectProductType("desktop");
        modelNumbers = instance.getModelNumbers();
        assertEquals(0, modelNumbers.size());
    }

    @Test
    public void testReadData_IterateCategories_None() throws Exception {
        InputStream inStream = new ByteArrayInputStream(emptyData.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        
        ArrayList<String> productTypes = new ArrayList<String>();
        for (String category: instance.categories()) {
        	productTypes.add(category);
        }
        
        assertEquals(0, productTypes.size());
    }

    @Test
    public void testReadData_IterateCategories_One() throws Exception {
        InputStream inStream = new ByteArrayInputStream(oneProductType.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        
        ArrayList<String> productTypes = new ArrayList<String>();
        for (String category: instance.categories()) {
        	productTypes.add(category);
        }
        
        assertEquals(1, productTypes.size());
        assertEquals("laptop", productTypes.get(0));
    }

    @Test
    public void testReadData_IterateCategories_Two() throws Exception {
        InputStream inStream = new ByteArrayInputStream(twoProductTypes.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        
        ArrayList<String> productTypes = new ArrayList<String>();
        for (String category: instance.categories()) {
        	productTypes.add(category);
        }
        
        assertEquals(2, productTypes.size());
        assertEquals("laptop", productTypes.get(0));
        assertEquals("desktop", productTypes.get(1));
    }

    @Test
    public void testReadData_IterateModels() throws Exception {
        InputStream inStream = new ByteArrayInputStream(twoModelNumbers.getBytes());
        DriverDataOfSpecificManufacturer instance = new DriverDataOfSpecificManufacturer();
        instance.readData(inStream);
        
        ArrayList<ModelAndUrl> models = new ArrayList<ModelAndUrl>();
        for (ModelAndUrl model: instance.models("laptop")) {
        	models.add(model);
        }
        
        assertEquals(2, models.size());
        assertEquals("123456", models.get(0).getModel());
        assertEquals("987654", models.get(1).getModel());
        assertEquals("http://www.man1.com/123456", models.get(0).getUrl());
        assertEquals("http://www.man1.com/987654", models.get(1).getUrl());
    }
}
