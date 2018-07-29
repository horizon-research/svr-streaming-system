/**
 * For VRPlayer to download video segments.
 */
public class FOVVideoSegmentDownloader extends TCPDownloader {
    /**
     * Setup for downloading a fov video segment.
     *
     * @param host     Host of the VRServer.
     * @param port     Port of the VRServer.
     * @param path     Path of the file.
     * @param snb      The segment number of the video segment.
     * @param pathID   Path id of the fov video segment.
     * @param segLen   The file size of the video segment.
     */
    public FOVVideoSegmentDownloader(String host, int port, String path, int snb, int pathID, int segLen) {
        // init instance variables
        super(host, port, Utilities.getClientFOVSegmentName(path, snb, pathID), segLen);
    }
}
