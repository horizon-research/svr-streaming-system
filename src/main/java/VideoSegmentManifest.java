import com.google.gson.*;

import java.io.*;
import java.util.Arrays;
import java.util.Vector;

// TODO fix for paris and nyc

/**
 * This class handles the creation and parsing of manifest files. The manifest file includes the file size of all the
 * video segments.
 */
public class VideoSegmentManifest implements Serializable {
    private int length;
    private Vector<VideoSegmentMetaData> predMetaDataVec;

    public class VideoSegmentMetaData {
        private Vector<FOVMetadata> pathVec;

        VideoSegmentMetaData(Vector<FOVMetadata> pathVec) {
            this.pathVec = pathVec;
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
        predMetaDataVec.add(new VideoSegmentMetaData(null));

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
                //System.out.println("gg " + Utilities.getIdFromFullSizeSegmentName(f.getName()));
                predMetaDataVec.add(new VideoSegmentMetaData(fovMetadata2DVec.get(size)));
            }
        } else {
            System.err.println(storagePath + " should be a directory!");
            System.exit(1);
        }

        this.length = predMetaDataVec.size();
    }

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
                    fovMetadataVec.add(new FOVMetadata(line, FOVProtocol.FOV_SIZE_WIDTH, FOVProtocol.FOV_SIZE_HEIGHT));
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
        String full_size_path;
        String det_file;
        String out;

        if (args.length == 1) {
            full_size_path = args[0] + "-full";
            det_file = args[0] + ".txt";
            out = args[0] + "-manifest.txt";
            VideoSegmentManifest fullSizeManifest = new VideoSegmentManifest(full_size_path, det_file);
            try {
                fullSizeManifest.write(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Vector<String> name_list = new Vector<>();
            name_list.add("rhino");
            name_list.add("elephant");
            name_list.add("paris");
            name_list.add("nyc");
            name_list.add("roller");
            for (String name : name_list) {
                full_size_path = name + "-full";
                det_file = name + ".txt";
                out = name + "-manifest.txt";
                VideoSegmentManifest fullSizeManifest = new VideoSegmentManifest(full_size_path, det_file);
                try {
                    fullSizeManifest.write(out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
