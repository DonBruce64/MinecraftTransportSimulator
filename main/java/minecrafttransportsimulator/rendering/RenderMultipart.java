package minecrafttransportsimulator.rendering;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSAxisAlignedBB;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Controls;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Instruments;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackControl;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackDisplayText;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackFileDefinitions;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackInstrument;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackPart;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle.LightTypes;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.entities.parts.EntityEngineCar;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.systems.ClientEventSystem;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderWorldLastEvent;

/**Main render class for all multipart entities.
 * Renders the parent model, and all child models that have been registered by
 * {@link registerChildRender}.  Ensures all parts are rendered in the exact
 * location they should be in as all rendering is done in the same operation.
 * Entities don't render above 255 well due to the new chunk visibility system.
 * This code is present to be called manually from
 * {@link ClientEventSystem#on(RenderWorldLastEvent)}.
 *
 * @author don_bruce
 */
public final class RenderMultipart extends Render<EntityMultipartMoving>{
	private static final Minecraft minecraft = Minecraft.getMinecraft();
	/**Display list GL integers.  Keyed by model name.*/
	private static final Map<String, Integer> displayLists = new HashMap<String, Integer>();
	/**Rotatable parts for models.  Keyed by model name.*/
	private static final Map<String, List<RotatablePart>> rotatableLists = new HashMap<String, List<RotatablePart>>();
	/**Window parts for models.  Keyed by model name.*/
	private static final Map<String, List<WindowPart>> windowLists = new HashMap<String, List<WindowPart>>();
	/**Lights for models.  Keyed by model name.*/
	private static final Map<String, List<LightPart>> lightLists = new HashMap<String, List<LightPart>>();
	/**Model texture name.  Keyed by model name.*/
	private static final Map<String, ResourceLocation> textureMap = new HashMap<String, ResourceLocation>();
	private static final Map<EntityMultipartMoving, Byte> lastRenderPass = new HashMap<EntityMultipartMoving, Byte>();
	private static final Map<EntityMultipartMoving, Long> lastRenderTick = new HashMap<EntityMultipartMoving, Long>();
	private static final Map<EntityMultipartMoving, Float> lastRenderPartial = new HashMap<EntityMultipartMoving, Float>();
	private static final ResourceLocation vanillaGlassTexture = new ResourceLocation("minecraft", "textures/blocks/glass.png");
	private static final ResourceLocation lensFlareTexture = new ResourceLocation(MTS.MODID, "textures/rendering/lensflare.png");
	private static final ResourceLocation lightTexture = new ResourceLocation(MTS.MODID, "textures/rendering/light.png");
	
	public RenderMultipart(RenderManager renderManager){
		super(renderManager);
		RenderMultipartChild.init();
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityMultipartMoving entity){
		return null;
	}
	
	@Override
	public void doRender(EntityMultipartMoving entity, double x, double y, double z, float entityYaw, float partialTicks){
		boolean didRender = false;
		if(entity.pack != null){ 
			if(lastRenderPass.containsKey(entity)){
				//Did we render this tick?
				if(lastRenderTick.get(entity) == entity.worldObj.getTotalWorldTime() && lastRenderPartial.get(entity) == partialTicks){
					//If we rendered last on a pass of 0 or 1 this tick, don't re-render some things.
					if(lastRenderPass.get(entity) != -1 && MinecraftForgeClient.getRenderPass() == -1){
						render(entity, Minecraft.getMinecraft().thePlayer, partialTicks, true);
						didRender = true;
					}
				}
			}
			if(!didRender){
				render(entity, Minecraft.getMinecraft().thePlayer, partialTicks, false);
			}
			lastRenderPass.put(entity, (byte) MinecraftForgeClient.getRenderPass());
			lastRenderTick.put(entity, entity.worldObj.getTotalWorldTime());
			lastRenderPartial.put(entity, partialTicks);
		}
	}
	
	public static void resetRenders(){
		for(Integer displayList : displayLists.values()){
			GL11.glDeleteLists(displayList, 1);
		}
		displayLists.clear();
		rotatableLists.clear();
		windowLists.clear();
		lightLists.clear();
	}
	
	public static boolean doesMultipartHaveLight(EntityMultipartMoving mover, LightTypes light){
		return lightLists.get(mover.pack.rendering.modelName).contains(light.name().toLowerCase());
	}
	
	private static void render(EntityMultipartMoving mover, EntityPlayer playerRendering, float partialTicks, boolean wasRenderedPrior){
		//Calculate various things.
		Entity renderViewEntity = minecraft.getRenderViewEntity();
		double playerX = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * (double)partialTicks;
		double playerY = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * (double)partialTicks;
		double playerZ = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * (double)partialTicks;
        
        
        double thisX = mover.lastTickPosX + (mover.posX - mover.lastTickPosX) * (double)partialTicks;
        double thisY = mover.lastTickPosY + (mover.posY - mover.lastTickPosY) * (double)partialTicks;
        double thisZ = mover.lastTickPosZ + (mover.posZ - mover.lastTickPosZ) * (double)partialTicks;
        double rotateYaw = -mover.rotationYaw + (mover.rotationYaw - mover.prevRotationYaw)*(double)(1 - partialTicks);
        double rotatePitch = mover.rotationPitch - (mover.rotationPitch - mover.prevRotationPitch)*(double)(1 - partialTicks);
        double rotateRoll = mover.rotationRoll - (mover.rotationRoll - mover.prevRotationRoll)*(double)(1 - partialTicks);

        //Set up position and lighting.
        GL11.glPushMatrix();
        GL11.glTranslated(thisX - playerX, thisY - playerY, thisZ - playerZ);
        int lightVar = mover.getBrightnessForRender(partialTicks);
        minecraft.entityRenderer.enableLightmap();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightVar%65536, lightVar/65536);
        RenderHelper.enableStandardItemLighting();
        
