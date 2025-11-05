package karaed.gui.options;

import java.awt.*;
import java.util.function.Function;

final class VerticalLayout implements LayoutManager {

    private static final int DELTA = 5;

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    private static Dimension layoutSize(Container parent, Function<Component, Dimension> getSize) {
        int count = parent.getComponentCount();
        int maxw = 0;
        int sumh = 0;
        for (int i = 0; i < count; i++) {
            Component child = parent.getComponent(i);
            Dimension size = getSize.apply(child);
            maxw = Math.max(maxw, size.width);
            if (i > 0) {
                sumh += DELTA;
            }
            sumh += size.height;
        }
        Insets insets = parent.getInsets();
        return new Dimension(insets.left + maxw + insets.right, insets.top + sumh + insets.bottom);
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
        int count = parent.getComponentCount();
        Insets insets = parent.getInsets();
        int y = insets.top;
        int width = parent.getWidth() - insets.left - insets.right;
        for (int i = 0; i < count; i++) {
            Component child = parent.getComponent(i);
            int height = child.getPreferredSize().height;
            child.setBounds(insets.left, y, width, height);
            y += height + DELTA;
        }
    }
}
