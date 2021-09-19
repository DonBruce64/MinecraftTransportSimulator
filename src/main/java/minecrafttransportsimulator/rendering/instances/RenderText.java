package minecrafttransportsimulator.rendering.instances;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.lwjgl.input.Keyboard;
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
	private static final Map<String, FontDataUnicode> unicodeFontDatas = new HashMap<String, FontDataUnicode>();
	private static final Map<String, FontDataASCII> asciiFontDatas = new HashMap<String, FontDataASCII>();
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
		IFontData fontData = getFontData(customFontName);
		mutablePosition.set(x, y, 0);
		//Need to invert 2D GUI text, as it's Y coord origin is top-left VS UV-mapped bottom-left.
		fontData.renderText(text, mutablePosition, FLIPPED_TEXT_FOR_GUIS, alignment, scale, wrapWidth, 1.0F, true, color);
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
		IFontData fontData = getFontData(definition.fontName);
		
		//If we have light-up text, disable lightmap.
		if(definition.lightsUp && entity.renderTextLit()){
			InterfaceRender.setInternalLightingState(false);
		}
		
		//Get the actual color we will need to render with based on JSON.
		ColorRGB inheritedColor = entity.getSecondaryTextColor();
		ColorRGB color = definition.colorInherited && inheritedColor != null ? inheritedColor : definition.color;
		
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
		fontData.renderText(text, mutablePosition, definition.rot, TextAlignment.values()[definition.renderPosition], scale, wrapWidth, preScaledFactor, pixelCoords, color);
		
		//Set light back to normal.
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
	private static IFontData getFontData(String fontName){
		if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)){
			FontDataASCII fontData = asciiFontDatas.get(fontName);
			if(fontData == null){
				fontData = new FontDataASCII(fontName);
				asciiFontDatas.put(fontName, fontData);
			}
			return fontData;
		}else{
			FontDataUnicode fontData = unicodeFontDatas.get(fontName);
			if(fontData == null){
				fontData = new FontDataUnicode(fontName);
				unicodeFontDatas.put(fontName, fontData);
			}
			return fontData;
		}
	}
	
	/**
	 *  Interface for rendering font data.  This can be ASCII or Unicode.
	 */
	private static interface IFontData{
		
		public void renderText(String text, Point3d position, Point3d rotation, TextAlignment alignment, float scale, int wrapWidth, float preScaledFactor, boolean pixelCoords, ColorRGB color);

		public float getStringWidth(String text);
	}
	
	/**
	 *  Class used for storing Unicode charset data for font rendering.
	 *  Contains font bounds and sizes.  Data is stored in an array with each
	 *  element representing the char index on the texture sheet.
	 */
	private static class FontDataUnicode implements IFontData{
		private static final char FORMATTING_CHAR = 167;//'§';
		private static final char BOLD_CHAR = 'l';
		private static final char ITALIC_CHAR = 'o';
		private static final char UNDERLINE_CHAR = 'm';
		private static final char STRIKETHROUGH_CHAR = 'n';
		private static final char RANDOM_CHAR = 'k';
		private static final char RESET_CHAR = 'r';
		
		private static final byte CHARS_PER_ROWCOL = 16;
		private static final int CHARS_PER_TEXTURE_SHEET = CHARS_PER_ROWCOL*CHARS_PER_ROWCOL;
		private static final byte DEFAULT_PIXELS_PER_CHAR = 8;
		private static final byte CHAR_SPACING = 1;
		private static final ColorRGB[] COLORS = new ColorRGB[]{
			new ColorRGB(0, 0, 0),
			new ColorRGB(0, 0, 170),
			new ColorRGB(0, 170, 0),
			new ColorRGB(0, 170, 170),
			new ColorRGB(170, 0, 0),
			new ColorRGB(170, 0, 170),
			new ColorRGB(255, 170, 0),
			new ColorRGB(170, 170, 170),
			new ColorRGB(85, 85, 85),
			new ColorRGB(85, 85, 255),
			new ColorRGB(85, 255, 85),
			new ColorRGB(85, 255, 255),
			new ColorRGB(255, 85, 85),
			new ColorRGB(255, 85, 255),
			new ColorRGB(255, 255, 85),
			new ColorRGB(255, 255, 255)
		};
		private static final FontRenderState[] STATES = FontRenderState.generateDefaults();
		private static final float[][] VERTICES = new float[1000*6][8];
		
		
		/*Texture locations for the font files.**/
		private final String[] fontLocations = new String[Character.MAX_VALUE/CHARS_PER_TEXTURE_SHEET];
		/**Char width, in actual game texture pixels (not font texture pixels).  May be fractions of a pixel if the font is up-scaled.**/
		private final float[] charWidths = new float[Character.MAX_VALUE];
		/**Left-most offset for font text position, from 0-1, relative to the texture png.**/
		private final float[] offsetsMinU = new float[Character.MAX_VALUE];
		/**Right-most offset for font text position, from 0-1, relative to the texture png.**/
		private final float[] offsetsMaxU = new float[Character.MAX_VALUE];
		/**Bottom-most offset for font text position, from 0-1, relative to the texture png.**/
		private final float[] offsetsMinV = new float[Character.MAX_VALUE];
		/**Top-most offset for font text position, from 0-1, relative to the texture png.**/
		private final float[] offsetsMaxV = new float[Character.MAX_VALUE];
		
		
		/**Font render blocks.  These are created initially for use in render calls.  Referencing is as follows:
		 * The first array element is the texture sheet being used.
		 * The second array element is the color.
		 * The third array element is a packed integer of the font state.
		 * This ensures that there will always be one element for any permutation of states.**/
		private static final Map<String, Map<ColorRGB, Map<FontRenderState, FontRenderBlock>>> createdRenderBlocks = new HashMap<String, Map<ColorRGB, Map<FontRenderState, FontRenderBlock>>>();
		/**Active font render blocks.  These are a list of the currently-used {@link #fontRenderBlocks}.
		 * Items are added to this list during string parsing.  At the end, it will be populated and
		 * should be looped over for drawing.*/
		private final Set<FontRenderBlock> activeRenderBlocks = new LinkedHashSet<FontRenderBlock>();
		/**Current index for pooled set of vertices in {@link #VERTICES}.  Used to track which vertices are being used.*/
		private int currentVertexIndex;
		/**Mutable helper for rotating vertices.**/
		private final Point3d rotatedVertex = new Point3d();
		
		
		private FontDataUnicode(String fontName){
			//Get font locations.
			String fontBaseLocation;
			if(fontName == null){
				fontBaseLocation = "/assets/minecraft/textures/font/unicode_page_";
			}else{
				fontBaseLocation = "/assets/" + fontName.substring(0, fontName.indexOf(":")) + "/textures/fonts/" + fontName.substring(fontName.indexOf(":") + 1) + "/unicode_page_";
			}
			
			//Parse char widths.
			for(int i=0; i<fontLocations.length; ++i){
				fontLocations[i] = String.format("%s%02d.png", fontBaseLocation, i);
				BufferedImage bufferedImage;
				try{
					bufferedImage = ImageIO.read(RenderText.class.getResourceAsStream(fontLocations[i]));
				}catch(Exception e){
					//Just continue, as we don't care about this file.  Not all files may be present for any given font.
					continue;
				}
				
				//Calculate min/max.
				//For each char, we look at the row/col bounds and check every pixel in the col
				//starting from right to left.  If we hit a pixel in this col sub-section, we know we
				//have found the end of the char and that's its width.
				//Order is all chars in row 1, then row 2, etc.
				int pixelsPerSide = bufferedImage.getHeight();
				byte pixelsPerRowCol = (byte) (pixelsPerSide/CHARS_PER_ROWCOL);
				for(int charRow=0; charRow<CHARS_PER_ROWCOL; ++charRow){
					for(int charCol=0; charCol<CHARS_PER_ROWCOL; ++charCol){
						//Get char and set defaults.
						char charChecking = (char) (i*CHARS_PER_TEXTURE_SHEET + charRow*CHARS_PER_ROWCOL + charCol);
						if(charChecking == ' '){
							//Need to map this to 1px away from the edge to avoid rendering the border, space is 1/2 char width.
							offsetsMinU[charChecking] = 1F/pixelsPerSide;
							offsetsMaxU[charChecking] = 1F/pixelsPerSide;
							offsetsMaxV[charChecking] = 1F/pixelsPerSide;
							offsetsMinV[charChecking] = 1F/pixelsPerSide;
							charWidths[charChecking] = DEFAULT_PIXELS_PER_CHAR/2;
						}else{
							offsetsMinU[charChecking] = charCol/(float)CHARS_PER_ROWCOL;
							offsetsMaxU[charChecking] = (charCol+1)/(float)CHARS_PER_ROWCOL;
							//Normally we'd invert the UV-mapping here to compensate for the inverted texture center.
							//But in this case, we don't have to do that.  Still not 100% sure on the math, but it works?
							offsetsMaxV[charChecking] = (charRow)/(float)CHARS_PER_ROWCOL;
							offsetsMinV[charChecking] = (charRow+1)/(float)CHARS_PER_ROWCOL;
							charWidths[charChecking] = DEFAULT_PIXELS_PER_CHAR;
							
							//Check each pixel in the pixel sub-col to get the actual width of the char.
							boolean foundPixelThisCol = false;
							for(int pixelCol=(charCol+1)*pixelsPerRowCol-1; pixelCol>=charCol*pixelsPerRowCol; --pixelCol){
								//Check all rows of pixels in this column to see if we have one.
								for(int pixelRow=charRow*pixelsPerRowCol; pixelRow<(charRow+1)*pixelsPerRowCol; ++pixelRow){
									//Check for alpha and color.  Some systems write color, but no alpha to a pixel.
									int pixelValue = bufferedImage.getRGB(pixelCol, pixelRow);
									if(pixelValue != 0 && (pixelValue >> 24) != 0){
										//Found a pixel, we must have this as our UV.
										++pixelCol;
										offsetsMaxU[charChecking] = pixelCol/(float)pixelsPerRowCol/CHARS_PER_ROWCOL;
										charWidths[charChecking] = (pixelCol-charCol*pixelsPerRowCol)*DEFAULT_PIXELS_PER_CHAR/(float)pixelsPerRowCol;
										foundPixelThisCol = true;
										break;
									}
								}
								if(foundPixelThisCol){
									break;
								}
							}
						}	
					}
				}
			}
		}
		
		@Override
		public float getStringWidth(String text){
			float width = 0;
			for(char textChar : text.toCharArray()){
				width += charWidths[textChar];
			}
			return width;
		}
		
		@Override
		public void renderText(String text, Point3d position, Point3d rotation, TextAlignment alignment, float scale, int wrapWidth, float preScaledFactor, boolean pixelCoords, ColorRGB color){
			if(!text.isEmpty()){
				//Multiply scale by (7/8)/(10/16) = 1.4.
				//This is because normally the font height is 7px of the 8 total.
				//But unicode uses 10px of the 16.  This makes it slightly smaller if we don't do this.
				if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
				scale *= 1.4;
				
				//Divide the wrap width by the scale.
				//This is required to ensure it's kept to pixel measurements.
				if(wrapWidth != 0){
					wrapWidth /= scale;
				}
				
				//Pre-calculate rotation of normals, as these won't change.
				boolean doRotation = !rotation.isZero();
				float[] normals = new float[]{0.0F, 0.0F, scale*preScaledFactor};
				if(doRotation){
					Point3d rotatedNormals = new Point3d(normals[0], normals[1], normals[2]).rotateFine(rotation);
					normals[0] = (float) rotatedNormals.x;
					normals[1] = (float) rotatedNormals.y;
					normals[2] = (float) rotatedNormals.z;
				}
				
				//Check if we need to adjust our offset for our alignment.
				//While this will be slightly off due to formatting and non-printable chars in the string,
				//it is better than trying to pre-strip them and then parse the text after.
				//Besides, who is going to do word-wrapping on fancy text?
				float alignmentOffset = 0;
				if(alignment.equals(TextAlignment.CENTERED)){
					alignmentOffset = (wrapWidth == 0 ? -getStringWidth(text) - text.length()*CHAR_SPACING : -wrapWidth)/2F;
				}else if(alignment.equals(TextAlignment.RIGHT_ALIGNED)){
					alignmentOffset = (-getStringWidth(text) - text.length()*CHAR_SPACING);
				}
				
				//Clear out UVs from blocks rendered prior, and then clear out the active block list.
				for(FontRenderBlock block : activeRenderBlocks){
					block.vertices.clear();
				}
				activeRenderBlocks.clear();
				
				
				//Loop through all the chars and get their UV points and add them to the object.
				//If we get past our wrapWidth, or we find a newline, we need to wrap the text.
				//Note that offsets are in in-game texture pixels, not font-texture pixels.
				currentVertexIndex = 0;
				int currentOffset = 0;
				int currentLineOffset = 0;
				ColorRGB currentColor = color;
				FontRenderState currentState = STATES[0];
				for(int i=0; i<text.length(); ++i){
					char textChar = text.charAt(i);
					
					//Check if we are a formatting code before doing any other parsing.
					if(textChar == FORMATTING_CHAR){
						//Get the format code and apply operation.
						char formattingChar = text.charAt(++i);
						switch(formattingChar){
							case (BOLD_CHAR) : currentState = STATES[currentState.index | FontRenderState.BOLD_BIT_INDEX]; break;
							case (ITALIC_CHAR) : currentState = STATES[currentState.index | FontRenderState.ITALIC_BIT_INDEX]; break;
							case (UNDERLINE_CHAR) : currentState = STATES[currentState.index | FontRenderState.UNDERLINE_BIT_INDEX]; break;
							case (STRIKETHROUGH_CHAR) : currentState = STATES[currentState.index | FontRenderState.STRIKETHROUGH_BIT_INDEX]; break;
							case (RANDOM_CHAR) : currentState = STATES[currentState.index | FontRenderState.RANDOM_BIT_INDEX]; break;
							case (RESET_CHAR) : currentState = STATES[0]; currentColor = color; break;
							default: currentColor = COLORS[Integer.decode("0x"+formattingChar)]; break;
						}
						//Go back though the loop again to the next char.
						continue;
					}
					
					//Not a formatting char, do normal rendering.
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
							}else{
								//Need to remove vertices in list so they don't get rendered.
								FontRenderBlock priorRenderBlock = getBlockFor(priorChar, currentColor, currentState);
								int vertMin = priorRenderBlock.vertices.size() - priorRenderBlock.state.vertexStep;
								int vertMax = priorRenderBlock.vertices.size() - 1;
								for(int k=vertMax; k>=vertMin; --k){
									priorRenderBlock.vertices.remove(k);
								}
								currentVertexIndex -= priorRenderBlock.state.vertexStep;
							}
						}
					}else{
						//Actual char to render.
						//Do normal char addition to the map of chars to draw.
						//If we are bold, we will double-render slightly offset.
						//If we are underline, add an underline overlay.
						//If we are italic, we slightly skew the UV map by 1px.
						//If we are strikethough, we add a strikethough overlay.
						//If we are random, we randomize the char we have (0/9, A/Z).
						FontRenderBlock currentRenderBlock = getBlockFor(textChar, currentColor, currentState);
						if(currentState.random){
							textChar = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt((int) (Math.random()*36));
							currentRenderBlock = getBlockFor(textChar, currentColor, currentState);
						}
						float charWidth = charWidths[textChar];
						int startingVertex = currentVertexIndex;
						for(int j=0; j<currentRenderBlock.state.vertexStep; ++j){
							//Get the next free vertex and set its properties.
							float[] charVertex = VERTICES[currentVertexIndex++];
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
									if(currentRenderBlock.state.italic){
										charVertex[0] += 1;
									}
									charVertex[1] = currentLineOffset;
									charVertex[3] = offsetsMaxU[textChar];
									charVertex[4] = offsetsMaxV[textChar];
									break;
								}
								case(2):{//Top-left
									charVertex[0] = alignmentOffset + currentOffset;
									if(currentRenderBlock.state.italic){
										charVertex[0] += 1;
									}
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
									if(currentRenderBlock.state.italic){
										charVertex[0] += 1;
									}
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
								default: {
									//Custom vertex, either bold, underline or strikethough.
									float[] masterVertex = VERTICES[startingVertex + j - 6];
									if(currentState.bold && j < 12){
										charVertex[0] = masterVertex[0] + 0.3F;
										charVertex[1] = masterVertex[1] + 0.3F;
										charVertex[2] = masterVertex[2];
										charVertex[3] = masterVertex[3];
										charVertex[4] = masterVertex[4];
									}else{
										//Set position to master and set custom char.
										charVertex[0] = masterVertex[0];
										charVertex[1] = masterVertex[1] + CHAR_SPACING;
										charVertex[2] = masterVertex[2];
										char customChar = currentState.underline ? '_' : '-';
										
										//May need to change render blocks here.
										if(j%6 == 0){
											currentRenderBlock = getBlockFor(customChar, currentColor, currentState);
										}
										
										switch(j%6){
											case(0):{//Bottom-right
												charVertex[0] += CHAR_SPACING;
												charVertex[3] = offsetsMaxU[customChar];
												charVertex[4] = offsetsMinV[customChar];
												break;
											}
											case(1):{//Top-right
												charVertex[0] += CHAR_SPACING;
												charVertex[3] = offsetsMaxU[customChar];
												charVertex[4] = offsetsMaxV[customChar];
												break;
											}
											case(2):{//Top-left
												charVertex[0] -= CHAR_SPACING;
												charVertex[3] = offsetsMinU[customChar];
												charVertex[4] = offsetsMaxV[customChar];
												break;
											}
											case(3):{//Bottom-right
												charVertex[0] += CHAR_SPACING;
												charVertex[3] = offsetsMaxU[customChar];
												charVertex[4] = offsetsMinV[customChar];
												break;
											}
											case(4):{//Top-left
												charVertex[0] -= CHAR_SPACING;
												charVertex[3] = offsetsMinU[customChar];
												charVertex[4] = offsetsMaxV[customChar];
												break;
											}
											case(5):{//Bottom-left
												charVertex[0] -= CHAR_SPACING;
												charVertex[3] = offsetsMinU[customChar];
												charVertex[4] = offsetsMinV[customChar];						
												break;
											}
										}	
									}
								}
							}
							
							if(j < 6){
								//Z is always 0 initially.  We rotate it to match the actual rotation.
								charVertex[2] = 0.0F;
								
								//Rotate vertices if required.
								if(doRotation){
									rotatedVertex.set(charVertex[0], charVertex[1], charVertex[2]).rotateFine(rotation);
									charVertex[0] = (float) rotatedVertex.x;
									charVertex[1] = (float) rotatedVertex.y;
									charVertex[2] = (float) rotatedVertex.z;
								}
							}
							
							//Set normals.  These will already have been rotated.
							charVertex[5] = normals[0];
							charVertex[6] = normals[1];
							charVertex[7] = normals[2];
							
							//Add char vertex to render block.
							currentRenderBlock.vertices.add(charVertex);
						}
						
						//Increment offset to next char position and set char points and add rener block to active list.
						currentOffset += charWidth + CHAR_SPACING;
						activeRenderBlocks.add(currentRenderBlock);
					}
				}
				
				//All points obtained, render.  Lighting is done by the calling method and will be set there, color is set here.
				String boundTexture = null;
				ColorRGB boundColor = null;
				GL11.glPushMatrix();
				GL11.glTranslated(position.x, position.y, position.z);
				GL11.glScalef(scale, scale, scale);
				for(FontRenderBlock block : activeRenderBlocks){
					if(!block.texture.equals(boundTexture)){
						InterfaceRender.bindTexture(block.texture);
						boundTexture = block.texture;
					}
					if(!block.color.equals(boundColor)){
						InterfaceRender.setColorState(block.color);
						boundColor = block.color;
					}
					InterfaceRender.renderVertices(block.vertices);
				}
				GL11.glPopMatrix();
				InterfaceRender.recallTexture();
				InterfaceRender.setColorState(ColorRGB.WHITE);
			}
		}
		
		private FontRenderBlock getBlockFor(char textChar, ColorRGB color, FontRenderState state){
			//First get the font block;
			String font = fontLocations[textChar/CHARS_PER_TEXTURE_SHEET];
			Map<ColorRGB, Map<FontRenderState, FontRenderBlock>> map1 = createdRenderBlocks.get(font);
			if(map1 == null){
				map1 = new HashMap<ColorRGB, Map<FontRenderState, FontRenderBlock>>();
				createdRenderBlocks.put(font, map1);
			}
			
			Map<FontRenderState, FontRenderBlock> map2 = map1.get(color);
			if(map2 == null){
				map2 = new HashMap<FontRenderState, FontRenderBlock>();
				map1.put(color, map2);
			}
			
			FontRenderBlock block = map2.get(state);
			if(block == null){
				block = new FontRenderBlock(font, color, state);
				map2.put(state, block);
			}
			return block;
		}
		
		private class FontRenderBlock{
			private final String texture;
			private final ColorRGB color;
			private final FontRenderState state;
			private final List<float[]> vertices = new ArrayList<float[]>();
			
			private FontRenderBlock(String texture, ColorRGB color, FontRenderState state){
				this.texture = texture;
				this.color = color;
				this.state = state;
			}
		}
		
		private static class FontRenderState{
			private static final int BOLD_BIT_INDEX = 1;
			private static final int ITALIC_BIT_INDEX = 2;
			private static final int UNDERLINE_BIT_INDEX = 4;
			private static final int STRIKETHROUGH_BIT_INDEX = 8;
			private static final int RANDOM_BIT_INDEX = 16;
					
			private final int index;
			private final boolean bold;
			private final boolean italic;
			private final boolean underline;
			private final boolean strikethrough;
			private final boolean random;
			private final int vertexStep;
			
			private FontRenderState(int index){
				this.index = index;
				this.bold = (index & BOLD_BIT_INDEX) == BOLD_BIT_INDEX;
				this.italic = (index & ITALIC_BIT_INDEX) == ITALIC_BIT_INDEX;
				this.underline = (index & UNDERLINE_BIT_INDEX) == UNDERLINE_BIT_INDEX;
				this.strikethrough = (index & STRIKETHROUGH_BIT_INDEX) == STRIKETHROUGH_BIT_INDEX;
				this.random = (index & RANDOM_BIT_INDEX) == RANDOM_BIT_INDEX;
				
				//6 vertices per char, more if we have conditions.
				int charsToDraw = 1;
				if(bold) ++charsToDraw;
				if(underline) ++charsToDraw;
				if(strikethrough) ++charsToDraw;
				this.vertexStep = 6*charsToDraw;
			}
			
			public static FontRenderState[] generateDefaults(){
				FontRenderState[] states = new FontRenderState[(int) Math.pow(2, 5)];
				for(int i=0; i<states.length; ++i){
					states[i] = new FontRenderState(i);
				}
				return states;
			}
		}
	}
	
	/**
	 *  Class used for storing ASCII charset data for font rendering.
	 *  Contains font bounds and sizes.  Data is stored in an array with each
	 *  element representing the char index on the texture sheet.
	 */
	private static class FontDataASCII implements IFontData{
		private static final byte CHARS_PER_ROWCOL = 16;
		private static final byte DEFAULT_PIXELS_PER_CHAR = 8;
		private static final byte CHAR_SPACING = 1;
		private static final char[] TEXTURE_CHARSET = generateCharset();
		private static final ColorRGB[] COLORS = new ColorRGB[]{
			new ColorRGB(0, 0, 0),
			new ColorRGB(0, 0, 170),
			new ColorRGB(0, 170, 0),
			new ColorRGB(0, 170, 170),
			new ColorRGB(170, 0, 0),
			new ColorRGB(170, 0, 170),
			new ColorRGB(255, 170, 0),
			new ColorRGB(170, 170, 170),
			new ColorRGB(85, 85, 85),
			new ColorRGB(85, 85, 255),
			new ColorRGB(85, 255, 85),
			new ColorRGB(85, 255, 255),
			new ColorRGB(255, 85, 85),
			new ColorRGB(255, 85, 255),
			new ColorRGB(255, 255, 85),
			new ColorRGB(255, 255, 255)
		};
		
		
		
		private final String fontLocation;
		/**Char width, in game pixels (not text-pixels).  May be fractions of a pixel for non-standard fonts.**/
		private final float charWidths[] = new float[Character.MAX_VALUE];
		//UV-offsets, in 0-1 coordinate space.
		private final float offsetsMinU[] = new float[Character.MAX_VALUE];
		private final float offsetsMaxU[] = new float[Character.MAX_VALUE];
		private final float offsetsMinV[] = new float[Character.MAX_VALUE];
		private final float offsetsMaxV[] = new float[Character.MAX_VALUE];
		
		//Mutable helpers and states.
		/**Bits: 1=bold, 2=italic, 3=underline,4=strikethough,5=random**/
		private final int[] fontStates = new int[100];
		private final int[] stateChangeOffsetsInString = new int[100];
		private final ColorRGB[] colorChangeValues = new ColorRGB[100];
		private final float[][][] fontVertices = new float[100][][];
		private final Point3d rotatedVertex = new Point3d();
		
		
		private FontDataASCII(String fontName){
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
					offsetsMaxU[charChecking] = (charCol+1)/(float)CHARS_PER_ROWCOL;
					//Normally we'd invert the UV-mapping here to compensate for the inverted texture center.
					//But in this case, we don't have to do that.  Still not 100% sure on the math, but it works?
					offsetsMaxV[charChecking] = (charRow)/(float)CHARS_PER_ROWCOL;
					offsetsMinV[charChecking] = (charRow+1)/(float)CHARS_PER_ROWCOL;
					
					//Space is 1/2 width, all else are normal width.
					charWidths[charChecking] = charChecking == ' ' ? DEFAULT_PIXELS_PER_CHAR/2 : DEFAULT_PIXELS_PER_CHAR;
					
					//Check each pixel in the col to get the actual width of the char.
					boolean foundPixelThisCol = false;
					for(int pixelCol=(charCol+1)*pixelsPerRowCol-1; pixelCol>=charCol*pixelsPerRowCol; --pixelCol){
						//Check all rows of pixels in this column to see if we have one.
						for(int pixelRow=charRow*pixelsPerRowCol; pixelRow<(charRow+1)*pixelsPerRowCol; ++pixelRow){
							if(bufferedImage.getRGB(pixelCol, pixelRow) != 0){
								//Found a pixel, we must have this as our UV.
								++pixelCol;
								offsetsMaxU[charChecking] = pixelCol/(float)pixelsPerRowCol/CHARS_PER_ROWCOL;
								charWidths[charChecking] = (pixelCol-charCol*pixelsPerRowCol)*DEFAULT_PIXELS_PER_CHAR/(float)pixelsPerRowCol;
								foundPixelThisCol = true;
								break;
							}
						}
						if(foundPixelThisCol){
							break;
						}
					}
				}
			}
		}
		
		@Override
		public float getStringWidth(String text){
			float width = 0;
			for(char textChar : text.toCharArray()){
				width += charWidths[textChar];
			}
			return width;
		}
		
		@Override
		public void renderText(String text, Point3d position, Point3d rotation, TextAlignment alignment, float scale, int wrapWidth, float preScaledFactor, boolean pixelCoords, ColorRGB color){
			if(!text.isEmpty()){
				//Divide the wrap width by the scale.
				//This is required to ensure it's kept to pixel measurements.
				if(wrapWidth != 0){
					wrapWidth /= scale;
				}
				
				//Pre-calculate rotation of normals, as these won't change.
				boolean doRotation = !rotation.isZero();
				float[] normals = new float[]{0.0F, 0.0F, scale*preScaledFactor};
				if(doRotation){
					Point3d rotatedNormals = new Point3d(normals[0], normals[1], normals[2]).rotateFine(rotation);
					normals[0] = (float) rotatedNormals.x;
					normals[1] = (float) rotatedNormals.y;
					normals[2] = (float) rotatedNormals.z;
				}
				
				//Parse the string to find all format changes.
				//Init states to default.
				int totalTextChunks = 0;
				String stringToParse = text;
				String stringToRender = "";
				fontStates[totalTextChunks] = 0;
				colorChangeValues[totalTextChunks] = color;
				stateChangeOffsetsInString[totalTextChunks] = 0;
				
				while(!stringToParse.isEmpty()){
					//If we found a format offset, reduce string length and adjust formatting.
					int formatOffset = stringToParse.indexOf("§");
					if(formatOffset != -1){
						if(formatOffset != 0){
							//Need to get next formatting state for this operation.
							++totalTextChunks;
							fontStates[totalTextChunks] = fontStates[totalTextChunks-1];
							colorChangeValues[totalTextChunks] = colorChangeValues[totalTextChunks-1];
							stateChangeOffsetsInString[totalTextChunks] = formatOffset + stateChangeOffsetsInString[totalTextChunks-1];
						}
						
						//Get formatting code data and indexes.
						char formattingCode = stringToParse.charAt(formatOffset + 1);
						switch(formattingCode){
							case ('l') : fontStates[totalTextChunks] = fontStates[totalTextChunks] & 1; break;
							case ('o') : fontStates[totalTextChunks] = fontStates[totalTextChunks] & 2; break;
							case ('n') : fontStates[totalTextChunks] = fontStates[totalTextChunks] & 4; break;
							case ('m') : fontStates[totalTextChunks] = fontStates[totalTextChunks] & 8; break;
							case ('k') : fontStates[totalTextChunks] = fontStates[totalTextChunks] & 16; break;
							case ('r') : fontStates[totalTextChunks] = 0; break;
							default: colorChangeValues[totalTextChunks] = COLORS[Integer.decode("0x"+formattingCode)]; break;
						}
						
						//Remove the two formatting chars from the string, and continue parsing if needed.
						stringToRender += stringToParse.substring(0, formatOffset);
						stringToParse = stringToParse.substring(formatOffset+2);
						if(formatOffset == 0){
							continue;
						}else{
							//Need to remove 1 from the total text chunks as it was added before.
							--totalTextChunks;
						}
					}else{
						//No formatting.  Offset to string length to parse out remaining string.
						formatOffset = stringToParse.length();
						stringToRender += stringToParse;
						stringToParse = "";
					}
					
					//Have offset of amount of string to put in this chunk section, add it.
					//Need to make vertices.  6 per char for tri rendering.  Need to use two chars for underline or strikethough.
					int vertexCount = 6;
					if((fontStates[totalTextChunks] & 4) != 0){
						vertexCount += 6;
					}
					if((fontStates[totalTextChunks] & 8) != 0){
						vertexCount += 6;
					}
					fontVertices[totalTextChunks] = new float[formatOffset*vertexCount][8];
					
					//Go to the next chunk for parsing.
					++totalTextChunks;
				}
				
				//Check if we need to adjust our offset for our alignment.
				//Needs to be done after we strip out formatting.
				float alignmentOffset = 0;
				if(alignment.equals(TextAlignment.CENTERED)){
					alignmentOffset = (wrapWidth == 0 ? -getStringWidth(stringToRender) - stringToRender.length()*CHAR_SPACING : -wrapWidth)/2F;
				}else if(alignment.equals(TextAlignment.RIGHT_ALIGNED)){
					alignmentOffset = (-getStringWidth(stringToRender) - stringToRender.length()*CHAR_SPACING);
				}
				
				//Loop through all the chars and get their UV points and add them to the object.
				//If we get past our wrapWidth, or we find a newline, we need to wrap the text.
				//Note that offsets are in texture pixels, not text-pixels.
				int currentOffset = 0;
				int currentLineOffset = 0;
				int currentTextChunk = -1;
				int currentTextChunkOffset = -1;
				int vertexStep = 0;
				float[][] currentFontVertices = null;
				
				boolean italic = false;
				boolean underline = false;
				boolean strikethrough = false;
				boolean random = false;
				for(int i=0; i<stringToRender.length(); ++i){
					//If we are above the offset, then increment it.
					if(currentTextChunk+1 < totalTextChunks){
						if(stateChangeOffsetsInString[currentTextChunk+1] <= i){
							++currentTextChunk;
							currentTextChunkOffset = stateChangeOffsetsInString[currentTextChunk];
							currentFontVertices = fontVertices[currentTextChunk];
							
							int currentFontState = fontStates[currentTextChunk];
							italic = (currentFontState & 2) != 0;
							underline = (currentFontState & 4) != 0;
							strikethrough = (currentFontState & 8) != 0;
							random = (currentFontState & 16) != 0;
							
							vertexStep = 6;
							if(underline) vertexStep += 6;
							if(strikethrough) vertexStep += 6;
						}
					}
					
					char textChar = stringToRender.charAt(i);
					if(textChar == '\n'){
						//Go down one line.
						currentOffset = 0;
						currentLineOffset -= DEFAULT_PIXELS_PER_CHAR + CHAR_SPACING;
					}else if(wrapWidth != 0 && currentOffset > wrapWidth){
						//Go backwards in text to find last space and split based on that.
						//After this we will re-do the parsing of the prior chars on the next line.
						for(int j=i; j>0; --j){
							char priorChar = stringToRender.charAt(j);
							if(priorChar == ' '){
								i = j;
								while(i < currentTextChunkOffset){
									//Need to go backwards in chunk offsets to get the last proper state.
									currentTextChunkOffset = stateChangeOffsetsInString[--currentTextChunk];
								}
								currentOffset = 0;
								currentLineOffset -= DEFAULT_PIXELS_PER_CHAR + CHAR_SPACING;
								break;
							}
						}
					}else{
						//Do normal char addition to the map of chars to draw.
						//If we are bold (bit 1), we will just use a different texture during rendering.
						//If we are underline (bit 2), add the underscore as an overlay char.
						//If we are italic (bit 3), we slightly skew the UV map by 1px.
						//If we are strikethough (bit 4), we add the strikethough as an overlay.
						//If we are random (bit 5), we randomize the char we have.
						float charWidth = charWidths[textChar];
						for(int j=0; j<vertexStep; ++j){
							float[] charVertex = currentFontVertices[(i-currentTextChunkOffset)*vertexStep + j];
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
								default: {
									//Custom vertex, either underline or strikethough.
									int masterIndex = j%6;
									float[] masterVertex = currentFontVertices[(i-currentTextChunkOffset)*vertexStep + masterIndex];
									charVertex[0] = masterVertex[0];
									charVertex[1] = masterVertex[1];
									charVertex[2] = masterVertex[2];
									char boxChar = '\u2588';
									float boxMinV;
									float boxMaxV;
									if(underline && j < 12){
										boxMinV = offsetsMinV[boxChar];
										boxMaxV = offsetsMinV[boxChar] + 1F/CHARS_PER_ROWCOL;
									}else{
										boxMinV = offsetsMinV[boxChar] + 7F/CHARS_PER_ROWCOL;
										boxMaxV = offsetsMinV[boxChar] + 8F/CHARS_PER_ROWCOL;
									}
									switch(masterIndex){
										case(0):{//Bottom-right
											charVertex[3] = offsetsMaxU[boxChar];
											charVertex[4] = boxMinV;
											break;
										}
										case(1):{//Top-right
											charVertex[3] = offsetsMaxU[boxChar];
											charVertex[4] = boxMaxV;
											break;
										}
										case(2):{//Top-left
											charVertex[3] = offsetsMinU[boxChar];
											charVertex[4] = boxMaxV;
											break;
										}
										case(3):{//Bottom-right
											charVertex[3] = offsetsMaxU[boxChar];
											charVertex[4] = boxMinV;
											break;
										}
										case(4):{//Top-left
											charVertex[3] = offsetsMinU[boxChar];
											charVertex[4] = boxMaxV;
											break;
										}
										case(5):{//Bottom-left
											charVertex[3] = offsetsMinU[boxChar];
											charVertex[4] = boxMinV;						
											break;
										}
									}
								}
							}
							
							if(j < 6){
								//Z is always 0 initially.  We rotate it to match the actual rotation.
								charVertex[2] = 0.0F;
								
								//Rotate vertices if required.
								if(doRotation){
									//FIXME can this go into OpenGL rotation now that our normalz aren't backwrads?
									rotatedVertex.set(charVertex[0], charVertex[1], charVertex[2]).rotateFine(rotation);
									charVertex[0] = (float) rotatedVertex.x;
									charVertex[1] = (float) rotatedVertex.y;
									charVertex[2] = (float) rotatedVertex.z;
								}
							}
							
							//Set normals.  These will already have been rotated.
							charVertex[5] = normals[0];
							charVertex[6] = normals[1];
							charVertex[7] = normals[2];
						}
						
						//Increment offset to next char position and set char points.
						currentOffset += charWidth + CHAR_SPACING;
					}
				}
				
				//All points obtained, render.  Lighting is done by the calling method and will be set there, color is set here.
				InterfaceRender.bindTexture(fontLocation);
				GL11.glPushMatrix();
				GL11.glTranslated(position.x, position.y, position.z);
				GL11.glScalef(scale, scale, scale);
				for(int i=0; i<totalTextChunks; ++i){
					InterfaceRender.setColorState(colorChangeValues[i]);
					InterfaceRender.renderVertices(fontVertices[i]);
				}
				GL11.glPopMatrix();
				InterfaceRender.recallTexture();
				InterfaceRender.setColorState(ColorRGB.WHITE);
			}
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
