package ui;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class ConnectDialogWindowAdaptor extends WindowAdapter {

    ConnectDialog connectDialog;

    public ConnectDialogWindowAdaptor(ConnectDialog connectDialog) {
        this.connectDialog = connectDialog;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        if (connectDialog.block) {
            JOptionPane.showMessageDialog(connectDialog, "正在进行网络连接,稍后再试...", "ZkView", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            int choose = JOptionPane.showConfirmDialog(connectDialog, "确认退出", "退出确认", JOptionPane.YES_NO_OPTION);
            if (choose != JOptionPane.YES_OPTION) {
                return;
            }
            super.windowClosing(e);
            System.exit(0);
        }
    }
}
