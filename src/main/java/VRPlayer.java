import com.google.gson.Gson;

import java.io.*;
import java.util.Vector;

/**
 * This class manage frame rendering, video segment downloading, and all bunch
 * of fov-logic.
 */
public class VRPlayer {
    private static final int TOTAL_SEG_FRAME = 10;
    private static final int SEGMENT_START_NUM = 1;
    private static final String CLIENT_FULLSIZE_MANIFEST = "client-full.txt";
    private static int FRAME_PER_VIDEO_SEGMENT = 20;
    private static String FULL_SIZE_SEG_NAME = "full";
    private static String FOV_SEG_NAME = "fov";

    private String host;
    private int port;
    private String segmentPath;
    private String segFilename;
    private int currFovSegTop;      // indicate the top video segment id could be decoded
    private VideoSegmentManifest manifest;
    private FOVTraces fovTraces;    // use currFovSegTop to extract fov from fovTraces

    /**
     * Construct a VRPlayer object which manage GUI, video segment downloading, and video segment decoding
     *
     * @param host        host of VRServer.
     * @param port        port to VRServer.
     * @param segmentPath path to the storage of video segments in a temporary path like tmp/.
     * @param trace       path of a user field-of-view trace file.
     */
    public VRPlayer(String host, int port, String segmentPath, String trace, Utilities.Mode mode) {
        // init vars
        this.host = host;
        this.port = port;
        this.segmentPath = segmentPath;
        this.currFovSegTop = SEGMENT_START_NUM;
        this.fovTraces = new FOVTraces(trace);

        File segmentDir = new File(segmentPath);
        if (!segmentDir.exists()) {
            segmentDir.mkdirs();
        }

        switch (mode) {
            case BASELINE:
                BaselineNetworkHandler();
                break;
            case SVR:
                downloadAndParseManifest();
                SVRNetworkHandler();
                System.out.println("[STEP 0-2] Receive manifest from VRServer");
                break;
            default:
                System.err.println("Should specify mode SVR or BASELINE");
                System.exit(1);
        }
    }

    /**
     * Download manifest file and feed it into manifest object
     */
    private void downloadAndParseManifest() {
        ManifestDownloader manifestDownloader = new ManifestDownloader(host, port, CLIENT_FULLSIZE_MANIFEST);
        try {
            manifestDownloader.request();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(CLIENT_FULLSIZE_MANIFEST));
            manifest = gson.fromJson(bufferedReader, VideoSegmentManifest.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String getFullSegFilenameFromId(int id) {
        return Utilities.getServerFullSizeSegmentName(segmentPath, FULL_SIZE_SEG_NAME, id);
    }

    private void downloadFullSizeVideoSegment() {
        FullVideoSegmentDownloader videoSegmentDownloader =
                new FullVideoSegmentDownloader(host, port, segmentPath, currFovSegTop,
                        (int) manifest.getFullSizeVideoSegmentLength(currFovSegTop));
        try {
            videoSegmentDownloader.request();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadFullSizeVideoSegment(int length) {
        FullVideoSegmentDownloader videoSegmentDownloader =
                new FullVideoSegmentDownloader(host, port, segmentPath, currFovSegTop, length);
        try {
            videoSegmentDownloader.request();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadFOVVideoSegment(int pathId, int length) {
        FOVVideoSegmentDownloader videoSegmentDownloader =
                new FOVVideoSegmentDownloader(host, port, segmentPath, currFovSegTop, pathId, length);
        try {
            videoSegmentDownloader.request();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void BaselineNetworkHandler() {
        TCPSerializeReceiver<Long> sizeMsgRecv = new TCPSerializeReceiver<>(host, port);
        sizeMsgRecv.request();
        long size = sizeMsgRecv.getSerializeObj();

        downloadFullSizeVideoSegment((int) size);
        String videoFilename = getFullSegFilenameFromId(currFovSegTop);
        PlayNative play =
                new PlayNative(videoFilename, 0, -1);
    }

    /**
     * Download video segments following svr fov protocol.
     */
    private void SVRNetworkHandler() {
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

            // 3-1. check whether the other video frames (exclude key frame) does not match fov
            // 3-2. if any frame does not match, request full size video segment from VRServer with "BAD"
            // 3-2  if all the frames matches, send back "GOOD"
            if (FOVProtocol.isFOV(predPathMsg)) {
                System.out.println("[STEP 6] download video segment from VRServer");
                downloadFOVVideoSegment(predPathMsg, manifest.getFovVideoSegmentLength(currFovSegTop, predPathMsg));

                // compare all the user-fov frames exclude for key frame with the predicted fov
                Vector<FOVMetadata> pathMetadataVec = manifest.getPredMetaDataVec().get(currFovSegTop).getPathVec();
                FOVMetadata pathMetadata = pathMetadataVec.get(predPathMsg);
                int secondDownloadMsg = FOVProtocol.GOOD;
                int totalDecodedFrame = 0;
                String videoFilename = Utilities.getClientFOVSegmentName(segmentPath, currFovSegTop, predPathMsg);

                // Iterate fov until fovTrace not match
                for (int i = 0; i < FRAME_PER_VIDEO_SEGMENT; i++) {
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
                        keyFrameID++;
                        totalDecodedFrame++;
                    }
                }
                new PlayNative(videoFilename, 0, totalDecodedFrame - 1);

                // send back GOOD for all-hit BAD for any fov-miss
                TCPSerializeSender finRequest = new TCPSerializeSender<>(host, port, secondDownloadMsg);
                finRequest.request();
                System.out.println("[STEP 7] send back " + FOVProtocol.print(secondDownloadMsg));

                // receive full size video segment if send back BAD
                if (secondDownloadMsg == FOVProtocol.BAD) {
                    System.out.println("[STEP 10] Download full size video segment from VRServer");
                    downloadFullSizeVideoSegment();

                    System.out.println("[DEBUG] Start decode from frame: " + totalDecodedFrame);
                    videoFilename = Utilities.getClientFullSegmentName(segmentPath, currFovSegTop);
                    new PlayNative(videoFilename, totalDecodedFrame, -1);
                }
            } else if (FOVProtocol.isFull(predPathMsg)) {
                System.out.println("[STEP 6] download video segment from VRServer");
                downloadFullSizeVideoSegment();
                String filename = Utilities.getClientFullSegmentName(segmentPath, currFovSegTop);
                new PlayNative(filename, 0, -1);
            } else {
                // should never go here
                assert (false);
            }

            System.out.println("---------------------------------------------------------");
            currFovSegTop++;
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
                Utilities.string2mode(args[4]));
    }
}
