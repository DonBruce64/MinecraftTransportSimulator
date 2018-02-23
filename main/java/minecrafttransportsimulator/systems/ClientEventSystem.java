package minecrafttransportsimulator.systems;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.MTSRegistryClient;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.guis.GUIConfig;
import minecrafttransportsimulator.guis.GUISplash;
import minecrafttransportsimulator.packets.general.PackReloadPacket;
import minecrafttransportsimulator.rendering.RenderHUD;
import minecrafttransportsimulator.rendering.RenderMultipart;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;

/**This class handles rendering/camera edits that need to happen when riding planes
 * as well as some other misc things.
 *
 * @author don_bruce
 */
@Mod.EventBusSubscriber(Side.CLIENT)
public final class ClientEventSystem{
    /**The last seat a player was in.  If null, this means the player is not in a seat.*/
    public static EntitySeat playerLastSeat = null;
    private static boolean firstTickRun = false;
    private static Minecraft minecraft = Minecraft.getMinecraft();

    /**
     * Adjusts camera zoom if player is seated and in third-person.
     * Also adjusts the player's rotation,
     */
    @SubscribeEvent
    public static void on(TickEvent.RenderTickEvent event){
    	if(event.phase.equals(event.phase.START)){
    		if(playerLastSeat != null){
    			if(minecraft.gameSettings.thirdPersonView != 0){
    				CameraSystem.runCustomCamera(event.renderTickTime);
    			}
    		}
    	}
    }
    
    /**
     * Updates player seated status and rotates player in the seat.
     * Forwards camera control options to the ControlSystem.
     * Checks on world load to see if player has loaded this major revision before.
     * If not, it shows the player the info screen once to appraise them of the changes.
     */
    @SubscribeEvent
    public static void on(TickEvent.ClientTickEvent event){
        if(minecraft.theWorld != null){
            if(event.phase.equals(Phase.END)){
            	if(!firstTickRun){
	            	if(MTSRegistry.multipartItemMap.size() == 0){
	            		FMLCommonHandler.instance().showGuiScreen(new GUISplash(false));
	            	}else if(ConfigSystem.getIntegerConfig("MajorVersion") != Integer.valueOf(MTS.MODVER.substring(0, 1))){
	                    ConfigSystem.setClientConfig("MajorVersion", Integer.valueOf(MTS.MODVER.substring(0, 1)));
	                    FMLCommonHandler.instance().showGuiScreen(new GUISplash(true));
	                }
            	}
            	firstTickRun = true;
            	
                //Update the player seated status
                if(minecraft.thePlayer.getRidingEntity() == null){
                    if(playerLastSeat != null){
                        playerLastSeat = null;
                    }
                }else if(minecraft.thePlayer.getRidingEntity() instanceof EntitySeat){
                    if(playerLastSeat == null || !playerLastSeat.equals(minecraft.thePlayer.getRidingEntity())){
                        playerLastSeat = (EntitySeat) minecraft.thePlayer.getRidingEntity();
                    }
                    if(!Minecraft.getMinecraft().ingameGUI.getChatGUI().getChatOpen()){
                    	if(playerLastSeat.parent instanceof EntityMultipartVehicle){
                    		ControlSystem.controlVehicle((EntityMultipartVehicle) playerLastSeat.parent, playerLastSeat.isController);
                        }
                    }
                    if(!minecraft.isGamePaused()){
        				if(playerLastSeat.parent != null){
        					CameraSystem.updatePlayerYawAndPitch(minecraft.thePlayer, playerLastSeat.parent);
        				}
                     }
                }
            }
        }
    }

