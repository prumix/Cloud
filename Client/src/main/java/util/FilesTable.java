package util;

import javax.swing.*;
import java.awt.*;

public class FilesTable extends JTable {

    public FilesTable(FileTableModel model) {
        super(model);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setAutoCreateRowSorter(true);
        setShowVerticalLines(true);
        setShowHorizontalLines(true);
        setIconColumnWidth(30);
        setIntercellSpacing(new Dimension(10, 0));

        addKeyListener(new TableKeyListener(this, model));
        addMouseListener(new TableMouseListener(this, model));
    }

    /**
     * Sets the icon column width.
     *
     * @param width Icon column width.
     */
    private void setIconColumnWidth(int width) {
        this.getColumnModel().getColumn(0).setMaxWidth(width);
    }
}

