import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class manage frame rendering, video segment downloading, and all bunch
 * of fov-logic.
 */
public class VRPlayer {
    public Manifest manifestCreator;
    private SegmentDecoder segmentDecoder;

    private String host;
    private int port;
    private String segmentPath;
    private String segFilename;
    private String traceFile;
    private JFrame vrPlayerFrame = new JFrame("VRPlayer");
    private JPanel mainPanel = new JPanel();
    private JLabel iconLabel = new JLabel();
    private ImageIcon icon;
    private Timer imageRenderingTimer;
    private AtomicInteger currSegTop;     // indicate the top video segment id could be decoded
    private FOVTraces fovTraces;    // use currSegTop to extract fov from fovTraces

    /**
     * Construct a VRPlayer object which manage GUI, video segment downloading, and video segment decoding
     *
     * @param host        host of VRServer.
     * @param port        port to VRServer.
     * @param segmentPath path to the storage of video segments in a temporary path like tmp/.
     * @param segFilename file name of video segment.
     * @param trace       path of a user field-of-view trace file.
     */
    public VRPlayer(String host, int port, String segmentPath,
                    String segFilename, String trace) {
        this.host = host;
        this.port = port;
        this.segmentPath = segmentPath;
        this.segFilename = segFilename;
        this.traceFile = trace;

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

        imageRenderingTimer = new Timer(15, new guiTimerListener());
        imageRenderingTimer.setInitialDelay(0);
        imageRenderingTimer.setCoalesce(false);

        // if the currSegTop is larger than decodedSegTop then we could decode segment from
        // #decodedSegTop+1 to #currSegTop
        currSegTop = new AtomicInteger(0);

        // Setup user fov trace object
        fovTraces = new FOVTraces(traceFile);

        // Download manifest file
        ManifestDownloader manifestDownloader = new ManifestDownloader(host, port, "manifest-client.txt");
        try {
            manifestDownloader.request();
        } catch (IOException e) {
            e.printStackTrace();
        }
        manifestCreator = new Manifest("manifest-client.txt");

        // While downloading video segment we use a separate thread to
        // decode the downloaded video segment using a decode worker thread.
        // TODO the decoder is not using a pure java library and there is only one decode worker thread, so should improve performance
        segmentDecoder = new SegmentDecoder(this);
        Thread decodeThd = new Thread(segmentDecoder);

        // Create network handler thread
        NetworkHandler networkHandler = new NetworkHandler();
        Thread networkThd = new Thread(networkHandler);

        decodeThd.start();
        imageRenderingTimer.start();
        networkThd.start();

        try {
            decodeThd.join();
            networkThd.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * getSegFilenameFromId.
     *
     * @param id id of video segment
     * @return name of video segment
     */
    public String getSegFilenameFromId(int id) {
        return Utilities.getSegmentName(segmentPath, segFilename, id);
    }

    /**
     * Get the largest segment id that has been downloaded.
     *
     * @return currSegTop
     */
    public int getCurrSegTop() {
        return this.currSegTop.get();
    }

    /**
     * Download video segments or sending fov metadata in a separate thread.
     */
    private class NetworkHandler implements Runnable {
        public void run() {
            Timer fovRequestTimer;

            // TODO request video segment manifest from VRServer
            // TODO the manifest should include the hierarchy of full/fov video segment
            // ex:
            // - full
            //   - 1 : length
            // - fov
            //   - 1 : coord (x, y, w, h), length

            // request video segment every tick-tock
            fovRequestTimer = new Timer(30, new fovRequestTimerListener());
            fovRequestTimer.setInitialDelay(0);
            fovRequestTimer.setCoalesce(false);
            fovRequestTimer.start();
        }
    }

    /**
     * Timer for rendering image from frame queue.
     */
    private class guiTimerListener implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            if (!segmentDecoder.getFrameQueue().isEmpty()) {
                Picture picture = segmentDecoder.getFrameQueue().poll();
                if (picture != null) {
                    BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
                    icon = new ImageIcon(bufferedImage);
                    iconLabel.setIcon(icon);
                    System.out.println("Render icon");
                }
            }
        }
    }

    /**
     * User requests for a video segment.
     */
    private class fovRequestTimerListener implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            if (currSegTop.get() < manifestCreator.getVideoSegmentAmount()) {
                // 1. request fov with the key frame metadata from VRServer
                // TODO suppose one video segment have 10 frames temporarily, check out storage/segment.py
                MetadataRequest metadataRequest = new MetadataRequest(host, port, fovTraces.get(currSegTop.get() * 10));
                metadataRequest.request();

                // 2. get response from VRServer which indicate "FULL" or "FOV"
                MsgReceiver msgReceiver = new MsgReceiver(host, port);
                msgReceiver.request();
                System.out.println(msgReceiver.getMessage());

                // 3. download video segment from VRServer
                // TODO if FOV then:
                // TODO 3-1. check whether the other video frames (exclude key frame) does not match fov
                // TODO 3-2. if any frame does not match, request full size video segment from VRServer with "BAD"
                // TODO 3-2  if all the frames matches, send back "GOOD"
                int localSegTop = currSegTop.get() + 1;
                VideoSegmentDownloader videoSegmentDownloader = new VideoSegmentDownloader(host, port, segmentPath, segFilename, localSegTop, (int) manifestCreator.getVideoSegmentLength(localSegTop));
                try {
                    videoSegmentDownloader.request();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                currSegTop.getAndSet(localSegTop);
                System.out.println("[DEBUG] currSegTop (video segment we now have downloaded): " + currSegTop.get());

                // if FULL then we are done
            }
        }
    }

    /**
     * Execute the VRplayer.
     *
     * @param args command line args.
     */
    public static void main(String[] args) {
        VRPlayer vrPlayer = new VRPlayer("localhost",
                1988,
                "tmp",
                "segment",
                "user-fov-trace.txt");
    }
}
