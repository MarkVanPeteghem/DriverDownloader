package driverdownloader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

public class ExceptionList {

	private static Set<String> files = new HashSet<String>();
	private static boolean fileNotFoundReported = false;
	
	static void load () {
		try {
			FileInputStream file = new FileInputStream("ExceptionList.txt");
		
	        BufferedReader reader = new BufferedReader(new InputStreamReader(file));
	        String line;
	        while ((line = reader.readLine())!=null) {
	        	line = line.trim();
	        	if (!line.isEmpty())
	        		files.add(line);
	        }
		} catch (Exception ex) {
			if (!fileNotFoundReported) {
				JOptionPane.showMessageDialog(null, ""+ex, "Error", JOptionPane.ERROR_MESSAGE);
				fileNotFoundReported = true;
			}
		}
	}
	
	static boolean has(String file) {
		return files.contains(file);
	}
}
