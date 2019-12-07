package minecrafttransportsimulator.systems;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.guis.GUIConfig;
import minecrafttransportsimulator.guis.GUIPackMissing;
import minecrafttransportsimulator.packets.general.PacketPackReload;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleAttacked;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleInteracted;
import minecrafttransportsimulator.radio.RadioManager;
import minecrafttransportsimulator.rendering.RenderHUD;
import minecrafttransportsimulator.rendering.RenderVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;
import minecrafttransportsimulator.vehicles.main.EntityVehicleC_Colliding;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**This class handles rendering/camera edits that need to happen when riding vehicles,
 * as well as clicking of vehicles and their parts, as well as some other misc things.
 *
 * @author don_bruce
 */
@Mod.EventBusSubscriber(Side.CLIENT)
@SideOnly(Side.CLIENT)
public final class ClientEventSystem{
    private static final Minecraft minecraft = Minecraft.getMinecraft();
    
    /**
     * Checks if a player has right-clicked a vehicle.
     * If so send a packet off to the server for processing.
     * This is done as the server culls all interactions based
     * on distance, so large entities aren't able to be interacted with.
     * We don't do anything in the entity interact function as we
     * do it in the packet instead.
     * 
     * The only exception here is if we use a wrench.  In that case, we
     * need to open a GUI, which is only on the client.
     */
    @SubscribeEvent
    public static void on(PlayerInteractEvent.EntityInteract event){
    	//You might think this only gets called on clients.  You'd be wrong.
    	//Forge will gladly call this on the client and server threads on SP.
    	//This is despite the fact this class is labeled as client-only.
    	EntityPlayer player = event.getEntityPlayer();
    	if(player.world.isRemote && event.getHand().equals(EnumHand.MAIN_HAND)){
    		if(event.getTarget() instanceof EntityVehicleC_Colliding){
    			EntityVehicleC_Colliding vehicle = (EntityVehicleC_Colliding) event.getTarget();
    			if(player.getHeldItemMainhand().getItem().equals(MTSRegistry.wrench)){
    				MTS.proxy.openGUI(vehicle, player);
				}else{
					MTS.MTSNet.sendToServer(new PacketVehicleInteracted(vehicle, event.getEntityPlayer()));
				}
    			event.setCanceled(true);
    			event.setCancellationResult(EnumActionResult.SUCCESS);
	    	}
    	}
    }
    
    /**
     * While the above event method will catch most interactions, it won't
     * handle interactions when we are seated in the vehicle we are trying
     * to interact with.  This prevents players from locking doors and
     * changing seats in planes.  We add this event here to catch these
     * edge-cases.
     */
    @SubscribeEvent
    public static void on(PlayerInteractEvent.RightClickItem event){
    	if(event.getEntityPlayer().world.isRemote && event.getHand().equals(EnumHand.MAIN_HAND)){
    		if(doClickEvent(event.getEntityPlayer())){
	    		event.setCanceled(true);
				event.setCancellationResult(EnumActionResult.SUCCESS);
    		}
    	}
    }
    
    /**
     * There is one more edge-case we need to account for.  And that's being
     * seated in a vehicle, but not having anything in our hands.  This, for
     * some dumb reason, results in a different event call.  We catch it here
     * to ensure even if the player isn't holding anything, they can still
     * change seats while in a vehicle.
     */
    @SubscribeEvent
    public static void on(PlayerInteractEvent.RightClickEmpty event){
    	if(event.getEntityPlayer().world.isRemote && event.getHand().equals(EnumHand.MAIN_HAND)){
    		doClickEvent(event.getEntityPlayer());
    	}
    }
    
