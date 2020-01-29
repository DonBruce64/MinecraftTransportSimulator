package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.control.LightPacket;
import minecrafttransportsimulator.packets.control.SirenPacket;
import minecrafttransportsimulator.packets.control.TrailerPacket;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal.PacketEngineTypes;
import minecrafttransportsimulator.rendering.RenderHUD;
import minecrafttransportsimulator.rendering.RenderInstruments;
import minecrafttransportsimulator.rendering.RenderVehicle;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightTypes;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Ground;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

/**A GUI/control system hybrid, this takes the place of the HUD when called up.
 * Used for controlling engines, lights, trailers, and other things.
 * 
 * @author don_bruce
 */
public class GUIPanelGround extends GuiScreen{
	private static final ResourceLocation toggleOn = new ResourceLocation("textures/blocks/redstone_lamp_on.png");
	private static final ResourceLocation toggleOff = new ResourceLocation("textures/blocks/redstone_lamp_off.png");
	private static final LightTypes[] lights = new LightTypes[]{LightTypes.RUNNINGLIGHT, LightTypes.HEADLIGHT, LightTypes.EMERGENCYLIGHT};
	private static final String[] lightText = new String[]{I18n.format("gui.panel.runninglights"), I18n.format("gui.panel.headlights"), I18n.format("gui.panel.emergencylights")};
	
	private final EntityVehicleF_Ground vehicle;
	private final APartEngine engine;
	private final boolean[] hasLight;
	private final int[][] lightButtonCoords;
	private final int[] starterButtonCoords;
	private final int[] sirenButtonCoords;
	private final int[][] trailerButtonCoords;
	
	private byte lastEngineStarted;
	
	public GUIPanelGround(EntityVehicleF_Ground vehicle){
		super();
		this.vehicle = vehicle;
		engine = vehicle.getEngineByNumber((byte) 0);
		hasLight = new boolean[3];
		lightButtonCoords = new int[3][4];
		for(byte i=0; i<lightButtonCoords.length; ++i){
			lightButtonCoords[i] = new int[]{32, 64, 280+50*i+32, 280+50*i};
		}
		starterButtonCoords = new int[]{112, 112 + 32, 280+32, 280};
		sirenButtonCoords = new int[]{112, 112 + 32, 280+32+50, 280+50};
		trailerButtonCoords = new int[8][4];
		for(byte i=0; i<trailerButtonCoords.length; ++i){
			int xOffset = i < 4 ? 192 : 272;
			trailerButtonCoords[i] = new int[]{xOffset, xOffset + 32, 280+50*(i%4)+32, 280+50*(i%4)};
		}
	}
	
