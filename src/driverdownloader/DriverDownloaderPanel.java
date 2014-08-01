package driverdownloader;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.text.JTextComponent;


import net.miginfocom.swing.MigLayout;

/**
 * This class displays the main user interface
 * @author Mark Van Peteghem
 *
 */
public class DriverDownloaderPanel extends JPanel {

    /** Creates new form DriverDownloaderPanel */
    public DriverDownloaderPanel(boolean extendedView) {
        initComponents(extendedView);

        switchToDownloadState(false);

        javax.swing.table.TableColumnModel columns = selectedDownloadsTable.getColumnModel();
        columns.getColumn(1).setMaxWidth(80);
        columns.getColumn(1).setMinWidth(80);
        columns.getColumn(2).setMaxWidth(40);
        columns.getColumn(2).setMinWidth(40);
        columns.getColumn(3).setMaxWidth(40);
        columns.getColumn(3).setMinWidth(40);

        loadData();
        
        updateManufacturers();
        updateProductTypes();
        updateModelNumbers();

        categoryComboBox.setRenderer(new MyCellRenderer());

        MessageHandler.add(new MessageHandler.Listener() {

            public void addMessage(String str) {
                threadSafeAddMessage(str);
            }
            public void addError(String err) {
                threadSafeAddMessage(err);
            }
        });

        DownloadObservable.get().addListener(driverDownloaderListener);
    }

