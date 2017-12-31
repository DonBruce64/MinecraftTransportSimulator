package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.MTSRegistryClient;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.guis.GUIConfig;
import minecrafttransportsimulator.guis.GUICredits;
import minecrafttransportsimulator.packets.general.PackPacket;
import minecrafttransportsimulator.rendering.RenderHUD;
import minecrafttransportsimulator.rendering.RenderMultipart;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

import org.lwjgl.opengl.GL11;

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
            event.player.inventory.addItemStackToInventory(new ItemStack(MTSRegistry.manual));
        }
    }


    /**
     * Adjusts camera zoom if player is seated and in third-person.
     * Also adjusts the player's rotation.
     */
    @SubscribeEvent
    public void on(TickEvent.RenderTickEvent event){
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
     */
    @SubscribeEvent
    public void on(TickEvent.ClientTickEvent event){
        if(minecraft.theWorld != null){
            if(event.phase.equals(Phase.END)){
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
                        ControlSystem.controlCamera();
                        if(playerLastSeat.isController){
                            if(playerLastSeat.parent instanceof EntityMultipartVehicle){
                                ControlSystem.controlVehicle((EntityMultipartVehicle) playerLastSeat.parent, minecraft.thePlayer);
                            }
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
    public void on(CameraSetup event){
        if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 0){
            if(event.getEntity().getRidingEntity() instanceof EntitySeat){
            	EntityMultipartParent parent = ((EntitySeat) event.getEntity().getRidingEntity()).parent;
                if(parent != null){
                    event.setRoll((float) (parent.rotationRoll  + (parent.rotationRoll - parent.prevRotationRoll)*(double)event.getRenderPartialTicks()));
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
    public void on(RenderWorldLastEvent event){
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
    public void on(RenderPlayerEvent.Pre event){
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
    public void on(RenderPlayerEvent.Post event){
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
    public void on(RenderGameOverlayEvent.Pre event){
        if(minecraft.thePlayer.getRidingEntity() instanceof EntitySeat){
            if(event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR)){
                event.setCanceled(!ConfigSystem.getBooleanConfig("XaerosCompatibility"));
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
    public void onKeyInput(InputEvent.KeyInputEvent event){
        if(ControlSystem.configKey.isPressed()){
            if(minecraft.currentScreen == null){
            	FMLCommonHandler.instance().showGuiScreen(new GUIConfig());
                if(Minecraft.getMinecraft().isSingleplayer()){
                	MTS.MTSNet.sendToServer(new PackPacket());
                	MTSRegistryClient.modelMap.clear();
                	MTSRegistryClient.loadCustomOBJModels();
                	RenderMultipart.resetDisplayLists();
                }
            }
        }
    }
}
