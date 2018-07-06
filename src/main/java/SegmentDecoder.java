import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Create a new thread to decode video segment into queue of pictures.
 */
public class SegmentDecoder implements Runnable {
    private VRPlayer vrPlayer;
    private int decodedSegTop;
    private ConcurrentLinkedQueue<Picture> frameQueue;

    /**
     * Create a video segment decoder object to execute as a separate thread so that GUI wont hang.
     *
     * @param vrPlayer reference to vrPlayer for using its currTopSeg.
     */
    public SegmentDecoder(VRPlayer vrPlayer) {
        this.decodedSegTop = 0;
        this.vrPlayer = vrPlayer;
        this.frameQueue = new ConcurrentLinkedQueue<Picture>();
    }

    /**
     * Get the concurrent frame queue that storing all the decoded video frames.
     *
     * @return frameQueue.
     */
    public ConcurrentLinkedQueue<Picture> getFrameQueue() {
        return this.frameQueue;
    }

    /**
     * Main logic of decoding "available" video segments.
     */
    public void run() {
        while (decodedSegTop < vrPlayer.manifest.getVideoSegmentAmount()) {
            int currSegTop = vrPlayer.getCurrSegTop();
            if (currSegTop > decodedSegTop) {
                System.out.println("[DEBUG] currSegTop: " + currSegTop + ", decodedSegTop: " + decodedSegTop);
                int start = decodedSegTop + 1;
                for (int i = start; i <= currSegTop; i++) {
                    String filename = vrPlayer.getSegFilenameFromId(i);
                    File file = new File(filename);
                    FrameGrab grab = null;
                    try {
                        grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
                        Picture picture;
                        while (null != (picture = grab.getNativeFrame())) {
                            System.out.println(filename + ": " + picture.getWidth() + "x" + picture.getHeight() + " " + picture.getColor());
                            frameQueue.add(picture);
                        }
                    } catch (JCodecException je1) {
                        je1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    decodedSegTop++;
                }
            }
        }
    }
}
