import com.google.gson.*;

import java.io.*;
import java.util.Arrays;
import java.util.Vector;

/**
 * This class handles the creation and parsing of manifest files. The manifest file includes the file size of all the
 * video segments.
 */
public class VideoSegmentManifest implements Serializable {
    private int length;
    private Vector<VideoSegmentMetaData> predMetaDataVec;

    public class VideoSegmentMetaData {
        private Vector<FOVMetadata> pathVec;
        private long size;

        VideoSegmentMetaData(Vector<FOVMetadata> pathVec, long size) {
            this.pathVec = pathVec;
            this.size = size;
        }

        public Vector<FOVMetadata> getPathVec() {
            return pathVec;
        }
    }

    /**
     * Create a manifest object using all the video segment file size and the object-predicted trace file.
     * The filename of video segments in the storagePath should follow the pattern: storagePath/name_{num}.mp4
     *
     * @param storagePath     should be a path to a directory.
     * @param predFilePath    path to a object detection file of the video.
     */
    public VideoSegmentManifest(String storagePath, String predFilePath) {
        Vector<Vector<FOVMetadata>> fovMetadata2DVec = parsePredFile(predFilePath);

        // get video segment size and feed into
        File storageDirectory = new File(storagePath);
        predMetaDataVec = new Vector<>();
        this.length = 0;

        // padding
        predMetaDataVec.add(new VideoSegmentMetaData(null, -1L));

        // Iterate whole file with the name rhino/output_xx.mp4
        if (storageDirectory.exists() && storageDirectory.isDirectory()) {
            File[] dirList = storageDirectory.listFiles();
            assert dirList != null;
            Arrays.sort(dirList, (f1, f2) -> {
                String f1name = f1.getName();
                String f2name = f2.getName();
                return Utilities.getIdFromFullSizeSegmentName(f1name) - Utilities.getIdFromFullSizeSegmentName(f2name);
            });
            for (File f : dirList) {
                int size = predMetaDataVec.size();
                predMetaDataVec.add(new VideoSegmentMetaData(fovMetadata2DVec.get(size), f.length()));
            }
        } else {
            System.err.println(storagePath + " should be a directory!");
            System.exit(1);
        }

        this.length = predMetaDataVec.size();
    }

    private int getPathLen(int segId, int pathId) { return predMetaDataVec.get(segId).getPathVec().get(pathId).getFileLength(); }

    // parse predict file metadata into fovMetadata2DVec.
    private Vector<Vector<FOVMetadata>> parsePredFile(String predFileName) {
        File predFile = new File(predFileName);
        Vector<Vector<FOVMetadata>> fovMetadata2DVec = new Vector<>();

        fovMetadata2DVec.add(null);
        if (predFile.exists()) {
            try {
                FileReader fileReader = new FileReader(predFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line;
                Vector<FOVMetadata> fovMetadataVec = new Vector<>();
                int last_id = 1;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] columns = line.split("\\s");
                    int id = Integer.parseInt(columns[0]);
                    int pathId = Integer.parseInt(columns[1]);

                    if (pathId == 0) {
                        if (id == last_id) {
                            last_id = id;
                        } else  {
                            fovMetadata2DVec.add(fovMetadataVec);
                            fovMetadataVec = new Vector<>();
                        }
                    }
                    FOVMetadata fovMD = new FOVMetadata(line, FOVProtocol.FOV_SIZE_WIDTH, FOVProtocol.FOV_SIZE_HEIGHT);
                    String filename = Utilities.getServerFOVSegmentName("storage/rhino-fov", id, pathId);
                    File vf = new File(filename);
                    int fileLen = (int) vf.length();
                    fovMD.setFileLength(fileLen);
                    fovMetadataVec.add(fovMD);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return fovMetadata2DVec;
    }

    /**
     * Write manifest to the specified file.
     *
     * @param path the path of the manifest file.
     * @throws FileNotFoundException        when the path not exists.
     * @throws UnsupportedEncodingException when utf8 not supported.
     */
    public void write(String path) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(path, "UTF-8");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        writer.write(gson.toJson(this));
        writer.flush();
        writer.close();
    }

    /**
     * Get the file length of a specified video segment.
     *
     * @param i identifier of video segment.
     * @return size of video segment.
     */
    public long getFullSizeVideoSegmentLength(int i) {
        assert (i > 0);
        return predMetaDataVec.get(i).size;
    }

    public int getFovVideoSegmentLength(int segId, int pathId) {
        assert (segId > 0 && pathId >= 0);
        return this.getPathLen(segId, pathId);
    }

    /**
     * Get the total number of video segments.
     *
     * @return the total of video segments.
     */
    public int getVideoSegmentAmount() {
        return predMetaDataVec.size() - 1;
    }


    @Override
    public String toString() {
        return "VideoSegmentManifest{" +
                "length=" + length +
                ", predMetaDataVec=" + predMetaDataVec +
                '}';
    }

    public int getLength() {
        return length;
    }

    public Vector<VideoSegmentMetaData> getPredMetaDataVec() {
        return predMetaDataVec;
    }

    /**
     * Create video segment manifest file.
     *
     * @param args Program arguments for creating video segment manifest file.
     */
    public static void main(String[] args) {
        VideoSegmentManifest fullSizeManifest = new VideoSegmentManifest("rhino-full", "rhinos-pred.txt");
        try {
            fullSizeManifest.write("rhino-manifest.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
