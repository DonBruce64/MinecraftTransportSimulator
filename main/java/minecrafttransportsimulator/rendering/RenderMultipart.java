package minecrafttransportsimulator.rendering;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MultipartAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.MTSControls.Controls;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackControl;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackDisplayText;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackInstrument;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackRotatableModelObject;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle.LightTypes;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle.VehicleInstrument;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;
import minecrafttransportsimulator.multipart.parts.APart;
import minecrafttransportsimulator.multipart.parts.PartEngineCar;
import minecrafttransportsimulator.systems.ClientEventSystem;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.systems.SFXSystem.FXPart;
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

/**Main render class for all multipart entities.  Renders the multipart, along with all parts.
 * As entities don't render above 255 well due to the new chunk visibility system, 
 * this code is called both from the regular render loop and manually from
 * {@link ClientEventSystem#on(RenderWorldLastEvent)}.
 *
 * @author don_bruce
 */
public final class RenderMultipart extends Render<EntityMultipartD_Moving>{
	private static final Minecraft minecraft = Minecraft.getMinecraft();
	
	/**Display list GL integers.  Keyed by multipart name name.*/
	private static final Map<String, Integer> multipartDisplayLists = new HashMap<String, Integer>();
	
	/**Display list GL integers.  Keyed by part ResourceLocation (this allows for part model changes).*/
	private static final Map<ResourceLocation, Integer> partDisplayLists = new HashMap<ResourceLocation, Integer>();
	
	/**Rotatable parts for models.  Keyed by multipart JSON name.*/
	private static final Map<String, List<RotatablePart>> rotatableLists = new HashMap<String, List<RotatablePart>>();
	
	/**Window parts for models.  Keyed by multipart JSON name.*/
	private static final Map<String, List<WindowPart>> windowLists = new HashMap<String, List<WindowPart>>();
	
	/**Lights for models.  Keyed by multipart JSON name.*/
	private static final Map<String, List<LightPart>> multipartLightLists = new HashMap<String, List<LightPart>>();
	
	/**Lights for parts.  Keyed by a combination of part name and position.*/
	private static final Map<String, List<LightPart>> partLightLists = new HashMap<String, List<LightPart>>();
	
	/**Multipart texture name.  Keyed by multipart name (NOT JSON!) or part name.*/
	private static final Map<String, ResourceLocation> textureMap = new HashMap<String, ResourceLocation>();
	
	private static final Map<EntityMultipartD_Moving, Byte> lastRenderPass = new HashMap<EntityMultipartD_Moving, Byte>();
	private static final Map<EntityMultipartD_Moving, Long> lastRenderTick = new HashMap<EntityMultipartD_Moving, Long>();
	private static final Map<EntityMultipartD_Moving, Float> lastRenderPartial = new HashMap<EntityMultipartD_Moving, Float>();
		
	private static final ResourceLocation vanillaGlassTexture = new ResourceLocation("minecraft", "textures/blocks/glass.png");
	private static final ResourceLocation lensFlareTexture = new ResourceLocation(MTS.MODID, "textures/rendering/lensflare.png");
	private static final ResourceLocation lightTexture = new ResourceLocation(MTS.MODID, "textures/rendering/light.png");
	private static final ResourceLocation lightBeamTexture = new ResourceLocation(MTS.MODID, "textures/rendering/lightbeam.png");
	
	public RenderMultipart(RenderManager renderManager){
		super(renderManager);
	}
	
	/**Used to clear out the rendering caches in dev mode to allow the re-loading of models.**/
	public static void clearCaches(){
		for(Integer index : multipartDisplayLists.values()){
			GL11.glDeleteLists(index, 1);
		}
		multipartDisplayLists.clear();
		for(Integer index : partDisplayLists.values()){
			GL11.glDeleteLists(index, 1);
		}
		partDisplayLists.clear();
		rotatableLists.clear();
		windowLists.clear();
		multipartLightLists.clear();
	}
	
	/**Returns the currently cached texture for the multipart.  Static for use in other functions.**/
	public static ResourceLocation getTextureForMultipart(EntityMultipartD_Moving entity){
		return textureMap.get(entity.multipartName);
	}
	
	@Override
	protected ResourceLocation getEntityTexture(EntityMultipartD_Moving entity){
		return null;
	}
	
	@Override
	public void doRender(EntityMultipartD_Moving multipart, double x, double y, double z, float entityYaw, float partialTicks){
		boolean didRender = false;
		if(multipart.pack != null){ 
			if(lastRenderPass.containsKey(multipart)){
				//Did we render this tick?
				if(lastRenderTick.get(multipart) == multipart.worldObj.getTotalWorldTime() && lastRenderPartial.get(multipart) == partialTicks){
					//If we rendered last on a pass of 0 or 1 this tick, don't re-render some things.
					if(lastRenderPass.get(multipart) != -1 && MinecraftForgeClient.getRenderPass() == -1){
						render(multipart, Minecraft.getMinecraft().thePlayer, partialTicks, true);
						didRender = true;
					}
				}
			}
			if(!didRender){
				render(multipart, Minecraft.getMinecraft().thePlayer, partialTicks, false);
			}
			lastRenderPass.put(multipart, (byte) MinecraftForgeClient.getRenderPass());
			lastRenderTick.put(multipart, multipart.worldObj.getTotalWorldTime());
			lastRenderPartial.put(multipart, partialTicks);
		}
	}
	
	public static boolean doesMultipartHaveLight(EntityMultipartE_Vehicle vehicle, LightTypes light){
		for(LightPart lightPart : multipartLightLists.get(vehicle.multipartJSONName)){
			if(lightPart.type.equals(light)){
				return true;
			}
		}
		return false;
	}
	
