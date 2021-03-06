/**
 * Include all the static utility methods for this project.
 */
public class Utilities {

    /**
     * Return the name of video segment with the pattern: dir/name#i.mp4
     *
     * @param dir  the path of video segment.
     * @param name the filename of video segment.
     * @param i    sequential id of video segment.
     * @return name of the video segment.
     */
    public static String getServerFullSizeSegmentName(String dir, String name, int i) {
        return dir + "/" + name + "_" + Integer.toString(i) + ".mp4";
    }

    /**
     * Return the name of video segment with the pattern: dir/name#i.ext
     *
     * @param dir  the path of video segment.
     * @param name the filename of video segment.
     * @param i    sequential id of video segment.
     * @param ext  file extension of the video segment.
     * @return name of the video segment.
     */
    public static String getServerFullSizeSegmentName(String dir, String name, int i, String ext) {
        return dir + "/" + name + "_" + Integer.toString(i) + "." + ext;
    }

    /**
     * Extract segment id from name.
     *
     * @param segName the name of video segment.
     * @return identifier of the video segment.
     */
    public static int getIdFromFullSizeSegmentName(String segName) {
        int pos1 = segName.indexOf('_');
        int pos2 = segName.indexOf('.');
        String numStr = segName.substring(pos1 + 1, pos2);
        return Integer.parseInt(numStr);
    }

    /**
     * Return the fov video segment name.
     *
     * @param dir    the path of video segment.
     * @param id     segment id.
     * @param pathid path id.
     * @return
     */
    public static String getServerFOVSegmentName(String dir, int id, int pathid) {
        return dir + "/" + Integer.toString(id) + "/" + Integer.toString(pathid) + ".mp4";
    }

    public static String getClientFullSegmentName(String dir, int id) {
        return dir + "/" + Integer.toString(id) + ".mp4";
    }

    public static String getClientFOVSegmentName(String dir, int id, int pathid) {
        return dir + "/" + Integer.toString(id) + "_" + Integer.toString(pathid) + ".mp4";
    }

    public enum Mode {
        BASELINE, SVR, NONE;
    }

    /**
     * Convert from string to mode
     *
     * @param str string could only be BASELINE or SVR.
     * @return mode.
     */
    public static Mode string2mode(String str) {
        if (str.equals("SVR")) {
            return Mode.SVR;
        } else if (str.equals("BASELINE")) {
            return Mode.BASELINE;
        } else {
            return Mode.NONE;
        }
    }
}
