import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class VRPlayer implements Runnable {

    private ServerSocket ss;
    private DataInputStream dis;

    /**
     * Constructor of a VRPlayer that creating a socket for getting video segments
     * @param port
     */
    public VRPlayer(int port) {
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
                dis = new DataInputStream(clientSock.getInputStream());
                for (int i = 1; i < 10; i++) {
                    saveFile(clientSock, i);
                }
                dis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveFile(Socket clientSock, int snb) throws IOException {
        FileOutputStream fos = new FileOutputStream(Utility.getSegmentName("tmp", "segment", snb));
        System.out.println("Saving " + Utility.getSegmentName("","segment", snb));
        byte[] buffer = new byte[4096];

        int[] filesize = {248419,
                303274,
                341205,
                344802,
                319294,
                335948,
                326207,
                344720,
                308883};
        int read = 0;
        int totalRead = 0;
        int remaining = filesize[snb];
        while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;
            System.out.println("read " + totalRead + " bytes.");
            fos.write(buffer, 0, read);
        }

        // get complete filestream

        fos.close();
    }

    public static void main(String[] args) {
        VRPlayer vrPlayer = new VRPlayer(1988);
        vrPlayer.run();
    }

}
