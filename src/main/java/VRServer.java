import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Vector;

/**
 * This object is a server sending fullSizeManifest file and video segments to VRPlayer.
 */
public class VRServer implements Runnable {
    private static final String fullSizeManifestName = "server-full.txt";
    private static final String bucketName = "vros-video-segments";

    private ServerSocket ss;
    private VideoSegmentManifest fullSizeManifest;
    private Utilities.Mode mode;
    private AmazonS3 s3;
    private String manifestFilename;

    /**
     * Setup a VRServer object that waiting for connections from VRPlayer.
     *
     * @param port            Port of the VRServer.
     * @param filename        Name of the video.
     * @param mode            Choose which mode to run, SVR or BASELINE for now.
     */
    public VRServer(int port, String filename, Utilities.Mode mode) {
        // init
        this.mode = mode;
        this.s3 = new AmazonS3Client();
        this.s3.setRegion(Region.getRegion(Regions.US_EAST_1));
        this.manifestFilename = filename + "-manifest.txt";

        downloadFileFromS3ToFileSystem(manifestFilename, fullSizeManifestName);
        parseManifest(fullSizeManifestName);

        // setup a tcp server socket that waiting for sending files
        try {
            ss = new ServerSocket(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void downloadFileFromS3ToFileSystem(String key, String out) {
        S3Object s3Object = s3.getObject(new GetObjectRequest(bucketName, key));
        InputStream in = s3Object.getObjectContent();
        try {
            Files.copy(in, Paths.get(out), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseManifest(String path) {
        Gson gson = new Gson();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            fullSizeManifest = gson.fromJson(bufferedReader, VideoSegmentManifest.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Accept the connection from VRPlayer and then send the fullSizeManifest file or video segments.
     */
    public void run() {
        switch (mode) {
            case BASELINE:
                break;
            case SVR:
                runSVRProtocol();
                break;
        }
    }

    private void runSVRProtocol() {
        while (true) {
            // send video segments
            for (int segId = 1; segId <= fullSizeManifest.getVideoSegmentAmount(); segId++) {
                // Get user fov metadata (only for key frame)
                TCPSerializeReceiver<FOVMetadata> fovMetadataTCPSerializeReceiver = new TCPSerializeReceiver<>(ss);
                fovMetadataTCPSerializeReceiver.request();

                // Inspect storage fullSizeManifest to know if there is a matched video segment,
                // if yes, send the most-match FOV,
                // if no, send FULL.
                FOVMetadata userFOVMetaData = fovMetadataTCPSerializeReceiver.getSerializeObj();
                System.out.println("[[STEP 2 SEGMENT #" + segId + "]] Get user fov: " + userFOVMetaData);
                Vector<FOVMetadata> pathMetadataVec = fullSizeManifest.getPredMetaDataVec().get(segId).getPathVec();
                int sizeMsg = FOVProtocol.FULL;
                for (int i = 0; i < pathMetadataVec.size(); i++) {
                    FOVMetadata pathMetadata = pathMetadataVec.get(i);
                    double ratio = pathMetadata.getOverlapRate(userFOVMetaData);
                    if (ratio >= FOVProtocol.THRESHOLD) {
                        sizeMsg = i;
                        break;
                    }
                }

                TCPSerializeSender<Integer> pathMsgRequest = new TCPSerializeSender<>(this.ss, sizeMsg);
                pathMsgRequest.request();
                System.out.println("[STEP 3] send video path msg: " + sizeMsg);
                System.out.println("---------------------------------------------------------");
            }
            break;
        }
    }

    /**
     * Example: java VRServer 1988 storage/rhino output storage/rhinos-pred.txt
     * The file name in the storage/rhino should be constructed as {storageFilename}_number.mp4.
     *
     * @param args command line args.
     */
    public static void main(String[] args) {
        VRServer vrServer = new VRServer(Integer.parseInt(args[0]), args[1], Utilities.string2mode(args[2]));
        vrServer.run();
    }
}
