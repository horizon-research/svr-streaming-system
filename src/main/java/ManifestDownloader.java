/**
 * This class is for downloading manifest file using TCP/IP.
 */
public class ManifestDownloader extends TCPDownloader {

    /**
     * Setup manifest downloader.
     *
     * @param host host of VRServer.
     * @param port port to VRServer.
     * @param path the location where the manifest file will be stored.
     */
    public ManifestDownloader(String host, int port, String path) {
        super(host, port, path, 321795);
    }
}
