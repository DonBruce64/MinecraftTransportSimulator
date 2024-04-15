package minecrafttransportsimulator.guis.components;

import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;

/**
 * Base class for all components.  Contains basic common variables to all rendering.
 * Note that {@link #constructedX} and {@link #constructedY} are only valid as the values
 * passed-in to the constructor, and should NOT be used for rendering or anything as the
 * component could have been moved at that point.  Also, because for construction ease
 * y is aligned with textures for a top-left origin.  Whereas OpenGL rendering uses a bottom-left.
 * Therefore, the y-value in {@link #position} will be inverted from this value upon construction.
 *
 * @author don_bruce
 */
public abstract class AGUIComponent {

    //Rendering variables.
    public final int constructedX;
    public final int constructedY;
    public final Point3D position;
    public final Point3D textPosition;
    public int width;
    public int height;

    //State variables.
    public boolean visible = true;
    public boolean ignoreGUILightingState;
    public String text;
    protected RenderableData renderable;
    private static final RenderableVertices mutableTooltipVertices = RenderableVertices.createSprite(9, null, null);
    private static final RenderableData mutableTooltipRenderable = new RenderableData(mutableTooltipVertices, AGUIBase.STANDARD_TEXTURE_NAME);
    private static final Point3D mutableTooltipPosition = new Point3D();

    protected static final int TEXT_DEFAULT_ZOFFSET = 200;
    protected static final int MODEL_DEFAULT_ZOFFSET = 100;
    private static final int TOOLTIP_BORDER_PADDING = 4;
    private static final int TOOLTIP_SECTION_WIDTH = 100;
    private static final int TOOLTIP_SECTION_HEIGHT = 60;
    private static final int TOOLTIP_SECTION_BORDER = 3;
    private static final int TOOLTIP_SECTION_WIDTH_OFFSET = 100;
    private static final int TOOLTIP_SECTION_HEIGHT_OFFSET = 196;
    private static final float TOOLTIP_TEXTURE_WIDTH = 256;//Float to force floating-point division in math.
    private static final float TOOLTIP_TEXTURE_HEIGHT = 256;

    static {
        //Don't take into account lighting.
        mutableTooltipRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
    }

    public AGUIComponent(int x, int y, int width, int height) {
        this.constructedX = x;
        this.constructedY = y;
        //Use -y as GUI construction uses inverted Y coords.
        this.position = new Point3D(x, -y, getZOffset());
        this.textPosition = new Point3D(position.x, position.y, position.z + TEXT_DEFAULT_ZOFFSET);
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the z-offset for this component.  This defines what "layer" the component will render on in the GUI.
     * Components of different types should NOT share layers, as this will cause z-fighting.  It is recommended that
     * any 3d models be placed at least 150 units away from all others to prevent clipping.  This includes
     * text, which should be rendered on top of all components.  Therefore, text is defaulted to 300 units forward.  All
     * of these defaults can be modified if desired, however, by overriding this method.
     */
    public int getZOffset() {
        return 0;
    }

    /**
     * Returns true if the mouse is within the bounds of this component.
     */
    public boolean isMouseInBounds(int mouseX, int mouseY) {
        return mouseX >= position.x && mouseY >= -position.y && mouseX < position.x + width && mouseY < -position.y + height;
    }

    /**
     * Renders the main portion of the component.
     * Note that this method, and all other methods, are not called if {@link #visible} is false.
     * This should create the {@link #renderable} object and render it.
     */
    public abstract void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks);

    /**
     * Renders the component's text.  This is done separately from the main render as text uses its own texture,
     * and we don't want to do this until the rest of the GUI is rendered as it will cause a texture bind.
     * Note that this method is not called if {@link #text} is null, but will render if it is empty since some systems
     * may consider empty text valid for triggering animations versus it just not existing whatsoever.
     */
    public void renderText(boolean renderTextLit, int worldLightValue) {
    }

