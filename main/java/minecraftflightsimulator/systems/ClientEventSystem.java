package minecraftflightsimulator.systems;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.entities.core.EntityVehicle;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.guis.GUIConfig;
import minecraftflightsimulator.guis.GUICredits;
import minecraftflightsimulator.minecrafthelpers.EntityHelper;
import minecraftflightsimulator.registry.MTSRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;

/**This class handles rendering/camera edits that need to happen when riding planes 
 * as well as some other misc things.
 * 
 * @author don_bruce
 */
public final class ClientEventSystem{
	public static ClientEventSystem instance = new ClientEventSystem();
	/**The last seat a player was in.  If null, this means the player is not in a seat.*/
	public static EntitySeat playerLastSeat = null;
	private static Minecraft minecraft = Minecraft.getMinecraft();
	
	/**
	 * Checks on world load to see if player has loaded the mod before.
	 * If not, it shows the player the credits and gives the player a flight manual.
	 */
	@SubscribeEvent
	public void on(PlayerLoggedInEvent event){
		if(ConfigSystem.getBooleanConfig("FirstRun")){
			ConfigSystem.setClientConfig("FirstRun", false);
			FMLCommonHandler.instance().showGuiScreen(new GUICredits());
			event.player.inventory.addItemStackToInventory(new ItemStack(MTSRegistry.flightManual));
		}
	}
	
	
	/**
	 * Moves children on the client.
	 * Adjusts zoom, pitch, and roll for camera.
	 * Tweaks player rendering.
	 */
	@SubscribeEvent
	public void on(TickEvent.ClientTickEvent event){
		if(minecraft.theWorld != null){
			if(event.phase.equals(Phase.END)){
				/*Minecraft skips updating children who were spawned before their parents.
				 *This forces them to update, but causes them to lag their rendering until
				 * the next tick.  It's one of the main reasons why all the child rendering
				 * is shoved into custom code.
				 */
				for(Object entity : minecraft.theWorld.getLoadedEntityList()){
					if(entity instanceof EntityParent){
						((EntityParent) entity).moveChildren();
					}
				}
				
				if(minecraft.thePlayer.ridingEntity == null){
					if(playerLastSeat != null){
						playerLastSeat = null;
						CameraSystem.setCameraZoomActive(false);
						//DEL180START
						CameraSystem.changeCameraRoll(0);
						//DEL180END
					}
				}else if(minecraft.thePlayer.ridingEntity instanceof EntitySeat){
					if(playerLastSeat == null || !playerLastSeat.equals(minecraft.thePlayer.ridingEntity)){
						CameraSystem.setCameraZoomActive(true);
						playerLastSeat = (EntitySeat) minecraft.thePlayer.ridingEntity;
					}
					//DEL180START
					if(CameraSystem.lockedView && minecraft.gameSettings.thirdPersonView == 0){
						if(playerLastSeat != null && playerLastSeat.parent != null){
							CameraSystem.changeCameraRoll(playerLastSeat.parent.rotationRoll);
						}
					}else{
						CameraSystem.changeCameraRoll(0);
					}
					//DEL180END
					if(!Minecraft.getMinecraft().ingameGUI.getChatGUI().getChatOpen()){
						ControlSystem.controlCamera();
						if(playerLastSeat.isController){
							if(playerLastSeat.parent instanceof EntityPlane){
								ControlSystem.controlPlane((EntityPlane) playerLastSeat.parent, minecraft.thePlayer);
							}
						}
					}
				}
								
				
			}else if(!minecraft.isGamePaused()){
				if(playerLastSeat != null){
					if(playerLastSeat.parent != null){
						//Increment yaw for rotation
						minecraft.thePlayer.renderYawOffset += playerLastSeat.parent.rotationYaw - playerLastSeat.parent.prevRotationYaw;
						if(CameraSystem.lockedView){
							//Adjust pitch and yaw for camera.
							minecraft.thePlayer.rotationYaw += playerLastSeat.parent.rotationYaw - playerLastSeat.parent.prevRotationYaw;
							if(playerLastSeat.parent.rotationPitch > 90 || playerLastSeat.parent.rotationPitch < -90){
								minecraft.thePlayer.rotationPitch -= playerLastSeat.parent.rotationPitch - playerLastSeat.parent.prevRotationPitch;
							}else{
								minecraft.thePlayer.rotationPitch += playerLastSeat.parent.rotationPitch - playerLastSeat.parent.prevRotationPitch;
							}
							if((playerLastSeat.parent.rotationPitch > 90 || playerLastSeat.parent.rotationPitch < -90) ^ playerLastSeat.parent.prevRotationPitch > 90 || playerLastSeat.parent.prevRotationPitch < -90){
								//rider.rotationYaw+=180;
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Resets the rendered state of all parents.  If the parent
	 * isn't rendered by the default Minecraft engine, then it will be
	 * rendered in {@link ClientEventSystem#on(RenderWorldLastEvent)}.
	 */
	@SubscribeEvent
	public void on(TickEvent.RenderTickEvent event){
		if(event.phase.equals(Phase.START) && minecraft.theWorld != null){
			for(Object obj : minecraft.theWorld.loadedEntityList){
				if(obj instanceof EntityParent){
					((EntityParent) obj).rendered = false;
				}
			}
		}
	}	
	
	/**
	 * Checks to see if any parents have not been rendered.  Used to
	 * force rendering of aircraft above the world height limit, as
	 * newer versions suppress this as part of the chunk visibility
	 * feature.
	 * Also renders exterior lights, as rendering them during regular calls
	 * results in water being invisible.
	 */
	@SubscribeEvent
	public void on(RenderWorldLastEvent event){
        RenderManager manager = RenderManager.instance;		
		for(Object obj : minecraft.theWorld.loadedEntityList){
			if(obj instanceof EntityParent){
				if(!((EntityParent) obj).rendered){
					/*INS180
					GlStateManager.depthFunc(515);
					INS180*/
					minecraft.entityRenderer.enableLightmap(0);
					RenderHelper.enableStandardItemLighting();
					((EntityParent) obj).rendered = true;
	                manager.renderEntityStatic((Entity) obj, event.partialTicks, false);
	                
	                for(EntityChild child : ((EntityParent) obj).getChildren()){
	                	Entity rider = EntityHelper.getRider(child);
	                	if(rider != null && !(minecraft.thePlayer.equals(rider) && minecraft.gameSettings.thirdPersonView == 0)){
	                		manager.renderEntityStatic(rider, event.partialTicks, false);
	                	}
	                }
	                RenderHelper.disableStandardItemLighting();
	                minecraft.entityRenderer.disableLightmap(0);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void on(RenderPlayerEvent.Pre event){
		if(event.entityPlayer.ridingEntity instanceof EntitySeat){
			EntityParent parent = ((EntitySeat) event.entityPlayer.ridingEntity).parent;
			if(parent!=null){
				GL11.glPushMatrix();
				if(!event.entityPlayer.equals(minecraft.thePlayer)){
					EntityPlayer masterPlayer = Minecraft.getMinecraft().thePlayer;
					EntityPlayer renderedPlayer = event.entityPlayer;
					float playerDistanceX = (float) (renderedPlayer.posX - masterPlayer.posX);
					float playerDistanceY = (float) (renderedPlayer.posY - masterPlayer.posY);
					float playerDistanceZ = (float) (renderedPlayer.posZ - masterPlayer.posZ);
					GL11.glTranslatef(playerDistanceX, playerDistanceY, playerDistanceZ);	
					GL11.glTranslated(0, masterPlayer.getEyeHeight(), 0);
					GL11.glRotated(parent.rotationPitch, Math.cos(parent.rotationYaw  * 0.017453292F), 0, Math.sin(parent.rotationYaw * 0.017453292F));
					GL11.glRotated(parent.rotationRoll, -Math.sin(parent.rotationYaw  * 0.017453292F), 0, Math.cos(parent.rotationYaw * 0.017453292F));
					GL11.glTranslated(0, -masterPlayer.getEyeHeight(), 0);
					GL11.glTranslatef(-playerDistanceX, -playerDistanceY, -playerDistanceZ);
				}else{
					GL11.glTranslated(0, event.entityPlayer.getEyeHeight(), 0);
					GL11.glRotated(parent.rotationPitch, Math.cos(parent.rotationYaw  * 0.017453292F), 0, Math.sin(parent.rotationYaw * 0.017453292F));
					GL11.glRotated(parent.rotationRoll, -Math.sin(parent.rotationYaw  * 0.017453292F), 0, Math.cos(parent.rotationYaw * 0.017453292F));
					GL11.glTranslated(0, -event.entityPlayer.getEyeHeight(), 0);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void on(RenderPlayerEvent.Post event){
		if(event.entityPlayer.ridingEntity instanceof EntitySeat){
			if(((EntitySeat) event.entityPlayer.ridingEntity).parent!=null){
				GL11.glPopMatrix();
			}
		}
	}
	
	/*INS180
	@SubscribeEvent
	public void on(CameraSetup event){
		if(Minecraft.getMinecraft().gameSettings.thirdPersonView==0){
			if(event.entity.ridingEntity instanceof EntitySeat){
				if(((EntitySeat) event.entity.ridingEntity).parent!=null){
					event.roll = ((EntitySeat) event.entity.ridingEntity).parent.rotationRoll;
				}
			}
		}
	}
	INS180*/
	
	/**
	 * Renders HUDs for Vehicles.
	 */
	@SubscribeEvent
	public void on(RenderGameOverlayEvent.Pre event){
		if(minecraft.thePlayer.ridingEntity instanceof EntitySeat){
			if(event.type.equals(RenderGameOverlayEvent.ElementType.HOTBAR)){
				event.setCanceled(!ConfigSystem.getBooleanConfig("XaerosCompatibility"));
			}else if(event.type.equals(RenderGameOverlayEvent.ElementType.CHAT)){
				if(playerLastSeat != null){
					if(playerLastSeat.parent instanceof EntityVehicle && playerLastSeat.isController && (minecraft.gameSettings.thirdPersonView==0 || CameraSystem.hudMode == 1) && !CameraSystem.disableHUD){
						((EntityVehicle) playerLastSeat.parent).drawHUD(event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
					}
				}
			}
		}
	}
	
	/**
	 * Opens the MFS config screen.
	 */
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event){
    	if(ControlSystem.configKey.isPressed()){
    		if(minecraft.currentScreen == null){
    			FMLCommonHandler.instance().showGuiScreen(new GUIConfig());
    		}
    	}
    }
}
