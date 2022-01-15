package minecrafttransportsimulator.systems;

import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.Orientation3d;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
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
	public static boolean runningCustomCameras;
	private static int customCameraIndex;
	private static float currentFOV;
	public static String customCameraOverlay;
	
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
    public static boolean adjustCamera(WrapperPlayer player, Point3d cameraPosition, Orientation3d cameraOrientation, float partialTicks){
    	//Get variables.
		AEntityE_Interactable<?> ridingEntity = player.getEntityRiding();
		AEntityF_Multipart<?> multipart = ridingEntity instanceof AEntityF_Multipart ? (AEntityF_Multipart<?>) ridingEntity : null;
		PartSeat sittingSeat = multipart != null ? (PartSeat) multipart.getPartAtLocation(multipart.locationRiderMap.inverse().get(player)) : null;
		EntityPlayerGun playerGunEntity = EntityPlayerGun.playerClientGuns.get(player.getID());
    	
    	//Reset FOV adn overlay.
    	if(!enableCustomCameras && currentFOV != 0){
			InterfaceClient.setFOV(currentFOV);
			currentFOV = 0; 
		}
		customCameraOverlay = null;
		
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
		    	int camerasChecked = 0;
		    	JSONCameraObject camera = null;
		    	AEntityD_Definable<?> cameraProvider = null;
		    	AnimationSwitchbox switchbox = null;
		    	
				if(multipart != null){
					if(!multipart.cameraSwitchboxes.isEmpty()){
						for(Entry<JSONCameraObject, AnimationSwitchbox> switchboxEntry : multipart.cameraSwitchboxes.entrySet()){
							JSONCameraObject testCamera = switchboxEntry.getKey();
							if(switchboxEntry.getValue().runSwitchbox(partialTicks)){
								if(camerasChecked++ == customCameraIndex){
									camera = testCamera;
									cameraProvider = multipart;
									switchbox = switchboxEntry.getValue();
									break;
								}
							}
						}
					}
					for(APart part : multipart.parts){
						if(!part.cameraSwitchboxes.isEmpty()){
							for(Entry<JSONCameraObject, AnimationSwitchbox> switchboxEntry : part.cameraSwitchboxes.entrySet()){
								JSONCameraObject testCamera = switchboxEntry.getKey();
								if(switchboxEntry.getValue().runSwitchbox(partialTicks)){
									if(camerasChecked++ == customCameraIndex){
										camera = testCamera;
										cameraProvider = part;
										switchbox = switchboxEntry.getValue();
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
					if(!playerGunEntity.activeGun.cameraSwitchboxes.isEmpty()){
						for(Entry<JSONCameraObject, AnimationSwitchbox> switchboxEntry : playerGunEntity.activeGun.cameraSwitchboxes.entrySet()){
							JSONCameraObject testCamera = switchboxEntry.getKey();
							if(switchboxEntry.getValue().runSwitchbox(partialTicks)){
								if(camerasChecked++ == customCameraIndex){
									camera = testCamera;
									cameraProvider = playerGunEntity.activeGun;
									switchbox = switchboxEntry.getValue();
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
					
					//If the camera has an FOV override, apply it.
            		if(camera.fovOverride != 0){
            			if(currentFOV == 0){
            				currentFOV = InterfaceClient.getFOV();
            			}
            			InterfaceClient.setFOV(camera.fovOverride);
            		}
					
					//Apply entity position to the camera first, as that's common.
					//Get the distance from the entity's center point to the rendered player to get a 0,0,0 starting point.
        			//Need to take into account the player's eye height.  This is where the camera is, but not where the player is positioned.
					Point3d entityPositionDelta = cameraProvider.prevPosition.getInterpolatedPoint(cameraProvider.position, partialTicks);
					entityPositionDelta.subtract(player.getRenderedPosition(partialTicks).add(0, player.getEyeHeight(), 0));
					cameraPosition.add(entityPositionDelta);
					Orientation3d entityOrientation = cameraProvider.prevOrientation.getInterpolated(cameraProvider.orientation, partialTicks);
					cameraOrientation.multiplyBy(entityOrientation);
					
					//Now that camera is aligned to entity, set it to the starting position/orientation.
					entityOrientation.addRotationToPoint(camera.pos, cameraPosition);
    				if(camera.rot != null){
    					cameraOrientation.multiplyBy(camera.rot);
    				}
					
					//Apply transforms.
					//These happen in-order to ensure proper rendering sequencing.
    				switchbox.runSwitchbox(partialTicks);
    				cameraOrientation.addRotationToPoint(switchbox.animationOffset, cameraPosition);
    				cameraOrientation.multiplyBy(switchbox.animationOrientation);
    				return true;
				}
				
				//We wern't running a custom camera.  Set running variable to false.
				enableCustomCameras = false;
				runningCustomCameras = false;
			}else if(sittingSeat != null){
            	cameraOrientation.setTo(sittingSeat.entityOn.prevOrientation.getInterpolated(sittingSeat.orientation, partialTicks));
            	cameraOrientation.multiplyBy(player.getOrientation());
            	return true;
			}
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
        		cameraOrientation.setTo(sittingSeat.entityOn.prevOrientation.getInterpolated(sittingSeat.orientation, partialTicks));
        		cameraOrientation.multiplyBy(player.getOrientation());
        		cameraPosition.set(-sittingSeat.localOffset.x, 0, -zoomLevel);
    			cameraOrientation.rotatePoint(cameraPosition);
    			return true;
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

	        	//Add the zoom offset for third-person view.
        		cameraOrientation.setTo(sittingSeat.entityOn.prevOrientation.getInterpolated(sittingSeat.orientation, partialTicks));
        		cameraOrientation.multiplyBy(player.getOrientation());
        		cameraPosition.set(-sittingSeat.localOffset.x, 0, zoomLevel);
    			cameraOrientation.rotatePoint(cameraPosition);
        		return true;
        	}
		}
		return false;
    }
}
