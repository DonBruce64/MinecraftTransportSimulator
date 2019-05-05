package minecrafttransportsimulator.systems;

import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.guis.GUIConfig;
import minecrafttransportsimulator.guis.GUIPackMissing;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.packets.general.PacketPackReload;
import minecrafttransportsimulator.packets.parts.PacketPartInteraction;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleAttacked;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleKey;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleNameTag;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleServerPartAddition;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleWindowFix;
import minecrafttransportsimulator.rendering.RenderHUD;
import minecrafttransportsimulator.rendering.RenderVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;
import minecrafttransportsimulator.vehicles.main.EntityVehicleC_Colliding;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
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
     * Checks if a player has right-clicked a vehicle with a valid item.
     * If so, does an action depending on what was clicked and what the player is holding.
     */
    @SubscribeEvent
    public static void on(PlayerInteractEvent.RightClickItem event){
    	if(event.getWorld().isRemote){
	    	for(Entity entity : minecraft.theWorld.loadedEntityList){
				if(entity instanceof EntityVehicleC_Colliding){
					EntityVehicleC_Colliding vehicle = (EntityVehicleC_Colliding) entity;
					EntityPlayer player = event.getEntityPlayer();
					
					//Before we check item actions, see if we clicked a part and we need to interact with it.
					//If so, do the part's interaction rather than part checks or other interaction.
					APart hitPart = vehicle.getHitPart(player);
					if(hitPart != null){
						if(hitPart.interactPart(player)){
							MTS.MTSNet.sendToServer(new PacketPartInteraction(hitPart, player.getEntityId()));
							return;
						}
					}
					
					//No in-use changes for sneaky sneaks!  Unless we're using a key to lock ourselves in.
					if(vehicle.equals(player.getRidingEntity())){
						if(player.getHeldItem(event.getHand()) != null){
							if(!MTSRegistry.key.equals(player.getHeldItem(event.getHand()).getItem())){
								return;
							}
						}
					}
					
					//If this item is a part, find if we are right-clicking a valid part area.
					//If so, send the info to the server to add a new part.
					//Note that the server will check if we can actually add the part in question.
					//All we do is get the first position we click that we *could* place a part.
			    	if(event.getItemStack().getItem() instanceof AItemPart && vehicle.pack != null){
			    		Vec3d lookVec = player.getLook(1.0F);
        				Vec3d clickedVec = player.getPositionVector().addVector(0, entity.getEyeHeight(), 0);
			    		for(float f=1.0F; f<4.0F; f += 0.1F){
			    			for(Entry<Vec3d, PackPart> packPartEntry : vehicle.getAllPossiblePackParts().entrySet()){
		    					Vec3d offset = RotationSystem.getRotatedPoint(packPartEntry.getKey().addVector(0, 0.25F, 0), vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll);
		    					VehicleAxisAlignedBB partBox = new VehicleAxisAlignedBB(vehicle.getPositionVector().add(offset), packPartEntry.getKey().addVector(0, 0.5F, 0), 0.75F, 1.75F, false);
		    					
		    					if(partBox.isVecInside(clickedVec)){
		    						//Check to make sure this spot is valid (server gets final say).
		    						if(packPartEntry.getValue().types.contains(PackParserSystem.getPartPack(((AItemPart) event.getItemStack().getItem()).partName).general.type)){
		    							//If we clicked an occupied spot with the same part type, don't do anything.
		        						if(vehicle.getPartAtLocation(packPartEntry.getKey().xCoord, packPartEntry.getKey().yCoord, packPartEntry.getKey().zCoord) != null){
		        							return;
										}
		        						//Spot is not occupied, send packet to server to spawn part if able.
			        					MTS.MTSNet.sendToServer(new PacketVehicleServerPartAddition(vehicle, packPartEntry.getKey().xCoord, packPartEntry.getKey().yCoord, packPartEntry.getKey().zCoord, player));
			        					return;
        							}
	        					}
		    				}
        					clickedVec = clickedVec.addVector(lookVec.xCoord*0.1F, lookVec.yCoord*0.1F, lookVec.zCoord*0.1F);
        				}
	    			}else{
	    				//If we are not holding a part, see if we at least clicked the vehicle.
	    				//If so, and we are holding a wrench or key or name tag or glass pane, act on that.
	    				if(vehicle.wasVehicleClicked(player)){
	    					if(event.getItemStack().getItem().equals(MTSRegistry.wrench)){
	    						MTS.proxy.openGUI(vehicle, player);
	    					}else if(event.getItemStack().getItem().equals(MTSRegistry.key)){
	    						MTS.MTSNet.sendToServer(new PacketVehicleKey(vehicle, player));
	    					}else if(event.getItemStack().getItem().equals(Items.NAME_TAG)){
	    						MTS.MTSNet.sendToServer(new PacketVehicleNameTag(vehicle, player));
	    					}else if(event.getItemStack().getItem().equals(Item.getItemFromBlock(Blocks.GLASS_PANE))){
	    						MTS.MTSNet.sendToServer(new PacketVehicleWindowFix(vehicle, player));
	    					}
	    				}
	    			}
	    		}
	    	}
    	}
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
    	if(event.getEntityPlayer().worldObj.isRemote){
	    	if(event.getTarget() instanceof EntityVehicleC_Colliding){
	    		MTS.MTSNet.sendToServer(new PacketVehicleAttacked((EntityVehicleB_Existing) event.getTarget(), event.getEntityPlayer()));
	    		event.getEntityPlayer().playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0F, 1.0F);
	    	}
    	}
    	if(event.getTarget() instanceof EntityVehicleC_Colliding){
    		event.setCanceled(true);
    	}
    }
    /**
     * Adjusts camera zoom if player is seated and in third-person.
     * Also adjusts the player's rotation,
     */
    @SubscribeEvent
    public static void on(TickEvent.RenderTickEvent event){
    	if(event.phase.equals(event.phase.START) && minecraft.thePlayer != null){
    		if(minecraft.thePlayer.getRidingEntity() instanceof EntityVehicleC_Colliding){
    			if(minecraft.gameSettings.thirdPersonView != 0){
    				CameraSystem.runCustomCamera(event.renderTickTime);
    			}
    		}
    	}
    }
    
    /**
     * Rotates player in the seat for proper rendering and forwards camera control options to the ControlSystem.
     */
    @SubscribeEvent
    public static void on(TickEvent.ClientTickEvent event){
        if(minecraft.theWorld != null){
            if(event.phase.equals(Phase.END)){            	
                if(minecraft.thePlayer.getRidingEntity() instanceof EntityVehicleC_Colliding){
                    if(!Minecraft.getMinecraft().ingameGUI.getChatGUI().getChatOpen()){
                    	if(minecraft.thePlayer.getRidingEntity() instanceof EntityVehicleE_Powered){
                    		EntityVehicleE_Powered vehicle = (EntityVehicleE_Powered) minecraft.thePlayer.getRidingEntity();
                    		if(vehicle.getSeatForRider(minecraft.thePlayer) != null){
                    			ControlSystem.controlVehicle(vehicle, vehicle.getSeatForRider(minecraft.thePlayer).isController);
                    		}
                        }
                    }
                    if(!minecraft.isGamePaused()){
        				CameraSystem.updatePlayerYawAndPitch(minecraft.thePlayer, (EntityVehicleB_Existing) minecraft.thePlayer.getRidingEntity());
                     }
                }
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
    		if(((WorldServer) serverPlayer.worldObj).getMinecraftServer().isSinglePlayer()){
	    		if(defaultRenderDistance == 0){
	    			defaultRenderDistance = ((WorldServer) serverPlayer.worldObj).getMinecraftServer().getPlayerList().getViewDistance();
	    			currentRenderDistance = defaultRenderDistance;
	    			renderReductionHeight = ConfigSystem.getIntegerConfig("RenderReductionHeight");
				}
	    		
	    		if(serverPlayer.posY > renderReductionHeight && currentRenderDistance != 1){
	    			currentRenderDistance = 1;
	    			((WorldServer) serverPlayer.worldObj).getPlayerChunkMap().setPlayerViewRadius(1);
	    		}else if(serverPlayer.posY < renderReductionHeight - 10 && currentRenderDistance == 1){
	    			currentRenderDistance = defaultRenderDistance;
	    			((WorldServer) serverPlayer.worldObj).getPlayerChunkMap().setPlayerViewRadius(defaultRenderDistance);
	    		}
	    	}
    	}
    }

    /**
     * Adjusts roll for camera.
     * Only works when camera is inside vehicles.
     */
    @SubscribeEvent
    public static void on(CameraSetup event){
        if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 0){
            if(event.getEntity().getRidingEntity() instanceof EntityVehicleC_Colliding){
            	EntityVehicleC_Colliding vehicle = (EntityVehicleC_Colliding) event.getEntity().getRidingEntity();
                event.setRoll((float) (vehicle.rotationRoll  + (vehicle.rotationRoll - vehicle.prevRotationRoll)*(double)event.getRenderPartialTicks()));
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
        for(Entity entity : minecraft.theWorld.loadedEntityList){
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
        	GL11.glPushMatrix();
        	EntityVehicleC_Colliding vehicle = (EntityVehicleC_Colliding) event.getEntityPlayer().getRidingEntity();
	        if(vehicle.pack != null){
	        	PartSeat seat = vehicle.getSeatForRider(event.getEntityPlayer());
	        	if(seat != null){
		            //First restrict the player's yaw to prevent them from being able to rotate their body in a seat.
		            Vec3d placementRotation = seat.partRotation;
		            event.getEntityPlayer().renderYawOffset = (float) (vehicle.rotationYaw + placementRotation.yCoord);
		            if(vehicle.rotationPitch > 90 || vehicle.rotationPitch < -90){
		            	event.getEntityPlayer().rotationYawHead = event.getEntityPlayer().rotationYaw*-1F;
		            }else{
			            event.getEntityPlayer().rotationYawHead = event.getEntityPlayer().rotationYaw;
		            }
		            
		            //Now add the pitch rotation.
		            if(!event.getEntityPlayer().equals(minecraft.thePlayer)){
		                EntityPlayer masterPlayer = Minecraft.getMinecraft().thePlayer;
		                EntityPlayer renderedPlayer = event.getEntityPlayer();
		                float playerDistanceX = (float) (renderedPlayer.posX - masterPlayer.posX);
		                float playerDistanceY = (float) (renderedPlayer.posY - masterPlayer.posY);
		                float playerDistanceZ = (float) (renderedPlayer.posZ - masterPlayer.posZ);
		                GL11.glTranslatef(playerDistanceX, playerDistanceY, playerDistanceZ);
		                GL11.glTranslated(0, masterPlayer.getEyeHeight(), 0);
		                GL11.glRotated(vehicle.rotationPitch + placementRotation.xCoord, Math.cos(vehicle.rotationYaw  * 0.017453292F), 0, Math.sin(vehicle.rotationYaw * 0.017453292F));
		                GL11.glRotated(vehicle.rotationRoll + placementRotation.zCoord, -Math.sin(vehicle.rotationYaw  * 0.017453292F), 0, Math.cos(vehicle.rotationYaw * 0.017453292F));
		                GL11.glTranslated(0, -masterPlayer.getEyeHeight(), 0);
		                GL11.glTranslatef(-playerDistanceX, -playerDistanceY, -playerDistanceZ);
		            }else{
		                GL11.glTranslated(0, event.getEntityPlayer().getEyeHeight(), 0);
		                GL11.glRotated(vehicle.rotationPitch + placementRotation.xCoord, Math.cos(vehicle.rotationYaw  * 0.017453292F), 0, Math.sin(vehicle.rotationYaw * 0.017453292F));
		                GL11.glRotated(vehicle.rotationRoll + placementRotation.zCoord, -Math.sin(vehicle.rotationYaw  * 0.017453292F), 0, Math.cos(vehicle.rotationYaw * 0.017453292F));
		                GL11.glTranslated(0, -event.getEntityPlayer().getEyeHeight(), 0);
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
        if(minecraft.thePlayer.getRidingEntity() instanceof EntityVehicleC_Colliding){
            if(event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR)){
                event.setCanceled(true);
            }else if(event.getType().equals(RenderGameOverlayEvent.ElementType.CHAT)){
                if(minecraft.thePlayer.getRidingEntity() instanceof EntityVehicleE_Powered){
                	EntityVehicleE_Powered vehicle = (EntityVehicleE_Powered) minecraft.thePlayer.getRidingEntity();
                	if(vehicle.getSeatForRider(minecraft.thePlayer) != null){
	                	if(vehicle.getSeatForRider(minecraft.thePlayer).isController && (minecraft.gameSettings.thirdPersonView==0 || CameraSystem.hudMode == 1) && !CameraSystem.disableHUD){
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
        		for(Entity entity : minecraft.getMinecraft().theWorld.loadedEntityList){
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
