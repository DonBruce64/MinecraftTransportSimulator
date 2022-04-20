package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.jsondefs.JSONCameraObject;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import net.minecraft.client.Minecraft;

/**System for handling camera zoom, position, and overlays.  Note that actual overlay
 * rendering is left up to the interface: this class only maintains which overlay
 * needs to be rendered, if any. 
 *
 * @author don_bruce
 */
public class CameraSystem{
    private static int zoomLevel;
	private static boolean enableCustomCameras;
	public static boolean runningCustomCameras;
	private static int customCameraIndex;
	private static int customCamerasChecked;
	private static float currentFOV;
	public static String customCameraOverlay;
	
	private static final Point3D cameraOffset = new Point3D();
	
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
	 * Call to have the camera position and rotation set to whatever the camera system required.
	 * Passed-in position and rotation should be zero, and will be set to the desired position
	 * and rotation, or left at zero if no transforms are required.  The important thing is
	 * that after calling this method, no other camera modification operations are performed
	 * and the camera is set to the position and rotation that were passed in.  We may or may not 
	 * specify an overlay for {@link #customCameraOverlay}.  
	 * This is assured to not change until this method is next called.  If we want to use global
	 * transforms for the camera, return true.  If we only offset the camera and want to keep its 
	 * frame of reference and use local transformations rather than global, return false.
	 */
    public static boolean adjustCamera(WrapperPlayer player, Point3D cameraAdjustedPosition, TransformationMatrix cameraOrientation, float partialTicks){
    	//Get variables.
		AEntityE_Interactable<?> ridingEntity = player.getEntityRiding();
		AEntityF_Multipart<?> multipart = ridingEntity instanceof AEntityF_Multipart ? (AEntityF_Multipart<?>) ridingEntity : null;
		PartSeat sittingSeat = multipart != null ? multipart.getSeatForRider(player) : null;
		EntityPlayerGun playerGunEntity = EntityPlayerGun.playerClientGuns.get(player.getID());
    	
    	//Reset FOV adn overlay.
    	if(!enableCustomCameras && currentFOV != 0){
			InterfaceClient.setFOV(currentFOV);
			currentFOV = 0; 
		}
		customCameraOverlay = null;
		if(!InterfaceClient.isGamePaused())System.out.println(Minecraft.getMinecraft().gameSettings.thirdPersonView);
		//Do camera operations.
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
				customCamerasChecked = 0;
		    	JSONCameraObject camera = null;
		    	AEntityD_Definable<?> cameraProvider = null;
		    	AnimationSwitchbox switchbox = null;
		    	
				if(multipart != null){
					camera = checkProviderForCameras(multipart, partialTicks);
					if(camera != null){
						cameraProvider = multipart;
						switchbox = multipart.cameraSwitchboxes.get(camera);
					}
					if(camera == null){
						for(APart part : multipart.parts){
							camera = checkProviderForCameras(part, partialTicks);
							if(camera != null){
								cameraProvider = part;
								switchbox = part.cameraSwitchboxes.get(camera);
								break;
							}
						}
					}
				}else if(playerGunEntity != null && playerGunEntity.activeGun != null){
					camera = checkProviderForCameras(playerGunEntity.activeGun, partialTicks);
					if(camera != null){
						cameraProvider = playerGunEntity.activeGun;
						switchbox = playerGunEntity.activeGun.cameraSwitchboxes.get(camera);
					}
				}
				
				//If we found a camera, use it.  If not, turn off custom cameras and go back to first-person mode.
				if(camera != null){
					//Set current overlay for future calls.
					customCameraOverlay = camera.overlay != null ? camera.overlay + ".png" : null;
					
					//If the camera has an FOV override, apply it.
            		if(camera.fovOverride != 0){
            			if(currentFOV == 0){
            				currentFOV = InterfaceClient.getFOV();
            			}
            			InterfaceClient.setFOV(camera.fovOverride);
            		}
            		
            		//First set the position of the camera to the defined position.
            		cameraAdjustedPosition.set(camera.pos);
            		
            		//Now run transforms on this position to get it's proper position.
            		if(switchbox != null){
	    				switchbox.runSwitchbox(partialTicks, false);
	    				cameraAdjustedPosition.transform(switchbox.netMatrix);
            		}
            		
    				//Get the rotational component of the operation.
    				//First, get the orientation of the entity we are on.
            		cameraProvider.getInterpolatedOrientation(cameraOrientation, partialTicks);
            		
            		//We need to transform the camera position by our orientation here.
            		//This puts the position into global orientation rather than animation-local.
            		cameraAdjustedPosition.transform(cameraOrientation);
            		
            		//Now add the rotation from the animation, plus the definition rotation, if we have it.
            		if(switchbox != null){
            			cameraOrientation.applyRotation(switchbox.rotation);
            		}
            		if(camera.rot != null){
    					cameraOrientation.applyRotation(camera.rot);
    				}
            		
            		//Rotational portion is good.  Finally, get the offset from the player to the provider origin.
            		//This is required as that's the camera's reference point.
            		//However, the math is in global space so just add to our offset.
            		cameraOffset.set(cameraProvider.prevPosition).interpolate(cameraProvider.position, partialTicks).subtract(player.getRenderedPosition(partialTicks));
            		cameraOffset.y -= player.getEyeHeight();
            		cameraAdjustedPosition.add(cameraOffset);
    				return true;
				}
				
				//We wern't running a custom camera.  Set running variable to false.
				enableCustomCameras = false;
				runningCustomCameras = false;
			}else if(sittingSeat != null){
				sittingSeat.getRiderInterpolatedOrientation(cameraOrientation, partialTicks);
				double eyeHeight = player.getEyeHeight();
				double seatOffset = player.getSeatOffset();
				double verticalScale = player.getVerticalScale();
				cameraAdjustedPosition.set(0, (eyeHeight + seatOffset)*verticalScale, 0).rotate(cameraOrientation).add(0, -eyeHeight, 0);
				cameraOrientation.applyRotation(player.getOrientation());
            	return true;
			}
    	}else if(InterfaceClient.inThirdPerson()){
    		//If we were running a custom camera, and hit the switch key, increment our camera index.
    		//We then go back to first-person to render the proper camera.
    		//If we weren't running a custom camera, try running one.  This will become active when we
    		//go back into first-person mode.  This only has an effect if we are riding an entity.
    		if(runningCustomCameras){
    			if(InterfaceClient.changedCameraState()){
	    			++customCameraIndex;
	    			InterfaceClient.toggleFirstPerson();
    			}
    		}else if(sittingSeat != null){
    			//Try to enable custom cameras.
    			enableCustomCameras = true;
        		customCameraIndex = 0;
    			
    			//Add the zoom offset for third-person view.  This takes hold if we don't have any custom cameras.
        		sittingSeat.getRiderInterpolatedOrientation(cameraOrientation, partialTicks);
        		double eyeHeight = player.getEyeHeight();
				double seatOffset = player.getSeatOffset();
				double verticalScale = player.getVerticalScale();
				cameraAdjustedPosition.set(0, (eyeHeight + seatOffset)*verticalScale, 0).rotate(cameraOrientation).add(0, -eyeHeight, 0);
        		cameraOffset.set(-sittingSeat.localOffset.x, 0, -zoomLevel);
            	cameraOrientation.setTranslation(cameraOffset);
        		cameraOrientation.applyRotation(player.getOrientation());
            	return true;
    		}
        }else{
        	//Assuming inverted third-person mode.
        	//If we get here, and don't have any custom cameras, stay here.
        	//If we do have custom cameras, use them instead.
        	if(sittingSeat != null){
        		if(InterfaceClient.changedCameraState()){
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
        		}
	        	
	        	//Add the zoom offset for third-person view.
	        	sittingSeat.getRiderInterpolatedOrientation(cameraOrientation, partialTicks);
	        	double eyeHeight = player.getEyeHeight();
				double seatOffset = player.getSeatOffset();
				double verticalScale = player.getVerticalScale();
				cameraAdjustedPosition.set(0, (eyeHeight + seatOffset)*verticalScale, 0).rotate(cameraOrientation).add(0, -eyeHeight, 0);
            	cameraOffset.set(-sittingSeat.localOffset.x, 0, zoomLevel);
            	cameraOrientation.setTranslation(cameraOffset);
        		cameraOrientation.applyRotation(player.getOrientation());
            	return true;
        	}
		}
		return false;
    }
    
    private static JSONCameraObject checkProviderForCameras(AEntityD_Definable<?> entity, float partialTicks){
    	if(entity.definition.rendering != null && entity.definition.rendering.cameraObjects != null){
			for(JSONCameraObject testCamera : entity.definition.rendering.cameraObjects){
				AnimationSwitchbox cameraSwitchbox = entity.cameraSwitchboxes.get(testCamera);
				if(cameraSwitchbox == null || cameraSwitchbox.runSwitchbox(partialTicks, false)){
					if(customCamerasChecked++ == customCameraIndex){
						return testCamera;
					}
				}
			}
		}
    	return null;
    }
}
