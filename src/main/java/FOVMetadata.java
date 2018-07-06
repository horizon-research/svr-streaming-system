import java.io.Serializable;

/**
 * Container of the fov metadata of a video segment.
 */
public class FOVMetadata implements Serializable {
    private static final int EMPTY = -1;    // VRPlayer do not need to pass pathId to server, so just leave it EMPTY in pathID
    private int id;                         // indicate the time unit
    private int pathId;                     // indicate which path or segment, start from 0, -1 indicate EMPTY
    private int x;
    private int y;
    private int width;
    private int height;

    /**
     * Construct FOVMetadata object by parsing a string line.
     *
     * @param id sequential identifier of the metadata.
     * @param line string line of user trace file.
     */
    FOVMetadata(int id, String line) {
        this.id = id;
        this.pathId = EMPTY;
        String[] columns = line.split("\\s");
        String[] coord = columns[2].split(",");
        this.x = Integer.parseInt(coord[0]);
        this.y = Integer.parseInt(coord[1]);
        this.width = Integer.parseInt(coord[2]);
        this.height = Integer.parseInt(coord[3]);
    }

    FOVMetadata(String line) {
        String[] columns = line.split("\\s");
        this.id = Integer.parseInt(columns[0]);
        this.pathId = Integer.parseInt(columns[1]);
        String[] coord = columns[2].split(",");
        this.x = Integer.parseInt(coord[0]);
        this.y = Integer.parseInt(coord[1]);
        this.width = Integer.parseInt(coord[2]);
        this.height = Integer.parseInt(coord[3]);
    }

    @Override
    public String toString() {
        return "FOVMetadata{" +
                "id=" + id +
                ", pathId=" + pathId +
                ", x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}