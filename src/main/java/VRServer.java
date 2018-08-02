import java.io.*;
import java.net.ServerSocket;
import java.util.Vector;

/**
 * This object is a server sending fullSizeManifest file and video segments to VRPlayer.
 */
public class VRServer implements Runnable {
    private static final String fullSizeManifestName = "server-full.txt";
    private ServerSocket ss;
    private String fullSegmentDir;
    private String fovSegmentDir;
    private String storageFilename;
    private String predFilename;
    private boolean hasSentManifest;
    private VideoSegmentManifest fullSizeManifest;
    private Utilities.Mode mode;

    /**
     * Setup a VRServer object that waiting for connections from VRPlayer.
     *
     * @param port            port of the VRServer.
     * @param fullSegmentDir  path to the storage of full size video segments.
     * @param fovSegmentDir   path to the storage of fov video segments.
     * @param storageFilename name of video segments.
     * @param predFilename    path of the object detection file.
     * @param mode            svr or baseline.
     */
    public VRServer(int port, String fullSegmentDir, String fovSegmentDir,
                    String storageFilename, String predFilename,
                    Utilities.Mode mode) {
        // init
        this.fullSegmentDir = fullSegmentDir;
        this.fovSegmentDir = fovSegmentDir;
        this.storageFilename = storageFilename;
        this.predFilename = predFilename;
        this.mode = mode;

        // setup a tcp server socket that waiting for sending files
        try {
            ss = new ServerSocket(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Accept the connection from VRPlayer and then send the fullSizeManifest file or video segments.
     */
    public void run() {
        switch (mode) {
            case BASELINE:
                runBaselineMode();
                break;
            case SVR:
                runSVRProtocol();
                break;
        }
    }

    private void runBaselineMode() {
        try {
            String filename = "storage/rhino.mp4";
            File file = new File(filename);
            System.out.println(filename + " size: " + file.length());

            TCPSerializeSender<Long> sizeMsgRequest = new TCPSerializeSender<>(this.ss, file.length());
            sizeMsgRequest.request();

            TCPFileSender tcpFileSender = new TCPFileSender(ss, filename);
            tcpFileSender.request();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runSVRProtocol() {
        while (true) {
            if (this.hasSentManifest) {
                // send video segments
                for (int segId = 1; segId <= fullSizeManifest.getVideoSegmentAmount(); segId++) {
                    // Get user fov metadata (only for key frame)
                    TCPSerializeReceiver<FOVMetadata> fovMetadataTCPSerializeReceiver = new TCPSerializeReceiver<>(ss);
                    fovMetadataTCPSerializeReceiver.request();

                    // Inspect storage fullSizeManifest to know if there is a matched video segment,
                    // if yes, send the most-match FOV,
                    // if no, send FULL.
                    FOVMetadata userFOVMetaData = fovMetadataTCPSerializeReceiver.getSerializeObj();
                    System.out.println("[[STEP 2 SEGMENT #" + segId + "]] Get user fov: " + userFOVMetaData);
                    Vector<FOVMetadata> pathMetadataVec = fullSizeManifest.getPredMetaDataVec().get(segId).getPathVec();
                    int sizeMsg = FOVProtocol.FULL;
                    for (int i = 0; i < pathMetadataVec.size(); i++) {
                        FOVMetadata pathMetadata = pathMetadataVec.get(i);
                        double ratio = pathMetadata.getOverlapRate(userFOVMetaData);
                        if (ratio >= FOVProtocol.THRESHOLD) {
                            sizeMsg = i;
                            break;
                        }
                    }

                    TCPSerializeSender<Integer> pathMsgRequest = new TCPSerializeSender<>(this.ss, sizeMsg);
                    pathMsgRequest.request();
                    System.out.println("[STEP 3] send video path msg: " + sizeMsg);
                    System.out.println("---------------------------------------------------------");
                }
            } else {
                // create and write manifest file
                fullSizeManifest = new VideoSegmentManifest(fullSegmentDir, predFilename);
                try {
                    fullSizeManifest.write(fullSizeManifestName);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // send the manifest file just created
                System.out.println("[STEP 0-1] Send fullSizeManifest file to VRPlayer");
                System.out.println("VideoSegmentManifest file size: " + new File(fullSizeManifestName).length());
                try {
                    File file = new File(fullSizeManifestName);
                    TCPSerializeSender<Integer> manifestLenSender = new TCPSerializeSender<>(this.ss, (int) file.length());
                    manifestLenSender.request();

                    TCPFileSender tcpFileSender = new TCPFileSender(ss, fullSizeManifestName);
                    tcpFileSender.request();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                this.hasSentManifest = true;
            }
        }
    }

    /**
     * Example: java VRServer 1988 storage/rhino output storage/rhinos-pred.txt
     * The file name in the storage/rhino should be constructed as {storageFilename}_number.mp4.
     *
     * @param args command line args.
     */
    public static void main(String[] args) {
        VRServer vrServer = new VRServer(Integer.parseInt(args[0]),
                args[1],
                args[2],
                args[3],
                args[4],
                Utilities.string2mode(args[5]));
        vrServer.run();
    }
}