		//Bind texture.  Adds new element to cache if needed.
		PackFileDefinitions definition = PackParserSystem.getDefinitionForPack(mover.name);
		if(!textureMap.containsKey(definition.modelTexture)){
			if(definition.modelTexture.contains(":")){
				textureMap.put(mover.name, new ResourceLocation(definition.modelTexture));
			}else{
				textureMap.put(mover.name, new ResourceLocation(MTS.MODID, "textures/models/" + definition.modelTexture));
			}
		}
		minecraft.getTextureManager().bindTexture(textureMap.get(mover.name));
		//Render all the model parts except windows.
		//Those need to be rendered after the player if the player is rendered manually.
		if(MinecraftForgeClient.getRenderPass() != 1 && !wasRenderedPrior){
			GL11.glPushMatrix();
			GL11.glRotated(rotateYaw, 0, 1, 0);
	        GL11.glRotated(rotatePitch, 1, 0, 0);
	        GL11.glRotated(rotateRoll, 0, 0, 1);
			renderMainModel(mover);
			renderChildren(mover, partialTicks);
			GL11.glEnable(GL11.GL_NORMALIZE);
			renderWindows(mover);
			renderTextMarkings(mover);
			if(mover instanceof EntityMultipartVehicle){
				renderInstrumentsAndControls((EntityMultipartVehicle) mover);
			}
			GL11.glDisable(GL11.GL_NORMALIZE);
			if(Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox()){
				renderBoundingBoxes(mover);
			}
			GL11.glPopMatrix();
		}
		
		//Check to see if we need to manually render riders.
		//MC culls rendering above build height depending on the direction the player is looking.
 		//Due to inconsistent culling based on view angle, this can lead to double-renders.
 		//Better than not rendering at all I suppose.
		if(MinecraftForgeClient.getRenderPass() != 1 && !wasRenderedPrior){
			 for(EntityMultipartChild child : mover.getChildren()){
				 if(child instanceof EntitySeat){
			         Entity rider = ((EntitySeat) child).getPassenger();
			         if(rider != null && !(minecraft.thePlayer.equals(rider) && minecraft.gameSettings.thirdPersonView == 0) && rider.posY > rider.worldObj.getHeight()){
			        	 GL11.glPushMatrix();
			        	 GL11.glTranslated(rider.posX - mover.posX, rider.posY - mover.posY, rider.posZ - mover.posZ);
			        	 Minecraft.getMinecraft().getRenderManager().renderEntityStatic(rider, partialTicks, false);
			        	 GL11.glPopMatrix();
			         }
				 }
		     }
		}
		
		//Lights and beacons get rendered in two passes.
		//The first renders the cases and bulbs, the second renders the beams and effects.
		if(mover instanceof EntityMultipartVehicle){
			EntityMultipartVehicle vehicle = (EntityMultipartVehicle) mover;
			float sunLight = vehicle.worldObj.getSunBrightness(0)*vehicle.worldObj.getLightBrightness(vehicle.getPosition());
			float blockLight = vehicle.worldObj.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, vehicle.getPosition())/15F;
			float electricFactor = (float) Math.min(vehicle.electricPower > 2 ? (vehicle.electricPower-2)/6F : 0, 1);
			float lightBrightness = (float) Math.min((1 - Math.max(sunLight, blockLight))*electricFactor, 1);

			GL11.glPushMatrix();
			GL11.glEnable(GL11.GL_NORMALIZE);
			GL11.glRotated(rotateYaw, 0, 1, 0);
	        GL11.glRotated(rotatePitch, 1, 0, 0);
	        GL11.glRotated(rotateRoll, 0, 0, 1);

	        renderLights(vehicle, sunLight, blockLight, lightBrightness, electricFactor, wasRenderedPrior);
			/*if(!wasRenderedPrior){
				renderBeacons(vehicle, sunLight, blockLight, lightBrightness, electricFactor);
			}*/
			GL11.glDisable(GL11.GL_NORMALIZE);
			GL11.glPopMatrix();
			
