import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.SocketHandler;

/**
 * This class is for sending user fov serializeObj to server.
 */
public class TCPSerializeRequest<T> extends TCPRequest {
    private T serializeObj;
    private ObjectOutputStream out;

    /**
     * Construct the client socket and connect to the server using the
     * specified host and port.
     *
     * @param host         host of VRServer.
     * @param port         port to VRServer.
     * @param serializeObj serializeObj class that containing field-of-view info.
     */
    public TCPSerializeRequest(String host, int port, T serializeObj) {
        super(host, port);
        this.serializeObj = serializeObj;

        try {
            out = new ObjectOutputStream(getClientSock().getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TCPSerializeRequest(ServerSocket ss, T serializeObj) {
        super(ss);
        this.serializeObj = serializeObj;

        try {
            out = new ObjectOutputStream(getClientSock().getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * send serializeObj object.
     */
    public void request() {
        try {
            out.writeObject(serializeObj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
