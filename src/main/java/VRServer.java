import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

/**
 * This object is a server sending manifest file and video segments to VRPlayer.
 */
public class VRServer implements Runnable {
    private static final int BUF_SIZE = 4096;

    private ServerSocket ss;
    private String videoSegmentDir;
    private String storageFilename;
    private String predFilename;
    private boolean hasSentManifest;
    private Manifest manifest;
    private static final String manifestFileName = "manifest-server.txt";
    private Utilities.Mode mode;

    /**
     * Setup a VRServer object that waiting for connections from VRPlayer.
     *
     * @param port            port of the VRServer.
     * @param videoSegmentDir path to the storage of video segments.
     * @param storageFilename name of video segments.
     * @param predFilename    path of the object detection file.
     * @param mode            svr or baseline.
     */
    public VRServer(int port, String videoSegmentDir, String storageFilename, String predFilename, Utilities.Mode mode) {
        // init
        this.videoSegmentDir = videoSegmentDir;
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
     * Send file to the specified socket.
     *
     * @param sock socket of the client.
     * @param file storageFilename of a video segment.
     * @throws IOException when dataOutputStream fails to write or fileInputStream fails to read.
     */
    private void sendFile(Socket sock, String file) throws IOException {
        DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[BUF_SIZE];

        while (fis.read(buffer) > 0) {
            dos.write(buffer);
        }

        fis.close();
        dos.close();
    }

    /**
     * Accept the connection from VRPlayer and then send the manifest file or video segments.
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
        Socket clientSock;
        try {
            String filename = "storage/rhino.mp4";
            File file = new File(filename);
            System.out.println(filename + " size: " + file.length());

            TCPSerializeSender<Long> sizeMsgRequest = new TCPSerializeSender<>(this.ss, file.length());
            sizeMsgRequest.request();

            clientSock = ss.accept();
            sendFile(clientSock, filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runSVRProtocol() {
        while (true) {
            Socket clientSock;
            if (this.hasSentManifest) {
                // send video segments
                for (int segId = 1; segId <= manifest.getVideoSegmentAmount(); segId++) {
                    try {
                        // get user fov metadata (key frame)
                        TCPSerializeReceiver<FOVMetadata> fovMetadataTCPSerializeReceiver = new TCPSerializeReceiver<FOVMetadata>(ss);
                        fovMetadataTCPSerializeReceiver.request();

                        // inspect storage manifest to know if there is a matched video segment, if yes, send the most-match FOV, no, send FULL
                        FOVMetadata userFOVMetaData = fovMetadataTCPSerializeReceiver.getSerializeObj();
                        System.out.println("[[STEP 2 SEGMENT #" + segId + "]] Get user fov: " + userFOVMetaData);
                        Vector<FOVMetadata> pathMetadataVec = manifest.getPredMetaDataVec().get(segId).getPathVec();
                        int videoSizeMsg = FOVProtocol.FULL;
                        for (int i = 0; i < pathMetadataVec.size(); i++) {
                            FOVMetadata pathMetadata = pathMetadataVec.get(i);
                            double ratio = pathMetadata.getOverlapRate(userFOVMetaData);
                            if (ratio >= FOVProtocol.THRESHOLD) {
                                videoSizeMsg = i;
                                break;
                            }
                        }

                        TCPSerializeSender<Integer> sizeMsgRequest = new TCPSerializeSender<Integer>(this.ss, videoSizeMsg);
                        sizeMsgRequest.request();
                        System.out.println("[STEP 3] send video size msg: " + videoSizeMsg);

                        // TODO choose the right video segment to send, now always send full size video segment
                        // send video segment
                        clientSock = ss.accept();
                        String filename = Utilities.getSegmentName(videoSegmentDir, this.storageFilename, segId);
                        sendFile(clientSock, filename);
                        if (videoSizeMsg == FOVProtocol.FULL) {
                            System.out.println("[STEP 5] Send full size: " + filename + " from VRServer");
                        } else {
                            System.out.println("[STEP 5] Send fov size: " + filename + " from VRServer");
                        }

                        // wait for "GOOD" or "BAD" message from VRPlayer
                        // if GOOD: continue the next iteration
                        // if BAD: send back full size video segment
                        if (FOVProtocol.isFOV(videoSizeMsg)) {
                            TCPSerializeReceiver<Integer> finReceiver = new TCPSerializeReceiver<Integer>(ss);
                            finReceiver.request();
                            int finMsg = finReceiver.getSerializeObj();
                            System.out.println("[Step 8] Receive final message: " + FOVProtocol.print(finMsg));

                            if (finMsg == FOVProtocol.BAD) {
                                // send video segment
                                clientSock = ss.accept();
                                sendFile(clientSock, Utilities.getSegmentName(videoSegmentDir, this.storageFilename, segId));
                                System.out.println("[STEP 9] Send full size: " + filename + " from VRServer");
                            }
                        }

                        System.out.println("---------------------------------------------------------");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // create manifest file for VRServer to send to VRPlayer
                manifest = new Manifest(videoSegmentDir, predFilename);
                try {
                    manifest.write(manifestFileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // send the manifest file just created
                System.out.println("[STEP 0-1] Send manifest file to VRPlayer");
                System.out.println("Manifest file size: " + new File(manifestFileName).length());
                try {
                    clientSock = ss.accept();
                    sendFile(clientSock, manifestFileName);
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
                Utilities.string2mode(args[4]));
        vrServer.run();
    }
}
