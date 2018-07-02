import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class VRServer implements Runnable {
    private Socket clientSock;
    private ServerSocket ss;
    private String videoSegmentDir;
    private String filename;
    private boolean hasSentManifest;
    private Manifest manifestCreator;

    /**
     * Dispatch manifest file to VRDownloader
     * @param port
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
     * Send file to the socket s
     * @param file filename of a video segment
     * @throws IOException
     */
    public void sendFile(Socket sock, String file) throws IOException {
        DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[4096];

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
            try {
                if (this.hasSentManifest) {
                    // send video segments
                    for (int i = 1; i < manifestCreator.getVideoSegmentAmount(); i++) {
                        clientSock = ss.accept();
                        sendFile(clientSock, Utilities.getSegmentName(videoSegmentDir, this.filename, i));
                    }
                } else {
                    this.hasSentManifest = true;
                    manifestCreator = new Manifest("storage/rhino/", "output", "mp4");
                    manifestCreator.write("manifest-server.txt");
                    clientSock = ss.accept();
                    sendFile(clientSock, "manifest-server.txt");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Usage java VRServer <dir> <filename>
     * The file name in the dir will be constructed as <filename>_number.mp4.
     * @param args
     */
    public static void main(String[] args) {
        VRServer vrServer = new VRServer(1988, "storage/rhino", "output");
        vrServer.run();
    }
}
