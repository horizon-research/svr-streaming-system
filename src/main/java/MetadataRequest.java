import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * This class is for sending user fov metadata to server.
 */
public class MetadataRequest extends TCPRequest {
    private FOVMetadata metadata;
    private ObjectOutputStream out;

    /**
     * Construct the client socket and connect to the server using the
     * specified host and port.
     *
     * @param host host of VRServer.
     * @param port port to VRServer.
     * @param metadata metadata class that containing field-of-view info.
     */
    public MetadataRequest(String host, int port, FOVMetadata metadata) {
        super(host, port);
        this.metadata = metadata;

        try {
            out = new ObjectOutputStream(getClientSock().getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * send metadata object to VRServer.
     */
    public void request() {
        try {
            out.writeObject(metadata);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
