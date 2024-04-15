package minecrafttransportsimulator.rendering;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;

/**
 * Main render class for text.  This class contains a few methods for rendering text.  These mainly pertain to rendering
 * text strings given a specified set of formatting and position/rotation.
 *
 * @author don_bruce
 */
public class RenderText {
    public static final char FORMATTING_CHAR = 167;//'§' [\u00a7], this doesn't always translate right across platforms, so we use the keycode here rather than the actual char.
    public static final char BOLD_FORMATTING_CHAR = 'l';
    public static final char ITALIC_FORMATTING_CHAR = 'o';
    public static final char UNDERLINE_FORMATTING_CHAR = 'n';
    public static final char STRIKETHROUGH_FORMATTING_CHAR = 'm';
    public static final char RANDOM_FORMATTING_CHAR = 'k';
    public static final char RESET_FORMATTING_CHAR = 'r';
    public static final char UNDERLINE_CHAR = '_';
    public static final char STRIKETHROUGH_CHAR = '-';

    private static final Map<String, FontData> fontDatas = new HashMap<>();
    private static final TransformationMatrix transformHelper = new TransformationMatrix();

    /**
     * Draws the specified text.  This is designed for general draws where text is defined in-code, but still may
     * use custom fonts.  This method can render the text in multiple ways depending on the parameters passed-in.
     * If a centered string is specified, then the point passed-in should be  the center point of the string, rather
     * that the top-left of the string like normal.  The same goes for right-justify.  If wrapWidth is anything else but 0,
     * then the wordWrap method will be called to render multi-line text.
     * Note that this method expects transforms to be applied such that the coordinate space is local
     * to the object rendering the text on, and is NOT the global coordinate space.  The coordinates MAY, however, be
     * in pixel-space.  This is essentially 1/16 scale of blocks, as blocks are 16 pixels each.
     * This is used for things that are already rendered in pixel-space, such as instruments and GUIs.
     * This also inverts the y coordinate as with those systems +y is down whereas normally it is up.
     * Also note that if a scale was applied prior to rendering this text, it should be passed-in here.
     * This allows for proper normal calculations to prevent needing to re-normalize the text.
     */
    public static void drawText(String text, String fontName, Point3D position, ColorRGB color, TextAlignment alignment, float scale, boolean autoScale, int wrapWidth, boolean renderLit, int worldLightValue) {
        if (!text.isEmpty()) {
            transformHelper.resetTransforms();
            transformHelper.applyTranslation(position);
            getFontData(fontName).renderText(text, transformHelper, null, alignment, scale, autoScale, wrapWidth, true, color, renderLit, worldLightValue, true);
        }
    }

    /**
     * Similar to the 2D text drawing method, except this method will render the text according to the passed-in text JSON in 3D space at the point specified.
     * Essentially, this is JSON-defined rendering rather than manual entry of points.
     */
    public static void draw3DText(String text, AEntityD_Definable<?> entity, TransformationMatrix transform, JSONText definition, boolean pixelCoords) {
        if (!text.isEmpty()) {
            //Get the actual color we will need to render with based on JSON.
            ColorRGB color = entity.getTextColor(definition.inheritedColorIndex, definition.color);

            //Render the text.
            transformHelper.set(transform);
            transformHelper.applyTranslation(definition.pos);
            getFontData(definition.fontName).renderText(text, transformHelper, definition.rot, TextAlignment.values()[definition.renderPosition], definition.scale, definition.autoScale, definition.wrapWidth, pixelCoords, color, definition.lightsUp && entity.renderTextLit(), entity.worldLightValue, false);
        }
    }

    /**
     * Returns the width of the passed-in text.  Units are in pixels,
     * though these are standardized for the default font.  Fonts with
     * higher resolutions may result in non-whole-pixel widths.
     * A full-width char is assumed to be 8px, most will be less than this.
     */
    public static float getStringWidth(String text, String fontName) {
        return getFontData(fontName).getStringWidth(text);
    }

    /**
     * Returns the height of the number of lines of text.  Units are in pixels,
     * though these are standardized for the default font.  Unlike
     * {@link #getStringWidth(String, String)}, font isn't important here as it's
     * always scaled to fit in a 8px area.
     */
    public static float getHeight(int numberLines) {
        return numberLines * (FontData.DEFAULT_PIXELS_PER_CHAR + FontData.LINE_SPACING);
    }

