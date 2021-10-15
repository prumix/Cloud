package util;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

public class TableMouseListener implements MouseListener {
    /**
     * The owner table of this listener.
     */
    private final JTable table;

    /**
     * The model of the owner table of this listener.
     */
    private final FileTableModel model;

    /**
     * Class constructor.
     *
     * @param table The owner table of this listener.
     * @param model The model of the owner table of this listener.
     */
    public TableMouseListener(JTable table, FileTableModel model) {
        this.table = table;
        this.model = model;
    }

    /**
     * Handles the mouse button clicked.
     *
     * @param event Mouse event.
     */
    @Override
    public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1) {
            handleLeftButtonDoubleClick();
        }
    }

    /**
     * Handles the mouse button pressed.
     *
     * @param event Mouse event.
     */
    @Override
    public void mousePressed(MouseEvent event) {
    }

    /**
     * Handles the mouse button released.
     *
     * @param event Mouse event.
     */
    @Override
    public void mouseReleased(MouseEvent event) {
    }

    /**
     * Handles the mouse entered.
     *
     * @param event Mouse event.
     */
    @Override
    public void mouseEntered(MouseEvent event) {
    }

    /**
     * Handles the mouse button exited.
     *
     * @param event Mouse event.
     */
    @Override
    public void mouseExited(MouseEvent event) {
    }

    /**
     * Handles left mouse button double click.
     */
    private void handleLeftButtonDoubleClick() {
        final int currentRowIndex = table.getSelectedRow();
        if (currentRowIndex > -1) {
            final File selectedDir = model.getFile(currentRowIndex);
            if (selectedDir.isDirectory()) {
                model.setDir(selectedDir);
                updateFilesPanel(selectedDir.getPath());
            }
        }
    }

    /**
     * Updates the path field and the files list int the table of the parent panel.
     *
     * @param path Path to the chosen directory.
     */
    private void updateFilesPanel(String path) {
        FilesPanel panel = ((FilesPanel) table.getParent().getParent().getParent());
        panel.updateFiles(path);
    }
}
