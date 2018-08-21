package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle.LightTypes;
import minecrafttransportsimulator.multipart.parts.APartEngine;
import minecrafttransportsimulator.packets.control.LightPacket;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal.PacketEngineTypes;
import minecrafttransportsimulator.rendering.RenderHUD;
import minecrafttransportsimulator.rendering.RenderInstruments;
import minecrafttransportsimulator.rendering.RenderMultipart;
import minecrafttransportsimulator.systems.CameraSystem;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

/**A GUI/control system hybrid, this takes the place of the HUD when called up.
 * Used for controlling engines, lights, trim, and other things.
 * 
 * @author don_bruce
 */
public class GUIPanelAircraft extends GuiScreen{
	private static final ResourceLocation toggleOn = new ResourceLocation("textures/blocks/redstone_lamp_on.png");
	private static final ResourceLocation toggleOff = new ResourceLocation("textures/blocks/redstone_lamp_off.png");
	
	private final EntityMultipartE_Vehicle aircraft;
	private final APartEngine[] engines;
	private final boolean[] hasLight;
	private final LightTypes[] lights = new LightTypes[]{LightTypes.NAVIGATIONLIGHT, LightTypes.STROBELIGHT, LightTypes.TAXILIGHT, LightTypes.LANDINGLIGHT};
	String[] lightText = new String[]{I18n.format("gui.panel.navigationlights"), I18n.format("gui.panel.strobelights"), I18n.format("gui.panel.taxilights"), I18n.format("gui.panel.landinglights")};
	private final int[][] lightButtonCoords;
	private final int[][] magnetoButtonCoords;
	private final int[][] starterButtonCoords;
	
	private byte lastEngineStarted;
	
	public GUIPanelAircraft(EntityMultipartE_Vehicle aircraft){
		super();
		this.aircraft = aircraft;
		engines = new APartEngine[aircraft.getNumberEngineBays()];
		for(byte i=0; i<engines.length; ++i){
			engines[i] = aircraft.getEngineByNumber(i);
		}
		hasLight = new boolean[4];
		lightButtonCoords = new int[4][4];
		magnetoButtonCoords = new int[engines.length][4];
		starterButtonCoords = new int[engines.length][4];
	}
	
