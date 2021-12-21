package minecrafttransportsimulator.mcinterface;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.vecmath.Matrix4f;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponent3DModel;
import minecrafttransportsimulator.rendering.components.RenderableObject;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**Interface for MC GUI classes.  Allows access to various GUI-specific functions.
*
* @author don_bruce
*/
public class InterfaceGUI{
	
	/**
	 *  Draws the item's tooltip on the GUI.  This should be
	 *  the last thing that gets rendered, as otherwise it may render
	 *  behind other components.
	 */
	public static void drawItemTooltip(AGUIBase gui, int mouseX, int mouseY, ItemStack stack){
		Minecraft mc = Minecraft.getMinecraft();
		List<String> tooltipText = stack.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
        for(int i = 0; i < tooltipText.size(); ++i){
            if(i == 0){
                tooltipText.set(i, tooltipText.get(i));
            }else{
                tooltipText.set(i, TextFormatting.GRAY + tooltipText.get(i));
            }
        }
		GuiUtils.drawHoveringText(stack, tooltipText, mouseX, mouseY, mc.currentScreen.width, mc.currentScreen.height, -1, mc.fontRenderer);
	}
	
	/**
	 *  Draws a tooltip into the GUI.  This is for things that are NOT items, so
	 *  rather than passing-in item parameters you need to pass in the lines to render.
	 *  This should be rendered at the end of the render call to prevent the odd texture
	 *  binding of this method from conflicting from other renders.
	 */
	public static void drawGenericTooltip(AGUIBase gui, int mouseX, int mouseY, String tooltip){
		Minecraft mc = Minecraft.getMinecraft();
		GuiUtils.drawHoveringText(Arrays.asList(new String[]{tooltip}), mouseX, mouseY, mc.currentScreen.width, mc.currentScreen.height, -1, mc.fontRenderer);
	}
	
	/**
	 *  Draws the specified item on the GUI at the specified scale.  Note that MC
	 *  renders all items from their top-left corner, so take this into account when
	 *  choosing where to put this component in your GUI.
	 */
	public static void drawItem(ItemStack stack, int x, int y, float scale){
		RenderItem itemRenderer = Minecraft.getMinecraft().getRenderItem();
		if(scale != 1.0F){
			GL11.glPushMatrix();
			GL11.glTranslatef(x, y, 0);
			GL11.glScalef(scale, scale, scale);
			itemRenderer.renderItemAndEffectIntoGUI(stack, 0, 0);
			if(stack.getCount() > 1){
				itemRenderer.renderItemOverlays(Minecraft.getMinecraft().fontRenderer, stack, 0, 0);
			}
			GL11.glPopMatrix();
		}else{
			itemRenderer.renderItemAndEffectIntoGUI(stack, x, y);
			if(stack.getCount() > 1){
				itemRenderer.renderItemOverlays(Minecraft.getMinecraft().fontRenderer, stack, x, y);
			}
		}
	}
	
