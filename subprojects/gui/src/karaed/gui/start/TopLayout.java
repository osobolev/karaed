package karaed.gui.start;

import java.awt.*;

final class TopLayout implements LayoutManager {

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        int count = parent.getComponentCount();
        int maxh = 0;
        int sumw = 0;
        for (int i = 0; i < count; i++) {
            Component c = parent.getComponent(i);
            Dimension size = c.getPreferredSize();
            sumw += size.width;
            maxh = Math.max(maxh, size.height);
        }
        Insets insets = parent.getInsets();
        return new Dimension(insets.left + sumw + insets.right, insets.top + maxh + insets.bottom);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        int count = parent.getComponentCount();
        if (count < 2)
            return;

        int width = parent.getWidth();
        int height = parent.getHeight();
        Insets insets = parent.getInsets();

        Component c1 = parent.getComponent(0);
        Dimension s1 = c1.getPreferredSize();
        c1.setBounds((width - s1.width) / 2, (height - s1.height) / 2, s1.width, s1.height);

        Component c2 = parent.getComponent(1);
        Dimension s2 = c2.getPreferredSize();
        c2.setBounds(width - s2.width - 5 - insets.right, (height - s2.height) / 2, s2.width, s2.height);
    }
}
