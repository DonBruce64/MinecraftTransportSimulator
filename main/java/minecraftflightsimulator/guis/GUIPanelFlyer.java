package minecraftflightsimulator.guis;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityVehicle;
import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.minecrafthelpers.PlayerHelper;
import minecraftflightsimulator.packets.control.EnginePacket;
import minecraftflightsimulator.packets.control.LightPacket;
import minecraftflightsimulator.rendering.AircraftInstruments;
import minecraftflightsimulator.rendering.VehicleHUDs;
import minecraftflightsimulator.systems.CameraSystem;
import minecraftflightsimulator.systems.ConfigSystem;
import minecraftflightsimulator.systems.GL11DrawSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

/**A GUI/control system hybrid, this takes the place of the HUD when called up.
 * Used for controlling engines, lights, trim, and other things.
 * 
 * @author don_bruce
 */
public class GUIPanelFlyer extends GuiScreen{
	private static final ResourceLocation toggleOn = new ResourceLocation("textures/blocks/redstone_lamp_on.png");
	private static final ResourceLocation toggleOff = new ResourceLocation("textures/blocks/redstone_lamp_off.png");
	private static final float[] blackColor = new float[]{0,0,0};
	
	private final ResourceLocation backplateTexture;
	private final ResourceLocation moldingTexture;
	private final EntityVehicle vehicle;
	private final EntityEngine[] engines;
	private final boolean electricStartEnabled = ConfigSystem.getBooleanConfig("ElectricStart");
	
	private byte lastEngineStarted;
	
	public GUIPanelFlyer(EntityVehicle vehicle){
		super();
		this.vehicle = vehicle;
		this.backplateTexture = vehicle.getBackplateTexture();
		this.moldingTexture = vehicle.getMouldingTexture();
		engines = new EntityEngine[vehicle.getNumberEngineBays()];
		for(byte i=0; i<engines.length; ++i){
			engines[i] = vehicle.getEngineByNumber(i);
		}
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		if(vehicle.isDead){
			mc.thePlayer.closeScreen();
			return;
		}
		
		CameraSystem.disableHUD = true;
		VehicleHUDs.startHUDDraw(vehicle);
		VehicleHUDs.drawPanelPanel(width, height);
		if(VehicleHUDs.areLightsOn(vehicle)){
			Minecraft.getMinecraft().entityRenderer.disableLightmap(0);
		}
		for(byte i=0; i<engines.length; ++i){
			GL11DrawSystem.bindTexture(engines[i] != null ? (engines[i].state.magnetoOn ? toggleOn : toggleOff) : toggleOff);
			GL11DrawSystem.renderSquare((2+i)*width/(2 + engines.length)-18, (2+i)*width/(2 + engines.length)-2, height-12+8, height-12-8, 0, 0, false);
			if(electricStartEnabled){
				GL11DrawSystem.bindTexture(engines[i] != null ? (engines[i].state.esOn ? toggleOn : toggleOff) : toggleOff);
				GL11DrawSystem.renderSquare((2+i)*width/(2 + engines.length)+2, (2+i)*width/(2 + engines.length)+18, height-12+8, height-12-8, 0, 0, false);
			}
		}
		for(byte i=1; i<=4; ++i){
			if(((vehicle.lightSetup & 1<<(i-1)) == 1<<(i-1))){
				GL11DrawSystem.bindTexture(((vehicle.lightStatus & 1<<(i-1)) == 1<<(i-1)) ? toggleOn : toggleOff);
				GL11DrawSystem.renderSquare(width/10-18, width/10, height-104+(i*25), height-120+i*25, 0, 0, false);
			}
		}
		for(byte i=0; i<engines.length; ++i){
			GL11DrawSystem.drawScaledStringAt(PlayerHelper.getTranslatedText("gui.panel.magneto"), (2+i)*width/(2 + engines.length)-12, height-26, 0, 0.6F, Color.WHITE);
			if(electricStartEnabled){
				GL11DrawSystem.drawScaledStringAt(PlayerHelper.getTranslatedText("gui.panel.starter"), (2+i)*width/(2 + engines.length)+12, height-26, 0, 0.6F, Color.WHITE);
			}
		}
		for(byte i=1; i<=4; ++i){
			if(((vehicle.lightSetup & 1<<(i-1)) == 1<<(i-1))){
				if(i==1){
					GL11DrawSystem.drawScaledStringAt(PlayerHelper.getTranslatedText("gui.panel.navigationlights"), width/10-10, height-126+(i*25), 0, 0.6F, Color.WHITE);
				}else if(i==2){
					GL11DrawSystem.drawScaledStringAt(PlayerHelper.getTranslatedText("gui.panel.strobelights"), width/10-10, height-126+(i*25), 0, 0.6F, Color.WHITE);
				}else if(i==3){
					GL11DrawSystem.drawScaledStringAt(PlayerHelper.getTranslatedText("gui.panel.taxilights"), width/10-10, height-126+(i*25), 0, 0.6F, Color.WHITE);
				}else if(i==4){
					GL11DrawSystem.drawScaledStringAt(PlayerHelper.getTranslatedText("gui.panel.landinglights"), width/10-10, height-126+(i*25), 0, 0.6F, Color.WHITE);
				}
			}
		}
		for(byte i=0; i<engines.length; ++i){
			GL11.glPushMatrix();
			GL11.glTranslatef((2+i)*width/(2 + engines.length), height - 72, 0);
			GL11.glScalef(0.60F, 0.60F, 0.60F);
			AircraftInstruments.drawFlyableInstrument(vehicle, -30, -30, vehicle.instruments.get((byte) (i*10 + 10)) != null ? vehicle.instruments.get((byte) (i*10 + 10)) : -1, true, i);
			AircraftInstruments.drawFlyableInstrument(vehicle, 30, -30, vehicle.instruments.get((byte) (i*10 + 11)) != null ? vehicle.instruments.get((byte) (i*10 + 11)) : -1, true, i);
			AircraftInstruments.drawFlyableInstrument(vehicle, -30, 30, vehicle.instruments.get((byte) (i*10 + 12)) != null ? vehicle.instruments.get((byte) (i*10 + 12)) : -1, true, i);
			AircraftInstruments.drawFlyableInstrument(vehicle, 30, 30, vehicle.instruments.get((byte) (i*10 + 13)) != null ? vehicle.instruments.get((byte) (i*10 + 13)) : -1, true, i);
			GL11.glPopMatrix();
		}

		VehicleHUDs.endHUDDraw();
	}
	
