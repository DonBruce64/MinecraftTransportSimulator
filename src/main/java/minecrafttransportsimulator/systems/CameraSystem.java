package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition.AnimationComponentType;
import minecrafttransportsimulator.jsondefs.JSONCameraObject;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;

/**System for handling camera zoom, position, and overlays.  Note that actual overlay
 * rendering is left up to the interface: this class only maintains which overlay
 * needs to be rendered, if any. 
 *
 * @author don_bruce
 */
public class CameraSystem{
    private static int zoomLevel;
	private static boolean enableCustomCameras;
	private static boolean runningCustomCameras;
	private static int customCameraIndex;
	private static float currentFOV;
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
	 *  Returns the current overlay, or null if there should be no overlay for the camera.
	 */
	public static String getOverlay(){
		return customCameraOverlay;
	}
	
	/**
	 *  Returns true if custom cameras are active, false if regular cameras are being used.
	 */
	public static boolean areCustomCamerasActive(){
		return runningCustomCameras;
	}
	
    /**
	 * Call to have the camera position and rotation set to whatever the camera system required.
	 * Passed-in position and rotation should be zero, and will be set to the desired position
	 * and rotation, or left at zero if no transforms are required.  The important thing is
	 * that after calling this method, no other camera modification operations are performed
	 * and the camera is set to the position and rotation that were passed in.  We may or may not 
	 * specify an overlay.  See {@link #getOverlay()} for the overlay we require when appropriate.  
	 * This is assured to not change until this method is next called.  If we want to use global
	 * transforms for the camera, return true.  If we only offset the camera and want to keep  its 
	 * frame of reference and use local transformations rather than global, return false.
	 */
    public static boolean adjustCamera(WrapperPlayer player, Point3d cameraPosition, Point3d cameraRotation, float partialTicks){
    	//Get variables.
		AEntityD_Interactable<?> ridingEntity = player.getEntityRiding();
		AEntityE_Multipart<?> multipart = ridingEntity instanceof AEntityE_Multipart ? (AEntityE_Multipart<?>) ridingEntity : null;
		PartSeat sittingSeat = multipart != null ? (PartSeat) multipart.getPartAtLocation(multipart.locationRiderMap.inverse().get(player)) : null;
		EntityPlayerGun playerGunEntity = EntityPlayerGun.playerClientGuns.get(player.getID());
		
    	if(InterfaceClient.inFirstPerson()){
    		//Force custom cameras for some states.
    		//If we are sneaking and holding a gun, enable custom cameras.
    		if(playerGunEntity != null && playerGunEntity.activeGun != null && sittingSeat == null){
    			enableCustomCameras = playerGunEntity.activeGun.isHandHeldGunAimed;
    			customCameraIndex = 0;
    		}
    		
    		//If our seat is set to automatically use custom cameras, enable them.
    		if(!enableCustomCameras && sittingSeat != null && sittingSeat.placementDefinition.forceCameras){
				enableCustomCameras = true;
    			customCameraIndex = 0;
    		}
    		
			//Do custom camera, or do normal rendering.
			if(enableCustomCameras){
		    	//Get cameras from entity or hand-held gun.
		    	//We check active cameras until we find one that we can use.
				runningCustomCameras = true;
		    	int camerasChecked = 0;
		    	JSONCameraObject camera = null;
		    	AEntityC_Definable<?> cameraProvider = null;
		    	
				if(multipart != null){
					if(multipart.definition.rendering.cameraObjects != null){
						for(JSONCameraObject testCamera : multipart.definition.rendering.cameraObjects){
							if(isCameraActive(testCamera, multipart, partialTicks)){
								if(camerasChecked++ == customCameraIndex){
									camera = testCamera;
									cameraProvider = multipart;
									break;
								}
							}
						}
					}
					for(APart part : multipart.parts){
						if(part.definition.rendering != null && part.definition.rendering.cameraObjects != null){
							for(JSONCameraObject testCamera : part.definition.rendering.cameraObjects){
								if(isCameraActive(testCamera, part, partialTicks)){
									if(camerasChecked++ == customCameraIndex){
										camera = testCamera;
										cameraProvider = part;
										break;
									}
								}
							}
						}
						if(camera != null){
							break;
						}
					}
				}else if(playerGunEntity != null && playerGunEntity.activeGun != null){
					if(playerGunEntity.activeGun.definition.rendering != null && playerGunEntity.activeGun.definition.rendering.cameraObjects != null){
						for(JSONCameraObject testCamera : playerGunEntity.activeGun.definition.rendering.cameraObjects){
							if(isCameraActive(testCamera, playerGunEntity, partialTicks)){
								if(camerasChecked++ == customCameraIndex){
									camera = testCamera;
									cameraProvider = playerGunEntity;
									break;
								}
							}
						}
					}
				}
				
				//If we found a camera, use it.  If not, turn off custom cameras and go back to first-person mode.
				if(camera != null){
					//Set current overlay for future calls.
					customCameraOverlay = camera.overlay != null ? camera.overlay + ".png" : null;
					
					//Apply transforms.
					//These happen in-order to ensure proper rendering sequencing.
					if(camera.animations != null){
						boolean inhibitAnimations = false;
        				for(JSONAnimationDefinition animation : camera.animations){
        					double variableValue = cameraProvider.getAnimatedVariableValue(cameraProvider.animationClocks.get(animation), partialTicks);
        					switch(animation.animationType){
	        					case TRANSLATION :{
            						if(!inhibitAnimations && variableValue != 0){
            							Point3d translationAmount = animation.axis.copy().multiply(variableValue).rotateFine(cameraRotation);
            							cameraPosition.add(translationAmount);
            						}
            						break;
            					}
	        					case ROTATION :{
	        						if(!inhibitAnimations && variableValue != 0){
            							Point3d rotationAmount = animation.axis.copy().multiply(variableValue);
            							Point3d rotationOffset = camera.pos.copy().subtract(animation.centerPoint);
            							if(!rotationOffset.isZero()){
            								cameraPosition.subtract(rotationOffset).add(rotationOffset.rotateFine(rotationAmount));
            							}
            							cameraRotation.add(rotationAmount);
            						}
	        						break;
	        					}
	        					case SCALING :{
	        						//Do nothing, cameras don't scale.
	        						break;
	        					}
	        					case VISIBILITY :{
	        						//Do nothing.  We checked this earlier.
	        						break;
	        					}
	        					case INHIBITOR :{
	        						if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
	        							inhibitAnimations = true;
	        						}
	        						break;
	        					}
	        					case ACTIVATOR :{
	        						if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
	        							inhibitAnimations = false;
	        						}
	        						break;
	        					}
        					}
        				}
        			}
					
    				//Now that the transformed camera is ready, add the camera initial offset position and rotation.
					Point3d entityAnglesDelta = cameraProvider.prevAngles.getInterpolatedPoint(cameraProvider.angles, partialTicks);
					cameraRotation.add(entityAnglesDelta);
					cameraPosition.add(camera.pos).rotateFine(entityAnglesDelta);
    				if(camera.rot != null){
    					cameraRotation.add(camera.rot);
    				}
    				
    				//Camera is positioned and rotated to match the entity.  Do OpenGL transforms to set it.
					//Get the distance from the entity's center point to the rendered player to get a 0,0,0 starting point.
        			//Need to take into account the player's eye height.  This is where the camera is, but not where the player is positioned.
					Point3d entityPositionDelta = cameraProvider.prevPosition.getInterpolatedPoint(cameraProvider.position, partialTicks);
					entityPositionDelta.subtract(player.getRenderedPosition(partialTicks).add(0, player.getEyeHeight(), 0));
					cameraPosition.add(entityPositionDelta);
            		
            		//If the camera has an FOV override, apply it.
            		if(camera.fovOverride != 0){
            			if(currentFOV == 0){
            				currentFOV = InterfaceClient.getFOV();
            			}
            			InterfaceClient.setFOV(camera.fovOverride);
            		}
            		
            		//Return to prevent further modifications.
    	    		return true;
				}
			}else if(sittingSeat != null){
            	//Get yaw delta between entity and player from-180 to 180.
            	double playerYawDelta = (360 + (multipart.angles.y - player.getYaw())%360)%360;
            	if(playerYawDelta > 180){
            		playerYawDelta-=360;
            	}
            	
            	//Get the angles from -180 to 180 for use by the component system for calculating roll and pitch angles.
            	double pitchAngle = multipart.prevAngles.x + (multipart.angles.x - multipart.prevAngles.x)*partialTicks;
            	double rollAngle = multipart.prevAngles.z + (multipart.angles.z - multipart.prevAngles.z)*partialTicks;
            	while(pitchAngle > 180){pitchAngle -= 360;}
    			while(pitchAngle < -180){pitchAngle += 360;}
    			while(rollAngle > 180){rollAngle -= 360;}
    			while(rollAngle < -180){rollAngle += 360;}
            	
            	//Get the component of the pitch and roll that should be applied based on the yaw delta.
            	//This is based on where the player is looking.  If the player is looking straight forwards, then we want 100% of the
            	//pitch to be applied as pitch.  But, if they are looking to the side, then we need to apply that as roll, not pitch.
            	double rollRollComponent = Math.cos(Math.toRadians(playerYawDelta))*rollAngle;
            	double pitchRollComponent = -Math.sin(Math.toRadians(playerYawDelta))*pitchAngle;
            	cameraRotation.z = rollRollComponent + pitchRollComponent;
			}
			