	@Override
	public void initGui(){
		for(byte i=0; i<lightButtonCoords.length; ++i){
			hasLight[i] = RenderVehicle.doesVehicleHaveLight(vehicle, lights[i]);
		}
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		//The true span of this GUI is from height-112 to height.
		//This is because it's on the bottom of the screen, or the highest height value.
		if(vehicle.isDead){
			mc.player.closeScreen();
			return;
		}
		
		//If the running lights are on, we want to light up the switches in the panel.
		//This is done in numerous places.
		final boolean lightsOn = RenderInstruments.isPanelIlluminated(vehicle);
		
		//Disable the main HUD overlay if it's active.
		CameraSystem.disableHUD = true;
		
		//Scale this HUD by the current screen ratio.
		GL11.glPushMatrix();
		GL11.glScalef(1.0F*this.width/RenderHUD.screenDefaultX, 1.0F*this.height/RenderHUD.screenDefaultY, 0);
		
		//Render the main backplate and instruments.
		//Note that while this is technically a GUI, the GUI parameter is for the instrument GUI.
		RenderHUD.drawAuxiliaryHUD(vehicle, width, height, false);
		
		//Render the 3 light buttons if we have lights for them.
		for(byte i=0; i<hasLight.length; ++i){
			if(hasLight[i]){
				drawRedstoneButton(lightButtonCoords[i], vehicle.isLightOn(lights[i]));
			}
		}
		
		//Render the engine button.
		//Check if an engine is present and render the status if so.
		//Otherwise just leave the light off.
		drawRedstoneButton(starterButtonCoords, engine != null ? engine.state.running || engine.state.esOn : false);
		
		//Render the siren button if the vehicle has a siren sound.
		if(vehicle.definition.motorized.sirenSound != null){
			drawRedstoneButton(sirenButtonCoords, vehicle.sirenOn);
		}
		
		//Render trailer buttons for hitches.
		EntityVehicleF_Ground currentVehicle = vehicle;
		for(byte i=0; i<trailerButtonCoords.length; ++i){
			if(currentVehicle != null && currentVehicle.definition.motorized.hitchPos != null){
				drawRedstoneButton(trailerButtonCoords[i], currentVehicle.towedVehicle != null);
				currentVehicle = currentVehicle.towedVehicle;
			}else{
				break;
			}
		}
		
		//Render light button text.
		for(byte i=0; i<lightButtonCoords.length; ++i){
			if(hasLight[i]){
				int textX = lightButtonCoords[i][0] + (lightButtonCoords[i][1] - lightButtonCoords[i][0])/2;
				int textY = lightButtonCoords[i][2] + 2; 
				fontRenderer.drawString(lightText[i], textX - fontRenderer.getStringWidth(lightText[i])/2, textY, lightsOn ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
			}
		}
		
		//Render engine button text.
		int textX = starterButtonCoords[0] + (starterButtonCoords[1] - starterButtonCoords[0])/2;
		int textY = starterButtonCoords[2] + 2;
		fontRenderer.drawString(I18n.format("gui.panel.starter"), textX - fontRenderer.getStringWidth(I18n.format("gui.panel.starter"))/2, textY, lightsOn ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
		
		//Render the siren button text if the vehicle has a siren sound.
		if(vehicle.definition.motorized.sirenSound != null){
			textX = sirenButtonCoords[0] + (sirenButtonCoords[1] - sirenButtonCoords[0])/2;
			textY = sirenButtonCoords[2] + 2;
			fontRenderer.drawString(I18n.format("gui.panel.siren"), textX - fontRenderer.getStringWidth(I18n.format("gui.panel.siren"))/2, textY, lightsOn ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
		}
		
		//Render trailer text depending on hitch status.EntityVehicleF_Ground currentVehicle = vehicle;
		currentVehicle = vehicle;
		for(byte i=0; i<trailerButtonCoords.length; ++i){
			if(currentVehicle != null && currentVehicle.definition.motorized.hitchPos != null){
				textX = trailerButtonCoords[i][0] + (trailerButtonCoords[i][1] - trailerButtonCoords[i][0])/2;
				textY = trailerButtonCoords[i][2] + 2;
				fontRenderer.drawString(I18n.format("gui.panel.trailer") + "#" + (i + 1), textX - fontRenderer.getStringWidth(I18n.format("gui.panel.trailer") + "#" + (i + 1))/2, textY, lightsOn ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
				currentVehicle = currentVehicle.towedVehicle;
			}else{
				break;
			}
		}
		GL11.glPopMatrix();
	}
	
	private void drawRedstoneButton(int[] coords, boolean isOn){
		mc.getTextureManager().bindTexture(isOn ? toggleOn : toggleOff);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3d(coords[0], coords[3], 0);
		GL11.glTexCoord2f(0, 1);
		GL11.glVertex3d(coords[0], coords[2], 0);
		GL11.glTexCoord2f(1, 1);
		GL11.glVertex3d(coords[1], coords[2], 0);
		GL11.glTexCoord2f(1, 0);
		GL11.glVertex3d(coords[1], coords[3], 0);
		GL11.glEnd();
	}
	
	@Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException{
		//Scale clicks to the scaled GUI;
		mouseX = (int) (1.0F*mouseX/width*RenderHUD.screenDefaultX);
		mouseY = (int) (1.0F*mouseY/height*RenderHUD.screenDefaultY);
		lastEngineStarted = -1;
		if(mouseY < RenderHUD.screenDefaultY/2){
			mc.player.closeScreen();
		}else{
			//Check if a light button has been pressed.
			for(byte i=0; i<lightButtonCoords.length; ++i){
				if(mouseX > lightButtonCoords[i][0] && mouseX < lightButtonCoords[i][1] && mouseY < lightButtonCoords[i][2] && mouseY > lightButtonCoords[i][3]){
					if(hasLight[i]){
						MTS.MTSNet.sendToServer(new LightPacket(vehicle.getEntityId(), lights[i]));
					}
				}
			}
			
			//Check if the starter button has been pressed.
			//If the engine is running, turn it off.
			//Otherwise, turn the electric starter on.
			if(engine != null){
				if(mouseX > starterButtonCoords[0] && mouseX < starterButtonCoords[1] && mouseY < starterButtonCoords[2] && mouseY > starterButtonCoords[3]){
					MTS.MTSNet.sendToServer(new PacketPartEngineSignal(engine, engine.state.magnetoOn ? PacketEngineTypes.MAGNETO_OFF : PacketEngineTypes.MAGNETO_ON));
				}
			}
			
			//Check if the starter button has been pressed.
			if(engine != null){
				if(mouseX > starterButtonCoords[0] && mouseX < starterButtonCoords[1] && mouseY < starterButtonCoords[2] && mouseY > starterButtonCoords[3]){
					if(engine.state.running){
						MTS.MTSNet.sendToServer(new PacketPartEngineSignal(engine, PacketEngineTypes.MAGNETO_OFF));
					}else if(!engine.state.esOn){
						MTS.MTSNet.sendToServer(new PacketPartEngineSignal(engine, PacketEngineTypes.MAGNETO_ON));
						MTS.MTSNet.sendToServer(new PacketPartEngineSignal(engine, PacketEngineTypes.ES_ON));
						lastEngineStarted = 0;
					}
				}
			}
			
			//Check if the siren button has been pressed.
			if(mouseX > sirenButtonCoords[0] && mouseX < sirenButtonCoords[1] && mouseY < sirenButtonCoords[2] && mouseY > sirenButtonCoords[3]){
				MTS.MTSNet.sendToServer(new SirenPacket(vehicle.getEntityId()));
			}
			
			//Check if any trailer buttons have been pressed.
			EntityVehicleF_Ground currentVehicle = vehicle;
			for(byte i=0; i<trailerButtonCoords.length; ++i){
				if(currentVehicle != null && currentVehicle.definition.motorized.hitchPos != null){
					if(mouseX > trailerButtonCoords[i][0] && mouseX < trailerButtonCoords[i][1] && mouseY < trailerButtonCoords[i][2] && mouseY > trailerButtonCoords[i][3]){
						MTS.MTSNet.sendToServer(new TrailerPacket(currentVehicle.getEntityId()));
					}
					currentVehicle = currentVehicle.towedVehicle;
				}else{
					break;
				}
			}
		}
	}
	
	@Override
	protected void mouseReleased(int mouseX, int mouseY, int actionType){
	    if(actionType == 0){
	    	if(lastEngineStarted != -1){
	    		MTS.MTSNet.sendToServer(new PacketPartEngineSignal(engine, PacketEngineTypes.ES_OFF));
	    	}
	    }
	}
	
	@Override
	public void onGuiClosed(){
		CameraSystem.disableHUD = false;
		if(lastEngineStarted != -1){
    		MTS.MTSNet.sendToServer(new PacketPartEngineSignal(engine, PacketEngineTypes.ES_OFF));
    	}
	}
	
	@Override
	public boolean doesGuiPauseGame(){
		return false;
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException{
		if(keyCode == 1 || mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode) || mc.gameSettings.keyBindSneak.isActiveAndMatches(keyCode)){
			super.keyTyped('0', 1);
        }
	}
}
