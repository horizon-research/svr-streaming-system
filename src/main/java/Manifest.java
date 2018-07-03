import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

public class Manifest {

    private File dir;
    private File[] dirList;
    private Vector<Long> fileLenVec;

    /**
     * Get the file size of all the video segments in the specified path.
     * The filename of video segments in the path should follow the pattern: path/name_{num}.mp4
     * @param path should be a dir
     */
    public Manifest(String path, String filename, String ext) {
        // init
        dir = new File(path);
        fileLenVec = new Vector<Long>();

        // fill fileLenVec
        fileLenVec.add(-1L);
        if (dir.exists() && dir.isDirectory()) {
            dirList = dir.listFiles();
            Arrays.sort(dirList, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    String f1name = f1.getName();
                    String f2name = f2.getName();
                    return Utilities.getIdFromSegmentName(f1name) - Utilities.getIdFromSegmentName(f2name);
                }
            });
            if (dirList != null) {
                for (File f : dirList) {
                    fileLenVec.add(f.length());
                }
            } else {
                System.err.println(path + " do not have any video segments.");
                System.exit(1);
            }
        } else {
            System.err.println(path + " should be a directory!");
            System.exit(1);
        }
    }

    /**
     * Construct manifest object from file
     * @param filename is the path to the manifest file
     */
    public Manifest(String filename) {
        // init
        this.fileLenVec = new Vector<Long>();

        // fill fileLenVec
        File file = new File(filename);
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                this.fileLenVec.add(Long.parseLong(line) + 10);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write manifest to the specified file
     * @param path the path of the manifest file
     */
    public void write(String path) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(path, "UTF-8");
        for (long len : fileLenVec) {
//            System.out.println(len);
            writer.println(Long.toString(len));
        }
        writer.close();
    }

    /**
     * Get the file length of a specified video segment
     * @param i identifier of video segment
     * @return size of video segment
     */
    public long getVideoSegmentLength(int i) {
        assert (i > 0);
        return fileLenVec.get(i);
    }

    /**
     * Get the total number of video segments
     * @return the total of video segments
     */
    public int getVideoSegmentAmount() {
        return fileLenVec.size() - 1;
    }
}
