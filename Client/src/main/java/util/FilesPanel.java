package util;

import client.GuiClient;
import com.formdev.flatlaf.extras.FlatSVGIcon;


import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

public class FilesPanel extends JPanel {
    /**
     * The template for the status label's text.
     */
    private final static String STATUS_TEMPLATE = "%s files of size %s bytes";

    /**
     * The table containing the current directory files view.
     */
    private final FilesTable files;

    /**
     * The label to show total number and size of files in the table's model current directory.
     */
    private final JLabel status;

    /**
     * Properties containing the panel's icons' names.
     */
    private final Properties properties;

    /**
     * The text field containing the current directory path.
     */
    private JTextField path;

    /**
     * The text field for the search query.
     */
    private JTextField searchField;

    /**
     * Class constructor.
     *
     * @param title The title to be shown in the titled border of the panel.
     * @param buttonsPanel The control buttons panel of the files table of the panel.
     * @param filesTable The files table of the panel.
     * @param statusLabel The status label to show total number and size
     *                    of the files in the table's model current directory .
     */
    public FilesPanel(String title, JPanel buttonsPanel, FilesTable filesTable, JLabel statusLabel) {
        this.files = filesTable;
        this.status = statusLabel;
        this.properties = new Properties();

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(title));

        final JPanel buttonsAndPathPanel = getButtonsAndPathPanel(buttonsPanel, filesTable);
        final JScrollPane filesPane = new JScrollPane(this.files);

