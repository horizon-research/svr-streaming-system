import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class VRDownloader {
    private String host;
    private int port;
    private String path;
    private String filename;
    private int snb;
    private int segLen;
    private Socket clientSock;
    private boolean dlManifest;
    private String manifestPath;

    /**
     * Setup for downloading a video segment.
     * @param port
     */
    public VRDownloader(String host, int port, String path, String filename, int snb, int segLen) {
        // init instance variables
        this.host = host;
        this.port = port;
        this.path = path;
        this.filename = filename;
        this.dlManifest = false;
        this.snb = snb;
        this.segLen = segLen;
        download();
    }

    /**
     * Setup for downloading manifest file.
     * @param host
     * @param port
     * @param path
     */
    public VRDownloader(String host, int port, String path) {
        // init instance variables
        this.host = host;
        this.port = port;
        this.manifestPath = path;
        this.dlManifest = true;
        download();
    }

    /**
     * Setup a TCP connection to VRServer and then download files from the server.
     */
    public void download() {
        try {
            clientSock = new Socket(host, port);
            if (dlManifest) {
                saveManifest(clientSock);
            } else {
                saveVideoSegment(clientSock, path, filename, snb, segLen);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save manifest from VRServer.
     * @param clientSock
     * @throws IOException
     */
    private void saveManifest(Socket clientSock) throws IOException {
        DataInputStream dis = new DataInputStream(clientSock.getInputStream());
        FileOutputStream fos = new FileOutputStream(manifestPath);
        byte[] buffer = new byte[4096];

        int read = 0;
        int totalRead = 0;
        int remaining = 1402;   // TODO how to know the manifest file size when we do not have manifest?
        while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;
            fos.write(buffer, 0, read);
        }

        fos.close();
        dis.close();
    }

    /**
     * Save video segment from VRServer.
     * @param clientSock
     * @throws IOException
     */
    private void saveVideoSegment(Socket clientSock, String path, String filename, int snb, int segLen) throws IOException {
        DataInputStream dis = new DataInputStream(clientSock.getInputStream());
        FileOutputStream fos = new FileOutputStream(Utilities.getSegmentName(path, filename, snb));
        byte[] buffer = new byte[4096];
        int read = 0;
        int totalRead = 0;
        int remaining = (int) segLen;

        System.out.println("Saving " + Utilities.getSegmentName("",filename, snb));
        while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;
            fos.write(buffer, 0, read);
        }
        System.out.println("Get " + totalRead + " bytes.");

        fos.close();
        dis.close();
    }
}
