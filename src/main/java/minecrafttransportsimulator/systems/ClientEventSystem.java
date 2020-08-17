package minecrafttransportsimulator.systems;

import mcinterface.BuilderGUI;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.guis.instances.GUIPackMissing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**This class handles rendering/camera edits that need to happen when riding vehicles,
 * as well as clicking of vehicles and their parts, as well as some other misc things.
 *
 * @author don_bruce
 */
//@Mod.EventBusSubscriber(Side.CLIENT)
//@SideOnly(Side.CLIENT)
public final class ClientEventSystem{
    private static final Minecraft minecraft = Minecraft.getMinecraft();
    public static boolean lockedView = true;
    private static int defaultRenderDistance;
	private static int currentRenderDistance;
	//FIXME fix all client events.
    /**
     * Performs updates to the player related to vehicle functions.  These include: <br>
     * 1) Sending the player to the {@link ControlSystem} for controls checks when seated in a vehicle.<br>
     * 2) Updating the player's yaw/pitch to match vehicle movement if camera is locked (roll happens in {@link #on(CameraSetup)}.<br>
     * 3) Disabling the mouse if mouseYoke is set and the camera is locked (also enables mouse if player isn't in a vehicle)<br>
     * 4) Automatically lowering render distance to 1 when flying above the world to reduce worldgen lag.<br>
     */
    @SubscribeEvent
    public static void on(TickEvent.PlayerTickEvent event){
    	/*
    	//Only do updates at the end of a phase to prevent double-updates.
        if(event.phase.equals(Phase.END)){
    		//If we are on the integrated server, and riding a vehicle, reduce render height.
    		if(event.side.isServer()){
    			if(event.player.getRidingEntity() instanceof EntityVehicleF_Physics){
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
        	}
        }*/
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
    	/*
    	if(event.getEntity().getRidingEntity() instanceof EntityVehicleF_Physics){
    		if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 0){            	
    			EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) event.getEntity().getRidingEntity();
            	//Get yaw delta from vehicle and player.  -180 to 180.
            	float playerYawAngle = (360 + (event.getEntity().rotationYaw - vehicle.rotationYaw))%360;
            	if(playerYawAngle > 180){
            		playerYawAngle = -360 + playerYawAngle;
            	}
            	float rollRollComponent = (float) (Math.cos(Math.toRadians(playerYawAngle))*(vehicle.rotationRoll + (vehicle.rotationRoll - vehicle.prevRotationRoll)*event.getRenderPartialTicks()));
            	float pitchRollComponent = (float) (Math.sin(Math.toRadians(playerYawAngle))*(vehicle.rotationPitch + (vehicle.rotationPitch - vehicle.prevRotationPitch)*event.getRenderPartialTicks()));
            	float rollPitchComponent = (float) (Math.sin(Math.toRadians(playerYawAngle))*(vehicle.rotationRoll + (vehicle.rotationRoll - vehicle.prevRotationRoll)*event.getRenderPartialTicks()));
            	float pitchPitchComponent = (float) (Math.cos(Math.toRadians(playerYawAngle))*(vehicle.rotationPitch + (vehicle.rotationPitch - vehicle.prevRotationPitch)*event.getRenderPartialTicks()));
            	GL11.glRotated(pitchPitchComponent + rollPitchComponent, 1, 0, 0);
        		GL11.glRotated(rollRollComponent - pitchRollComponent, 0, 0, 1);
        	}else if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 1){
        		GL11.glTranslatef(0, 0F, -zoomLevel);
            }else{
                GL11.glTranslatef(0, 0F, zoomLevel);
        	}
        }*/
    }

    /**
     * Renders the HUD on vehicles.  We don't use the GUI here as it would lock inputs.
     */
    @SubscribeEvent
    public static void on(RenderGameOverlayEvent.Post event){
    	/*
    	boolean inFirstPerson = minecraft.gameSettings.thirdPersonView == 0;
        if(minecraft.player.getRidingEntity() instanceof EntityVehicleF_Physics && (inFirstPerson ? ConfigSystem.configObject.client.renderHUD_1P.value : ConfigSystem.configObject.client.renderHUD_3P.value)){
            if(event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR)){
            	EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) minecraft.player.getRidingEntity();
            	if(vehicle.getSeatForRider(minecraft.player) != null){
                	if(vehicle.getSeatForRider(minecraft.player).vehicleDefinition.isController){
                		//Translate far enough to not render behind the items.
                		GL11.glTranslated(0, 0, 250);
                		
                		//Get the HUD start position.
                		boolean halfHud = inFirstPerson ? ConfigSystem.configObject.client.fullHUD_1P.value : ConfigSystem.configObject.client.fullHUD_3P.value; 
                		final int guiLeft = (event.getResolution().getScaledWidth() - GUIHUD.HUD_WIDTH)/2;
                		final int guiTop = halfHud ? event.getResolution().getScaledHeight() - GUIHUD.HUD_HEIGHT : (event.getResolution().getScaledHeight() - GUIHUD.HUD_HEIGHT/2);
                		
                		//Set lighting to vehicle, and enable alpha.
                		//This makes the GUI the same brightness as the vehicle.
                		//We need to disable the OpenGL lighting, however.
                		//This causes the HUD to change lighting based on the vehicle orientation, which we don't want. 
                		InterfaceRender.setLightingToEntity(vehicle);
                		InterfaceRender.setSystemLightingState(false);
        				GL11.glEnable(GL11.GL_ALPHA_TEST);
                		
                		//Bind the HUD texture and render it if set in the config.
                		if(inFirstPerson ? !ConfigSystem.configObject.client.transpHUD_1P.value : !ConfigSystem.configObject.client.transpHUD_3P.value){
                			//Set the render height depending on the config.
	                		Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(vehicle.definition.rendering.hudTexture != null ? vehicle.definition.rendering.hudTexture : "mts:textures/guis/hud.png"));
	                		BuilderGUI.renderSheetTexture(guiLeft, guiTop, GUIHUD.HUD_WIDTH, GUIHUD.HUD_HEIGHT, 0, 0, GUIHUD.HUD_WIDTH, GUIHUD.HUD_HEIGHT, 512, 256);
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
                		
                		//Disable the translating, lightmap, alpha to put it back to its old state.
                		GL11.glTranslated(0, 0, -250);
                		InterfaceRender.setInternalLightingState(false);
                		GL11.glDisable(GL11.GL_ALPHA_TEST);
                	}
            	}
            }
        }*/
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
	    			BuilderGUI.openGUI(new GUIPackMissing());
	    		}
	    	}
    	}
    }
}
