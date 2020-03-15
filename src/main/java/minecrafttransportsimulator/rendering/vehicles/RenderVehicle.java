package minecrafttransportsimulator.rendering.vehicles;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.items.packs.parts.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.PackInstrument;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleDisplayText;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.systems.ClientEventSystem;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.systems.VehicleEffectsSystem.FXPart;
import minecrafttransportsimulator.systems.VehicleSoundSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartGroundDeviceTread;
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
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderWorldLastEvent;

/**Main render class for all vehicles.  Renders the vehicle, along with all parts.
 * As entities don't render above 255 well due to the new chunk visibility system, 
 * this code is called both from the regular render loop and manually from
 * {@link ClientEventSystem#on(RenderWorldLastEvent)}.
 *
 * @author don_bruce
 */
public final class RenderVehicle extends Render<EntityVehicleE_Powered>{
	private static final Minecraft minecraft = Minecraft.getMinecraft();
	
	//VEHICLE MAPS.  Maps are keyed by generic name.
	private static final Map<String, Integer> vehicleDisplayLists = new HashMap<String, Integer>();
	private static final Map<String, String> vehicleModelOverrides = new HashMap<String, String>();
	private static final Map<String, List<RenderVehicle_RotatablePart>> vehicleRotatableLists = new HashMap<String, List<RenderVehicle_RotatablePart>>();
	private static final Map<String, List<RenderVehicle_TranslatablePart>> vehicleTranslatableLists = new HashMap<String, List<RenderVehicle_TranslatablePart>>();
	private static final Map<String, List<RenderVehicle_LightPart>> vehicleLightLists = new HashMap<String, List<RenderVehicle_LightPart>>();
	private static final Map<String, List<WindowPart>> vehicleWindowLists = new HashMap<String, List<WindowPart>>();
	private static final Map<String, List<Float[]>> treadDeltas = new HashMap<String, List<Float[]>>();
	private static final Map<String, List<Double[]>> treadPoints = new HashMap<String, List<Double[]>>();
	
	//PART MAPS.  Maps are keyed by the part model location.
	private static final Map<ResourceLocation, Integer> partDisplayLists = new HashMap<ResourceLocation, Integer>();
	private static final Map<ResourceLocation, List<RenderVehicle_RotatablePart>> partRotatableLists = new HashMap<ResourceLocation, List<RenderVehicle_RotatablePart>>();
	private static final Map<ResourceLocation, List<RenderVehicle_TranslatablePart>> partTranslatableLists = new HashMap<ResourceLocation, List<RenderVehicle_TranslatablePart>>();
	private static final Map<ResourceLocation, List<RenderVehicle_LightPart>> partLightLists = new HashMap<ResourceLocation, List<RenderVehicle_LightPart>>();
	
	//COMMON MAPS.  Keyed by systemName.
	private static final Map<String, ResourceLocation> textureMap = new HashMap<String, ResourceLocation>();
	
	//Maps to check last render times for each vehicle.
	private static final Map<EntityVehicleE_Powered, Byte> lastRenderPass = new HashMap<EntityVehicleE_Powered, Byte>();
	private static final Map<EntityVehicleE_Powered, Long> lastRenderTick = new HashMap<EntityVehicleE_Powered, Long>();
	private static final Map<EntityVehicleE_Powered, Float> lastRenderPartial = new HashMap<EntityVehicleE_Powered, Float>();
	
	//Additional maps to handle shaders compatibility.
	private static boolean shadersDetected = false;
	private static final Map<EntityVehicleE_Powered, Boolean> renderedShaderShadow = new HashMap<EntityVehicleE_Powered, Boolean>();
	private static final Map<EntityVehicleE_Powered, Boolean> renderedShaderModel = new HashMap<EntityVehicleE_Powered, Boolean>();
	
	//Constants for built-in textures.
	private static final ResourceLocation vanillaGlassTexture = new ResourceLocation("minecraft", "textures/blocks/glass.png");
	
	public RenderVehicle(RenderManager renderManager){
		super(renderManager);
	}
	
	/**Used to clear out the rendering caches of the passed-in vehicle in dev mode to allow the re-loading of models.**/
	public static void clearVehicleCaches(EntityVehicleE_Powered vehicle){
		vehicleDisplayLists.remove(vehicle.definition.genericName);
		for(RenderVehicle_RotatablePart rotatable : vehicleRotatableLists.get(vehicle.definition.genericName)){
			rotatable.clearCaches();
		}
		vehicleRotatableLists.remove(vehicle.definition.genericName);
		
		for(RenderVehicle_TranslatablePart translatable : vehicleTranslatableLists.get(vehicle.definition.genericName)){
			translatable.clearCaches();
		}
		vehicleTranslatableLists.remove(vehicle.definition.genericName);
		vehicleLightLists.remove(vehicle.definition.genericName);
		vehicleWindowLists.remove(vehicle.definition.genericName);
		treadDeltas.remove(vehicle.definition.genericName);
		treadPoints.remove(vehicle.definition.genericName);
	}
	
	/**
	 * Used to inject a new model into the model map for vehicles.
	 * Allow for hotloading models outside of the normal jar locations.
	 **/
	public static void injectModel(EntityVehicleE_Powered vehicle, String modelLocation){
		vehicleModelOverrides.put(vehicle.definition.genericName, modelLocation);
	}
	
	@Override
	protected ResourceLocation getEntityTexture(EntityVehicleE_Powered entity){
		return null;
	}
	
	@Override
	public void doRender(EntityVehicleE_Powered vehicle, double x, double y, double z, float entityYaw, float partialTicks){
		boolean didRender = false;
		if(vehicle.definition != null){ 
			if(lastRenderPass.containsKey(vehicle)){
				//Did we render this tick?
				if(lastRenderTick.get(vehicle) == vehicle.world.getTotalWorldTime() && lastRenderPartial.get(vehicle) == partialTicks){
					//If we rendered last on a pass of 0 or 1 this tick, don't re-render some things.
					//This prevents double-rendering in pass 0 and pass -1 from the event system.
					if(lastRenderPass.get(vehicle) != -1 && MinecraftForgeClient.getRenderPass() == -1){
						//If we have shaders, make sure we don't call this if we really haven't rendered the model.
						if(!shadersDetected || (renderedShaderModel.containsKey(vehicle) && renderedShaderModel.get(vehicle))){
							render(vehicle, Minecraft.getMinecraft().player, partialTicks, true);
							didRender = true;
						}
					}
				}
			}
			
			//If we didn't render the vehicle previously in any ticks do so now.
			//This normally gets called when we get two consecutive renders from events in pass -1,
			//but it also can happen if shaders are present and we only render the vehicle shadow.
			if(!didRender){
				render(vehicle, Minecraft.getMinecraft().player, partialTicks, false);
			}

			//If we previously rendered on pass 0 without rendering on pass -1, it means shaders are present.
			//Set bit to detect these buggers and keep vehicles from disappearing.
			//Also init map entries to prevent nulls from hitting logic checks.
			if(!shadersDetected && lastRenderPass.containsKey(vehicle) && lastRenderPass.get(vehicle) == 1 && MinecraftForgeClient.getRenderPass() == 0){
				shadersDetected = true;
				renderedShaderShadow.put(vehicle, false);
				renderedShaderModel.put(vehicle, false);
			}
			
			//Update maps.
			lastRenderPass.put(vehicle, (byte) MinecraftForgeClient.getRenderPass());
			lastRenderTick.put(vehicle, vehicle.world.getTotalWorldTime());
			lastRenderPartial.put(vehicle, partialTicks);
			
			//If we are in pass 1, and shaders are detected, let the system know one render has been completed.
			//This will first be the shadow, and second be the model.
			if(shadersDetected){
				if(MinecraftForgeClient.getRenderPass() == 1){
					if(!renderedShaderShadow.containsKey(vehicle) || renderedShaderShadow.get(vehicle)){
						renderedShaderModel.put(vehicle, true);
					}else{
						renderedShaderShadow.put(vehicle, true);
					}
				}else if(MinecraftForgeClient.getRenderPass() == -1){
					renderedShaderShadow.put(vehicle, false);
					renderedShaderModel.put(vehicle, false);
				}
			}
		}
	}
	
