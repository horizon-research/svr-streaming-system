import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Receive message from a tcp connection.
 */
public class MsgReceiver extends TCPRequest {
    private ObjectInputStream in;
    private String message;

    /**
     * Setup the env for receiving message from a tcp end point.
     * @param host host of VRServer
     * @param port port to VRServer
     */
    public MsgReceiver(String host, int port) {
        super(host, port);
        message = null;

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
            message = (String) in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the message we got from request() instance method.
     * @return message from request()
     */
    public String getMessage() {
        assert message != null;
        return message;
    }
}
