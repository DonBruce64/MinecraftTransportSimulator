package minecrafttransportsimulator.mcinterface;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Interface for handling events pertaining to entity rendering.  This modifies the player's rendered state
 * to handle them being in vehicles, as well as ensuring their model adapts to any objects they may be holding.
 * This also handles the final world rendering pass, which may render entities, and the 2D GUI rendering.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceEventsEntityRendering{
	private static final Map<EntityPlayer, ItemStack> tempHeldItemStorage = new HashMap<EntityPlayer, ItemStack>();
	private static float playerOffsetYawTemp;
	private static float playerPrevOffsetYawTemp;
	private static float playerHeadYawTemp;
	private static float playerPrevHeadYawTemp;
	protected static boolean renderCurrentRiderSitting;
	protected static boolean renderCurrentRiderControlling;
	
	/**
	 *  World last event.  This occurs at the end of rendering in a special pass of -1.
	 *  We normally don't do anything here.  The exception is if the {@link BuilderEntityRenderForwarder}
	 *  didn't get rendered.  In this case, we manually render it.  The rendering pipelines
	 *  of those methods are set up to handle this and will tread a -1 pass as a combined 0/1 pass.
	 */
    @SubscribeEvent
    public static void on(RenderWorldLastEvent event){
    	float partialTicks = event.getPartialTicks();
    	
    	//Enable lighting as pass -1 has that disabled.
    	RenderHelper.enableStandardItemLighting();
    	//TODO check if we need this.  If so, this goes into the render interface as a block.
    	//Minecraft.getMinecraft().entityRenderer.enableLightmap();
    	InterfaceRender.setLightingState(true);
    	
    	//Render pass 0 and 1 here manually.
    	for(int pass=0; pass<2; ++pass){
    		if(pass == 1){
    			InterfaceRender.setBlend(true);
    			GlStateManager.depthMask(false);
    		}
    		
    		for(Entity entity : Minecraft.getMinecraft().world.loadedEntityList){
	            if(entity instanceof BuilderEntityRenderForwarder){
	            	BuilderEntityRenderForwarder forwarder = (BuilderEntityRenderForwarder) entity;
	    			Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(forwarder).doRender(forwarder, 0, 0, 0, 0, partialTicks);
	            }
    		}
			
			if(pass == 1){
    			InterfaceRender.setBlend(false);
    			GlStateManager.depthMask(true);
    		}
    	}
		
		//Turn lighting back off.
		RenderHelper.disableStandardItemLighting();
		InterfaceRender.setLightingState(false);
    }
    
    private static int lastScreenWidth;
	private static int lastScreenHeight;
    
    /**
     * Renders an overlay GUI, or other overlay components like the fluid in a tank if we are mousing-over a vehicle.
     * Also responsible for rendering overlays on custom cameras.  If we need to render a GUI,
     * it should be returned.  Otherwise, return null.
     */
	@SubscribeEvent
    public static void on(RenderGameOverlayEvent.Pre event){
		//If we are rendering any GUI except the overlay (which is always rendering), don't render the hotbpar.
		if(event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR) && AGUIBase.activeGUIs.size() > 1){
			event.setCanceled(true);
			 return;
		}
		
		 //If we have a custom camera overlay active, don't render the crosshairs.
		 if(event.getType().equals(RenderGameOverlayEvent.ElementType.CROSSHAIRS) && CameraSystem.customCameraOverlay != null){
			 event.setCanceled(true);
			 return;
		 }
    	
    	//Do overlay rendering before the chat window is rendered.
    	//This renders them over the main hotbar, but doesn't block the chat window.
    	if(event.getType().equals(RenderGameOverlayEvent.ElementType.CHAT)){
			//Set up variables.
	    	ScaledResolution screenResolution = new ScaledResolution(Minecraft.getMinecraft());
	    	int screenWidth = screenResolution.getScaledWidth();
	    	int screenHeight = screenResolution.getScaledHeight();
	    	int mouseX = Mouse.getX() * screenWidth / Minecraft.getMinecraft().displayWidth;
	        int mouseY = screenHeight - Mouse.getY() * screenHeight / Minecraft.getMinecraft().displayHeight - 1;
	    	
	    	float partialTicks = event.getPartialTicks();
	    	boolean updateGUIs = screenWidth != lastScreenWidth || screenHeight != lastScreenHeight;
	    	if(updateGUIs){
	    		lastScreenWidth = screenWidth;
	    		lastScreenHeight = screenHeight;
	    	}
	    	
	    	//Render GUIs, re-creating their components if needed.
	    	//Set Y-axis to inverted to have correct orientation.
			GL11.glScalef(1.0F, -1.0F, 1.0F);
			
			//Enable alpha testing.  This can be disabled by mods doing bad state management during their event calls.
    		//We don't want to enable blending though, as that's on-demand.
    		//Just in case it is enabled, however, disable it.
    		//This ensures the blending state is as it will be for the main rendering pass of -1.
    		InterfaceRender.setBlend(false);
    		GL11.glEnable(GL11.GL_ALPHA_TEST);
			
			//Enable lighting.
			RenderHelper.enableStandardItemLighting();
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
			InterfaceRender.setLightingState(true);
			
			//Translate far enough to render behind the chat window.
			GL11.glPushMatrix();
			GL11.glTranslated(0, 0, -500);
			
			//Render main pass, then blended pass.
			for(AGUIBase gui : AGUIBase.activeGUIs){
	    		if(updateGUIs || gui.components.isEmpty()){
	    			gui.setupComponentsInit(screenWidth, screenHeight);
	    		}
	    		gui.render(mouseX, mouseY, false, partialTicks);
	    		GL11.glTranslated(0, 0, 250);
	    	}
			GL11.glTranslated(0, 0, -250*AGUIBase.activeGUIs.size());
			InterfaceRender.setBlend(true);
			for(AGUIBase gui : AGUIBase.activeGUIs){
				gui.render(mouseX, mouseY, true, partialTicks);
				GL11.glTranslated(0, 0, 250);
	    	}
			
			//Pop the matrix, and set lighting back to normal.
    		GL11.glPopMatrix();
			InterfaceRender.setLightingState(false);
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
			RenderHelper.disableStandardItemLighting();
			GL11.glScalef(1.0F, -1.0F, 1.0F);
    	}
    }
	
	 /**
     * Pre-post methods for adjusting player angles while seated.
     * This adjusts the player's model to move with the seat/vehicle.
     */
    @SubscribeEvent
    public static void on(RenderPlayerEvent.Pre event){
    	EntityPlayer player = event.getEntityPlayer();
    	ModelPlayer playerModel = event.getRenderer().getMainModel();
    	renderCurrentRiderSitting = false;
    	renderCurrentRiderControlling = false;
    	playerOffsetYawTemp = player.renderYawOffset;
    	playerPrevOffsetYawTemp = player.prevRenderYawOffset;
    	playerHeadYawTemp = player.rotationYawHead;
    	playerPrevHeadYawTemp = player.prevRotationYawHead;    	
    	
    	if(ConfigSystem.configObject.clientRendering.playerTweaks.value){
	    	//If we are holding a gun, disable the third-person item icon.
	    	//We can't use the setHeldItem hand as it plays the equip sound, so we use slots instead.
	    	//We also hide the right arm so it doesn't render, then render it manually at the end with our angles.
	    	EntityPlayerGun gunEntity = EntityPlayerGun.playerClientGuns.get(player.getUniqueID().toString());
	    	if(gunEntity != null && gunEntity.activeGun != null){
	    		if(gunEntity.activeGun.isHandHeldGunAimed){
	    			disableBothArms(playerModel, player, true);
	    		}else{
	    			disableRightArm(playerModel, player, true);
	    		}
	    	}
    	}
    	
    	//If we are riding an entity, adjust seating.
    	if(player.getRidingEntity() instanceof BuilderEntityExisting){
        	AEntityE_Interactable<?> ridingEntity = (AEntityE_Interactable<?>) ((BuilderEntityExisting) player.getRidingEntity()).entity;
        	float playerWidthScale = 1.0F;
        	float playerHeightScale = 1.0F;
        	GL11.glPushMatrix();
        	if(ridingEntity != null){
        		//Get total angles for the entity the player is riding.
        		Point3d entityAngles = ridingEntity.angles.copy();
        		Point3d ridingAngles = new Point3d();
	            if(ridingEntity instanceof EntityVehicleF_Physics){
	            	//Set our angles to match the seat we are riding in.
	            	for(WrapperEntity rider : ridingEntity.locationRiderMap.values()){
						if(player.equals(rider.entity)){
							PartSeat seat = (PartSeat) ((EntityVehicleF_Physics) ridingEntity).getPartAtLocation(ridingEntity.locationRiderMap.inverse().get(rider));
							ridingAngles = seat.prevAngles.getInterpolatedPoint(seat.angles, event.getPartialRenderTick()).subtract(seat.entityOn.angles);
		            		
		            		//Set sitting mode to the seat we are sitting in.
		            		//If we aren't standing, we'll need to adjust the legs.
		            		if(!seat.definition.seat.standing){
		            			if(ConfigSystem.configObject.clientRendering.playerTweaks.value){
		            				disableLegs(playerModel);
		            			}
		    	            	renderCurrentRiderSitting = true;
		            		}
		            		
		            		//Check if arms need to be set for adjustment.
		            		if(ConfigSystem.configObject.clientRendering.playerTweaks.value){
			            		if(seat.placementDefinition.isController){
			            			disableBothArms(playerModel, player, false);
			            			renderCurrentRiderControlling = true;
			            		}
		            		}
		            		
		            		//Get seat scale, if we have it.
		            		if(seat.definition.seat.widthScale != 0){
		            			playerWidthScale = seat.definition.seat.widthScale;
		            		}
		            		if(seat.placementDefinition.widthScale != 0){
		            			playerWidthScale *= seat.placementDefinition.widthScale;
		            		}
		            		if(seat.definition.seat.heightScale != 0){
		            			playerHeightScale = seat.definition.seat.heightScale; 
		            		}
		            		if(seat.placementDefinition.heightScale != 0){
		            			playerHeightScale *= seat.placementDefinition.heightScale;
		            		}
		            		break;
						}
	            	}
	            }
	            
        		//Set the player yaw offset to 0.  This forces their body to always face the front of the seat.
	            //This isn't the player's normal yaw, which is the direction they are facing.
	            player.renderYawOffset = 0;
	            player.prevRenderYawOffset = 0;
	            
	            //Set the player's head yaw to the delta between their yaw and their angled yaw.
	            //This needs to be relative as we're going to render relative to the seat here, not the world.
	            player.rotationYawHead = (float) (player.rotationYaw + entityAngles.y + ridingAngles.y);
	            player.prevRotationYawHead =  player.rotationYawHead;
	            
	            //Now add the rotations.
	            //We have to do this via OpenGL, as changing the player's pitch doesn't make them tilt in the seat, and roll doesn't exist for them.
	            //In this case, the player's eyes are their center point for rotation, but these aren't the same as 
	            //their actual position.  Means we have to do funky math.
	            //We also need to check if we are the client player or another player, as other players require a
	            //different pre-render offset to be performed to get them into the right place. 
	            if(!player.equals(Minecraft.getMinecraft().player)){
	            	EntityPlayerSP masterPlayer = Minecraft.getMinecraft().player;
	            	double playerDistanceX = player.lastTickPosX + - masterPlayer.lastTickPosX + (player.posX - player.lastTickPosX -(masterPlayer.posX - masterPlayer.lastTickPosX))*event.getPartialRenderTick();
	            	double playerDistanceY = player.lastTickPosY + - masterPlayer.lastTickPosY + (player.posY - player.lastTickPosY -(masterPlayer.posY - masterPlayer.lastTickPosY))*event.getPartialRenderTick();
	            	double playerDistanceZ = player.lastTickPosZ + - masterPlayer.lastTickPosZ + (player.posZ - player.lastTickPosZ -(masterPlayer.posZ - masterPlayer.lastTickPosZ))*event.getPartialRenderTick();
	                GL11.glTranslated(playerDistanceX, playerDistanceY, playerDistanceZ);
	                
	                GL11.glTranslated(0, player.getEyeHeight(), 0);
	                GL11.glRotated(entityAngles.y, 0, 1, 0);
	                GL11.glRotated(entityAngles.x, 1, 0, 0);
	                GL11.glRotated(entityAngles.z, 0, 0, 1);
	                if(!ridingAngles.isZero()){
		                GL11.glRotated(ridingAngles.y, 0, 1, 0);
		                GL11.glRotated(ridingAngles.x, 1, 0, 0);
		                GL11.glRotated(ridingAngles.z, 0, 0, 1);
	                }
	                GL11.glTranslated(0, -player.getEyeHeight()*playerHeightScale, 0);
	                
	                GL11.glTranslated(-playerDistanceX*playerWidthScale, -playerDistanceY*playerHeightScale, -playerDistanceZ*playerWidthScale);
	                GL11.glScalef(playerWidthScale, playerHeightScale, playerWidthScale);
	            }else{
	            	GL11.glTranslated(0, player.getEyeHeight(), 0);
	            	GL11.glRotated(entityAngles.y, 0, 1, 0);
	                GL11.glRotated(entityAngles.x, 1, 0, 0);
	                GL11.glRotated(entityAngles.z, 0, 0, 1);
	                if(!ridingAngles.isZero()){
		                GL11.glRotated(ridingAngles.y, 0, 1, 0);
		                GL11.glRotated(ridingAngles.x, 1, 0, 0);
		                GL11.glRotated(ridingAngles.z, 0, 0, 1);
	                }
	            	GL11.glTranslated(0, -player.getEyeHeight()*playerHeightScale, 0);
	            	GL11.glScalef(playerWidthScale, playerHeightScale, playerWidthScale);
	            }
        	}
        }
    }

    @SubscribeEvent
    public static void on(RenderPlayerEvent.Post event){
    	EntityPlayer player = event.getEntityPlayer();
    	ModelPlayer playerModel = event.getRenderer().getMainModel();
    	AEntityB_Existing ridingEntity = null;
    	Point3d rightArmAngles = null;
		Point3d leftArmAngles = null;
		
		//Set variables back to their previous values.
    	player.renderYawOffset = playerOffsetYawTemp;
    	player.prevRenderYawOffset = playerPrevOffsetYawTemp;
    	player.rotationYawHead = playerHeadYawTemp;
    	player.prevRotationYawHead = playerPrevHeadYawTemp;
		
		//Get riding entity.
		if(player.getRidingEntity() instanceof BuilderEntityExisting){
			ridingEntity = ((BuilderEntityExisting) player.getRidingEntity()).entity;
		}
    	
    	//Reset limb states to normal.
		resetAllLimbs(playerModel, player);
    	
    	//Do manual player model rendering.
    	if(ConfigSystem.configObject.clientRendering.playerTweaks.value){
    		
    		//If we are holding a gun, get arm angles.
    		EntityPlayerGun gunEntity = EntityPlayerGun.playerClientGuns.get(player.getUniqueID().toString());
	    	if(gunEntity != null && gunEntity.activeGun != null){	    		
	    		//Get arm rotations.
	    		Point3d heldVector;
				if(gunEntity.activeGun.isHandHeldGunAimed){
					heldVector = gunEntity.activeGun.definition.gun.handHeldAimedOffset;
				}else{
					heldVector = gunEntity.activeGun.definition.gun.handHeldNormalOffset;
				}
				double heldVectorLength = heldVector.length();
				double armPitchOffset = Math.toRadians(-90 + player.rotationPitch) - Math.asin(heldVector.y/heldVectorLength);
				double armYawOffset = -Math.atan2(heldVector.x/heldVectorLength, heldVector.z/heldVectorLength);
	    		
	    		//Set rotation points on the model.
	    		rightArmAngles = new Point3d(armPitchOffset, armYawOffset, 0);
	    		leftArmAngles = gunEntity.activeGun.isHandHeldGunAimed ? new Point3d(armPitchOffset, -armYawOffset, 0) : null;
	    	}else if(renderCurrentRiderControlling){
	    		if(ridingEntity instanceof EntityVehicleF_Physics){
    				double turningAngle = ((EntityVehicleF_Physics) ridingEntity).rudderAngle/2D;
    				rightArmAngles = new Point3d(Math.toRadians(-75 + turningAngle), Math.toRadians(-10), 0);
    				leftArmAngles = new Point3d(Math.toRadians(-75 - turningAngle), Math.toRadians(10), 0);
    	        }
	    	}
	    	
	    	//If we are sitting, adjust legs and possibly arms.
	    	//Also adjust if we have a gun.
	    	if(renderCurrentRiderSitting || rightArmAngles != null){
	    		//Push matrix to start rendering.
	    		GL11.glPushMatrix();
	    		
	    		//Need to offset the position if we're rendering another player.
	    		if(!player.equals(Minecraft.getMinecraft().player)){
	            	EntityPlayerSP masterPlayer = Minecraft.getMinecraft().player;
	            	double playerDistanceX = player.lastTickPosX + - masterPlayer.lastTickPosX + (player.posX - player.lastTickPosX -(masterPlayer.posX - masterPlayer.lastTickPosX))*event.getPartialRenderTick();
	            	double playerDistanceY = player.lastTickPosY + - masterPlayer.lastTickPosY + (player.posY - player.lastTickPosY -(masterPlayer.posY - masterPlayer.lastTickPosY))*event.getPartialRenderTick();
	            	double playerDistanceZ = player.lastTickPosZ + - masterPlayer.lastTickPosZ + (player.posZ - player.lastTickPosZ -(masterPlayer.posZ - masterPlayer.lastTickPosZ))*event.getPartialRenderTick();
	                GL11.glTranslated(playerDistanceX, playerDistanceY, playerDistanceZ);
	            }
	    		
	    		//Get model scale.
	    		float scale = event.getRenderer().prepareScale((AbstractClientPlayer) player, event.getPartialRenderTick());
	    		
	    		//Bind texture in case it's been un-bound by something.
	    		event.getRenderer().bindTexture(((AbstractClientPlayer) player).getLocationSkin());
	    		
	    		//Rotate 180 as rendering is inverted.
	    		GL11.glRotated(180, 0, 1, 0);
	    		if(renderCurrentRiderSitting){
	    			renderLegsSitting(playerModel, scale);
	    		}
	    		
	    		if(rightArmAngles != null){
	    			//Rotate to match player's facing direction if we aren't in a vehicle
	    			if(!renderCurrentRiderControlling){
		    			if(ridingEntity != null){
		    	    		GL11.glRotated(player.rotationYaw + ridingEntity.angles.y, 0, 1, 0);
		    	        }else{
		    	        	GL11.glRotated(player.rotationYaw, 0, 1, 0);
		    	        }
	    			}
	    			
		    		if(player.isSneaking()){
		    			//Lower arm if sneaking.
		    			GL11.glTranslatef(0.0F, 0.2F, 0.0F);
		    		}
		    		renderArmsAtAngles(playerModel, scale, rightArmAngles, leftArmAngles);
	    		}
	    		GL11.glPopMatrix();
	    	}
    	}
    	
    	//Pop the final matrix.
    	if(player.getRidingEntity() instanceof BuilderEntityExisting){
    		GL11.glPopMatrix();
        }
    	
    	
    }
    
    /**
	 *  Hand render events.  We use these to disable rendering of the item in the player's hand
	 *  if they are holding a gun.  Not sure why there's two events, but we cancel them both!
	 */
    @SubscribeEvent
    public static void on(RenderHandEvent event){
    	EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getMinecraft().player.getUniqueID().toString());
    	if(entity != null && entity.activeGun != null){
    		event.setCanceled(true);
    	}
    }
    
    @SubscribeEvent
    public static void on(RenderSpecificHandEvent event){
    	EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getMinecraft().player.getUniqueID().toString());
    	if(entity != null && entity.activeGun != null){
    		event.setCanceled(true);
    	}
    }
    
    private static void disableRightArm(ModelPlayer playerModel, EntityPlayer player, boolean hideItem){
    	if(hideItem){
	    	tempHeldItemStorage.put(player, player.getHeldItemMainhand());
			player.inventory.mainInventory.set(player.inventory.currentItem, ItemStack.EMPTY);
    	}
    	playerModel.bipedRightArm.isHidden = true;
    	playerModel.bipedRightArmwear.isHidden = true;
    }
    
    private static void disableBothArms(ModelPlayer playerModel, EntityPlayer player, boolean hideItem){
    	if(hideItem){
	    	tempHeldItemStorage.put(player, player.getHeldItemMainhand());
			player.inventory.mainInventory.set(player.inventory.currentItem, ItemStack.EMPTY);
    	}
    	playerModel.bipedRightArm.isHidden = true;
    	playerModel.bipedRightArmwear.isHidden = true;
    	playerModel.bipedLeftArm.isHidden = true;
    	playerModel.bipedLeftArmwear.isHidden = true;
    }
    
    private static void disableLegs(ModelPlayer playerModel){
    	playerModel.bipedRightLeg.isHidden = true;
    	playerModel.bipedRightLegwear.isHidden = true;
    	playerModel.bipedLeftLeg.isHidden = true;
    	playerModel.bipedLeftLegwear.isHidden = true;
    }
    
    private static void resetAllLimbs(ModelPlayer playerModel, EntityPlayer player){
    	if(tempHeldItemStorage.containsKey(player)){
	    	player.inventory.mainInventory.set(player.inventory.currentItem, tempHeldItemStorage.get(player));
			tempHeldItemStorage.remove(player);
    	}
    	playerModel.bipedRightArm.isHidden = false;
    	playerModel.bipedRightArmwear.isHidden = false;
    	playerModel.bipedLeftArm.isHidden = false;
    	playerModel.bipedLeftArmwear.isHidden = false;
    	playerModel.bipedRightLeg.isHidden = false;
    	playerModel.bipedRightLegwear.isHidden = false;
    	playerModel.bipedLeftLeg.isHidden = false;
    	playerModel.bipedLeftLegwear.isHidden = false;
    }
    
    private static void renderArmsAtAngles(ModelPlayer playerModel, float scale, Point3d rightArmAngles, Point3d leftArmAngles){
		//Render right and left arms if we are told.
		if(rightArmAngles != null){
			ModelRenderer rightArm = playerModel.bipedRightArm;
			ModelRenderer rightArmwear = playerModel.bipedRightArmwear;
			rightArm.rotateAngleY = (float) rightArmAngles.y;
    		rightArm.rotateAngleX = (float) rightArmAngles.x;
    		rightArm.rotateAngleZ = (float) rightArmAngles.z;
    		ModelPlayer.copyModelAngles(rightArm, rightArmwear);
    		rightArm.render(scale);
    		rightArmwear.render(scale);
		}
		if(leftArmAngles != null){
			ModelRenderer leftArm = playerModel.bipedLeftArm;
			ModelRenderer leftArmwear = playerModel.bipedLeftArmwear;
			leftArm.rotateAngleY = (float) leftArmAngles.y;
			leftArm.rotateAngleX = (float) leftArmAngles.x;
			leftArm.rotateAngleZ = (float) leftArmAngles.z;
    		ModelPlayer.copyModelAngles(leftArm, leftArmwear);
    		leftArm.render(scale);
    		leftArmwear.render(scale);
		}
    }
    
    private static void renderLegsSitting(ModelPlayer playerModel, float scale){
    	ModelRenderer rightLeg = playerModel.bipedRightLeg;
		ModelRenderer rightLegwear = playerModel.bipedRightLegwear;
		ModelRenderer leftLeg = playerModel.bipedLeftLeg;
		ModelRenderer leftLegwear = playerModel.bipedLeftLegwear;
		rightLeg.rotateAngleY = 0;
		rightLeg.rotateAngleX = (float) Math.toRadians(-90);
		rightLeg.rotateAngleZ = 0;
		leftLeg.rotateAngleY = 0;
		leftLeg.rotateAngleX = (float) Math.toRadians(-90);
		leftLeg.rotateAngleZ = 0;
		ModelPlayer.copyModelAngles(rightLeg, rightLegwear);
		ModelPlayer.copyModelAngles(leftLeg, leftLegwear);
		rightLeg.render(scale);
		rightLegwear.render(scale);
		leftLeg.render(scale);
		leftLegwear.render(scale);
    }
}
