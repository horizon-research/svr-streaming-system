import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class VRPlayer {
    private String host;
    private int port;
    private String segmentPath;
    private String segFilename;
    private JFrame vrPlayerFrame = new JFrame("VRPlayer");
    private JPanel mainPanel = new JPanel();
    private JLabel iconLabel = new JLabel();
    private ImageIcon icon;
    private Timer timer;
    private VRDownloader vrDownloader;
    private int currSegTop;     // indicate the top video segment id could be decoded
    public Manifest manifestCreator;
    private SegmentDecoder segmentDecoder;

    public VRPlayer(String host, int port, String segmentPath, String segFilename) {
        this.host = host;
        this.port = port;
        this.segmentPath = segmentPath;
        this.segFilename = segFilename;

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
        iconLabel.setBounds(0, 0, 1280, 720);

        vrPlayerFrame.getContentPane().add(mainPanel, BorderLayout.CENTER);
        vrPlayerFrame.setSize(new Dimension(1280, 720));
        vrPlayerFrame.setVisible(true);

        timer = new Timer(30, new VRPlayer.timerListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        // if the currSegTop is larger than decodedSegTop then we could decode segment from
        // #decodedSegTop+1 to #currSegTop
        currSegTop = 0;

        // download the manifest file sequentially and then create a thread to download all the video segments
        VRDownloader vrDownloader = new VRDownloader(host, port, "manifest-client.txt");
        manifestCreator = new Manifest("manifest-client.txt");
        SegmentDownloader downloader = new SegmentDownloader();
        Thread downloadThd = new Thread(downloader);
        downloadThd.start();

        // while downloading video segment we use a separate thread to decode the downloaded video segment using a
        // decode worker thread
        // TODO the decoder is not using a pure java library and there is only one decode worker thread, so should improve performance
        segmentDecoder = new SegmentDecoder(this);
        Thread decodeThd = new Thread(segmentDecoder);
        decodeThd.start();

        // render decoded frames
        timer.start();
    }

    public String getSegFilenameFromId(int id) {
        return Utilities.getSegmentName(segmentPath, segFilename, id);
    }

    public int getCurrSegTop() {
        return this.currSegTop;
    }

    /**
     * All the file transfer happens in a separate thread in this inner class.
     */
    private class SegmentDownloader implements Runnable {
        // downloading video segment in a separate thread
        public void run() {
            for (int i = 1; i < manifestCreator.getVideoSegmentAmount(); i++) {
                VRDownloader vrDownloader = new VRDownloader(host, port, segmentPath, segFilename, i, (int) manifestCreator.getVideoSegmentLength(i));
                currSegTop = i;
            }
        }
    }

    /**
     * Render the frames from frame queue
     */
    private class timerListener implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            if (!segmentDecoder.getFrameQueue().isEmpty()) {
                Picture picture = segmentDecoder.getFrameQueue().poll();
                if (picture != null) {
                    BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
                    icon = new ImageIcon(bufferedImage);
                    iconLabel.setIcon(icon);
                }
            }
        }
    }

    public static void main(String[] args) {
        VRPlayer vrPlayer = new VRPlayer("localhost", 1988, "tmp", "segment");
    }
}
