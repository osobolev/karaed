package karaed.gui.util;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class ButtonAreaLayout implements LayoutManager {

    private final int xgap = 5;
    private final int ygap = 5;

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    private int width(int maxWidth, int n) {
        return maxWidth * n + xgap * (n - 1);
    }

    private Dimension layoutSize(Container parent, Function<Component, Dimension> getSize) {
        int n = parent.getComponentCount();
        int maxWidth = 0;
        int maxHeight = 0;
        for (int i = 0; i < n; i++) {
            Component child = parent.getComponent(i);
            Dimension d = getSize.apply(child);
            maxWidth = Math.max(maxWidth, d.width);
            maxHeight = Math.max(maxHeight, d.height);
        }
        Insets insets = parent.getInsets();
        return new Dimension(
            width(maxWidth, n) + insets.left + insets.right + xgap * 2,
            maxHeight + insets.top + insets.bottom + ygap * 2
        );
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return layoutSize(parent, Component::getPreferredSize);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return layoutSize(parent, Component::getMinimumSize);
    }

    @Override
    public void layoutContainer(Container parent) {
        int n = parent.getComponentCount();
        int maxWidth = 0;
        for (int i = 0; i < n; i++) {
            Component child = parent.getComponent(i);
            Dimension d = child.getPreferredSize();
            maxWidth = Math.max(maxWidth, d.width);
        }
        Insets insets = parent.getInsets();
        int x = (parent.getWidth() - width(maxWidth, n)) / 2;
        int y = insets.top + ygap;
        int h = parent.getHeight() - insets.top - insets.bottom - ygap * 2;
        for (int i = 0; i < n; i++) {
            Component child = parent.getComponent(i);
            child.setBounds(x, y, maxWidth, h);
            x += maxWidth + xgap;
        }
    }

    public static JPanel newButt() {
        return new JPanel(new ButtonAreaLayout());
    }
}