	private static void render(EntityMultipartD_Moving multipart, EntityPlayer playerRendering, float partialTicks, boolean wasRenderedPrior){
		//Calculate various things.
		Entity renderViewEntity = minecraft.getRenderViewEntity();
		double playerX = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * (double)partialTicks;
		double playerY = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * (double)partialTicks;
		double playerZ = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * (double)partialTicks;
        
        
        double thisX = multipart.lastTickPosX + (multipart.posX - multipart.lastTickPosX) * (double)partialTicks;
        double thisY = multipart.lastTickPosY + (multipart.posY - multipart.lastTickPosY) * (double)partialTicks;
        double thisZ = multipart.lastTickPosZ + (multipart.posZ - multipart.lastTickPosZ) * (double)partialTicks;
        double rotateYaw = -multipart.rotationYaw + (multipart.rotationYaw - multipart.prevRotationYaw)*(double)(1 - partialTicks);
        double rotatePitch = multipart.rotationPitch - (multipart.rotationPitch - multipart.prevRotationPitch)*(double)(1 - partialTicks);
        double rotateRoll = multipart.rotationRoll - (multipart.rotationRoll - multipart.prevRotationRoll)*(double)(1 - partialTicks);

        //Set up position and lighting.
        GL11.glPushMatrix();
        GL11.glTranslated(thisX - playerX, thisY - playerY, thisZ - playerZ);
        int lightVar = multipart.getBrightnessForRender(partialTicks);
        minecraft.entityRenderer.enableLightmap();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightVar%65536, lightVar/65536);
        RenderHelper.enableStandardItemLighting();
        
		//Bind texture.  Adds new element to cache if needed.
		if(!textureMap.containsKey(multipart.multipartName)){
			textureMap.put(multipart.multipartName, new ResourceLocation(multipart.multipartName.substring(0, multipart.multipartName.indexOf(':')), "textures/vehicles/" + multipart.multipartName.substring(multipart.multipartName.indexOf(':') + 1) + ".png"));
		}
		minecraft.getTextureManager().bindTexture(textureMap.get(multipart.multipartName));
		//Render all the model parts except windows.
		//Those need to be rendered after the player if the player is rendered manually.
		if(MinecraftForgeClient.getRenderPass() != 1 && !wasRenderedPrior){
			GL11.glPushMatrix();
			GL11.glRotated(rotateYaw, 0, 1, 0);
	        GL11.glRotated(rotatePitch, 1, 0, 0);
	        GL11.glRotated(rotateRoll, 0, 0, 1);
			renderMainModel(multipart, partialTicks);
			renderParts(multipart, partialTicks);
			GL11.glEnable(GL11.GL_NORMALIZE);
			renderWindows(multipart, partialTicks);
			renderTextMarkings(multipart);
			if(multipart instanceof EntityMultipartE_Vehicle){
				renderInstrumentsAndControls((EntityMultipartE_Vehicle) multipart);
			}
			GL11.glDisable(GL11.GL_NORMALIZE);
			if(Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox()){
				renderBoundingBoxes(multipart);
			}
			GL11.glPopMatrix();
		}
		
		//Check to see if we need to manually render riders.
		//MC culls rendering above build height depending on the direction the player is looking.
 		//Due to inconsistent culling based on view angle, this can lead to double-renders.
 		//Better than not rendering at all I suppose.
		if(MinecraftForgeClient.getRenderPass() != 1 && !wasRenderedPrior){
			for(Entity passenger : multipart.getPassengers()){
				if(!(minecraft.thePlayer.equals(passenger) && minecraft.gameSettings.thirdPersonView == 0) && passenger.posY > passenger.worldObj.getHeight()){
		        	 GL11.glPushMatrix();
		        	 GL11.glTranslated(passenger.posX - multipart.posX, passenger.posY - multipart.posY, passenger.posZ - multipart.posZ);
		        	 Minecraft.getMinecraft().getRenderManager().renderEntityStatic(passenger, partialTicks, false);
		        	 GL11.glPopMatrix();
		         }
			}
		}
		
		//Lights and beacons get rendered in two passes.
		//The first renders the cases and bulbs, the second renders the beams and effects.
		//Make sure the light list is populated here before we try to render this, as loading de-syncs can leave it null.
		if(multipart instanceof EntityMultipartE_Vehicle && multipartLightLists.get(multipart.multipartJSONName) != null){
			EntityMultipartE_Vehicle vehicle = (EntityMultipartE_Vehicle) multipart;
			float sunLight = vehicle.worldObj.getSunBrightness(0)*vehicle.worldObj.getLightBrightness(vehicle.getPosition());
			float blockLight = vehicle.worldObj.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, vehicle.getPosition())/15F;
			float electricFactor = (float) Math.min(vehicle.electricPower > 2 ? (vehicle.electricPower-2)/6F : 0, 1);
			float lightBrightness = (float) Math.min((1 - Math.max(sunLight, blockLight))*electricFactor, 1);

			GL11.glPushMatrix();
			GL11.glEnable(GL11.GL_NORMALIZE);
			GL11.glRotated(rotateYaw, 0, 1, 0);
	        GL11.glRotated(rotatePitch, 1, 0, 0);
	        GL11.glRotated(rotateRoll, 0, 0, 1);
	        renderLights(vehicle, sunLight, blockLight, lightBrightness, electricFactor, wasRenderedPrior, partialTicks);
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
			renderPartBoxes(multipart);
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
		
