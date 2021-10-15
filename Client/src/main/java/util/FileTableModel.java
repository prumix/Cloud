package util;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class FileTableModel extends AbstractTableModel {
    /**
     * A representation of the model's host file system.
     */
    private final FileSystemView fileSystemView = FileSystemView.getFileSystemView();

    /**
     * Names of the columns.
     */
    private final String[] columnNames = new String[]{
            "",
            "Name",
            "Size",
            "Created",
            "Last modified",
            "Attributes"
    };

    /**
     * Types of data in the columns.
     */
    private final Class<?>[] columnClasses = new Class[]{
            ImageIcon.class,
            String.class,
            Long.class,
            Date.class,
            Date.class,
            String.class
    };
    /**
     * Current directory of the model.
     */
    private File dir;

    /**
     * Files of the current directory.
     */
    private File[] files;

    /**
     * Class constructor.
     *
     * @param dir The directory to be set as the model's current directory.
     */
    public FileTableModel(File dir) {
        this.dir = dir;
        if (dir != null) {
            this.files = dir.listFiles();
        }
    }

    /**
     * @return The current directory.
     */
    public File getDir() {
        return dir;
    }

    /**
     * Sets the current directory for the model.
     *
     * @param dir The directory to be set as the current directory.
     */
    public void setDir(File dir) {
        this.dir = dir;
        if (dir == null) {
            setFiles(new File[0]);
        } else {
            setFiles(dir.listFiles());
        }
    }

    /**
     * Sets the current directory files.
     * Used to show a filtered list of the current directory files.
     *
     * @param files Array of the files to be set as the current directory files.
     */
    public void setFiles(File[] files) {
        this.files = files;
        fireTableDataChanged();
    }

    /**
     * @return The number of columns of this model.
     */
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    /**
     * @return The number of rows of this model.
     */
    @Override
    public int getRowCount() {
        if (files == null) {
            return 0;
        }
        return files.length;
    }

    /**
     * @param col Index of the column.
     * @return The name of the column.
     */
    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    /**
     * @param col Index of the column.
     * @return The type of the column data.
     */
    @Override
    public Class<?> getColumnClass(int col) {
        return columnClasses[col];
    }

    /**
     * @param row The row index of the file in this model.
     * @return The file in the specified row.
     */
    public File getFile(int row) {
        return files[row];
    }

    /**
     * @param row The row index of the data.
     * @param col The column index of the data.
     * @return The data in the specified row and column.
     */
    @Override
    public Object getValueAt(int row, int col) {
        File f = files[row];
        BasicFileAttributes attributes = null;
        if (!f.exists()) {
            return null;
        }
        try {
            attributes = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        switch (col) {
            case 0:
                return fileSystemView.getSystemIcon(f);
            case 1:
                return f.getName();
            case 2:
                return f.length();
            case 3:
                if (attributes != null) {
                    return new Date(attributes.creationTime().to(TimeUnit.MILLISECONDS));
                }
                return new Date(f.lastModified());
            case 4:
                return new Date(f.lastModified());
            case 5:
                return
                        (f.isDirectory() ? "d" : "")
                                + (f.canRead() ? "r" : "")
                                + (f.canWrite() ? "w" : "");
            default:
                return null;
        }
    }
}
