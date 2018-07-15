import com.google.gson.Gson;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class manage frame rendering, video segment downloading, and all bunch
 * of fov-logic.
 */
public class VRPlayer {
    private static final int TOTAL_SEG_FRAME = 10;
    private static final int GUI_RENDER_FREQUENCY = 30;
    private static final int VRPLAYER_WIDTH = 1200;
    private static final int VRPLAYER_HEIGHT = 1200;
    private static final int SEGMENT_START_NUM = 1;
    private static final String CLIENT_MANIFEST = "manifest-client.txt";

    private String host;
    private int port;
    private String segmentPath;
    private String segFilename;
    private JFrame vrPlayerFrame = new JFrame("VRPlayer");
    private JPanel mainPanel = new JPanel();
    private JLabel iconLabel = new JLabel();
    private Timer imageRenderingTimer;
    private int currFovSegTop;      // indicate the top video segment id could be decoded
    private Manifest manifest;
    private FOVTraces fovTraces;    // use currFovSegTop to extract fov from fovTraces
    private ConcurrentLinkedQueue<BufferedImage> frameBufferQueue = new ConcurrentLinkedQueue<>();
    private NetworkHandler networkHandler;

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
                    String segFilename, String trace, Utilities.Mode mode) {
        // init vars
        this.host = host;
        this.port = port;
        this.segmentPath = segmentPath;
        this.segFilename = segFilename;
        this.currFovSegTop = SEGMENT_START_NUM;
        this.fovTraces = new FOVTraces(trace);

        File segmentDir = new File(segmentPath);
        if (!segmentDir.exists()) {
            segmentDir.mkdirs();
        }

        setupGUI();

        switch (mode) {
            case BASELINE:
                networkHandler = new BaselineNetworkHandler();
                break;
            case SVR:
                networkHandler = new SVRNetworkHandler();
                downloadAndParseManifest();
                System.out.println("[STEP 0-2] Receive manifest from VRServer");
                break;
            default:
                System.err.println("Should specify mode SVR or BASELINE");
                System.exit(1);
        }
        Thread networkThd = new Thread(networkHandler);

        // Start main thread gui timer and network handler thread
        imageRenderingTimer.start();
        networkThd.start();

        // Wait for all the worker thread to finish
        try {
            networkThd.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setupGUI() {
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
        iconLabel.setBounds(0, 0, VRPLAYER_WIDTH, VRPLAYER_HEIGHT);

        vrPlayerFrame.getContentPane().add(mainPanel, BorderLayout.CENTER);
        vrPlayerFrame.setSize(new Dimension(VRPLAYER_WIDTH, VRPLAYER_HEIGHT));
        vrPlayerFrame.setVisible(true);

        imageRenderingTimer = new Timer(GUI_RENDER_FREQUENCY, new guiTimerListener());
        imageRenderingTimer.setInitialDelay(0);
        imageRenderingTimer.setCoalesce(true);
    }

    /**
     * Download manifest file and feed it into manifest object
     */
    private void downloadAndParseManifest() {
        ManifestDownloader manifestDownloader = new ManifestDownloader(host, port, CLIENT_MANIFEST);
        try {
            manifestDownloader.request();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(CLIENT_MANIFEST));
            manifest = gson.fromJson(bufferedReader, Manifest.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * getSegFilenameFromId.
     *
     * @param id identifier of the video segment.
     * @return name of video segment.
     */
    private String getSegFilenameFromId(int id) {
        return Utilities.getSegmentName(segmentPath, segFilename, id);
    }

    private void downloadVideoSegment() {
        VideoSegmentDownloader videoSegmentDownloader =
                new VideoSegmentDownloader(host, port, segmentPath, segFilename, currFovSegTop,
                        (int) manifest.getVideoSegmentLength(currFovSegTop));
        try {
            videoSegmentDownloader.request();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadVideoSegment(int length) {
        VideoSegmentDownloader videoSegmentDownloader =
                new VideoSegmentDownloader(host, port, segmentPath, segFilename, currFovSegTop, length);
        try {
            videoSegmentDownloader.request();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private abstract class NetworkHandler implements Runnable {}

    private class BaselineNetworkHandler extends NetworkHandler {
        @Override
        public void run() {
            TCPSerializeReceiver<Long> sizeMsgRecv = new TCPSerializeReceiver<>(host, port);
            sizeMsgRecv.request();
            long size = sizeMsgRecv.getSerializeObj();

            downloadVideoSegment((int) size);
            String videoFilename = getSegFilenameFromId(currFovSegTop);
            decodeVideoSegment(videoFilename);
        }
    }

    /**
     * Download video segments following svr fov protocol.
     */
    private class SVRNetworkHandler extends NetworkHandler {
        @Override
        public void run() {
            while (currFovSegTop <= manifest.getVideoSegmentAmount()) {
                // 1. request fov with the key frame metadata from VRServer
                // TODO suppose one video segment have 10 frames temporarily, check out storage/segment.py
                int keyFrameID = (currFovSegTop - 1) * TOTAL_SEG_FRAME;
                TCPSerializeSender metadataRequest = new TCPSerializeSender<>(host, port, fovTraces.get(keyFrameID));
                metadataRequest.request();
                System.out.println("[STEP 1] SEGMENT #" + currFovSegTop + " send metadata to server");

                // 2. get response from VRServer which indicate "FULL" or "FOV"
                TCPSerializeReceiver msgReceiver = new TCPSerializeReceiver<Integer>(host, port);
                msgReceiver.request();
                int predPathMsg = (Integer) msgReceiver.getSerializeObj();
                System.out.println("[STEP 4] get size message: " + FOVProtocol.print(predPathMsg));

                // 3. download video segment from VRServer
                System.out.println("[STEP 6] download video segment from VRServer");
                downloadVideoSegment();

                // 3-1. check whether the other video frames (exclude key frame) does not match fov
                // 3-2. if any frame does not match, request full size video segment from VRServer with "BAD"
                // 3-2  if all the frames matches, send back "GOOD"
                if (FOVProtocol.isFOV(predPathMsg)) {
                    // compare all the user-fov frames exclude for key frame with the predicted fov
                    Vector<FOVMetadata> pathMetadataVec = manifest.getPredMetaDataVec().get(currFovSegTop).getPathVec();
                    FOVMetadata pathMetadata = pathMetadataVec.get(predPathMsg);
                    int secondDownloadMsg = FOVProtocol.GOOD;
                    int totalDecodedFrame = 0;
                    // TODO fov file name should be corrected after storage has been prepared
                    String videoFilename = getSegFilenameFromId(currFovSegTop);

                    // decode fov until fovTrace not match
                    try {
                        Java2DFrameConverter converter = new Java2DFrameConverter();
                        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoFilename);
                        frameGrabber.start();
                        Frame frame;
                        for (int ii = 0; ii < frameGrabber.getLengthInVideoFrames(); ii++) {
                            FOVMetadata userFov = fovTraces.get(keyFrameID);
                            double coverRatio = pathMetadata.getOverlapRate(userFov);
                            if (coverRatio < FOVProtocol.THRESHOLD) {
                                System.out.println("[DEBUG] fail at keyFrameID: " + keyFrameID);
                                System.out.println("[DEBUG] user fov: " + userFov);
                                System.out.println("[DEBUG] path metadata: " + pathMetadata);
                                System.out.println("[DEBUG] overlap ratio: " + coverRatio);
                                secondDownloadMsg = FOVProtocol.BAD;
                                break;
                            } else {
                                frameGrabber.setFrameNumber(ii);
                                frame = frameGrabber.grab();
                                BufferedImage b = converter.convert(frame);
                                if (b != null)
                                    frameBufferQueue.add(b);
                                keyFrameID++;
                                totalDecodedFrame++;
                            }
                        }
                        frameGrabber.stop();
                    } catch (FrameGrabber.Exception e) {
                        e.printStackTrace();
                    }

                    // send back GOOD for all-hit BAD for any fov-miss
                    TCPSerializeSender finRequest = new TCPSerializeSender<>(host, port, secondDownloadMsg);
                    finRequest.request();
                    System.out.println("[STEP 7] send back " + FOVProtocol.print(secondDownloadMsg));

                    // receive full size video segment if send back BAD
                    if (secondDownloadMsg == FOVProtocol.BAD) {
                        System.out.println("[STEP 10] Download full size video segment from VRServer");
                        downloadVideoSegment();

                        // TODO the file name of full size video segment is the same as fov video segment for now
                        System.out.println("[DEBUG] Start decode from frame: " + totalDecodedFrame);
                        videoFilename = getSegFilenameFromId(currFovSegTop);
                        decodeVideoSegment(videoFilename, totalDecodedFrame);
                    }
                } else if (FOVProtocol.isFull(predPathMsg)) {
                    // TODO the file name of full size video segment is the same as fov video segment for now
                    String filename = getSegFilenameFromId(currFovSegTop);
                    decodeVideoSegment(filename);
                } else {
                    // should never go here
                    assert (false);
                }

                System.out.println("---------------------------------------------------------");
                currFovSegTop++;
            }
        }
    }

    /**
     * The main thread timer that render image from frame queue.
     */
    private class guiTimerListener implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            if (!frameBufferQueue.isEmpty()) {
                BufferedImage bufferedImage = frameBufferQueue.poll();
                if (bufferedImage != null) {
                    ImageIcon icon = new ImageIcon(bufferedImage);
                    iconLabel.setIcon(icon);
//                    System.out.println("[RENDER] Render icon, picture size: " + frameBufferQueue.size());
                }
            }
        }
    }

    /**
     * Decode all the frames in the specified video segment using JavaCV
     *
     * @param path the path to the full-sized video segment
     */
    private void decodeVideoSegment(String path) {
        decodeVideoSegment(path, 0);
    }

    private void decodeVideoSegment(String path, int from) {
        Java2DFrameConverter converter = new Java2DFrameConverter();
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(path);

        try {
            frameGrabber.start();
            Frame frame;
            // double frameRate = frameGrabber.getFrameRate(); // TODO
            // System.out.println("framerate: " + frameRate);
            for (int i = from; i < frameGrabber.getLengthInVideoFrames(); i++) {
                frameGrabber.setFrameNumber(i);
                frame = frameGrabber.grab();
                BufferedImage b = converter.convert(frame);
                if (b != null)
                    frameBufferQueue.add(b);
            }
            frameGrabber.stop();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Example: java VRPlayer localhost 1988 tmp segment user-fov-trace.txt
     *
     * @param args command line args.
     */
    public static void main(String[] args) {
        VRPlayer vrPlayer = new VRPlayer(args[0],
                Integer.parseInt(args[1]),
                args[2],
                args[3],
                args[4],
                Utilities.string2mode(args[5]));
    }
}
