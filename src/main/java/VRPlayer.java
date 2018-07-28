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
    private static final String CLIENT_MANIFEST = "manifest-client.txt";

    private String host;
    private int port;
    private String segmentPath;
    private String segFilename;
    private int currFovSegTop;      // indicate the top video segment id could be decoded
    private Manifest manifest;
    private FOVTraces fovTraces;    // use currFovSegTop to extract fov from fovTraces

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

    private void BaselineNetworkHandler() {
        TCPSerializeReceiver<Long> sizeMsgRecv = new TCPSerializeReceiver<>(host, port);
        sizeMsgRecv.request();
        long size = sizeMsgRecv.getSerializeObj();

        downloadVideoSegment((int) size);
        String videoFilename = getSegFilenameFromId(currFovSegTop);
        // TODO decodeVideoSegment(videoFilename);
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

                // TODO decode fov until fovTrace not match

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
                    // TODO decodeVideoSegment(videoFilename, totalDecodedFrame);
                }
            } else if (FOVProtocol.isFull(predPathMsg)) {
                // TODO the file name of full size video segment is the same as fov video segment for now
                String filename = getSegFilenameFromId(currFovSegTop);
                // TODO decodeVideoSegment(filename);
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
                args[4],
                Utilities.string2mode(args[5]));
    }
}
