package minecrafttransportsimulator.rendering;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class responsible for parsing GIF images into their rendered form.  No clue how this works.  This should
 * really be a built-in function of the built-in libraries...
 *
 * @author don_bruce
 */
public class GIFParser {

    public static ParsedGIF parseGIF(ImageReader reader) throws IOException {
        ArrayList<GIFImageFrame> frames = new ArrayList<>(2);

        int width = -1;
        int height = -1;

        IIOMetadata metadata = reader.getStreamMetadata();
        if (metadata != null) {
            IIOMetadataNode globalRoot = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
            NodeList globalScreenDescriptor = globalRoot.getElementsByTagName("LogicalScreenDescriptor");
            if (globalScreenDescriptor != null && globalScreenDescriptor.getLength() > 0) {
                IIOMetadataNode screenDescriptor = (IIOMetadataNode) globalScreenDescriptor.item(0);
                if (screenDescriptor != null) {
                    width = Integer.parseInt(screenDescriptor.getAttribute("logicalScreenWidth"));
                    height = Integer.parseInt(screenDescriptor.getAttribute("logicalScreenHeight"));
                }
            }
        }

        BufferedImage master = null;
        Graphics2D masterGraphics = null;
        for (int frameIndex = 0; ; frameIndex++) {
            BufferedImage image;
            try {
                image = reader.read(frameIndex);
            } catch (IndexOutOfBoundsException io) {
                break;
            }

            if (width == -1 || height == -1) {
                width = image.getWidth();
                height = image.getHeight();
            }

            IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(frameIndex).getAsTree("javax_imageio_gif_image_1.0");
            IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
            int delay = Integer.parseInt(gce.getAttribute("delayTime"));
            String disposal = gce.getAttribute("disposalMethod");

            int x = 0;
            int y = 0;

            if (master == null) {
                master = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                masterGraphics = master.createGraphics();
                masterGraphics.setBackground(new Color(0, 0, 0, 0));
            } else {
                NodeList children = root.getChildNodes();
                for (int nodeIndex = 0; nodeIndex < children.getLength(); nodeIndex++) {
                    Node nodeItem = children.item(nodeIndex);
                    if (nodeItem.getNodeName().equals("ImageDescriptor")) {
                        NamedNodeMap map = nodeItem.getAttributes();
                        x = Integer.parseInt(map.getNamedItem("imageLeftPosition").getNodeValue());
                        y = Integer.parseInt(map.getNamedItem("imageTopPosition").getNodeValue());
                    }
                }
            }
            masterGraphics.drawImage(image, x, y, null);
            BufferedImage copy = new BufferedImage(master.getColorModel(), master.copyData(null), master.isAlphaPremultiplied(), null);
            frames.add(new GIFImageFrame(copy, delay, disposal));

            if (disposal.equals("restoreToPrevious")) {
                BufferedImage from = null;
                for (int i = frameIndex - 1; i >= 0; i--) {
                    if (!frames.get(i).getDisposal().equals("restoreToPrevious") || frameIndex == 0) {
                        from = frames.get(i).getImage();
                        break;
                    }
                }

                master = new BufferedImage(from.getColorModel(), from.copyData(null), from.isAlphaPremultiplied(), null);
                masterGraphics = master.createGraphics();
                masterGraphics.setBackground(new Color(0, 0, 0, 0));
            } else if (disposal.equals("restoreToBackgroundColor")) {
                masterGraphics.clearRect(x, y, image.getWidth(), image.getHeight());
            }
        }
        reader.dispose();

        return !frames.isEmpty() ? new ParsedGIF(frames) : null;
    }

    public static class ParsedGIF {

        public final Map<Integer, GIFImageFrame> frames = new LinkedHashMap<>();
        public final int totalDuration;
        public long currentCycleTime;
        private long lastCycleCheck;

        private ParsedGIF(List<GIFImageFrame> frames) {
            int cumulativeDuration = 0;
            for (GIFImageFrame frame : frames) {
                this.frames.put(cumulativeDuration, frame);
                cumulativeDuration += frame.getDelay();
            }
            this.totalDuration = cumulativeDuration;
        }

        public GIFImageFrame getCurrentFrame() {
            //Get current delta since last pass.
            long currentTime = System.currentTimeMillis() / 10;
            currentCycleTime += (currentTime - lastCycleCheck);
            lastCycleCheck = currentTime;

            //Get us in the delay bounds.
            if (currentCycleTime > totalDuration * 2L) {
                //Reset to prevent loop slowdowns.
                currentCycleTime = 0;
            }
            while (currentCycleTime > totalDuration) {
                currentCycleTime -= totalDuration;
            }

            //Return image index for our delay.
            int lastDelayChecked = 0;
            for (Integer totalDelay : frames.keySet()) {
                if (totalDelay > currentCycleTime) {
                    return frames.get(lastDelayChecked);
                } else {
                    lastDelayChecked = totalDelay;
                }
            }
            return frames.get(lastDelayChecked);
        }
    }

    public static class GIFImageFrame {
        private final int delay;
        private final BufferedImage image;
        private final String disposal;

        private GIFImageFrame(BufferedImage image, int delay, String disposal) {
            this.image = image;
            this.delay = delay;
            this.disposal = disposal;
        }

        public BufferedImage getImage() {
            return image;
        }

        public int getDelay() {
            return delay;
        }

        public String getDisposal() {
            return disposal;
        }
    }
}