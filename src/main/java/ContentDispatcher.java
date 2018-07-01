import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Vector;

public class VRServer {

    private Socket s;
    private String videoSegmentDir;
    private String filename;
    private int snb;
    private long filelength;
    private DataOutputStream dos;

    /**
     * @param host
     * @param port
     * @param path
     * @param filename-
     */
    public VRServer(String host, int port, String path, String filename, int snb) {
        // init
        this.videoSegmentDir = path;
        this.filename = filename;
        this.snb = snb;

        File file = new File(Utility.getSegmentName(videoSegmentDir, this.filename, snb));
        this.filelength = file.length();
        System.out.println(this.filelength);

        // setup tcp file transfer
        try {
            s = new Socket(host, port);
            sendFile(Utility.getSegmentName(videoSegmentDir, this.filename, this.snb));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Send file to the socket s
     * @param file filename of a video segment
     * @throws IOException
     */
    public void sendFile(String file) throws IOException {
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[4096];

        System.out.println("sendfile");

        while (fis.read(buffer) > 0) {
            dos.write(buffer);
        }

        fis.close();
        dos.close();
    }

    /**
     * Usage java VRServer <directory> <filename>
     *     the file name in the directory will be constructed as <filename>_number.mp4
     * @param args
     */
    public static void main(String[] args) {
        File dir = new File(args[0]);
        if (dir.exists() && dir.isDirectory()) {
            for (int i = 1; i < 10; i++) {
                VRServer vrServer = new VRServer("localhost", 1988, args[0], args[1], i);
            }
        }
    }
}
