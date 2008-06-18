package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.core.Assert;
import com.jidesoft.grid.ColorCellRenderer;
import com.jidesoft.grid.ColorCellEditor;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.Component;
import java.awt.Color;

class Continuous1BandTabularForm implements PaletteEditorForm, ImageInfoHolder  {
    private final ColorManipulationForm parentForm;
    private ImageInfoTableModel tableModel;
    private JScrollPane contentPanel;

    public Continuous1BandTabularForm(ColorManipulationForm parentForm) {
        this.parentForm = parentForm;
        tableModel = new ImageInfoTableModel(null);
        tableModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                Continuous1BandTabularForm.this.parentForm.setApplyEnabled(true);
            }
        });

        final JTable table = new JTable(tableModel);
        final ColorCellRenderer colorCellRenderer = new ColorCellRenderer();
        colorCellRenderer.setColorValueVisible(false);
        table.setDefaultRenderer(Color.class, colorCellRenderer);
        table.setDefaultEditor(Color.class, new ColorCellEditor());

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        final JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.getViewport().setPreferredSize(table.getPreferredSize());
        contentPanel = tableScrollPane;
    }

    public void performApply(ProductSceneView productSceneView) {
        Assert.notNull(productSceneView, "productSceneView");
        productSceneView.getRaster().setImageInfo(getCurrentImageInfo().createDeepCopy());
    }

    public AbstractButton[] getButtons() {
        return new AbstractButton[]{new JButton(":D")};  // todo
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    public ImageInfo getCurrentImageInfo() {
        return tableModel.getImageInfo();
    }

    public void setCurrentImageInfo(ImageInfo imageInfo) {
        tableModel.setImageInfo(imageInfo);
        parentForm.setApplyEnabled(false);
    }

    public String getTitle(ProductSceneView productSceneView) {
        return ":P";  // todo
    }

    public void handleFormShown(ProductSceneView productSceneView) {
        Assert.notNull(productSceneView, "productSceneView");
        setCurrentImageInfo(productSceneView.getRaster().getImageInfo());
    }

    public void handleFormHidden() {
    }

    public void performReset(ProductSceneView productSceneView) {
        Assert.notNull(productSceneView, "productSceneView");

    }

    public void updateState(ProductSceneView productSceneView) {
        Assert.notNull(productSceneView, "productSceneView");

    }

    private static class ImageInfoTableModel extends AbstractTableModel {

        private ImageInfo imageInfo;
        private static final String[] COLUMN_NAMES = new String[]{"Colour", "Value"};
        private static final Class<?>[] COLUMN_TYPES = new Class<?>[]{Color.class, Double.class};

        private ImageInfoTableModel(ImageInfo imageInfo) {
            this.imageInfo = imageInfo;
        }

        public ImageInfo getImageInfo() {
            return imageInfo;
        }

        public void setImageInfo(ImageInfo imageInfo) {
            this.imageInfo = imageInfo;
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return COLUMN_NAMES[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return COLUMN_TYPES[columnIndex];
        }

        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        public int getRowCount() {
            return imageInfo != null ? imageInfo.getColorPaletteDef().getNumPoints() : 0;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            final ColorPaletteDef.Point point = imageInfo.getColorPaletteDef().getPointAt(rowIndex);
            if (columnIndex == 0) {
                return point.getColor();
            } else if (columnIndex == 1) {
                return point.getSample();
            }
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            final ColorPaletteDef.Point point = imageInfo.getColorPaletteDef().getPointAt(rowIndex);
            if (columnIndex == 0) {
                point.setColor((Color) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            } else if (columnIndex == 1) {
                point.setSample((Double) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0 || columnIndex == 1;
        }

    }
}