import java.io.*;
import java.util.Iterator;
import java.util.Vector;

/**
 * This class should be used by VRPlayer which includes the user fov
 * trace. Once the VRPlayer construct this class, it will use the fov
 * to request for video segment from VRServer.
 */
public class FOVTraces implements Iterable<FOVMetadata> {
    private Vector<FOVMetadata> fovTraces;

    /**
     * Parse the user fov trace file.
     * @param trace the path of a user fov traces file, the format
     *              of the file should be delimit by space.
     */
    public FOVTraces(String trace) {
        File file = new File(trace);
        fovTraces = new Vector<FOVMetadata>();

        // parse file line by line
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                fovTraces.add(new FOVMetadata(line));
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
    public FOVMetadata get(int segmentNb) {
        return fovTraces.get(segmentNb);
    }

    public Iterator<FOVMetadata> iterator() {
        return fovTraces.iterator();
    }
}
