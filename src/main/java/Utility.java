public class Utility {

    public static String getSegmentName(String dir, String name, int i) {
        return dir + "/" + name + "_" + Integer.toString(i) + ".mp4";
    }

    public static String getSegmentName(String dir, String name, int i, String ext) {
        return dir + "/" + name + "_" + Integer.toString(i) + "." + ext;
    }
}
