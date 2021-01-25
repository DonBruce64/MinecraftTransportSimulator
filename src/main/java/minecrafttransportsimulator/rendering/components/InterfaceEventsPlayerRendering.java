package minecrafttransportsimulator.rendering.components;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.BuilderEntity;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityPlayerGun;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Interface for handling events pertaining to entity rendering.  This modifies the player's rendered state
 * to handle them being in vehicles, as well as ensuring their model adapts to any objects they may be holding.
 * Note that this does NOT affect modifications to what the player sees; this only affects how the player looks
 * and what parts of him are visible and where.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceEventsPlayerRendering{
	private static final Map<EntityPlayer, ItemStack> tempHeldItemStorage = new HashMap<EntityPlayer, ItemStack>();
	public static boolean renderCurrentRiderSitting;
	public static boolean renderCurrentRiderControlling;
	
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
    	
    	if(ConfigSystem.configObject.clientRendering.playerTweaks.value){
	    	//If we are holding a gun, disable the third-person item icon.
	    	//We can't use the setHeldItem hand as it plays the equip sound, so we use slots instead.
	    	//We also hide the right arm so it doesn't render, then render it manually at the end with our angles.
	    	EntityPlayerGun gunEntity = EntityPlayerGun.playerClientGuns.get(player.getUniqueID().toString());
	    	if(gunEntity != null && gunEntity.gun != null){
	    		if(player.isSneaking()){
	    			disableBothArms(playerModel, player, true);
	    		}else{
	    			disableRightArm(playerModel, player, true);
	    		}
	    	}
    	}
    	
    	//If we are riding an entity, adjust seating.
    	if(player.getRidingEntity() instanceof BuilderEntity){
        	AEntityBase ridingEntity = ((BuilderEntity) player.getRidingEntity()).entity;
        	float playerWidthScale = 1.0F;
        	float playerHeightScale = 1.0F;
        	GL11.glPushMatrix();
        	if(ridingEntity != null){
        		//Get total angles for the entity the player is riding.
        		Point3d entityAngles = ridingEntity.angles.copy();
        		Point3d ridingAngles = new Point3d(0, 0, 0);
	            if(ridingEntity instanceof EntityVehicleF_Physics){
	            	//Set our angles to match the seat we are riding in.
	            	for(WrapperEntity rider : ridingEntity.locationRiderMap.values()){
						if(player.equals(rider.entity)){
							PartSeat seat = (PartSeat) ((EntityVehicleF_Physics) ridingEntity).getPartAtLocation(ridingEntity.locationRiderMap.inverse().get(rider));
							ridingAngles = seat.prevTotalRotation.getInterpolatedPoint(seat.totalRotation, event.getPartialRenderTick());
		            		
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
			            		if(seat.vehicleDefinition.isController){
			            			disableBothArms(playerModel, player, false);
			            			renderCurrentRiderControlling = true;
			            		}
		            		}
		            		
		            		//Get seat scale, if we have it.
		            		if(seat.definition.seat.widthScale != 0){
		            			playerWidthScale = seat.definition.seat.widthScale; 
		            		}
		            		if(seat.definition.seat.heightScale != 0){
		            			playerHeightScale = seat.definition.seat.heightScale; 
		            		}
		            		break;
						}
	            	}
	            }
	            
        		//Set the player yaw offset to 0.  This is needed as we are rotating the player manually.
	            player.renderYawOffset = 0;
	            
	            //Set the player's head yaw to the delta between their yaw and their angled yaw.
	            player.rotationYawHead = (float) (player.rotationYaw + entityAngles.y + ridingAngles.y);
	            
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
	                
	                GL11.glTranslated(-playerDistanceX, -playerDistanceY, -playerDistanceZ);
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
    	AEntityBase ridingEntity = null;
    	Point3d rightArmAngles = null;
		Point3d leftArmAngles = null;
		
		//Get riding entity.
		if(player.getRidingEntity() instanceof BuilderEntity){
			ridingEntity = ((BuilderEntity) player.getRidingEntity()).entity;
		}
    	
    	//Reset limb states to normal.
		resetAllLimbs(playerModel, player);
    	
    	//Do manual player model rendering.
    	if(ConfigSystem.configObject.clientRendering.playerTweaks.value){
    		
    		//If we are holding a gun, get arm angles.
    		EntityPlayerGun gunEntity = EntityPlayerGun.playerClientGuns.get(player.getUniqueID().toString());
	    	if(gunEntity != null && gunEntity.gun != null){	    		
	    		//Get arm rotations.
	    		Point3d heldVector;
				if(player.isSneaking()){
					heldVector = gunEntity.gun.definition.gun.handHeldAimedOffset;
				}else{
					heldVector = gunEntity.gun.definition.gun.handHeldNormalOffset;
				}
				double heldVectorLength = heldVector.length();
				double armPitchOffset = Math.toRadians(-90 + player.rotationPitch) - Math.asin(heldVector.y/heldVectorLength);
				double armYawOffset = -Math.atan2(heldVector.x/heldVectorLength, heldVector.z/heldVectorLength);
	    		
	    		//Set rotation points on the model.
	    		rightArmAngles = new Point3d(armPitchOffset, armYawOffset, 0);
	    		leftArmAngles = player.isSneaking() ? new Point3d(armPitchOffset, -armYawOffset, 0) : null;
	    	}else if(renderCurrentRiderControlling){
	    		if(ridingEntity instanceof EntityVehicleF_Physics){
    				double turningAngle = ((EntityVehicleF_Physics) ridingEntity).rudderAngle/10D/2D;
    				rightArmAngles = new Point3d(Math.toRadians(-75 + turningAngle), Math.toRadians(-10), 0);
    				leftArmAngles = new Point3d(Math.toRadians(-75 - turningAngle), Math.toRadians(10), 0);
    	        }
	    	}
	    	
	    	//If we are sitting, adjust legs and possibly arms.
	    	//Also adjust if we have a gun.
	    	if(renderCurrentRiderSitting || rightArmAngles != null){
	    		//Push matrix to start rendering.
	    		GL11.glPushMatrix();
	    		
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
    	if(player.getRidingEntity() instanceof BuilderEntity){
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
    	if(entity != null && entity.gun != null){
    		event.setCanceled(true);
    	}
    }
    
    @SubscribeEvent
    public static void on(RenderSpecificHandEvent event){
    	EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getMinecraft().player.getUniqueID().toString());
    	if(entity != null && entity.gun != null){
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
