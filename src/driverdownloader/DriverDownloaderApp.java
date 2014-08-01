package driverdownloader;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JFrame;

public class DriverDownloaderApp extends JFrame {

    DriverDownloaderPanel driverDownLoaderPanel;

    public DriverDownloaderApp(boolean extendedView) {
    	if (extendedView)
    		setTitle("Driver Downloader");
    	else
    		setTitle("Driver Overview");
        setSize(300, 300);
        driverDownLoaderPanel = new DriverDownloaderPanel(extendedView);
        Component c = (Component)driverDownLoaderPanel;
        getContentPane().add(c, BorderLayout.CENTER);

        pack();
    }

    public static void main(String args[]) {
    	boolean extendedView = !(args.length>=1 && args[0].equals("-overview"));    		
        DriverDownloaderApp frame = new DriverDownloaderApp(extendedView);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
