import java.io.IOException;

/**
 * For VRPlayer to download video segments.
 */
public class VideoSegmentDownloader extends TCPDownloader {
    /**
     * Setup for downloading a video segment.
     *
     * @param host     to the VRServer.
     * @param port     to the VRServer.
     * @param path     for the manifest file.
     * @param filename of the video segment.
     * @param snb      the segment number of the video segment.
     * @param segLen   the file size of the video segment.
     */
    public VideoSegmentDownloader(String host, int port, String path, String filename,
                                  int snb, int segLen) {
        // init instance variables
        super(host, port, Utilities.getSegmentName(path, filename, snb), segLen);
        try {
            request();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
