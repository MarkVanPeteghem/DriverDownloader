package driverdownloader;

import java.util.ArrayList;
import java.util.List;

public class ErrorHandler {

	private static List<Listener> listeners = new ArrayList<Listener>();
	
	static public interface Listener {
		public void addError(String err);
	}
	
	static public void addError(String err) {
		for (Listener listener: listeners) {
			listener.addError(err);
		}
	}

	static public void addError(Exception ex) {
		addError(ex.toString());
	}
	
	static public void add(Listener listener) {
		listeners.add(listener);
	}
}
