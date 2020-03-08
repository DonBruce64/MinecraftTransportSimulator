package minecrafttransportsimulator.systems;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.guis.instances.GUIConfig;
import minecrafttransportsimulator.guis.instances.GUIHUD;
import minecrafttransportsimulator.guis.instances.GUIPackMissing;
import minecrafttransportsimulator.items.packs.parts.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.PackInstrument;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.packets.general.PacketPackReload;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleAttacked;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleInteracted;
import minecrafttransportsimulator.radio.RadioManager;
import minecrafttransportsimulator.radio.RadioThread;
import minecrafttransportsimulator.rendering.vehicles.RenderInstrument;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import minecrafttransportsimulator.vehicles.main.EntityVehicleC_Colliding;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperInput;
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
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
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
    private static final RadioThread radioThread = new RadioThread();
    
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
    				boolean isPlayerOP = player.getServer() == null || player.getServer().isSinglePlayer() || player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null;
					if(vehicle.ownerName.isEmpty() || EntityPlayer.getUUID(player.getGameProfile()).toString().equals(vehicle.ownerName) || isPlayerOP){
						MTS.proxy.openGUI(vehicle, player);
					}else{
						MTS.MTSNet.sendTo(new PacketChat("interact.failure.vehicleowned"), (EntityPlayerMP) player);
					}
				}else if(!(player.getHeldItemMainhand().getItem() instanceof AItemPart)){
					MTS.MTSNet.sendToServer(new PacketVehicleInteracted(vehicle, event.getEntityPlayer()));
					event.setCanceled(true);
	    			event.setCancellationResult(EnumActionResult.SUCCESS);
				}
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
		for(Entity entity : player.world.loadedEntityList){
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
    
    public static boolean lockedView = true;
    private static int defaultRenderDistance;
	private static int currentRenderDistance;
    /**
     * Performs updates to the player related to vehicle functions.  These include: <br>
     * 1) Sending the player to the {@link ControlSystem} for controls checks when seated in a vehicle.<br>
     * 2) Updating the player's yaw/pitch to match vehicle movement if camera is locked (roll happens in {@link #on(CameraSetup)}.<br>
     * 3) Disabling the mouse if mouseYoke is set and the camera is locked (also enables mouse if player isn't in a vehicle)<br>
     * 4) Automatically lowering render distance to 1 when flying above the world to reduce worldgen lag.<br>
     */
    @SubscribeEvent
    public static void on(TickEvent.PlayerTickEvent event){
    	//Only do updates at the end of a phase to prevent double-updates.
        if(event.phase.equals(Phase.END)){
    		//If we are on the integrated server, and riding a vehicle, reduce render height.
    		if(event.side.isServer()){
    			if(event.player.getRidingEntity() instanceof EntityVehicleE_Powered){
            		WorldServer serverWorld = (WorldServer) event.player.world;
            		if(serverWorld.getMinecraftServer().isSinglePlayer()){
        	    		//If default render distance is 0, we must have not set it yet.
            			//Set both it and the current distance to the actual current distance.
            			if(defaultRenderDistance == 0){
        	    			defaultRenderDistance = serverWorld.getMinecraftServer().getPlayerList().getViewDistance();
        	    			currentRenderDistance = defaultRenderDistance;
        				}
        	    		
            			//If the player is above the configured renderReductionHeight, reduce render.
            			//Once the player drops 10 blocks below it, put the render back to the value it was before.
            			//We don't want to set this every tick as it'll confuse the server.
        	    		if(event.player.posY > ConfigSystem.configObject.client.renderReductionHeight.value && currentRenderDistance != 1){
        	    			currentRenderDistance = 1;
        	    			serverWorld.getPlayerChunkMap().setPlayerViewRadius(1);
        	    		}else if(event.player.posY < ConfigSystem.configObject.client.renderReductionHeight.value - 10 && currentRenderDistance == 1){
        	    			currentRenderDistance = defaultRenderDistance;
        	    			serverWorld.getPlayerChunkMap().setPlayerViewRadius(defaultRenderDistance);
        	    		}
        	    	}
    			}
        	}else{
        		//We are on the client.  Do update logic.
        		//First update the radio threa.
        		if(!radioThread.isAlive()){
                	radioThread.start();
                }else{
                	radioThread.setListenerPosition(event.player.posX, event.player.posY, event.player.posZ, !minecraft.isGamePaused());	
                }
        		
        		//If we are riding a vehicle, do rotation and control operation.
        		if(event.player.getRidingEntity() instanceof EntityVehicleE_Powered){
        			EntityVehicleE_Powered vehicle = (EntityVehicleE_Powered) event.player.getRidingEntity();

                    //If we aren't paused, and we have a lockedView, rotate us with the vehicle.
                    if(!minecraft.isGamePaused() && lockedView){
            			event.player.rotationYaw += vehicle.rotationYaw - vehicle.prevRotationYaw;
            			if((vehicle.rotationPitch > 90 || vehicle.rotationPitch < -90) ^ (vehicle.prevRotationPitch > 90 || vehicle.prevRotationPitch < -90)){
            				event.player.rotationYaw+=180;
            			}
                		if(Minecraft.getMinecraft().gameSettings.thirdPersonView != 0){
                			event.player.rotationPitch += vehicle.rotationPitch - vehicle.prevRotationPitch;
                		}
                     }
                	
                	//If the player is seated, and the seat is a controller, check their controls.
        			//If the seat is a controller, and we have mouseYoke enabled, and our view is locked disable the mouse from MC.
                	//We need to check here for the seat because the link could be broken for a bit due to syncing errors.
                	if(vehicle.getSeatForRider(event.player) != null){
                		PartSeat playeSeat = vehicle.getSeatForRider(event.player);
                		if(playeSeat != null){
                			ControlSystem.controlVehicle(vehicle, playeSeat.isController);
                			WrapperInput.setMouseEnabled(!(playeSeat.isController && ConfigSystem.configObject.client.mouseYoke.value && lockedView));
                			return;
                		}
            		}
        		}
        		
        		//If we got down here, we must not be riding and controlling a vehicle via mouseYoke.
        		//Re-enable the mouse to ensure we don't keep it locked.
    			if(ConfigSystem.configObject.client.mouseYoke.value){
            		WrapperInput.setMouseEnabled(true);
            	}
        	}
        }
    }

    
    public static int zoomLevel = 0;
    /**
     * Adjusts roll, pitch, and zoom for camera.
     * Roll and pitch only gets updated when inside vehicles as we use OpenGL transforms.
     * For external rotations, we just move the player's head with the vehicle's movement as
     * the camera will need to naturally follow the vehicle's motion.
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
        	}else if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 1){
        		GL11.glTranslatef(0, 0F, -zoomLevel);
            }else{
                GL11.glTranslatef(0, 0F, zoomLevel);
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
        	if(vehicle.definition != null){
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
     * Renders the HUD on vehicles.  We don't use the GUI here as it would lock inputs.
     */
    @SubscribeEvent
    public static void on(RenderGameOverlayEvent.Pre event){    	
    	boolean inFirstPerson = minecraft.gameSettings.thirdPersonView == 0;
        if(minecraft.player.getRidingEntity() instanceof EntityVehicleE_Powered && (inFirstPerson ? ConfigSystem.configObject.client.renderHUD_1P.value : ConfigSystem.configObject.client.renderHUD_3P.value)){
            if(event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR)){
                event.setCanceled(true);
            }else if(event.getType().equals(RenderGameOverlayEvent.ElementType.CHAT)){
            	EntityVehicleE_Powered vehicle = (EntityVehicleE_Powered) minecraft.player.getRidingEntity();
            	if(vehicle.getSeatForRider(minecraft.player) != null){
                	if(vehicle.getSeatForRider(minecraft.player).isController){
                		//Get the HUD start position.
                		boolean halfHud = inFirstPerson ? ConfigSystem.configObject.client.fullHUD_1P.value : ConfigSystem.configObject.client.fullHUD_3P.value; 
                		final int guiLeft = (event.getResolution().getScaledWidth() - GUIHUD.HUD_WIDTH)/2;
                		final int guiTop = halfHud ? event.getResolution().getScaledHeight() - GUIHUD.HUD_HEIGHT : (event.getResolution().getScaledHeight() - GUIHUD.HUD_HEIGHT/2);
                		
                		//Enable the lightmap to take brightness into account.
                		//Normally this is disabled for the overlays.
                		//The same goes for alpha testing.
                		Minecraft.getMinecraft().entityRenderer.enableLightmap();
        				GL11.glEnable(GL11.GL_ALPHA_TEST);
                		
                		//Bind the HUD texture and render it if set in the config.
                		if(inFirstPerson ? !ConfigSystem.configObject.client.transpHUD_1P.value : !ConfigSystem.configObject.client.transpHUD_3P.value){
                			//Set the render height depending on the config.
	                		Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(vehicle.definition.rendering.hudTexture != null ? vehicle.definition.rendering.hudTexture : "mts:textures/guis/hud.png"));
	                		WrapperGUI.renderSheetTexture(guiLeft, guiTop, GUIHUD.HUD_WIDTH, GUIHUD.HUD_HEIGHT, 0, 0, GUIHUD.HUD_WIDTH, GUIHUD.HUD_HEIGHT, 512, 256);
                		}
                		
                		//Iterate though all the instruments the vehicle has and render them. 
                		for(Byte instrumentNumber : vehicle.instruments.keySet()){
                			PackInstrument packInstrument = vehicle.definition.motorized.instruments.get(instrumentNumber);
                			//Only render instruments that don't have an optionalPartNumber.
                			if(vehicle.definition.motorized.instruments.get(instrumentNumber).optionalPartNumber == 0){
                				GL11.glPushMatrix();
                				GL11.glTranslated(guiLeft + packInstrument.hudX, guiTop + packInstrument.hudY, 0);
                				GL11.glScalef(packInstrument.hudScale, packInstrument.hudScale, packInstrument.hudScale);
                				RenderInstrument.drawInstrument(vehicle.instruments.get(instrumentNumber), packInstrument.optionalPartNumber, vehicle);
                				GL11.glPopMatrix();
                			}
                		}
                		
                		//Disable the lightmap and alpha to put it back to its old state.
                		Minecraft.getMinecraft().entityRenderer.disableLightmap();
                		GL11.glDisable(GL11.GL_ALPHA_TEST);
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
    	if(MTSRegistry.packItemMap.size() == 0){
	    	if(event.getGui() instanceof GuiContainerCreative){
	    		GuiContainerCreative creativeScreen = (GuiContainerCreative) event.getGui();
	    		if(CreativeTabs.CREATIVE_TAB_ARRAY[creativeScreen.getSelectedTabIndex()].equals(MTSRegistry.coreTab)){
	    			WrapperGUI.openGUI(new GUIPackMissing());
	    		}
	    	}
    	}
    }

    /**
     * Opens the config screen when the config key is pressed.
     * Also reloads assets if we are in devMode.
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event){
        if(WrapperInput.isMasterControlButttonPressed()){
        	if(ConfigSystem.configObject.client.devMode.value && minecraft.isSingleplayer()){
        		FMLClientHandler.instance().refreshResources(VanillaResourceType.MODELS);
        		FMLClientHandler.instance().refreshResources(VanillaResourceType.TEXTURES);
        		for(Entity entity : Minecraft.getMinecraft().world.loadedEntityList){
					if(entity instanceof EntityVehicleA_Base){
						EntityVehicleA_Base vehicle = (EntityVehicleA_Base) entity;
						vehicle.definition = (JSONVehicle) MTSRegistry.packItemMap.get(vehicle.definition.packID).get(vehicle.definition.systemName).definition;
					}
				}
        		MTS.MTSNet.sendToServer(new PacketPackReload());
        	}
            if(minecraft.currentScreen == null){
            	WrapperGUI.openGUI(new GUIConfig());
            }
        }
    }
}