    /**
     * Cell renderer for the list box with the available models.
     * We need this to do the conversion of HTML characters to unicode.
     */
    class MyCellRenderer extends JLabel implements ListCellRenderer {
        public MyCellRenderer() {
            setOpaque(true);
        }
        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
        	if (null!=value) {
	            String str = (String)value;
	            setText(HTMLToUnicode(str));
        	}
            setBackground(isSelected ? selectedDownloadsTable.getSelectionBackground() : categoryComboBox.getBackground());
            setForeground(isSelected ? selectedDownloadsTable.getSelectionForeground() : categoryComboBox.getForeground());
            return this;
        }
    }

    final DriverManager driverManager = new DriverManager();

    /** This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents(boolean extendedView) {

        javax.swing.JLabel manufacturerLabel = new javax.swing.JLabel();
        javax.swing.JLabel categoryLabel = new javax.swing.JLabel();
        javax.swing.JLabel modelsLabel = new javax.swing.JLabel();
        manufacturerComboBox = new javax.swing.JComboBox();
        categoryComboBox = new javax.swing.JComboBox();
        updateDataButton = new javax.swing.JButton();
        modelsListScrollPane = new javax.swing.JScrollPane();
        modelsList = new javax.swing.JList();
        modelsFilterTextField = new javax.swing.JTextField();
        addSelectedModelsButton = new javax.swing.JButton();
        javax.swing.JLabel filterLabel = new javax.swing.JLabel();
        selectedDownloadsTableScrollPane = new javax.swing.JScrollPane();
        selectedDownloadsTable = new javax.swing.JTable();
        downloadButton = new javax.swing.JButton();
        interruptDownloadButton = new javax.swing.JButton();
        removeModelButton = new javax.swing.JButton();
        filenameTextField = new javax.swing.JTextField();
        fileProgressLabel = new javax.swing.JLabel();
        messagesTextAreaScrollPane = new javax.swing.JScrollPane();
        messagesTextArea = new javax.swing.JTextArea();

        manufacturerLabel.setText("Manufacturer");

        categoryLabel.setText("Category");

        modelsLabel.setText("Models");

        manufacturerComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                manufacturerComboBoxItemStateChanged(evt);
            }
        });

        categoryComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                categoryComboBoxItemStateChanged(evt);
            }
        });

        updateDataButton.setText("Update");
        updateDataButton.setMargin(new Insets(1, 1, 1, 1)); // make it as small as possible
        updateDataButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateDataButtonActionPerformed(evt);
            }
        });

        modelsListScrollPane.setViewportView(modelsList);

        modelsFilterTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                modelsFilterTextFieldKeyReleased(evt);
            }
        });

        addSelectedModelsButton.setText("Add selected models");
        addSelectedModelsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSelectedModelsButtonActionPerformed(evt);
            }
        });

        filterLabel.setText("Filter models:");
        
        modelsList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				// double click action shows url
				if (arg0.getClickCount()==2) {
					try {
						String name = (String)modelsList.getSelectedValue();
						PostableUrl url = driverManager.getURL(name);
			    		openUrl(url, name);
					} catch (Exception ex) {
						
					}
					//showModelUrl();
				}				
			}
		});

        selectedDownloadsTable.setModel(new SelectedDownloadsTableModel(selectedDownloads));
        selectedDownloadsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                selectedDownloadsTableMouseClicked(evt);
            }
        });
        selectedDownloadsTableScrollPane.setViewportView(selectedDownloadsTable);

        downloadButton.setText("Download");
        downloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadButtonActionPerformed(evt);
            }
        });

        interruptDownloadButton.setText("Interrupt download...");
        interruptDownloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                interruptDownloadButtonActionPerformed(evt);
            }
        });
        
        removeModelButton.setText("Remove model");
        removeModelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	removeModelButtonActionPerformed(evt);
            }
        });        

        filenameTextField.setEditable(false);

        fileProgressLabel.setText("0");

        messagesTextArea.setColumns(20);
        messagesTextArea.setEditable(false);
        messagesTextArea.setRows(5);
        messagesTextAreaScrollPane.setViewportView(messagesTextArea);
        
        MigLayout layout = new MigLayout();
        setLayout(layout);
        add(manufacturerLabel);
        add(manufacturerComboBox, "split 2, width 250!");
        if (extendedView) {
            add(updateDataButton, "gapleft 15, width 40");
        	add(messagesTextAreaScrollPane, "span 1 8, width 300!, height 500:500:1500, gapleft 15, growx, wrap");
        } else {
            add(updateDataButton, "gapleft 15, width 40, wrap");        	
        }
        add(categoryLabel);
        add(categoryComboBox, "wrap, width 250!");
        add(filterLabel);
        add(modelsFilterTextField, "width 250!, wrap");
        add(modelsLabel);
        add(modelsListScrollPane, "width 250!, wrap");
        
        if (extendedView) {
        	add(new JLabel(""));
	        add(addSelectedModelsButton, "wrap");
	        add(selectedDownloadsTableScrollPane, "span 2, width 200:420:420, height 200!, wrap");
	        add(downloadButton);
	        add(interruptDownloadButton, "split 2");
	        add(removeModelButton, "gapleft 30, wrap");
	        add(filenameTextField, "width 200!, span 2, split 2");
	        add(fileProgressLabel, "wrap");
        }

    }

    private void updateDataButtonActionPerformed(java.awt.event.ActionEvent evt) {
    	// holding Ctrl while clicking the update button updates only the selected category 
    	boolean updateOnlyCategory = (evt.getModifiers()&ActionEvent.CTRL_MASK)==ActionEvent.CTRL_MASK;
        updateData(updateOnlyCategory);
    }

    private void modelsFilterTextFieldKeyReleased(java.awt.event.KeyEvent evt) {
        filterModelNumbers();
    }

    private void addSelectedModelsButtonActionPerformed(java.awt.event.ActionEvent evt) {
        addSelectedModels();
    }

    private void manufacturerComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {
        String str = (String)evt.getItem();
        driverManager.selectManufacturer(str);
        updateProductTypes();
        updateModelNumbers();
    }

    private void categoryComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {
        if ((evt.getStateChange()&ItemEvent.SELECTED) == ItemEvent.SELECTED) {
            String str = (String)evt.getItem();
            try {
                    driverManager.selectProductType(str);
                    updateModelNumbers();
            } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ""+ex, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void downloadButtonActionPerformed(java.awt.event.ActionEvent evt) {
    	// Holding Ctrl while clicking the download button performs a dry run,
    	// meaning that all pages necessary to find the drivers to download
    	// will be loaded and parsed, but the drivers will not be actually downloaded.
    	// This makes it faster to test the parsing of the pages.
        GlobalOptions.setDryRun((evt.getModifiers()&ActionEvent.CTRL_MASK)==ActionEvent.CTRL_MASK);
        download();
    }

    private void interruptDownloadButtonActionPerformed(java.awt.event.ActionEvent evt) {
        Interrupt.set(true);
    }

    private void removeModelButtonActionPerformed(java.awt.event.ActionEvent evt) {
		int row = selectedDownloadsTable.getSelectedRow();
		selectedDownloads.remove(row);

		selectedDownloadsTable.revalidate();
        selectedDownloadsTable.repaint();
    }

    private void selectedDownloadsTableMouseClicked(java.awt.event.MouseEvent evt) {
        int col = selectedDownloadsTable.getSelectedColumn();
		int row = selectedDownloadsTable.getSelectedRow();
        if (2==col) {
        	PostableUrl url = selectedDownloads.get(row).url;
    		String model = selectedDownloads.get(row).model;
            openUrl(url, model);
        } else if (3==col) {
        	String os = System.getProperty("os.name");
        	if (os.toLowerCase().contains("windows")) {
	        	String folder = selectedDownloads.get(row).directory.toString();
				try {
					Process p = Runtime.getRuntime().exec(System.getenv("windir") +"\\explorer "+folder);
				}
				catch (Exception err) {
				  err.printStackTrace();
				}
        	} else {
        		JOptionPane.showMessageDialog(this, "Can't open explorer window on "+os, "Error", JOptionPane.ERROR_MESSAGE);
        	}
        }
        
    }

	private void openUrl(PostableUrl url, String model) throws HeadlessException {

		boolean shown = false;
		if (Desktop.isDesktopSupported()) {
		    Desktop desktop = Desktop.getDesktop();
		    if (desktop.isSupported(Desktop.Action.BROWSE)) {
		        try {
		            URI uri = new URI(url.getLink());
		            desktop.browse(uri);
		            shown = true;
		        } catch (Exception ex) {

		        }
		    }
		}
		if (!shown)
		    JOptionPane.showMessageDialog(this, url, "URL for "+model, JOptionPane.INFORMATION_MESSAGE);
	}


    // Variables declaration
    private javax.swing.JButton addSelectedModelsButton;
    private javax.swing.JComboBox categoryComboBox;
    private javax.swing.JButton downloadButton;
    private javax.swing.JLabel fileProgressLabel;
    private javax.swing.JTextField filenameTextField;
    private javax.swing.JButton interruptDownloadButton;
    private javax.swing.JButton removeModelButton;
    private javax.swing.JScrollPane modelsListScrollPane;
    private javax.swing.JScrollPane selectedDownloadsTableScrollPane;
    private javax.swing.JScrollPane messagesTextAreaScrollPane;
    private javax.swing.JComboBox manufacturerComboBox;
    private javax.swing.JTextArea messagesTextArea;
    private javax.swing.JTextField modelsFilterTextField;
    private javax.swing.JList modelsList;
    private javax.swing.JTable selectedDownloadsTable;
    private javax.swing.JButton updateDataButton;

    void updateManufacturers() {
        ArrayList<String> manufacturersList = driverManager.getManufacturers();
        manufacturerComboBox.removeAllItems();
        for (String str: manufacturersList) {
            manufacturerComboBox.addItem(str);
        }
    }

    void updateProductTypes() {
        ArrayList<String> productTypesList = driverManager.getProductTypes();
        categoryComboBox.removeAllItems();
        for (String str: productTypesList) {
            categoryComboBox.addItem(str);
        }
    }

    void updateModelNumbers() {
        modelNumbers = driverManager.getModelNumbers();
        filterModelNumbers();
    }

    private void filterModelNumbers() {
    	// filteredModelNumbersList holds the models that are displayed,
    	// here we determine which ones should be in it
    	
        filteredModelNumbersList.clear();

        String filter = modelsFilterTextField.getText().toLowerCase();
        filter.replaceAll(" ", ""); // ignore spaces
        if (!filter.isEmpty()) {
            for (String modelNumber : modelNumbers) {
                if (modelNumber.replaceAll(" ", "").toLowerCase().contains(filter)) {
                    filteredModelNumbersList.add(modelNumber);
                }
            }
        } else {
            for (String modelNumber : modelNumbers) {
                filteredModelNumbersList.add(modelNumber);
            }
        }
        modelsList.setListData(filteredModelNumbersList);
    }
    
    void showModelUrl() {
    	try {
			String name = (String)modelsList.getSelectedValue();
			PostableUrl url = driverManager.getURL(name);
    		JOptionPane.showInputDialog(this, "URL for "+name, url.getLink());
    	} catch (Exception ex) {
    		JOptionPane.showMessageDialog(this, ""+ex);
    	}
    }

    void loadData() {
        List<String> errors = driverManager.loadData();
        for (String error: errors) {
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void download() {
        downloadThread = new DownloadThread(selectedDownloads, new DownloadThreadListener());
        downloadThread.start();
    }

    public void updateData(boolean updateOnlyCategory) {
    	String categoryName = (String)categoryComboBox.getSelectedItem();
    	String text = "Are you sure you want to update"+ (updateOnlyCategory ? " the category "+categoryName : "")+"?";
        if (JOptionPane.showConfirmDialog(this, text, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            UpdateDataThread thread = new UpdateDataThread(driverManager, (String)manufacturerComboBox.getSelectedItem(), updateOnlyCategory, new UpdateDataThreadListener());
            thread.start();
        }
    }

    private File askDownloadDirectory(String model)
    {
        JFileChooser pathSelector = new JFileChooser();
        pathSelector.setCurrentDirectory(new File("."));
        pathSelector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        pathSelector.setDialogTitle("Select download directory for "+model);
        int result = pathSelector.showSaveDialog(this);
        if (JFileChooser.APPROVE_OPTION==result) {
            return pathSelector.getSelectedFile();
        } else {
            return null;
        }
    }

    public void addSelectedModels() {
        String manufacturer = (String)manufacturerComboBox.getSelectedItem();
        IDriverDownloader driverDownloader = null;
        try {
            driverDownloader = driverManager.createDriverDownloader(manufacturer);
        } catch (DriverManager.Exception ex) {
            JOptionPane.showMessageDialog(this, ""+ex,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int selection[] = modelsList.getSelectedIndices();
        for (int idx: selection) {
            String model = filteredModelNumbersList.get(idx);
            File directory = askDownloadDirectory(model);
            if (null!=directory) {
            	try {
            		PostableUrl url = driverManager.getURL(model);
	                SelectedDownload selectedDownload = new SelectedDownload(driverDownloader, model, url, directory);
	                selectedDownload.addStatusListener(selectedDownloadStatusListener);
	                selectedDownloads.add(selectedDownload);
	            } catch (Exception ex) {
	            	JOptionPane.showMessageDialog(this, ""+ex, "Error", JOptionPane.ERROR_MESSAGE);
	            }
            }
        }
        selectedDownloadsTable.revalidate();
        selectedDownloadsTable.repaint();
    }

    private void switchToDownloadState(boolean b) {
        interruptDownloadButton.setEnabled(b);
        downloadButton.setEnabled(!b);
        updateDataButton.setEnabled(!b);
    }

    static private String HTMLToUnicode(String str) {
        int pos = 0;
        while (true) {
            pos = str.indexOf("&", pos);
            if (pos<0)
                break;
            int endPos = str.indexOf(";", pos);
            if (endPos<0)
                break;
            String unicodeChar = htmlChars.get(str.substring(pos, endPos+1));
            if (null!=unicodeChar)
                str = str.substring(0, pos)+unicodeChar+str.substring(endPos+1);
            ++pos;
        }
        return str;
    }

    class ThreadSafeLabelSetText implements Runnable {
        JLabel label;
        String text;

        public ThreadSafeLabelSetText(JLabel label, String text) {
            this.label = label;
            this.text = text;
        }

        public void run() {
            label.setText(text);
        }
    }

    class ThreadSafeTextComponentSetText implements Runnable {
        JTextComponent textComponent;
        String text;

        public ThreadSafeTextComponentSetText(JTextComponent textComponent, String text) {
            this.textComponent = textComponent;
            this.text = text;
        }

        public void run() {
            textComponent.setText(text);
        }
    }

    class ThreadSafeShowMessage implements Runnable {
        Object text;
        String title = null;
        int type = 0;

        public ThreadSafeShowMessage(Object text) {
            this.text = text;
        }

        public ThreadSafeShowMessage(Object text, String title, int type) {
            this.text = text;
            this.title = title;
            this.type = type;
        }

        public void run() {
        	if (null==title)
        		JOptionPane.showMessageDialog(DriverDownloaderPanel.this, text);
        	else
        		JOptionPane.showMessageDialog(DriverDownloaderPanel.this, text, title, type);
        }
    }

    DownloadThread downloadThread;

    ArrayList<String> modelNumbers = new ArrayList<String>();
    Vector<String> filteredModelNumbersList = new Vector<String>();

    List<SelectedDownload> selectedDownloads = Collections.synchronizedList(new ArrayList<SelectedDownload>());

    static Map<String, String> htmlChars = new HashMap<String, String>();
    static {
        htmlChars.put("&amp;", "&");
        htmlChars.put("&alpha;", "\u03b1");
        htmlChars.put("&reg;", "\u00AE");
        htmlChars.put("&trade;", "\u2122");
        htmlChars.put("&Eacute;", "\u00C9");
        htmlChars.put("&eacute;", "\u00E9");
    }

    DownloadObserver driverDownloaderListener = new DownloadObserver() {
        public void updateFile(String file) {
            EventQueue.invokeLater(new ThreadSafeTextComponentSetText(filenameTextField, file));
        }
        public void updateProgress(long bytes) {
            String text = bytes<0 ? "" : ""+bytes;
            EventQueue.invokeLater(new ThreadSafeLabelSetText(fileProgressLabel, text));
        }
    };

    class ThreadSafeTableRefresh implements Runnable {
        public void run() {
            selectedDownloadsTable.revalidate();
            selectedDownloadsTable.repaint();
        }
    }

    class ThreadSafeAddMessage implements Runnable {
        String text;

        ThreadSafeAddMessage(String text) {
            this.text = text;
        }

        public void run() {
            messagesTextArea.setText(messagesTextArea.getText()+text+"\n");
        }
    }

    private void threadSafeAddMessage(String text) {
        EventQueue.invokeLater(new ThreadSafeAddMessage(text));
    }

    SelectedDownload.Listener selectedDownloadStatusListener = new SelectedDownload.Listener() {
        ThreadSafeTableRefresh threadSafeTableRefresh = new ThreadSafeTableRefresh();

        public void update() {
            EventQueue.invokeLater(threadSafeTableRefresh);
        }
    };

    class DownloadThreadListener implements ThreadListener {
		@Override
		public void started() {
	        switchToDownloadState(true);
		}
	
		@Override
		public void stopped() {
	        switchToDownloadState(false);
		}
		
		@Override
		public void reportMessage(String msg, String title, int type) {
	    	EventQueue.invokeLater(new ThreadSafeShowMessage(msg, title, type));		
		}
		
		@Override
		public void addMessage(String msg) {
			EventQueue.invokeLater(new ThreadSafeAddMessage(msg));
		}
    }

    class UpdateDataThreadListener extends DownloadThreadListener {
		@Override
		public void stopped() {
	        switchToDownloadState(false);
	        updateManufacturers();
	        updateProductTypes();
	        updateModelNumbers();		
		}
    }
}
