package ui;

import manager.Application;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class ConnectLoseDialog extends JDialog {
    Application application;

    public ConnectLoseDialog(Application application, Frame owner, String title) {
        super(owner, title, true);
        this.setSize(Application.X_CONNECT_LOSE_DIALOG_WIDTH, Application.Y_CONNECT_LOSE_DIALOG_HEIGHT);
        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.application = application;

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());

        JLabel jLabel = new JLabel("*重连成功此窗口将自动消失");
        contentPanel.add(
            jLabel,
            new GridBagConstraints(
                0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(2, 2, 2, 2), 0, 0
            )
        );

        JProgressBar jProgressBar = new JProgressBar(0, 0, 100);
        jProgressBar.setIndeterminate(true);
        jProgressBar.setString("断线重连中...");
        jProgressBar.setStringPainted(true);
        contentPanel.add(
            jProgressBar,
            new GridBagConstraints(
                0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(2, 2, 2, 2), 0, 0
            )
        );

        JButton jButton = new JButton("彻底退出");
        jButton.setFocusable(false);
        contentPanel.add(
            jButton,
            new GridBagConstraints(
                0, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2), 0, 0
            )
        );

        jButton.addActionListener((e) -> {
            CompletableFuture.supplyAsync(() -> {
                EventQueue.invokeLater(() -> application.mainFrame.setVisible(false));
                return 0;
            }, application.executorService).thenApplyAsync((a) -> {
                EventQueue.invokeLater(() -> this.setVisible(false));
                return 0;
            }, application.executorService);
            application.stop();
            System.exit(0);
        });

        this.setContentPane(contentPanel);
    }
}
