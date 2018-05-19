package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSInstruments;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Instruments;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackInstrument;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.packets.multipart.PacketMultipartInstruments;
import minecrafttransportsimulator.rendering.RenderHUD;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class GUIInstruments extends GuiScreen{
	private final EntityMultipartE_Vehicle vehicle;
	private final EntityPlayer player;
	private final byte numberEngineBays;
	private final Map<Float[], Byte> instrumentCoords;
	
	private boolean fault;
	private byte lastInstrumentClicked = -1;
	private List<Byte> renderedInstruments = new ArrayList<Byte>();
	
	public GUIInstruments(EntityMultipartE_Vehicle vehicle, EntityPlayer player){
		this.vehicle = vehicle;
		this.player = player;
		numberEngineBays = vehicle.getNumberEngineBays();
		instrumentCoords = new HashMap<Float[], Byte>();
	}
	
	@Override
	public void initGui(){
		for(byte i=0; i<vehicle.pack.motorized.instruments.size(); ++i){
			PackInstrument packInstrument = vehicle.pack.motorized.instruments.get(i);
			if(packInstrument.optionalEngineNumber == 0){
				instrumentCoords.put(new Float[]{width/2 + 0.5F*packInstrument.hudpos[0]*width/100F, 0.5F*packInstrument.hudpos[1]*height/100F, packInstrument.hudScale}, i);
			}else{
				instrumentCoords.put(new Float[]{width/2 + 0.5F*packInstrument.hudpos[0]*width/100F, 3*height/8 + 0.5F*packInstrument.hudpos[1]*height/100F, packInstrument.hudScale}, i);
			}
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
		GL11.glTranslatef(width/2, 0, 0);
		GL11.glScalef(0.5F, 0.5F, 0.5F);
		RenderHUD.drawMainHUD(vehicle, width, height, true);
		GL11.glDisable(GL11.GL_LIGHTING);
		mc.entityRenderer.disableLightmap();
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(width/2, 3*height/8, 0);
		GL11.glScalef(0.5F, 0.5F, 0.5F);
		RenderHUD.drawAuxiliaryHUD(vehicle, width, height, true);
		GL11.glDisable(GL11.GL_LIGHTING);
		mc.entityRenderer.disableLightmap();
		GL11.glPopMatrix();
		
		if(lastInstrumentClicked != -1){
			GL11.glPushMatrix();
	    	GL11.glColor4f(1, 1, 1, 1);
	    	GL11.glDisable(GL11.GL_TEXTURE_2D);
	    	GL11.glEnable(GL11.GL_BLEND);
	    	GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
	    	GL11.glBegin(GL11.GL_TRIANGLES);
	    	for(Entry<Float[], Byte> instrumentCoordEntry : instrumentCoords.entrySet()){
	    		if(lastInstrumentClicked == instrumentCoordEntry.getValue()){
	    			Float[] coords = instrumentCoordEntry.getKey();
	    			GL11.glVertex2d(coords[0]-12*coords[2], coords[1]-12*coords[2]);
	    			GL11.glVertex2d(coords[0]-12*coords[2], coords[1]+12*coords[2]);
	    			GL11.glVertex2d(coords[0]+12*coords[2], coords[1]+12*coords[2]);
	    			GL11.glVertex2d(coords[0]+12*coords[2], coords[1]+12*coords[2]);
	    			GL11.glVertex2d(coords[0]+12*coords[2], coords[1]-12*coords[2]);
	    			GL11.glVertex2d(coords[0]-12*coords[2], coords[1]-12*coords[2]);
	    		}
	    	}
	    	GL11.glEnd();
	    	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	    	GL11.glDisable(GL11.GL_BLEND);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glPopMatrix();
		}
		
		boolean[] hasInstrument = new boolean[Instruments.values().length];
		for(byte i=0; i<MTSInstruments.Instruments.values().length; ++i){
			if(player.capabilities.isCreativeMode){
				hasInstrument[i] = true;
			}else{
				hasInstrument[i] = player.inventory.hasItemStack(new ItemStack(MTSRegistry.instrument, 1, i));
			}
		}

		renderedInstruments.clear();
		for(byte i=MTSInstruments.numberBlanks; i<MTSInstruments.Instruments.values().length; ++i){
			if(hasInstrument[i]){
				boolean isInstrumentForVehicle = false;
				for(Class<? extends EntityMultipartD_Moving> validClass : MTSInstruments.Instruments.values()[i].validClasses){
					if(validClass.isAssignableFrom(vehicle.getClass())){
						isInstrumentForVehicle = true;
						break;
					}
				}
				if(isInstrumentForVehicle){
					GL11.glPushMatrix();
					GL11.glTranslatef((renderedInstruments.size()%4-2)*width/20 + width/4, renderedInstruments.size()/4*(height/10) + height/4, 0);
					GL11.glScalef(1.5F, 1.5F, 1.5F);
					mc.getRenderItem().renderItemIntoGUI(new ItemStack(MTSRegistry.instrument, 1, i), 0, 0);
					GL11.glPopMatrix();
					renderedInstruments.add(i);
				}
			}
		}
		
		GL11.glPushMatrix();
		GL11.glTranslatef(width/4 - 15, 11*height/16 + 15, 0);
		GL11.glScalef(2.0F, 2.0F, 2.0F);
		mc.getRenderItem().renderItemIntoGUI(new ItemStack(MTSRegistry.instrument, 1, vehicle.getBlankInstrument().ordinal()), 0, 0);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(3*width/4, 2.5F*height/16, 0);
		GL11.glScalef(1.5F, 1.5F, 1.5F);
		fontRendererObj.drawString(I18n.format("gui.instruments.main"), (int) (-fontRendererObj.getStringWidth(I18n.format("gui.instruments.main"))/2), 0, Color.WHITE.getRGB());
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(3*width/4, 9*height/16, 0);
		GL11.glScalef(1.5F, 1.5F, 1.5F);
		fontRendererObj.drawString(I18n.format("gui.instruments.control"), (int) (-fontRendererObj.getStringWidth(I18n.format("gui.instruments.control"))/2), 0, Color.WHITE.getRGB());
		GL11.glPopMatrix();
				
		
		if(lastInstrumentClicked == -1){
			if(vehicle.ticksExisted%40 >= 20){
				GL11.glPushMatrix();
				GL11.glTranslatef(3*width/4, height/16, 0);
				GL11.glScalef(1.5F, 1.5F, 1.5F);
				fontRendererObj.drawString(I18n.format("gui.instruments.idle"), (int) (-fontRendererObj.getStringWidth(I18n.format("gui.instruments.idle"))/2), 0, Color.WHITE.getRGB());
				GL11.glPopMatrix();
			}
		}else{
			GL11.glPushMatrix();
			GL11.glTranslatef(10, 6, 0);
			GL11.glScalef(1.5F, 1.5F, 1.5F);
			fontRendererObj.drawString(String.valueOf(lastInstrumentClicked), 0, 0, Color.WHITE.getRGB());
			GL11.glPopMatrix();
			if(!fault){
				GL11.glPushMatrix();
				GL11.glTranslatef(width/2, height/16, 0);
				GL11.glScalef(1.5F, 1.5F, 1.5F);
				fontRendererObj.drawString(I18n.format("gui.instruments.decide"), (int) (-fontRendererObj.getStringWidth(I18n.format("gui.instruments.decide"))/2), 0, Color.WHITE.getRGB());
				GL11.glPopMatrix();
			}else{
				if(vehicle.ticksExisted%20 >= 10){
					GL11.glPushMatrix();
					GL11.glTranslatef(width/2, height/16, 0);
					GL11.glScalef(1.5F, 1.5F, 1.5F);
					fontRendererObj.drawString(I18n.format("gui.instruments.fault"), (int) (-fontRendererObj.getStringWidth(I18n.format("gui.instruments.fault"))/2), 0, Color.RED.getRGB());
					GL11.glPopMatrix();
				}
			}
		}
		
		GL11.glPushMatrix();
		GL11.glTranslatef(width/4, 11*height/16, 0);
		fontRendererObj.drawString(I18n.format("gui.instruments.clear"), (int) (-fontRendererObj.getStringWidth(I18n.format("gui.instruments.clear"))/2), 0, Color.WHITE.getRGB());
		GL11.glPopMatrix();
		
		//Need to do mouseover after main rendering or you get rendering issues.
		for(byte i=0; i<renderedInstruments.size(); ++i){
			float xStart = (i%4-2)*width/20 + width/4;
			float xEnd = (i%4-1)*width/20 + width/4;
			float yStart = i/4*(height/10) + height/4;
			float yEnd = (i/4+1)*(height/10) + height/4;
			if(mouseX > xStart && mouseX < xEnd && mouseY > yStart && mouseY < yEnd){
				renderToolTip(new ItemStack(MTSRegistry.instrument, 1, renderedInstruments.get(i)), mouseX,  mouseY);
			}
		}
	}
	
	@Override
    protected void mouseClicked(int x, int y, int button){
		for(Entry<Float[], Byte> entry : instrumentCoords.entrySet()){
			float xCenter = entry.getKey()[0];
			float yCenter = entry.getKey()[1];
			float scale = entry.getKey()[2];
			if(x > xCenter-12*scale && x < xCenter+12*scale && y > yCenter-12*scale && y < yCenter+12*scale){
				lastInstrumentClicked = entry.getValue();
				return;
			}
		}
		
		if(lastInstrumentClicked != -1){
			//Check to see if the player clicked an instrument, and if so which one.
			for(byte i=0; i<renderedInstruments.size(); ++i){
				float xStart = (i%4-2)*width/20 + width/4;
				float xEnd = (i%4-1)*width/20 + width/4;
				float yStart = i/4*(height/10) + height/4;
				float yEnd = (i/4+1)*(height/10) + height/4;
				if(x > xStart && x < xEnd && y > yStart && y < yEnd){
					//Check to make sure we don't try to put a regular instrument in the motor section.
					if(vehicle.pack.motorized.instruments.get(lastInstrumentClicked).optionalEngineNumber != 0 && !MTSInstruments.Instruments.values()[renderedInstruments.get(i)].canConnectToEngines){
						fault = true;
						return;
					}else{
						fault = false;
					}
					MTS.MTSNet.sendToServer(new PacketMultipartInstruments(vehicle, player, lastInstrumentClicked, renderedInstruments.get(i)));
					lastInstrumentClicked = -1;
					return;
				}
			}
			
			//Either the player didn't click an instrument, or they clicked the blank.
			if(x > width/4 - 15 && x < width/4 + 15 && y > 11*height/16 + 15 && y < 11*height/16 + 45){
				if(!vehicle.getInstrumentNumber(lastInstrumentClicked).equals(vehicle.getBlankInstrument())){
					MTS.MTSNet.sendToServer(new PacketMultipartInstruments(vehicle, player, lastInstrumentClicked, (byte) vehicle.getBlankInstrument().ordinal()));
					lastInstrumentClicked = -1;
					return;
				}
			}
		}
	}
	
	@Override
	public boolean doesGuiPauseGame(){
		return false;
	}
}
