public class FOVProtocol {
    public static final int BAD = 1001;
    public static final int GOOD = 1002;
    public static final int FULL = -1;
    // FOV is from 0~1000

    public static final int FULL_SIZE_WIDTH = 3840;
    public static final int FULL_SIZE_HEIGHT = 2160;
    public static final int FOV_SIZE_WIDTH = 1800;
    public static final int FOV_SIZE_HEIGHT = 1800;

    public static final double THRESHOLD = 0.96;

    public static String print(int code) {
        switch (code) {
            case BAD:
                return "BAD";
            case GOOD:
                return "GOOD";
            case FULL:
                return "FULL";
            default:
                if (isFOV(code)) {
                    return "FOV";
                } else {
                    return "WRONG FOV CODE";
                }
        }
    }

    public static boolean isFOV(int code) {
        return code >= 0 && code <= 1000;
    }

    public static boolean isFull(int code) {
        return code == FULL;
    }
}
