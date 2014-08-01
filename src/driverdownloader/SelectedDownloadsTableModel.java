package driverdownloader;

import java.net.URL;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Mark Van Peteghem
 */
public class SelectedDownloadsTableModel extends AbstractTableModel {

    List<SelectedDownload> selectedDownloads;
    ImageIcon urlIcon;
    ImageIcon folderIcon;

    SelectedDownloadsTableModel(List<SelectedDownload> selectedDownloads) {
        this.selectedDownloads = selectedDownloads;

        try {
            URL iconUrl = DriverDownloaderPanel.class.getResource("url.gif");
            this.urlIcon = new ImageIcon(iconUrl);

            URL folderUrl = DriverDownloaderPanel.class.getResource("folder.gif");
            this.folderIcon = new ImageIcon(folderUrl);
        } catch (Exception ex) {
            
        }
    }

    public int getRowCount() {
        return selectedDownloads.size();
    }

    public int getColumnCount() {
        return 4;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return selectedDownloads.get(rowIndex).model;
            case 1:
                return selectedDownloads.get(rowIndex).status;
            case 2:
                return urlIcon;
            case 3:
                return folderIcon;
            default:
                return null;
        }
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Model";
            case 1:
                return "Status";
            case 2:
                return "URL";
            case 3:
                return "Folder";
            default:
                return null;
        }
    }

    @Override
    public Class getColumnClass(int column) {
        if (2==column || 3==column)
            return ImageIcon.class;
        else
            return Object.class;
    }
}
