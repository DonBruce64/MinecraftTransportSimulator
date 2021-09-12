package minecrafttransportsimulator.rendering.instances;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceRender;

/**Main render class for text.  This class contains a few methods for rendering text.  These mainly pertain to rendering
 * text strings given a specified set of formatting and position/rotation.
 *
 * @author don_bruce
 */
public class RenderText{
	private static final Map<String, FontData> fontDatas = new HashMap<String, FontData>();
	private static final Point3d mutablePosition = new Point3d();
	private static final Point3d FLIPPED_TEXT_FOR_GUIS = new Point3d(180, 0, 0);
	
	/**
	 *  Draws the specified text.  This is designed for GUIs where text only has an x and y component, but still may
	 *  use custom fonts.  This method can render the text in multiple ways depending on the parameters passed-in.  
	 *  If a centered string is specified, then the point passed-in should be  the center point of the string, rather 
	 *  that the top-left of the string like normal.  The same goes for right-justify.  If wrapWidth is anything else but 0, 
	 *  then the wordWrap method will be called to render multi-line text.
	 */
	public static void draw2DText(String text, String customFontName, int x, int y, ColorRGB color, TextAlignment alignment, float scale, int wrapWidth){
		FontData fontData = getFontData(customFontName);
		InterfaceRender.setColorState(color);
		mutablePosition.set(x, y, 0);
		//Need to invert 2D GUI text, as it's Y coord origin is top-left VS UV-mapped bottom-left.
		fontData.renderText(text, mutablePosition, FLIPPED_TEXT_FOR_GUIS, alignment, scale, wrapWidth, 1.0F, true);
		InterfaceRender.setColorState(ColorRGB.WHITE);
	}
	
	/**
	 *  Similar to {@link #drawBasicText(String, int, int, ColorRGB, TextAlignment, int)}, except this method
	 *  will render the text according to the passed-in text JSON in 3D space at the point specified.
	 *  Essentially, this is JSON-defined rendering rather than manual entry of points.
	 *  Note that this method expects transforms to be applied such that the coordinate space is local
	 *  to the passed-in entity, and is NOT the global coordinate space.  The coordinates MAY, however, be
	 *  in pixel-space.  This is essentially 1/16 scale of blocks, as blocks are 16 pixels each.
	 *  This is used for things that are already rendered in pixel-space, such as instruments.
	 *  Also note that if a scale was applied prior to rendering this text, it should be passed-in here.
	 *  This allows for proper normal calculations to prevent needing to re-normalize the text.
	 */
	public static void draw3DText(String text, AEntityC_Definable<?> entity, JSONText definition, float preScaledFactor, boolean pixelCoords){
		FontData fontData = getFontData(definition.fontName);
		
		//If we have light-up text, disable lightmap.
		if(definition.lightsUp && entity.renderTextLit()){
			InterfaceRender.setInternalLightingState(false);
		}
		
		//Set the color.
		ColorRGB inheritedColor = entity.getSecondaryTextColor();
		ColorRGB color = definition.colorInherited && inheritedColor != null ? inheritedColor : definition.color;
		InterfaceRender.setColorState(color);
		
		//Check for auto-scaling.
		//Also reduce scale by 1/16 if we're not using pixel coords.  Entity JSON assumes 1 unit is 1 block, not 1px.
		float scale = pixelCoords ? definition.scale : definition.scale/16F;
		int wrapWidth = definition.wrapWidth;
		if(definition.autoScale){
			//Get the string width.  This is in text-pixels.
			//We scale this to the actual pixel-width by multiplying it by the incoming scale.
			float stringWidth = scale*fontData.getStringWidth(text);
			
			//If the string width in pixels is greater than the wrap width, adjust scale.
			//We also need to cancel wrapping if our scaled value is within bounds.
			float scaleFactor = stringWidth > 0 ? wrapWidth/stringWidth : 1.0F;
			if(stringWidth/scale > wrapWidth){
				if(stringWidth > wrapWidth){
					scale *= scaleFactor;
				}
				wrapWidth = 0;
			}
		}
		
		//Render the text.
		mutablePosition.setTo(definition.pos);
		if(pixelCoords){
			//GUI coordinates use top-left as origin rather than bottom-left.
			mutablePosition.y = -mutablePosition.y;
		}
		fontData.renderText(text, mutablePosition, definition.rot, TextAlignment.values()[definition.renderPosition], scale, wrapWidth, preScaledFactor, pixelCoords);
		
		//Set light and color back to normal.
		InterfaceRender.setColorState(ColorRGB.WHITE);
		if(definition.lightsUp && entity.renderTextLit()){
			InterfaceRender.setInternalLightingState(true);
		}
	}
	
