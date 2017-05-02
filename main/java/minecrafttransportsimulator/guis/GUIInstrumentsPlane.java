package minecrafttransportsimulator.guis;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.minecrafthelpers.PlayerHelper;
import minecrafttransportsimulator.packets.general.InstrumentPlanePacket;
import minecrafttransportsimulator.rendering.AircraftInstruments;
import minecrafttransportsimulator.rendering.AircraftInstruments.AircraftGauges;
import minecrafttransportsimulator.rendering.PlaneHUD;
import minecrafttransportsimulator.systems.GL11DrawSystem;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class GUIInstrumentsPlane extends GuiScreen{
	private final EntityPlane plane;
	private final EntityPlayer player;
	private final byte numberEngineBays;
	private final Map<Float[], Byte> gaugeCoords;
	
	private boolean fault;
	private byte lastInstrumentClicked = -1;
	private int lastClickedX;
	private int lastClickedY;
	
	public GUIInstrumentsPlane(EntityPlane plane, EntityPlayer player){
		this.plane = plane;
		this.player = player;
		numberEngineBays = plane.getNumberEngineBays();
		gaugeCoords = new HashMap<Float[], Byte>();
	}
	
	@Override
	public void initGui(){
		if(plane.instruments.get((byte) 0) != null){
			gaugeCoords.put(new Float[]{width*17/64*0.5F*0.75F, (height - 88)*4/3*0.5F*0.75F}, (byte) 0);
		}
		for(byte i=1; i<4; ++i){
			if(plane.instruments.get(i) != null){
				gaugeCoords.put(new Float[]{(5*i+6)*width/32*0.5F, (height-96)*0.5F}, i);
			}
		}
		if(plane.instruments.get((byte) 4) != null){
			gaugeCoords.put(new Float[]{width*17/16*0.5F*0.75F, (height - 88)*4/3*0.5F*0.75F}, (byte) 4);
		}
		for(byte i=5; i<10; ++i){
			if(plane.instruments.get(i) != null){
				gaugeCoords.put(new Float[]{(5*(i-5)+6)*width/32*0.5F, (height-32)*0.5F}, i);
			}
		}
		for(byte i=0; i<numberEngineBays; ++i){
			gaugeCoords.put(new Float[]{((2+i)*width/(2 + numberEngineBays) + width - 15)*0.5F, (height - 72 - 15)*0.5F}, (byte) (10*i+10));
			gaugeCoords.put(new Float[]{((2+i)*width/(2 + numberEngineBays) + width + 15)*0.5F, (height - 72 - 15)*0.5F}, (byte) (10*i+11));
			gaugeCoords.put(new Float[]{((2+i)*width/(2 + numberEngineBays) + width - 15)*0.5F, (height - 72 + 15)*0.5F}, (byte) (10*i+12));
			gaugeCoords.put(new Float[]{((2+i)*width/(2 + numberEngineBays) + width + 15)*0.5F, (height - 72 + 15)*0.5F}, (byte) (10*i+13));
		}
	}

	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		if(plane.isDead || player.getDistance(plane.posX, plane.posY, plane.posZ) > 20){
			mc.thePlayer.closeScreen();
			return;
		}
		
		this.drawDefaultBackground();
		GL11.glPushMatrix();
		GL11.glScalef(0.5F, 0.5F, 0.5F);
		PlaneHUD.startHUDDraw(plane);
		height-=64;
		PlaneHUD.drawLeftPlanePanel(width, height);
		if(plane.instruments.get((byte) 0) != null){
			GL11.glPushMatrix();
			GL11.glScalef(0.75F, 0.75F, 0.75F);
			GL11DrawSystem.bindTexture(AircraftInstruments.gauges[plane.instruments.get((byte) 0)]);
			GL11DrawSystem.renderSquare(width*17/64-20, width*17/64+20, (height - 24)*4/3+20, (height - 24)*4/3-20, 0, 0, false);
			GL11.glPopMatrix();
		}
		
		PlaneHUD.drawUpperPlanePanel(width, height);
		for(byte i=1; i<4; ++i){
			if(plane.instruments.get(i) != null){
				GL11.glPushMatrix();
				GL11DrawSystem.bindTexture(AircraftInstruments.gauges[plane.instruments.get((byte) i)]);
				GL11DrawSystem.renderSquare((5*i+6)*width/32-20, (5*i+6)*width/32+20, height-32+20, height-32-20, 0, 0, false);
				GL11.glPopMatrix();
			}
		}
		
		PlaneHUD.drawRightPlanePanel(width, height);
		if(plane.instruments.get((byte) 4) != null){
			GL11.glPushMatrix();
			GL11.glScalef(0.75F, 0.75F, 0.75F);
			GL11DrawSystem.bindTexture(AircraftInstruments.gauges[plane.instruments.get((byte) 4)]);
			GL11DrawSystem.renderSquare(width*17/16-20, width*17/16+20, (height - 24)*4/3+20, (height - 24)*4/3-20, 0, 0, false);
			GL11.glPopMatrix();
		}
		
		height+=64;
		PlaneHUD.drawLowerPlanePanel(width, height);
		for(byte i=5; i<10; ++i){
	    	if(plane.instruments.get(i) != null){
				GL11.glPushMatrix();
				GL11DrawSystem.bindTexture(AircraftInstruments.gauges[plane.instruments.get((byte) i)]);
				GL11DrawSystem.renderSquare((5*(i-5)+6)*width/32-20, (5*(i-5)+6)*width/32+20, height-32+20, height-32-20, 0, 0, false);
				GL11.glPopMatrix();
	    	}
		}
		

		GL11.glTranslatef(width, 0, 0);
		PlaneHUD.drawPanelPanel(width, height);
		byte numberEngineBays = plane.getNumberEngineBays();
		for(byte i=0; i<numberEngineBays; ++i){
			GL11.glPushMatrix();
			GL11.glTranslatef((2+i)*width/(2 + numberEngineBays), height - 72, 0);
			GL11.glScalef(0.60F, 0.60F, 0.60F);
	    	GL11DrawSystem.bindTexture(AircraftInstruments.gauges[plane.instruments.get((byte) (i*10 + 10))]);
	    	GL11DrawSystem.renderSquare(-60, 0, 0, -60, 0, 0, false);
	    	GL11DrawSystem.bindTexture(AircraftInstruments.gauges[plane.instruments.get((byte) (i*10 + 11))]);
	    	GL11DrawSystem.renderSquare(0, 60, 0, -60, 0, 0, false);
	    	GL11DrawSystem.bindTexture(AircraftInstruments.gauges[plane.instruments.get((byte) (i*10 + 12))]);
	    	GL11DrawSystem.renderSquare(-60, 0, 60, 0, 0, 0, false);
	    	GL11DrawSystem.bindTexture(AircraftInstruments.gauges[plane.instruments.get((byte) (i*10 + 13))]);
	    	GL11DrawSystem.renderSquare(0, 60, 60, 0, 0, 0, false);
			GL11.glPopMatrix();
		}
		PlaneHUD.endHUDDraw();
		GL11.glPopMatrix();
		
		if(lastClickedX != 0 && lastClickedY != 0){
			GL11.glPushMatrix();
	    	GL11.glColor4f(1, 1, 1, 1);
	    	GL11.glDisable(GL11.GL_TEXTURE_2D);
	    	GL11.glEnable(GL11.GL_BLEND);
	    	GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
	    	GL11.glBegin(GL11.GL_QUADS);
	    	GL11.glVertex2d(lastClickedX - 10, lastClickedY + 10);
	    	GL11.glVertex2d(lastClickedX + 10, lastClickedY + 10);
	    	GL11.glVertex2d(lastClickedX + 10, lastClickedY - 10);
	    	GL11.glVertex2d(lastClickedX - 10, lastClickedY - 10);
	    	GL11.glEnd();
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glPopMatrix();
		}
		
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_BLEND);
		GL11DrawSystem.drawScaledStringAt(I18n.format("gui.instruments.main"), width/4, 32, 0, 1.5F, Color.WHITE);
		GL11DrawSystem.drawScaledStringAt(I18n.format("gui.instruments.control"), 3*width/4, 32, 0, 1.5F, Color.WHITE);
		if(lastInstrumentClicked == -1){
			if(plane.ticksExisted%40 >= 20){
				GL11DrawSystem.drawScaledStringAt(I18n.format("gui.instruments.idle"), width/2, height/2 + 32, 0, 1.5F, Color.WHITE);
			}
		}else{
			GL11DrawSystem.drawScaledStringAt(String.valueOf(lastInstrumentClicked), 10, 5, 0, 1.5F, Color.WHITE);
			if(!fault){
				GL11DrawSystem.drawScaledStringAt(I18n.format("gui.instruments.decide"), width/2, height/2 + 8, 0, 1.5F, Color.WHITE);
			}else{
				if(plane.ticksExisted%20 >= 10){
					GL11DrawSystem.drawScaledStringAt(I18n.format("gui.instruments.fault"), width/2, height/2 + 8, 0, 1.5F, Color.RED);
				}
			}
			boolean[] hasInstrument = new boolean[AircraftGauges.values().length];
			for(byte i=1; i<AircraftGauges.values().length; ++i){
				if(player.capabilities.isCreativeMode){
					hasInstrument[i] = true;
				}else{
					hasInstrument[i] = PlayerHelper.getQtyOfItemInInventory(MTSRegistry.flightInstrument, i, player) > 0;
				}
			}
			for(byte i=1; i<AircraftGauges.values().length; ++i){
				if(hasInstrument[i]){
					GL11.glPushMatrix();
					GL11DrawSystem.bindTexture(AircraftInstruments.gauges[i]);
					if(i<9){
						GL11DrawSystem.renderSquare(((i-1)%8+1)*width/19-10, ((i-1)%8+1)*width/19+10, height*0.7 + 10, height*0.7 - 10, 0, 0, false);
					}else{
						GL11DrawSystem.renderSquare(((i-1)%8+1)*width/19-10, ((i-1)%8+1)*width/19+10, height*0.9 + 10, height*0.9 - 10, 0, 0, false);
					}
					GL11.glPopMatrix();
				}
			}
			GL11DrawSystem.bindTexture(AircraftInstruments.gauges[0]);
			GL11DrawSystem.renderSquare(width/2, width/2+height*0.2, height*0.9, height*0.7, 0, 0, false);
			GL11DrawSystem.drawScaledStringAt(I18n.format("gui.instruments.clear"), width/2+height*0.1F, height*0.775F, 0, 1.0F, Color.WHITE);
		}
		GL11.glPopMatrix();
	}
	
	@Override
    protected void mouseClicked(int x, int y, int button){
		if(lastClickedX == 0 && lastClickedY == 0){
			for(Entry<Float[], Byte> entry : gaugeCoords.entrySet()){
				float xCenter = entry.getKey()[0];
				float yCenter = entry.getKey()[1];
				if(Math.abs(x - xCenter) <= 10 && Math.abs(y - yCenter) <= 10){
					lastClickedX = (int) xCenter;
					lastClickedY = (int) yCenter;
					lastInstrumentClicked = entry.getValue();
					return;
				}
			}
		}else{
			boolean[] hasInstrument = new boolean[AircraftGauges.values().length];
			for(byte i=1; i<AircraftGauges.values().length; ++i){
				if(player.capabilities.isCreativeMode){
					hasInstrument[i] = true;
				}else{
					hasInstrument[i] = PlayerHelper.getQtyOfItemInInventory(MTSRegistry.flightInstrument, i, player) > 0;
				}
			}
			for(byte i=1; i<AircraftGauges.values().length; ++i){
				if(x >= ((i-1)%8+1)*width/19-10 && x <= ((i-1)%8+1)*width/19+10){
					if(y >= height*((i-1) < 8 ? 0.7 : 0.9) - 10 && y <= height*((i-1) < 8 ? 0.7 : 0.9) + 10){
						if(hasInstrument[i]){
							if(lastInstrumentClicked > 10 && (i != 11 && i != 13 && i != 14 && i != 15)){
								fault = true;
								return;
							}else{
								fault = false;
							}
							MTS.MFSNet.sendToServer(new InstrumentPlanePacket(plane.getEntityId(), player.getEntityId(), lastInstrumentClicked, i));
							lastClickedX = 0;
							lastClickedY = 0;
							lastInstrumentClicked = -1;
						}
						return;
					}
				}
			}
			if(x >= width/2 && x<= width/2+height*0.2 && y >= height*0.7 && y <= height*0.9){
				if(plane.instruments.get(lastInstrumentClicked) != 0){
					MTS.MFSNet.sendToServer(new InstrumentPlanePacket(plane.getEntityId(), player.getEntityId(), lastInstrumentClicked, (byte) 0));
				}
			}
			lastClickedX = 0;
			lastClickedY = 0;
			lastInstrumentClicked = -1;
		}
	}
	
	@Override
	public boolean doesGuiPauseGame(){
		return false;
	}
}