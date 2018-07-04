import java.io.Serializable;

/**
 * Container of the fov metadata of a video segment.
 */
public class FOVMetadata implements Serializable {
    private int x;
    private int y;
    private int conf;
    private int width;
    private int height;

    /**
     * Construct FOVMetadata object by parsing a string line.
     * @param line
     */
    FOVMetadata(String line) {
        String[] columns = line.split("\\s");
        this.conf = Integer.parseInt(columns[1]);
        String[] coord = columns[2].split(",");
        this.x = Integer.parseInt(coord[0]);
        this.y = Integer.parseInt(coord[1]);
        this.width = Integer.parseInt(coord[2]);
        this.height = Integer.parseInt(coord[3]);
    }

    @Override
    public String toString() {
        return "FOVMetadata{" +
                "x=" + x +
                ", y=" + y +
                ", conf=" + conf +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}