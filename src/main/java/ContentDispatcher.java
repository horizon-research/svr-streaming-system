import java.io.*;
import java.net.Socket;

public class ContentDispatcher {

    private Socket s;
    private String videoSegmentDir;
    private String filename;
    private int snb;
    private long filelength;
    private DataOutputStream dos;

    /**
     * Dispatch video segments to VRPlayer
     * @param host
     * @param port
     * @param path
     * @param filename
     */
    public ContentDispatcher(String host, int port, String path, String filename, int snb) {
        // init
        this.videoSegmentDir = path;
        this.filename = filename;
        this.snb = snb;

        File file = new File(Utilities.getSegmentName(videoSegmentDir, this.filename, snb));
        this.filelength = file.length();
        System.out.println(this.filelength);

        // setup tcp file transfer
        try {
            s = new Socket(host, port);
            sendFile(Utilities.getSegmentName(videoSegmentDir, this.filename, this.snb));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Dispatch manifest file to VRPlayer
     * @param host
     * @param port
     * @param path
     */
    public ContentDispatcher(String host, int port, String path) {
        // setup tcp file transfer
        try {
            s = new Socket(host, port);
            sendFile(path);
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
     * Usage java ContentDispatcher <dir> <filename>
     * The file name in the dir will be constructed as <filename>_number.mp4.
     * @param args
     */
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        ManifestCreator manifestCreator = new ManifestCreator("storage/rhino/", "output", "mp4");
        manifestCreator.write("manifest-server.txt");
        File dir = new File(args[0]);

        if (dir.exists() && dir.isDirectory()) {
            // send manifest
            ContentDispatcher manifestServer = new ContentDispatcher("localhost", 1988, "manifest-server.txt");

            // send video segments
            for (int i = 1; i < manifestCreator.getVideoSegmentAmount(); i++) {
                ContentDispatcher vsServer = new ContentDispatcher("localhost", 1988, args[0], args[1], i);
            }
        }
    }
}
