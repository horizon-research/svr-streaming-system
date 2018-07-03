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
     * Send file to the specified socket s.
     *
     * @param sock socket of the client.
     * @param file filename of a video segment.
     * @throws IOException when dataOutputStream fails to write or fileInputStream fails to read.
     */
    public void sendFile(Socket sock, String file) throws IOException {
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
            if (this.hasSentManifest) {
                // send video segments
                for (int i = 1; i <= manifestCreator.getVideoSegmentAmount(); i++) {
                    try {
                        clientSock = ss.accept();
                        sendFile(clientSock, Utilities.getSegmentName(videoSegmentDir, this.filename, i));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                this.hasSentManifest = true;
                manifestCreator = new Manifest("storage/rhino/", "output", "mp4");
                try {
                    manifestCreator.write("manifest-server.txt");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    clientSock = ss.accept();
                    sendFile(clientSock, "manifest-server.txt");
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
