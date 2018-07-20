package minecrafttransportsimulator.systems;

import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MultipartAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.guis.GUIConfig;
import minecrafttransportsimulator.guis.GUIPackMissing;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartA_Base;
import minecrafttransportsimulator.multipart.main.EntityMultipartB_Existing;
import minecrafttransportsimulator.multipart.main.EntityMultipartC_Colliding;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.multipart.parts.APart;
import minecrafttransportsimulator.packets.general.PackReloadPacket;
import minecrafttransportsimulator.packets.multipart.PacketMultipartAttacked;
import minecrafttransportsimulator.packets.multipart.PacketMultipartKey;
import minecrafttransportsimulator.packets.multipart.PacketMultipartNameTag;
import minecrafttransportsimulator.packets.multipart.PacketMultipartServerPartAddition;
import minecrafttransportsimulator.packets.multipart.PacketMultipartWindowFix;
import minecrafttransportsimulator.packets.parts.PacketPartInteraction;
import minecrafttransportsimulator.rendering.RenderHUD;
import minecrafttransportsimulator.rendering.RenderMultipart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;
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
 * as well as clicking of multiparts and their parts, as well as some other misc things.
 *
 * @author don_bruce
 */
@Mod.EventBusSubscriber(Side.CLIENT)
@SideOnly(Side.CLIENT)
public final class ClientEventSystem{
    private static final Minecraft minecraft = Minecraft.getMinecraft();
    
    /**
     * Need this to do part interaction in cases when we aren't holding anything.
     * Note that in 1.11+ this method is obsolete as the player is always holding an
     * itemstack, it's just an "empty" itemstack.
     */
    @SubscribeEvent
    public static void on(PlayerInteractEvent.RightClickEmpty event){
    	if(event.getWorld().isRemote){
	    	for(Entity entity : minecraft.theWorld.loadedEntityList){
				if(entity instanceof EntityMultipartC_Colliding){
					EntityMultipartC_Colliding multipart = (EntityMultipartC_Colliding) entity;
					EntityPlayer player = event.getEntityPlayer();
					APart hitPart = multipart.getHitPart(player);
					if(hitPart != null){
						if(hitPart.interactPart(player)){
							MTS.MTSNet.sendToServer(new PacketPartInteraction(hitPart, player.getEntityId()));
							return;
						}
					}
				}
	    	}
    	}
    }
    