    /**
     * Renders the tooltip for this component.  This needs to be done after the main render
     * of all components as otherwise it will render behind other components.
     * Most components don't have tooltips, but components that have complex functionality
     * may need them to help explain what they do.  Note that this method is not called if
     * {@link #isMouseInBounds(int, int)} returns false, and that this method won't render
     * anything if {@link #getTooltipText()} returns null.
     */
    public void renderTooltip(AGUIBase gui, int mouseX, int mouseY) {
        List<String> tooltipTextLines = getTooltipText();
        if (tooltipTextLines != null && !tooltipTextLines.isEmpty()) {
            //Find the max string width.  This is used to define text bounds.
            //We need to know how many lines we wrap to offset our rendering of our text.
            int wrapWidth = gui.screenWidth - mouseX > mouseX ? gui.screenWidth - mouseX - 2 * TOOLTIP_BORDER_PADDING : mouseX - 2 * TOOLTIP_BORDER_PADDING;
            float longestLineWidth = 0;
            int linesOfText = 0;
            StringBuilder tooltipCombinedText = new StringBuilder();
            for (String tooltipText : tooltipTextLines) {
                float lineWidth = RenderText.getStringWidth(tooltipText, null);
                linesOfText += Math.ceil(lineWidth / wrapWidth);
                tooltipCombinedText.append(tooltipText).append("\n");
                if (lineWidth > longestLineWidth) {
                    longestLineWidth = lineWidth;
                }
            }
            int actualStringWidth = (int) (longestLineWidth > wrapWidth ? wrapWidth : longestLineWidth);
            int actualStringHeight = (int) Math.ceil(RenderText.getHeight(linesOfText));

            //We now know how wide the text will be, and how high.  Find quadrant.
            int xOffset;
            if (gui.screenWidth - mouseX > mouseX) {
                //Render right.
                xOffset = mouseX;
            } else {
                //Render left.
                xOffset = mouseX - actualStringWidth - 2 * TOOLTIP_BORDER_PADDING;
            }

            int yOffset;
            if (gui.screenHeight - mouseY > mouseY) {
                //Render bottom.
                yOffset = mouseY;
            } else {
                //Render top.
                yOffset = mouseY - actualStringHeight - 2 * TOOLTIP_BORDER_PADDING;
            }

            //Render the 4 corners, and the 4 edge bits, and then the center.  This prevents stretching.
            int horizontalSegmentSize = actualStringWidth + 2 * TOOLTIP_BORDER_PADDING;
            int verticalSegmentSize = actualStringHeight + 2 * TOOLTIP_BORDER_PADDING;
            
            //Top-left.
            mutableTooltipVertices.setSpriteProperties(0, xOffset, -yOffset, TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET / TOOLTIP_TEXTURE_WIDTH, TOOLTIP_SECTION_HEIGHT_OFFSET / TOOLTIP_TEXTURE_HEIGHT, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_HEIGHT);
            //Top-right.
            mutableTooltipVertices.setSpriteProperties(1, xOffset + horizontalSegmentSize - TOOLTIP_SECTION_BORDER, -yOffset, TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH - TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_WIDTH, TOOLTIP_SECTION_HEIGHT_OFFSET / TOOLTIP_TEXTURE_HEIGHT, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH) / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_HEIGHT);
            //Bottom-left.
            mutableTooltipVertices.setSpriteProperties(2, xOffset, -(yOffset + verticalSegmentSize - TOOLTIP_SECTION_BORDER), TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT - TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_HEIGHT, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT) / TOOLTIP_TEXTURE_HEIGHT);
            //Bottom-right.
            mutableTooltipVertices.setSpriteProperties(3, xOffset + horizontalSegmentSize - TOOLTIP_SECTION_BORDER, -(yOffset + verticalSegmentSize - TOOLTIP_SECTION_BORDER), TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH - TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT - TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_HEIGHT, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH) / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT) / TOOLTIP_TEXTURE_HEIGHT);

            //Top-center.
            mutableTooltipVertices.setSpriteProperties(4, xOffset + TOOLTIP_SECTION_BORDER, -yOffset, horizontalSegmentSize - 2 * TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_WIDTH, TOOLTIP_SECTION_HEIGHT_OFFSET / TOOLTIP_TEXTURE_HEIGHT, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH - TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_HEIGHT);
            //Bottom-center.
            mutableTooltipVertices.setSpriteProperties(5, xOffset + TOOLTIP_SECTION_BORDER, -(yOffset + verticalSegmentSize - TOOLTIP_SECTION_BORDER), horizontalSegmentSize - 2 * TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT - TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_HEIGHT, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH - TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT) / TOOLTIP_TEXTURE_HEIGHT);
            //Left-center.
            mutableTooltipVertices.setSpriteProperties(6, xOffset, -(yOffset + TOOLTIP_SECTION_BORDER), TOOLTIP_SECTION_BORDER, verticalSegmentSize - 2 * TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_HEIGHT, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT - TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_HEIGHT);
            //Right-center.
            mutableTooltipVertices.setSpriteProperties(7, xOffset + horizontalSegmentSize - TOOLTIP_SECTION_BORDER, -(yOffset + TOOLTIP_SECTION_BORDER), TOOLTIP_SECTION_BORDER, verticalSegmentSize - 2 * TOOLTIP_SECTION_BORDER, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH - TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_HEIGHT, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH) / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT - TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_HEIGHT);

            //Center-center.
            mutableTooltipVertices.setSpriteProperties(8, xOffset + TOOLTIP_SECTION_BORDER, -(yOffset + TOOLTIP_SECTION_BORDER), horizontalSegmentSize - 2 * TOOLTIP_SECTION_BORDER, verticalSegmentSize - 2 * TOOLTIP_SECTION_BORDER, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_HEIGHT, (TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH - TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_WIDTH, (TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT - TOOLTIP_SECTION_BORDER) / TOOLTIP_TEXTURE_HEIGHT);

            //Do the actual rendering.
            mutableTooltipRenderable.transform.setTranslation(0, 0, textPosition.z + 25);
            mutableTooltipRenderable.render();

            //Need to move tooltip text by -y to account for inverted coords.
            mutableTooltipPosition.set(xOffset + TOOLTIP_BORDER_PADDING, -(yOffset + TOOLTIP_BORDER_PADDING), textPosition.z + 50);
            RenderText.drawText(tooltipCombinedText.toString(), null, mutableTooltipPosition, ColorRGB.WHITE, TextAlignment.LEFT_ALIGNED, 1.0F, false, wrapWidth, true, gui.worldLightValue);
        }
    }

    /**
     * Returns the tooltip text lines for this tooltip.  Only called if {@link #renderTooltip(AGUIBase, int, int)}
     * is called.
     */
    public List<String> getTooltipText() {
        return null;
    }
}
