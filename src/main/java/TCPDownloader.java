import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Download specified file using TCP/IP.
 */
public class TCPDownloader extends TCPRequest {
    private String path;
    private int length;

    /**
     * Construct client socket and the filename of the downloaded file.
     *
     * @param host   host of VRServer.
     * @param port   port to VRServer.
     * @param path   the path of the downloaded file.
     * @param length the file size of the downloaded file.
     */
    public TCPDownloader(String host, int port, String path, int length) {
        super(host, port);
        this.path = path;
        this.length = length;
    }

    /**
     * Save file from VRServer.
     *
     * @throws IOException when dataInputStream fails to read data from socket or the file name in fileOutputStream
     *                     does not exists or not able to write.
     */
    public void request() throws IOException {
        DataInputStream dis = new DataInputStream(getClientSock().getInputStream());
        FileOutputStream fos = new FileOutputStream(path);
        byte[] buffer = new byte[4096];

        int read = 0;
        int totalRead = 0;
        int remaining = length;
        while ((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;
            fos.write(buffer, 0, read);
        }

        fos.close();
        dis.close();
    }
}