    /**
     * Returns the correct font rendering charset for the passed-in font,
     * creating it if it does not exist.  Does not bind the actual texture,
     * but does load it for calculating charset bounds.
     */
    private static FontData getFontData(String fontName) {
        FontData fontData = fontDatas.get(fontName);
        if (fontData == null) {
            fontData = new FontData(fontName);
            fontDatas.put(fontName, fontData);
        }
        return fontData;
    }

    /**
     * Class used for storing Unicode charset data for font rendering.
     * Contains font bounds and sizes.  Data is stored in an array with each
     * element representing the char index on the texture sheet.
     */
    private static class FontData {
        private static final byte CHARS_PER_ROWCOL = 16;
        private static final int CHARS_PER_TEXTURE_SHEET = CHARS_PER_ROWCOL * CHARS_PER_ROWCOL;
        private static final byte DEFAULT_CHAR_HEIGHT_PIXELS = 7;
        private static final byte DEFAULT_PIXELS_PER_CHAR = 8;
        private static final float LINE_SPACING = 1.0F;
        private static final ColorRGB[] COLORS = new ColorRGB[]{new ColorRGB(0, 0, 0), new ColorRGB(0, 0, 170), new ColorRGB(0, 170, 0), new ColorRGB(0, 170, 170), new ColorRGB(170, 0, 0), new ColorRGB(170, 0, 170), new ColorRGB(255, 170, 0), new ColorRGB(170, 170, 170), new ColorRGB(85, 85, 85), new ColorRGB(85, 85, 255), new ColorRGB(85, 255, 85), new ColorRGB(85, 255, 255), new ColorRGB(255, 85, 85), new ColorRGB(255, 85, 255), new ColorRGB(255, 255, 85), new ColorRGB(255, 255, 255)};
        private static final FontRenderState[] STATES = FontRenderState.generateDefaults();
        private static final int MAX_VERTCIES_PER_RENDER = 1000 * 6;
        private static final Point3D adjustmentOffset = new Point3D();

        /**
         * Texture locations for the font files.
         **/
        private final String[] fontLocations = new String[Character.MAX_VALUE / CHARS_PER_TEXTURE_SHEET];
        /**
         * Char scale.  Normally 1.0, but can be more or less depending on the char ratio.
         * Standard ASCII font is 7px tall out of 8, so this ratio is a scale of 1.0.
         * Unicode font is 10px tall out of 16, or 5 out of 8.
         * Therefore, standard unicode scale is (7/8)/(5/8) = 1.4.
         **/
        private final float charScale;
        /**
         * Offset of the char from the top of the texture sheet, in actual game texture pixels (not texture pixels).
         * Used to know how far to adjust chars for this font up and down during rendering. 
         **/
        private final float charTopOffset;
        /**
         * Char width, in actual game texture pixels (not texture pixels).  May be fractions of a pixel if the font is up-scaled.
         **/
        private final float[] charWidths = new float[Character.MAX_VALUE];
        /**
         * Char spacing, in actual game texture pixels (not texture pixels).  May be fractions of a pixel if the font is up-scaled.
         * This is for BOTH the left and right side, total spacing is double this.
         **/
        private final float[] charSpacings = new float[Character.MAX_VALUE];
        /**
         * Left-most offset for font text position, from 0-1, relative to the texture png.
         **/
        private final float[] offsetsMinU = new float[Character.MAX_VALUE];
        /**
         * Right-most offset for font text position, from 0-1, relative to the texture png.
         **/
        private final float[] offsetsMaxU = new float[Character.MAX_VALUE];
        /**
         * Bottom-most offset for font text position, from 0-1, relative to the texture png.
         **/
        private final float[] offsetsMinV = new float[Character.MAX_VALUE];
        /**
         * Top-most offset for font text position, from 0-1, relative to the texture png.
         **/
        private final float[] offsetsMaxV = new float[Character.MAX_VALUE];