	/**
	 *  Returns a {@link RenderableObject} of the passed-in item model for item rendering.
	 *  Note that this does not include the count of the items in the stack: this must be
	 *  rendered on its own.
	 */
	public static RenderableObject getItemModel(ItemStack stack){
		//Get normal model.
		IBakedModel itemModel = Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(stack, null, Minecraft.getMinecraft().player);
		
		//Get transformation matrix, if this model has one.
		Matrix4f matrix = itemModel.handlePerspective(ItemCameraTransforms.TransformType.GUI).getRight();
		
		//Get all quads for the model. We assume that 
		List<BakedQuad> quads = new ArrayList<BakedQuad>();
		for(EnumFacing enumfacing : EnumFacing.values()){
			quads.addAll(itemModel.getQuads((IBlockState)null, enumfacing, 0L));
        }
		quads.addAll(itemModel.getQuads((IBlockState)null, null, 0L));
		
		//Convert quads to floatbuffer for our rendering.
		//Each 4-vertex quad becomes two tris, with standard rendering for normals and UVs.
		//Note that the offsets here are the byte index, so we need to convert them when addressing the int array.
		FloatBuffer vertexData = FloatBuffer.allocate(quads.size()*6*8);
		for(BakedQuad quad : quads){
			//Get a byte buffer of data to handle for conversion.
			int[] quadArray = quad.getVertexData();
			ByteBuffer quadData = ByteBuffer.allocate(quadArray.length*Integer.BYTES);
			quadData.asIntBuffer().put(quadArray);
			
			VertexFormat format = quad.getFormat();
			int quadDataIndexOffset = 0;
			for(int i=0; i<6; ++i){
				int offsetThisCycle = 0;
				if(i==3){
					//4th vertex is the same as 3rd vertex.
					quadDataIndexOffset -= format.getSize();
					offsetThisCycle = format.getSize();
				}else if(i==5){
					//6th vertex is the same as 1st vertex.
					quadDataIndexOffset -= 4*format.getSize();
				}else{
					//Actual vertex, add to buffer at current position.
					offsetThisCycle = format.getSize();
				}
				
				//Default normal to face direction.
				Vec3i vec3i = quad.getFace().getDirectionVec();
				vertexData.put(vec3i.getX());
				vertexData.put(vec3i.getY());
				vertexData.put(vec3i.getZ());
				
				//Use UV data.
				int uvOffset = format.getUvOffsetById(0);
				vertexData.put(quadData.getFloat(quadDataIndexOffset + uvOffset));
				vertexData.put(quadData.getFloat(quadDataIndexOffset + uvOffset + Float.BYTES));
				
				//For some reason, position isn't saved as an index.  Rather, it's in the general list.
				//Loop through the elements to find it.
				for(VertexFormatElement element : format.getElements()){
					if(element.isPositionElement()){
						int vertexOffset = format.getOffset(format.getElements().indexOf(element));
						float x = quadData.getFloat(quadDataIndexOffset + vertexOffset);
						float y = quadData.getFloat(quadDataIndexOffset + vertexOffset + Float.BYTES);
						float z = quadData.getFloat(quadDataIndexOffset + vertexOffset + 2*Float.BYTES);
						
						if(matrix != null){
							//TODO Need to also add offsets to align in GUIs, this gets removed when GUIs are overhauled.  
							float xNew = matrix.m00*x + matrix.m01*y + matrix.m02*z + matrix.m03 + 1;
							float yNew = matrix.m10*x + matrix.m11*y + matrix.m12*z + matrix.m13 + 0.25F;
							float zNew = matrix.m30*x + matrix.m31*y + matrix.m32*z + matrix.m33;
							//Don't multiply by w, we don't care about that value, and it really won't matter anyways.
							vertexData.put(xNew);
							vertexData.put(yNew);
							vertexData.put(zNew);
						}else{
							vertexData.put(x);
							vertexData.put(y);
							vertexData.put(z);
						}
						break;
					}
				}
				quadDataIndexOffset += offsetThisCycle;
			}
		}
		vertexData.flip();
		return new RenderableObject("item_generated", RenderableObject.GLOBAL_TEXTURE_NAME, ColorRGB.WHITE, vertexData, false);
	}
	
	/**
	 *  Draws the specified portion of the currently-bound texture.  Texture size needs to be
	 *  passed-in here to allow this method to translate pixels into relative texture coords.  
	 *  Draw starts at the  bottom-left point and goes counter-clockwise to the top-left point.
	 */
	public static void renderSheetTexture(int x, int y, int width, int height, float u, float v, float U, float V, int textureWidth, int textureHeight){
	 	float widthPixelPercent = 1.0F/textureWidth;
        float heightPixelPercent = 1.0F/textureHeight;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, 			y + height, 0.0D).tex(u * widthPixelPercent, 	V * heightPixelPercent).endVertex();
        bufferbuilder.pos(x + width, 	y + height, 0.0D).tex(U * widthPixelPercent, 	V * heightPixelPercent).endVertex();
        bufferbuilder.pos(x + width, 	y, 			0.0D).tex(U * widthPixelPercent, 	v * heightPixelPercent).endVertex();
        bufferbuilder.pos(x, 			y, 			0.0D).tex(u * widthPixelPercent, 	v * heightPixelPercent).endVertex();
        tessellator.draw();
	}
	
	/**
	 *  Returns the currently-active GUI, or null if no GUI is active.
	 */
	public static AGUIBase getActiveGUI(){
		return Minecraft.getMinecraft().currentScreen instanceof BuilderGUI ? ((BuilderGUI) Minecraft.getMinecraft().currentScreen).gui : null;
	}
	
	/**
	 *  Closes the currently-opened GUI, returning back to the main game.
	 */
	public static void closeGUI(){
		//Set current screen to null and clear out the OBJ DisplayLists if we have any.
		Minecraft.getMinecraft().displayGuiScreen(null);
		GUIComponent3DModel.clearModelCaches();
	}
	
	/**
	 *  Opens the passed-in GUI, replacing any opened GUI in the process.
	 */
	public static void openGUI(AGUIBase gui){
		FMLCommonHandler.instance().showGuiScreen(new BuilderGUI(gui));
	}
}
