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
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackBeacon;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackControl;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackDisplayText;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackFileDefinitions;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackInstrument;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackLight;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackPart;
import minecrafttransportsimulator.dataclasses.MTSRegistryClient;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.entities.parts.EntityEngineCar;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.systems.ClientEventSystem;
import minecrafttransportsimulator.systems.ConfigSystem;
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
	/**Rotatable maps.  Key of main map is model name, key of value is part name.  Value of part name key is rotation point.*/
	private static final Map<String, Map<String, Vec3d>> rotatableMaps = new HashMap<String, Map<String, Vec3d>>();
	/**Names of windows in a model.  Keyed by model name.*/
	private static final Map<String, List<String>> windowLists = new HashMap<String, List<String>>();
	/**Model texture name.  Keyed by model name.*/
	private static final Map<String, ResourceLocation> textureMap = new HashMap<String, ResourceLocation>();
	private static final Map<EntityMultipartMoving, Byte> lastRenderPass = new HashMap<EntityMultipartMoving, Byte>();
	private static final Map<EntityMultipartMoving, Long> lastRenderTick = new HashMap<EntityMultipartMoving, Long>();
	private static final Map<EntityMultipartMoving, Float> lastRenderPartial = new HashMap<EntityMultipartMoving, Float>();
	private static final ResourceLocation vanillaGlassTexture = new ResourceLocation("minecraft", "textures/blocks/glass.png");
	private static final ResourceLocation lensFlareTexture = new ResourceLocation(MTS.MODID, "textures/parts/lensflare.png");
	
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
	
	public static void resetDisplayLists(){
		for(Integer displayList : displayLists.values()){
			GL11.glDeleteLists(displayList, 1);
		}
		displayLists.clear();
	}
		
	private static void render(EntityMultipartMoving mover, EntityPlayer playerRendering, float partialTicks, boolean wasRenderedPrior){
		//Calculate various things.
		double playerX;
        double playerY;
        double playerZ;
		Entity renderViewEntity = minecraft.getRenderViewEntity();
		
		boolean wasParentSpawnedAfterSeat = false;
		boolean wasParentSpawnedAfterPlayer = false;
		if(ClientEventSystem.playerLastSeat != null){
			wasParentSpawnedAfterPlayer = mover.getEntityId() > playerRendering.getEntityId();
			if(mover.equals(ClientEventSystem.playerLastSeat.parent)){
				wasParentSpawnedAfterSeat = mover.getEntityId() > ClientEventSystem.playerLastSeat.getEntityId();
				//System.out.format("%d %d %d\n", mover.getEntityId(), ClientEventSystem.playerLastSeat.getEntityId(), playerRendering.getEntityId());
			}
		}

		//FIXME figure out just what boolean goes here.
		//Last issue was with IDs 11336407 11336416 11336394 for parent, seat, and player.
		//Gotta be related to how the parent and seat link.  Rendered fine outside the plane, screwy inside.
		if(wasParentSpawnedAfterSeat){
			playerX = renderViewEntity.posX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * (double)partialTicks;
	        playerY = renderViewEntity.posY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * (double)partialTicks;
	        playerZ = renderViewEntity.posZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * (double)partialTicks;
		}else{
			playerX = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * (double)partialTicks;
	        playerY = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * (double)partialTicks;
	        playerZ = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * (double)partialTicks;
		}
        
        
        double thisX = mover.lastTickPosX + (mover.posX - mover.lastTickPosX) * (double)partialTicks;
        double thisY = mover.lastTickPosY + (mover.posY - mover.lastTickPosY) * (double)partialTicks;
        double thisZ = mover.lastTickPosZ + (mover.posZ - mover.lastTickPosZ) * (double)partialTicks;
        double rotateYaw = -mover.rotationYaw - (mover.rotationYaw - mover.prevRotationYaw)*(double)partialTicks;
        double rotatePitch = mover.rotationPitch + (mover.rotationPitch - mover.prevRotationPitch)*(double)partialTicks;
        double rotateRoll = mover.rotationRoll + (mover.rotationRoll - mover.prevRotationRoll)*(double)partialTicks;

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
			if(!wasRenderedPrior){
				renderBeacons(vehicle, sunLight, blockLight, lightBrightness, electricFactor);
			}
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
	
	private static Vec3d getRotationPointForMovable(Entry<String, Float[][]> entry){
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
		
		//If there are more than one of these parts remove everything after the second underscore.
		String partName = entry.getKey(); 
		while(partName.indexOf('_') != partName.lastIndexOf('_')){
			partName = partName.substring(0, partName.lastIndexOf('_'));
		}
		switch(partName){
			case("$Door_L"): return new Vec3d(maxX, minY, maxZ);
			case("$Door_R"): return new Vec3d(minX, minY, maxZ);
			
			case("$Gas"): return new Vec3d(minX + (maxX - minX)/2D, minY, minZ);
			case("$Brake"): return new Vec3d(minX + (maxX - minX)/2D, minY, minZ);
			case("$Parking"): return new Vec3d(minX + (maxX - minX)/2D, minY, minZ);
			case("$Gearshift"): return new Vec3d(minX + (maxX - minX)/2D, minY, minZ + (maxZ - minZ)/2D);
			case("$SteeringWheel"): return new Vec3d(minX + (maxX - minX)/2D, minY + (maxY - minY)/2D, maxZ);
			
			case("$Aileron_L"): return new Vec3d(maxX, minY + (maxY - minY)/2D, maxZ);
			case("$Aileron_R"): return new Vec3d(minX, minY + (maxY - minY)/2D, maxZ);
			case("$Elevator_L"): return new Vec3d(maxX, minY + (maxY - minY)/2D, maxZ);
			case("$Elevator_R"): return new Vec3d(minX, minY + (maxY - minY)/2D, maxZ);
			case("$Rudder"): return new Vec3d(0, minY + (maxY - minY)/2D, maxZ);
			case("$Flap_L"): return new Vec3d(maxX, minY + (maxY - minY)/2D, maxZ);
			case("$Flap_R"): return new Vec3d(minX, minY + (maxY - minY)/2D, maxZ);
			case("$WheelStrut"): return new Vec3d(minX + (maxX - minX)/2D, maxY, minZ + (maxZ - minZ)/2D);
			
			default: return new Vec3d(0, 0, 0);
		} 
	}
	
	private static void renderMainModel(EntityMultipartMoving mover){
		GL11.glPushMatrix();
		//Normally we use the pack name, but since all displaylists
		//are the same for all models, this is more appropriate.
		if(displayLists.containsKey(mover.pack.rendering.modelName)){
			GL11.glCallList(displayLists.get(mover.pack.rendering.modelName));
			
			//The display list only renders static parts.  We need to render dynamic ones manually.
			//If this is a window, don't render it as that gets done all at once later.
			for(Entry<String, Vec3d> entry : rotatableMaps.get(mover.pack.rendering.modelName).entrySet()){
				if(!windowLists.get(mover.pack.rendering.modelName).contains(entry.getKey())){
					GL11.glPushMatrix();
					rotateObject(mover, entry.getKey(), entry.getValue());
					GL11.glBegin(GL11.GL_TRIANGLES);
					for(Float[] vertex : MTSRegistryClient.modelMap.get(mover.name).get(entry.getKey())){
						GL11.glTexCoord2f(vertex[3], vertex[4]);
						GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
						GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
					}
					GL11.glEnd();
					GL11.glPopMatrix();
				}
			}
		}else{
			int displayListIndex = GL11.glGenLists(1);
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			Map<String, Vec3d> rotatableMap = new HashMap<String, Vec3d>();
			for(Entry<String, Float[][]> entry : MTSRegistryClient.modelMap.get(mover.name).entrySet()){
				//Don't add movable model parts or windows to the display list.
				//Those go in separate maps, with windows getting parsed after all parts in case they're movable.
				if(!entry.getKey().toLowerCase().contains("window")){
					if(!entry.getKey().toLowerCase().contains("$")){
						for(Float[] vertex : entry.getValue()){
							GL11.glTexCoord2f(vertex[3], vertex[4]);
							GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
							GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
						}
					}else{
						rotatableMap.put(entry.getKey(), getRotationPointForMovable(entry));
					}
				}
			}
			
			//Windows need to come after movables so they can take the movable's rotation points if they're movable too.
			List<String> windowList = new ArrayList<String>();
			for(Entry<String, Float[][]> entry : MTSRegistryClient.modelMap.get(mover.name).entrySet()){
				if(entry.getKey().toLowerCase().contains("window")){
					if(entry.getKey().toLowerCase().contains("$")){
						for(String rotatableName : rotatableMap.keySet()){
							if(entry.getKey().contains(rotatableName)){
								rotatableMap.put(entry.getKey(), rotatableMap.get(rotatableName));
								break;
							}
						}
					}
					windowList.add(entry.getKey());
				}
			}
			
			//Now finalize the moving and window maps.
			rotatableMaps.put(mover.pack.rendering.modelName, rotatableMap);
			windowLists.put(mover.pack.rendering.modelName, windowList);
			GL11.glEnd();
			GL11.glEndList();
			displayLists.put(mover.pack.rendering.modelName, displayListIndex);
		}
		GL11.glPopMatrix();
	}
	
	private static void rotateObject(EntityMultipartMoving mover, String rotatableName, Vec3d rotationPoint){
		//First translate to the center of the model for proper rotation.
		GL11.glTranslated(rotationPoint.xCoord, rotationPoint.yCoord, rotationPoint.zCoord);
		
		//Next rotate the part.
		if(rotatableName.contains("Door_L")){
			if(mover.parkingBrakeOn && mover.velocity == 0 && !mover.locked){
				GL11.glRotatef(-60F, 0, 1, 0);
			}
		}else if(rotatableName.contains("Door_R")){
			if(mover.parkingBrakeOn && mover.velocity == 0 && !mover.locked){
				GL11.glRotatef(60F, 0, 1, 0);
			}
		}else if(rotatableName.contains("Gas")){
			GL11.glRotatef(((EntityMultipartVehicle) mover).throttle/4F, 1, 0, 0);
		}else if(rotatableName.contains("Brake")){
			if(((EntityMultipartVehicle) mover).brakeOn){
				GL11.glRotatef(30, 1, 0, 0);
			}
		}else if(rotatableName.contains("Parking")){
			if(((EntityMultipartVehicle) mover).parkingBrakeOn){
				GL11.glRotatef(30, -1, 0, 0);
			}
		}else if(rotatableName.contains("Gearshift")){
			if(((EntityMultipartVehicle) mover).getEngineByNumber((byte) 1) != null){
				if(((EntityEngineCar) ((EntityMultipartVehicle) mover).getEngineByNumber((byte) 1)).isAutomatic){
					GL11.glRotatef(((EntityEngineCar) ((EntityMultipartVehicle) mover).getEngineByNumber((byte) 1)).getCurrentGear()*15, -1, 0, 0);
				}else{
					GL11.glRotatef(((EntityEngineCar) ((EntityMultipartVehicle) mover).getEngineByNumber((byte) 1)).getCurrentGear()*3, -1, 0, 0);
				}
			}
		}else if(rotatableName.contains("SteeringWheel")){
			GL11.glRotatef(mover.getSteerAngle(), 0, 0, 1);
		}else if(rotatableName.contains("Aileron_L")){
			GL11.glRotatef(((EntityPlane) mover).aileronAngle/10F, -1, 0, 0);
		}else if(rotatableName.contains("Aileron_R")){
			GL11.glRotatef(((EntityPlane) mover).aileronAngle/10F, 1, 0, 0);
		}else if(rotatableName.contains("Elevator")){
			GL11.glRotatef(((EntityPlane) mover).elevatorAngle/10F, 1, 0, 0);
		}else if(rotatableName.contains("Rudder")){
			GL11.glRotatef(((EntityPlane) mover).rudderAngle/10F, 0, 1, 0);
		}else if(rotatableName.contains("Flap")){
			GL11.glRotatef(((EntityPlane) mover).flapAngle/10F, -1, 0, 0);
		}else if(rotatableName.contains("WheelStrut")){
			GL11.glRotatef(((EntityPlane) mover).rudderAngle/10F, 0, -1, 0);
		}
		
		//Now translate the part back to it's original position.
		GL11.glTranslated(-rotationPoint.xCoord, -rotationPoint.yCoord, -rotationPoint.zCoord);
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
				String windowName = windowLists.get(mover.pack.rendering.modelName).get(i);
				Float[][] windowCoords =  MTSRegistryClient.modelMap.get(mover.name).get(windowName);
				if(rotatableMaps.get(mover.pack.rendering.modelName).containsKey(windowName)){
					rotateObject(mover, windowName, rotatableMaps.get(mover.pack.rendering.modelName).get(windowName));
				}
				//If this window is a quad, draw quads.  Otherwise draw tris.
				if(windowCoords.length == 4){
					GL11.glBegin(GL11.GL_QUADS);
				}else{
					GL11.glBegin(GL11.GL_TRIANGLES);
				}
				for(Float[] vertex : windowCoords){
					GL11.glTexCoord2f(vertex[3], vertex[4]);
					GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
					GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
				}
				if(ConfigSystem.getBooleanConfig("InnerWindows")){
					for(int j=windowCoords.length-1; j >= 0; --j){
						Float[] vertex = windowCoords[j];
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
		for(PackLight light : vehicle.pack.rendering.lights){
			boolean lightOn = (vehicle.lightStatus>>(light.switchNumber-1) & 1) == 1;
			boolean overrideCaseBrightness = lightBrightness > Math.max(sunLight, blockLight) && lightOn && lightBrightness > 0;

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
				minecraft.getTextureManager().bindTexture(vanillaGlassTexture);
				GL11.glTranslatef(light.pos[0], light.pos[1], light.pos[2]);
				GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
				GL11.glRotatef(light.rot[0], 1, 0, 0);
				GL11.glRotatef(light.rot[1], 0, 1, 0);
				GL11.glRotatef(light.rot[2], 0, 0, 1);
				GL11.glColor3f(1.0F, 1.0F, 1.0F);
				
				//Light case cover.
				GL11.glPushMatrix();
				GL11.glTranslatef(0, 0, 0.01F);
				GL11.glRotatef(180, 0, 1, 0);
				renderQuad(light.width, light.length);
				GL11.glPopMatrix();
				
				if(lightOn){
					//Light pre-render operations
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					GL11.glDisable(GL11.GL_LIGHTING);
					GL11.glEnable(GL11.GL_BLEND);
					Color lightColor = Color.decode(light.color);
					GL11.glColor4f(lightColor.getRed(), lightColor.getGreen(), lightColor.getBlue(), electricFactor);
					
					//Light center.
					GL11.glPushMatrix();
					GL11.glTranslatef(0, 0, 0.005F);
					GL11.glRotatef(180, 1, 0, 0);
					renderQuad(light.width, light.length);
					GL11.glPopMatrix();
					
					GL11.glDisable(GL11.GL_BLEND);
					GL11.glEnable(GL11.GL_LIGHTING);
					GL11.glEnable(GL11.GL_TEXTURE_2D);
				}
				GL11.glPopMatrix();
			}
			
			//Light cone
			if(lightOn && light.beamDistance != 0 && MinecraftForgeClient.getRenderPass() == -1 && lightBrightness > 0){
				GL11.glPushMatrix();
		    	GL11.glColor4f(1, 1, 1, Math.min(vehicle.electricPower > 4 ? 1.0F : 0, lightBrightness/2F));
		    	GL11.glDisable(GL11.GL_TEXTURE_2D);
		    	GL11.glDisable(GL11.GL_LIGHTING);
		    	GL11.glEnable(GL11.GL_BLEND);
		    	//Allows changing by changing alpha value.
		    	GL11.glDepthMask(false);
		    	GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
		    	
				minecraft.entityRenderer.disableLightmap();
				GL11.glTranslatef(light.pos[0], light.pos[1], light.pos[2]);
				GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
				GL11.glRotatef(light.lightRot[0], 0, 1, 0);
				GL11.glRotatef(light.lightRot[1], 1, 0, 0);
				GL11.glRotatef(light.lightRot[2], 0, 0, 0);
				GL11.glScalef(16F, 16F, 16F);
				GL11.glDepthMask(false);
				byte lightReps = (byte) ((light.brightness/20F) + 1);
				for(byte i=0; i<=lightReps; ++i){
		    		drawCone(light.beamDiameter, light.beamDistance, false);
		    	}
		    	drawCone(light.beamDiameter, light.beamDistance, true);
		    	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		    	GL11.glDepthMask(true);
				GL11.glPopMatrix();
			}
			
			if(MinecraftForgeClient.getRenderPass() != 0 && !wasRenderedPrior){
				//Render lens flare in pass 1 or -1 for transparency.
				if(lightOn && lightBrightness > 0){
					renderLensFlare(light.pos, light.rot, light.brightness, light.color, lightBrightness, true);
				}
			}
		}
	}
	
    private static void drawCone(double r, double l, boolean reverse){
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glVertex3d(0, 0, 0);
    	if(reverse){
    		for(float theta=0; theta < 2*Math.PI + 0.1; theta += 2F*Math.PI/40F){
    			GL11.glVertex3d(r*Math.cos(theta), r*Math.sin(theta), l);
    		}
    	}else{
    		for(float theta=(float) (2*Math.PI); theta>=0 - 0.1; theta -= 2F*Math.PI/40F){
    			GL11.glVertex3d(r*Math.cos(theta), r*Math.sin(theta), l);
    		}
    	}
    	GL11.glEnd();
    }
	
	private static void renderBeacons(EntityMultipartVehicle vehicle, float sunLight, float blockLight, float lightBrightness, float electricFactor){		
		for(PackBeacon beacon : vehicle.pack.rendering.beacons){
			boolean beaconOn = (vehicle.lightStatus>>(beacon.switchNumber-1) & 1) == 1 && (!beacon.flashing || vehicle.ticksExisted%20 <= 2);
			boolean overrideCaseBrightness = lightBrightness > Math.max(sunLight, blockLight) && beaconOn;

			if(MinecraftForgeClient.getRenderPass() != 1){
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
				minecraft.getTextureManager().bindTexture(vanillaGlassTexture);
				GL11.glTranslatef(beacon.pos[0], beacon.pos[1], beacon.pos[2]);
				GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
				GL11.glRotatef(beacon.rot[0], 1, 0, 0);
				GL11.glRotatef(beacon.rot[1], 0, 1, 0);
				GL11.glRotatef(beacon.rot[2], 0, 0, 1);
				GL11.glColor3f(1.0F, 1.0F, 1.0F);
			
				//Light case edges.
				for(byte i=0; i<=3; ++i){
					GL11.glPushMatrix();
					GL11.glRotatef(90*i, 0, 1, 0);
					if(i == 0 || i == 2){
						GL11.glTranslatef(0, beacon.height/2F, -beacon.length/2F);
						renderQuad(beacon.width, beacon.height);
					}else{
						GL11.glTranslatef(0, beacon.height/2F, -beacon.width/2F);
						renderQuad(beacon.length, beacon.height);
					}
					GL11.glPopMatrix();
				}
				
				//Light case cover.
				GL11.glPushMatrix();
				GL11.glTranslatef(0, beacon.height, 0);
				GL11.glRotatef(90, 1, 0, 0);
				renderQuad(beacon.width, beacon.length);
				GL11.glPopMatrix();
				
				if(beaconOn){
					//Light pre-render operations
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					GL11.glDisable(GL11.GL_LIGHTING);
					GL11.glEnable(GL11.GL_BLEND);
					Color beaconColor = Color.decode(beacon.color);
					GL11.glColor4f(beaconColor.getRed(), beaconColor.getGreen(), beaconColor.getBlue(), electricFactor);
					
					//Light edges.
					for(byte i=0; i<=3; ++i){
						GL11.glPushMatrix();
						GL11.glRotatef(90*i, 0, 1, 0);
						if(i == 0 || i == 2){
							GL11.glTranslatef(0, beacon.height/2F - 0.01F, -beacon.length/2F + 0.01F);
							renderQuad(beacon.width, beacon.height);
						}else{
							GL11.glTranslatef(0, beacon.height/2F, -beacon.width/2F);
							renderQuad(beacon.length, beacon.height);
						}
						GL11.glPopMatrix();
					}
					
					
					//Light center.
					GL11.glPushMatrix();
					GL11.glTranslatef(0, beacon.height - 0.005F, 0);
					GL11.glRotatef(90, 1, 0, 0);
					renderQuad(beacon.width - 0.01F, beacon.length - 0.01F);
					GL11.glPopMatrix();
					
					GL11.glDisable(GL11.GL_BLEND);
					GL11.glEnable(GL11.GL_LIGHTING);
					GL11.glEnable(GL11.GL_TEXTURE_2D);
				}				
				GL11.glPopMatrix();
			}
			if(MinecraftForgeClient.getRenderPass() != 0){
				//Render lens flare in pass 1 or -1 for transparency.
				if(beaconOn && lightBrightness > 0){
					renderLensFlare(beacon.pos, beacon.rot, beacon.brightness, beacon.color, lightBrightness, false);
				}
			}
		}
	}
	
	private static void renderLensFlare(float[] pos, float[] rot, float size, String color, float brightness, boolean isLight){
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LIGHTING);
		minecraft.entityRenderer.disableLightmap();
		minecraft.getTextureManager().bindTexture(lensFlareTexture);
		GL11.glTranslatef(pos[0], pos[1], pos[2]);
		GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
		GL11.glRotatef(rot[0], 1, 0, 0);
		GL11.glRotatef(rot[1], 0, 1, 0);
		GL11.glRotatef(rot[2], 0, 0, 1);
		if(isLight){
			GL11.glRotatef(180, 0, 1, 0);
		}else{
			GL11.glRotatef(90, 1, 0, 0);
		}
		GL11.glTranslatef(0, 0, -0.001F);
		
		Color flareColor = Color.decode(color);
		GL11.glColor4f(flareColor.getRed()/255F, flareColor.getGreen()/255F, flareColor.getBlue()/255F, brightness);
		renderQuad(size*brightness, size*brightness);
		GL11.glPopMatrix();
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
}
