import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Create a new thread to decode video segment into queue of pictures
 */
public class SegmentDecoder implements Runnable {
    private VRPlayer vrPlayer;
    private int decodedSegTop;
    private ConcurrentLinkedQueue<Picture> frameQueue;

    public SegmentDecoder(VRPlayer vrPlayer) {
        this.decodedSegTop = 0;
        this.vrPlayer = vrPlayer;
        this.frameQueue = new ConcurrentLinkedQueue<Picture>();
    }

    public ConcurrentLinkedQueue<Picture> getFrameQueue() {
        return this.frameQueue;
    }

    public void run() {
        while (decodedSegTop < vrPlayer.manifestCreator.getVideoSegmentAmount()) {
            if (vrPlayer.getCurrSegTop() != 0) {
                if (vrPlayer.getCurrSegTop() > decodedSegTop) {
                    int start = decodedSegTop + 1;
                    int end = vrPlayer.getCurrSegTop();
                    for (int i = start; i <= end; i++) {
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
}
