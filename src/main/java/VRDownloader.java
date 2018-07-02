import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class VRDownloader implements Runnable {
    private ServerSocket ss;
    private Socket clientSock;
    private int segmentNb;
    private boolean gotManifest;
    private ManifestCreator manifestCreator;

    /**
     * Constructor of a VRDownloader that creating a socket for getting video segments.
     *
     * All the video segments and the manifest file downloaded by VRDownloader will be saved in 'tmp/'.
     * The first call of savefile is the manifest file from ContentDispatcher and the following call
     * of save file will be video segments.
     *
     * @param port
     */
    public VRDownloader(int port) {
        // init instance variables
        this.segmentNb = 1;
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
                clientSock = ss.accept();
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

    public int getSegmentNb() {
        return this.segmentNb;
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
        int remaining = 1402;   // TODO how to know the manifest file size when we do not have manifest?
        while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;
            fos.write(buffer, 0, read);
        }

        this.manifestCreator = new ManifestCreator("manifest-client.txt");
        System.out.println("Successfully get manifest from Server");

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
        FileOutputStream fos = new FileOutputStream(Utilities.getSegmentName("tmp", "segment", segmentNb));
        byte[] buffer = new byte[4096];
        int read = 0;
        int totalRead = 0;
        int remaining = (int) this.manifestCreator.getVideoSegmentLength(this.segmentNb);

        System.out.println("Saving " + Utilities.getSegmentName("","segment", segmentNb));
        while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;
            fos.write(buffer, 0, read);
        }
        System.out.println("Get " + totalRead + " bytes.");

        /*
         * In this point we can decode the video segment into a queue and then
         * render it from the timerListener.
         * So I'm going to use a worker thread to produce picture data from
         * the video segment.
         */

        fos.close();
        dis.close();
        this.segmentNb++;
    }
}