    /**
     * Adjusts roll for camera.
     * Only works when camera is inside the plane.
     */
    @SubscribeEvent
    public static void on(CameraSetup event){
        if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 0){
            if(event.getEntity().getRidingEntity() instanceof EntitySeat){
            	EntityMultipartMoving mover = (EntityMultipartMoving) ((EntitySeat) event.getEntity().getRidingEntity()).parent;
                if(mover != null){
                    event.setRoll((float) (mover.rotationRoll  + (mover.rotationRoll - mover.prevRotationRoll)*(double)event.getRenderPartialTicks()));
                }
            }
        }
    }

    /**
     * Used to force rendering of aircraft above the world height limit, as
     * newer versions suppress this as part of the chunk visibility
     * feature.
     * Also causes lights to render, as rendering them during regular calls
     * results in water being invisible.
     */
    @SubscribeEvent
    public static void on(RenderWorldLastEvent event){
        for(Entity entity : minecraft.theWorld.loadedEntityList){
            if(entity instanceof EntityMultipartMoving){
            	minecraft.getRenderManager().getEntityRenderObject(entity).doRender(entity, 0, 0, 0, 0, event.getPartialTicks());
            }
        }
    }

    /**
     * Pre-post methods for adjusting player pitch while seated.
     */
    @SubscribeEvent
    public static void on(RenderPlayerEvent.Pre event){
        if(event.getEntityPlayer().getRidingEntity() instanceof EntitySeat){
            EntityMultipartParent parent = ((EntitySeat) event.getEntityPlayer().getRidingEntity()).parent;
            if(parent!=null){
                GL11.glPushMatrix();
                if(!event.getEntityPlayer().equals(minecraft.thePlayer)){
                    EntityPlayer masterPlayer = Minecraft.getMinecraft().thePlayer;
                    EntityPlayer renderedPlayer = event.getEntityPlayer();
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
                    GL11.glTranslated(0, event.getEntityPlayer().getEyeHeight(), 0);
                    GL11.glRotated(parent.rotationPitch, Math.cos(parent.rotationYaw  * 0.017453292F), 0, Math.sin(parent.rotationYaw * 0.017453292F));
                    GL11.glRotated(parent.rotationRoll, -Math.sin(parent.rotationYaw  * 0.017453292F), 0, Math.cos(parent.rotationYaw * 0.017453292F));
                    GL11.glTranslated(0, -event.getEntityPlayer().getEyeHeight(), 0);
                }
            }
        }
    }

    @SubscribeEvent
    public static void on(RenderPlayerEvent.Post event){
        if(event.getEntityPlayer().getRidingEntity() instanceof EntitySeat){
            if(((EntitySeat) event.getEntityPlayer().getRidingEntity()).parent!=null){
                GL11.glPopMatrix();
            }
        }
    }

    /**
     * Renders HUDs for Planes.
     */
    @SubscribeEvent
    public static void on(RenderGameOverlayEvent.Pre event){
        if(minecraft.thePlayer.getRidingEntity() instanceof EntitySeat){
            if(event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR)){
                event.setCanceled(true);
            }else if(event.getType().equals(RenderGameOverlayEvent.ElementType.CHAT)){
                if(playerLastSeat != null){
                    if(playerLastSeat.parent instanceof EntityMultipartVehicle && playerLastSeat.isController && (minecraft.gameSettings.thirdPersonView==0 || CameraSystem.hudMode == 1) && !CameraSystem.disableHUD){
                        RenderHUD.drawMainHUD((EntityMultipartVehicle) playerLastSeat.parent, event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight(), false);
                    }
                }
            }
        }
    }

    /**
     * Opens the MFS config screen.
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event){
        if(ControlSystem.isMasterControlButttonPressed()){
            if(minecraft.currentScreen == null){
            	FMLCommonHandler.instance().showGuiScreen(new GUIConfig());
                if(Minecraft.getMinecraft().isSingleplayer()){
                	MTS.MTSNet.sendToServer(new PackReloadPacket());
                	MTSRegistryClient.loadCustomOBJModels();
                	RenderMultipart.resetDisplayLists();
                	EntityMultipartMoving.resetCollisionBoxes();
                }
            }
        }
    }
}
