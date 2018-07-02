import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class VRPlayer {
    private JFrame vrPlayerFrame = new JFrame("VRDownloader");
    private JPanel mainPanel = new JPanel();
    private JPanel buttonPanel = new JPanel();
    private JLabel statLabel1 = new JLabel();
    private JLabel statLabel2 = new JLabel();
    private JLabel statLabel3 = new JLabel();
    private JLabel iconLabel = new JLabel();
    private ImageIcon icon;
    private Timer timer;
    private VRDownloader vrDownloader;
    public ManifestCreator manifestCreator;

    public VRPlayer() {
        // setup frame
        this.vrPlayerFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent windowEvent) {
                super.windowOpened(windowEvent);
            }
        });

        //Image display label
        iconLabel.setIcon(null);

        //frame layout
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        mainPanel.add(statLabel1);
        mainPanel.add(statLabel2);
        mainPanel.add(statLabel3);
        iconLabel.setBounds(0, 0, 1280, 720);

        vrPlayerFrame.getContentPane().add(mainPanel, BorderLayout.CENTER);
        vrPlayerFrame.setSize(new Dimension(1280, 720));
        vrPlayerFrame.setVisible(true);

        timer = new Timer(5, new VRPlayer.timerListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        // download video segment in a separate thread
        VRDownloader vrDownloader = new VRDownloader("localhost", 1988, "manifest-client.txt");
        Thread mthd = new Thread(vrDownloader);
        mthd.start();
        try {
            mthd.join();
            this.manifestCreator = new ManifestCreator("manifest-client.txt");
            System.out.println("Successfully get manifest from Server");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 1; i < this.manifestCreator.getVideoSegmentAmount(); i++) {
            vrDownloader = new VRDownloader("localhost", 1988, i, (int) this.manifestCreator.getVideoSegmentLength(i));
            Thread vthd = new Thread(vrDownloader);
            vthd.start();
            try {
                vthd.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        timer.start();
    }

    /**
     * Render the frames from Picture queue
     */
    private class timerListener implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
//            System.out.println("snb: " + vrDownloader.getSegmentNb());
        }
    }

    public static void main(String[] args) {
        VRPlayer vrPlayer = new VRPlayer();
        System.out.println("fff");
    }
}
