package minecrafttransportsimulator.rendering;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftLarge;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftSmall;
import minecrafttransportsimulator.entities.parts.EntityPontoon;
import minecrafttransportsimulator.entities.parts.EntityPropeller;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntitySkid;
import minecrafttransportsimulator.entities.parts.EntityVehicleChest;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.rendering.partmodels.ModelEngineLarge;
import minecrafttransportsimulator.rendering.partmodels.ModelEngineSmall;
import minecrafttransportsimulator.rendering.partmodels.ModelPontoon;
import minecrafttransportsimulator.rendering.partmodels.ModelPropeller;
import minecrafttransportsimulator.rendering.partmodels.ModelSeat;
import minecrafttransportsimulator.rendering.partmodels.ModelSkid;
import minecrafttransportsimulator.rendering.partmodels.ModelVehicleChest;
import minecrafttransportsimulator.rendering.partmodels.ModelWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public final class RenderMultipartChild{
	private static final Map<Class<? extends EntityMultipartChild>, RenderChild> childRenderMap = new HashMap<Class<? extends EntityMultipartChild>, RenderChild>();
	
	public static void init(){
		childRenderMap.clear();
		childRenderMap.put(EntityEngineAircraftSmall.class, new RenderEngine());
		childRenderMap.put(EntityEngineAircraftLarge.class, childRenderMap.get(EntityEngineAircraftSmall.class));
		childRenderMap.put(EntityVehicleChest.class, new RenderVehicleChest());
		childRenderMap.put(EntityPontoon.class, new RenderPontoon());
		childRenderMap.put(EntityPropeller.class, new RenderPropeller());
		childRenderMap.put(EntitySeat.class, new RenderSeat());
		childRenderMap.put(EntitySkid.class, new RenderSkid());
		childRenderMap.put(EntityWheel.EntityWheelSmall.class, new RenderWheel());
		childRenderMap.put(EntityWheel.EntityWheelLarge.class, childRenderMap.get(EntityWheel.EntityWheelSmall.class));
	}
	
	public static void renderChildEntity(EntityMultipartChild child, float partialTicks){
		if(childRenderMap.containsKey(child.getClass())){
			childRenderMap.get(child.getClass()).doRender(child, Minecraft.getMinecraft().getTextureManager(), partialTicks);
		}
	}
	
    private static abstract class RenderChild{
    	public abstract void doRender(EntityMultipartChild child, TextureManager textureManger, float partialTicks);
    }
    
    private static final class RenderEngine extends RenderChild{
    	private static final ModelEngineSmall modelEngineSmall = new ModelEngineSmall();
    	private static final ModelEngineLarge modelEngineLarge = new ModelEngineLarge();
    	private static final ResourceLocation textureEngineSmall = new ResourceLocation(MTS.MODID, "textures/parts/enginesmall.png");
    	private static final ResourceLocation textureEngineLarge = new ResourceLocation(MTS.MODID, "textures/parts/enginelarge.png");
    	
    	@Override
    	public void doRender(EntityMultipartChild child, TextureManager textureManger, float partialTicks){		
    		GL11.glRotatef(180, 1, 0, 0);
            GL11.glTranslatef(0, -child.height/2, 0);
    		if(child instanceof EntityEngineAircraftSmall){
    			textureManger.bindTexture(textureEngineSmall);
    			modelEngineSmall.render();
    		}else if(child instanceof EntityEngineAircraftLarge){
    			GL11.glTranslatef(0, 0, -0.2F);
    			textureManger.bindTexture(textureEngineLarge);
    			modelEngineLarge.render();
    		}
    	}
    }
    
    private static final class RenderPontoon extends RenderChild{
    	private static final ModelPontoon modelPontoon = new ModelPontoon();
    	private static final ResourceLocation texturePontoon = new ResourceLocation("minecraft", "textures/blocks/iron_block.png");
    	
    	@Override
    	public void doRender(EntityMultipartChild child, TextureManager textureManger, float partialTicks){		
    		GL11.glRotatef(180, 1, 0, 0);
    		GL11.glTranslatef(0, -0.4F, -0.2F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            textureManger.bindTexture(texturePontoon);
            modelPontoon.render();
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
    
    private static final class RenderVehicleChest extends RenderChild{
    	private static final ModelVehicleChest modelChest = new ModelVehicleChest();
    	private static final ResourceLocation textureChest = new ResourceLocation("minecraft", "textures/entity/chest/normal.png");
    	
    	@Override
    	public void doRender(EntityMultipartChild child, TextureManager textureManger, float partialTicks){
    		GL11.glRotatef(180, 1, 0, 0);
    		textureManger.bindTexture(textureChest);
    		modelChest.renderAll(-((EntityVehicleChest) child).lidAngle);
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
    
    private static final class RenderSkid extends RenderChild{
    	private static final ModelSkid modelSkid = new ModelSkid();
    	private static final ResourceLocation textureSkid = new ResourceLocation(MTS.MODID, "textures/parts/skid.png");
    	
    	@Override
    	public void doRender(EntityMultipartChild child, TextureManager textureManger, float partialTicks){
    		GL11.glRotatef(180, 1, 0, 0);
    		GL11.glTranslatef(0, -0.25F, 0);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            textureManger.bindTexture(textureSkid);
            modelSkid.render();
    	}
    }
    
    private static final class RenderWheel extends RenderChild{
    	private static final ModelWheel modelWheel = new ModelWheel();
    	private static final ResourceLocation textureWheelInner = new ResourceLocation("minecraft", "textures/blocks/wool_colored_white.png");
    	private static final ResourceLocation textureWheelOuter = new ResourceLocation("minecraft", "textures/blocks/wool_colored_black.png");
    	
    	@Override
    	public void doRender(EntityMultipartChild child, TextureManager textureManger, float partialTicks){
    		EntityWheel wheel = (EntityWheel) child;
    		if(wheel.isFlat){
    			GL11.glTranslated(0, -wheel.height/2F, 0);
    		}
    		if(wheel instanceof EntityWheel.EntityWheelSmall){
    			textureManger.bindTexture(textureWheelInner);
    			modelWheel.renderSmallInnerWheel(wheel.angularPosition + wheel.angularVelocity*partialTicks);
    			if(!wheel.isFlat){
    				textureManger.bindTexture(textureWheelOuter);
    				modelWheel.renderSmallOuterWheel(wheel.angularPosition + wheel.angularVelocity*partialTicks);
    			}
    		}else{
    			textureManger.bindTexture(textureWheelInner);
    			modelWheel.renderLargeInnerWheel(wheel.angularPosition + wheel.angularVelocity*partialTicks);
    			if(!wheel.isFlat){
    				textureManger.bindTexture(textureWheelOuter);
    				modelWheel.renderLargeOuterWheel(wheel.angularPosition + wheel.angularVelocity*partialTicks);
    			}
    		}
    	}
    }
}