	/**
	 *  Returns the width of the passed-in text string.  This will be in pixels.
	 *  Note that this may not directly correspond to font-pixels, as higher-res
	 *  fonts may have multiple font-pixels per texture pixel.
	 */
	public static float getStringWidth(String text, String fontName){
		return getFontData(fontName).getStringWidth(text);
	}
	
	/**
	 *  Returns the correct font rendering charset for the passed-in font,
	 *  creating it if it does not exist.  Does not bind the actual texture,
	 *  but does load it for calculating charset bounds.
	 */
	private static FontData getFontData(String fontName){
		FontData fontData = fontDatas.get(fontName);
		if(fontData == null){
			fontData = new FontData(fontName);
			fontDatas.put(fontName, fontData);
		}
		return fontData;
	}
	
	/**
	 *  Class used for storing ASCII charset data for font rendering.
	 *  Contains font bounds and sizes.  Data is stored in an array with each
	 *  element representing the char index on the texture sheet.
	 */
	private static class FontData{
		private static final byte CHARS_PER_ROWCOL = 16;
		private static final byte DEFAULT_PIXELS_PER_CHAR = 8;
		private static final byte CHAR_SPACING = 1;
		private static final char[] TEXTURE_CHARSET = generateCharset();
		
		private final String fontLocation;
		/**Char width, in game pixels (not text-pixels).  May be fractions of a pixel for non-standard fonts.**/
		private final float charWidths[] = new float[Character.MAX_VALUE];
		//UV-offsets, in 0-1 coordinate space.
		private final float offsetsMinU[] = new float[Character.MAX_VALUE];
		private final float offsetsMaxU[] = new float[Character.MAX_VALUE];
		private final float offsetsMinV[] = new float[Character.MAX_VALUE];
		private final float offsetsMaxV[] = new float[Character.MAX_VALUE];
		
		//Mutable helper.
		private final Point3d rotatedVertex = new Point3d();
		
