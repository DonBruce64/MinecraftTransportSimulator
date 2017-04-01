package minecraftflightsimulator.guis;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityVehicle;
import minecraftflightsimulator.minecrafthelpers.PlayerHelper;
import minecraftflightsimulator.packets.general.InstrumentFlyerPacket;
import minecraftflightsimulator.registry.MTSRegistry;
import minecraftflightsimulator.rendering.AircraftInstruments.AircraftGauges;
import minecraftflightsimulator.rendering.VehicleHUDs;
import minecraftflightsimulator.systems.GL11DrawSystem;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class GUIInstrumentsFlyer extends GuiScreen{
	private static final ResourceLocation emptyGauge = new ResourceLocation("mfs", "textures/items/flightinstrumentbase.png");
	private static final ResourceLocation[] gauges = getGaugeTextures();
	
	private final EntityVehicle vehicle;
	private final EntityPlayer player;
	private final byte numberEngineBays;
	private final Map<Float[], Byte> gaugeCoords;
	
	private boolean fault;
	private byte lastInstrumentClicked = -1;
	private int lastClickedX;
	private int lastClickedY;
	
	public GUIInstrumentsFlyer(EntityVehicle vehicle, EntityPlayer player){
		this.vehicle = vehicle;
		this.player = player;
		numberEngineBays = vehicle.getNumberEngineBays();
		gaugeCoords = new HashMap<Float[], Byte>();
	}
	
	@Override
	public void initGui(){
		gaugeCoords.put(new Float[]{width*17/64*0.5F*0.75F, (height - 88)*4/3*0.5F*0.75F}, (byte) 0);
		for(byte i=1; i<4; ++i){
			gaugeCoords.put(new Float[]{(5*i+6)*width/32*0.5F, (height-96)*0.5F}, i);
		}
		gaugeCoords.put(new Float[]{width*17/16*0.5F*0.75F, (height - 88)*4/3*0.5F*0.75F}, (byte) 4);
		for(byte i=5; i<10; ++i){
			gaugeCoords.put(new Float[]{(5*(i-5)+6)*width/32*0.5F, (height-32)*0.5F}, i);
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
		if(vehicle.isDead || player.getDistance(vehicle.posX, vehicle.posY, vehicle.posZ) > 20){
			mc.thePlayer.closeScreen();
			return;
		}
		
		this.drawDefaultBackground();
		GL11.glPushMatrix();
		GL11.glScalef(0.5F, 0.5F, 0.5F);
		VehicleHUDs.startHUDDraw(vehicle);
		height-=64;
		VehicleHUDs.drawLeftPlanePanel(width, height);
		if(vehicle.instruments.get((byte) 0) == null || vehicle.instruments.get((byte) 0) != -1){
			GL11.glPushMatrix();
			GL11.glScalef(0.75F, 0.75F, 0.75F);
			GL11DrawSystem.bindTexture(vehicle.instruments.get((byte) 0) == null ? emptyGauge : gauges[vehicle.instruments.get((byte) 0)]);
			GL11DrawSystem.renderSquare(width*17/64-20, width*17/64+20, (height - 24)*4/3+20, (height - 24)*4/3-20, 0, 0, false);
			GL11.glPopMatrix();
		}
		
		VehicleHUDs.drawUpperPlanePanel(width, height);
		for(byte i=1; i<4; ++i){
			GL11.glPushMatrix();
			GL11DrawSystem.bindTexture(vehicle.instruments.get((byte) i) == null ? emptyGauge : gauges[vehicle.instruments.get((byte) i)]);
			GL11DrawSystem.renderSquare((5*i+6)*width/32-20, (5*i+6)*width/32+20, height-32+20, height-32-20, 0, 0, false);
			GL11.glPopMatrix();
		}
		
		VehicleHUDs.drawRightPlanePanel(width, height);
		if(vehicle.instruments.get((byte) 4) == null || vehicle.instruments.get((byte) 4) != -1){
			GL11.glPushMatrix();
			GL11.glScalef(0.75F, 0.75F, 0.75F);
			GL11DrawSystem.bindTexture(vehicle.instruments.get((byte) 4) == null ? emptyGauge : gauges[vehicle.instruments.get((byte) 4)]);
			GL11DrawSystem.renderSquare(width*17/16-20, width*17/16+20, (height - 24)*4/3+20, (height - 24)*4/3-20, 0, 0, false);
			GL11.glPopMatrix();
		}
		
		height+=64;
		VehicleHUDs.drawLowerPlanePanel(width, height);
		for(byte i=5; i<10; ++i){
	    	if(vehicle.instruments.get(i) == null || vehicle.instruments.get(i) != -1){
				GL11.glPushMatrix();
				GL11DrawSystem.bindTexture(vehicle.instruments.get((byte) i) == null ? emptyGauge : gauges[vehicle.instruments.get((byte) i)]);
				GL11DrawSystem.renderSquare((5*(i-5)+6)*width/32-20, (5*(i-5)+6)*width/32+20, height-32+20, height-32-20, 0, 0, false);
				GL11.glPopMatrix();
	    	}
		}
		

		GL11.glTranslatef(width, 0, 0);
		VehicleHUDs.drawPanelPanel(width, height);
		byte numberEngineBays = vehicle.getNumberEngineBays();
		for(byte i=0; i<numberEngineBays; ++i){
			GL11.glPushMatrix();
			GL11.glTranslatef((2+i)*width/(2 + numberEngineBays), height - 72, 0);
			GL11.glScalef(0.60F, 0.60F, 0.60F);
	    	GL11DrawSystem.bindTexture(vehicle.instruments.get((byte) (i*10 + 10)) == null ? emptyGauge : gauges[vehicle.instruments.get((byte) (i*10 + 10))]);
	    	GL11DrawSystem.renderSquare(-60, 0, 0, -60, 0, 0, false);
	    	GL11DrawSystem.bindTexture(vehicle.instruments.get((byte) (i*10 + 11)) == null ? emptyGauge : gauges[vehicle.instruments.get((byte) (i*10 + 11))]);
	    	GL11DrawSystem.renderSquare(0, 60, 0, -60, 0, 0, false);
	    	GL11DrawSystem.bindTexture(vehicle.instruments.get((byte) (i*10 + 12)) == null ? emptyGauge : gauges[vehicle.instruments.get((byte) (i*10 + 12))]);
	    	GL11DrawSystem.renderSquare(-60, 0, 60, 0, 0, 0, false);
	    	GL11DrawSystem.bindTexture(vehicle.instruments.get((byte) (i*10 + 13)) == null ? emptyGauge : gauges[vehicle.instruments.get((byte) (i*10 + 13))]);
	    	GL11DrawSystem.renderSquare(0, 60, 60, 0, 0, 0, false);
			GL11.glPopMatrix();
		}
		VehicleHUDs.endHUDDraw();
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
		GL11DrawSystem.drawScaledStringAt(PlayerHelper.getTranslatedText("gui.instruments.main"), width/4, 32, 0, 1.5F, Color.WHITE);
		GL11DrawSystem.drawScaledStringAt(PlayerHelper.getTranslatedText("gui.instruments.control"), 3*width/4, 32, 0, 1.5F, Color.WHITE);
		if(lastInstrumentClicked == -1){
			if(vehicle.ticksExisted%40 >= 20){
				GL11DrawSystem.drawScaledStringAt(PlayerHelper.getTranslatedText("gui.instruments.idle"), width/2, height/2 + 32, 0, 1.5F, Color.WHITE);
			}
		}else{
			GL11DrawSystem.drawScaledStringAt(String.valueOf(lastInstrumentClicked), 10, 5, 0, 1.5F, Color.WHITE);
			if(!fault){
				GL11DrawSystem.drawScaledStringAt(PlayerHelper.getTranslatedText("gui.instruments.decide"), width/2, height/2 + 8, 0, 1.5F, Color.WHITE);
			}else{
				if(vehicle.ticksExisted%20 >= 10){
					GL11DrawSystem.drawScaledStringAt(PlayerHelper.getTranslatedText("gui.instruments.fault"), width/2, height/2 + 8, 0, 1.5F, Color.RED);
				}
			}
			boolean[] hasInstrument = new boolean[AircraftGauges.values().length];
			for(byte i=0; i<AircraftGauges.values().length; ++i){
				if(player.capabilities.isCreativeMode){
					hasInstrument[i] = true;
				}else{
					hasInstrument[i] = PlayerHelper.getQtyOfItemInInventory(MTSRegistry.flightInstrument, i, player) > 0;
				}
			}
			for(byte i=0; i<AircraftGauges.values().length; ++i){
				if(hasInstrument[i]){
					GL11.glPushMatrix();
					GL11DrawSystem.bindTexture(gauges[i]);
					if(i<8){
						GL11DrawSystem.renderSquare((i%8+1)*width/19-10, (i%8+1)*width/19+10, height*0.7 + 10, height*0.7 - 10, 0, 0, false);
					}else{
						GL11DrawSystem.renderSquare((i%8+1)*width/19-10, (i%8+1)*width/19+10, height*0.9 + 10, height*0.9 - 10, 0, 0, false);
					}
					GL11.glPopMatrix();
				}
			}
			GL11DrawSystem.bindTexture(emptyGauge);
			GL11DrawSystem.renderSquare(width/2, width/2+height*0.2, height*0.9, height*0.7, 0, 0, false);
			GL11DrawSystem.drawScaledStringAt(PlayerHelper.getTranslatedText("gui.instruments.clear"), width/2+height*0.1F, height*0.775F, 0, 1.0F, Color.WHITE);
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
			for(byte i=0; i<AircraftGauges.values().length; ++i){
				if(player.capabilities.isCreativeMode){
					hasInstrument[i] = true;
				}else{
					hasInstrument[i] = PlayerHelper.getQtyOfItemInInventory(MTSRegistry.flightInstrument, i, player) > 0;
				}
			}
			for(byte i=0; i<AircraftGauges.values().length; ++i){
				if(x >= (i%8+1)*width/19-10 && x <= (i%8+1)*width/19+10){
					if(y >= height*(i < 8 ? 0.7 : 0.9) - 10 && y <= height*(i < 8 ? 0.7 : 0.9) + 10){
						if(hasInstrument[i]){
							if(lastInstrumentClicked >= 10 && i != 10 && i != 12 && i != 13 && i != 14){
								fault = true;
								return;
							}else{
								fault = false;
							}
							MFS.MFSNet.sendToServer(new InstrumentFlyerPacket(vehicle.getEntityId(), player.getEntityId(), lastInstrumentClicked, i));
							lastClickedX = 0;
							lastClickedY = 0;
							lastInstrumentClicked = -1;
						}
						return;
					}
				}
			}
			if(x >= width/2 && x<= width/2+height*0.2 && y >= height*0.7 && y <= height*0.9){
				MFS.MFSNet.sendToServer(new InstrumentFlyerPacket(vehicle.getEntityId(), player.getEntityId(), lastInstrumentClicked, (byte) -1));
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
	
	private static ResourceLocation[] getGaugeTextures(){
		ResourceLocation[] texArray = new ResourceLocation[AircraftGauges.values().length];
		for(byte i=0; i<texArray.length; ++i){
			texArray[i] = new ResourceLocation("mfs", "textures/items/flightinstrument" + String.valueOf(i) + ".png");
		}
		return texArray;
	}
}