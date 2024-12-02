package ui;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import dao.TreeNode;
import dao.ZkNodePack;
import manager.Application;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.IconHelper;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainFrame extends JFrame {
    protected Logger logger = LoggerFactory.getLogger(MainFrame.class);

    Application application;
    String title;

    JSplitPane contentPanel;

    JScrollPane left;

    JPanel right;

    public TreeNode root;

    public JTree jTree;

    JTextField nodePathTextField;

    JTextArea nodeContent;
    JTextArea nodeState;

    public ConnectLoseDialog connectLoseDialog;

    public Charset charset = StandardCharsets.UTF_8;

    public JProgressBar jProgressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);

    final JSONConfig jsonConfig = JSONConfig.create().setOrder(true);

    public MainFrame(Application application, String title) throws HeadlessException {
        this.application = application;
        this.title = title;
        this.setTitle(title);
        this.setLocationByPlatform(true);
        this.setSize(Application.X_MAIN_FRAME_WIDTH, Application.Y_MAIN_FRAME_HEIGHT);
        this.setIconImage(IconHelper.ImageTransparentProduce(Application.X_ICON_WIDTH, Application.Y_ICON_HEIGHT));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setMinimumSize(new Dimension(Application.X_MAIN_FRAME_WIDTH, Application.Y_MAIN_FRAME_HEIGHT));
        this.addWindowListener(new MainFrameWindowActionListener(this));

        connectLoseDialog = new ConnectLoseDialog(application, this, "ZkView - Connect Lost");
        connectLoseDialog.setVisible(false);

        contentPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        contentPanel.setEnabled(false);
        contentPanel.setDividerLocation(Application.MAIN_FRAME_DIVIDE_LOCATION_PIXEL);
        contentPanel.setDividerSize(Application.MAIN_FRAME_DIVIDE_WIDTH_PIXEL);
        root = new TreeNode("root");
        root.setOverrideString("/");
        TreeNode animal = new TreeNode("b");
        animal.insert(new TreeNode("tiger"));
        animal.insert(new TreeNode("lion"));
        animal.insert(new TreeNode("chicken"));
        TreeNode bird = new TreeNode("bird");
        bird.insert(new TreeNode("yanZi"));
        bird.insert(new TreeNode("maQue"));
        animal.insert(bird);
        root.insert(animal);

        jTree = new JTree(root);
        jTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        jTree.setEditable(false);
        jTree.addTreeSelectionListener((event) -> {
            final Object lastPath = event.getPath().getLastPathComponent();
            if (lastPath instanceof TreeNode) {
                final String selectPath = ((TreeNode) lastPath).getPathNotNull();
                nodePathTextField.setText(selectPath);
                nodeState.setText("");
                logger.trace("jTree new Select Path: {}", selectPath);
            } else {
                logger.warn("jTree produce unexpected type tree path {}", lastPath.getClass().getCanonicalName());
            }
        });
        left = new JScrollPane(jTree);
        left.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        left.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        left.setPreferredSize(new Dimension(400, Application.Y_MAIN_FRAME_HEIGHT));

        right = new JPanel();
        GridBagLayout gridBagLayout = new GridBagLayout();

        JButton btnCreate = new JButton("新建空节点");
        JButton btnViewCurrentSelect = new JButton("查看节点内容");
        JButton btnSave = new JButton("保存节点内容");
        JButton btnClear = new JButton("清空文本");
        JButton btnDelNode = new JButton("删除节点");
        JButton btnReload = new JButton("重载所有节点");
        JButton btnJsonCompress = new JButton("json压缩");
        JButton btnJsonExpand = new JButton("json展开");
        JButton btnNodeExport = new JButton("节点数据导出");
        LinkedHashMap<JButton, ButtonCmd> btnMap = new LinkedHashMap<>(6);
        btnMap.put(btnCreate, ButtonCmd.ButtonCreate);
        btnMap.put(btnViewCurrentSelect, ButtonCmd.ButtonView);
        btnMap.put(btnNodeExport, ButtonCmd.ButtonNodeExport);
        btnMap.put(btnSave, ButtonCmd.ButtonSave);
        btnMap.put(btnDelNode, ButtonCmd.ButtonDel);
        btnMap.put(btnReload, ButtonCmd.ButtonReload);
        btnMap.put(btnJsonCompress, ButtonCmd.ButtonJsonCompress);
        btnMap.put(btnClear, ButtonCmd.ButtonClear);
        btnMap.put(btnJsonExpand, ButtonCmd.ButtonJsonExpand);

        btnMap.forEach((k, v) -> k.addActionListener((e) -> action(v.cmd)));

        right.setLayout(gridBagLayout);
        GridBagConstraints gridBagConstraintsForNodePath = new GridBagConstraints(
            0,
            0,
            GridBagConstraints.REMAINDER,
            1,
            0,
            0,
            GridBagConstraints.NORTHWEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(2, 2, 1, 2),
            0,
            0
        );

        nodePathTextField = new JTextField();
        nodePathTextField.setEditable(false);
        right.add(nodePathTextField, gridBagConstraintsForNodePath);

        GridBagConstraints gridBagConstraintsForTextFieldNodeContent = new GridBagConstraints(
            0,
            1,
            GridBagConstraints.REMAINDER,
            2,
            0,
            0.5,
            GridBagConstraints.NORTHWEST,
            GridBagConstraints.BOTH,
            new Insets(1, 2, 1, 2),
            0,
            0
        );

        nodeContent = new JTextArea();
        JScrollPane jScrollPane = new JScrollPane(nodeContent, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        nodeContent.setLineWrap(false);
        nodeContent.setEditable(true);
        right.add(jScrollPane, gridBagConstraintsForTextFieldNodeContent);

        GridBagConstraints gridBagConstraintsForTextFieldNodeStat = new GridBagConstraints(
            0,
            3,
            GridBagConstraints.REMAINDER,
            2,
            0,
            0.1,
            GridBagConstraints.SOUTHWEST,
            GridBagConstraints.BOTH,
            new Insets(1, 2, 1, 2),
            0,
            0
        );

        nodeState = new JTextArea();
        JScrollPane jScrollPane2 = new JScrollPane(nodeState, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        nodeState.setLineWrap(false);
        nodeState.setEditable(false);
        right.add(jScrollPane2, gridBagConstraintsForTextFieldNodeStat);

        final List<Map.Entry<JButton, ButtonCmd>> keySet = new LinkedList<>(btnMap.entrySet());
        final int keySetSize = keySet.size();
        for (int i = 0; i < keySetSize; i++) {
            Map.Entry<JButton, ButtonCmd> entry = keySet.get(i);
            GridBagConstraints gridBagConstraintsForBtn = new GridBagConstraints(
                i,
                5,
                1,
                1,
                0.4,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(1, 1, 1, 1),
                0,
                0
            );
            right.add(entry.getKey(), gridBagConstraintsForBtn);
        }

        right.add(
            jProgressBar,
            new GridBagConstraints(
                0, 6, keySetSize, 1, 0, 0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0
            )
        );

        contentPanel.add(JSplitPane.LEFT, left);
        contentPanel.add(JSplitPane.RIGHT, right);

        this.setContentPane(contentPanel);

        JComponent glassPanel = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 128));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        glassPanel.setVisible(false);
        this.setGlassPane(glassPanel);

    }

    protected void action(String actionCode) {
        if (ButtonCmd.ButtonView.cmd.compareToIgnoreCase(actionCode) == 0) {
            actForBtnView();
            return;
        }
        if (ButtonCmd.ButtonDel.cmd.compareToIgnoreCase(actionCode) == 0) {
            actForBtnDel();
            return;
        }

        if (ButtonCmd.ButtonJsonExpand.cmd.compareToIgnoreCase(actionCode) == 0) {
            actForBtnJsonExpand();
            return;
        }

        if (ButtonCmd.ButtonJsonCompress.cmd.compareToIgnoreCase(actionCode) == 0) {
            actForBtnJsonCompress();
            return;
        }
        if (ButtonCmd.ButtonClear.cmd.compareToIgnoreCase(actionCode) == 0) {
            actForBtnClear();
            return;
        }
        if (ButtonCmd.ButtonSave.cmd.compareToIgnoreCase(actionCode) == 0) {
            actForBtnSave();
            return;
        }
        if (ButtonCmd.ButtonCreate.cmd.compareToIgnoreCase(actionCode) == 0) {
            actForCreate();
            return;
        }
        if (ButtonCmd.ButtonReload.cmd.compareToIgnoreCase(actionCode) == 0) {
            actForBtnReload();
            return;
        }
        if (ButtonCmd.ButtonNodeExport.cmd.compareToIgnoreCase(actionCode) == 0) {
            actForNodeExport();
            return;
        }
        logger.warn("action {} not caught", actionCode);
    }

    protected void actForNodeExport() {
        TreePath select = jTree.getSelectionPath();
        if (select == null) {
            JOptionPane.showMessageDialog(this, "暂未选中任何节点", "ZkView", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Object selectObj = select.getLastPathComponent();
        if (!(selectObj instanceof TreeNode)) {
            JOptionPane.showMessageDialog(this, "内部错误:读取选中节点返回了非预期的类型:" + selectObj.getClass().getCanonicalName(), "ZkView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        TreeNode treeNode = (TreeNode) selectObj;
        String path = treeNode.getPathNotNull();

        final CuratorFramework curatorFramework = application.curatorFramework;
        if (curatorFramework == null || curatorFramework.getState() != CuratorFrameworkState.STARTED) {
            JOptionPane.showMessageDialog(this, "zookeeper没有准备好", "ZkView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setDialogTitle("导出到文件:");
        jFileChooser.setMultiSelectionEnabled(false);
        jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jFileChooser.setSelectedFile(new File(path.replaceAll("/", "_")));
        int operation = jFileChooser.showDialog(this, "保存");
        if (operation != JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(this, "导出取消", "ZkView", JOptionPane.PLAIN_MESSAGE);
            return;
        }

        File file = jFileChooser.getSelectedFile();
        CompletableFuture.supplyAsync(() -> {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file, false);
                byte[] data = curatorFramework.getData().forPath(path);
                fileOutputStream.write(data);
                fileOutputStream.flush();
                fileOutputStream.close();
                return false;
            } catch (FileNotFoundException fileNotFoundException) {
                EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, "无法写入文件或者无法创建文件", "ZkView", JOptionPane.ERROR_MESSAGE));
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, application.executorService).whenCompleteAsync(
            (doNotNeedNotify, exception) -> {
                if (exception != null) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, true);
                    exception.printStackTrace(printWriter);
                    EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, byteArrayOutputStream.toString(), "ZkView", JOptionPane.ERROR_MESSAGE));
                }
                if (!doNotNeedNotify) {
                    EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, "成功保存至文件：" + file.getAbsolutePath(), "ZkView", JOptionPane.PLAIN_MESSAGE));
                }
            }, application.executorService
        );

    }

    protected void actForBtnView() {
        TreePath select = jTree.getSelectionPath();
        if (select == null) {
            JOptionPane.showMessageDialog(this, "暂未选中任何节点", "ZkView", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Object selectObj = select.getLastPathComponent();
        if (!(selectObj instanceof TreeNode)) {
            JOptionPane.showMessageDialog(this, "内部错误:读取选中节点返回了非预期的类型:" + selectObj.getClass().getCanonicalName(), "ZkView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        TreeNode treeNode = (TreeNode) selectObj;
        String path = treeNode.getPathNotNull();

        final CuratorFramework curatorFramework = application.curatorFramework;
        if (curatorFramework == null || curatorFramework.getState() != CuratorFrameworkState.STARTED) {
            JOptionPane.showMessageDialog(this, "zookeeper没有准备好", "ZkView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                Stat stat = curatorFramework.checkExists().forPath(path);
                if (stat == null) {
                    return null;
                }
                return new ZkNodePack(stat, curatorFramework.getData().forPath(path));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }, application.executorService).thenApplyAsync(
            (pack) -> {
                byte[] data = pack.data;
                if (data == null) {
                    EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, "节点不存在", "ZkView", JOptionPane.WARNING_MESSAGE));
                }
                return pack;
            }, application.executorService
        ).whenCompleteAsync((pack, exception) -> {
            var optionalBytes = pack.data;
            String nodeContentText = new String(optionalBytes, charset);
            var stat = pack.stat;
            var nodeStatText = String.format(
                "节点创建事务Id: %d\n节点最后更新事务Id:%d\n节点创建时间：%s\n节点最后修改时间：%s\n节点数据长度：%d\n子节点数量：%d\n节点数据版本号：%d\n节点子节点版本号：%d\n" +
                    "节点子节点配置版本号：%d\n节点临时会话Id：%d",
                stat.getCzxid(),
                stat.getMzxid(),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(stat.getCtime()), ZoneId.systemDefault())),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(stat.getMtime()), ZoneId.systemDefault())),
                stat.getDataLength(),
                stat.getNumChildren(),
                stat.getVersion(),
                stat.getAversion(),
                stat.getCversion(),
                stat.getEphemeralOwner()
            );
            EventQueue.invokeLater(() -> {
                nodeContent.setText(nodeContentText);
                nodeState.setText(nodeStatText);
                jProgressBar.setValue(100);
            });
            application.scheduledExecutorService.schedule(() -> EventQueue.invokeLater(() -> jProgressBar.setValue(0)), 500, TimeUnit.MILLISECONDS);
        }, application.executorService).whenCompleteAsync(
            (data, exception) -> {
                if (exception != null) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, true);
                    exception.printStackTrace(printWriter);

                    EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, byteArrayOutputStream.toString(), "ZkView - Error", JOptionPane.ERROR_MESSAGE));
                }

            }, application.executorService
        );
    }

    protected void actForCreate() {
        TreePath selectPath = jTree.getSelectionPath();
        if (selectPath == null) {
            JOptionPane.showMessageDialog(this, "没有选择任何节点", "ZkView", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Object selectObj = selectPath.getLastPathComponent();
        if (!(selectObj instanceof TreeNode)) {
            logger.error("selectObj is not TreeNode but {}", selectObj.getClass().getCanonicalName());
            return;
        }
        TreeNode selectNode = (TreeNode) selectObj;
        String parentPath = selectNode.getPathNotNull();
        String nodeName = nodeContent.getText();
        if (!nodeName.matches("[a-zA-Z0-9_\\-:]+")) {
            JOptionPane.showMessageDialog(this, "节点名无效", "ZkView", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String newNodePath = parentPath + "/" + nodeName;
        if (parentPath.compareTo("/") == 0) {
            newNodePath = "/" + nodeName;
        }
        final String finalNodePath = newNodePath;
        if (Application.blackPath.stream().anyMatch(finalNodePath::startsWith)) {
            JOptionPane.showMessageDialog(this, "不支持修改安全目录", "ZkView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final CuratorFramework curatorFramework = application.curatorFramework;
        if (curatorFramework == null || curatorFramework.getState() != CuratorFrameworkState.STARTED) {
            JOptionPane.showMessageDialog(this, "zookeeper没有准备好", "ZkView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Stat stat = curatorFramework.checkExists().forPath(finalNodePath);
                if (stat != null) {
                    EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, "节点已存在", "ZkView", JOptionPane.ERROR_MESSAGE));
                    return;
                }
                curatorFramework.create().forPath(finalNodePath, new byte[]{});
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, application.executorService).whenCompleteAsync(
            (_t, exception) -> {
                if (exception != null) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, true);
                    exception.printStackTrace(printWriter);
                    EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, byteArrayOutputStream.toString(), "ZkView - Error", JOptionPane.ERROR_MESSAGE));
                } else {
                    selectNode.insert(new TreeNode(nodeName));
                    EventQueue.invokeLater(() -> {
                        jTree.updateUI();
                        JOptionPane.showMessageDialog(this, "创建成功:" + finalNodePath, "ZkView", JOptionPane.INFORMATION_MESSAGE);
                    });
                }
            },
            application.executorService
        );

    }

    protected void actForBtnDel() {
        TreePath selectPath = jTree.getSelectionPath();
        if (selectPath == null) {
            JOptionPane.showMessageDialog(this, "没有选择任何节点", "ZkView", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Object selectObj = selectPath.getLastPathComponent();
        if (!(selectObj instanceof TreeNode)) {
            logger.error("selectObj is not TreeNode but {}", selectObj.getClass().getCanonicalName());
            return;
        }
        TreeNode selectNode = (TreeNode) selectObj;
        TreeNode parent = selectNode.getParent();
        int selectRow = jTree.getMinSelectionRow();
        String nodePath = selectNode.getPathNotNull();
        if (nodePath.compareTo("/") == 0) {
            JOptionPane.showMessageDialog(this, "非法操作", "ZkView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (Application.blackPath.stream().anyMatch(nodePath::startsWith)) {
            JOptionPane.showMessageDialog(this, "不支持修改安全目录", "ZkView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final CuratorFramework curatorFramework = application.curatorFramework;
        if (curatorFramework == null || curatorFramework.getState() != CuratorFrameworkState.STARTED) {
            JOptionPane.showMessageDialog(this, "zookeeper没有准备好", "ZkView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Stat stat = curatorFramework.checkExists().forPath(nodePath);
                if (stat == null) {
                    EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, "节点不存在", "ZkView", JOptionPane.INFORMATION_MESSAGE));
                    return;
                }
                curatorFramework.delete().deletingChildrenIfNeeded().forPath(nodePath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, application.executorService).whenCompleteAsync((_t, exception) -> {
            if (exception != null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, true);
                exception.printStackTrace(printWriter);

                EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, byteArrayOutputStream.toString(), "ZkView - Error", JOptionPane.ERROR_MESSAGE));
            } else {

                EventQueue.invokeLater(() -> {
                    if (parent == null) {
                        root = null;
                        logger.trace("delete node: root");
                        jTree.setModel(null);
                        jTree.updateUI();
                    } else {
                        parent.remove(selectNode);
                        jTree.setSelectionRow(selectRow - 1);
                        jTree.updateUI();
                        logger.trace("del node: {}", nodePath);
                    }
                    JOptionPane.showMessageDialog(this, "删除成功:" + nodePath, "ZkView", JOptionPane.INFORMATION_MESSAGE);
                });
            }
        }, application.executorService);


    }

    protected void actForBtnJsonCompress() {
        final String text = nodeContent.getText();
        CompletableFuture.supplyAsync(() -> {
            if (text.startsWith("[")) {
                final JSONArray jsonArray = new JSONArray(text, jsonConfig);
                return jsonArray.toJSONString(0);
            } else {
                final JSONObject jsonObject = new JSONObject(text, jsonConfig);
                return jsonObject.toJSONString(0);
            }
        }, application.executorService).whenCompleteAsync((json, exception) -> {
            if (exception != null) {
                EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, "不是有效的JSON字符串", "ZkView", JOptionPane.ERROR_MESSAGE));
                return;
            }
            nodeContent.setText(json);
        }, application.executorService);
    }

    protected void actForBtnReload() {

        final CuratorFramework curatorFramework = application.curatorFramework;
        if (curatorFramework == null || curatorFramework.getState() != CuratorFrameworkState.STARTED) {
            JOptionPane.showMessageDialog(this, "", "ZkView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        this.setEnabled(false);
        final ScheduledFuture<?> scheduledFuture = application.scheduledExecutorService.scheduleWithFixedDelay(() -> EventQueue.invokeLater(() -> {
            final int nowV = jProgressBar.getValue();
            if (nowV < 100) {
                jProgressBar.setValue(Integer.min(100, jProgressBar.getValue() + 2));
            }
        }), 0, 100, TimeUnit.MILLISECONDS);
        CompletableFuture.supplyAsync(() -> {
            EventQueue.invokeLater(() -> {
                this.getGlassPane().setVisible(true);
                jProgressBar.setValue(0);
            });
            // region 读取zk里的数据生成Tree
            TreeNode root = new TreeNode("root");
            root.setOverrideString("/");
            Queue<TreeNode> queue = new LinkedList<>();
            queue.offer(root);
            try {
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
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return root;
            // endregion
        }, application.executorService).whenCompleteAsync((treeNode, exception) -> {
            scheduledFuture.cancel(false);
            EventQueue.invokeLater(() -> {
                jTree.clearSelection();
                nodePathTextField.setText("");
                nodeContent.setText("");
                nodeState.setText("");
            });
            if (exception != null) {
                logger.error("重载全部节点发生异常", exception);
                EventQueue.invokeLater(() -> {
                    jProgressBar.setValue(0);
                    this.getGlassPane().setVisible(false);
                });
            }
            if (treeNode != null) {
                EventQueue.invokeLater(() -> {
                    jTree.setModel(new DefaultTreeModel(treeNode));
                    jTree.updateUI();
                    this.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "重载成功", "ZkView", JOptionPane.INFORMATION_MESSAGE);
                });
                // 不能合到1一起,因为showMessageDialog会卡线程
                EventQueue.invokeLater(() -> {
                    jProgressBar.setValue(100);
                    this.getGlassPane().setVisible(false);
                });
                application.scheduledExecutorService.schedule(() -> jProgressBar.setValue(0), 700, TimeUnit.MILLISECONDS);
            }

        }, application.executorService);


    }

    protected void actForBtnSave() {
        TreePath select = jTree.getSelectionPath();
        if (select == null) {
            JOptionPane.showMessageDialog(this, "暂未选中任何节点", "ZkView", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Object selectObj = select.getLastPathComponent();
        if (!(selectObj instanceof TreeNode)) {
            JOptionPane.showMessageDialog(this, "内部错误:读取选中节点返回了非预期的类型:" + selectObj.getClass().getCanonicalName(), "ZkView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        TreeNode treeNode = (TreeNode) selectObj;
        String path = treeNode.getPathNotNull();
        final CuratorFramework curatorFramework = application.curatorFramework;
        if (curatorFramework == null || curatorFramework.getState() != CuratorFrameworkState.STARTED) {
            JOptionPane.showMessageDialog(this, "zookeeper没有准备好", "ZkView", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (Application.blackPath.stream().anyMatch(path::startsWith)) {
            JOptionPane.showMessageDialog(this, "不支持修改安全目录", "ZkView", JOptionPane.ERROR_MESSAGE);
            return;
        }
        CompletableFuture.runAsync(() -> {
            byte[] data = nodeContent.getText().getBytes(charset);
            try {
                curatorFramework.setData().forPath(path, data);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, application.executorService).whenCompleteAsync((_t, exception) -> {
            if (exception == null) {
                EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, "保存成功", "ZkView", JOptionPane.INFORMATION_MESSAGE));
            } else {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, true);
                exception.printStackTrace(printWriter);
                JOptionPane.showMessageDialog(this, byteArrayOutputStream.toString(), "ZkView - Error", JOptionPane.ERROR_MESSAGE);
            }
        }, application.executorService);


    }

    protected void actForBtnJsonExpand() {
        final String text = nodeContent.getText();
        CompletableFuture.supplyAsync(() -> {
            if (text.startsWith("[")) {
                final JSONArray jsonArray = new JSONArray(text, jsonConfig);
                return jsonArray.toJSONString(2);
            } else {
                final JSONObject jsonObject = new JSONObject(text, jsonConfig);
                return jsonObject.toJSONString(2);
            }
        }, application.executorService).whenCompleteAsync((json, exception) -> {
            if (exception != null) {
                EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, "不是有效的JSON字符串", "ZkView", JOptionPane.ERROR_MESSAGE));
                return;
            }
            nodeContent.setText(json);
        }, application.executorService);
    }

    protected void actForBtnClear() {
        nodeContent.setText("");
        nodeState.setText("");
    }

    void stop() {
        application.stop();
    }
}
