import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

/**
 * This class handles the creation and parsing of manifest files. The manifest file includes the file size of all the
 * video segments.
 */
public class Manifest implements Serializable {
    private int length;
    private Vector<VideoSegmentMetaData> fileMetadataVec;

    private class VideoSegmentMetaData {
        FOVMetadata fovMetadata;
        long size;
        VideoSegmentMetaData(FOVMetadata fovMetadata, long size) {
            this.fovMetadata = fovMetadata;
            this.size = size;
        }
    }

    /**
     * Get the file size of all the video segments in the specified storagePath.
     * The filename of video segments in the storagePath should follow the pattern: storagePath/name_{num}.mp4
     *
     * @param storagePath     should be a path to a directory.
     * @param predFilePath    path to a object detection file of the video.
     */
    public Manifest(String storagePath, String predFilePath) {
        // parse predict file
        File predFile = new File(predFilePath);
        Vector<FOVMetadata> fovMetadataVector = new Vector<FOVMetadata>();
        fovMetadataVector.add(null);
        if (predFile.exists()) {
            try {
                FileReader fileReader = new FileReader(predFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    fovMetadataVector.add(new FOVMetadata(line));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // get video segment size and feed into
        File storageDirectory = new File(storagePath);
        fileMetadataVec = new Vector<VideoSegmentMetaData>();
        this.length = 0;

        fileMetadataVec.add(new VideoSegmentMetaData(new FOVMetadata(0,"0 0 0,0,0,0"), -1L));
        if (storageDirectory.exists() && storageDirectory.isDirectory()) {
            File[] dirList = storageDirectory.listFiles();
            Arrays.sort(dirList, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    String f1name = f1.getName();
                    String f2name = f2.getName();
                    return Utilities.getIdFromSegmentName(f1name) - Utilities.getIdFromSegmentName(f2name);
                }
            });
            for (File f : dirList) {
                int size = fileMetadataVec.size();
                fileMetadataVec.add(new VideoSegmentMetaData(fovMetadataVector.get(size), f.length()));
            }
        } else {
            System.err.println(storagePath + " should be a directory!");
            System.exit(1);
        }

        this.length = fileMetadataVec.size();
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
        String manifestStr = gson.toJson(this);
        writer.write(manifestStr);
        writer.flush();
        writer.close();
    }

    /**
     * Get the file length of a specified video segment.
     *
     * @param i identifier of video segment.
     * @return size of video segment.
     */
    public long getVideoSegmentLength(int i) {
        assert (i > 0);
        return fileMetadataVec.get(i).size;
    }

    /**
     * Get the total number of video segments.
     *
     * @return the total of video segments.
     */
    public int getVideoSegmentAmount() {
        return fileMetadataVec.size() - 1;
    }
}
