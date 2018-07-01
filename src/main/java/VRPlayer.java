import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class VRPlayer implements Runnable {

    private ServerSocket ss;
    private DataInputStream dis;
    private int snb;
    private boolean gotManifest;
    private ManifestCreator manifestCreator;

    /**
     * Constructor of a VRPlayer that creating a socket for getting video segments.
     *
     * All the video segments and the manifest file downloaded by VRPlayer will be saved in 'tmp/'.
     * The first call of savefile is the manifest file from ContentDispatcher and the following call
     * of save file will be video segments.
     *
     * @param port
     */
    public VRPlayer(int port) {
        // init instance variables
        this.snb = 1;
        this.gotManifest = false;

        // setup tcp server socket for ContentDispatcher
        try {
            ss = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            try {
                Socket clientSock = ss.accept();
                if (this.gotManifest) {
                    saveVideoSegment(clientSock);
                } else {
                    saveManifest(clientSock);
                    this.gotManifest = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Save manifest from ContentDispatcher.
     * @param clientSock
     * @throws IOException
     */
    private void saveManifest(Socket clientSock) throws IOException {
        DataInputStream dis = new DataInputStream(clientSock.getInputStream());
        FileOutputStream fos = new FileOutputStream("manifest-client.txt");
        byte[] buffer = new byte[4096];

        int read = 0;
        int totalRead = 0;
        int remaining = 1402;
        while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;
            System.out.println("read " + totalRead + " bytes.");
            fos.write(buffer, 0, read);
        }

        this.manifestCreator = new ManifestCreator("manifest-client.txt");

        fos.close();
        dis.close();
    }

    /**
     * Save video segment from ContentDispatcher.
     * @param clientSock
     * @throws IOException
     */
    private void saveVideoSegment(Socket clientSock) throws IOException {
        DataInputStream dis = new DataInputStream(clientSock.getInputStream());
        FileOutputStream fos = new FileOutputStream(Utility.getSegmentName("tmp", "segment", snb));
        byte[] buffer = new byte[4096];
        int read = 0;
        int totalRead = 0;
        int remaining = (int) this.manifestCreator.getVideoSegmentLength(this.snb);

        System.out.println("Saving " + Utility.getSegmentName("","segment", snb));
        while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;
            System.out.println("read " + totalRead + " bytes.");
            fos.write(buffer, 0, read);
        }

        fos.close();
        dis.close();
        this.snb++;
    }

    public static void main(String[] args) {
        VRPlayer vrPlayer = new VRPlayer(1988);
        vrPlayer.run();
    }
}
