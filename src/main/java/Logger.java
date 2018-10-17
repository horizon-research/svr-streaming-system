public class Logger {
    private boolean printProto;
    private boolean printLat;
    private boolean printError;

    public Logger(boolean printProto, boolean printLat, boolean printError) {
        this.printProto = printProto;
        this.printLat = printLat;
        this.printError = printError;
    }

    public void printProtocol(String s) {
        if (printProto) {
            System.out.println(s);
        }
    }

    public void printLatency(String s) {
        if (printLat) {
            System.out.println(s);
        }
    }

    public void printErr(String s) {
        if (printError) {
            System.out.println(s);
        }
    }
}
