import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;

/**
 * Receive serializeObj from a tcp connection.
 */
public class TCPSerializeReceiver<T> extends TCPRequest {
    private ObjectInputStream in;
    private T serializeObj;

    /**
     * Setup the env for receiving serializeObj from a tcp end point.
     *
     * @param host host of VRServer
     * @param port port to VRServer
     */
    public TCPSerializeReceiver(String host, int port) {
        super(host, port);
        serializeObj = null;

        try {
            in = new ObjectInputStream(getClientSock().getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Setup the env for receiving serializeObj from a tcp end point.
     *
     * @param ss an already initiated server socket that is waiting for client socket.
     */
    public TCPSerializeReceiver(ServerSocket ss) {
        super(ss);
        serializeObj = null;

        try {
            in = new ObjectInputStream(getClientSock().getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Receive a string object from the tcp connection.
     */
    void request() {
        try {
            serializeObj = (T) in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the serializeObj we got from request() instance method.
     *
     * @return serializeObj from request()
     */
    public T getSerializeObj() {
        assert serializeObj != null;
        return serializeObj;
    }
}