		private FontData(String fontName){
			if(fontName == null){
				fontName = MasterLoader.MODID + ":default";
			}
			this.fontLocation = "/assets/" + fontName.substring(0, fontName.indexOf(":")) + "/textures/fonts/" + fontName.substring(fontName.indexOf(":") + 1) + ".png";
			BufferedImage bufferedImage;
			try{
				bufferedImage = ImageIO.read(RenderText.class.getResourceAsStream(fontLocation));
			}catch(Exception e){
				throw new NullPointerException("Could not load font file " + fontName + " for reasons?");
			}
			
			//Calculate min/max.
			//i is the row we are looking on.
			//j is the column.
			//x is the texture pixel-row.
			//y is the texture pixel column.
			//For each char, we look at the row/col bounds and check every pixel in the col
			//starting from left to right.  If we hit a pixel in this col sub-section, we know we
			//haven't found the end of the char and need to continue.
			//Order is all chars in row 1, then row 2, etc.
			byte pixelsPerRowCol = (byte) (bufferedImage.getHeight()/CHARS_PER_ROWCOL);
			for(int charRow=0; charRow<CHARS_PER_ROWCOL; ++charRow){
				for(int charCol=0; charCol<CHARS_PER_ROWCOL; ++charCol){
					char charChecking = TEXTURE_CHARSET[charRow*CHARS_PER_ROWCOL + charCol];
					offsetsMinU[charChecking] = charCol/(float)CHARS_PER_ROWCOL;
					//Normally we'd invert the UV-mapping here to compensate for the inverted texture center.
					//But in this case, we don't have to do that.  Still not 100% sure on the math, but it works?
					offsetsMaxV[charChecking] = (charRow)/(float)CHARS_PER_ROWCOL;
					offsetsMinV[charChecking] = (charRow+1)/(float)CHARS_PER_ROWCOL;
					boolean foundPixelThisCol;
					for(int pixelCol=charCol*pixelsPerRowCol; pixelCol<(charCol+1)*pixelsPerRowCol; ++pixelCol){
						foundPixelThisCol = false;
						//Check all rows of pixels in this column to see if we have one.
						for(int pixelRow=charRow*pixelsPerRowCol; pixelRow<(charRow+1)*pixelsPerRowCol; ++pixelRow){
							if(bufferedImage.getRGB(pixelCol, pixelRow) != 0){
								foundPixelThisCol = true;
								break;
							}
						}
						if(!foundPixelThisCol){
							offsetsMaxU[charChecking] = pixelCol/(float)pixelsPerRowCol/CHARS_PER_ROWCOL;
							charWidths[charChecking] = (pixelCol-charCol*pixelsPerRowCol)*DEFAULT_PIXELS_PER_CHAR/(float)pixelsPerRowCol;
							break;
						}
					}
					//Need this for invisible chars and those of a whole width.
					if(charWidths[charChecking] == 0){
						offsetsMaxU[charChecking] = (charCol+1)/(float)CHARS_PER_ROWCOL;
						charWidths[charChecking] = charChecking == ' ' ? DEFAULT_PIXELS_PER_CHAR/2 : DEFAULT_PIXELS_PER_CHAR;
					}
				}
			}
		}
		
		private float getStringWidth(String text){
			float width = 0;
			for(char textChar : text.toCharArray()){
				width += charWidths[textChar];
			}
			return width;
		}
		
