/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.testui;

import net.mm2d.upnp.Action;
import net.mm2d.upnp.ControlPoint;
import net.mm2d.upnp.ControlPoint.DiscoveryListener;
import net.mm2d.upnp.ControlPoint.NotifyEventListener;
import net.mm2d.upnp.Device;
import net.mm2d.upnp.Service;
import net.mm2d.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class MainWindow extends JFrame {
    private static final String TAG = "MainWindow";

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            Log.w(TAG, e);
        }
        new MainWindow();
    }

    private final ControlPoint mControlPoint;
    private final JTree mTree;
    private final JTextArea mDetail1;
    private final JTextArea mDetail2;
    private final JTextArea mEvent;
    private final UpnpNode mRootNode;
    private final DiscoveryListener mListener = new DiscoveryListener() {
        @Override
        public void onDiscover(Device device) {
            update();
        }

        @Override
        public void onLost(Device device) {
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
    private final TreeSelectionListener mSelectionListener = new TreeSelectionListener() {
        @Override
        public void valueChanged(TreeSelectionEvent event) {
            final UpnpNode node = (UpnpNode) mTree.getLastSelectedPathComponent();
            mDetail1.setText(node.getDetailText());
            mDetail2.setText(node.getDetailXml());
            if (node.getUserObject() instanceof Action) {
                final Action action = (Action) node.getUserObject();
                if (!action.getName().equals("Browse")) {
                    return;
                }
                final Map<String, String> arg = new HashMap<>();
                arg.put("ObjectID", "0");
                arg.put("BrowseFlag", "BrowseDirectChildren");
                arg.put("Filter", "*");
                arg.put("StartingIndex", "0");
                arg.put("RequestedCount", "0");
                arg.put("SortCriteria", "");
                try {
                    final Map<String, String> result = action.invoke(arg);
                    if (result == null) {
                        return;
                    }
                    perseResult(result.get("Result"));
                } catch (IOException | SAXException | ParserConfigurationException e) {
                    Log.w(TAG, e);
                }
            } else if (node.getUserObject() instanceof Service) {
                final Service service = (Service) node.getUserObject();
                try {
                    service.subscribe(true);
                } catch (final IOException e) {
                    Log.w(TAG, e);
                }
            }
        }
    };

    private void perseResult(String xml)
            throws IOException, SAXException, ParserConfigurationException {
        final StringBuilder sb = new StringBuilder();
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final Document doc = db.parse(new InputSource(new StringReader(xml)));
        Node n = doc.getDocumentElement().getFirstChild();
        for (; n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if ("item".equals(n.getLocalName())
                    || "container".equals(n.getLocalName())) {
                Node i = n.getFirstChild();
                for (; i != null; i = i.getNextSibling()) {
                    if (i.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if ("title".equals(i.getLocalName())) {
                        sb.append(i.getTextContent());
                        sb.append('\n');
                    }
                }
            }
        }
        mDetail2.setText(sb.toString());
    }

    private final NotifyEventListener mEventListener = new NotifyEventListener() {
        @Override
        public void onNotifyEvent(Service service, long seq, String variable, String value) {
            mEvent.setText(mEvent.getText() + service.getServiceType() + " : " + seq + " : "
                    + variable + " : " + value + "\n");
        }
    };

    public MainWindow() {
        super();
        mControlPoint = new ControlPoint();
        mControlPoint.initialize();
        mControlPoint.addDiscoveryListener(mListener);
        mControlPoint.addNotifyEventListener(mEventListener);
        setTitle("UPnP");
        setSize(800, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new FlowLayout());
        final JButton button1 = new JButton("START");
        contentPane.add(button1);
        final JButton button2 = new JButton("STOP");
        contentPane.add(button2);
        final JButton button3 = new JButton("M-SEARCH");
        contentPane.add(button3);
        getContentPane().add(contentPane, BorderLayout.NORTH);
        mRootNode = new UpnpNode("Device");
        mRootNode.setAllowsChildren(true);
        mTree = new JTree(mRootNode, true);
        mTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        mTree.addTreeSelectionListener(mSelectionListener);
        mDetail1 = new JTextArea();
        mDetail1.setEditable(false);
        mDetail1.setFont(new Font("Monospaced", Font.PLAIN, 12));
        mDetail2 = new JTextArea();
        mDetail2.setEditable(false);
        mDetail2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        final JSplitPane detail = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(mDetail1), new JScrollPane(mDetail2));
        detail.setDividerLocation(250);
        final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(mTree), detail);
        split.setDividerLocation(300);
        getContentPane().add(split, BorderLayout.CENTER);
        mEvent = new JTextArea();
        mEvent.setFont(new Font("Monospaced", Font.PLAIN, 12));
        final JScrollPane s = new JScrollPane(mEvent);
        s.setPreferredSize(new Dimension(100, 100));
        getContentPane().add(s, BorderLayout.SOUTH);
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mControlPoint.start();
            }
        });
        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mControlPoint.stop();
            }
        });
        button3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mControlPoint.search();
            }
        });
        setVisible(true);
    }
}
