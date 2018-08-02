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
    private int fileLength;

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
        this.fileLength = -1;
    }

    /**
     * Construct FOVMetadata object by parsing a string line with specified fov width and height to configure.
     *
     * @param line      string line of user trace file.
     * @param width     fov width.
     * @param height    fov height.
     */
    FOVMetadata(String line, int width, int height) {
        String[] columns = line.split("\\s");
        this.id = Integer.parseInt(columns[0]);
        this.pathId = Integer.parseInt(columns[1]);
        String[] coord = columns[2].split(",");

        int temp_width = Integer.parseInt(coord[2]);
        int temp_height = Integer.parseInt(coord[3]);

        this.x = Integer.parseInt(coord[0]) + ((temp_width - width) / 2);
        this.y = Integer.parseInt(coord[1]) + ((temp_height - height) / 2);

        // WARNING: workarounds, should be same as how fov video is created.
        if (this.x < 0) {
            this.x = FOVProtocol.FULL_SIZE_WIDTH + x;
            if (this.x > FOVProtocol.FULL_SIZE_WIDTH - width) {
                this.x = FOVProtocol.FULL_SIZE_WIDTH - width;
            }
        }
        if (this.x + width > FOVProtocol.FULL_SIZE_WIDTH) {
            this.x = FOVProtocol.FULL_SIZE_WIDTH - width;
        }
        if (this.y + height > FOVProtocol.FULL_SIZE_HEIGHT - height) {
            this.y = FOVProtocol.FULL_SIZE_HEIGHT - height;
        }

        this.width = width;
        this.height = height;
        this.fileLength = -1;
    }

    /**
     * Set fov file length for the fov object.
     *
     * @param len Length of a fov video segment.
     */
    public void setFileLength(int len) {
        this.fileLength = len;
    }

    /**
     * Compute the overlap ratio of two viewport.
     *
     * @param other the other fov metadata object.
     * @return overlap ratio.
     */
    public double getOverlapRate(FOVMetadata other) {
        double total_x = 0;
        double self_rightmost = this.x + this.width;
        double other_rightmost = other.x + other.width;

        if (self_rightmost > FOVProtocol.FULL_SIZE_WIDTH) {
            if (other_rightmost > FOVProtocol.FULL_SIZE_WIDTH) {
                double left_1 = 0;
                double right_1 = Math.min(self_rightmost - FOVProtocol.FULL_SIZE_WIDTH, other_rightmost - FOVProtocol.FULL_SIZE_WIDTH);
                total_x += right_1 - left_1;
                double left_2 = Math.max(this.x, other.x);
                double right_2 = FOVProtocol.FULL_SIZE_WIDTH;
                total_x += right_2 - left_2;
            } else if (other_rightmost >= 0) {
                if (this.x + this.width - FOVProtocol.FULL_SIZE_WIDTH > other.x) {
                    double left = other.x;
                    double right = Math.min(self_rightmost - FOVProtocol.FULL_SIZE_WIDTH, other_rightmost);
                    total_x += right - left;
                }
                if (other.x + other.width > this.x) {
                    double left = Math.max(this.x, other.x);
                    double right = Math.min(FOVProtocol.FULL_SIZE_WIDTH, other_rightmost);
                    total_x += right - left;
                }
            } else {
                assert (false);
            }
        } else if (self_rightmost >= 0) {
            if (other_rightmost > FOVProtocol.FULL_SIZE_WIDTH) {
                if (this.x < (other_rightmost - FOVProtocol.FULL_SIZE_WIDTH)) {
                    double left_1 = Math.max(this.x, 0);
                    double right_1 = Math.min(self_rightmost, other_rightmost - FOVProtocol.FULL_SIZE_WIDTH);
                    total_x += right_1 - left_1;
                }
                if (this.x + this.width > other.x) {
                    double left_2 = other.x;
                    total_x += self_rightmost - left_2;
                }
            } else if (other_rightmost >= 0) {
                double left = Math.max(this.x, other.x);
                double right = Math.min(self_rightmost, other_rightmost);
                if (right - left > 0) {
                    total_x = right - left;
                }
            } else {
                assert (false);
            }
        } else {
            assert (false);
        }

        double bottom = Math.max(this.y, other.y);
        double top = Math.min(this.y + this.height, other.y + other.height);
        double total_y = Math.abs(top - bottom) > other.height ? other.height : Math.abs(top - bottom);
        total_x = Math.abs(total_x);
        double ratio = (total_x * total_y) / (other.width * other.height);
        if (ratio > 1.0) {
            System.out.println("total_x: " + total_x);
            System.out.println("this: " + this.toString());
            System.out.println("other: " + other.toString());
            assert (false);
        }
        return ratio;
    }

    public int getFileLength() {
        return this.fileLength;
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