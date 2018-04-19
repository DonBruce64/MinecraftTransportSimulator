package minecrafttransportsimulator.rendering;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.parts.EntityCrate;
import minecrafttransportsimulator.entities.parts.EntityEngineAMCI4;
import minecrafttransportsimulator.entities.parts.EntityEngineBristolMercury;
import minecrafttransportsimulator.entities.parts.EntityEngineDetroitDiesel;
import minecrafttransportsimulator.entities.parts.EntityEngineLycomingO360;
import minecrafttransportsimulator.entities.parts.EntityPontoon;
import minecrafttransportsimulator.entities.parts.EntityPropeller;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntitySkid;
import minecrafttransportsimulator.entities.parts.EntityWheelLarge;
import minecrafttransportsimulator.entities.parts.EntityWheelLarge.EntityWheelLargeFlat;
import minecrafttransportsimulator.entities.parts.EntityWheelMedium;
import minecrafttransportsimulator.entities.parts.EntityWheelMedium.EntityWheelMediumFlat;
import minecrafttransportsimulator.entities.parts.EntityWheelSmall;
import minecrafttransportsimulator.entities.parts.EntityWheelSmall.EntityWheelSmallFlat;
import minecrafttransportsimulator.rendering.partmodels.ModelPropeller;
import minecrafttransportsimulator.rendering.partmodels.ModelSeat;
import minecrafttransportsimulator.systems.OBJParserSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

public final class RenderMultipartChild{
	private static final Map<Class<? extends EntityMultipartChild>, RenderChild> renderClassMap = new HashMap<Class<? extends EntityMultipartChild>, RenderChild>();
	private static final Map<Class<? extends EntityMultipartChild>, Integer> displayListMap = new HashMap<Class<? extends EntityMultipartChild>, Integer>();
	private static final Map<Class<? extends EntityMultipartChild>, ResourceLocation> textureMap = new HashMap<Class<? extends EntityMultipartChild>, ResourceLocation>();
	
	public static void init(){
		RenderOBJGeneric objRender = new RenderOBJGeneric();
		renderClassMap.clear();
		renderClassMap.put(EntityEngineLycomingO360.class, objRender);
		renderClassMap.put(EntityEngineBristolMercury.class, objRender);
		renderClassMap.put(EntityEngineAMCI4.class, objRender);
		renderClassMap.put(EntityEngineDetroitDiesel.class, objRender);
		renderClassMap.put(EntityCrate.class, objRender);
		renderClassMap.put(EntityPontoon.class, objRender);
		renderClassMap.put(EntityPropeller.class, new RenderPropeller());
		renderClassMap.put(EntitySeat.class, new RenderSeat());
		renderClassMap.put(EntitySkid.class, objRender);
		renderClassMap.put(EntityWheelSmall.class, objRender);
		renderClassMap.put(EntityWheelSmallFlat.class, objRender);
		renderClassMap.put(EntityWheelMedium.class, objRender);
		renderClassMap.put(EntityWheelMediumFlat.class, objRender);
		renderClassMap.put(EntityWheelLarge.class, objRender);
		renderClassMap.put(EntityWheelLargeFlat.class, objRender);
	}
	
	public static void renderChildEntity(EntityMultipartChild child, float partialTicks){
		if(renderClassMap.containsKey(child.getClass())){
			renderClassMap.get(child.getClass()).doRender(child, Minecraft.getMinecraft().getTextureManager(), partialTicks);
		}
	}
	
    private static abstract class RenderChild{
    	public abstract void doRender(EntityMultipartChild child, TextureManager textureManger, float partialTicks);
    }
    
