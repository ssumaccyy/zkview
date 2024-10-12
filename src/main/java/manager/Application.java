package manager;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ui.ConnectDialog;
import ui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Application {

    Logger logger = LoggerFactory.getLogger(Application.class);
    public static final int X_MAIN_FRAME_WIDTH = 1200;
    public static final int Y_MAIN_FRAME_HEIGHT = 900;
    public static final int X_CONNECT_DIALOG_WIDTH = 400;
    public static final int Y_CONNECT_DIALOG_HEIGHT = 300;
    public static final int X_CONNECT_LOSE_DIALOG_WIDTH = 300;
    public static final int Y_CONNECT_LOSE_DIALOG_HEIGHT = 100;

    public static final int X_ICON_WIDTH = 32;
    public static final int Y_ICON_HEIGHT = 32;

    public static final int MAIN_FRAME_DIVIDE_LOCATION_PIXEL = 200;
    public static final int MAIN_FRAME_DIVIDE_WIDTH_PIXEL = 2;

    public ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static final List<String> blackPath = List.of("/zookeeper");

    public MainFrame mainFrame = new MainFrame(this, "ZkView");
    public ConnectDialog connectDialog = new ConnectDialog(mainFrame, true, this, "ZkView - ConnectDialog");

    // 托盘图标
    public SystemTray systemTray = SystemTray.getSystemTray();

    // 定时任务池,不需要显式的关闭
    public ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor((runnable) -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("applicationScheduledPoll-" + thread.getId());
        return thread;
    });

    public CuratorFramework curatorFramework = null;

    public Application() {
        ImageIcon imageIcon = new ImageIcon(Objects.requireNonNull(Application.class.getResource("/ico_128x128.ico")));
        TrayIcon trayIcon = new TrayIcon(imageIcon.getImage());
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("ZkView - 点击显示窗口");
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                EventQueue.invokeLater(() -> {
                    if (connectDialog.isVisible()) {
                        connectDialog.requestFocus();
                        return;
                    }
                    if (mainFrame.isVisible()) {
                        mainFrame.requestFocus();
                    }
                });
            }
        });
        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            logger.error("创建托盘图标时发生异常", e);
        }
    }

    public static void main(String[] args) {
        Application application = new Application();
        application.show();
    }

    private void show() {
        // this.mainFrame.setVisible(true);
        this.connectDialog.setAlwaysOnTop(true);
        this.connectDialog.setVisible(true);
    }

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignore) {

        }
    }

    public void stop() {
        executorService.shutdown();
        final CuratorFramework curatorFramework1 = curatorFramework;
        if (curatorFramework1 != null) {
            curatorFramework1.close();
        }
    }
}