	public static boolean doesVehicleHaveLight(EntityVehicleE_Powered vehicle, LightType light){
		for(RenderVehicle_LightPart lightPart : vehicleLightLists.get(vehicle.definition.genericName)){
			if(lightPart.type.equals(light)){
				return true;
			}
		}
		for(APart<? extends EntityVehicleE_Powered> part : vehicle.getVehicleParts()){
			if(partLightLists.containsKey(part.getModelLocation())){
				for(RenderVehicle_LightPart lightPart : partLightLists.get(part.getModelLocation())){
					if(lightPart.type.equals(light)){
						return true;
					}
				}
			}
		}
		return false;
	}
	
    /**
     * Checks if lights are on for this vehicle and instruments need to be lit up.
     */
	public static boolean isVehicleIlluminated(EntityVehicleE_Powered vehicle){
		return (vehicle.isLightOn(LightType.NAVIGATIONLIGHT) || vehicle.isLightOn(LightType.RUNNINGLIGHT) || vehicle.isLightOn(LightType.HEADLIGHT)) && vehicle.electricPower > 3;
	}
	
	/**
	 *  Renders the vehicle in its entirety.  Rendering happens in all three passes (0, 1, and -1), unless the vehicle wasn't rendered in
	 *  pass 0 or 1 due to it being over the world height limit.  The parameter wasRenderedPrior should be true if the vehicle was rendered
	 *  before in either of those passes, otherwise it will render again in pass -1 which will cause issues.  Some rendering routines
	 *  only run on specific passes, so see the comments on the called methods for information on what is rendered when.  Note that if we
	 *  didn't get rendered in pass 0 or 1, then all rendering will need to be done in pass -1, so methods that wish to run in pass 0 or 1
	 *  will be forced to run in pass -1 if wasRenderedPrior is false.
	 */
	private static void render(EntityVehicleE_Powered vehicle, EntityPlayer playerRendering, float partialTicks, boolean wasRenderedPrior){
		//Get position and rotation.
		Entity renderViewEntity = minecraft.getRenderViewEntity();
		double playerX = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * (double)partialTicks;
		double playerY = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * (double)partialTicks;
		double playerZ = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * (double)partialTicks;
        double thisX = vehicle.lastTickPosX + (vehicle.posX - vehicle.lastTickPosX) * (double)partialTicks;
        double thisY = vehicle.lastTickPosY + (vehicle.posY - vehicle.lastTickPosY) * (double)partialTicks;
        double thisZ = vehicle.lastTickPosZ + (vehicle.posZ - vehicle.lastTickPosZ) * (double)partialTicks;
		double rotateYaw = -vehicle.rotationYaw + (vehicle.rotationYaw - vehicle.prevRotationYaw)*(double)(1 - partialTicks);
        double rotatePitch = vehicle.rotationPitch - (vehicle.rotationPitch - vehicle.prevRotationPitch)*(double)(1 - partialTicks);
        double rotateRoll = vehicle.rotationRoll - (vehicle.rotationRoll - vehicle.prevRotationRoll)*(double)(1 - partialTicks);
       
        //Set up lighting.
        int lightVar = vehicle.getBrightnessForRender();
        minecraft.entityRenderer.enableLightmap();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightVar%65536, lightVar/65536);
        RenderHelper.enableStandardItemLighting();
        
		//Bind texture.  Adds new element to cache if needed.
		if(!textureMap.containsKey(vehicle.definition.systemName)){
			textureMap.put(vehicle.definition.systemName, new ResourceLocation(vehicle.definition.packID, "textures/vehicles/" + vehicle.definition.systemName + ".png"));
		}
		minecraft.getTextureManager().bindTexture(textureMap.get(vehicle.definition.systemName));

		//Render all the model parts except windows.
		//Those need to be rendered after the player if the player is rendered manually.
        GL11.glPushMatrix();
        GL11.glTranslated(thisX - playerX, thisY - playerY, thisZ - playerZ);
		if(MinecraftForgeClient.getRenderPass() != 1 && !wasRenderedPrior){
			GL11.glPushMatrix();
			GL11.glShadeModel(GL11.GL_SMOOTH);
	        GL11.glRotated(rotateYaw, 0, 1, 0);
	        GL11.glRotated(rotatePitch, 1, 0, 0);
	        GL11.glRotated(rotateRoll, 0, 0, 1);
			renderMainModel(vehicle, partialTicks);
			renderParts(vehicle, partialTicks);
			GL11.glEnable(GL11.GL_NORMALIZE);
			renderWindows(vehicle, partialTicks);
			GL11.glDisable(GL11.GL_NORMALIZE);
			renderTextMarkings(vehicle);
			renderInstruments(vehicle);
			GL11.glShadeModel(GL11.GL_FLAT);
			GL11.glPopMatrix();
			
			//Don't want the rotation applied for bounding boxes as they don't rotate.
			if(Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox()){
				renderBoundingBoxes(vehicle);
			}
		}
		
		//Check to see if we need to manually render riders.
		//MC culls rendering above build height depending on the direction the player is looking.
 		//Due to inconsistent culling based on view angle, this can lead to double-renders.
 		//Better than not rendering at all I suppose.
		if(MinecraftForgeClient.getRenderPass() != 1 && !wasRenderedPrior){
			for(Entity passenger : vehicle.getPassengers()){
				if(!(minecraft.player.equals(passenger) && minecraft.gameSettings.thirdPersonView == 0) && passenger.posY > passenger.world.getHeight()){
		        	 GL11.glPushMatrix();
		        	 GL11.glTranslated(passenger.posX - vehicle.posX, passenger.posY - vehicle.posY, passenger.posZ - vehicle.posZ);
		        	 Minecraft.getMinecraft().getRenderManager().renderEntityStatic(passenger, partialTicks, false);
		        	 GL11.glPopMatrix();
		         }
			}
		}
		
		//Render lights, but make sure the light list is populated here before we try to render this, as loading de-syncs can leave it null.
		if(vehicleLightLists.get(vehicle.definition.genericName) != null){
			GL11.glPushMatrix();
			GL11.glEnable(GL11.GL_NORMALIZE);
	        GL11.glRotated(rotateYaw, 0, 1, 0);
	        GL11.glRotated(rotatePitch, 1, 0, 0);
	        GL11.glRotated(rotateRoll, 0, 0, 1);
	        renderLights(vehicle, wasRenderedPrior, partialTicks);
			GL11.glDisable(GL11.GL_NORMALIZE);
			GL11.glPopMatrix();
		}
		
		//Render holograms for missing parts if applicable.
		if(MinecraftForgeClient.getRenderPass() != 0 && !wasRenderedPrior){
			renderPartBoxes(vehicle);
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
		
		//Update SFX, but only once per render cycle.
		if(MinecraftForgeClient.getRenderPass() == -1){
			VehicleSoundSystem.updateVehicleSounds(vehicle);
			if(!minecraft.isGamePaused()){
				for(APart<? extends EntityVehicleE_Powered> part : vehicle.getVehicleParts()){
					if(part instanceof FXPart){
						((FXPart) part).spawnParticles();
					}
				}
			}
		}
	}
	