			//Return all states to normal.
			minecraft.entityRenderer.enableLightmap();
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glDepthMask(true);
			GL11.glColor4f(1, 1, 1, 1);
		}
		
		//Render holograms for missing parts if applicable.
		if(MinecraftForgeClient.getRenderPass() != 0){
			renderPartBoxes(mover);
		}
		
		//Make sure lightmaps are set correctly.
		if(MinecraftForgeClient.getRenderPass() == -1){
			RenderHelper.disableStandardItemLighting();
			minecraft.entityRenderer.disableLightmap();
		}else{
			RenderHelper.enableStandardItemLighting();
			minecraft.entityRenderer.enableLightmap();
		}
		 GL11.glPopMatrix();
	}
	
	private static Vec3d getRotationPointForRotatable(Entry<String, Float[][]> entry){
		double minX = 999;
		double maxX = -999;
		double minY = 999;
		double maxY = -999;
		double minZ = 999;
		double maxZ = -999;
		for(Float[] vertex : entry.getValue()){
			minX = Math.min(vertex[0], minX);
			maxX = Math.max(vertex[0], maxX);
			minY = Math.min(vertex[1], minY);
			maxY = Math.max(vertex[1], maxY);
			minZ = Math.min(vertex[2], minZ);
			maxZ = Math.max(vertex[2], maxZ);
		}
		
		//First check parts that have a Left-Right specific rotation.
		//These have suffixes of _L and _R, so only take away stuff after the second underscore.
		String partName = entry.getKey(); 
		while(partName.indexOf('_') != partName.lastIndexOf('_')){
			partName = partName.substring(0, partName.lastIndexOf('_'));
		}
		switch(partName){
			case("$Door_L"): return new Vec3d(maxX, minY, maxZ);
			case("$Door_R"): return new Vec3d(minX, minY, maxZ);
			
			case("$Aileron_L"): return new Vec3d(maxX, minY + (maxY - minY)/2D, maxZ);
			case("$Aileron_R"): return new Vec3d(minX, minY + (maxY - minY)/2D, maxZ);
			case("$Elevator_L"): return new Vec3d(maxX, minY + (maxY - minY)/2D, maxZ);
			case("$Elevator_R"): return new Vec3d(minX, minY + (maxY - minY)/2D, maxZ);
			case("$Flap_L"): return new Vec3d(maxX, minY + (maxY - minY)/2D, maxZ);
			case("$Flap_R"): return new Vec3d(minX, minY + (maxY - minY)/2D, maxZ);
		}
		
		//If we didn't find the part, it must be a singular type.
		//Remove the underscore and anything after it.
		if(partName.indexOf('_') != -1){
			partName = partName.substring(0, partName.indexOf('_'));
		}
		switch(partName){
			case("$Gas"): return new Vec3d(minX + (maxX - minX)/2D, minY, minZ);
			case("$Brake"): return new Vec3d(minX + (maxX - minX)/2D, minY, minZ);
			case("$Parking"): return new Vec3d(minX + (maxX - minX)/2D, minY, minZ);
			case("$Gearshift"): return new Vec3d(minX + (maxX - minX)/2D, minY, minZ + (maxZ - minZ)/2D);
			case("$SteeringWheel"): return new Vec3d(minX + (maxX - minX)/2D, minY + (maxY - minY)/2D, maxZ);
			
			case("$Rudder"): return new Vec3d(0, minY + (maxY - minY)/2D, maxZ);
			case("$WheelStrut"): return new Vec3d(minX + (maxX - minX)/2D, maxY, minZ + (maxZ - minZ)/2D);
			case("$Throttle"): return new Vec3d(minX + (maxX - minX)/2D, minY + (maxY - minY)/2D, maxZ);
			case("$Yoke"): return new Vec3d(minX + (maxX - minX)/2D, minY + (maxY - minY)/2D, maxZ);
			case("$Stick"): return new Vec3d(minX + (maxX - minX)/2D, minY, minZ + (maxZ - minZ)/2D);
		}
		
		//Default to this if we don't find the rotation.
		return new Vec3d(0, 0, 0);
	}
	
	private static void renderMainModel(EntityMultipartMoving mover){
		GL11.glPushMatrix();
		//Normally we use the pack name, but since all displaylists
		//are the same for all models, this is more appropriate.
		if(displayLists.containsKey(mover.pack.rendering.modelName)){
			GL11.glCallList(displayLists.get(mover.pack.rendering.modelName));
			
			//The display list only renders static parts.  We need to render dynamic ones manually.
			//If this is a window, don't render it as that gets done all at once later.
			for(RotatablePart rotatable : rotatableLists.get(mover.pack.rendering.modelName)){
				if(!windowLists.get(mover.pack.rendering.modelName).contains(rotatable.name)){
					GL11.glPushMatrix();
					rotateObject(mover, rotatable);
					GL11.glBegin(GL11.GL_TRIANGLES);
					for(Float[] vertex : rotatable.vertices){
						GL11.glTexCoord2f(vertex[3], vertex[4]);
						GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
						GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
					}
					GL11.glEnd();
					GL11.glPopMatrix();
				}
			}
		}else{
			Map<String, Float[][]> parsedModel = OBJParserSystem.parseOBJModel(new ResourceLocation(MTS.MODID, "objmodels/" + PackParserSystem.getPack(mover.name).rendering.modelName));
			int displayListIndex = GL11.glGenLists(1);
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			List<RotatablePart> rotatableParts = new ArrayList<RotatablePart>();
			List<LightPart> lightParts = new ArrayList<LightPart>();
			for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
				//Don't add movable model parts or windows to the display list.
				//Those go in separate maps, with windows getting parsed after all parts in case they're movable.
				//Do add lights, as they will be rendered both as part of the model and with special things.
				if(!entry.getKey().toLowerCase().contains("window")){
					if(entry.getKey().contains("$")){
						rotatableParts.add(new RotatablePart(entry.getKey(), getRotationPointForRotatable(entry), entry.getValue()));
					}else{
						for(Float[] vertex : entry.getValue()){
							GL11.glTexCoord2f(vertex[3], vertex[4]);
							GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
							GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
						}
						if(entry.getKey().contains("&")){
							lightParts.add(new LightPart(entry.getKey(), entry.getValue()));
						}
					}
				}
			}
			
			//Windows need to come after movables so they can take the movable's rotation points if they're movable too.
			List<WindowPart> windows = new ArrayList<WindowPart>();
			for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
				if(entry.getKey().toLowerCase().contains("window")){
					if(entry.getKey().toLowerCase().contains("$")){
						for(RotatablePart rotatable : rotatableParts){
							if(entry.getKey().contains(rotatable.name)){
								rotatableParts.add(new RotatablePart(entry.getKey(), rotatable.rotationPoint, entry.getValue()));
								break;
							}
						}
					}
					windows.add(new WindowPart(entry.getKey(), entry.getValue()));
				}
			}
			
			//Now finalize the maps.
			rotatableLists.put(mover.pack.rendering.modelName, rotatableParts);
			windowLists.put(mover.pack.rendering.modelName, windows);
			lightLists.put(mover.pack.rendering.modelName, lightParts);
			GL11.glEnd();
			GL11.glEndList();
			displayLists.put(mover.pack.rendering.modelName, displayListIndex);
		}
		GL11.glPopMatrix();
	}
	
	private static void rotateObject(EntityMultipartMoving mover, RotatablePart rotatable){
		//First translate to the center of the model for proper rotation.
		GL11.glTranslated(rotatable.rotationPoint.xCoord, rotatable.rotationPoint.yCoord, rotatable.rotationPoint.zCoord);
		
		//Next rotate the part.
		if(rotatable.name.contains("Door_L")){
			if(mover.parkingBrakeOn && mover.velocity == 0 && !mover.locked){
				GL11.glRotatef(-60F, 0, 1, 0);
			}
		}else if(rotatable.name.contains("Door_R")){
			if(mover.parkingBrakeOn && mover.velocity == 0 && !mover.locked){
				GL11.glRotatef(60F, 0, 1, 0);
			}
		}else if(rotatable.name.contains("Gas") || rotatable.name.contains("Throttle")){
			GL11.glRotatef(((EntityMultipartVehicle) mover).throttle/4F, 1, 0, 0);
		}else if(rotatable.name.contains("Brake")){
			if(((EntityMultipartVehicle) mover).brakeOn){
				GL11.glRotatef(30, 1, 0, 0);
			}
		}else if(rotatable.name.contains("Parking")){
			if(((EntityMultipartVehicle) mover).parkingBrakeOn){
				GL11.glRotatef(30, -1, 0, 0);
			}
		}else if(rotatable.name.contains("Gearshift")){
			if(((EntityMultipartVehicle) mover).getEngineByNumber((byte) 1) != null){
				if(((EntityEngineCar) ((EntityMultipartVehicle) mover).getEngineByNumber((byte) 1)).isAutomatic){
					GL11.glRotatef(((EntityEngineCar) ((EntityMultipartVehicle) mover).getEngineByNumber((byte) 1)).getCurrentGear()*15, -1, 0, 0);
				}else{
					GL11.glRotatef(((EntityEngineCar) ((EntityMultipartVehicle) mover).getEngineByNumber((byte) 1)).getCurrentGear()*3, -1, 0, 0);
				}
			}
		}else if(rotatable.name.contains("SteeringWheel")){
			GL11.glRotatef(mover.getSteerAngle(), 0, 0, 1);
		}else if(rotatable.name.contains("Aileron_L")){
			GL11.glRotatef(((EntityPlane) mover).aileronAngle/10F, -1, 0, 0);
		}else if(rotatable.name.contains("Aileron_R")){
			GL11.glRotatef(((EntityPlane) mover).aileronAngle/10F, 1, 0, 0);
		}else if(rotatable.name.contains("Elevator")){
			GL11.glRotatef(((EntityPlane) mover).elevatorAngle/10F, 1, 0, 0);
		}else if(rotatable.name.contains("Rudder")){
			GL11.glRotatef(((EntityPlane) mover).rudderAngle/10F, 0, 1, 0);
		}else if(rotatable.name.contains("Flap")){
			GL11.glRotatef(((EntityPlane) mover).flapAngle/10F, -1, 0, 0);
		}else if(rotatable.name.contains("WheelStrut")){
			GL11.glRotatef(((EntityPlane) mover).rudderAngle/10F, 0, -1, 0);
		}else if(rotatable.name.contains("Stick")){
			GL11.glRotatef(((EntityPlane) mover).aileronAngle/10F, 0, 0, 1);
			GL11.glRotatef(((EntityPlane) mover).elevatorAngle/10F, 1, 0, 0);
		}else if(rotatable.name.contains("Yoke")){
			GL11.glRotatef(((EntityPlane) mover).aileronAngle/10F, 0, 0, 1);
			GL11.glTranslatef(0, 0, -((EntityPlane) mover).elevatorAngle/10F/100F);
		}
		
		//Now translate the part back to it's original position.
		GL11.glTranslated(-rotatable.rotationPoint.xCoord, -rotatable.rotationPoint.yCoord, -rotatable.rotationPoint.zCoord);
	}
	
	private static void renderChildren(EntityMultipartMoving mover, float partialTicks){
		for(EntityMultipartChild child : mover.getChildren()){
			GL11.glPushMatrix();
    		GL11.glTranslatef(child.offsetX, child.offsetY, child.offsetZ);
    		if(child.turnsWithSteer){
    			if(child.offsetZ >= 0){
    				GL11.glRotatef(mover.getSteerAngle(), 0, 1, 0);
    			}else{
    				GL11.glRotatef(-mover.getSteerAngle(), 0, 1, 0);
    			}
    		}
    		RenderMultipartChild.renderChildEntity(child, partialTicks);
			GL11.glPopMatrix();
        }
	}
	
	private static void renderWindows(EntityMultipartMoving mover){
		minecraft.getTextureManager().bindTexture(vanillaGlassTexture);
		//Iterate through all windows.
		for(byte i=0; i<windowLists.get(mover.pack.rendering.modelName).size(); ++i){
			if(i >= mover.brokenWindows){
				GL11.glPushMatrix();
				//This is a window or set of windows.  Like the model, it will be triangle-based.
				//However, windows may be rotatable.  Check this before continuing.
				WindowPart window = windowLists.get(mover.pack.rendering.modelName).get(i);
				for(RotatablePart rotatable : rotatableLists.get(mover.pack.rendering.modelName)){
					if(rotatable.name.equals(window.name)){
						rotateObject(mover, rotatable);
					}
				}
				//If this window is a quad, draw quads.  Otherwise draw tris.
				if(window.vertices.length == 4){
					GL11.glBegin(GL11.GL_QUADS);
				}else{
					GL11.glBegin(GL11.GL_TRIANGLES);
				}
				for(Float[] vertex : window.vertices){
					GL11.glTexCoord2f(vertex[3], vertex[4]);
					GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
					GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
				}
				if(ConfigSystem.getBooleanConfig("InnerWindows")){
					for(int j=window.vertices.length-1; j >= 0; --j){
						Float[] vertex = window.vertices[j];
						GL11.glTexCoord2f(vertex[3], vertex[4]);
						GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
						GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);	
					}
				}
				GL11.glEnd();
				GL11.glPopMatrix();
			}
		}
	}
	
	private static void renderTextMarkings(EntityMultipartMoving mover){
		for(PackDisplayText text : mover.pack.rendering.textMarkings){
			GL11.glPushMatrix();
			GL11.glTranslatef(text.pos[0], text.pos[1], text.pos[2]);
			GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
			GL11.glRotatef(text.rot[0], 1, 0, 0);
			GL11.glRotatef(text.rot[1] + 180, 0, 1, 0);
			GL11.glRotatef(text.rot[2] + 180, 0, 0, 1);
			GL11.glScalef(text.scale, text.scale, text.scale);
			RenderHelper.disableStandardItemLighting();
			minecraft.fontRendererObj.drawString(mover.displayText, -minecraft.fontRendererObj.getStringWidth(mover.displayText)/2, 0, Color.decode(text.color).getRGB());
			GL11.glPopMatrix();
		}
		GL11.glColor3f(1.0F, 1.0F, 1.0F);
		RenderHelper.enableStandardItemLighting();
	}
	
	private static void renderLights(EntityMultipartVehicle vehicle, float sunLight, float blockLight, float lightBrightness, float electricFactor, boolean wasRenderedPrior){
		for(LightPart light : lightLists.get(vehicle.pack.rendering.modelName)){
			boolean lightSwitchOn = vehicle.isLightOn(light.type);
			//Fun with bit shifting!  20 bits make up the light on index here, so align to a 20 tick cycle.
			boolean lightActuallyOn = lightSwitchOn && ((light.flashBits >> vehicle.ticksExisted%20) & 1) > 0;
			//Used to make the cases of the lights full brightness.  Used when lights are brighter than the surroundings.
			boolean overrideCaseBrightness = lightBrightness > Math.max(sunLight, blockLight) && lightActuallyOn;

			if(MinecraftForgeClient.getRenderPass() != 1 && !wasRenderedPrior){
				GL11.glPushMatrix();
				if(overrideCaseBrightness){
					GL11.glDisable(GL11.GL_LIGHTING);
					minecraft.entityRenderer.disableLightmap();
				}else{
					GL11.glEnable(GL11.GL_LIGHTING);
					minecraft.entityRenderer.enableLightmap();
				}
				GL11.glEnable(GL11.GL_TEXTURE_2D);
				GL11.glDisable(GL11.GL_BLEND);
				
				//Cover rendering.
				minecraft.getTextureManager().bindTexture(vanillaGlassTexture);
				GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
				GL11.glBegin(GL11.GL_TRIANGLES);
				for(Float[] vertex : light.vertices){
					//Add a slight translation and scaling to the light coords based on the normals to make the lens cover.
					//Also modify the cover size to ensure the whole cover is a single glass square.
					GL11.glTexCoord2f(vertex[3], vertex[4]);
					GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
					GL11.glVertex3f(vertex[0]+vertex[5]*0.0003F, vertex[1]+vertex[6]*0.0003F, vertex[2]+vertex[7]*0.0003F);	
				}
				GL11.glEnd();
				
				//Light rendering.
				if(lightActuallyOn){
					GL11.glDisable(GL11.GL_LIGHTING);
					GL11.glEnable(GL11.GL_BLEND);
					minecraft.getTextureManager().bindTexture(lightTexture);
					GL11.glColor4f(light.color.getRed(), light.color.getGreen(), light.color.getBlue(), electricFactor);
					GL11.glBegin(GL11.GL_TRIANGLES);
					for(Float[] vertex : light.vertices){
						//Add a slight translation and scaling to the light coords based on the normals to make the light.
						GL11.glTexCoord2f(vertex[3], vertex[4]);
						GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
						GL11.glVertex3f(vertex[0]+vertex[5]*0.0001F, vertex[1]+vertex[6]*0.0001F, vertex[2]+vertex[7]*0.0001F);	
					}
					GL11.glEnd();
					GL11.glDisable(GL11.GL_BLEND);
					GL11.glEnable(GL11.GL_LIGHTING);
				}
				GL11.glPopMatrix();
			}
			
			//Lens flare.
			if(lightActuallyOn && lightBrightness > 0 && MinecraftForgeClient.getRenderPass() != 0 && !wasRenderedPrior){
				for(byte i=0; i<light.centerPoints.length; ++i){
					GL11.glPushMatrix();
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					GL11.glEnable(GL11.GL_BLEND);
					GL11.glDisable(GL11.GL_LIGHTING);
					minecraft.entityRenderer.disableLightmap();
					minecraft.getTextureManager().bindTexture(lensFlareTexture);
					GL11.glColor4f(light.color.getRed(), light.color.getGreen(), light.color.getBlue(), lightBrightness);
					GL11.glBegin(GL11.GL_TRIANGLES);
					for(byte j=0; j<6; ++j){
						Float[] vertex = light.vertices[((short) i)*6+j];
						//Add a slight translation to the light size to make the flare move off it.
						//Then apply scaling factor to make the flare larger than the light.
						GL11.glTexCoord2f(vertex[3], vertex[4]);
						GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
						GL11.glVertex3d(vertex[0]+vertex[5]*0.0002F + (vertex[0] - light.centerPoints[i].xCoord)*light.size[i], 
								vertex[1]+vertex[6]*0.0002F + (vertex[1] - light.centerPoints[i].yCoord)*light.size[i], 
								vertex[2]+vertex[7]*0.0002F + (vertex[2] - light.centerPoints[i].zCoord)*light.size[i]);	
					}
					GL11.glEnd();
					GL11.glPopMatrix();
				}
			}
			
			//Render beam if light has one.
			if(lightActuallyOn && lightBrightness > 0 && light.type.hasBeam && MinecraftForgeClient.getRenderPass() == -1){
				GL11.glPushMatrix();
		    	GL11.glColor4f(1, 1, 1, Math.min(vehicle.electricPower > 4 ? 1.0F : 0, lightBrightness/2F));
		    	//TODO have Limit make a beam texture here.
		    	GL11.glDisable(GL11.GL_TEXTURE_2D);
		    	GL11.glDisable(GL11.GL_LIGHTING);
		    	GL11.glEnable(GL11.GL_BLEND);
		    	//Allows changing by changing alpha value.
		    	GL11.glDepthMask(false);
		    	GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
				minecraft.entityRenderer.disableLightmap();
				
				//As we can have more than one light per definition, we will only render 6 vertices at a time.
				//Use the center point arrays for this; normals are the same for all 6 vertex sets so use whichever.
				for(byte i=0; i<light.centerPoints.length; ++i){
					GL11.glPushMatrix();
					GL11.glTranslated(light.centerPoints[i].xCoord - light.vertices[i*6][5]*0.15F, light.centerPoints[i].yCoord - light.vertices[i*6][6]*0.15F, light.centerPoints[i].zCoord - light.vertices[i*6][7]*0.15F);
					Vec3d endpointVec = new Vec3d(light.vertices[i*6][5]*light.size[i]*2F, light.vertices[i*6][6]*light.size[i]*2F, light.vertices[i*6][7]*light.size[i]*2F);
					//Now that we are at the starting location for the beam, rotate the matrix to get the correct direction.
					GL11.glDepthMask(false);
					for(byte j=0; j<=2; ++j){
			    		drawCone(endpointVec, light.size[i]*0.75F, false);
			    	}
					drawCone(endpointVec, light.size[i]*0.75F, true);
					GL11.glPopMatrix();
				}
		    	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		    	GL11.glDepthMask(true);
				GL11.glDisable(GL11.GL_BLEND);
				GL11.glEnable(GL11.GL_LIGHTING);
				GL11.glEnable(GL11.GL_TEXTURE_2D);
				GL11.glPopMatrix();
			}
		}
	}
	
    private static void drawCone(Vec3d endPoint, double radius, boolean reverse){
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glVertex3d(0, 0, 0);
    	if(reverse){
    		for(float theta=0; theta < 2*Math.PI + 0.1; theta += 2F*Math.PI/40F){
    			GL11.glVertex3d(endPoint.xCoord + radius*Math.cos(theta), endPoint.yCoord + radius*Math.sin(theta), endPoint.zCoord);
    		}
    	}else{
    		for(float theta=(float) (2*Math.PI); theta>=0 - 0.1; theta -= 2F*Math.PI/40F){
    			GL11.glVertex3d(endPoint.xCoord + radius*Math.cos(theta), endPoint.yCoord + radius*Math.sin(theta), endPoint.zCoord);
    		}
    	}
    	GL11.glEnd();
    }
    
	private static void renderQuad(float width, float height){
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0, 0);
		GL11.glNormal3f(0, 0, -1);
		GL11.glVertex3f(width/2, height/2, 0);
		GL11.glTexCoord2f(0, 1);
		GL11.glNormal3f(0, 0, -1);
		GL11.glVertex3f(width/2, -height/2, 0);
		GL11.glTexCoord2f(1, 1);
		GL11.glNormal3f(0, 0, -1);
		GL11.glVertex3f(-width/2, -height/2, 0);
		GL11.glTexCoord2f(1, 0);
		GL11.glNormal3f(0, 0, -1);
		GL11.glVertex3f(-width/2, height/2, 0);
		GL11.glEnd();
	}
	
	private static void renderInstrumentsAndControls(EntityMultipartVehicle vehicle){
		GL11.glPushMatrix();
		GL11.glScalef(1F/16F/8F, 1F/16F/8F, 1F/16F/8F);
		for(byte i=0; i<vehicle.pack.motorized.instruments.size(); ++i){
			Instruments instrument = vehicle.getInstrumentNumber(i);
			PackInstrument packInstrument = vehicle.pack.motorized.instruments.get(i);
			if(instrument != null && packInstrument != null){
				GL11.glPushMatrix();
				GL11.glTranslatef(packInstrument.pos[0]*8, packInstrument.pos[1]*8, packInstrument.pos[2]*8);
				GL11.glRotatef(packInstrument.rot[0], 1, 0, 0);
				GL11.glRotatef(packInstrument.rot[1], 0, 1, 0);
				GL11.glRotatef(packInstrument.rot[2], 0, 0, 1);
				GL11.glScalef(packInstrument.scale, packInstrument.scale, packInstrument.scale);
				RenderInstruments.drawInstrument(vehicle, 0, 0, instrument, false, packInstrument.optionalEngineNumber);
				GL11.glPopMatrix();
			}
		}
		for(byte i=0; i<vehicle.pack.motorized.controls.size(); ++i){
			PackControl packControl = vehicle.pack.motorized.controls.get(i);
			GL11.glPushMatrix();
			GL11.glTranslatef(packControl.pos[0]*8, packControl.pos[1]*8, packControl.pos[2]*8);
			for(Controls control : Controls.values()){
				if(control.name().toLowerCase().equals(packControl.controlName)){
					RenderControls.drawControl(vehicle, control, false);
				}
			}
			GL11.glPopMatrix();
		}
		GL11.glPopMatrix();
	}
	
	private static void renderBoundingBoxes(EntityMultipartMoving mover){
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glColor3f(0.0F, 0.0F, 0.0F);
		GL11.glLineWidth(3.0F);
		for(MTSAxisAlignedBB box : mover.getCurrentCollisionBoxes()){
			//box = box.offset(-mover.posX, -mover.posY, -mover.posZ);
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex3d(box.relX - box.width/2F, box.relY - box.height/2F, box.relZ - box.width/2F);
			GL11.glVertex3d(box.relX + box.width/2F, box.relY - box.height/2F, box.relZ - box.width/2F);
			GL11.glVertex3d(box.relX - box.width/2F, box.relY - box.height/2F, box.relZ + box.width/2F);
			GL11.glVertex3d(box.relX + box.width/2F, box.relY - box.height/2F, box.relZ + box.width/2F);
			GL11.glVertex3d(box.relX - box.width/2F, box.relY + box.height/2F, box.relZ - box.width/2F);
			GL11.glVertex3d(box.relX + box.width/2F, box.relY + box.height/2F, box.relZ - box.width/2F);
			GL11.glVertex3d(box.relX - box.width/2F, box.relY + box.height/2F, box.relZ + box.width/2F);
			GL11.glVertex3d(box.relX + box.width/2F, box.relY + box.height/2F, box.relZ + box.width/2F);
			
			GL11.glVertex3d(box.relX - box.width/2F, box.relY - box.height/2F, box.relZ - box.width/2F);
			GL11.glVertex3d(box.relX - box.width/2F, box.relY - box.height/2F, box.relZ + box.width/2F);
			GL11.glVertex3d(box.relX + box.width/2F, box.relY - box.height/2F, box.relZ - box.width/2F);
			GL11.glVertex3d(box.relX + box.width/2F, box.relY - box.height/2F, box.relZ + box.width/2F);
			GL11.glVertex3d(box.relX - box.width/2F, box.relY + box.height/2F, box.relZ - box.width/2F);
			GL11.glVertex3d(box.relX - box.width/2F, box.relY + box.height/2F, box.relZ + box.width/2F);
			GL11.glVertex3d(box.relX + box.width/2F, box.relY + box.height/2F, box.relZ - box.width/2F);
			GL11.glVertex3d(box.relX + box.width/2F, box.relY + box.height/2F, box.relZ + box.width/2F);
			
			GL11.glVertex3d(box.relX - box.width/2F, box.relY - box.height/2F, box.relZ - box.width/2F);
			GL11.glVertex3d(box.relX - box.width/2F, box.relY + box.height/2F, box.relZ - box.width/2F);
			GL11.glVertex3d(box.relX + box.width/2F, box.relY - box.height/2F, box.relZ - box.width/2F);
			GL11.glVertex3d(box.relX + box.width/2F, box.relY + box.height/2F, box.relZ - box.width/2F);
			GL11.glVertex3d(box.relX - box.width/2F, box.relY - box.height/2F, box.relZ + box.width/2F);
			GL11.glVertex3d(box.relX - box.width/2F, box.relY + box.height/2F, box.relZ + box.width/2F);
			GL11.glVertex3d(box.relX + box.width/2F, box.relY - box.height/2F, box.relZ + box.width/2F);
			GL11.glVertex3d(box.relX + box.width/2F, box.relY + box.height/2F, box.relZ + box.width/2F);
			GL11.glEnd();
		}
		GL11.glLineWidth(1.0F);
		GL11.glColor3f(1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glPopMatrix();
	}
	
	private static void renderPartBoxes(EntityMultipartMoving mover){
		EntityPlayer player = minecraft.thePlayer;
		ItemStack heldStack = player.getHeldItemMainhand();
		if(heldStack != null){
			String partBoxToRender = heldStack.getItem().getRegistryName().getResourcePath();
			
			for(PackPart packPart : mover.pack.parts){
				boolean isPresent = false;
				boolean isHoldingPart = false;
				for(EntityMultipartChild child : mover.getChildren()){
					if(child.offsetX == packPart.pos[0] && child.offsetY == packPart.pos[1] && child.offsetZ == packPart.pos[2]){
						isPresent = true;
						break;
					}
				}
	
				for(String partName : packPart.names){
					if(partName.equals(partBoxToRender)){
						isHoldingPart = true;
						break;
					}
				}
						
				if(!isPresent && isHoldingPart){
					MTSVector offset = RotationSystem.getRotatedPoint(packPart.pos[0], packPart.pos[1], packPart.pos[2], mover.rotationPitch, mover.rotationYaw, mover.rotationRoll);
					AxisAlignedBB box = new AxisAlignedBB((float) (offset.xCoord) - 0.75F, (float) (offset.yCoord) - 0.75F, (float) (offset.zCoord) - 0.75F, (float) (offset.xCoord) + 0.75F, (float) (offset.yCoord) + 1.25F, (float) (offset.zCoord) + 0.75F);
					
					GL11.glPushMatrix();
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					GL11.glDisable(GL11.GL_LIGHTING);
					GL11.glEnable(GL11.GL_BLEND);
					GL11.glColor4f(0, 1, 0, 0.25F);
					GL11.glBegin(GL11.GL_QUADS);
					
					GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
					GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
					GL11.glVertex3d(box.minX, box.minY, box.maxZ);
					GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
					
					GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
					GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
					GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
					GL11.glVertex3d(box.maxX, box.minY, box.minZ);
					
					GL11.glVertex3d(box.maxX, box.minY, box.minZ);
					GL11.glVertex3d(box.minX, box.minY, box.minZ);
					GL11.glVertex3d(box.minX, box.maxY, box.minZ);
					GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
					
					GL11.glVertex3d(box.minX, box.minY, box.minZ);
					GL11.glVertex3d(box.minX, box.minY, box.maxZ);
					GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
					GL11.glVertex3d(box.minX, box.maxY, box.minZ);
					
					GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
					GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
					GL11.glVertex3d(box.minX, box.maxY, box.minZ);
					GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
					
					GL11.glVertex3d(box.minX, box.minY, box.maxZ);
					GL11.glVertex3d(box.minX, box.minY, box.minZ);
					GL11.glVertex3d(box.maxX, box.minY, box.minZ);
					GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
					GL11.glEnd();

					GL11.glColor4f(1, 1, 1, 1);
					GL11.glDisable(GL11.GL_BLEND);
					GL11.glEnable(GL11.GL_LIGHTING);
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					GL11.glPopMatrix();
				}
			}
		}
	}
	
	private static final class RotatablePart{
		private final String name;
		private final Vec3d rotationPoint;
		private final Float[][] vertices;
		
		private RotatablePart(String name, Vec3d rotationPoint, Float[][] vertices){
			this.name = name;
			this.rotationPoint = rotationPoint;
			this.vertices = vertices;
		}
	}
	
	private static final class WindowPart{
		private final String name;
		private final Float[][] vertices;
		
		private WindowPart(String name, Float[][] vertices){
			this.name = name;
			this.vertices = vertices;
		}
	}
	
	private static final class LightPart{
		private final LightTypes type;
		private final Float[][] vertices;
		private final Vec3d[] centerPoints;
		private final Float[] size;
		private final Color color;
		private final int flashBits;
		
		private LightPart(String name, Float[][] vertices){
			this.type = getTypeFromName(name.substring(1, name.indexOf('_')).toLowerCase());
			this.vertices = vertices;
			this.centerPoints = new Vec3d[vertices.length/6];
			this.size = new Float[vertices.length/6];
			
			for(byte i=0; i<centerPoints.length; ++i){
				double minX = 999;
				double maxX = -999;
				double minY = 999;
				double maxY = -999;
				double minZ = 999;
				double maxZ = -999;
				for(byte j=0; j<6; ++j){
					Float[] vertex = this.vertices[((short) i)*6 + j];
					minX = Math.min(vertex[0], minX);
					maxX = Math.max(vertex[0], maxX);
					minY = Math.min(vertex[1], minY);
					maxY = Math.max(vertex[1], maxY);
					minZ = Math.min(vertex[2], minZ);
					maxZ = Math.max(vertex[2], maxZ);
					
					//Adjust UV point here to change this to glass coords.
					switch(j){
						case(0): vertex[3] = 0.0F; vertex[4] = 0.0F; break;
						case(1): vertex[3] = 0.0F; vertex[4] = 1.0F; break;
						case(2): vertex[3] = 1.0F; vertex[4] = 1.0F; break;
						case(3): vertex[3] = 1.0F; vertex[4] = 1.0F; break;
						case(4): vertex[3] = 1.0F; vertex[4] = 0.0F; break;
						case(5): vertex[3] = 0.0F; vertex[4] = 0.0F; break;
					}
				}
				centerPoints[i] = new Vec3d(minX + (maxX - minX)/2D, minY + (maxY - minY)/2D, minZ + (maxZ - minZ)/2D);
				size[i] = (float) Math.max(Math.max(maxX - minX, maxZ - minZ), maxY - minY)*16F;
			}
			//Lights are in the format of "&NAME_FFFFFF_FFFFF_EXTRASTUFF"
			//Where NAME is what switch it goes to, FFFFFF is the color, and FFFFF is the blink rate. 
			this.color = Color.decode("0x" + name.substring(name.indexOf('_') + 1, name.indexOf('_') + 7));
			this.flashBits = Integer.decode("0x" + name.substring(name.lastIndexOf('_') + 1, name.lastIndexOf('_') + 6));
		}
		
		private LightTypes getTypeFromName(String lightName){
			for(LightTypes light : LightTypes.values()){
				if(light.name().toLowerCase().equals(lightName)){
					return light;
				}
			}
			return null;
		}
	}
}
