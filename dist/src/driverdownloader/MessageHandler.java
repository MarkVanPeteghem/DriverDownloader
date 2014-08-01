package driverdownloader;

import java.util.ArrayList;
import java.util.List;

public class MessageHandler {

	private static List<Listener> listeners = new ArrayList<Listener>();
	
	static public interface Listener {
		public void addMessage(String err);
		public void addError(String err);
	}
	
	static public void addMessage(String str) {
		for (Listener listener: listeners) {
			listener.addMessage(str);
		}
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
