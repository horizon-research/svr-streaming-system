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

    /**
     * Setup a VRServer object that waiting for connections from VRPlayer.
     *
     * @param port            port of the VRServer.
     * @param videoSegmentDir path to the storage of video segments.
     * @param storageFilename name of video segments.
     * @param predFilename
     */
    public VRServer(int port, String videoSegmentDir, String storageFilename, String predFilename) {
        // init
        this.videoSegmentDir = videoSegmentDir;
        this.storageFilename = storageFilename;
        this.predFilename = predFilename;

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

        System.out.println("Send " + file + " from VRServer");

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
                        System.out.println("Get user fov: " + userFOVMetaData);
                        Vector<FOVMetadata> pathMetadataVec = manifest.getPredMetaDataVec().get(segId).getPathVec();
                        int videoSizeMsg = FOVProtocol.FULL;
                        for (int i = 0; i < pathMetadataVec.size(); i++) {
                            FOVMetadata pathMetadata = pathMetadataVec.get(i);
                            double ratio = pathMetadata.getOverlapRate(userFOVMetaData);
                            if (ratio >= FOVProtocol.THRESHOLD) {
                                videoSizeMsg = i;
                                System.out.println("[DEBUG] videoSizeMsg id: " + videoSizeMsg);
                                break;
                            }
                        }

                        TCPSerializeSender<Integer> msgRequest = new TCPSerializeSender<Integer>(this.ss, videoSizeMsg);
                        msgRequest.request();

                        // send video segment
                        clientSock = ss.accept();
                        sendFile(clientSock, Utilities.getSegmentName(videoSegmentDir, this.storageFilename, segId));

                        // wait for "GOOD" or "BAD" message from VRPlayer
                        // if GOOD: continue the next iteration
                        // if BAD: send back full size video segment
                        if (FOVProtocol.isFOV(videoSizeMsg)) {
                            TCPSerializeReceiver<Integer> finReceiver = new TCPSerializeReceiver<Integer>(ss);
                            finReceiver.request();
                            int finMsg = finReceiver.getSerializeObj();
                            System.out.println("fin message: " + FOVProtocol.print(finMsg));

                            if (finMsg == FOVProtocol.BAD) {
                                // send video segment
                                clientSock = ss.accept();
                                sendFile(clientSock, Utilities.getSegmentName(videoSegmentDir, this.storageFilename, segId));
                            }
                        }
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
     * Usage java VRServer {dir} {storageFilename}
     * The file name in the dir will be constructed as {storageFilename}_number.mp4.
     *
     * @param args command line args.
     */
    public static void main(String[] args) {
        VRServer vrServer = new VRServer(1988,
                "storage/rhino",
                "output",
                "storage/rhinos-pred.txt");
        vrServer.run();
    }
}