    private static boolean doClickEvent(EntityPlayer player){
    	Vec3d lookVec = player.getLook(1.0F);
		for(Entity entity : minecraft.world.loadedEntityList){
			if(entity instanceof EntityVehicleC_Colliding){
				EntityVehicleC_Colliding vehicle = (EntityVehicleC_Colliding) entity;
				Vec3d clickedVec = player.getPositionVector().addVector(0, entity.getEyeHeight(), 0);
	    		for(float f=1.0F; f<4.0F; f += 0.1F){
	    			if(vehicle.getEntityBoundingBox().contains(clickedVec)){
	    				MTS.MTSNet.sendToServer(new PacketVehicleInteracted(vehicle, player));
	    				return true;
	    			}
	    			clickedVec = clickedVec.addVector(lookVec.x*0.1F, lookVec.y*0.1F, lookVec.z*0.1F);
	    		}
			}
		}
		return false;
    }
    
    
    /**
     * If a player swings and misses a vehicle they may still have hit it.
     * MC doesn't look for attacks based on AABB, rather it uses RayTracing.
     * This works on the client where we can see the part, but on the server
     * the internal distance check nulls this out.
     * If we are attacking a vehicle here cancel the attack and instead fire
     * the attack manually from a packet to make dang sure we get it to the vehicle!
     */
    @SubscribeEvent
    public static void on(AttackEntityEvent event){
    	//You might think this only gets called on clients, you'd be wrong.
    	//Forge will gladly call this on the client and server threads on SP.
    	if(event.getEntityPlayer().world.isRemote){
	    	if(event.getTarget() instanceof EntityVehicleC_Colliding){
	    		MTS.MTSNet.sendToServer(new PacketVehicleAttacked((EntityVehicleC_Colliding) event.getTarget(), event.getEntityPlayer()));
	    		event.getEntityPlayer().playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0F, 1.0F);
	    	}
    	}
    	if(event.getTarget() instanceof EntityVehicleC_Colliding){
    		event.setCanceled(true);
    	}
    }
    
    /**
     * Rotates player in the seat for proper rendering and forwards camera control options to the ControlSystem.
     * Also tells the RadioSystem to update, so it can adjust volume and playing status.
     */
    @SubscribeEvent
    public static void on(TickEvent.ClientTickEvent event){
        if(minecraft.world != null){
            if(event.phase.equals(Phase.END)){            	
                if(minecraft.player.getRidingEntity() instanceof EntityVehicleC_Colliding){
                    if(!Minecraft.getMinecraft().ingameGUI.getChatGUI().getChatOpen()){
                    	if(minecraft.player.getRidingEntity() instanceof EntityVehicleE_Powered){
                    		EntityVehicleE_Powered vehicle = (EntityVehicleE_Powered) minecraft.player.getRidingEntity();
                    		if(vehicle.getSeatForRider(minecraft.player) != null){
                    			ControlSystem.controlVehicle(vehicle, vehicle.getSeatForRider(minecraft.player).isController);
                    		}
                        }
                    }
                    if(!minecraft.isGamePaused()){
        				CameraSystem.updatePlayerYawAndPitch(minecraft.player, (EntityVehicleB_Existing) minecraft.player.getRidingEntity());
                     }
                }
                //Update the radios at the end of the client tick.
                RadioManager.updateRadios();
            }
        }
    }
    
    
    private static int defaultRenderDistance;
	private static int currentRenderDistance;
	private static int renderReductionHeight;
    /**
     * Automatically lowers render distance when flying above the world to reduce worldgen.
     * Results in significant TPS improvements at high speeds.
     * Note that this only runs on the integrated server.
     */
    @SubscribeEvent
    public static void on(TickEvent.PlayerTickEvent event){
    	if(event.side.isServer() && event.phase.equals(event.phase.END)){
    		EntityPlayerMP serverPlayer = (EntityPlayerMP) event.player;
    		if(((WorldServer) serverPlayer.world).getMinecraftServer().isSinglePlayer()){
	    		if(defaultRenderDistance == 0){
	    			defaultRenderDistance = ((WorldServer) serverPlayer.world).getMinecraftServer().getPlayerList().getViewDistance();
	    			currentRenderDistance = defaultRenderDistance;
	    			renderReductionHeight = ConfigSystem.getIntegerConfig("RenderReductionHeight");
				}
	    		
	    		if(serverPlayer.posY > renderReductionHeight && currentRenderDistance != 1){
	    			currentRenderDistance = 1;
	    			((WorldServer) serverPlayer.world).getPlayerChunkMap().setPlayerViewRadius(1);
	    		}else if(serverPlayer.posY < renderReductionHeight - 10 && currentRenderDistance == 1){
	    			currentRenderDistance = defaultRenderDistance;
	    			((WorldServer) serverPlayer.world).getPlayerChunkMap().setPlayerViewRadius(defaultRenderDistance);
	    		}
	    	}
    	}
    }

    /**
     * Adjusts roll and pitch for camera.
     * Only works when camera is inside vehicles.
     */
    @SubscribeEvent
    public static void on(CameraSetup event){
    	if(event.getEntity().getRidingEntity() instanceof EntityVehicleC_Colliding){
    		if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 0){            	
            	EntityVehicleC_Colliding vehicle = (EntityVehicleC_Colliding) event.getEntity().getRidingEntity();
            	//Get yaw delta from vehicle and player.  -180 to 180.
            	float playerYawAngle = (360 + (event.getEntity().rotationYaw - vehicle.rotationYaw))%360;
            	if(playerYawAngle > 180){
            		playerYawAngle = -360 + playerYawAngle;
            	}
            	float rollRollComponent = (float) (Math.cos(Math.toRadians(playerYawAngle))*(vehicle.rotationRoll + (vehicle.rotationRoll - vehicle.prevRotationRoll)*(double)event.getRenderPartialTicks()));
            	float pitchRollComponent = (float) (Math.sin(Math.toRadians(playerYawAngle))*(vehicle.rotationPitch + (vehicle.rotationPitch - vehicle.prevRotationPitch)*(double)event.getRenderPartialTicks()));
            	float rollPitchComponent = (float) (Math.sin(Math.toRadians(playerYawAngle))*(vehicle.rotationRoll + (vehicle.rotationRoll - vehicle.prevRotationRoll)*(double)event.getRenderPartialTicks()));
            	float pitchPitchComponent = (float) (Math.cos(Math.toRadians(playerYawAngle))*(vehicle.rotationPitch + (vehicle.rotationPitch - vehicle.prevRotationPitch)*(double)event.getRenderPartialTicks()));
            	GL11.glRotated(pitchPitchComponent + rollPitchComponent, 1, 0, 0);
        		GL11.glRotated(rollRollComponent - pitchRollComponent, 0, 0, 1);
        	}else{
        		CameraSystem.performZoomAction();
        	}
        }
    }

    /**
     * Used to force rendering of aircraft above the world height limit, as
     * newer versions suppress this as part of the chunk visibility
     * feature.  Also causes lights to render, as rendering them during regular calls
     * results in water being invisible.
     */
    @SubscribeEvent
    public static void on(RenderWorldLastEvent event){
        for(Entity entity : minecraft.world.loadedEntityList){
            if(entity instanceof EntityVehicleE_Powered){
            	minecraft.getRenderManager().getEntityRenderObject(entity).doRender(entity, 0, 0, 0, 0, event.getPartialTicks());
            }
        }
    }

    /**
     * Pre-post methods for adjusting player pitch while seated.
     */
    @SubscribeEvent
    public static void on(RenderPlayerEvent.Pre event){
        if(event.getEntityPlayer().getRidingEntity() instanceof EntityVehicleC_Colliding){
        	EntityVehicleC_Colliding vehicle = (EntityVehicleC_Colliding) event.getEntityPlayer().getRidingEntity();
        	GL11.glPushMatrix();
        	if(vehicle.pack != null){
	        	PartSeat seat = vehicle.getSeatForRider(event.getEntityPlayer());
	        	if(seat != null){
		            //First restrict the player's yaw to prevent them from being able to rotate their body in a seat.
		            Vec3d placementRotation = seat.partRotation;
		            event.getEntityPlayer().renderYawOffset = (float) (vehicle.rotationYaw + placementRotation.y);
		            if(vehicle.rotationPitch > 90 || vehicle.rotationPitch < -90){
		            	event.getEntityPlayer().rotationYawHead = event.getEntityPlayer().rotationYaw*-1F;
		            }else{
			            event.getEntityPlayer().rotationYawHead = event.getEntityPlayer().rotationYaw;
		            }
		            
		            //Now add the pitch rotation.
		            if(!event.getEntityPlayer().equals(minecraft.player)){
		                EntityPlayer masterPlayer = Minecraft.getMinecraft().player;
		                EntityPlayer renderedPlayer = event.getEntityPlayer();
		                float playerDistanceX = (float) (renderedPlayer.posX - masterPlayer.posX);
		                float playerDistanceY = (float) (renderedPlayer.posY - masterPlayer.posY);
		                float playerDistanceZ = (float) (renderedPlayer.posZ - masterPlayer.posZ);
		                GL11.glTranslatef(playerDistanceX, playerDistanceY, playerDistanceZ);
		                GL11.glTranslated(0, masterPlayer.getEyeHeight(), 0);
		                GL11.glRotated(vehicle.rotationPitch + placementRotation.x, Math.cos(vehicle.rotationYaw  * 0.017453292F), 0, Math.sin(vehicle.rotationYaw * 0.017453292F));
		                GL11.glRotated(vehicle.rotationRoll + placementRotation.z, -Math.sin(vehicle.rotationYaw  * 0.017453292F), 0, Math.cos(vehicle.rotationYaw * 0.017453292F));
		                GL11.glTranslated(0, -masterPlayer.getEyeHeight(), 0);
		                GL11.glTranslatef(-playerDistanceX, -playerDistanceY, -playerDistanceZ);
		            }else{
		                GL11.glTranslated(0, event.getEntityPlayer().getEyeHeight(), 0);
		                GL11.glRotated(vehicle.rotationPitch + placementRotation.x, Math.cos(vehicle.rotationYaw  * 0.017453292F), 0, Math.sin(vehicle.rotationYaw * 0.017453292F));
		                GL11.glRotated(vehicle.rotationRoll + placementRotation.z, -Math.sin(vehicle.rotationYaw  * 0.017453292F), 0, Math.cos(vehicle.rotationYaw * 0.017453292F));
		                GL11.glTranslated(0, -event.getEntityPlayer().getEyeHeight(), 0);
		            }
		            
		            //Make the player dance if the radio is playing.
		            RenderPlayer render = event.getRenderer();
		            if(RadioManager.getRadio((EntityVehicleE_Powered) vehicle).getPlayState() != -1){
		            	render.getMainModel().bipedHead.offsetZ = 0.075F - 0.15F*(Minecraft.getMinecraft().world.getTotalWorldTime()%6)/6F;
		            }else{
		            	render.getMainModel().bipedHead.offsetZ = 0.0F;
		            }
	        	}
        	}
        }
    }

    @SubscribeEvent
    public static void on(RenderPlayerEvent.Post event){
    	if(event.getEntityPlayer().getRidingEntity() instanceof EntityVehicleC_Colliding){
    		GL11.glPopMatrix();
        }
    }

    /**
     * Renders HUDs for Aircraft.
     */
    @SubscribeEvent
    public static void on(RenderGameOverlayEvent.Pre event){    	
        if(minecraft.player.getRidingEntity() instanceof EntityVehicleC_Colliding){
            if(event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR)){
                event.setCanceled(true);
            }else if(event.getType().equals(RenderGameOverlayEvent.ElementType.CHAT)){
                if(minecraft.player.getRidingEntity() instanceof EntityVehicleE_Powered){
                	EntityVehicleE_Powered vehicle = (EntityVehicleE_Powered) minecraft.player.getRidingEntity();
                	if(vehicle.getSeatForRider(minecraft.player) != null){
	                	if(vehicle.getSeatForRider(minecraft.player).isController && (minecraft.gameSettings.thirdPersonView==0 || CameraSystem.hudMode == 1) && !CameraSystem.disableHUD){
	                		GL11.glPushMatrix();
	                		GL11.glScalef(1.0F*event.getResolution().getScaledWidth()/RenderHUD.screenDefaultX, 1.0F*event.getResolution().getScaledHeight()/RenderHUD.screenDefaultY, 0);
	                		RenderHUD.drawMainHUD(vehicle, false);
	                		GL11.glPopMatrix();
	                	}
                	}
                }
            }
        }
    }
    
    /**
     * Renders a warning on the MTS core creative tab if there is no pack data.
     */
    @SubscribeEvent
    public static void on(DrawScreenEvent.Post event){
    	if(PackParserSystem.getAllVehiclePackNames().isEmpty()){
	    	if(event.getGui() instanceof GuiContainerCreative){
	    		GuiContainerCreative creativeScreen = (GuiContainerCreative) event.getGui();
	    		if(CreativeTabs.CREATIVE_TAB_ARRAY[creativeScreen.getSelectedTabIndex()].equals(MTSRegistry.coreTab)){
	    			FMLCommonHandler.instance().showGuiScreen(new GUIPackMissing());
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
        	if(ConfigSystem.getBooleanConfig("DevMode") && minecraft.isSingleplayer()){
        		PackParserSystem.reloadPackData();
        		RenderVehicle.clearCaches();
        		minecraft.refreshResources();
        		for(Entity entity : minecraft.getMinecraft().world.loadedEntityList){
					if(entity instanceof EntityVehicleA_Base){
						EntityVehicleA_Base vehicle = (EntityVehicleA_Base) entity;
						vehicle.pack = PackParserSystem.getVehiclePack(vehicle.vehicleName);
					}
				}
        		MTS.MTSNet.sendToServer(new PacketPackReload());
        	}
            if(minecraft.currentScreen == null){
            	FMLCommonHandler.instance().showGuiScreen(new GUIConfig());
            }
        }
    }
}
