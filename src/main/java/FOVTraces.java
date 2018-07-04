import java.io.*;
import java.util.Vector;

/**
 * This class should be used by VRPlayer which includes the user fov
 * trace. Once the VRPlayer construct this class, it will use the fov
 * to request for video segment from VRServer.
 */
public class FOVTraces {
    private Vector<MetaData> fovTraces;

    /**
     * Parse the user fov trace file.
     * @param trace the path of a user fov traces file, the format
     *              of the file should be delimit by space.
     */
    public FOVTraces(String trace) {
        File file = new File(trace);
        fovTraces = new Vector<MetaData>();

        // parse file line by line
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                fovTraces.add(new MetaData(line));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This function send back the metadata object with the specified segment number.
     * @param segmentNb The identifier of video segment.
     * @return A serializable metadata object.
     */
    public MetaData get(int segmentNb) {
        return fovTraces.get(segmentNb);
    }

    /**
     * Container of the metadata of a video segment
     */
    private class MetaData implements Serializable {
        private int x;
        private int y;
        private int conf;
        private int width;
        private int height;

        /**
         * Construct MetaData object by parsing a string line.
         * @param line
         */
        MetaData(String line) {
            String[] columns = line.split("\\s");
            this.conf = Integer.parseInt(columns[1]);
            String[] coord = columns[2].split(",");
            this.x = Integer.parseInt(coord[0]);
            this.y = Integer.parseInt(coord[1]);
            this.width = Integer.parseInt(coord[2]);
            this.height = Integer.parseInt(coord[3]);
        }
    }
}