	/**
	 *  Renders the main vehicle model.  The model file is determined from the general name of the JSON, which is really
	 *  just the JSON's file name.  Vehicle model is first translated to the position of the vehicle in the world,
	 *  rotated to the roll, pitch, and yaw, of the vehicle, and then all static portions are rendered.  Dynamic
	 *  animated portions like {@link RenderVehicle_RotatablePart}s, {@link RenderVehicle_TranslatablePart}s, and
	 *  {@link WindowPart}s are rendered after this with their respective transformations applied.  All renders are
	 *  cached in DisplayLists, as we only need to translate and rotate them, not apply any transforms or splits.
	 *  This should only be called in pass 0, as we don't do any alpha blending in this routine.
	 */
	private static void renderMainModel(EntityVehicleE_Powered vehicle, float partialTicks){
		GL11.glPushMatrix();
		//Normally we use the pack name, but since all displaylists
		//are the same for all models, this is more appropriate.
		if(vehicleDisplayLists.containsKey(vehicle.definition.genericName)){
			GL11.glCallList(vehicleDisplayLists.get(vehicle.definition.genericName));
			
			//The display list only renders static parts.  We need to render dynamic ones manually.
			//If this is a window, don't render it as that gets done all at once later.
			//First render all rotatable parts.  If they are also translatable, translate first.
			for(RenderVehicle_RotatablePart rotatable : vehicleRotatableLists.get(vehicle.definition.genericName)){
				if(!rotatable.name.toLowerCase().contains("window")){
					GL11.glPushMatrix();
					if(rotatable.name.contains("%")){
						for(RenderVehicle_TranslatablePart translatable : vehicleTranslatableLists.get(vehicle.definition.genericName)){
							if(translatable.name.equals(rotatable.name)){
								translatable.translate(vehicle, null, partialTicks);
								break;
							}
						}
					}
					rotatable.render(vehicle, null, partialTicks);
					GL11.glPopMatrix();
				}
			}
			
			//Now render all translatable parts that don't rotate.
			for(RenderVehicle_TranslatablePart translatable : vehicleTranslatableLists.get(vehicle.definition.genericName)){
				if(!translatable.name.toLowerCase().contains("window") && !translatable.name.contains("$")){
					GL11.glPushMatrix();
					translatable.render(vehicle, null, partialTicks);
					GL11.glPopMatrix();
				}
			}
		}else{
			List<RenderVehicle_RotatablePart> rotatableParts = new ArrayList<RenderVehicle_RotatablePart>();
			List<RenderVehicle_TranslatablePart> translatableParts = new ArrayList<RenderVehicle_TranslatablePart>();
			List<RenderVehicle_LightPart> lightParts = new ArrayList<RenderVehicle_LightPart>();
			List<WindowPart> windows = new ArrayList<WindowPart>();
			
			ResourceLocation vehicleModelLocation = new ResourceLocation(vehicle.definition.packID, "objmodels/vehicles/" + vehicle.definition.genericName + ".obj");
			Map<String, Float[][]> parsedModel;
			if(vehicleModelOverrides.containsKey(vehicle.definition.genericName)){
				parsedModel = OBJParserSystem.parseOBJModel(null, vehicleModelOverrides.get(vehicle.definition.genericName));
			}else{
				parsedModel = OBJParserSystem.parseOBJModel(vehicleModelLocation.getResourceDomain(), vehicleModelLocation.getResourcePath());
			}
			int displayListIndex = GL11.glGenLists(1);
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
				//Don't add rotatable model parts or windows to the display list.
				//Those go in separate maps, with windows going into both a rotatable and window mapping.
				//Do add lights, as they will be rendered both as part of the model and with special things.
				boolean shouldShapeBeInDL = true;
				if(entry.getKey().contains("$")){
					if(vehicle.definition.rendering.rotatableModelObjects != null){
    					rotatableParts.add(new RenderVehicle_RotatablePart(entry.getKey(), entry.getValue(), vehicleModelLocation.toString(), vehicle.definition.rendering.rotatableModelObjects));
    					shouldShapeBeInDL = false;
					}else{
						throw new NullPointerException("ERROR: Vehicle:" + vehicle.definition.packID + ":" + vehicle.definition.genericName + " has a rotatable part:" + entry.getKey() + ", but no rotatableModelObjects are present in the JSON!");
					}
				}
				if(entry.getKey().contains("%")){
					if(vehicle.definition.rendering.translatableModelObjects != null){
    					translatableParts.add(new RenderVehicle_TranslatablePart(entry.getKey(), entry.getValue(), vehicleModelLocation.toString(), vehicle.definition.rendering.translatableModelObjects));
    					shouldShapeBeInDL = false;
					}else{
						throw new NullPointerException("ERROR: Vehicle:" + vehicle.definition.packID + ":" + vehicle.definition.genericName + " has a translatable part:" + entry.getKey() + ", but no translatableModelObjects are present in the JSON!");
					}
				}
				if(entry.getKey().contains("&")){
					lightParts.add(new RenderVehicle_LightPart(entry.getKey(), entry.getValue()));
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
			vehicleDisplayLists.put(vehicle.definition.genericName, displayListIndex);
			vehicleRotatableLists.put(vehicle.definition.genericName, rotatableParts);
			vehicleTranslatableLists.put(vehicle.definition.genericName, translatableParts);
			vehicleLightLists.put(vehicle.definition.genericName, lightParts);
			vehicleWindowLists.put(vehicle.definition.genericName, windows);
		}
		GL11.glPopMatrix();
	}
	
	/**
	 *  Renders all parts on the vehicle.  Parts are first translated to their actual position, which they keep track of.
	 *  After this they are rotated via {@link #rotatePart(APart, Vec3d, boolean)}.  Finally, any parts of the part
	 *  model that are {@link RenderVehicle_RotatablePart}s or {@link RenderVehicle_TranslatablePart}s are rendered with
	 *  their rotations applied.  This makes rendering a split process.  Translate to position, rotate at position,
	 *  render static portions of part model, apply transforms to animated portions of the part model, and then
	 *  render the animated portions.  This should only be called in pass 0, as we don't do any alpha blending in this routine.
	 */
	private static void renderParts(EntityVehicleE_Powered vehicle, float partialTicks){
		for(APart<? extends EntityVehicleE_Powered> part : vehicle.getVehicleParts()){
			ResourceLocation partModelLocation = part.getModelLocation();
			if(partModelLocation == null){
				continue;
			}else if(!partDisplayLists.containsKey(partModelLocation)){
				List<RenderVehicle_RotatablePart> rotatableParts = new ArrayList<RenderVehicle_RotatablePart>();
				List<RenderVehicle_TranslatablePart> translatableParts = new ArrayList<RenderVehicle_TranslatablePart>();
    			List<RenderVehicle_LightPart> lightParts = new ArrayList<RenderVehicle_LightPart>();
				
    			Map<String, Float[][]> parsedModel = OBJParserSystem.parseOBJModel(partModelLocation.getResourceDomain(), partModelLocation.getResourcePath());
    			int displayListIndex = GL11.glGenLists(1);
    			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
    			GL11.glBegin(GL11.GL_TRIANGLES);
    			for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
    				boolean shouldShapeBeInDL = true;
    				if(entry.getKey().contains("$")){
    					if(part.definition.rendering.rotatableModelObjects != null){
	    					rotatableParts.add(new RenderVehicle_RotatablePart(entry.getKey(), entry.getValue(), partModelLocation.toString(), part.definition.rendering.rotatableModelObjects));
	    					shouldShapeBeInDL = false;
    					}else{
    						throw new NullPointerException("ERROR: Part:" + part.definition.packID + ":" + part.definition.systemName + " has a rotatable part:" + entry.getKey() + ", but no rotatableModelObjects are present in the JSON!");
    					}
    				}
    				if(entry.getKey().contains("%")){
    					if(part.definition.rendering.translatableModelObjects != null){
	    					translatableParts.add(new RenderVehicle_TranslatablePart(entry.getKey(), entry.getValue(), partModelLocation.toString(), part.definition.rendering.translatableModelObjects));
	    					shouldShapeBeInDL = false;
    					}else{
    						throw new NullPointerException("ERROR: Part:" + part.definition.packID + ":" + part.definition.systemName + " has a translatable part:" + entry.getKey() + ", but no translatableModelObjects are present in the JSON!");
    					}
    				}
    				if(entry.getKey().contains("&")){
    					lightParts.add(new RenderVehicle_LightPart(entry.getKey(), entry.getValue()));
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
    			
    			//Now finalize the maps
    			partDisplayLists.put(partModelLocation, displayListIndex);
    			partRotatableLists.put(partModelLocation, rotatableParts);
    			partTranslatableLists.put(partModelLocation, translatableParts);
    			partLightLists.put(partModelLocation, lightParts);
    		}else{
    			//If we aren't using the vehicle texture, bind the texture for this part.
    			//Otherwise, bind the vehicle texture as it may have been un-bound prior to this.
    			if(!part.definition.general.useVehicleTexture){
    				if(!textureMap.containsKey(part.definition.systemName)){
        				textureMap.put(part.definition.systemName, part.getTextureLocation());
        			}
    				minecraft.getTextureManager().bindTexture(textureMap.get(part.definition.systemName));
    			}else{
    				minecraft.getTextureManager().bindTexture(textureMap.get(vehicle.definition.systemName));
    			}
    			
    			//Get basic rotation properties and start the matrix.
    			Vec3d actionRotation = part.getActionRotation(partialTicks);
    			GL11.glPushMatrix();
    			
    			//If we are a tread, do the tread-specific render.
        		//Otherwise render like all other parts.
        		if(part instanceof PartGroundDeviceTread){
        			//We need to manually do x translation here before rotating to prevent incorrect translation.
        			GL11.glTranslated(part.offset.x, 0, 0);
        			rotatePart(part, actionRotation, true);
        			if(part.packVehicleDef.treadZPoints != null){
        				doManualTreadRender((PartGroundDeviceTread) part, partialTicks, partDisplayLists.get(partModelLocation));	
        			}else{
        				doAutomaticTreadRender((PartGroundDeviceTread) part, partialTicks, partDisplayLists.get(partModelLocation));
        			}
        		}else{
	    			//Rotate and translate the part prior to rendering the displayList.
	    			//Note that if the part's parent has a rotation, use that to transform
	    			//the translation to match that rotation.  Needed for things like
	    			//tank turrets with seats or guns.
	    			
	    			if(part.parentPart != null && !part.parentPart.getActionRotation(partialTicks).equals(Vec3d.ZERO)){
	    				//TODO play around with this to see if we need the math or we can use partPos.
	    				Vec3d parentActionRotation = part.parentPart.getActionRotation(partialTicks);
	    				Vec3d partRelativeOffset = part.offset.subtract(part.parentPart.offset);
	    				Vec3d partTranslationOffset = part.parentPart.offset.add(RotationSystem.getRotatedPoint(partRelativeOffset, (float) parentActionRotation.x, (float) parentActionRotation.y, (float) parentActionRotation.z));
	    				GL11.glTranslated(partTranslationOffset.x, partTranslationOffset.y, partTranslationOffset.z);
	    				rotatePart(part, parentActionRotation.add(actionRotation), true);
	    			}else{
	    				GL11.glTranslated(part.offset.x, part.offset.y, part.offset.z);
	    				rotatePart(part, actionRotation, true);
	    			}
	        		GL11.glCallList(partDisplayLists.get(partModelLocation));
	    			
	    			//The display list only renders static parts.  We need to render dynamic ones manually.
	    			for(RenderVehicle_RotatablePart rotatable : partRotatableLists.get(partModelLocation)){
	    				GL11.glPushMatrix();
	    				rotatable.render(vehicle, part, partialTicks);
	    				GL11.glPopMatrix();
	    			}
    			}
        		GL11.glCullFace(GL11.GL_BACK);
        		GL11.glPopMatrix();
    		}
        }
	}
	
	/**
	 *  Rotates a part on the model.  This is an actual part, not an instance of a {@link RenderVehicle_RotatablePart}.
	 *  The rotation takes into account the vehicle, static, JSON-applied rotation, as well as the dynamic
	 *  rotation returned by {@link APart#getActionRotation(float)}.  Rotation needs to be done after translation to the
	 *  part's position to avoid coordinate system conflicts. 
	 */
	private static void rotatePart(APart<? extends EntityVehicleE_Powered> part, Vec3d actionRotation, boolean cullface){
		if(part.turnsWithSteer){
			//Use custom steering rotation point if it's set in the JSON.
			if(part.packVehicleDef.steerRotationOffset == null){
				if(part.offset.z >= 0){
					GL11.glRotatef(part.vehicle.getSteerAngle(), 0, 1, 0);
				}else{
					GL11.glRotatef(-part.vehicle.getSteerAngle(), 0, 1, 0);
				}
			}else{
				Vec3d offset = new Vec3d(-part.packVehicleDef.steerRotationOffset[0], -part.packVehicleDef.steerRotationOffset[1], -part.packVehicleDef.steerRotationOffset[2]);
				Vec3d rotatedPoint = RotationSystem.getRotatedPoint(offset, 0, part.offset.z >= 0 ? -part.vehicle.getSteerAngle() : part.vehicle.getSteerAngle(), 0);
				GL11.glTranslated(part.packVehicleDef.steerRotationOffset[0] + rotatedPoint.x, part.packVehicleDef.steerRotationOffset[1] + rotatedPoint.y, part.packVehicleDef.steerRotationOffset[2] + rotatedPoint.z);
				if(part.offset.z >= 0){
					GL11.glRotatef(part.vehicle.getSteerAngle(), 0, 1, 0);
				}else{
					GL11.glRotatef(-part.vehicle.getSteerAngle(), 0, 1, 0);
				}
			}
		}
		
		if(((part.offset.x < 0 && !part.inverseMirroring) || (part.offset.x > 0 && part.inverseMirroring)) && !part.disableMirroring){
			GL11.glScalef(-1.0F, 1.0F, 1.0F);
			if(cullface){
				GL11.glCullFace(GL11.GL_FRONT);
			}
		}
		
		if(!part.partRotation.equals(Vec3d.ZERO)){
			GL11.glRotated(part.partRotation.x, 1, 0, 0);
			GL11.glRotated(part.partRotation.y, 0, 1, 0);
			GL11.glRotated(part.partRotation.z, 0, 0, 1);
		}

		if(!actionRotation.equals(Vec3d.ZERO)){
			//Need to rotate in reverse order, otherwise guns are off.
			GL11.glRotated(actionRotation.z, 0, 0, 1);
			GL11.glRotated(-actionRotation.y, 0, 1, 0);
			GL11.glRotated(actionRotation.x, 1, 0, 0);
		}
	}
	
	/**
	 *  Renders the treads using a manual system.  Points are defined by pack authors and are located in the
	 *  vehicle JSON.  This method is more cumbersome for the authors, but allows for precise path control.
	 */
	private static void doManualTreadRender(PartGroundDeviceTread treadPart, float partialTicks, int displayListIndex){
		List<Float[]> deltas = treadDeltas.get(treadPart.vehicle.definition.genericName);
		if(deltas == null){
			//First calculate the total distance the treads need to be rendered.
			float totalDistance = 0;
			float lastY = treadPart.packVehicleDef.treadYPoints[0];
			float lastZ = treadPart.packVehicleDef.treadZPoints[0];
			for(byte i=1; i<treadPart.packVehicleDef.treadYPoints.length; ++i){
				totalDistance += Math.hypot((treadPart.packVehicleDef.treadYPoints[i] - lastY), (treadPart.packVehicleDef.treadYPoints[i] - lastZ));
				lastY = treadPart.packVehicleDef.treadYPoints[i];
				lastZ = treadPart.packVehicleDef.treadZPoints[i];
			}
			
			//Now that we have the total distance, generate a set of points for the path.
			//These points should be as far apart as the spacing parameter.
			deltas = new ArrayList<Float[]>();
			final float spacing = treadPart.definition.tread.spacing;
			byte pointIndex = 0;
			float currentY = treadPart.packVehicleDef.treadYPoints[pointIndex];
			float currentZ = treadPart.packVehicleDef.treadZPoints[pointIndex];
			float nextY = treadPart.packVehicleDef.treadYPoints[pointIndex + 1];
			float nextZ = treadPart.packVehicleDef.treadZPoints[pointIndex + 1];
			float deltaYBeforeSegment = 0;
			float deltaZBeforeSegment = 0;
			float deltaBeforeSegment = 0;
			float segmentDeltaY = (nextY - currentY);
			float segmentDeltaZ = (nextZ - currentZ);
			float segmentDeltaTotal = (float) Math.hypot(segmentDeltaY, segmentDeltaZ);
			float angle = treadPart.packVehicleDef.treadAngles[pointIndex];
			float currentAngle = 0;
			
			//Keep moving along the sets of points, making another set of evenly-spaced points.
			//This set of points will be used for rendering.
			while(totalDistance > 0){
				//If we are further than the delta between points, go to the next one.
				//Set the points to the next index set and increment delta and angle.
				while(deltaBeforeSegment + segmentDeltaTotal < spacing){
					++pointIndex;
					//If we run out of points go back to the start of the point set.
					//If we are out again, exit the loop.
					if(pointIndex + 1 == treadPart.packVehicleDef.treadYPoints.length){
						currentY = treadPart.packVehicleDef.treadYPoints[pointIndex];
						currentZ = treadPart.packVehicleDef.treadZPoints[pointIndex];
						nextY = treadPart.packVehicleDef.treadYPoints[0];
						nextZ = treadPart.packVehicleDef.treadZPoints[0];
						//Ensure we rotate the angle by the correct amount for the joint.
						//It's possible that we will add a negative angle here due to going from something like 270 to 0.
						//This will cause a -270 rotation rather than the +30 we want.
						float angleToAdd = treadPart.packVehicleDef.treadAngles[0] - treadPart.packVehicleDef.treadAngles[pointIndex];
						while(angleToAdd < 0){
							angleToAdd += 360; 
						}
						angle += angleToAdd;
					}else if(pointIndex + 1 > treadPart.packVehicleDef.treadYPoints.length){
						break;
					}else{
						currentY = treadPart.packVehicleDef.treadYPoints[pointIndex];
						currentZ = treadPart.packVehicleDef.treadZPoints[pointIndex];
						nextY = treadPart.packVehicleDef.treadYPoints[pointIndex + 1];
						nextZ = treadPart.packVehicleDef.treadZPoints[pointIndex + 1];
						angle += treadPart.packVehicleDef.treadAngles[pointIndex] - treadPart.packVehicleDef.treadAngles[pointIndex - 1];
					}
					
					//Update deltas.
					deltaBeforeSegment += segmentDeltaTotal;
					deltaYBeforeSegment += segmentDeltaY;
					deltaZBeforeSegment += segmentDeltaZ;
					segmentDeltaY = nextY - currentY;
					segmentDeltaZ = nextZ - currentZ;
					segmentDeltaTotal = (float) Math.hypot(segmentDeltaY, segmentDeltaZ);
				}
				
				//If we have enough distance for a segment, make one.
				//Otherwise add the end distance and set the total to 0.
				if(deltaBeforeSegment + segmentDeltaTotal >= spacing){
					//We are now at a point where the distance between the current point and the next point
					//are greater than the inter-point distance.  Use the slope of these two points to make a delta.
					//If we have any delta before the point, make sure we take that into account when getting the new point.
					float segmentPercentage = (spacing - deltaBeforeSegment)/segmentDeltaTotal;
					float segmentY = deltaYBeforeSegment + segmentDeltaY*segmentPercentage;
					float segmentZ = deltaZBeforeSegment + segmentDeltaZ*segmentPercentage;
					
					//Normally we could add the point now, but since OpenGL rotation changes the coordinate system
					//we need to correct for that here.  Use trigonometry to rotate the segment before adding it.
					currentAngle += angle;
					float correctedZ = (float) (Math.cos(Math.toRadians(currentAngle))*segmentZ - Math.sin(Math.toRadians(currentAngle))*segmentY);
					float correctedY = (float) (Math.sin(Math.toRadians(currentAngle))*segmentZ + Math.cos(Math.toRadians(currentAngle))*segmentY);
					deltas.add(new Float[]{correctedY, correctedZ, angle});
					//Decrement distance traveled off the variables.
					totalDistance -= spacing;
					segmentDeltaTotal -= spacing;
					segmentDeltaY -= segmentDeltaY*segmentPercentage;
					segmentDeltaZ -= segmentDeltaZ*segmentPercentage;
					deltaBeforeSegment = 0;
					deltaYBeforeSegment = 0;
					deltaZBeforeSegment = 0;
					angle = 0;
				}else{
					//If we have half or more a link left, make an extra one before exiting.
					if(deltaBeforeSegment + segmentDeltaTotal > spacing/2F){
						deltas.add(deltas.get(deltas.size() - 1));
					}
					totalDistance = 0;
				}
			}
			//Add the finalized delta list to the map.
			treadDeltas.put(treadPart.vehicle.definition.genericName, deltas);
		}
		
		
		float treadMovementPercentage = (float) ((treadPart.angularPosition + treadPart.angularVelocity*partialTicks)*treadPart.getHeight()/Math.PI%treadPart.definition.tread.spacing/treadPart.definition.tread.spacing);
		GL11.glPushMatrix();
		//First translate to the initial point.
		GL11.glTranslated(0, treadPart.offset.y + treadPart.packVehicleDef.treadYPoints[0], treadPart.offset.z + treadPart.packVehicleDef.treadZPoints[0]);
		//Next use the deltas to get the amount needed to translate and rotate each link.
		for(Float[] point : deltas){
			if(point[2] != 0){
				GL11.glRotatef(point[2], 1, 0, 0);
				GL11.glTranslatef(0, point[0]*treadMovementPercentage, point[1]*treadMovementPercentage);
				GL11.glRotatef(-point[2]*(1 - treadMovementPercentage), 1, 0, 0);
				GL11.glCallList(displayListIndex);
				GL11.glRotatef(point[2]*(1 - treadMovementPercentage), 1, 0, 0);
				GL11.glTranslatef(0, point[0]*(1 - treadMovementPercentage), point[1]*( 1 - treadMovementPercentage));
			}else{
				GL11.glTranslatef(0, point[0]*treadMovementPercentage, point[1]*treadMovementPercentage);
				GL11.glCallList(displayListIndex);
				GL11.glTranslatef(0, point[0]*(1 - treadMovementPercentage), point[1]*( 1 - treadMovementPercentage));
			}
			
		}
		GL11.glPopMatrix();
	}
	
	/**
	 *  Renders the treads using an automatic calculation system.  This system is good for simple treads,
	 *  though will render oddly on complex paths.
	 */
	private static void doAutomaticTreadRender(PartGroundDeviceTread treadPart, float partialTicks, int displayListIndex){
		List<Double[]> points = treadPoints.get(treadPart.vehicle.definition.genericName);
		if(points == null){
			//If we don't have the deltas, calculate them based on the points of the rollers on the vehicle.			
			//Search through rotatable parts on the vehicle and grab the rollers.
			Map<Integer, RenderVehicle_TreadRoller> parsedRollers = new HashMap<Integer, RenderVehicle_TreadRoller>();
			for(RenderVehicle_RotatablePart rotatable : vehicleRotatableLists.get(treadPart.vehicle.definition.genericName)){
				if(rotatable.name.contains("roller")){
					parsedRollers.put(Integer.valueOf(rotatable.name.substring(rotatable.name.lastIndexOf('_') + 1)), rotatable.createTreadRoller());
				}
			}
			
			//Now that we have all the rollers, we can start calculating points.
			//First calculate the endpoints on the rollers by calling the calculation method.
			//We also transfer the rollers to an ordered array for convenience later.
			RenderVehicle_TreadRoller[] rollers = new RenderVehicle_TreadRoller[parsedRollers.size()];
			for(int i=0; i<parsedRollers.size(); ++ i){
				if(i < parsedRollers.size() - 1){
					parsedRollers.get(i).calculateEndpoints(parsedRollers.get(i + 1));
				}else{
					parsedRollers.get(i).calculateEndpoints(parsedRollers.get(0));
				}
				rollers[i] = parsedRollers.get(i);
			}
			
			//We need to ensure the endpoints are all angle-aligned.
			//It's possible to have a start angle of -181 and end angle of
			//181, which is really just 2 degress of angle (179-181).
			//To do this, we set the end angle of roller 0 and start
			//angle of roller 1 to be around 180, or downward-facing.
			//From there, we add angles to align things.
			//At the end, we should have an end angle of 540, or 180 + 360.
			rollers[0].endAngle = 180;
			for(int i=1; i<rollers.length; ++i){
				RenderVehicle_TreadRoller roller = rollers[i];
				roller.startAngle = rollers[i - 1].endAngle;
				//End angle should be 0-360 greater than start angle, or within
				//10 degrees less, as is the case for concave rollers. 
				while(roller.endAngle < roller.startAngle - 10){
					roller.endAngle += 360;
				}
				while(roller.endAngle > roller.startAngle + 360){
					roller.endAngle += 360;
				}
			}
			//Set the end angle of the last roller, or start angle of the first roller, manually.
			//Need to get it between the value of 360 + 0-180 as that's where we will connect.
			while(rollers[0].startAngle < 0){
				rollers[0].startAngle += 360;
			}
			if(rollers[0].startAngle > 180){
				rollers[0].startAngle -= 360;
			}
			rollers[0].startAngle += 360;
			rollers[rollers.length - 1].endAngle = rollers[0].startAngle;
			
			
			//Now that the endpoints are set, we can calculate the path.
			//Do this by following the start and end points at small increments.
			points = new ArrayList<Double[]>();
			double deltaDist = treadPart.definition.tread.spacing;
			double leftoverPathLength = 0;
			for(int i=0; i<rollers.length; ++i){
				RenderVehicle_TreadRoller roller = rollers[i];
				//Follow the curve of the roller from the start and end point.
				//Do this until we don't have enough roller path left to make a point.
				//If we have any remaining path from a prior operation, we
				//need to offset our first point on the roller path to account for it.
				//It can very well be that this remainder will be more than the path length
				//of the roller.  If so, we just skip the roller entirely.
				//For the first roller we need to do some special math, as the angles will be inverted
				//For start and end due to the tread making a full 360 path.				
				double rollerPathLength = 2*Math.PI*roller.radius*Math.abs(roller.endAngle - (i == 0 ? roller.startAngle - 360 : roller.startAngle))/360D;
				double currentAngle = roller.startAngle;
				
				//Add the first point here, and add more as we follow the path.
				if(i == 0){
					double yPoint = roller.yPos + roller.radius*Math.cos(Math.toRadians(currentAngle));
					double zPoint = roller.zPos + roller.radius*Math.sin(Math.toRadians(currentAngle));
					points.add(new Double[]{yPoint, zPoint, currentAngle + 180});
				}
				
				//If we have any leftover straight path, account for it here to keep spacing consistent.
				//We will need to interpolate the point that the straight path would have gone to, but
				//take our rotation angle into account.  Only do this if we have enough of a path to do so.
				//If not, we should just skip this roller as we can't put any points on it.
				if(deltaDist - leftoverPathLength < rollerPathLength){
					if(leftoverPathLength > 0){
						//Make a new point that's along a line from the last point and the start of this roller.
						//Then increment currentAngle to account for the new point made.
						//Add an angle relative to the point on the roller.
						Double[] lastPoint = points.get(points.size() - 1);
						double yPoint = roller.yPos + roller.radius*Math.cos(Math.toRadians(currentAngle));
						double zPoint = roller.zPos + roller.radius*Math.sin(Math.toRadians(currentAngle));
						double pointDist = Math.hypot(yPoint - lastPoint[0], zPoint - lastPoint[1]);
						double normalizedY = (yPoint - lastPoint[0])/pointDist;
						double normalizedZ = (zPoint - lastPoint[1])/pointDist;
						double rollerAngleSpan = 360D*((deltaDist - leftoverPathLength)/roller.circumference);
						
						points.add(new Double[]{lastPoint[0] + deltaDist*normalizedY, lastPoint[1] + deltaDist*normalizedZ, lastPoint[2] + rollerAngleSpan});
						lastPoint = points.get(points.size() - 1);						
						currentAngle += rollerAngleSpan;
						leftoverPathLength = 0;
					}
					
					while(rollerPathLength > deltaDist){
						//Go to and add the next point on the roller path.
						rollerPathLength -= deltaDist;
						currentAngle += 360D*(deltaDist/roller.circumference);
						double yPoint = roller.yPos + roller.radius*Math.cos(Math.toRadians(currentAngle));
						double zPoint = roller.zPos + roller.radius*Math.sin(Math.toRadians(currentAngle));
						points.add(new Double[]{yPoint, zPoint, currentAngle + 180});
					}
					
					//Done following roller.  Set angle to end angle.
					currentAngle = roller.endAngle;
				}
				
				//If we have any leftover roller path, account for it here to keep spacing consistent.
				//We may also have leftover straight path length if we didn't do anything on a roller.
				//If we are on the last roller, we need to get the first roller to complete the loop.
				RenderVehicle_TreadRoller nextRoller = i == rollers.length - 1 ? rollers[0] : rollers[i + 1];
				double straightPathLength = Math.hypot(nextRoller.startY - roller.endY, nextRoller.startZ - roller.endZ);
				double normalizedY = (nextRoller.startY - roller.endY)/straightPathLength;
				double normalizedZ = (nextRoller.startZ - roller.endZ)/straightPathLength;
				double currentY = roller.endY - normalizedY*(leftoverPathLength + rollerPathLength);
				double currentZ = roller.endZ - normalizedZ*(leftoverPathLength + rollerPathLength);
				straightPathLength += leftoverPathLength + rollerPathLength;
				while(straightPathLength > deltaDist){
					//Go to and add the next point on the straight path.
					straightPathLength -= deltaDist;
					currentY += normalizedY*deltaDist;
					currentZ += normalizedZ*deltaDist;
					points.add(new Double[]{currentY, currentZ, roller.endAngle + 180});
				}
				leftoverPathLength = straightPathLength;
			}
			
			//Add a final point to the list to account for the tread gap.
			//This point is in the middle of the first and last point.
			Double[] firstPoint = points.get(0);
			Double[] lastPoint = points.get(points.size() - 1);
			points.add(new Double[]{lastPoint[0] + (firstPoint[0] - lastPoint[0])/2D, lastPoint[1] + (firstPoint[1] - lastPoint[1])/2D, lastPoint[2]});
			treadPoints.put(treadPart.vehicle.definition.genericName, points);
		}
				
		//Render the treads along their points.
		//We manually set point 0 here due to the fact it's a joint between two differing angles.
		//We also need to translate to that point to start rendering as we're currently at 0,0,0.
		//For each remaining point, we only translate the delta of the point.
		float treadMovementPercentage = (float) ((treadPart.angularPosition + treadPart.angularVelocity*partialTicks)*treadPart.getHeight()/Math.PI%treadPart.definition.tread.spacing/treadPart.definition.tread.spacing);
		Double[] priorPoint = points.get(points.size() - 1);
		Double[] point = points.get(0);
		double yDelta = point[0] - priorPoint[0];
		double zDelta = point[1] - priorPoint[1];
		double angleDelta = point[2] - priorPoint[2];
		
		GL11.glPushMatrix();
		GL11.glTranslated(0, point[0] - yDelta, point[1] - zDelta);
		for(int i=0; i<points.size() - 1; ++i){
			//Update variables, except for point 0 as we've already calculated it.
			if(i != 0){
				point = points.get(i);
				yDelta = point[0] - priorPoint[0];
				zDelta = point[1] - priorPoint[1];
				angleDelta = point[2] - priorPoint[2];
			}
			
			//If our angle delta is greater than 180, we can assume that we're inverted.
			//This happens when we cross the 360 degree rotation barrier.
			if(angleDelta > 180){
				angleDelta -= 360;
			}else if(angleDelta < -180){
				angleDelta += 360;
			}
			//If there's no rotation to the point, and no delta between points, don't do rotation.
			//That's an expensive operation due to sin and cos operations.
			//Do note that the model needs to be flipped 180 on the X-axis due to all our points
			//assuming a YZ coordinate system with 0 degrees rotation being in +Y.
			//This is why 180 is added to all points cached in the operations above.
			if(priorPoint[2] != 0 || angleDelta != 0){
				//We can't use a running rotation here as we'll end up translating in the rotated
				//coordinate system.  To combat this, we translate like normal, but then push a
				//stack and rotate prior to rendering.  This keeps us from having to do another
				//rotation to get the old coordinate system back.
				GL11.glPushMatrix();
				GL11.glTranslated(0, yDelta*treadMovementPercentage, zDelta*treadMovementPercentage);
				GL11.glRotated(priorPoint[2] + angleDelta*treadMovementPercentage, 1, 0, 0);
				GL11.glCallList(displayListIndex);
				GL11.glPopMatrix();
				GL11.glTranslated(0, yDelta, zDelta);
			}else{
				//Translate to the current position of the tread based on the percent it has moved.
				//This is determined by partial ticks and actual tread position.
				//Once there, render the tread.  Then translate the remainder of the way to prepare
				//to render the next tread.
				GL11.glTranslated(0, yDelta*treadMovementPercentage, zDelta*treadMovementPercentage);
				GL11.glCallList(displayListIndex);
				GL11.glTranslated(0, yDelta*(1 - treadMovementPercentage), zDelta*(1 - treadMovementPercentage));
			}
			
			//Set prior point to current point.
			priorPoint = point;
		}
		GL11.glPopMatrix();
	}
	
	/**
	 *  Renders all windows in this vehicle.  Windows may rotate or translate like
	 *  regular rotatable or translatable parts.  This allows for placement on doors
	 *  or other animated vehicle sections.  This should only be called in pass 0, as
	 *  we don't do any alpha blending in this routine.
	 */
	private static void renderWindows(EntityVehicleE_Powered vehicle, float partialTicks){
		minecraft.getTextureManager().bindTexture(vanillaGlassTexture);
		//Iterate through all windows.
		for(byte i=0; i<vehicleWindowLists.get(vehicle.definition.genericName).size(); ++i){
			GL11.glPushMatrix();
			//This is a window or set of windows.  Like the model, it will be triangle-based.
			//However, windows may be rotatable or translatable.  Check this before continuing.
			WindowPart window = vehicleWindowLists.get(vehicle.definition.genericName).get(i);
			for(RenderVehicle_RotatablePart rotatable : vehicleRotatableLists.get(vehicle.definition.genericName)){
				if(rotatable.name.equals(window.name)){
					rotatable.rotate(vehicle, null, partialTicks);
				}
			}
			for(RenderVehicle_TranslatablePart translatable : vehicleTranslatableLists.get(vehicle.definition.genericName)){
				if(translatable.name.equals(window.name)){
					translatable.translate(vehicle, null, partialTicks);
				}
			}
			GL11.glBegin(GL11.GL_TRIANGLES);
			for(Float[] vertex : window.vertices){
				GL11.glTexCoord2f(vertex[3], vertex[4]);
				GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
				GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
			}
			GL11.glEnd();
			GL11.glPopMatrix();
		}
	}
	
	/**
	 *  Renders all text markings for this vehicle.  Text is changed via the player,
	 *  and may be lit up if configured to do so in the JSON.  This should only be called 
	 *  in pass 0, as we don't do any alpha blending in this routine.
	 */
	private static void renderTextMarkings(EntityVehicleE_Powered vehicle){
		if(vehicle.definition.rendering.textLighted && isVehicleIlluminated(vehicle)){
			GL11.glDisable(GL11.GL_LIGHTING);
			minecraft.entityRenderer.disableLightmap();
		}
		for(VehicleDisplayText text : vehicle.definition.rendering.textMarkings){
			GL11.glPushMatrix();
			GL11.glTranslatef(text.pos[0], text.pos[1], text.pos[2]);
			GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
			//First rotate 180 along the X-axis to get us rendering right-side up.
			GL11.glRotatef(180F, 1, 0, 0);
			
			//Next, apply rotations.  Only doing so if they exist.
			//Apply the Y-rotation first as it will always be used and allows for correct X-rotations.
			if(text.rot[1] != 0){
				GL11.glRotatef(-text.rot[1], 0, 1, 0);
			}
			if(text.rot[0] != 0){
				GL11.glRotatef(text.rot[0], 1, 0, 0);
			}
			if(text.rot[2] != 0){
				GL11.glRotatef(text.rot[2], 0, 0, 1);
			}
			
			//Finally, scale and render the text.
			GL11.glScalef(text.scale, text.scale, text.scale);
			RenderHelper.disableStandardItemLighting();
			minecraft.fontRenderer.drawString(vehicle.displayText, -minecraft.fontRenderer.getStringWidth(vehicle.displayText)/2, 0, Color.decode(text.color).getRGB());
			GL11.glPopMatrix();
		}
		GL11.glColor3f(1.0F, 1.0F, 1.0F);
		RenderHelper.enableStandardItemLighting();
		if(vehicle.definition.rendering.textLighted){
			GL11.glEnable(GL11.GL_LIGHTING);
			minecraft.entityRenderer.enableLightmap();
		}
	}
	
	/**
	 *  Renders all lights for this vehicle.  
	 */
	private static void renderLights(EntityVehicleE_Powered vehicle, boolean wasRenderedPrior, float partialTicks){
		//Get all the lights for the vehicle and parts and put them into one common list.
		List<RenderVehicle_LightPart> vehicleLights = vehicleLightLists.get(vehicle.definition.genericName);
		Map<Integer, APart<? extends EntityVehicleE_Powered>> lightIndexToParts = new HashMap<Integer, APart<? extends EntityVehicleE_Powered>>();
		List<RenderVehicle_LightPart> allLights = new ArrayList<RenderVehicle_LightPart>();
		allLights.addAll(vehicleLights);
		for(APart<? extends EntityVehicleE_Powered> part : vehicle.getVehicleParts()){
			if(partLightLists.containsKey(part.getModelLocation())){
				for(RenderVehicle_LightPart partLight : partLightLists.get(part.getModelLocation())){
					lightIndexToParts.put(allLights.size(), part);
					allLights.add(partLight);
				}
			}
		}
		
		//Iterate through the common light list to render all the lights.
		for(int lightIndex=0; lightIndex<allLights.size(); ++lightIndex){
			RenderVehicle_LightPart light = allLights.get(lightIndex);
			GL11.glPushMatrix();
			//This light may be rotateable.  Check this before continuing.
			//It could rotate based on a vehicle rotation variable, or a part rotation.
			if(vehicleLights.contains(light)){
				for(RenderVehicle_RotatablePart rotatable : vehicleRotatableLists.get(vehicle.definition.genericName)){
					if(rotatable.name.equals(light.name)){
						rotatable.rotate(vehicle, null, partialTicks);
					}
				}
			}else{
				APart<? extends EntityVehicleE_Powered> part = lightIndexToParts.get(lightIndex);
				GL11.glTranslated(part.offset.x, part.offset.y, part.offset.z);
				rotatePart(part, part.getActionRotation(partialTicks), false);
				for(RenderVehicle_RotatablePart rotatable : partRotatableLists.get(part.getModelLocation())){
					if(rotatable.name.equals(light.name)){
						rotatable.rotate(vehicle, part, partialTicks);
					}
				}
			}

			//Render the light.
			light.render(vehicle, wasRenderedPrior);
			GL11.glPopMatrix();
		}
	}
	
	/**
	 *  Renders all instruments on the vehicle.  Uses the instrument's render code.
	 *  We only apply the appropriate translation and rotation.
	 */
	private static void renderInstruments(EntityVehicleE_Powered vehicle){
		for(byte i=0; i<vehicle.definition.motorized.instruments.size(); ++i){
			PackInstrument packInstrument = vehicle.definition.motorized.instruments.get(i);
			GL11.glPushMatrix();
			GL11.glTranslatef(packInstrument.pos[0], packInstrument.pos[1], packInstrument.pos[2]);
			GL11.glRotatef(packInstrument.rot[0], 1, 0, 0);
			GL11.glRotatef(packInstrument.rot[1], 0, 1, 0);
			GL11.glRotatef(packInstrument.rot[2], 0, 0, 1);
			//Need to scale by -1 to get the coordinate system to behave and align to the texture-based coordinate system.
			GL11.glScalef(-packInstrument.scale/16F, -packInstrument.scale/16F, -packInstrument.scale/16F);
			if(vehicle.instruments.containsKey(i)){
				RenderInstrument.drawInstrument(vehicle.instruments.get(i), packInstrument.optionalPartNumber, vehicle);
			}
			GL11.glPopMatrix();
		}
	}
	
	/**
	 *  Renders the bounding boxes for the vehicle collision, and centers of all
	 *  parts currently on the vehicle.
	 */
	private static void renderBoundingBoxes(EntityVehicleE_Powered vehicle){
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glColor3f(0.0F, 0.0F, 0.0F);
		GL11.glLineWidth(3.0F);
		//Draw collision boxes for the vehicle.
		for(VehicleAxisAlignedBB box : vehicle.collisionBoxes){
			Vec3d boxOffset = box.pos.subtract(vehicle.posX, vehicle.posY, vehicle.posZ);
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y - box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y - box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y - box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y - box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y + box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y + box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y + box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y + box.height/2F, boxOffset.z + box.width/2F);
			
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y - box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y - box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y - box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y - box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y + box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y + box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y + box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y + box.height/2F, boxOffset.z + box.width/2F);
			
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y - box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y + box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y - box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y + box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y - box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y + box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y - box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y + box.height/2F, boxOffset.z + box.width/2F);
			GL11.glEnd();
		}
		
