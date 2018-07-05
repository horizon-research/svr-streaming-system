import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class MsgRequest extends TCPRequest {
    private String message;
    private ObjectOutputStream out;

    /**
     * Construct the client socket and connect to the server using the specified host
     * and port.
     *
     * @param host host of VRServer
     * @param port port to VRServer
     */
    public MsgRequest(String host, int port, String message) {
        super(host, port);
        this.message = message;

        try {
            out = new ObjectOutputStream(getClientSock().getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MsgRequest(Socket clientSocket, String message) {
        super(clientSocket);
        this.message = message;

        try {
            out = new ObjectOutputStream(getClientSock().getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * send fov protocol message.
     */
    void request() {
        try {
            out.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
