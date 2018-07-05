import java.io.IOException;
import java.net.Socket;

/**
 * A class that handles all the request to VRserver.
 */
abstract public class TCPRequest {
    private String host;
    private int port;
    private Socket clientSock;

    /**
     * Construct the client socket and connect to the server using the specified host
     * and port.
     *
     * @param host host of VRServer
     * @param port port to VRServer
     */
    public TCPRequest(String host, int port) {
        this.host = host;
        this.port = port;
        try {
            this.clientSock = new Socket(this.host, this.port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Construct the the request in server side using a known client socket.
     * and port.
     *
     */
    public TCPRequest(Socket clientSock) {
        this.clientSock = clientSock;
    }

    /**
     * Turn back a client socket.
     *
     * @return clientSock
     */
    public Socket getClientSock() {
        return clientSock;
    }

    /**
     * Sending various kind of object such as message and metadata.
     *
     * @throws IOException when socket went wrong.
     */
    abstract void request() throws IOException;
}