		//Draw part center points.
		GL11.glColor3f(0.0F, 1.0F, 0.0F);
		GL11.glBegin(GL11.GL_LINES);
		for(APart<? extends EntityVehicleE_Powered> part : vehicle.getVehicleParts()){
			GL11.glVertex3d(part.partPos.x - vehicle.posX, part.partPos.y - vehicle.posY - part.getHeight(), part.partPos.z - vehicle.posZ);
			GL11.glVertex3d(part.partPos.x - vehicle.posX, part.partPos.y - vehicle.posY + part.getHeight(), part.partPos.z - vehicle.posZ);
		}
		GL11.glEnd();
		
		//Set params back to normal.
		GL11.glLineWidth(1.0F);
		GL11.glColor3f(1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glPopMatrix();
	}
	
	/**
	 *  Renders holographic part boxes when holding parts that can go on this vehicle.  This
	 *  needs to be rendered in pass 1 to do alpha blending.
	 */
	private static void renderPartBoxes(EntityVehicleE_Powered vehicle){
		EntityPlayer player = minecraft.player;
		ItemStack heldStack = player.getHeldItemMainhand();
		if(heldStack != null){
			if(heldStack.getItem() instanceof AItemPart){
				AItemPart heldItem = (AItemPart) heldStack.getItem();
				for(Entry<Vec3d, VehiclePart> packPartEntry : vehicle.getAllPossiblePackParts().entrySet()){
					boolean isPresent = false;
					boolean isHoldingPart = false;
					boolean isPartValid = false;
					
					if(vehicle.getPartAtLocation(packPartEntry.getKey().x, packPartEntry.getKey().y, packPartEntry.getKey().z) != null){
						isPresent = true;
					}
					
					if(packPartEntry.getValue().types.contains(heldItem.definition.general.type)){
						isHoldingPart = true;
						if(heldItem.isPartValidForPackDef(packPartEntry.getValue())){
							isPartValid = true;
						}
					}
							
					if(!isPresent && isHoldingPart){
						Vec3d offset = RotationSystem.getRotatedPoint(packPartEntry.getKey(), vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll);
						//If we are a custom part, use the custom hitbox.  Otherwise use the regular one.
						AxisAlignedBB box;
						if(packPartEntry.getValue().types.contains("custom") && heldItem.definition.general.type.equals("custom")){
							box = new AxisAlignedBB((float) (offset.x) - heldItem.definition.custom.width/2F, (float) (offset.y) - heldItem.definition.custom.height/2F, (float) (offset.z) - heldItem.definition.custom.width/2F, (float) (offset.x) + heldItem.definition.custom.width/2F, (float) (offset.y) + heldItem.definition.custom.height/2F, (float) (offset.z) + heldItem.definition.custom.width/2F);		
						}else{
							box = new AxisAlignedBB((float) (offset.x) - 0.375F, (float) (offset.y) - 0.5F, (float) (offset.z) - 0.375F, (float) (offset.x) + 0.375F, (float) (offset.y) + 1.25F, (float) (offset.z) + 0.375F);
						}
						
						Minecraft.getMinecraft().entityRenderer.disableLightmap();
						GL11.glPushMatrix();
						GL11.glDisable(GL11.GL_TEXTURE_2D);
						GL11.glDisable(GL11.GL_LIGHTING);
						if(isPartValid){
							GL11.glColor4f(0, 1, 0, 0.5F);
						}else{
							GL11.glColor4f(1, 0, 0, 0.5F);
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
						GL11.glEnable(GL11.GL_LIGHTING);
						GL11.glEnable(GL11.GL_TEXTURE_2D);
						GL11.glPopMatrix();
						Minecraft.getMinecraft().entityRenderer.enableLightmap();
					}
				}
			}
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
}
