package Component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class BreadcrumbPanel extends JPanel {
    private String currentPath = "";
    private BreadcrumbListener listener;

    public BreadcrumbPanel(BreadcrumbListener listener) {
        this.listener = listener;
        setLayout(new FlowLayout(FlowLayout.LEFT, 4, 3));
        setBackground(Color.WHITE);
        updateBreadcrumb("");
    }
    
    public void updateBreadcrumb(String path) {
        this.currentPath = path;
        removeAll();

        JLabel rootLabel = createLinkLabel("Главная", "");
        add(rootLabel);

        if (!path.isEmpty()) {
            String[] parts = path.split("\\Q" + File.separator + "\\E");
            String cumulative = "";
            for (String part : parts) {
                if (part.isEmpty()) continue;
                cumulative += File.separator + part;

                add(createArrowLabel());
                JLabel partLabel = createLinkLabel(part, cumulative);
                add(partLabel);
            }
        }
        revalidate();
        repaint();
    }

    private JLabel createArrowLabel() {
        JLabel arrow = new JLabel(">");
        arrow.setForeground(new Color(120, 120, 120));
        arrow.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        arrow.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        return arrow;
    }

    private JLabel createLinkLabel(String text, final String targetPath) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.BLACK);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                listener.onBreadcrumbClicked(targetPath);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                label.setBackground(new Color(230, 230, 230));
                label.setForeground(Color.BLACK);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setBackground(Color.WHITE);
                label.setForeground(Color.BLACK);
            }
        });
        return label;
    }

    public interface BreadcrumbListener {
        void onBreadcrumbClicked(String path);
    }
}