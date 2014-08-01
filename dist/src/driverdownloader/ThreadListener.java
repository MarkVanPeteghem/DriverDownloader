package driverdownloader;

public interface ThreadListener {
	public void started(); 
	public void stopped();
	public void reportMessage(String msg, String title, int type);
	public void addMessage(String msg);
}