        add(buttonsAndPathPanel, BorderLayout.NORTH);
        add(filesPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    /**
     * @param buttonsPanel Control buttons panel for the files table.
     * @param filesTable Files table.
     * @return Top panel for the file table: buttons, path field and search field.
     */
    private JPanel getButtonsAndPathPanel(JPanel buttonsPanel, FilesTable filesTable) {
        final JPanel pathAndSearchPanel = getPathAndSearchPanel(filesTable);

        final JPanel buttonsAndPath = new JPanel();
        buttonsAndPath.setLayout(new BoxLayout(buttonsAndPath, BoxLayout.PAGE_AXIS));
        buttonsAndPath.add(buttonsPanel);
        buttonsAndPath.add(pathAndSearchPanel);

        return buttonsAndPath;
    }

    /**
     * @param filesTable Files table.
     * @return Path and search panel for the files table.
     */
    private JPanel getPathAndSearchPanel(FilesTable filesTable) {
        final JButton upButton = getUpButton(filesTable);
        path = getPathField();
        final JPanel searchPanel = getSearchPanel();

        final JPanel pathPanel = new JPanel(new BorderLayout());
        pathPanel.add(upButton, BorderLayout.WEST);
        pathPanel.add(path, BorderLayout.CENTER);
        pathPanel.add(searchPanel, BorderLayout.EAST);

        return pathPanel;
    }

    /**
     * @return Path field for the files table.
     */
    private JTextField getPathField() {
        String pathString = "";
        final File dir = getDir();
        if (dir != null) {
            pathString = dir.getPath();
        }
        final JTextField pathField = new JTextField(pathString);
        pathField.setToolTipText("Current directory path");
        pathField.addActionListener(e -> changeDirectory());

        return pathField;
    }

    /**
     * @param filesTable Files table.
     * @return Button to go to the parent directory.
     */
    private JButton getUpButton(FilesTable filesTable) {
        final JButton upButton = new JButton();
        try (InputStream in = GuiClient.class.getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(in);
        } catch (IOException e) {
            // TODO: add logging.
        }
        upButton.setIcon(new FlatSVGIcon(properties.getProperty("upIcon"), 16, 16));
        upButton.setToolTipText("Go to parent directory");
        upButton.addActionListener(e -> goToParentDirectory(filesTable));

        return upButton;
    }

    /**
     * @return Search panel.
     */
    private JPanel getSearchPanel() {
        final JLabel searchLabel = getSearchLabel();
        searchField = getSearchField();
        final JButton clearSearchQueryButton = getClearSearchQueryButton();

        final JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(clearSearchQueryButton, BorderLayout.EAST);

        return searchPanel;
    }

    /**
     * @return Search panel label.
     */
    private JLabel getSearchLabel() {
        final JLabel searchLabel = new JLabel();
        searchLabel.setIcon(new FlatSVGIcon(properties.getProperty("searchIcon"), 16, 16));
        return searchLabel;
    }

    /**
     * @return Search panel query input text field.
     */
    private JTextField getSearchField() {
        final JTextField field = new JTextField();
        field.setToolTipText("Filter file names by entering search query here");
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterFiles();
                updateStatus();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterFiles();
                updateStatus();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterFiles();
                updateStatus();
            }
        });
        return field;
    }

    public void appendSearchQuery(char c) {
        String searchQuery = searchField.getText() + c;
        searchField.setText(searchQuery);
    }

    /**
     * @return Clear search query button.
     */
    private JButton getClearSearchQueryButton() {
        final JButton endSearchButton = new JButton();
        endSearchButton.setToolTipText("Clear search filter and show all files");
        endSearchButton.setIcon(new FlatSVGIcon(properties.getProperty("endSearchIcon"), 16, 16));
        endSearchButton.addActionListener(e -> clearSearchQuery());
        return endSearchButton;
    }

    /**
     * Clears the search query.
     */
    private void clearSearchQuery() {
        if (searchField.getText().isEmpty()) {
            return;
        }
        searchField.setText("");
        setDir(getDir());
    }

    /**
     * Filters files in the current directory according to the entered query.
     */
    private void filterFiles() {
        setFiles(getFiles());
    }

    /**
     * Sets the files to show in the current directory.
     *
     * @param fileArray Array of the files to show in the current directory.
     */
    public void setFiles(File[] fileArray) {
        ((FileTableModel) files.getModel()).setFiles(fileArray);
    }

    /**
     * @return Array of the files to show in the current directory based on the applied filter (if any).
     */
    public File[] getFiles() {
        if (mayUseDir()) {
            return getDir().listFiles((dir, name) -> name
                    .toLowerCase(Locale.getDefault())
                    .contains(searchField
                            .getText()
                            .toLowerCase(Locale.getDefault())));
        }
        return new File[0];
    }

    /**
     * Changes current directory when the path is edited manually.
     */
    private void changeDirectory() {
        final File enteredPath = new File(path.getText());
        if (!enteredPath.exists()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Entered path does not exist.",
                    "Wrong path",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!enteredPath.isDirectory()) {
            JOptionPane.showMessageDialog(
                    this,
                    "You should enter the path to the directory, not file.",
                    "Wrong path",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        setDir(enteredPath);
    }

    /**
     * Changes current directory to its parent.
     *
     * @param filesTable Table representing current directory files.
     */
    private void goToParentDirectory(FilesTable filesTable) {
        final FileTableModel model = (FileTableModel) filesTable.getModel();
        final String parentName = model.getDir().getParent();
        if (parentName != null) {
            final File parent = new File(parentName);
            if (!model.getDir().equals(parent)) {
                model.setDir(parent);
                updateFiles(parent.getPath());
            }
        }
    }

    /**
     * Updates the file table based on the applied filter (if any).
     *
     * @param path Path to the current directory.
     */
    public void updateFiles(String path) {
        setPath(path);
        filterFiles();
        updateStatus();
    }

    /**
     * @return A flag whether the current directory can be accessed.
     */
    private boolean mayUseDir() {
        return this.files != null
                && this.files.getModel() != null
                && ((FileTableModel) this.files.getModel()).getDir() != null;
    }

    /**
     * Sets the string representation of the current directory status.
     */
    public void updateStatus() {
        final long numOfFiles = getNumOfFiles();
        final long numOfBytes = getNumOfBytes();

        DecimalFormat decimalFormat = new DecimalFormat("#");
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);

        final String statusString = String.format(STATUS_TEMPLATE, decimalFormat.format(numOfFiles), decimalFormat.format(numOfBytes));
        status.setText(statusString);
    }

    /**
     * @return Total number of filtered (if any filter is applied) files in a current directory.
     */
    private long getNumOfFiles() {
        return Arrays.stream(getFiles()).mapToLong(e -> e.exists() ? 1 : 0).sum();
    }

    /**
     * @return Total size (in bytes) of filtered (if any filter is applied) files in a current directory.
     */
    private long getNumOfBytes() {
        return Arrays.stream(getFiles()).mapToLong(e -> e.exists() ? e.length() : 0).sum();
    }

    /**
     * Sets the string representation of the current directory path.
     *
     * @param newPath New path string.
     */
    public void setPath(String newPath) {
        path.setText(newPath);
    }

    /**
     * Sets the current directory.
     *
     * @param dir New directory.
     */
    public void setDir(File dir) {
        ((FileTableModel) files.getModel()).setDir(dir);
    }

    /**
     * @return Current directory.
     */
    public File getDir() {
        if (mayUseDir()) {
            return ((FileTableModel) files.getModel()).getDir();
        }
        return null;
    }

    /**
     * @return Selected file.
     */
    public File getSelectedFile() {
        if (files.getModel() == null) {
            return null;
        }
        final int currentRowIndex = files.getSelectedRow();
        if (currentRowIndex > -1) {
            return ((FileTableModel) files.getModel()).getFile(currentRowIndex);
        }
        return null;
    }
}
