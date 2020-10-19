package minecrafttransportsimulator.rendering.components;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.AGUIBase.TextPosition;
import minecrafttransportsimulator.guis.instances.GUIHUD;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleCameraObject;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.VehicleAnimationSystem;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;
import minecrafttransportsimulator.vehicles.parts.PartSeat;

/**This class is responsible for handling events sent from the rendering interface.  These events are used to
 * do things like render the vehicle HUD, adjust the camera to the vehicle orientation, or change the
 * camera position based on states.
 *
 * @author don_bruce
 */
public class RenderEventHandler{
	private static int zoomLevel;
	private static boolean enableCustomCameras;
	private static boolean runningCustomCameras;
	private static int customCameraIndex;
	private static String customCameraOverlay;
	
	/**
	 *  Adjusts the camera zoom, zooming in or out depending on the flag.
	 */
	public static void changeCameraZoom(boolean zoomIn){
		if(zoomIn && zoomLevel > 0){
			zoomLevel -= 2;
		}else if(!zoomIn){
			zoomLevel += 2;
		}
	}
    
    /**
     * Adjusts roll, pitch, and zoom for camera.
     * Roll and pitch only gets updated when in first-person as we use OpenGL transforms.
     * For external rotations, we just let the entity adjust the player's pitch and yaw.
     * This is because first-person view is for direct control, while third-person is for passive control.
     * If we do any overriding logic to the camera pitch and yaw, return true.
     */
    public static boolean onCameraSetup(IWrapperEntity renderEntity, float partialTicks){
		AEntityBase entity = renderEntity.getEntityRiding();
		if(entity instanceof EntityVehicleF_Physics){
			EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) entity;
			Point3d riderLocation = vehicle.locationRiderMap.inverse().get(renderEntity);
    		if(riderLocation != null){
	    		if(MasterLoader.gameInterface.inFirstPerson()){
	    			//Do custom camera, or do normal rendering.
	    			if(enableCustomCameras){
	    				runningCustomCameras = true;
	    				VehicleCameraObject camera = null;
	    				APart optionalPart = null;
	    				int camerasChecked = 0;
	    				
	    				//Get the next custom camera the vehicle has.
	    				if(vehicle.definition.rendering.cameraObjects != null){
	    					camerasChecked += vehicle.definition.rendering.cameraObjects.size();
	    					if(customCameraIndex < vehicle.definition.rendering.cameraObjects.size()){
	    						camera = vehicle.definition.rendering.cameraObjects.get(customCameraIndex);
	    					}
	    				}
	    				
	    				//If we aren't using a vehicle camera, check for part cameras.
	    				if(camera == null){
	    					for(APart part : vehicle.parts){
	    						if(part.definition.rendering != null && part.definition.rendering.cameraObjects != null){
	    							if(customCameraIndex < camerasChecked + part.definition.rendering.cameraObjects.size()){
	    								camera = part.definition.rendering.cameraObjects.get(customCameraIndex - camerasChecked);
	    								optionalPart = part;
	    								break;
	    							}else{
	    								camerasChecked += part.definition.rendering.cameraObjects.size();
	    							}
	    						}
	    					}
	    				}
	    				
	    				//If we found a camera, use it.  If not, turn off custom cameras and go back to first-person mode.
	    				if(camera != null){
	    					//Set current overlay for future calls.
	    					customCameraOverlay = camera.overlay != null ? camera.overlay + ".png" : null;
	            			
	            			//First rotate by 180 to get the forwards-facing orientation; MC does everything backwards.
	                		GL11.glRotated(180, 0, 1, 0);
	                		
	                		//Rotate to the camera's rotation, if it has one.
	                		//We also need to take into account the rotation of the part if we have a part camera.
	                		Point3d totalRotation;
	                		if(optionalPart != null){
	                			if(camera.rot != null){
	                				totalRotation = optionalPart.totalRotation.copy().add(camera.rot);
	                			}else{
	                				totalRotation = optionalPart.totalRotation;
	                			}
	                		}else{
	                			totalRotation = camera.rot;
	                		}
	            			if(totalRotation != null){
	            	    		GL11.glRotated(-camera.rot.y, 0, 1, 0);
	            	    		GL11.glRotated(-camera.rot.x, 1, 0, 0);
	            	    		GL11.glRotated(-camera.rot.z, 0, 0, 1);
	            			}
	            			
	            			//Apply any rotations from rotation animations.
	            			if(camera.animations != null){
	            				for(VehicleAnimationDefinition animation : camera.animations){
	            					double animationValue = VehicleAnimationSystem.getVariableValue(animation.variable, animation.axis.length(), animation.offset, animation.clampMin, animation.clampMax, animation.absolute, partialTicks, vehicle, optionalPart);
	            					if(animation.animationType.equals("rotation")){
	            						if(animationValue != 0){
	            							Point3d rotationAxis = animation.axis.copy().normalize();
	                						if(animationValue != 0){
	                							GL11.glTranslated(animation.centerPoint.x - camera.pos.x, animation.centerPoint.y - camera.pos.y, animation.centerPoint.z - camera.pos.z);
	                							GL11.glRotated(animationValue, -rotationAxis.x, -rotationAxis.y, -rotationAxis.z);
	                							GL11.glTranslated(-(animation.centerPoint.x - camera.pos.x), -(animation.centerPoint.y - camera.pos.y), -(animation.centerPoint.z - camera.pos.z));
	                						}
	            						}
	            					}
	            				}
	            			}
	                		
	                		//Translate to the camera's position.
	            			//Need to take into account the player's eye height.  This is where the camera is, but not where the player is positioned.
	            			//We also need to take into account the part's position, if we are using one.
	            			double playerPositionToEyeOffset = 0.87;
	            			if(optionalPart != null){
	            				GL11.glTranslated(-(optionalPart.totalOffset.x + camera.pos.x - riderLocation.x), -(optionalPart.totalOffset.y + camera.pos.y - playerPositionToEyeOffset - riderLocation.y), -(optionalPart.totalOffset.z + camera.pos.z - riderLocation.z));
	            			}else{
	            				GL11.glTranslated(-(camera.pos.x - riderLocation.x), -(camera.pos.y - playerPositionToEyeOffset - riderLocation.y), -(camera.pos.z - riderLocation.z));
	            			}
	            			
	            			//Translate again to any camera animations.
	            			if(camera.animations != null){
	            				for(VehicleAnimationDefinition animation : camera.animations){
	            					double animationValue = VehicleAnimationSystem.getVariableValue(animation.variable, animation.axis.length(), animation.offset, animation.clampMin, animation.clampMax, animation.absolute, partialTicks, vehicle, optionalPart);
	            					if(animation.animationType.equals("translation")){
	            						if(animationValue != 0){
	            							if(animation.animationType.equals("translation")){
	                    						Point3d translationAmount = animation.axis.copy().normalize().multiply(animationValue);
	                    						GL11.glTranslated(-translationAmount.x, -translationAmount.y, -translationAmount.z);
	                    					}
	            						}
	            					}
	            				}
	            			}
	                		
	            			//Now rotate to match the vehicle's angles.
	            			Point3d vehicleSmoothedRotation = vehicle.prevAngles.copy().add(vehicle.angles.copy().subtract(vehicle.prevAngles).multiply(partialTicks));
	                		GL11.glRotated(-vehicleSmoothedRotation.z, 0, 0, 1);
	                		GL11.glRotated(-vehicleSmoothedRotation.x, 1, 0, 0);
	                		GL11.glRotated(-vehicleSmoothedRotation.y, 0, 1, 0);
	            			
	            			//Return true to signal that we overrode the camera movement.
	            			return true;
	    				}else{
	    					enableCustomCameras = false;
	    					runningCustomCameras = false;
	    					customCameraOverlay = null;
	    				}
	    			}else{
		            	//Get yaw delta between entity and player from-180 to 180.
		            	double playerYawDelta = (360 + (vehicle.angles.y - renderEntity.getHeadYaw())%360)%360;
		            	if(playerYawDelta > 180){
		            		playerYawDelta-=360;
		            	}
		            	
		            	//Get the angles from -180 to 180 for use by the component system for calculating roll and pitch angles.
		            	double pitchAngle = vehicle.prevAngles.x + (vehicle.angles.x - vehicle.prevAngles.x)*partialTicks;
		            	double rollAngle = vehicle.prevAngles.z + (vehicle.angles.z - vehicle.prevAngles.z)*partialTicks;
		            	while(pitchAngle > 180){pitchAngle -= 360;}
		    			while(pitchAngle < -180){pitchAngle += 360;}
		    			while(rollAngle > 180){rollAngle -= 360;}
		    			while(rollAngle < -180){rollAngle += 360;}
		            	
		            	//Get the component of the pitch and roll that should be applied based on the yaw delta.
		            	//This is based on where the player is looking.  If the player is looking straight forwards, then we want 100% of the
		            	//pitch to be applied as pitch.  But, if they are looking to the side, then we need to apply that as roll, not pitch.
		            	double rollRollComponent = Math.cos(Math.toRadians(playerYawDelta))*rollAngle;
		            	double pitchRollComponent = -Math.sin(Math.toRadians(playerYawDelta))*pitchAngle;
		            	GL11.glRotated(rollRollComponent + pitchRollComponent, 0, 0, 1);
	    			}
	        	}else if(!MasterLoader.gameInterface.inFirstPerson()){
	        		//If we were running a custom camera, and hit the switch key, increment our camera index.
	        		//We then go back to first-person to render the proper camera.
	        		//If we weren't running a custom camera, try running one.  This will become active when we
	        		//go back into first-oerson mode.
	        		if(runningCustomCameras){
	        			++customCameraIndex;
	        			MasterLoader.gameInterface.toggleFirstPerson();
	        		}else{
	        			enableCustomCameras = true;
	            		customCameraIndex = 0;
	        		}
	        		GL11.glTranslated(-riderLocation.x, 0F, -zoomLevel);
	            }
    		}
		}
		customCameraOverlay = null;
		return false;
    }
    
    /**
     * Returns true if the HUD components such as the hotbar and cross-hairs should be disabled.
     * This should be done for custom cameras with overlays.
     */
    public static boolean disableHUDComponents(){
    	return customCameraOverlay != null;
    }
    
    /**
     * Renders the HUD on vehicles, or the fluid in a tank if we are mousing-over a vehicle.
     * Also responsible for rendering overlays on custom cameras.  If we need to render a GUI,
     * it should be returned.  Otherwise, return null.
     */
    public static AGUIBase onOverlayRender(int screenWidth, int screenHeight, float partialTicks, AGUIBase currentGUI, AEntityBase mousedOverEntity, Point3d mousedOverPoint){
    	IWrapperPlayer player = MasterLoader.gameInterface.getClientPlayer();
    	AEntityBase ridingEntity = player.getEntityRiding();
    	if(MasterLoader.gameInterface.inFirstPerson() && ridingEntity == null){
			if(mousedOverEntity instanceof EntityVehicleF_Physics){
				EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) mousedOverEntity;
				for(BoundingBox box : vehicle.interactionBoxes){
					if(box.isPointInside(mousedOverPoint)){
						APart part = vehicle.getPartAtLocation(box.localCenter);
						if(part instanceof PartInteractable){
							FluidTank tank = ((PartInteractable) part).tank;
							if(tank != null){
								String tankText = tank.getFluid().isEmpty() ? "EMPTY" : tank.getFluid().toUpperCase() + " : " + tank.getFluidLevel() + "/" + tank.getMaxLevel();
								MasterLoader.guiInterface.drawBasicText(tankText, screenWidth/2 + 4, screenHeight/2, Color.WHITE, TextPosition.LEFT_ALIGNED, 0);
								return null;
							}
						}
					}
				}
			}
		}else if(customCameraOverlay != null){
			MasterLoader.renderInterface.bindTexture(customCameraOverlay);
			MasterLoader.renderInterface.setBlendState(true, false);
			MasterLoader.guiInterface.renderSheetTexture(0, 0, screenWidth, screenHeight, 0.0F, 0.0F, 1.0F, 1.0F, 1, 1);
			MasterLoader.renderInterface.setBlendState(false, false);
    		return null;
		}else if(MasterLoader.gameInterface.inFirstPerson() ? ConfigSystem.configObject.client.renderHUD_1P.value : ConfigSystem.configObject.client.renderHUD_3P.value){
			if(ridingEntity instanceof EntityVehicleF_Physics){
				for(IWrapperEntity rider : ridingEntity.locationRiderMap.values()){
					if(rider.equals(player)){
						PartSeat seat = (PartSeat) ((EntityVehicleF_Physics) ridingEntity).getPartAtLocation(ridingEntity.locationRiderMap.inverse().get(rider));
						//If the seat is controlling a gun, render a text line for it.
						if(seat.activeGun != null && !MasterLoader.gameInterface.isChatOpen()){
							MasterLoader.guiInterface.drawBasicText("Active Gun:", screenWidth, 0, Color.WHITE, TextPosition.RIGHT_ALIGNED, 0);
							MasterLoader.guiInterface.drawBasicText(seat.activeGun.getItemName(), screenWidth, 8, Color.WHITE, TextPosition.RIGHT_ALIGNED, 0);
						}
						
						//If the seat is a controller, render the HUD.
						if(seat.vehicleDefinition.isController){
							return currentGUI != null ? currentGUI : new GUIHUD((EntityVehicleF_Physics) ridingEntity);
						}
					}
				}
			}
		}
    	
    	//No HUD rendered, return null.
    	return null;
    }
}
