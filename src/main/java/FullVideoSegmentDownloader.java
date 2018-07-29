/**
 * For VRPlayer to download video segments.
 */
public class FullVideoSegmentDownloader extends TCPDownloader {
    /**
     * Setup for downloading a video segment.
     *
     * @param host     to the VRServer.
     * @param port     to the VRServer.
     * @param path     for the manifest file.
     * @param snb      the segment number of the video segment.
     * @param segLen   the file size of the video segment.
     */
    public FullVideoSegmentDownloader(String host, int port, String path,
                                      int snb, int segLen) {
        // init instance variables
        super(host, port, Utilities.getClientFullSegmentName(path, snb), segLen);
    }
}
