package ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class MainFrameWindowActionListener extends WindowAdapter {
    MainFrame mainFrame;

    public MainFrameWindowActionListener(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        mainFrame.stop();
        super.windowClosing(e);
    }
}
