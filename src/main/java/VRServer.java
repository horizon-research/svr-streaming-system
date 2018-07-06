import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This object is a server sending manifest file and video segments to VRPlayer.
 */
public class VRServer implements Runnable {
    private static final int BUF_SIZE = 4096;

    private Socket clientSock;
    private ServerSocket ss;
    private String videoSegmentDir;
    private String filename;
    private boolean hasSentManifest;
    private Manifest manifestCreator;

    /**
     * Setup a VRServer object that waiting for connections from VRPlayer.
     *
     * @param port            port of the VRServer.
     * @param videoSegmentDir path to the storage of video segments.
     * @param filename        name of video segments.
     */
    public VRServer(int port, String videoSegmentDir, String filename) {
        // init
        this.videoSegmentDir = videoSegmentDir;
        this.filename = filename;

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
     * @param file filename of a video segment.
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

    private FOVMetadata getMetaData() {
        ObjectInputStream in = null;
        FOVMetadata fovMetadata = null;
        try {
            clientSock = ss.accept();
            in = new ObjectInputStream(clientSock.getInputStream());
            try {
                fovMetadata = (FOVMetadata) in.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fovMetadata;
    }

    /**
     * Accept the connection from VRPlayer and then send the manifest file or video segments.
     */
    public void run() {
        while (true) {
            if (this.hasSentManifest) {
                // send video segments
                for (int i = 1; i <= manifestCreator.getVideoSegmentAmount(); i++) {
                    try {
                        // get user fov metadata (key frame)
                        FOVMetadata fovMetadata = getMetaData();
                        System.out.println(fovMetadata);

                        // TODO inspect storage to know if there is a matched video segment, if yes, send FOV, no, send FULL
                        // now just send FULL since we only have full size segment
                        TCPSerializeSender<String> msgRequest = new TCPSerializeSender<String>(this.ss, "FULL");
                        msgRequest.request();

                        // send video segment
                        clientSock = ss.accept();
                        sendFile(clientSock, Utilities.getSegmentName(videoSegmentDir, this.filename, i));

                        // TODO wait for "GOOD" or "BAD"
                        // TODO GOOD: continue the next iteration
                        // TODO BAD: send back full size video segment
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // TODO manifest file now is only lines of file size, and should make it like:
                // ex:
                // - full
                //   - 1 : length
                // - fov
                //   - 1 : coord (x, y, w, h), length

                // create manifest file for VRServer to send to VRPlayer
                manifestCreator = new Manifest("storage/rhino/", "storage/rhinos-pred.txt");
                try {
                    manifestCreator.write("manifest-server.txt");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // send the manifest file just created
                System.out.println("Manifest file size: " + new File("manifest-server.txt").length());
                try {
                    this.clientSock = ss.accept();
                    sendFile(clientSock, "manifest-server.txt");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                this.hasSentManifest = true;
            }
        }
    }

    /**
     * Usage java VRServer {dir} {filename}
     * The file name in the dir will be constructed as {filename}_number.mp4.
     *
     * @param args command line args.
     */
    public static void main(String[] args) {
        VRServer vrServer = new VRServer(1988, "storage/rhino", "output");
        vrServer.run();
    }
}
