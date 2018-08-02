import java.io.IOException;
import java.nio.file.*;

public class PlayNative {

    /**
     * Execute a process that make use of native TX2 decoder to play a video
     * segment for a specifing frame range.
     *
     * @param name        The relative path of the video file.
     * @param start_frame The frame number that we want to start with.
     * @param end_frame   The index of frame that we want to stop for.
     */
    public PlayNative(String name, int start_frame, int end_frame) {
        String start = Integer.toString(start_frame);
        String end = Integer.toString(end_frame);

        //System.out.println(getFileUriFromName(name));
        try {
            Process nativePlayerProc = new ProcessBuilder()
                    .command("./render", getFileUriFromName(name), start, end)
                    .inheritIO()
                    .start();
            nativePlayerProc.waitFor();
        } catch (IOException | InterruptedException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    private static String getFileUriFromName(String name) {
        String protocol = "file://";
        Path currentRelativePath = Paths.get("");
        String pwd = currentRelativePath.toAbsolutePath().toString();
        StringBuilder sb = new StringBuilder();
        sb.append(protocol).append(pwd).append("/").append(name);
        return sb.toString();
    }


    /**
     * Just for test.
     */
    public static void main(String[] args) {
        PlayNative play =
                new PlayNative("out.mp4", 10, 20);
    }
}