		private void renderText(String text, Point3d position, Point3d rotation, TextAlignment alignment, float scale, int wrapWidth, float preScaledFactor, boolean pixelCoords){
			//Create the Float array for the chars.  6 vertices per char for rendering.
			float[][] textObject = new float[text.length()*6][8];
			
			//Check if we need to adjust our offset for our alignment.
			float alignmentOffset = 0;
			if(alignment.equals(TextAlignment.CENTERED)){
				alignmentOffset = (wrapWidth == 0 ? -getStringWidth(text) - text.length()*CHAR_SPACING : -wrapWidth)/2F;
			}else if(alignment.equals(TextAlignment.RIGHT_ALIGNED)){
				alignmentOffset = (-getStringWidth(text) - text.length()*CHAR_SPACING);
			}
			
			//Loop through all the chars and get their UV points and add them to the object.
			//If we get past our wrapWidth, or we find a newline, we need to wrap the text.
			//Note that offsets are in texture pixels, not text-pixels.
			int currentOffset = 0;
			int currentLineOffset = 0;
			
			//Pre-calculate rotation of normals, as these won't change.
			boolean doRotation = !rotation.isZero();
			float[] normals = new float[]{0.0F, 0.0F, scale*preScaledFactor};
			if(doRotation){
				Point3d rotatedNormals = new Point3d(normals[0], normals[1], normals[2]).rotateFine(rotation);
				normals[0] = (float) rotatedNormals.x;
				normals[1] = (float) rotatedNormals.y;
				normals[2] = (float) rotatedNormals.z;
			}
			
			for(int i=0; i<text.length(); ++i){
				char textChar = text.charAt(i);
				if(textChar == '\n'){
					//Go down one line.
					currentOffset = 0;
					currentLineOffset -= DEFAULT_PIXELS_PER_CHAR + CHAR_SPACING;
				}else if(wrapWidth != 0 && currentOffset > wrapWidth){
					//Go backwards in text to find last space and split based on that.
					//After this we will re-do the parsing of the prior chars on the next line.
					for(int j=i; j>0; --j){
						char priorChar = text.charAt(j);
						if(priorChar == ' '){
							i = j;
							currentOffset = 0;
							currentLineOffset -= DEFAULT_PIXELS_PER_CHAR + CHAR_SPACING;
							break;
						}
					}
				}else{
					//Do normal char addition to the map of chars to draw.
					float charWidth = charWidths[textChar];
					for(int j=0; j<6; ++j){
						float[] charVertex = new float[8];
						switch(j){
							case(0):{//Bottom-right
								charVertex[0] = alignmentOffset + currentOffset + charWidth;
								charVertex[1] = currentLineOffset - DEFAULT_PIXELS_PER_CHAR;
								charVertex[3] = offsetsMaxU[textChar];
								charVertex[4] = offsetsMinV[textChar];
								break;
							}
							case(1):{//Top-right
								charVertex[0] = alignmentOffset + currentOffset + charWidth;
								charVertex[1] = currentLineOffset;
								charVertex[3] = offsetsMaxU[textChar];
								charVertex[4] = offsetsMaxV[textChar];
								break;
							}
							case(2):{//Top-left
								charVertex[0] = alignmentOffset + currentOffset;
								charVertex[1] = currentLineOffset;
								charVertex[3] = offsetsMinU[textChar];
								charVertex[4] = offsetsMaxV[textChar];
								break;
							}
							case(3):{//Bottom-right
								charVertex[0] = alignmentOffset + currentOffset + charWidth;
								charVertex[1] = currentLineOffset - DEFAULT_PIXELS_PER_CHAR;
								charVertex[3] = offsetsMaxU[textChar];
								charVertex[4] = offsetsMinV[textChar];
								break;
							}
							case(4):{//Top-left
								charVertex[0] = alignmentOffset + currentOffset;
								charVertex[1] = currentLineOffset;
								charVertex[3] = offsetsMinU[textChar];
								charVertex[4] = offsetsMaxV[textChar];
								break;
							}
							case(5):{//Bottom-left
								charVertex[0] = alignmentOffset + currentOffset;
								charVertex[1] = currentLineOffset - DEFAULT_PIXELS_PER_CHAR;
								charVertex[3] = offsetsMinU[textChar];
								charVertex[4] = offsetsMinV[textChar];						
								break;
							}
						}
						//Z is always 0 initially.  We rotate it to match the actual rotation.
						charVertex[2] = 0.0F;
						
						//Rotate vertices if required.
						if(doRotation){
							rotatedVertex.set(charVertex[0], charVertex[1], charVertex[2]).rotateFine(rotation);
							charVertex[0] = (float) rotatedVertex.x;
							charVertex[1] = (float) rotatedVertex.y;
							charVertex[2] = (float) rotatedVertex.z;
						}
						
						//Set normals.  These will already have been rotated.
						charVertex[5] = normals[0];
						charVertex[6] = normals[1];
						charVertex[7] = normals[2];
						
						//Set char vertex.
						textObject[i*6 + j] = charVertex;
					}
					
					//Increment offset to next char position and set char points.
					currentOffset += charWidth + CHAR_SPACING;
				}
			}
			
			//All points obtained, render.  Color and lighting is done by the calling method and will be set there.
			//Note that if we are doing pixel coords, we will need to use inverted Y as that's going off texture sheet/GUI Y.
			//We will also need to 
			InterfaceRender.bindTexture(fontLocation);
			GL11.glPushMatrix();
			GL11.glTranslated(position.x, position.y, position.z);
			GL11.glScalef(scale, scale, scale);
			InterfaceRender.renderVertices(textObject);
			GL11.glPopMatrix();
			InterfaceRender.recallTexture();
		}
		
		private static char[] generateCharset(){
			char[] charset = new char[CHARS_PER_ROWCOL*CHARS_PER_ROWCOL];
			String charsetString = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000";
			for(int i=0; i<charset.length; ++i){
				charset[i] = charsetString.charAt(i);
			}
			return charset;
		}
	}
	
	/**
	 *  List of enums that define how text is rendered.
	 */
	public static enum TextAlignment{
		CENTERED,
		LEFT_ALIGNED,
		RIGHT_ALIGNED;
	}
}