    private static final class RenderOBJGeneric extends RenderChild{
    	@Override
    	public void doRender(EntityMultipartChild child, TextureManager textureManger, float partialTicks){
    		if(!displayListMap.containsKey(child.getClass())){
    			int displayListIndex = GL11.glGenLists(1);
    			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
    			GL11.glBegin(GL11.GL_TRIANGLES);
    			String fileName = child.getClass().getSimpleName().substring("Entity".length()).toLowerCase();
    			for(Entry<String, Float[][]> entry : OBJParserSystem.parseOBJModel(new ResourceLocation(MTS.MODID, "objmodels/" + fileName + ".obj")).entrySet()){
    				for(Float[] vertex : entry.getValue()){
    					GL11.glTexCoord2f(vertex[3], vertex[4]);
    					GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
    					GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
    				}
    			}
    			GL11.glEnd();
    			GL11.glEndList();
    			displayListMap.put(child.getClass(), displayListIndex);
    			textureMap.put(child.getClass(), new ResourceLocation(MTS.MODID, "textures/parts/" + child.getTextureName() + ".png"));
    		}
    		
    		GL11.glPushMatrix();
    		GL11.glRotatef(child.getXRotation(partialTicks), 1, 0, 0);
    		GL11.glRotatef(child.getYRotation(partialTicks), 0, 1, 0);
    		GL11.glRotatef(child.getZRotation(partialTicks), 0, 0, 1);
            textureManger.bindTexture(textureMap.get(child.getClass()));
			GL11.glCallList(displayListMap.get(child.getClass()));
			GL11.glPopMatrix();
    	}
    }
    
    private static final class RenderPropeller extends RenderChild{
    	private static final ModelPropeller modelPropeller = new ModelPropeller();
    	private static final ResourceLocation texturePropellerOne = new ResourceLocation("minecraft", "textures/blocks/planks_oak.png");
    	private static final ResourceLocation texturePropellerTwo = new ResourceLocation("minecraft", "textures/blocks/iron_block.png");
    	private static final ResourceLocation texturePropellerThree = new ResourceLocation("minecraft", "textures/blocks/obsidian.png");
    	
    	@Override
    	public void doRender(EntityMultipartChild child, TextureManager textureManger, float partialTicks){
    		GL11.glRotatef(180, 0, 1, 0);
    		if(child.propertyCode==1){
    			textureManger.bindTexture(texturePropellerTwo);
    		}else if(child.propertyCode==2){
    			textureManger.bindTexture(texturePropellerThree);
    		}else{
    			textureManger.bindTexture(texturePropellerOne);
    		}
    		modelPropeller.renderPropeller(((EntityPropeller) child).numberBlades, ((EntityPropeller) child).diameter, -((EntityPropeller) child).angularPosition - ((EntityPropeller) child).angularVelocity*partialTicks);
    	}
    }
    
    private static final class RenderSeat extends RenderChild{
    	private static final ModelSeat modelSeat = new ModelSeat();
    	private static final ResourceLocation textureSeatLeather = new ResourceLocation(MTS.MODID, "textures/parts/leather.png");
    	private static final ResourceLocation[] woodTextures = getWoodTextures();
    	private static final ResourceLocation[] woolTextures = getWoolTextures();
    	
    	@Override
    	public void doRender(EntityMultipartChild child, TextureManager textureManger, float partialTicks){	
    		textureManger.bindTexture(woodTextures[child.propertyCode%6]);
    		modelSeat.renderFrame();
    		if(child.propertyCode < 96){
    			textureManger.bindTexture(woolTextures[child.propertyCode/6]);
    		}else{
    			textureManger.bindTexture(textureSeatLeather);
    		}
    		modelSeat.renderCushion();
    	}
    	
    	private static ResourceLocation[] getWoodTextures(){
    		ResourceLocation[] texArray = new ResourceLocation[6];
    		int texIndex = 0;
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_oak.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_spruce.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_birch.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_jungle.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_acacia.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_big_oak.png");
    		return texArray;
    	}
    	
    	private static ResourceLocation[] getWoolTextures(){
    		ResourceLocation[] texArray = new ResourceLocation[16];
    		int texIndex = 0;
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_white.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_orange.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_magenta.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_light_blue.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_yellow.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_lime.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_pink.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_gray.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_silver.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_cyan.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_purple.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_blue.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_brown.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_green.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_red.png");
    		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_black.png");
    		return texArray;
    	}
    }
}
