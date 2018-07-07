public class FOVProtocol {
    public static final int BAD = 100;
    public static final int GOOD = 200;
    public static final int FULL = 300;
    public static final int FOV = 400;

    public static String print(int code) {
        switch (code) {
            case BAD:
                return "BAD";
            case GOOD:
                return "GOOD";
            case FULL:
                return "FULL";
            case FOV:
                return "FOV";
            default:
                return "WRONG-PROTOCOL-CODE";
        }
    }
}