	@Override
    protected void mouseClicked(int x, int y, int button){
		lastEngineStarted = -1;
		if(y < this.height - 128){
			mc.thePlayer.closeScreen();
		}else if(x >= width/10-18 && x <= width/10-2){
			for(byte i=1; i<=4; ++i){
				if(((vehicle.lightSetup & 1<<(i-1)) == 1<<(i-1))){
					if(y >= height-120+(i*25) && y <= height-104+i*25){
						MFS.MFSNet.sendToServer(new LightPacket(vehicle.getEntityId(), (byte) (1<<(i-1))));
					}
				}
			}
		}else if(y >= height-12-8 && y <= height-12+8){
			for(byte i=0; i<engines.length; ++i){
				if(engines[i] != null){
					if(engines[i].parent != null){
						if(x >= (2+i)*width/(2 + engines.length)-18 && x <= (2+i)*width/(2 + engines.length)-2){
							MFS.MFSNet.sendToServer(new EnginePacket(engines[i].parent.getEntityId(), engines[i].getEntityId(), engines[i].state.magnetoOn ? (byte) 0 : (byte) 1));
						}else if(x >= (2+i)*width/(2 + engines.length)+2 && x <= (2+i)*width/(2 + engines.length)+18){
							if(electricStartEnabled){
								if(!engines[i].state.esOn){
									MFS.MFSNet.sendToServer(new EnginePacket(engines[i].parent.getEntityId(), engines[i].getEntityId(), (byte) 3));
								}
								lastEngineStarted = i;
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	protected void mouseMovedOrUp(int mouseX, int mouseY, int actionType){
	    if(actionType == 0){
	    	if(lastEngineStarted != -1 && electricStartEnabled){
	    		MFS.MFSNet.sendToServer(new EnginePacket(engines[lastEngineStarted].parent.getEntityId(), engines[lastEngineStarted].getEntityId(), (byte) 2));
	    	}
	    }
	}
	
    @Override
    protected void keyTyped(char key, int bytecode){
    	super.keyTyped(key, bytecode);
    	if(bytecode == Minecraft.getMinecraft().gameSettings.keyBindInventory.getKeyCode() || bytecode == Minecraft.getMinecraft().gameSettings.keyBindSneak.getKeyCode()){
    		super.keyTyped('0', 1);
    	}
    }
	
	@Override
	public boolean doesGuiPauseGame(){
		return false;
	}
	
	@Override
	public void onGuiClosed(){
		CameraSystem.disableHUD = false;
	}
}
