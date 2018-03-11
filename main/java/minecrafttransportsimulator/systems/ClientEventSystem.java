package minecrafttransportsimulator.systems;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSCreativeTabs;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackPart;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.MTSRegistryClient;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.guis.GUIConfig;
import minecrafttransportsimulator.guis.GUIPackMissing;
import minecrafttransportsimulator.guis.GUISplash;
import minecrafttransportsimulator.items.ItemPart;
import minecrafttransportsimulator.packets.general.MultipartAttackPacket;
import minecrafttransportsimulator.packets.general.MultipartKeyActionPacket;
import minecrafttransportsimulator.packets.general.MultipartNameTagActionPacket;
import minecrafttransportsimulator.packets.general.MultipartPartAdditionPacket;
import minecrafttransportsimulator.packets.general.MultipartPartInteractionPacket;
import minecrafttransportsimulator.packets.general.PackReloadPacket;
import minecrafttransportsimulator.rendering.RenderHUD;
import minecrafttransportsimulator.rendering.RenderMultipart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
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

/**This class handles rendering/camera edits that need to happen when riding vehicles,
 * as well as clicking of multiparts and their parts, as well as some other misc things.
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
     * Need this to do part interaction in cases when we aren't holding anything.
     */
    @SubscribeEvent
    public static void on(PlayerInteractEvent.RightClickEmpty event){
    	for(Entity entity : minecraft.theWorld.loadedEntityList){
			if(entity instanceof EntityMultipartMoving){
				EntityMultipartMoving mover = (EntityMultipartMoving) entity;
				EntityPlayer player = event.getEntityPlayer();
				EntityMultipartChild hitChild = mover.getHitChild(player);
				if(hitChild != null){
					hitChild.interactPart(player);
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
    	for(Entity entity : minecraft.theWorld.loadedEntityList){
			if(entity instanceof EntityMultipartMoving){
				EntityMultipartMoving mover = (EntityMultipartMoving) entity;
				EntityPlayer player = event.getEntityPlayer();
				
				//Before we check item actions, see if we clicked a part and we need to interact with it.
				//If so, do the part's interaction rather than part checks or other interaction.
				EntityMultipartChild hitChild = mover.getHitChild(player);
				if(hitChild != null){
					if(hitChild.interactPart(player)){
						MTS.MTSNet.sendToServer(new MultipartPartInteractionPacket(hitChild.getEntityId(), player.getEntityId()));
						return;
					}
				}
				
				//No in-use changes for sneaky sneaks!  Unless we're using a key to lock ourselves in.
				if(player.getRidingEntity() instanceof EntitySeat){
					if(player.getHeldItem(event.getHand()) != null){
						if(!MTSRegistry.key.equals(player.getHeldItem(event.getHand()).getItem())){
							return;
						}
					}
				}
				
				//If this item is a part, find if we are right-clicking a valid part area.
				//If so, send the info to the server to add a new part.
				//Note that the server will check if we can actually add the part in question.
		    	if(event.getItemStack().getItem() instanceof ItemPart){
    				for(byte i=0; i<mover.pack.parts.size(); ++i){
    					PackPart packPart = mover.pack.parts.get(i);
    					MTSVector offset = RotationSystem.getRotatedPoint(packPart.pos[0], packPart.pos[1], packPart.pos[2], mover.rotationPitch, mover.rotationYaw, mover.rotationRoll);
    					AxisAlignedBB partBox = new AxisAlignedBB((float) (mover.posX + offset.xCoord) - 0.75F, (float) (mover.posY + offset.yCoord) - 0.75F, (float) (mover.posZ + offset.zCoord) - 0.75F, (float) (mover.posX + offset.xCoord) + 0.75F, (float) (mover.posY + offset.yCoord) + 0.75F, (float) (mover.posZ + offset.zCoord) + 0.75F);
    					Vec3d lookVec = player.getLook(1.0F);
        				Vec3d clickedVec = player.getPositionVector().addVector(0, entity.getEyeHeight(), 0);
        				for(float f=1.0F; f<4.0F; f += 0.1F){
        					if(partBox.isVecInside(clickedVec)){
        						boolean isPartPresent = false;
        						for(EntityMultipartChild child : mover.getChildren()){
									if(child.offsetX == packPart.pos[0] && child.offsetY == packPart.pos[1] && child.offsetZ == packPart.pos[2]){
										isPartPresent = true;
										break;
									}
								}
        						if(!isPartPresent){
	        						MTS.MTSNet.sendToServer(new MultipartPartAdditionPacket(mover.getEntityId(), player.getEntityId(), i));
	        						return;
        						}
        					}
        					clickedVec = clickedVec.addVector(lookVec.xCoord*0.1F, lookVec.yCoord*0.1F, lookVec.zCoord*0.1F);
        				}
    				}
    			}else{
    				//If we are not holding a part, see if we at least clicked the multipart.
    				//If so, and we are holding a wrench or key or name tag, act on that.
    				if(mover.wasMultipartClicked(player)){
    					if(event.getItemStack().getItem().equals(MTSRegistry.wrench)){
    						MTS.proxy.openGUI(mover, player);
    					}else if(event.getItemStack().getItem().equals(MTSRegistry.key)){
    						MTS.MTSNet.sendToServer(new MultipartKeyActionPacket(mover.getEntityId(), player.getEntityId()));
    					}else if(event.getItemStack().getItem().equals(Items.NAME_TAG)){
    						MTS.MTSNet.sendToServer(new MultipartNameTagActionPacket(mover.getEntityId(), player.getEntityId()));
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
	    	if(event.getTarget() instanceof EntityMultipartMoving){
	    		event.setCanceled(true);
	    		MTS.MTSNet.sendToServer(new MultipartAttackPacket(event.getTarget().getEntityId(), event.getEntityPlayer().getEntityId()));
	    		event.getEntityPlayer().playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0F, 1.0F);
	    	}
    	}else{
    		event.setCanceled(true);
    	}
    }
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
	            	if(ConfigSystem.getIntegerConfig("MajorVersion") != Integer.valueOf(MTS.MODVER.substring(0, 1))){
	                    ConfigSystem.setClientConfig("MajorVersion", Integer.valueOf(MTS.MODVER.substring(0, 1)));
	                    FMLCommonHandler.instance().showGuiScreen(new GUISplash());
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
     * Renders HUDs for Aircraft.
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
     * Renders a warning on the MTS creative tab if there is no pack data.
     */
    @SubscribeEvent
    public static void on(DrawScreenEvent.Post event){
    	if(PackParserSystem.getRegisteredNames().isEmpty()){
	    	if(event.getGui() instanceof GuiContainerCreative){
	    		GuiContainerCreative creativeScreen = (GuiContainerCreative) event.getGui();
	    		if(CreativeTabs.CREATIVE_TAB_ARRAY[creativeScreen.getSelectedTabIndex()] instanceof MTSCreativeTabs){
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
            if(minecraft.currentScreen == null){
            	FMLCommonHandler.instance().showGuiScreen(new GUIConfig());
                if(Minecraft.getMinecraft().isSingleplayer()){
                	MTS.MTSNet.sendToServer(new PackReloadPacket());
                	MTSRegistryClient.loadCustomOBJModels();
                	RenderMultipart.resetDisplayLists();
                }
            }
        }
    }
}
