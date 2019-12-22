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
	private final int[] magnetoButtonCoords;
	private final int[] starterButtonCoords;
	private final int[] sirenButtonCoords;
	private final int[] trailerButtonCoords;
	private final int[] trailer2ButtonCoords;
	
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
		magnetoButtonCoords = new int[]{96, 96 + 32, 280+32, 280};
		starterButtonCoords = new int[]{128, 128 + 32, 280+32, 280};
		sirenButtonCoords = new int[]{112, 112 + 32, 280+32+50, 280+50};
		trailerButtonCoords = new int[]{112, 112 + 32, 280+32+100, 280+100};
		trailer2ButtonCoords = new int[]{112, 112 + 32, 280+32+150, 280+150};
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
		final boolean lightsOn = RenderInstruments.lightsOn(vehicle);
		
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
		
		//Render the engine buttons.
		//Check if an engine is present and render the status if so.
		//Otherwise just leave the lights off.
		drawRedstoneButton(magnetoButtonCoords, engine != null ? engine.state.magnetoOn : false);
		drawRedstoneButton(starterButtonCoords, engine != null ? engine.state.esOn : false);
		
		//Render the siren button if the vehicle has a siren sound.
		if(vehicle.pack.motorized.sirenSound != null){
			drawRedstoneButton(sirenButtonCoords, vehicle.sirenOn);
		}
		
		//Render trailer button if we have a hitch.
		//Render second trailer button if trailer has hitch.
		if(vehicle.pack.motorized.hitchPos != null){
			drawRedstoneButton(trailerButtonCoords, vehicle.towedVehicle != null);
			if(vehicle.towedVehicle != null && vehicle.towedVehicle.pack.motorized.hitchPos != null){
				drawRedstoneButton(trailer2ButtonCoords, vehicle.towedVehicle.towedVehicle != null);
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
		int textX = magnetoButtonCoords[0] + (magnetoButtonCoords[1] - magnetoButtonCoords[0])/2;
		int textY = magnetoButtonCoords[2] + 2;
		fontRenderer.drawString(I18n.format("gui.panel.powerswitch"), textX - fontRenderer.getStringWidth(I18n.format("gui.panel.powerswitch"))/2, textY, lightsOn ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
		fontRenderer.drawString(I18n.format("gui.panel.starter"), textX - fontRenderer.getStringWidth(I18n.format("gui.panel.starter"))/2 + 32, textY, lightsOn ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
		
		//Render the siren button text if the vehicle has a siren sound.
		if(vehicle.pack.motorized.sirenSound != null){
			textX = sirenButtonCoords[0] + (sirenButtonCoords[1] - sirenButtonCoords[0])/2;
			textY = sirenButtonCoords[2] + 2;
			fontRenderer.drawString(I18n.format("gui.panel.siren"), textX - fontRenderer.getStringWidth(I18n.format("gui.panel.siren"))/2, textY, lightsOn ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
		}
		
		//Render trailer text depending on hitch status.
		if(vehicle.pack.motorized.hitchPos != null){
			textX = trailerButtonCoords[0] + (trailerButtonCoords[1] - trailerButtonCoords[0])/2;
			textY = trailerButtonCoords[2] + 2;
			fontRenderer.drawString(I18n.format("gui.panel.trailer"), textX - fontRenderer.getStringWidth(I18n.format("gui.panel.trailer"))/2, textY, lightsOn ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
			if(vehicle.towedVehicle != null && vehicle.towedVehicle.pack.motorized.hitchPos != null){
				textX = trailer2ButtonCoords[0] + (trailer2ButtonCoords[1] - trailer2ButtonCoords[0])/2;
				textY = trailer2ButtonCoords[2] + 2;
				fontRenderer.drawString(I18n.format("gui.panel.secondtrailer"), textX - fontRenderer.getStringWidth(I18n.format("gui.panel.secondtrailer"))/2, textY, lightsOn ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
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
			
			//Check if the magneto button has been pressed.
			if(engine != null){
				if(mouseX > magnetoButtonCoords[0] && mouseX < magnetoButtonCoords[1] && mouseY < magnetoButtonCoords[2] && mouseY > magnetoButtonCoords[3]){
					MTS.MTSNet.sendToServer(new PacketPartEngineSignal(engine, engine.state.magnetoOn ? PacketEngineTypes.MAGNETO_OFF : PacketEngineTypes.MAGNETO_ON));
				}
			}
			
			//Check if the starter button has been pressed.
			if(engine != null){
				if(mouseX > starterButtonCoords[0] && mouseX < starterButtonCoords[1] && mouseY < starterButtonCoords[2] && mouseY > starterButtonCoords[3]){
					if(!engine.state.esOn){
						MTS.MTSNet.sendToServer(new PacketPartEngineSignal(engine, PacketEngineTypes.ES_ON));
					}
					lastEngineStarted = 0;
				}
			}
			
			//Check if the siren button has been pressed.
			if(mouseX > sirenButtonCoords[0] && mouseX < sirenButtonCoords[1] && mouseY < sirenButtonCoords[2] && mouseY > sirenButtonCoords[3]){
				MTS.MTSNet.sendToServer(new SirenPacket(vehicle.getEntityId()));
			}
			
			//Check if the trailer buttons has been pressed.
			if(vehicle.pack.motorized.hitchPos != null){
				if(mouseX > trailerButtonCoords[0] && mouseX < trailerButtonCoords[1] && mouseY < trailerButtonCoords[2] && mouseY > trailerButtonCoords[3]){
					MTS.MTSNet.sendToServer(new TrailerPacket(vehicle.getEntityId()));
				}else if(vehicle.towedVehicle != null && vehicle.towedVehicle.pack.motorized.hitchPos != null){
					if(mouseX > trailer2ButtonCoords[0] && mouseX < trailer2ButtonCoords[1] && mouseY < trailer2ButtonCoords[2] && mouseY > trailer2ButtonCoords[3]){
						MTS.MTSNet.sendToServer(new TrailerPacket(vehicle.towedVehicle.getEntityId()));
					}
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