    /**
     * Checks if a player has right-clicked a multipart with a valid item.
     * If so, does an action depending on what was clicked and what the player is holding.
     */
    @SubscribeEvent
    public static void on(PlayerInteractEvent.RightClickItem event){
    	if(event.getWorld().isRemote){
	    	for(Entity entity : minecraft.theWorld.loadedEntityList){
				if(entity instanceof EntityMultipartC_Colliding){
					EntityMultipartC_Colliding multipart = (EntityMultipartC_Colliding) entity;
					EntityPlayer player = event.getEntityPlayer();
					
					//Before we check item actions, see if we clicked a part and we need to interact with it.
					//If so, do the part's interaction rather than part checks or other interaction.
					APart hitPart = multipart.getHitPart(player);
					if(hitPart != null){
						if(hitPart.interactPart(player)){
							MTS.MTSNet.sendToServer(new PacketPartInteraction(hitPart, player.getEntityId()));
							return;
						}
					}
					
					//No in-use changes for sneaky sneaks!  Unless we're using a key to lock ourselves in.
					if(multipart.equals(player.getRidingEntity())){
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
			    	if(event.getItemStack().getItem() instanceof AItemPart && multipart.pack != null){
			    		Vec3d lookVec = player.getLook(1.0F);
        				Vec3d clickedVec = player.getPositionVector().addVector(0, entity.getEyeHeight(), 0);
			    		for(float f=1.0F; f<4.0F; f += 0.1F){
			    			for(Entry<Vec3d, PackPart> packPartEntry : multipart.getAllPossiblePackParts().entrySet()){
		    					Vec3d offset = RotationSystem.getRotatedPoint(packPartEntry.getKey().addVector(0, 0.25F, 0), multipart.rotationPitch, multipart.rotationYaw, multipart.rotationRoll);
		    					MultipartAxisAlignedBB partBox = new MultipartAxisAlignedBB(multipart.getPositionVector().add(offset), packPartEntry.getKey().addVector(0, 0.25F, 0), 1.5F, 2.0F);	    					
		    					
		    					if(partBox.isVecInside(clickedVec)){
	        						//If we clicked an occupied spot, don't do anything.
	        						if(multipart.getPartAtLocation(packPartEntry.getKey().xCoord, packPartEntry.getKey().yCoord, packPartEntry.getKey().zCoord) != null){
										return;
									}
	        						//Spot is not occupied, send packet to server to spawn part if able.
		        					MTS.MTSNet.sendToServer(new PacketMultipartServerPartAddition(multipart, packPartEntry.getKey().xCoord, packPartEntry.getKey().yCoord, packPartEntry.getKey().zCoord, player));
		        					return;
	        					}
		    				}
        					clickedVec = clickedVec.addVector(lookVec.xCoord*0.1F, lookVec.yCoord*0.1F, lookVec.zCoord*0.1F);
        				}
	    			}else{
	    				//If we are not holding a part, see if we at least clicked the multipart.
	    				//If so, and we are holding a wrench or key or name tag or glass pane, act on that.
	    				if(multipart.wasMultipartClicked(player)){
	    					if(event.getItemStack().getItem().equals(MTSRegistry.wrench)){
	    						MTS.proxy.openGUI(multipart, player);
	    					}else if(event.getItemStack().getItem().equals(MTSRegistry.key)){
	    						MTS.MTSNet.sendToServer(new PacketMultipartKey(multipart, player));
	    					}else if(event.getItemStack().getItem().equals(Items.NAME_TAG)){
	    						MTS.MTSNet.sendToServer(new PacketMultipartNameTag(multipart, player));
	    					}else if(event.getItemStack().getItem().equals(Item.getItemFromBlock(Blocks.GLASS_PANE))){
	    						MTS.MTSNet.sendToServer(new PacketMultipartWindowFix(multipart, player));
	    					}
	    				}
	    			}
	    		}
	    	}
    	}
    }

    /**
     * If a player swings and misses a multipart they may still have hit it.
     * MC doesn't look for attacks based on AABB, rather it uses RayTracing.
     * This works on the client where we can see the part, but on the server
     * the internal distance check nulls this out.
     * If we are attacking a multipart here cancel the attack and instead fire
     * the attack manually from a packet to make dang sure we get it to the multipart!
     */
    @SubscribeEvent
    public static void on(AttackEntityEvent event){
    	//You might think this only gets called on clients, you'd be wrong.
    	//Forge will gladly call this on the client and server threads on SP.
    	if(event.getEntityPlayer().worldObj.isRemote){
	    	if(event.getTarget() instanceof EntityMultipartC_Colliding){
	    		MTS.MTSNet.sendToServer(new PacketMultipartAttacked((EntityMultipartB_Existing) event.getTarget(), event.getEntityPlayer()));
	    		event.getEntityPlayer().playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0F, 1.0F);
	    	}
    	}
    	if(event.getTarget() instanceof EntityMultipartC_Colliding){
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
    		if(minecraft.thePlayer.getRidingEntity() instanceof EntityMultipartC_Colliding){
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
                if(minecraft.thePlayer.getRidingEntity() instanceof EntityMultipartC_Colliding){
                    if(!Minecraft.getMinecraft().ingameGUI.getChatGUI().getChatOpen()){
                    	if(minecraft.thePlayer.getRidingEntity() instanceof EntityMultipartE_Vehicle){
                    		EntityMultipartE_Vehicle vehicle = (EntityMultipartE_Vehicle) minecraft.thePlayer.getRidingEntity();
                    		if(vehicle.getSeatForRider(minecraft.thePlayer) != null){
                    			ControlSystem.controlVehicle(vehicle, vehicle.getSeatForRider(minecraft.thePlayer).isController);
                    		}
                        }
                    }
                    if(!minecraft.isGamePaused()){
        				CameraSystem.updatePlayerYawAndPitch(minecraft.thePlayer, (EntityMultipartC_Colliding) minecraft.thePlayer.getRidingEntity());
                     }
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
            if(event.getEntity().getRidingEntity() instanceof EntityMultipartC_Colliding){
            	EntityMultipartC_Colliding multipart = (EntityMultipartC_Colliding) event.getEntity().getRidingEntity();
                event.setRoll((float) (multipart.rotationRoll  + (multipart.rotationRoll - multipart.prevRotationRoll)*(double)event.getRenderPartialTicks()));
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
            if(entity instanceof EntityMultipartE_Vehicle){
            	minecraft.getRenderManager().getEntityRenderObject(entity).doRender(entity, 0, 0, 0, 0, event.getPartialTicks());
            }
        }
    }

    /**
     * Pre-post methods for adjusting player pitch while seated.
     */
    @SubscribeEvent
    public static void on(RenderPlayerEvent.Pre event){
        if(event.getEntityPlayer().getRidingEntity() instanceof EntityMultipartC_Colliding){
        	EntityMultipartC_Colliding multipart = (EntityMultipartC_Colliding) event.getEntityPlayer().getRidingEntity();
            GL11.glPushMatrix();
            //First restrict the player's yaw to prevent them from being able to rotate their body in a seat.
            event.getEntityPlayer().renderYawOffset = multipart.rotationYaw;
            
            //Now add the pitch rotation.
            if(!event.getEntityPlayer().equals(minecraft.thePlayer)){
                EntityPlayer masterPlayer = Minecraft.getMinecraft().thePlayer;
                EntityPlayer renderedPlayer = event.getEntityPlayer();
                float playerDistanceX = (float) (renderedPlayer.posX - masterPlayer.posX);
                float playerDistanceY = (float) (renderedPlayer.posY - masterPlayer.posY);
                float playerDistanceZ = (float) (renderedPlayer.posZ - masterPlayer.posZ);
                GL11.glTranslatef(playerDistanceX, playerDistanceY, playerDistanceZ);
                GL11.glTranslated(0, masterPlayer.getEyeHeight(), 0);
                GL11.glRotated(multipart.rotationPitch, Math.cos(multipart.rotationYaw  * 0.017453292F), 0, Math.sin(multipart.rotationYaw * 0.017453292F));
                GL11.glRotated(multipart.rotationRoll, -Math.sin(multipart.rotationYaw  * 0.017453292F), 0, Math.cos(multipart.rotationYaw * 0.017453292F));
                GL11.glTranslated(0, -masterPlayer.getEyeHeight(), 0);
                GL11.glTranslatef(-playerDistanceX, -playerDistanceY, -playerDistanceZ);
            }else{
                GL11.glTranslated(0, event.getEntityPlayer().getEyeHeight(), 0);
                GL11.glRotated(multipart.rotationPitch, Math.cos(multipart.rotationYaw  * 0.017453292F), 0, Math.sin(multipart.rotationYaw * 0.017453292F));
                GL11.glRotated(multipart.rotationRoll, -Math.sin(multipart.rotationYaw  * 0.017453292F), 0, Math.cos(multipart.rotationYaw * 0.017453292F));
                GL11.glTranslated(0, -event.getEntityPlayer().getEyeHeight(), 0);
            }
        }
    }

    @SubscribeEvent
    public static void on(RenderPlayerEvent.Post event){
    	if(event.getEntityPlayer().getRidingEntity() instanceof EntityMultipartC_Colliding){
    		GL11.glPopMatrix();
        }
    }

    /**
     * Renders HUDs for Aircraft.
     */
    @SubscribeEvent
    public static void on(RenderGameOverlayEvent.Pre event){    	
        if(minecraft.thePlayer.getRidingEntity() instanceof EntityMultipartC_Colliding){
            if(event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR)){
                event.setCanceled(true);
            }else if(event.getType().equals(RenderGameOverlayEvent.ElementType.CHAT)){
                if(minecraft.thePlayer.getRidingEntity() instanceof EntityMultipartE_Vehicle){
                	EntityMultipartE_Vehicle vehicle = (EntityMultipartE_Vehicle) minecraft.thePlayer.getRidingEntity();
                	if(vehicle.getSeatForRider(minecraft.thePlayer) != null){
	                	if(vehicle.getSeatForRider(minecraft.thePlayer).isController && (minecraft.gameSettings.thirdPersonView==0 || CameraSystem.hudMode == 1) && !CameraSystem.disableHUD){
	                		RenderHUD.drawMainHUD(vehicle, event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight(), false);
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
    	if(PackParserSystem.getAllMultipartPackNames().isEmpty()){
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
        		RenderMultipart.clearCaches();
        		minecraft.refreshResources();
        		for(Entity entity : minecraft.getMinecraft().theWorld.loadedEntityList){
					if(entity instanceof EntityMultipartA_Base){
						EntityMultipartA_Base multipart = (EntityMultipartA_Base) entity;
						multipart.pack = PackParserSystem.getMultipartPack(multipart.multipartName);
					}
				}
        		MTS.MTSNet.sendToServer(new PackReloadPacket());
        	}
            if(minecraft.currentScreen == null){
            	FMLCommonHandler.instance().showGuiScreen(new GUIConfig());
            }
        }
    }
}