			//We wern't running a custom camera.  Set running variable to false.
			enableCustomCameras = false;
			runningCustomCameras = false;
    	}else if(InterfaceClient.inThirdPerson()){
    		//If we were running a custom camera, and hit the switch key, increment our camera index.
    		//We then go back to first-person to render the proper camera.
    		//If we weren't running a custom camera, try running one.  This will become active when we
    		//go back into first-person mode.  This only has an effect if we are riding an entity.
    		if(runningCustomCameras){
    			++customCameraIndex;
    			InterfaceClient.toggleFirstPerson();
    		}else if(sittingSeat != null){
    			//Try to enable custom cameras.
    			enableCustomCameras = true;
        		customCameraIndex = 0;
    			
    			//Add the zoom offset for third-person view.  This takes hold if we don't have any custom cameras.
    			cameraPosition.set(-sittingSeat.localOffset.x, 0, zoomLevel);
    		}
        }else{
        	//Assuming inverted third-person mode.
        	//If we get here, and don't have any custom cameras, stay here.
        	//If we do have custom cameras, use them instead.
        	if(sittingSeat != null){
	        	if(multipart.definition.rendering.cameraObjects != null){
	        		InterfaceClient.toggleFirstPerson();
				}else{
					for(APart part : multipart.parts){
						if(part.definition.rendering != null && part.definition.rendering.cameraObjects != null){
							InterfaceClient.toggleFirstPerson();
							break;
						}
					}
				}

	        	//Add the zoom offset for third-person view.  This is inverted in Z-axis.
        		cameraPosition.set(-sittingSeat.localOffset.x, 0F, -zoomLevel);
        	}
		}
		customCameraOverlay = null;
		if(currentFOV != 0){
			InterfaceClient.setFOV(currentFOV);
			currentFOV = 0; 
		}
		
		return false;
    }
    
    private static boolean isCameraActive(JSONCameraObject camera, AEntityC_Definable<?> entity, float partialTicks){
		if(camera.animations != null){
			for(JSONAnimationDefinition animation : camera.animations){
				if(animation.animationType.equals(AnimationComponentType.VISIBILITY)){
					double value = entity.getAnimatedVariableValue(entity.animationClocks.get(animation), partialTicks);
					if(value < animation.clampMin || value > animation.clampMax){
						return false;
					}
				}
			}
		}
		return true;
    }
}
