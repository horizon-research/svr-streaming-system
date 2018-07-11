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
    public static String getSegmentName(String dir, String name, int i) {
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
    public static String getSegmentName(String dir, String name, int i, String ext) {
        return dir + "/" + name + "_" + Integer.toString(i) + "." + ext;
    }

    /**
     * Extract segment id from name.
     *
     * @param segName the name of video segment.
     * @return identifier of the video segment.
     */
    public static int getIdFromSegmentName(String segName) {
        int pos1 = segName.indexOf('_');
        int pos2 = segName.indexOf('.');
        String numstr = segName.substring(pos1 + 1, pos2);
        return Integer.parseInt(numstr);
    }

    public enum Mode {
        BASELINE, SVR, NONE;
    }

    /**
     * Convert from string to mode
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
