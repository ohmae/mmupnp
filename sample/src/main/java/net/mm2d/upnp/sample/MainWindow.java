/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample;

import net.mm2d.upnp.ControlPoint;
import net.mm2d.upnp.ControlPoint.DiscoveryListener;
import net.mm2d.upnp.ControlPoint.NotifyEventListener;
import net.mm2d.upnp.Device;
import net.mm2d.upnp.IconFilter;
import net.mm2d.upnp.Service;
import net.mm2d.util.Log;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.annotation.Nonnull;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class MainWindow extends JFrame {
    private static final String TAG = "MainWindow";

    public static void main(final String[] args) {
        Log.setLogLevel(Log.VERBOSE);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            Log.w(TAG, e);
        }
        new MainWindow();
    }

    private static class MyTreeCellRenderer extends DefaultTreeCellRenderer {
        private final Icon mDeviceIcon;
        private final Icon mServiceIcon;
        private final Icon mVariableListIcon;
        private final Icon mVariableIcon;
        private final Icon mArgumentIcon;
        private final Icon mActionIcon;

        MyTreeCellRenderer() {
            mDeviceIcon = UIManager.getIcon("FileView.computerIcon");
            mServiceIcon = UIManager.getIcon("FileView.directoryIcon");
            mVariableListIcon = UIManager.getIcon("FileView.hardDriveIcon");
            mVariableIcon = UIManager.getIcon("FileView.fileIcon");
            mArgumentIcon = UIManager.getIcon("FileView.fileIcon");
            mActionIcon = UIManager.getIcon("FileView.floppyDriveIcon");
        }

        @Override
        public Component getTreeCellRendererComponent(
                final JTree tree,
                final Object value,
                final boolean sel,
                final boolean expanded,
                final boolean leaf,
                final int row,
                final boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DeviceNode) {
                setIcon(mDeviceIcon);
            } else if (value instanceof ServiceNode) {
                setIcon(mServiceIcon);
                final ServiceNode node = (ServiceNode) value;
                if (node.isSubscribing()) {
                    setForeground(Color.BLUE);
                }
            } else if (value instanceof StateVariableListNode) {
                setIcon(mVariableListIcon);
            } else if (value instanceof StateVariableNode) {
                setIcon(mVariableIcon);
            } else if (value instanceof ArgumentNode) {
                setIcon(mArgumentIcon);
            } else if (value instanceof ActionNode) {
                setIcon(mActionIcon);
            }
            return this;
        }
    }

    private final ControlPoint mControlPoint;
    private final JTree mTree;
    private final JTextArea mDetail1;
    private final JTextArea mDetail2;
    private final JTextArea mEventArea;
    private final UpnpNode mRootNode;

    private final DiscoveryListener mDiscoveryListener = new DiscoveryListener() {
        @Override
        public void onDiscover(@Nonnull final Device device) {
            update();
        }

        @Override
        public void onLost(@Nonnull final Device device) {
            update();
        }

        private void update() {
            final List<Device> deviceList = mControlPoint.getDeviceList();
            mRootNode.removeAllChildren();
            for (final Device device : deviceList) {
                mRootNode.add(new DeviceNode(device));
            }
            final DefaultTreeModel model = (DefaultTreeModel) mTree.getModel();
            model.reload();
        }
    };

    private final NotifyEventListener mEventListener = new NotifyEventListener() {
        @Override
        public void onNotifyEvent(
                @Nonnull final Service service,
                final long seq,
                @Nonnull final String variable,
                @Nonnull final String value) {
            mEventArea.setText(mEventArea.getText() + service.getServiceType() + " : " + seq + " : "
                    + variable + " : " + value + "\n");
        }
    };


    private final TreeSelectionListener mSelectionListener = new TreeSelectionListener() {
        @Override
        public void valueChanged(final TreeSelectionEvent event) {
            final UpnpNode node = (UpnpNode) mTree.getLastSelectedPathComponent();
            mDetail1.setText(node.getDetailText());
            mDetail2.setText(node.getDetailXml());
        }
    };

    private ControlPoint initControlPoint() {
        final ControlPoint controlPoint = new ControlPoint();
        controlPoint.setIconFilter(IconFilter.ALL);
        controlPoint.initialize();
        controlPoint.addDiscoveryListener(mDiscoveryListener);
        controlPoint.addNotifyEventListener(mEventListener);
        return controlPoint;
    }

    private JButton makeStartButton() {
        final JButton button = new JButton("START");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                mControlPoint.start();
                mControlPoint.search();
            }
        });
        return button;
    }

    private JButton makeStopButton() {
        final JButton button = new JButton("STOP");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                mControlPoint.stop();
            }
        });
        return button;
    }

    private JButton makeSearchButton() {
        final JButton button = new JButton("M-SEARCH");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                mControlPoint.search();
            }
        });
        return button;
    }

    private JPanel makeControlPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(makeStartButton());
        panel.add(makeStopButton());
        panel.add(makeSearchButton());
        return panel;
    }

    private JTextArea makeTextArea() {
        final JTextArea area = new JTextArea();
        area.setTabSize(2);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return area;
    }

    private JTree makeTree() {
        final JTree tree = new JTree(mRootNode, true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addMouseListener(mTreeMouseListener);
        tree.addTreeSelectionListener(mSelectionListener);
        tree.setCellRenderer(new MyTreeCellRenderer());
        return tree;
    }

    private final MouseListener mTreeMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (!SwingUtilities.isRightMouseButton(e)) {
                return;
            }
            final int x = e.getX();
            final int y = e.getY();
            final int row = mTree.getRowForLocation(x, y);
            if (row < 0) {
                return;
            }
            mTree.setSelectionRow(row);
            final UpnpNode node = (UpnpNode) mTree.getLastSelectedPathComponent();
            if (node == null) {
                return;
            }
            node.showContextMenu(MainWindow.this, mTree, x, y);
        }
    };

    public MainWindow() {
        super();
        mControlPoint = initControlPoint();
        setTitle("UPnP");
        setSize(800, 800);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().add(makeControlPanel(), BorderLayout.NORTH);
        mRootNode = new UpnpNode("Device");
        mRootNode.setAllowsChildren(true);
        mTree = makeTree();
        mDetail1 = makeTextArea();
        mDetail2 = makeTextArea();
        mEventArea = makeTextArea();

        final JSplitPane detail = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(mDetail1), new JScrollPane(mDetail2));
        detail.setDividerLocation(250);
        final JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(mTree), detail);
        main.setDividerLocation(300);

        final JSplitPane content = new JSplitPane(JSplitPane.VERTICAL_SPLIT, main, new JScrollPane(mEventArea));
        content.setDividerLocation(600);

        getContentPane().add(content, BorderLayout.CENTER);
        setVisible(true);
    }
}
