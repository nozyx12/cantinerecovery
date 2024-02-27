package fr.nozyx.cantinerecovery;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CenteredTextListCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(new EmptyBorder(5, 10, 5, 10)); // Espacement intérieur

        return label;
    }
}