		//Update SFX.
		if(!wasRenderedPrior && multipart instanceof EntityMultipartE_Vehicle){
			EntityMultipartE_Vehicle vehicle = (EntityMultipartE_Vehicle) multipart;
			SFXSystem.updateMultipartSounds(vehicle, partialTicks);
			for(APart part : vehicle.getMultipartParts()){
				if(part instanceof FXPart){
					SFXSystem.doFX((FXPart) part, vehicle.worldObj);
				}
			}
		}
	}
	
	private static void renderMainModel(EntityMultipartD_Moving multipart, float partialTicks){
		GL11.glPushMatrix();
		//Normally we use the pack name, but since all displaylists
		//are the same for all models, this is more appropriate.
		if(multipartDisplayLists.containsKey(multipart.multipartJSONName)){
			GL11.glCallList(multipartDisplayLists.get(multipart.multipartJSONName));
			
			//The display list only renders static parts.  We need to render dynamic ones manually.
			//If this is a window, don't render it as that gets done all at once later.
			for(RotatablePart rotatable : rotatableLists.get(multipart.multipartJSONName)){
				if(!rotatable.name.contains("window")){
					GL11.glPushMatrix();
					rotateObject(multipart, rotatable, partialTicks);
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
			Map<String, Float[][]> parsedModel = OBJParserSystem.parseOBJModel(new ResourceLocation(multipart.multipartName.substring(0, multipart.multipartName.indexOf(':')), "objmodels/vehicles/" + multipart.multipartJSONName + ".obj"));
			int displayListIndex = GL11.glGenLists(1);
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			List<RotatablePart> rotatableParts = new ArrayList<RotatablePart>();
			List<WindowPart> windows = new ArrayList<WindowPart>();
			List<LightPart> lightParts = new ArrayList<LightPart>();
			for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
				//Don't add rotatable model parts or windows to the display list.
				//Those go in separate maps, with windows going into both a rotatable and window mapping.
				//Do add lights, as they will be rendered both as part of the model and with special things.
				boolean shouldShapeBeInDL = true;
				if(entry.getKey().contains("$")){
					rotatableParts.add(new RotatablePart(multipart, entry.getKey(), entry.getValue()));
					shouldShapeBeInDL = false;
				}
				if(entry.getKey().contains("&")){
					lightParts.add(new LightPart(entry.getKey(), entry.getValue()));
				}
				if(entry.getKey().toLowerCase().contains("window")){
					windows.add(new WindowPart(entry.getKey(), entry.getValue()));
					shouldShapeBeInDL = false;
				}
				if(shouldShapeBeInDL){
					for(Float[] vertex : entry.getValue()){
						GL11.glTexCoord2f(vertex[3], vertex[4]);
						GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
						GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
					}
				}
			}
			GL11.glEnd();
			GL11.glEndList();
			
			//Now finalize the maps.
			rotatableLists.put(multipart.multipartJSONName, rotatableParts);
			windowLists.put(multipart.multipartJSONName, windows);
			multipartLightLists.put(multipart.multipartJSONName, lightParts);
			multipartDisplayLists.put(multipart.multipartJSONName, displayListIndex);
		}
		GL11.glPopMatrix();
	}
	
	private static void rotateObject(EntityMultipartD_Moving multipart, RotatablePart rotatable, float partialTicks){
		for(byte i=0; i<rotatable.rotationVariables.length; ++i){
			float rotation = getRotationAngleForVariable(multipart, rotatable.rotationVariables[i], partialTicks);
			if(rotation != 0){
				GL11.glTranslated(rotatable.rotationPoints[i].xCoord, rotatable.rotationPoints[i].yCoord, rotatable.rotationPoints[i].zCoord);
				GL11.glRotated(rotation*rotatable.rotationMagnitudes[i], rotatable.rotationAxis[i].xCoord, rotatable.rotationAxis[i].yCoord, rotatable.rotationAxis[i].zCoord);
				GL11.glTranslated(-rotatable.rotationPoints[i].xCoord, -rotatable.rotationPoints[i].yCoord, -rotatable.rotationPoints[i].zCoord);
			}
		}
	}
	
	private static float getRotationAngleForVariable(EntityMultipartD_Moving multipart, String variable, float partialTicks){
		switch(variable){
			case("door"): return multipart.parkingBrakeOn && multipart.velocity == 0 && !multipart.locked ? 60 : 0;
			case("throttle"): return ((EntityMultipartE_Vehicle) multipart).throttle/4F;
			case("brake"): return multipart.brakeOn ? 25 : 0;
			case("p_brake"): return multipart.parkingBrakeOn ? 30 : 0;
			case("gearshift"): return ((EntityMultipartE_Vehicle) multipart).getEngineByNumber((byte) 0) != null ? (((PartEngineCar) ((EntityMultipartE_Vehicle) multipart).getEngineByNumber((byte) 0))).getGearshiftRotation() : 0;
			case("engine"): return (float) (((EntityMultipartE_Vehicle) multipart).getEngineByNumber((byte) 0) != null ? ((PartEngineCar) ((EntityMultipartE_Vehicle) multipart).getEngineByNumber((byte) 0)).getEngineRotation(partialTicks) : 0);
			case("driveshaft"): return (float) (((EntityMultipartE_Vehicle) multipart).getEngineByNumber((byte) 0) != null ? ((PartEngineCar) ((EntityMultipartE_Vehicle) multipart).getEngineByNumber((byte) 0)).getDriveshaftRotation(partialTicks) : 0);
			case("steeringwheel"): return multipart.getSteerAngle();
			
			case("aileron"): return ((EntityMultipartF_Plane) multipart).aileronAngle/10F;
			case("elevator"): return ((EntityMultipartF_Plane) multipart).elevatorAngle/10F;
			case("rudder"): return ((EntityMultipartF_Plane) multipart).rudderAngle/10F;
			case("flap"): return ((EntityMultipartF_Plane) multipart).flapAngle/10F;
			case("trim_aileron"): return ((EntityMultipartF_Plane) multipart).aileronTrim/10F;
			case("trim_elevator"): return ((EntityMultipartF_Plane) multipart).elevatorTrim/10F;
			case("trim_rudder"): return ((EntityMultipartF_Plane) multipart).rudderTrim/10F;
			case("reverser"): return ((EntityMultipartF_Plane) multipart).reversePercent/1F;
			default: return 0;
		}
	}
	
	private static void rotatePart(APart part, Vec3d actionRotation, boolean cullface){
		if(part.turnsWithSteer){
			if(part.offset.zCoord >= 0){
				GL11.glRotatef(part.multipart.getSteerAngle(), 0, 1, 0);
			}else{
				GL11.glRotatef(-part.multipart.getSteerAngle(), 0, 1, 0);
			}
		}
		
		if((part.offset.xCoord < 0 && !part.overrideMirror) || (part.offset.xCoord > 0 && part.overrideMirror)){
			GL11.glScalef(-1.0F, 1.0F, 1.0F);
			if(cullface){
				GL11.glCullFace(GL11.GL_FRONT);
			}
		}
		
		GL11.glRotated(part.partRotation.xCoord, 1, 0, 0);
		GL11.glRotated(part.partRotation.yCoord, 0, 1, 0);
		GL11.glRotated(part.partRotation.zCoord, 0, 0, 1);
		
		GL11.glRotated(actionRotation.xCoord, 1, 0, 0);
		GL11.glRotated(actionRotation.yCoord, 0, 1, 0);
		GL11.glRotated(actionRotation.zCoord, 0, 0, 1);
	}
	
	private static void renderParts(EntityMultipartD_Moving multipart, float partialTicks){
		for(APart part : multipart.getMultipartParts()){
			ResourceLocation partModelLocation = part.getModelLocation();
			if(partModelLocation == null){
				continue;
			}else if(!partDisplayLists.containsKey(partModelLocation)){
    			Map<String, Float[][]> parsedModel = OBJParserSystem.parseOBJModel(partModelLocation);
    			int displayListIndex = GL11.glGenLists(1);
    			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
    			GL11.glBegin(GL11.GL_TRIANGLES);
    			for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
    				for(Float[] vertex : entry.getValue()){
    					GL11.glTexCoord2f(vertex[3], vertex[4]);
    					GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
    					GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
    				}
    			}
    			GL11.glEnd();
    			GL11.glEndList();
    			partDisplayLists.put(partModelLocation, displayListIndex);
    		}else if(!partLightLists.containsKey(part.partName + part.offset.toString())){
				List<LightPart> lightParts = new ArrayList<LightPart>();
				for(Entry<String, Float[][]> entry : OBJParserSystem.parseOBJModel(partModelLocation).entrySet()){
					if(entry.getKey().contains("&")){
						lightParts.add(new LightPart(entry.getKey(), entry.getValue()));
					}
    			}
				partLightLists.put(part.partName + part.offset.toString(), lightParts);
			}
			
			if(!textureMap.containsKey(part.partName)){
				textureMap.put(part.partName, part.getTextureLocation());
			}
			
			Vec3d actionRotation = part.getActionRotation(partialTicks);
			GL11.glPushMatrix();
			GL11.glTranslated(part.offset.xCoord, part.offset.yCoord, part.offset.zCoord);
			rotatePart(part, actionRotation, true);
    		minecraft.getTextureManager().bindTexture(textureMap.get(part.partName));
			GL11.glCallList(partDisplayLists.get(partModelLocation));
			GL11.glCullFace(GL11.GL_BACK);
			GL11.glPopMatrix();
        }
	}
	
	private static void renderWindows(EntityMultipartD_Moving multipart, float partialTicks){
		minecraft.getTextureManager().bindTexture(vanillaGlassTexture);
		//Iterate through all windows.
		for(byte i=0; i<windowLists.get(multipart.multipartJSONName).size(); ++i){
			if(i >= multipart.brokenWindows){
				GL11.glPushMatrix();
				//This is a window or set of windows.  Like the model, it will be triangle-based.
				//However, windows may be rotatable.  Check this before continuing.
				WindowPart window = windowLists.get(multipart.multipartJSONName).get(i);
				for(RotatablePart rotatable : rotatableLists.get(multipart.multipartJSONName)){
					if(rotatable.name.equals(window.name)){
						rotateObject(multipart, rotatable, partialTicks);
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
	
	private static void renderTextMarkings(EntityMultipartD_Moving multipart){
		for(PackDisplayText text : multipart.pack.rendering.textMarkings){
			GL11.glPushMatrix();
			GL11.glTranslatef(text.pos[0], text.pos[1], text.pos[2]);
			GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
			GL11.glRotatef(text.rot[0], 1, 0, 0);
			GL11.glRotatef(text.rot[1] + 180, 0, 1, 0);
			GL11.glRotatef(text.rot[2] + 180, 0, 0, 1);
			GL11.glScalef(text.scale, text.scale, text.scale);
			RenderHelper.disableStandardItemLighting();
			minecraft.fontRendererObj.drawString(multipart.displayText, -minecraft.fontRendererObj.getStringWidth(multipart.displayText)/2, 0, Color.decode(text.color).getRGB());
			GL11.glPopMatrix();
		}
		GL11.glColor3f(1.0F, 1.0F, 1.0F);
		RenderHelper.enableStandardItemLighting();
	}
	
	private static void renderLights(EntityMultipartE_Vehicle vehicle, float sunLight, float blockLight, float lightBrightness, float electricFactor, boolean wasRenderedPrior, float partialTicks){
		List<LightPart> vehicleLights = multipartLightLists.get(vehicle.multipartJSONName);
		Map<LightPart, APart> partLights = new HashMap<LightPart, APart>();
		List<LightPart> allLights = new ArrayList<LightPart>();
		allLights.addAll(vehicleLights);
		for(APart part : vehicle.getMultipartParts()){
			String partKey = part.partName + part.offset.toString();
			if(partLightLists.containsKey(partKey)){
				for(LightPart partLight : partLightLists.get(partKey)){
					allLights.add(partLight);
					partLights.put(partLight, part);
				}
			}
		}

		for(LightPart light : allLights){
			boolean lightSwitchOn = vehicle.isLightOn(light.type);
			//Fun with bit shifting!  20 bits make up the light on index here, so align to a 20 tick cycle.
			boolean lightActuallyOn = lightSwitchOn && ((light.flashBits >> vehicle.ticksExisted%20) & 1) > 0;
			//Used to make the cases of the lights full brightness.  Used when lights are brighter than the surroundings.
			boolean overrideCaseBrightness = lightBrightness > Math.max(sunLight, blockLight) && lightActuallyOn;
			
			GL11.glPushMatrix();
			//This light may be rotatable.  Check this before continuing.
			//It could rotate based on a vehicle rotation variable, or a part rotation.
			if(vehicleLights.contains(light)){
				for(RotatablePart rotatable : rotatableLists.get(vehicle.multipartJSONName)){
					if(rotatable.name.equals(light.name)){
						rotateObject(vehicle, rotatable, partialTicks);
					}
				}
			}else{
				APart part = partLights.get(light);
				GL11.glTranslated(part.offset.xCoord, part.offset.yCoord, part.offset.zCoord);
				rotatePart(part, part.getActionRotation(partialTicks), false);
			}

			if(MinecraftForgeClient.getRenderPass() != 1 && !wasRenderedPrior){
				GL11.glPushMatrix();
				if(overrideCaseBrightness){
					GL11.glDisable(GL11.GL_LIGHTING);
					minecraft.entityRenderer.disableLightmap();
				}else{
					GL11.glEnable(GL11.GL_LIGHTING);
					minecraft.entityRenderer.enableLightmap();
				}
				GL11.glDisable(GL11.GL_BLEND);
				//Cover rendering.
				if(light.renderCover){
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
				}
				
				//Light rendering.
				if(lightActuallyOn && light.renderColor){
					GL11.glDisable(GL11.GL_LIGHTING);
					GL11.glEnable(GL11.GL_BLEND);
					minecraft.getTextureManager().bindTexture(lightTexture);
					GL11.glColor4f(light.color.getRed()/255F, light.color.getGreen()/255F, light.color.getBlue()/255F, electricFactor);
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
			if(lightActuallyOn && lightBrightness > 0 && MinecraftForgeClient.getRenderPass() != 0 && !wasRenderedPrior && light.renderFlare){
				for(byte i=0; i<light.centerPoints.length; ++i){
					GL11.glPushMatrix();
					GL11.glEnable(GL11.GL_BLEND);
					GL11.glDisable(GL11.GL_LIGHTING);
					minecraft.entityRenderer.disableLightmap();
					minecraft.getTextureManager().bindTexture(lensFlareTexture);
					GL11.glColor4f(light.color.getRed()/255F, light.color.getGreen()/255F, light.color.getBlue()/255F, lightBrightness);
					GL11.glBegin(GL11.GL_TRIANGLES);
					for(byte j=0; j<6; ++j){
						Float[] vertex = light.vertices[((short) i)*6+j];
						//Add a slight translation to the light size to make the flare move off it.
						//Then apply scaling factor to make the flare larger than the light.
						GL11.glTexCoord2f(vertex[3], vertex[4]);
						GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
						GL11.glVertex3d(vertex[0]+vertex[5]*0.0002F + (vertex[0] - light.centerPoints[i].xCoord)*(2 + light.size[i]*0.25F), 
								vertex[1]+vertex[6]*0.0002F + (vertex[1] - light.centerPoints[i].yCoord)*(2 + light.size[i]*0.25F), 
								vertex[2]+vertex[7]*0.0002F + (vertex[2] - light.centerPoints[i].zCoord)*(2 + light.size[i]*0.25F));	
					}
					GL11.glEnd();
					GL11.glPopMatrix();
				}
			}
			
			//Render beam if light has one.
			if(lightActuallyOn && lightBrightness > 0 && light.type.hasBeam && MinecraftForgeClient.getRenderPass() == -1){
				GL11.glPushMatrix();
		    	GL11.glDisable(GL11.GL_LIGHTING);
		    	GL11.glEnable(GL11.GL_BLEND);
		    	minecraft.entityRenderer.disableLightmap();
				minecraft.getTextureManager().bindTexture(lightBeamTexture);
		    	GL11.glColor4f(1, 1, 1, Math.min(vehicle.electricPower > 4 ? 1.0F : 0, lightBrightness/2F));
		    	//Allows making things brighter by using alpha blending.
		    	GL11.glDepthMask(false);
		    	GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
				
				//As we can have more than one light per definition, we will only render 6 vertices at a time.
				//Use the center point arrays for this; normals are the same for all 6 vertex sets so use whichever.
				for(byte i=0; i<light.centerPoints.length; ++i){
					GL11.glPushMatrix();
					GL11.glTranslated(light.centerPoints[i].xCoord - light.vertices[i*6][5]*0.15F, light.centerPoints[i].yCoord - light.vertices[i*6][6]*0.15F, light.centerPoints[i].zCoord - light.vertices[i*6][7]*0.15F);
					Vec3d endpointVec = new Vec3d(light.vertices[i*6][5]*light.size[i]*3F, light.vertices[i*6][6]*light.size[i]*3F, light.vertices[i*6][7]*light.size[i]*3F);
					//Now that we are at the starting location for the beam, rotate the matrix to get the correct direction.
					GL11.glDepthMask(false);
					for(byte j=0; j<=2; ++j){
			    		drawLightCone(endpointVec, light.size[i], false);
			    	}
					drawLightCone(endpointVec, light.size[i], true);
					GL11.glPopMatrix();
				}
		    	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		    	GL11.glDepthMask(true);
				GL11.glDisable(GL11.GL_BLEND);
				GL11.glEnable(GL11.GL_LIGHTING);
				GL11.glPopMatrix();
			}
			GL11.glPopMatrix();
		}
	}
	
    private static void drawLightCone(Vec3d endPoint, double radius, boolean reverse){
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3d(0, 0, 0);
    	if(reverse){
    		for(float theta=0; theta < 2*Math.PI + 0.1; theta += 2F*Math.PI/40F){
    			GL11.glTexCoord2f(theta, 1);
    			GL11.glVertex3d(endPoint.xCoord + radius*Math.cos(theta), endPoint.yCoord + radius*Math.sin(theta), endPoint.zCoord);
    		}
    	}else{
    		for(float theta=(float) (2*Math.PI); theta>=0 - 0.1; theta -= 2F*Math.PI/40F){
    			GL11.glTexCoord2f(theta, 1);
    			GL11.glVertex3d(endPoint.xCoord + radius*Math.cos(theta), endPoint.yCoord + radius*Math.sin(theta), endPoint.zCoord);
    		}
    	}
    	GL11.glEnd();
    }
	
	private static void renderInstrumentsAndControls(EntityMultipartE_Vehicle vehicle){
		GL11.glPushMatrix();
		for(byte i=0; i<vehicle.pack.motorized.instruments.size(); ++i){
			PackInstrument packInstrument = vehicle.pack.motorized.instruments.get(i);
			GL11.glPushMatrix();
			GL11.glTranslatef(packInstrument.pos[0], packInstrument.pos[1], packInstrument.pos[2]);
			GL11.glRotatef(packInstrument.rot[0], 1, 0, 0);
			GL11.glRotatef(packInstrument.rot[1], 0, 1, 0);
			GL11.glRotatef(packInstrument.rot[2], 0, 0, 1);
			GL11.glScalef(packInstrument.scale/16F, packInstrument.scale/16F, packInstrument.scale/16F);
			VehicleInstrument instrument = vehicle.getInstrumentInfoInSlot(i);
			if(instrument != null){
				RenderInstruments.drawInstrument(vehicle, instrument, false, packInstrument.optionalEngineNumber);
			}
			GL11.glPopMatrix();
		}
		for(byte i=0; i<vehicle.pack.motorized.controls.size(); ++i){
			PackControl packControl = vehicle.pack.motorized.controls.get(i);
			GL11.glPushMatrix();
			GL11.glTranslatef(packControl.pos[0], packControl.pos[1], packControl.pos[2]);
			GL11.glScalef(1F/16F/16F, 1F/16F/16F, 1F/16F/16F);
			for(Controls control : Controls.values()){
				if(control.name().toLowerCase().equals(packControl.controlName)){
					RenderControls.drawControl(vehicle, control, false);
				}
			}
			GL11.glPopMatrix();
		}
		GL11.glPopMatrix();
	}
	
	private static void renderBoundingBoxes(EntityMultipartD_Moving multipart){
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glColor3f(0.0F, 0.0F, 0.0F);
		GL11.glLineWidth(3.0F);
		for(MultipartAxisAlignedBB box : multipart.getCurrentCollisionBoxes()){
			//box = box.offset(-multipart.posX, -multipart.posY, -multipart.posZ);
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex3d(box.rel.xCoord - box.width/2F, box.rel.yCoord - box.height/2F, box.rel.zCoord - box.width/2F);
			GL11.glVertex3d(box.rel.xCoord + box.width/2F, box.rel.yCoord - box.height/2F, box.rel.zCoord - box.width/2F);
			GL11.glVertex3d(box.rel.xCoord - box.width/2F, box.rel.yCoord - box.height/2F, box.rel.zCoord + box.width/2F);
			GL11.glVertex3d(box.rel.xCoord + box.width/2F, box.rel.yCoord - box.height/2F, box.rel.zCoord + box.width/2F);
			GL11.glVertex3d(box.rel.xCoord - box.width/2F, box.rel.yCoord + box.height/2F, box.rel.zCoord - box.width/2F);
			GL11.glVertex3d(box.rel.xCoord + box.width/2F, box.rel.yCoord + box.height/2F, box.rel.zCoord - box.width/2F);
			GL11.glVertex3d(box.rel.xCoord - box.width/2F, box.rel.yCoord + box.height/2F, box.rel.zCoord + box.width/2F);
			GL11.glVertex3d(box.rel.xCoord + box.width/2F, box.rel.yCoord + box.height/2F, box.rel.zCoord + box.width/2F);
			
			GL11.glVertex3d(box.rel.xCoord - box.width/2F, box.rel.yCoord - box.height/2F, box.rel.zCoord - box.width/2F);
			GL11.glVertex3d(box.rel.xCoord - box.width/2F, box.rel.yCoord - box.height/2F, box.rel.zCoord + box.width/2F);
			GL11.glVertex3d(box.rel.xCoord + box.width/2F, box.rel.yCoord - box.height/2F, box.rel.zCoord - box.width/2F);
			GL11.glVertex3d(box.rel.xCoord + box.width/2F, box.rel.yCoord - box.height/2F, box.rel.zCoord + box.width/2F);
			GL11.glVertex3d(box.rel.xCoord - box.width/2F, box.rel.yCoord + box.height/2F, box.rel.zCoord - box.width/2F);
			GL11.glVertex3d(box.rel.xCoord - box.width/2F, box.rel.yCoord + box.height/2F, box.rel.zCoord + box.width/2F);
			GL11.glVertex3d(box.rel.xCoord + box.width/2F, box.rel.yCoord + box.height/2F, box.rel.zCoord - box.width/2F);
			GL11.glVertex3d(box.rel.xCoord + box.width/2F, box.rel.yCoord + box.height/2F, box.rel.zCoord + box.width/2F);
			
			GL11.glVertex3d(box.rel.xCoord - box.width/2F, box.rel.yCoord - box.height/2F, box.rel.zCoord - box.width/2F);
			GL11.glVertex3d(box.rel.xCoord - box.width/2F, box.rel.yCoord + box.height/2F, box.rel.zCoord - box.width/2F);
			GL11.glVertex3d(box.rel.xCoord + box.width/2F, box.rel.yCoord - box.height/2F, box.rel.zCoord - box.width/2F);
			GL11.glVertex3d(box.rel.xCoord + box.width/2F, box.rel.yCoord + box.height/2F, box.rel.zCoord - box.width/2F);
			GL11.glVertex3d(box.rel.xCoord - box.width/2F, box.rel.yCoord - box.height/2F, box.rel.zCoord + box.width/2F);
			GL11.glVertex3d(box.rel.xCoord - box.width/2F, box.rel.yCoord + box.height/2F, box.rel.zCoord + box.width/2F);
			GL11.glVertex3d(box.rel.xCoord + box.width/2F, box.rel.yCoord - box.height/2F, box.rel.zCoord + box.width/2F);
			GL11.glVertex3d(box.rel.xCoord + box.width/2F, box.rel.yCoord + box.height/2F, box.rel.zCoord + box.width/2F);
			GL11.glEnd();
		}
		GL11.glLineWidth(1.0F);
		GL11.glColor3f(1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glPopMatrix();
	}
	
	private static void renderPartBoxes(EntityMultipartD_Moving multipart){
		EntityPlayer player = minecraft.thePlayer;
		ItemStack heldStack = player.getHeldItemMainhand();
		if(heldStack != null){
			if(heldStack.getItem() instanceof AItemPart){
				AItemPart heldItem = (AItemPart) heldStack.getItem();
				for(Entry<Vec3d, PackPart> packPartEntry : multipart.getAllPossiblePackParts().entrySet()){
					boolean isPresent = false;
					boolean isHoldingPart = false;
					boolean isPartValid = false;
					
					if(multipart.getPartAtLocation(packPartEntry.getKey().xCoord, packPartEntry.getKey().yCoord, packPartEntry.getKey().zCoord) != null){
						isPresent = true;
					}
					
					if(packPartEntry.getValue().types.contains(PackParserSystem.getPartPack(heldItem.partName).general.type)){
						isHoldingPart = true;
						if(heldItem.isPartValidForPackDef(packPartEntry.getValue())){
							isPartValid = true;
						}
					}
							
					if(!isPresent && isHoldingPart){
						Vec3d offset = RotationSystem.getRotatedPoint(packPartEntry.getKey(), multipart.rotationPitch, multipart.rotationYaw, multipart.rotationRoll);
						AxisAlignedBB box = new AxisAlignedBB((float) (offset.xCoord) - 0.5F, (float) (offset.yCoord) - 0.5F, (float) (offset.zCoord) - 0.5F, (float) (offset.xCoord) + 0.5F, (float) (offset.yCoord) + 1.25F, (float) (offset.zCoord) + 0.5F);
						
						GL11.glPushMatrix();
						GL11.glDisable(GL11.GL_TEXTURE_2D);
						GL11.glDisable(GL11.GL_LIGHTING);
						GL11.glEnable(GL11.GL_BLEND);
						if(isPartValid){
							GL11.glColor4f(0, 1, 0, 0.25F);
						}else{
							GL11.glColor4f(1, 0, 0, 0.25F);
						}
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
	
	private static final class RotatablePart{
		private final String name;
		private final Float[][] vertices;
		
		private final Vec3d[] rotationPoints;
		private final Vec3d[] rotationAxis;
		private final float[] rotationMagnitudes;
		private final String[] rotationVariables;
		
		private RotatablePart(EntityMultipartD_Moving multipart, String name, Float[][] vertices){
			this.name = name.toLowerCase();
			this.vertices = vertices;
			this.rotationPoints = getRotationPoints(multipart, name);
			
			Vec3d rotationAxisTemp[] = getRotationAxis(multipart, name);
			this.rotationAxis = new Vec3d[rotationAxisTemp.length];
			this.rotationMagnitudes = new float[rotationAxisTemp.length];
			for(byte i=0; i<rotationAxisTemp.length; ++i){
				rotationAxis[i] = rotationAxisTemp[i].normalize();
				rotationMagnitudes[i] = (float) rotationAxisTemp[i].lengthVector();
			}
			this.rotationVariables = getRotationVariables(multipart, name);
		}
		
		private static Vec3d[] getRotationPoints(EntityMultipartD_Moving multipart, String name){
			List<Vec3d> rotationPoints = new ArrayList<Vec3d>();
			for(PackRotatableModelObject rotatable : multipart.pack.rendering.rotatableModelObjects){
				if(rotatable.partName.equals(name)){
					if(rotatable.rotationPoint != null){
						rotationPoints.add(new Vec3d(rotatable.rotationPoint[0], rotatable.rotationPoint[1], rotatable.rotationPoint[2]));
					}
				}
			}
			return rotationPoints.toArray(new Vec3d[rotationPoints.size()]);
		}
		
		private static Vec3d[] getRotationAxis(EntityMultipartD_Moving multipart, String name){
			List<Vec3d> rotationAxis = new ArrayList<Vec3d>();
			for(PackRotatableModelObject rotatable : multipart.pack.rendering.rotatableModelObjects){
				if(rotatable.partName.equals(name)){
					if(rotatable.rotationAxis != null){
						rotationAxis.add(new Vec3d(rotatable.rotationAxis[0], rotatable.rotationAxis[1], rotatable.rotationAxis[2]));
					}
				}
			}
			return rotationAxis.toArray(new Vec3d[rotationAxis.size()]);
		}
		
		private static String[] getRotationVariables(EntityMultipartD_Moving multipart, String name){
			List<String> rotationVariables = new ArrayList<String>();
			for(PackRotatableModelObject rotatable : multipart.pack.rendering.rotatableModelObjects){
				if(rotatable.partName.equals(name)){
					if(rotatable.partName != null){
						rotationVariables.add(rotatable.rotationVariable.toLowerCase());
					}
				}
			}
			return rotationVariables.toArray(new String[rotationVariables.size()]);
		}
	}
	
	private static final class WindowPart{
		private final String name;
		private final Float[][] vertices;
		
		private WindowPart(String name, Float[][] vertices){
			this.name = name.toLowerCase();
			this.vertices = vertices;
		}
	}
	
	private static final class LightPart{
		private final String name;
		private final LightTypes type;
		private final Float[][] vertices;
		private final Vec3d[] centerPoints;
		private final Float[] size;
		private final Color color;
		private final int flashBits;
		private final boolean renderFlare;
		private final boolean renderColor;
		private final boolean renderCover;
		
		private LightPart(String name, Float[][] masterVertices){
			this.name = name.toLowerCase();
			this.type = getTypeFromName(name);
			this.vertices = new Float[masterVertices.length][];
			this.centerPoints = new Vec3d[masterVertices.length/6];
			this.size = new Float[masterVertices.length/6];
			
			for(short i=0; i<centerPoints.length; ++i){
				double minX = 999;
				double maxX = -999;
				double minY = 999;
				double maxY = -999;
				double minZ = 999;
				double maxZ = -999;
				for(byte j=0; j<6; ++j){
					Float[] masterVertex = masterVertices[i*6 + j];
					minX = Math.min(masterVertex[0], minX);
					maxX = Math.max(masterVertex[0], maxX);
					minY = Math.min(masterVertex[1], minY);
					maxY = Math.max(masterVertex[1], maxY);
					minZ = Math.min(masterVertex[2], minZ);
					maxZ = Math.max(masterVertex[2], maxZ);
					
					Float[] newVertex = new Float[masterVertex.length];
					newVertex[0] = masterVertex[0];
					newVertex[1] = masterVertex[1];
					newVertex[2] = masterVertex[2];
					//Adjust UV point here to change this to glass coords.
					switch(j){
						case(0): newVertex[3] = 0.0F; newVertex[4] = 0.0F; break;
						case(1): newVertex[3] = 0.0F; newVertex[4] = 1.0F; break;
						case(2): newVertex[3] = 1.0F; newVertex[4] = 1.0F; break;
						case(3): newVertex[3] = 1.0F; newVertex[4] = 1.0F; break;
						case(4): newVertex[3] = 1.0F; newVertex[4] = 0.0F; break;
						case(5): newVertex[3] = 0.0F; newVertex[4] = 0.0F; break;
					}
					newVertex[5] = masterVertex[5];
					newVertex[6] = masterVertex[6];
					newVertex[7] = masterVertex[7];
					
					this.vertices[((short) i)*6 + j] = newVertex;
				}
				this.centerPoints[i] = new Vec3d(minX + (maxX - minX)/2D, minY + (maxY - minY)/2D, minZ + (maxZ - minZ)/2D);
				this.size[i] = (float) Math.max(Math.max(maxX - minX, maxZ - minZ), maxY - minY)*32F;
			}
			//Lights are in the format of "&NAME_XXXXXX_YYYYY_ZZZ"
			//Where NAME is what switch it goes to.
			//XXXXXX is the color.
			//YYYYY is the blink rate.
			//ZZZ is the light type.  The first bit renders the flare, the second the color, and the third the cover.
			this.color = Color.decode("0x" + name.substring(name.indexOf('_') + 1, name.indexOf('_') + 7));
			this.flashBits = Integer.decode("0x" + name.substring(name.indexOf('_', name.indexOf('_') + 7) + 1, name.lastIndexOf('_')));
			this.renderFlare = Integer.valueOf(name.substring(name.length() - 3, name.length() - 2)) > 0;
			this.renderColor = Integer.valueOf(name.substring(name.length() - 2, name.length() - 1)) > 0;
			this.renderCover = Integer.valueOf(name.substring(name.length() - 1)) > 0;
		}
		
		private LightTypes getTypeFromName(String lightName){
			for(LightTypes light : LightTypes.values()){
				if(lightName.toLowerCase().contains(light.name().toLowerCase())){
					return light;
				}
			}
			return null;
		}
	}
}
