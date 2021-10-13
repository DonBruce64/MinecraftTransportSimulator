package minecrafttransportsimulator.rendering.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFluidLoader;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.AGUIPanel;
import minecrafttransportsimulator.guis.instances.GUIHUD;
import minecrafttransportsimulator.mcinterface.BuilderEntityExisting;
import minecrafttransportsimulator.mcinterface.BuilderGUI;
import minecrafttransportsimulator.mcinterface.BuilderTileEntityFluidTank;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceGUI;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.rendering.instances.RenderText;
import minecrafttransportsimulator.rendering.instances.RenderText.TextAlignment;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Interface for handling events pertaining to overlays on the screen.  This includes the HUD rendered while
 * in a vehicle, plus custom cameras overlays. and text-based overlays based on what the player is pointing at.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceEventsOverlay{
	private static AGUIBase currentGUI;
	private static BuilderGUI currentBuilder;
	private static boolean inFirstPersonLastRender;
	
	/**
	 *  Resets the overlay GUI by nulling it out.  This will cause it to re-create itself next tick.
	 *  Useful if something on it has changed and you need it to re-create the overlay.
	 */
	public static void resetGUI(){
		currentGUI = null;
	}
    
    /**
     * Renders an overlay GUI, or other overlay components like the fluid in a tank if we are mousing-over a vehicle.
     * Also responsible for rendering overlays on custom cameras.  If we need to render a GUI,
     * it should be returned.  Otherwise, return null.
     */
	@SubscribeEvent
    public static void on(RenderGameOverlayEvent.Pre event){
		 //If we have a custom camera overlay active, don't render the crosshairs or the hotbar.
		 String cameraOverlay = CameraSystem.getOverlay();
		 if(CameraSystem.getOverlay() != null && (event.getType().equals(RenderGameOverlayEvent.ElementType.CROSSHAIRS) || event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR))){
			 event.setCanceled(true);
			 return;
		 }
    	
    	//Do overlay rendering before the chat window is rendered.
    	//This rendered them over the main hotbar, but doesn't block the chat window.
    	if(event.getType().equals(RenderGameOverlayEvent.ElementType.CHAT)){
			//Set up variables.
			WrapperPlayer player = InterfaceClient.getClientPlayer();
	    	AEntityD_Interactable<?> ridingEntity = player.getEntityRiding();
	    	int screenWidth = event.getResolution().getScaledWidth();
	    	int screenHeight = event.getResolution().getScaledHeight();
	    	
	    	//If we have a custom camera overlay, render it.
	    	//Don't render anything else but this if we do.
	    	if(cameraOverlay != null){
				InterfaceRender.bindTexture(cameraOverlay);
				InterfaceRender.setBlend(true);
				InterfaceGUI.renderSheetTexture(0, 0, screenWidth, screenHeight, 0.0F, 0.0F, 1.0F, 1.0F, 1, 1);
				InterfaceRender.setBlend(false);
				return;
			}
	    	
	    	//If we are in first-person see if we are mousing over a tank.
	    	//If so, render that tank's info as floating text a-la-IE.
	    	if(InterfaceClient.inFirstPerson()){
	    		//See what we are hitting.
	    		RayTraceResult lastHit = Minecraft.getMinecraft().objectMouseOver;
				if(lastHit != null){
					Point3d mousedOverPoint = new Point3d(lastHit.hitVec.x, lastHit.hitVec.y, lastHit.hitVec.z);
					if(lastHit.entityHit != null){
						if(lastHit.entityHit instanceof BuilderEntityExisting){
							AEntityB_Existing mousedOverEntity = ((BuilderEntityExisting) lastHit.entityHit).entity;
							if(mousedOverEntity instanceof EntityVehicleF_Physics){
								EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) mousedOverEntity;
								for(BoundingBox box : vehicle.allInteractionBoxes){
									if(box.isPointInside(mousedOverPoint)){
										APart part = vehicle.getPartWithBox(box);
										if(part instanceof PartInteractable){
											EntityFluidTank tank = ((PartInteractable) part).tank;
											if(tank != null){
												String tankText = tank.getFluid().isEmpty() ? "EMPTY" : tank.getFluid().toUpperCase() + " : " + tank.getFluidLevel() + "/" + tank.getMaxLevel();
												RenderText.draw2DText(tankText, null, screenWidth/2 + 4, screenHeight/2, ColorRGB.WHITE, TextAlignment.LEFT_ALIGNED, 1.0F, false, 0);
											}
										}
									}
								}
							}
							
						}
					}else{
						TileEntity mcTile = player.getWorld().world.getTileEntity(lastHit.getBlockPos());
						if(mcTile instanceof BuilderTileEntityFluidTank){
							BuilderTileEntityFluidTank<?> builder = (BuilderTileEntityFluidTank<?>) mcTile; 
							if(builder.tileEntity != null && builder.tileEntity instanceof TileEntityFluidLoader){
								EntityFluidTank tank = builder.tileEntity.getTank();
								if(tank != null){
									String tankText = tank.getFluid().isEmpty() ? "EMPTY" : tank.getFluid().toUpperCase() + " : " + tank.getFluidLevel() + "/" + tank.getMaxLevel();
									RenderText.draw2DText(tankText, null, screenWidth/2 + 4, screenHeight/2, ColorRGB.WHITE, TextAlignment.LEFT_ALIGNED, 1.0F, false, 0);
								}
							}
						}
					}
				}
			}
	    	
	    	//Do HUD rendering for vehicles.
			if(ridingEntity instanceof EntityVehicleF_Physics){
				for(WrapperEntity rider : ridingEntity.locationRiderMap.values()){
					if(rider.equals(player)){
						//Get seat we are in.
						PartSeat seat = (PartSeat) ((EntityVehicleF_Physics) ridingEntity).getPartAtLocation(ridingEntity.locationRiderMap.inverse().get(rider));
						
						//If we are in a seat controlling a gun, render a text line for it.
						if(seat.canControlGuns && !InterfaceClient.isChatOpen()){
							RenderText.draw2DText("Active Gun:", null, screenWidth, 0, ColorRGB.WHITE, TextAlignment.RIGHT_ALIGNED, 1.0F, false, 0);
							if(seat.activeGun != null){
								String gunNumberText = seat.activeGun.definition.gun.fireSolo ? " [" + (seat.gunIndex + 1) + "]" : "";
								RenderText.draw2DText(seat.activeGun.getItemName() + gunNumberText, null, screenWidth, 8, ColorRGB.WHITE, TextAlignment.RIGHT_ALIGNED, 1.0F, false, 0);
							}else{
								RenderText.draw2DText("None", null, screenWidth, 8, ColorRGB.WHITE, TextAlignment.RIGHT_ALIGNED, 1.0F, false, 0);
							}
						}
						
						//If the seat is a controller, render the HUD if it's set.
						if(seat.placementDefinition.isController && (InterfaceClient.inFirstPerson() ? ConfigSystem.configObject.clientRendering.renderHUD_1P.value : ConfigSystem.configObject.clientRendering.renderHUD_3P.value)){
							//Create a new GUI for the HUD if we don't have one or if we changed from first-person to third-person.
							if(currentGUI == null || (inFirstPersonLastRender ^ InterfaceClient.inFirstPerson())){
								currentGUI = new GUIHUD((EntityVehicleF_Physics) ridingEntity);
								currentBuilder = new BuilderGUI(currentGUI);
								currentBuilder.initGui();
								currentBuilder.setWorldAndResolution(Minecraft.getMinecraft(), screenWidth, screenHeight);
							}
							
							//Render the HUD now.  This is based on settings in the config.
							//Don't render the HUD if the panel is up though.
							if(!(InterfaceGUI.getActiveGUI() instanceof AGUIPanel)){
								//Translate far enough to not render behind the items.
								//Also translate down if we are a half-HUD.
								GL11.glPushMatrix();
				        		GL11.glTranslated(0, 0, 250);
				        		if(currentGUI instanceof GUIHUD && (InterfaceClient.inFirstPerson() ? !ConfigSystem.configObject.clientRendering.fullHUD_1P.value : !ConfigSystem.configObject.clientRendering.fullHUD_3P.value)){
				        			GL11.glTranslated(0, currentGUI.getHeight()/2D, 0);
				        		}
				        		
				        		//Enable alpha testing.  This can be disabled by mods doing bad state management during their event calls.
				        		//We don't want to enable blending though, as that's on-demand.
				        		//Just in case it is enabled, however, disable it.
				        		//This ensures the blending state is as it will be for the main rendering pass of -1.
				        		InterfaceRender.setBlend(false);
				        		GL11.glEnable(GL11.GL_ALPHA_TEST);
				        		
				        		//Draw the GUI.
				        		currentBuilder.drawScreen(0, 0, event.getPartialTicks());
				        		
				        		//Pop the matrix, and set blending and lighting back to normal.
				        		GL11.glPopMatrix();
				        		InterfaceRender.setBlend(true);
				        		InterfaceRender.setInternalLightingState(false);
							}
			        		
			        		//Return to prevent resetting the GUI.
			        		return;
						}
					}
				}
			}
			
			//Not riding a vehicle.  Reset GUI.
			resetGUI();
    	}
    }
}