        /**
         * Font render objects.  These are created initially for use in render calls.  Referencing is as follows:
         * The first array element is the texture sheet being used.
         * The second array element is the color.
         * This ensures that there will always be one element for any permutation of states.
         **/
        private static final Map<String, Map<ColorRGB, RenderableData>> createdRenderObjects = new HashMap<>();
        /**
         * Active font render objects.  Items are added to this list during string parsing.
         * At the end, it will be populated and should be looped over for drawing.
         */
        private final Set<RenderableData> activeRenderObjects = new LinkedHashSet<>();
        /**
         * Mutable helper for doing vertex-building operations.
         **/
        private final float[] charVertex = new float[3];
        /**
         * Mutable helper for doing vertex-building operations for font effects like bold and underline.
         **/
        private final float[] supplementalVertex = new float[3];
        /**
         * Mutable helper for doing uv-building operations.
         **/
        private final float[] charUV = new float[2];
        /**
         * Mutable helper for doing uv-building operations for font effects like bold and underline.
         **/
        private final float[] supplementalUV = new float[2];

        private FontData(String fontName) {
            //Get font locations.
            String fontBaseLocation;
            if (fontName == null) {
                fontBaseLocation = "/assets/minecraft/textures/font/unicode_page_";
            } else {
                fontBaseLocation = "/assets/" + fontName.substring(0, fontName.indexOf(":")) + "/textures/fonts/" + fontName.substring(fontName.indexOf(":") + 1) + "/unicode_page_";
            }

            //Parse char widths and the height when applicable.
            float scale = 1.0F;
            float charTopOffset = 0;
            for (int i = 0; i < fontLocations.length; ++i) {
                fontLocations[i] = String.format("%s%02x.png", fontBaseLocation, i);
                BufferedImage bufferedImage;
                try {
                    bufferedImage = ImageIO.read(InterfaceManager.renderingInterface.getTextureStream(fontLocations[i]));
                } catch (Exception e) {
                    //Just continue, as we don't care about this file.  Not all files may be present for any given font.
                    continue;
                }

                //Calculate min/max.
                //For each char, we look at the row/col bounds and check every pixel in the col
                //starting from right to left.  If we hit a pixel in this col sub-section, we know we
                //have found the end of the char and that's its width.
                //Order is all chars in row 1, then row 2, etc.
                int pixelsPerSide = bufferedImage.getHeight();
                int pixelsPerCharRowCol = pixelsPerSide / CHARS_PER_ROWCOL;
                for (int charRow = 0; charRow < CHARS_PER_ROWCOL; ++charRow) {
                    for (int charCol = 0; charCol < CHARS_PER_ROWCOL; ++charCol) {
                        //Get char and set defaults.
                        char charChecking = (char) (i * CHARS_PER_TEXTURE_SHEET + charRow * CHARS_PER_ROWCOL + charCol);
                        if (charChecking == '0') {
                            //We will always have 0, and it's a known-height char, so use this for our scale checks.
                            //Look top-down for pixels to see if we have any gaps.
                            boolean foundTopPixel = false;
                            int topPixel = charRow * pixelsPerCharRowCol;
                            int bottomPixel = (charRow + 1) * pixelsPerCharRowCol - 1;
                            for (int pixelRow = charRow * pixelsPerCharRowCol; pixelRow < (charRow + 1) * pixelsPerCharRowCol; ++pixelRow) {
                                boolean foundPixelThisRow = false;
                                for (int pixelCol = charCol * pixelsPerCharRowCol; pixelCol < (charCol + 1) * pixelsPerCharRowCol; ++pixelCol) {
                                    //Check all pixels in this row to see if we have one.
                                    //Check for alpha and color.  Some systems write color, but no alpha to a pixel.
                                    int pixelValue = bufferedImage.getRGB(pixelCol, pixelRow);
                                    if (pixelValue != 0 && (pixelValue >> 24) != 0) {
                                        foundPixelThisRow = true;
                                        if (!foundTopPixel) {
                                            //First existing pixel found, must be top.
                                            topPixel = pixelRow;
                                            foundTopPixel = true;
                                            charTopOffset = (float) DEFAULT_PIXELS_PER_CHAR * (pixelRow - charRow * pixelsPerCharRowCol) / (pixelsPerCharRowCol);
                                        }
                                    }
                                }
                                if (!foundPixelThisRow && foundTopPixel) {
                                    //First blank pixel found after finding some pixels, must be bottom.
                                    bottomPixel = pixelRow;
                                    break;
                                }
                            }

                            //Scale should make this font render the size of 7px out of the 8px high.  This allows a 1px bottom buffer to match ASCII standards.
                            scale = (DEFAULT_CHAR_HEIGHT_PIXELS / (float) DEFAULT_PIXELS_PER_CHAR) / ((bottomPixel - topPixel) / (float) pixelsPerCharRowCol);
                        }
                        if (charChecking == ' ') {
                            //Space isn't rendered, but is half-width with 1 spacing on each side.
                            charWidths[charChecking] = DEFAULT_PIXELS_PER_CHAR / 2;
                            charSpacings[charChecking] = 0;
                        } else {
                            offsetsMinU[charChecking] = charCol / (float) CHARS_PER_ROWCOL;
                            offsetsMaxU[charChecking] = (charCol + 1) / (float) CHARS_PER_ROWCOL;
                            //Normally we'd invert the UV-mapping here to compensate for the inverted texture center.
                            //But in this case, we don't have to do that.  Still not 100% sure on the math, but it works?
                            offsetsMaxV[charChecking] = (charRow) / (float) CHARS_PER_ROWCOL;
                            offsetsMinV[charChecking] = (charRow + 1) / (float) CHARS_PER_ROWCOL;
                            charWidths[charChecking] = DEFAULT_PIXELS_PER_CHAR;

                            //Check each pixel in the pixel sub-col to get the actual width of the char.
                            //Do this for the left and right side to get the bounds.
                            boolean foundPixelThisCol = false;
                            for (int pixelCol = charCol * pixelsPerCharRowCol; pixelCol < (charCol + 1) * pixelsPerCharRowCol; ++pixelCol) {
                                //Check all rows of pixels in this column to see if we have one.
                                for (int pixelRow = charRow * pixelsPerCharRowCol; pixelRow < (charRow + 1) * pixelsPerCharRowCol; ++pixelRow) {
                                    //Check for alpha and color.  Some systems write color, but no alpha to a pixel.
                                    int pixelValue = bufferedImage.getRGB(pixelCol, pixelRow);
                                    if (pixelValue != 0 && (pixelValue >> 24) != 0) {
                                        //Found a pixel, we must have this as our UV.
                                        offsetsMinU[charChecking] = pixelCol / (float) pixelsPerCharRowCol / CHARS_PER_ROWCOL;
                                        charSpacings[charChecking] = (pixelCol - charCol * pixelsPerCharRowCol) / (float) pixelsPerCharRowCol * DEFAULT_PIXELS_PER_CHAR;
                                        foundPixelThisCol = true;
                                        break;
                                    }
                                }
                                if (foundPixelThisCol) {
                                    break;
                                }
                            }

                            foundPixelThisCol = false;
                            for (int pixelCol = (charCol + 1) * pixelsPerCharRowCol - 1; pixelCol >= charCol * pixelsPerCharRowCol; --pixelCol) {
                                //Check all rows of pixels in this column to see if we have one.
                                for (int pixelRow = charRow * pixelsPerCharRowCol; pixelRow < (charRow + 1) * pixelsPerCharRowCol; ++pixelRow) {
                                    //Check for alpha and color.  Some systems write color, but no alpha to a pixel.
                                    int pixelValue = bufferedImage.getRGB(pixelCol, pixelRow);
                                    if (pixelValue != 0 && (pixelValue >> 24) != 0) {
                                        //Found a pixel, we must have this as our UV.
                                        ++pixelCol;
                                        offsetsMaxU[charChecking] = pixelCol / (float) pixelsPerCharRowCol / CHARS_PER_ROWCOL;
                                        charWidths[charChecking] = (offsetsMaxU[charChecking] - offsetsMinU[charChecking]) * CHARS_PER_ROWCOL * DEFAULT_PIXELS_PER_CHAR;
                                        foundPixelThisCol = true;
                                        break;
                                    }
                                }
                                if (foundPixelThisCol) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            this.charScale = scale;
            this.charTopOffset = charTopOffset;
        }

        private void renderText(String text, TransformationMatrix transform, RotationMatrix rotation, TextAlignment alignment, float scale, boolean autoScale, int wrapWidth, boolean pixelCoords, ColorRGB color, boolean renderLit, int worldLightValue, boolean onGUI) {
            //Clear out the active object list as it was set last pass.
            for (RenderableData object : activeRenderObjects) {
                object.vertexObject.vertices.clear();
            }
            activeRenderObjects.clear();

            //Cull text to total chars.
            //This is all we can render in one pass.
            if (text.length() > MAX_VERTCIES_PER_RENDER / 6) {
                text = text.substring(0, MAX_VERTCIES_PER_RENDER / 6);
            }

            //Pre-calculate normals, as these won't change.
            float[] normals = new float[]{0.0F, 0.0F, 1.0F};

            //Check the string for a random font code char.  If we have one, we need to substitute chars.
            //Do this prior to rendering operations as this will affect string length and blocks.
            if (text.indexOf(FORMATTING_CHAR + RANDOM_FORMATTING_CHAR) != -1) {
                char[] textArray = text.toCharArray();
                boolean randomActive = false;
                for (int i = 0; i < textArray.length; ++i) {

                    //Check if we are a formatting code before doing any other parsing.
                    if (textArray[i] == FORMATTING_CHAR) {
                        char formattingChar = textArray[++i];
                        if (formattingChar == RANDOM_FORMATTING_CHAR) {
                            randomActive = true;
                        } else if (formattingChar == RESET_FORMATTING_CHAR) {
                            randomActive = false;
                        }
                    } else if (randomActive) {
                        textArray[i] = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt((int) (Math.random() * 36));
                    }
                }
                text = String.valueOf(textArray);
            }

            //Set adjustment offset Y position to make the char go up to be top-aligned.
            adjustmentOffset.set(0, charTopOffset, 0);

            //Apply char scaling factor to the scale to account for font upscaling or downscaling.
            scale *= charScale;

            //Get the text width.
            float stringWidth = getStringWidth(text);

            //Check for auto-scaling.
            if (autoScale && wrapWidth > 0) {
                //Need to get the actual string width we would render at.
                //This takes scale into account, as string upscaling would
                //cause the width to go up.
                float scaledStringWidth = scale * stringWidth;
                if (scaledStringWidth > wrapWidth) {
                    double scaleFactor = wrapWidth / scaledStringWidth;
                    scale *= scaleFactor;
                    //Adjust text down based on how much we changed scale.
                    //Adjust based on 1/2 the height of the text, times how much we adjusted scale.
                    //So 1/2 reduction in scale will move the text 1/4 down.
                    adjustmentOffset.add(0, DEFAULT_PIXELS_PER_CHAR / 2D * (scaleFactor - 1), 0);
                }
                //Don't use wrap width if we already adjusted scale for it.
                wrapWidth = 0;
            }

            //Reduce scale by 16 if we're not using pixel coords.
            //Entity rendering systems calling this function feeding params will scale in block coords.
            //We need to be be in pixel coords for all the rendering operations here.
            //We need to do this after auto-wrapping since that doesn't use the global 1/16 scale modifier.
            if (!pixelCoords) {
                scale /= 16;
            }

            //Check if we need to adjust our offset for our alignment.
            //While this will be slightly off due to formatting and non-printable chars in the string,
            //it is better than trying to pre-strip them and then parse the text after.
            //Besides, who is going to do word-wrapping on fancy text?
            float alignmentOffset = 0;
            if (alignment.equals(TextAlignment.CENTERED)) {
                alignmentOffset = (wrapWidth == 0 ? -stringWidth : -wrapWidth) / 2F;
            } else if (alignment.equals(TextAlignment.RIGHT_ALIGNED)) {
                alignmentOffset = -stringWidth;
            }

            //Adjust wrapWidth to account for total scale.
            wrapWidth /= scale;

            //The following rendering setup code operates on the assumption of a 1.0 scale.
            //This function sets up the font using the paramters defined above, and sets all UV points.
            //After things are set up, the entire renderable object set is scaled to get it at the right size.
            //This greatly simplifies things, as we don't need to track scale anywhere.
            float currentOffset = 0;
            float currentLineOffset = 0;
            int indexAtLastNewline = 0;
            ColorRGB currentColor = color;
            FontRenderState currentState = STATES[0];
            for (int i = 0; i < text.length(); ++i) {
                char textChar = text.charAt(i);

                //Check if we are a formatting code before doing any other parsing.
                if (textChar == FORMATTING_CHAR) {
                    //Get the format code and apply operation.
                    char formattingChar = text.charAt(++i);
                    switch (formattingChar) {
                        case (BOLD_FORMATTING_CHAR):
                            currentState = STATES[currentState.index | FontRenderState.BOLD_BIT_INDEX];
                            break;
                        case (ITALIC_FORMATTING_CHAR):
                            currentState = STATES[currentState.index | FontRenderState.ITALIC_BIT_INDEX];
                            break;
                        case (UNDERLINE_FORMATTING_CHAR):
                            currentState = STATES[currentState.index | FontRenderState.UNDERLINE_BIT_INDEX];
                            break;
                        case (STRIKETHROUGH_FORMATTING_CHAR):
                            currentState = STATES[currentState.index | FontRenderState.STRIKETHROUGH_BIT_INDEX];
                            break;
                        case (RESET_FORMATTING_CHAR):
                            currentState = STATES[0];
                            currentColor = color;
                            break;
                        default: {
                            try {
                                currentColor = COLORS[Integer.decode("0x" + formattingChar)];
                                break;
                            } catch (Exception e) {
                                //Skip this formatting char, as it's invalid.
                            }
                        }
                    }
                    //Go back though the loop again to the next char.
                    continue;
                }

                //Not a formatting char, do normal rendering.
                if (textChar == '\n') {
                    //Go down one line.
                    currentOffset = 0;
                    currentLineOffset -= DEFAULT_PIXELS_PER_CHAR + LINE_SPACING;
                    indexAtLastNewline = i;
                } else if (wrapWidth != 0 && currentOffset > wrapWidth) {
                    //Go backwards in text to find last space and split based on that.
                    //After this we will re-do the parsing of the prior chars on the next line.
                    //Don't do this if we don't have a space in this line though.  This is the case for URLs
                    //and other long segments of text.
                    if (text.substring(indexAtLastNewline + 1, i).indexOf(' ') != -1) {
                        for (int j = i - 1; j > 0; --j) {
                            char priorChar = text.charAt(j);
                            if (priorChar == ' ') {
                                i = j;
                                currentOffset = 0;
                                currentLineOffset -= DEFAULT_PIXELS_PER_CHAR + LINE_SPACING;
                                indexAtLastNewline = i;
                                break;
                            } else {
                                //Need to remove vertices in buffer so they don't get rendered.
                                //However, only do this if we have vertices.
                                //We could end up needing to word wrap before a formatting char.
                                RenderableData priorRenderObject = getObjectFor(priorChar, currentColor);
                                if (priorRenderObject.vertexObject.vertices.position() != 0) {
                                    priorRenderObject.vertexObject.vertices.position(priorRenderObject.vertexObject.vertices.position() - 6 * 8);
                                }

                                //If we had supplemental state rendering, remove from those blocks too.
                                if (currentState.bold) {
                                    //Bold, remove another char as we double-rendered.
                                    priorRenderObject.vertexObject.vertices.position(priorRenderObject.vertexObject.vertices.position() - 6 * 8);
                                }
                                if (currentState.underline) {
                                    //Remove 1 char from the underline object.
                                    RenderableData underlineRenderObject = getObjectFor(UNDERLINE_CHAR, currentColor);
                                    underlineRenderObject.vertexObject.vertices.position(underlineRenderObject.vertexObject.vertices.position() - 6 * 8);
                                }
                                if (currentState.strikethrough) {
                                    //Remove 1 char from the strikethough object.
                                    RenderableData strikethroughRenderObject = getObjectFor(STRIKETHROUGH_CHAR, currentColor);
                                    strikethroughRenderObject.vertexObject.vertices.position(strikethroughRenderObject.vertexObject.vertices.position() - 6 * 8);
                                }
                            }
                        }
                    } else {
                        currentOffset = 0;
                        currentLineOffset -= DEFAULT_PIXELS_PER_CHAR + LINE_SPACING;
                        indexAtLastNewline = i;
                    }
                } else if (textChar == ' ') {
                    //Just increment the offset, spaces don't render.
                    currentOffset += charWidths[textChar] + charSpacings[textChar];
                } else {
                    //Actual char to render.  Add leading spacing.
                    currentOffset += charSpacings[textChar];

                    //Do normal char addition to the map of chars to draw.
                    //If we are bold, we will double-render slightly offset.
                    //If we are underline, add an underline overlay.
                    //If we are italic, we slightly skew the UV map by 1px.
                    //If we are strikethough, we add a strikethough overlay.
                    RenderableData currentRenderObject = getObjectFor(textChar, currentColor);
                    float charWidth = charWidths[textChar];
                    int charSteps = 6;
                    if (currentState.bold)
                        charSteps += 6;
                    if (currentState.underline)
                        charSteps += 6;
                    if (currentState.strikethrough)
                        charSteps += 6;
                    for (int j = 0; j < charSteps; ++j) {
                        //Set vertex properties.
                        switch (j) {
                            case (0)://Bottom-right
                            case (3): {
                                charVertex[0] = alignmentOffset + currentOffset + charWidth;
                                charVertex[1] = currentLineOffset - DEFAULT_PIXELS_PER_CHAR;
                                charUV[0] = offsetsMaxU[textChar];
                                charUV[1] = offsetsMinV[textChar];
                                break;
                            }
                            case (1): {//Top-right
                                charVertex[0] = alignmentOffset + currentOffset + charWidth;
                                if (currentState.italic) {
                                    charVertex[0] += 1;
                                }
                                charVertex[1] = currentLineOffset;
                                charUV[0] = offsetsMaxU[textChar];
                                charUV[1] = offsetsMaxV[textChar];
                                break;
                            }
                            case (2):
                            case (4): {//Top-left
                                charVertex[0] = alignmentOffset + currentOffset;
                                if (currentState.italic) {
                                    charVertex[0] += 1;
                                }
                                charVertex[1] = currentLineOffset;
                                charUV[0] = offsetsMinU[textChar];
                                charUV[1] = offsetsMaxV[textChar];
                                break;
                            }
                            case (5): {//Bottom-left
                                charVertex[0] = alignmentOffset + currentOffset;
                                charVertex[1] = currentLineOffset - DEFAULT_PIXELS_PER_CHAR;
                                charUV[0] = offsetsMinU[textChar];
                                charUV[1] = offsetsMinV[textChar];
                                break;
                            }
                            default: {
                                //Custom vertex, either bold, underline or strikethough.
                                //Bold is done in indexes 6-11, underline 12-17, strikethrough 18-23.
                                //Note that if one of the three isn't active, the others will move "up" in the order.
                                //This is like how lists work.

                                //Get the current char vertex we are mimicking.  This requires checking the buffer.
                                //Skip the first three indexes as they are normal data we don't care about.
                                int currentIndex = currentRenderObject.vertexObject.vertices.position();
                                currentRenderObject.vertexObject.vertices.position(currentIndex - (6 - j % 6 + j - 6) * 8 + 3);
                                currentRenderObject.vertexObject.vertices.get(supplementalUV);
                                currentRenderObject.vertexObject.vertices.get(supplementalVertex);
                                currentRenderObject.vertexObject.vertices.position(currentIndex);

                                if (currentState.bold && j < 12) {
                                    //Just render a second char slightly offset.
                                    supplementalVertex[0] += 0.2F;
                                    supplementalVertex[1] += 0.2F;
                                    currentRenderObject.vertexObject.vertices.put(normals).put(supplementalUV).put(supplementalVertex);
                                } else {
                                    char customChar;
                                    if (currentState.underline && j < 18) {
                                        customChar = UNDERLINE_CHAR;
                                    } else if (currentState.strikethrough) {
                                        customChar = STRIKETHROUGH_CHAR;
                                    } else {
                                        //We'll never get here (I hope?) , but it makes the complier happy.
                                        continue;
                                    }

                                    //Set position to master and set custom char.
                                    //supplementalVertex[1] += CHAR_SPACING;
                                    switch (j % 6) {
                                        case (0):
                                        case (3): {//Bottom-right
                                            //supplementalVertex[0] += CHAR_SPACING;
                                            supplementalUV[0] = offsetsMaxU[customChar];
                                            supplementalUV[1] = offsetsMinV[customChar];
                                            break;
                                        }
                                        case (1): {//Top-right
                                            //supplementalVertex[0] += CHAR_SPACING;
                                            supplementalUV[0] = offsetsMaxU[customChar];
                                            supplementalUV[1] = offsetsMaxV[customChar];
                                            break;
                                        }
                                        case (2):
                                        case (4): {//Top-left
                                            //supplementalVertex[0] -= CHAR_SPACING;
                                            supplementalUV[0] = offsetsMinU[customChar];
                                            supplementalUV[1] = offsetsMaxV[customChar];
                                            break;
                                        }
                                        case (5): {//Bottom-left
                                            //supplementalVertex[0] -= CHAR_SPACING;
                                            supplementalUV[0] = offsetsMinU[customChar];
                                            supplementalUV[1] = offsetsMinV[customChar];
                                            break;
                                        }
                                    }

                                    //Add supplemental vertex to render object, and add to active list if required.
                                    RenderableData customRenderObject = getObjectFor(customChar, currentColor);
                                    customRenderObject.vertexObject.vertices.put(normals).put(supplementalUV).put(supplementalVertex);
                                    activeRenderObjects.add(customRenderObject);
                                }
                            }
                        }

                        //Only do these calcs for the first 6 vertices for the main char.
                        if (j < 6) {
                            //Z is always 0.
                            charVertex[2] = 0.0F;

                            //Add char vertex to render block.
                            currentRenderObject.vertexObject.vertices.put(normals).put(charUV).put(charVertex);
                        }
                    }

                    //Increment offset to next char position and set char points and add render block to active list.
                    currentOffset += charWidth + charSpacings[textChar];
                    activeRenderObjects.add(currentRenderObject);
                }
            }

            //All points obtained, render.
            //Prior to rendering we need to scale the font objects to their requested scale, multiplied by their internal scale factor.
            //After this, we apply the known-constant adjustmentOffset, which will itself be scaled.
            for (RenderableData object : activeRenderObjects) {
                object.setLightValue(worldLightValue);
                object.setLightMode(renderLit ? LightingMode.IGNORE_ALL_LIGHTING : (onGUI ? LightingMode.IGNORE_ORIENTATION_LIGHTING : LightingMode.NORMAL));
                object.transform.set(transform);
                if (rotation != null) {
                    object.transform.applyRotation(rotation);
                }
                object.transform.applyScaling(scale, scale, scale);
                object.transform.applyTranslation(adjustmentOffset);
                object.vertexObject.vertices.flip();
                object.render();
            }
        }

        private RenderableData getObjectFor(char textChar, ColorRGB color) {
            //First get the font block;
            //MNake sure we didn't get passed a bad char from some unicode junk text.
            if (textChar / CHARS_PER_TEXTURE_SHEET >= fontLocations.length) {
                textChar = 0;
            }
            String font = fontLocations[textChar / CHARS_PER_TEXTURE_SHEET];
            Map<ColorRGB, RenderableData> map1 = createdRenderObjects.computeIfAbsent(font, k -> new HashMap<>());

            RenderableData object = map1.get(color);
            if (object == null) {
                object = new RenderableData(new RenderableVertices("font_block", FloatBuffer.allocate(MAX_VERTCIES_PER_RENDER * 8), false), font);
                object.setColor(color);
                map1.put(color, object);
            }
            return object;
        }

        private float getStringWidth(String text) {
            float stringWidth = 0;
            boolean skipNext = false;
            boolean foundCharAlready = false;
            for (char textChar : text.toCharArray()) {
                //Skip formatting chars and their next char.
                if (textChar == FORMATTING_CHAR) {
                    skipNext = true;
                } else if (skipNext) {
                    skipNext = false;
                } else {
                    stringWidth += charWidths[textChar] + 2 * charSpacings[textChar];
                    if (!foundCharAlready) {
                        foundCharAlready = true;
                    }
                }
            }
            return stringWidth;
        }

        private static class FontRenderState {
            private static final int BOLD_BIT_INDEX = 1;
            private static final int ITALIC_BIT_INDEX = 2;
            private static final int UNDERLINE_BIT_INDEX = 4;
            private static final int STRIKETHROUGH_BIT_INDEX = 8;

            private final int index;
            private final boolean bold;
            private final boolean italic;
            private final boolean underline;
            private final boolean strikethrough;

            private FontRenderState(int index) {
                this.index = index;
                this.bold = (index & BOLD_BIT_INDEX) == BOLD_BIT_INDEX;
                this.italic = (index & ITALIC_BIT_INDEX) == ITALIC_BIT_INDEX;
                this.underline = (index & UNDERLINE_BIT_INDEX) == UNDERLINE_BIT_INDEX;
                this.strikethrough = (index & STRIKETHROUGH_BIT_INDEX) == STRIKETHROUGH_BIT_INDEX;
            }

            public static FontRenderState[] generateDefaults() {
                FontRenderState[] states = new FontRenderState[(int) Math.pow(2, 4)];
                for (int i = 0; i < states.length; ++i) {
                    states[i] = new FontRenderState(i);
                }
                return states;
            }
        }
    }

    /**
     * List of enums that define how text is rendered.
     */
    public enum TextAlignment {
        CENTERED,
        LEFT_ALIGNED,
        RIGHT_ALIGNED
    }
}
