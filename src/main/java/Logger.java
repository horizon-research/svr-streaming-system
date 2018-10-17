public class Logger {
    private boolean printProto;
    private boolean printLat;
    private boolean printError;

    private long start;

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

    public void startLogTime() {
        this.start = System.nanoTime();
    }

    public void endLogAndPrint() {
        long elapsed = System.nanoTime() - this.start;
        this.start = 0L;
        if (printLat) {
            System.out.println(Double.toString((double) elapsed / 1000000000.0));
        }
    }

    public void printErr(String s) {
        if (printError) {
            System.out.println(s);
        }
    }
}
