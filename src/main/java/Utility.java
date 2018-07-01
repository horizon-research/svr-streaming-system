public class Utility {

    public static String getSegmentName(String dir, String name, int i) {
        return dir + "/" + name + "_" + Integer.toString(i) + ".mp4";
    }

    public static String getSegmentName(String dir, String name, int i, String ext) {
        return dir + "/" + name + "_" + Integer.toString(i) + "." + ext;
    }

    public static int getIdFromSegmentName(String segName) {
        int pos1 = segName.indexOf('_');
        int pos2 = segName.indexOf('.');
        String numstr = segName.substring(pos1+1, pos2);
        return Integer.parseInt(numstr);
    }
}
