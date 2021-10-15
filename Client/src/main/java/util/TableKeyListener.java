package util;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

public class TableKeyListener implements KeyListener {
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
    public TableKeyListener(JTable table, FileTableModel model) {
        this.table = table;
        this.model = model;
    }

    /**
     * Handles the key typed.
     *
     * @param event Key event.
     */
    @Override
    public void keyTyped(KeyEvent event) {
    }

    /**
     * Handles the key pressed.
     *
     * @param event Key event.
     */
    @Override
    public void keyPressed(KeyEvent event) {
        switch (event.getKeyChar()) {
            case KeyEvent.VK_ENTER:
                handleEnterKeyPress();
                break;
            case KeyEvent.VK_BACK_SPACE:
                handleBackspaceKeyPress();
                break;
        }
    }

    /**
     * Handles the key released.
     *
     * @param event Key event.
     */
    @Override
    public void keyReleased(KeyEvent event) {
        char inputChar = event.getKeyChar();
        if (isValidCharacter(inputChar)) {
            FilesPanel panel = ((FilesPanel) table.getParent().getParent().getParent());
            panel.appendSearchQuery(event.getKeyChar());
        }
    }

    /**
     * @param inputChar Character to check.
     * @return The result of checking whether the specified character is valid for input in a search query.
     */
    private boolean isValidCharacter(char inputChar) {
        return isNumber(inputChar)
                || isLatinLetter(inputChar)
                || isCyrillicLetter(inputChar);
    }

    /**
     * @param inputChar Character to check.
     * @return The result of checking whether the specified character is a number.
     */
    private boolean isNumber(char inputChar) {
        return inputChar >= '\u0030' && inputChar <= '\u0039';
    }

    /**
     * @param inputChar Character to check.
     * @return The result of checking whether the specified character is a latin letter.
     */
    private boolean isLatinLetter(char inputChar) {
        return inputChar >= '\u0041' && inputChar <= '\u005A'       // Latin letters A-Z.
                || inputChar >= '\u0061' && inputChar <= '\u007A';  // Latin letters a-z.
    }

    /**
     * @param inputChar Character to check.
     * @return The result of checking whether the specified character is a cyrillic letter.
     */
    private boolean isCyrillicLetter(char inputChar) {
        return (inputChar >= '\u0410' && inputChar <= '\u044F') // Cyrillic letters А-я.
                || inputChar == '\u0401'  // Cyrillic letter Ё.
                || inputChar == '\u0451'; // Cyrillic letter ё.
    }

    /**
     * Handles the Enter key pressed.
     */
    private void handleEnterKeyPress() {
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
     * Handles the Backspace key pressed.
     */
    private void handleBackspaceKeyPress() {
        final String parentPath = model.getDir().getParent();
        if (parentPath != null) {
            final File parent = new File(parentPath);
            if (!model.getDir().equals(parent)) {
                model.setDir(parent);
                updateFilesPanel(parentPath);
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

