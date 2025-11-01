package karaed.gui.start;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

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
        return new Dimension(sumw, maxh);
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
        Component c1 = parent.getComponent(0);
        Dimension s1 = c1.getPreferredSize();

        c1.setBounds((parent.getWidth() - s1.width) / 2, (parent.getHeight() - s1.height) / 2, s1.width, s1.height);

        Component c2 = parent.getComponent(1);
        Dimension s2 = c2.getPreferredSize();
        c2.setBounds(parent.getWidth() - s2.width - 5, (parent.getHeight() - s2.height) / 2, s2.width, s2.height);
    }
}
