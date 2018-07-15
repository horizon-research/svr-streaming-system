import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;

public class TCPFileSender extends TCPRequest {
    private static final int BUF_SIZE = 4096;
    private String filename;

    /**
     * Send file using the constructed server socket.
     *
     * @param ss        socket of the client.
     * @param filename  storageFilename of a video segment.
     */
    public TCPFileSender(ServerSocket ss, String filename) {
        super(ss);
        this.filename = filename;
    }

    @Override
    void request() throws IOException {
        DataOutputStream dos = new DataOutputStream(getClientSock().getOutputStream());
        FileInputStream fis = new FileInputStream(filename);
        byte[] buffer = new byte[BUF_SIZE];

        while (fis.read(buffer) > 0) {
            dos.write(buffer);
        }

        fis.close();
        dos.close();
    }
}
