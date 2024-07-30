package ui;

import dao.FastButtonCfg;
import dao.TreeNode;
import manager.Application;
import manager.Environment;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryForever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.FastConnectHelper;
import util.IconHelper;
import util.NumberHelper;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ConnectDialog extends JDialog {

    protected final Logger logger = LoggerFactory.getLogger(ConnectDialog.class);

    Application application;
    String title;

    public ConnectDialog(Frame owner, boolean modal, Application application, String title) {
        super(owner, modal);
        this.application = application;
        this.title = title;
        this.setTitle(title);
        this.setSize(Application.X_CONNECT_DIALOG_WIDTH, Application.Y_CONNECT_DIALOG_HEIGHT);
        this.setResizable(false);
    }

    JLabel ipLabel = new JLabel("IP：");
    JTextField ipField = new JTextField("127.0.0.1", 15);
    JLabel portLabel = new JLabel("Port：");
    JTextField portField = new JTextField("2181", 6);
    JPanel jPanel = new JPanel(new FlowLayout());

    JButton btnTest = new JButton("测试");
    JButton btnConnect = new JButton("连接");

    JProgressBar jProgressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);

    java.util.LinkedList<JComponent> touchComponents = new LinkedList<>();

    java.util.List<FastButtonCfg> fastButton = FastConnectHelper.fetchFastButtonCfg();

    boolean block = false;

    {
        this.setTitle(this.title);
        this.setLocationByPlatform(true);
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new ConnectDialogWindowAdaptor(this));
        this.setIconImage(IconHelper.ImageTransparentProduce(Application.X_ICON_WIDTH, Application.Y_ICON_HEIGHT));

        GridBagLayout gridBagLayout = new GridBagLayout();
        jPanel.setLayout(gridBagLayout);
        Insets insets = new Insets(1, 1, 1, 1);
        // content
        jPanel.add(
            ipLabel,
            new GridBagConstraints(
                0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL,
                insets,
                0, 0)
        );
        ipField.setToolTipText("服务器IP地址");
        jPanel.add(
            ipField,
            new GridBagConstraints(
                1, 0, 5, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0
            )
        );
        portField.setToolTipText("服务器端口");
        jPanel.add(portLabel,
            new GridBagConstraints(
                0, 1, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0
            ));
        jPanel.add(portField, new GridBagConstraints(
            1, 1, 5, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0
        ));

        final int fastButtonStart = 2;
        final int fastButtonEnd = fastButtonStart + fastButton.size();

        for(int i = 0; i < fastButton.size(); i++) {
            final FastButtonCfg cfg = fastButton.get(i);
            JButton jButton = new JButton(MessageFormat.format("{0}:{1,number,#}", cfg.host, cfg.port));
            jButton.addActionListener((e)-> {
                ipField.setText(cfg.host);
                portField.setText(cfg.port.toString());
            });
            jPanel.add(jButton, new GridBagConstraints(
                0, fastButtonStart + i, 6, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0
            ));
            touchComponents.add(jButton);
        }

        jPanel.add(btnTest, new GridBagConstraints(
            0, fastButtonEnd + 1, 3, 1, 0.5, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0
        ));
        jPanel.add(btnConnect, new GridBagConstraints(
            3, fastButtonEnd + 1, 3, 1, 0.5, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0
        ));

        jProgressBar.setValue(0);
        jProgressBar.setVisible(true);
        jProgressBar.setDoubleBuffered(true);
        jProgressBar.setForeground(Color.GREEN);
        jPanel.add(jProgressBar, new GridBagConstraints(
            0, 5, 6, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0
        ));

        btnTest.addActionListener(e -> {
            if (!tryNext()) {
                return;
            }
            freeze();
            String ip = ipField.getText();
            String portStr = portField.getText();
            String connectString = MessageFormat.format("{0}:{1}", ip, portStr);
            CuratorFramework curatorFramework = CuratorFrameworkFactory.builder().connectString(
                connectString
            ).connectionTimeoutMs(3_000).sessionTimeoutMs(30_000).retryPolicy(new RetryForever(3000)).build();
            jProgressBar.setIndeterminate(true);
            CompletableFuture.supplyAsync(
                () -> {
                    try {
                        curatorFramework.start();
                        return curatorFramework.blockUntilConnected(30, TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                },
                application.executorService
            ).whenCompleteAsync(
                (connected, exception) -> {
                    curatorFramework.close();
                    EventQueue.invokeLater(() -> {
                        jProgressBar.setIndeterminate(false);
                        if (connected != null) {
                            if (connected) {
                                FastConnectHelper.writeToTmpFile(connectString);
                                EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, "连接成功", "ZkView - ConnectTest", JOptionPane.INFORMATION_MESSAGE));
                            } else {
                                EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, "连接失败", "ZkView - ConnectTest", JOptionPane.WARNING_MESSAGE));
                            }
                        }
                        unlock();
                    });


                },
                application.executorService
            );
        });
        if (Environment.Instance.properties.getProperty("env", "-").compareTo("debug") == 0) {
            btnConnect.addActionListener((e) -> EventQueue.invokeLater(() -> {
                application.connectDialog.setVisible(false);
                application.mainFrame.setVisible(true);
            }));

        } else {
            btnConnect.addActionListener(e -> {
                if (!tryNext()) {
                    return;
                }
                freeze();
                Integer port = Objects.requireNonNull(NumberHelper.parse2IntOrNull(portField.getText()));
                String ip = ipField.getText();

                CuratorFramework curatorFramework = CuratorFrameworkFactory.builder().connectString(
                    MessageFormat.format("{0}:{1,number,#}", ip, port)
                ).connectionTimeoutMs(3_000).sessionTimeoutMs(60_000).retryPolicy(new RetryForever(15_000)).build();
                jProgressBar.setIndeterminate(true);
                CompletableFuture.supplyAsync(() -> {

                    try {
                        long timeBeforeConnect = System.currentTimeMillis();
                        curatorFramework.start();
                        final boolean connected = curatorFramework.blockUntilConnected(30, TimeUnit.SECONDS);
                        if (!connected) {
                            throw new RuntimeException("connect time out");
                        }
                        logger.trace("连接成功,正在读取数据");
                        final long timeConnected = System.currentTimeMillis();
                        // region 读取zk里的数据生成Tree
                        TreeNode root = new TreeNode("root");
                        root.setOverrideString("/");
                        Queue<TreeNode> queue = new LinkedList<>();
                        queue.offer(root);
                        while (!queue.isEmpty()) {
                            TreeNode index = queue.poll();
                            if (index == null) {
                                break;
                            }
                            String path = index.getPathNotNull();

                            LinkedList<String> mutableList = new LinkedList<>(curatorFramework.getChildren().forPath(path));
                            if (mutableList.isEmpty()) {
                                continue;
                            }
                            Collections.sort(mutableList);
                            for (String nodeName : mutableList) {
                                if (!nodeName.matches("[\\u4e00-\\u9fa5_a-zA-Z0-9.\\-:]+")) {
                                    logger.warn("ignore zk node {}/{}, because of invalid name", index.getPathNotNull(), nodeName);
                                    continue;
                                }
                                TreeNode node = new TreeNode(nodeName);
                                index.insert(node);
                                queue.offer(node);
                            }
                        }

                        final long timeAfterRead = System.currentTimeMillis();
                        final long connectMillis = timeConnected - timeBeforeConnect;
                        logger.trace(MessageFormat.format(
                            "连接Zk耗时{0,number,#}秒{1,number,#}毫秒", connectMillis / 1_000, connectMillis % 1_000
                        ));

                        final long costMillis = timeAfterRead - timeConnected;

                        logger.trace(MessageFormat.format(
                            "本次读取Zk全部目录耗时{0,number,#}秒{1,number,#}毫秒", costMillis / 1_000, costMillis % 1_000
                        ));

                        return root;
                        // endregion
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }, application.executorService).whenCompleteAsync((treeNode, exception) -> {
                    if (exception != null) {
                        curatorFramework.close();
                        logger.error("fail to build treeNode", exception);
                        EventQueue.invokeLater(() -> {
                            jProgressBar.setIndeterminate(false);
                            JOptionPane.showMessageDialog(application.connectDialog, "连接失败", "ZkView", JOptionPane.ERROR_MESSAGE);
                            application.connectDialog.unlock();
                        });
                        return;
                    }
                    application.mainFrame.root = treeNode;
                    application.curatorFramework = curatorFramework;

                    // 添加一个zk连接断开的监听器
                    curatorFramework.getConnectionStateListenable().addListener((client, state) -> {
                        application.mainFrame.logger.trace("zk state change to {}", state.toString());
                        boolean shouldShow = !state.isConnected();
                        EventQueue.invokeLater(() -> {
                            if (application.mainFrame.isVisible()) {
                                Point point = application.mainFrame.getLocationOnScreen();
                                Dimension dimension = application.mainFrame.getSize();
                                Dimension dimension1 = application.mainFrame.connectLoseDialog.getSize();
                                int x = point.x + dimension.width / 2 - dimension1.width / 2;
                                int y = point.y + dimension.height / 2 - dimension1.height / 2;
                                application.mainFrame.connectLoseDialog.setLocation(new Point(x, y));
                            }
                            application.mainFrame.connectLoseDialog.setVisible(shouldShow);
                        });
                    }, application.executorService);

                    EventQueue.invokeLater(() -> {
                        jProgressBar.setIndeterminate(false);
                        application.mainFrame.jTree.setModel(new DefaultTreeModel(treeNode));
                        application.mainFrame.jTree.updateUI();
                        ConnectDialog.this.setVisible(false);
                        final String newTitle = MessageFormat.format(
                            "{0} - {1}:{2,number,#}",
                            application.mainFrame.title,
                            ip,
                            port
                        );
                        application.mainFrame.setTitle(newTitle);
                        application.mainFrame.setVisible(true);
                    });
                }, application.executorService);
            });
        }


        touchComponents.add(ipField);
        touchComponents.add(portField);
        touchComponents.add(btnTest);
        touchComponents.add(btnConnect);

        this.setContentPane(jPanel);
    }

    protected void freeze() {
        block = true;
        touchComponents.forEach(it -> it.setEnabled(false));
    }

    protected void unlock() {
        touchComponents.forEach(it -> it.setEnabled(true));
        block = false;
    }

    boolean tryNext() {
        if (!checkIp()) {
            JOptionPane.showMessageDialog(this, "IP格式无效", "错误提示", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (!checkPort()) {
            JOptionPane.showMessageDialog(this, "端口无效", "错误提示", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    boolean checkIp() {
        String value = ipField.getText();
        if (!value.matches("(\\d{1,3}\\.){3}\\d{1,3}")) {
            return false;
        }
        String[] list = value.split("\\.");
        if (list.length != 4) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            final Integer num = NumberHelper.parse2IntOrNull(list[i]);
            if (num == null) {
                return false;
            }
            if (num < 0 || num > 255) {
                return false;
            }
            if (i == 0 && num == 0 || i == 3 && num == 255 || i == 3 && num == 0) {
                return false;
            }
        }
        return true;
    }

    boolean checkPort() {
        String value = portField.getText();

        final Integer port = NumberHelper.parse2IntOrNull(value);
        if (port == null) {
            return false;
        }
        return port >= 1 && port <= 65535;
    }
}