	@Override
	public void initGui(){
		for(byte i=0; i<lightButtonCoords.length; ++i){
			hasLight[i] = RenderMultipart.doesMultipartHaveLight(aircraft, lights[i]);
			lightButtonCoords[i] = new int[]{16, 48, 280+50*i+32, 280+50*i};
		}
		for(byte i=0; i<magnetoButtonCoords.length; ++i){
			magnetoButtonCoords[i] = new int[]{64, 96, 280+50*i+32, 280+50*i};
			starterButtonCoords[i] = new int[]{96, 128, 280+50*i+32, 280+50*i};
		}
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		//The true span of this GUI is from height-112 to height.
		//This is because it's on the bottom of the screen, or the highest height value.
		if(aircraft.isDead){
			mc.thePlayer.closeScreen();
			return;
		}
		
		//If the navigation lights are on, we want to light up the switches in the panel.
		//This is done in numerous places.
		final boolean lightsOn = RenderInstruments.lightsOn(aircraft);
		
		//Disable the main HUD overlay if it's active.
		CameraSystem.disableHUD = true;
		
		//Scale this HUD by the current screen ratio.
		GL11.glPushMatrix();
		GL11.glScalef(1.0F*this.width/RenderHUD.screenDefaultX, 1.0F*this.height/RenderHUD.screenDefaultY, 0);
		
		//Render the main backplate and instruments.
		//Note that while this is technically a GUI, the GUI parameter is for the instrument GUI.
		RenderHUD.drawAuxiliaryHUD(aircraft, width, height, false);
		
		//Render the 4 light buttons if we have lights for them.
		for(byte i=0; i<hasLight.length; ++i){
			if(hasLight[i]){
				drawRedstoneButton(lightButtonCoords[i], aircraft.isLightOn(lights[i]));
			}
		}
		
		//Render the engine buttons.
		//Check if an engine is present and render the status if so.
		//Otherwise just leave the lights off.
		for(byte i=0; i<magnetoButtonCoords.length; ++i){
			drawRedstoneButton(magnetoButtonCoords[i], engines[i] != null ? engines[i].state.magnetoOn : false);
			drawRedstoneButton(starterButtonCoords[i], engines[i] != null ? engines[i].state.esOn : false);
		}

		//Render light button text.
		for(byte i=0; i<lightButtonCoords.length; ++i){
			if(hasLight[i]){
				int textX = lightButtonCoords[i][0] + (lightButtonCoords[i][1] - lightButtonCoords[i][0])/2;
				int textY = lightButtonCoords[i][2] + 2; 
				fontRendererObj.drawString(lightText[i], textX - fontRendererObj.getStringWidth(lightText[i])/2, textY, lightsOn ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
			}
		}
		
		//Render engine button text.
		for(byte i=0; i<magnetoButtonCoords.length; ++i){
			int textX = magnetoButtonCoords[i][0] + (magnetoButtonCoords[i][1] - magnetoButtonCoords[i][0])/2;
			int textY = magnetoButtonCoords[i][2] + 2;
			fontRendererObj.drawString(I18n.format("gui.panel.magneto"), textX - fontRendererObj.getStringWidth(I18n.format("gui.panel.magneto"))/2, textY, lightsOn ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
			fontRendererObj.drawString(I18n.format("gui.panel.starter"), textX - fontRendererObj.getStringWidth(I18n.format("gui.panel.starter"))/2 + 32, textY, lightsOn ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
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
    protected void mouseClicked(int x, int y, int button){
		//Scale clicks to the scaled GUI;
		x = (int) (1.0F*x/width*RenderHUD.screenDefaultX);
		y = (int) (1.0F*y/height*RenderHUD.screenDefaultY);
		lastEngineStarted = -1;
		if(y < RenderHUD.screenDefaultY/2){
			mc.thePlayer.closeScreen();
		}else{
			//Check if a light button has been pressed.
			for(byte i=0; i<lightButtonCoords.length; ++i){
				if(x > lightButtonCoords[i][0] && x < lightButtonCoords[i][1] && y < lightButtonCoords[i][2] && y > lightButtonCoords[i][3]){
					if(hasLight[i]){
						MTS.MTSNet.sendToServer(new LightPacket(aircraft.getEntityId(), lights[i]));
					}
				}
			}
			
			//Check if a magneto button has been pressed.
			for(byte i=0; i<magnetoButtonCoords.length; ++i){
				if(engines[i] != null){
					if(x > magnetoButtonCoords[i][0] && x < magnetoButtonCoords[i][1] && y < magnetoButtonCoords[i][2] && y > magnetoButtonCoords[i][3]){
						MTS.MTSNet.sendToServer(new PacketPartEngineSignal(engines[i], engines[i].state.magnetoOn ? PacketEngineTypes.MAGNETO_OFF : PacketEngineTypes.MAGNETO_ON));
					}
				}
			}
			
			//Check if a starter button has been pressed.
			for(byte i=0; i<starterButtonCoords.length; ++i){
				if(engines[i] != null){
					if(x > starterButtonCoords[i][0] && x < starterButtonCoords[i][1] && y < starterButtonCoords[i][2] && y > starterButtonCoords[i][3]){
						if(!engines[i].state.esOn){
							MTS.MTSNet.sendToServer(new PacketPartEngineSignal(engines[i], PacketEngineTypes.ES_ON));
						}
						lastEngineStarted = i;
					}
				}
			}
		}
	}
	
	@Override
	protected void mouseReleased(int mouseX, int mouseY, int actionType){
	    if(actionType == 0){
	    	if(lastEngineStarted != -1 && starterButtonCoords.length > 0){
	    		MTS.MTSNet.sendToServer(new PacketPartEngineSignal(engines[lastEngineStarted], PacketEngineTypes.ES_OFF));
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
