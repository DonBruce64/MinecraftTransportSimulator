package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackInstrument;
import minecrafttransportsimulator.items.core.ItemInstrument;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.packets.multipart.PacketMultipartInstruments;
import minecrafttransportsimulator.rendering.RenderHUD;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class GUIInstruments extends GuiScreen{
	private final EntityMultipartE_Vehicle vehicle;
	private final EntityPlayer player;
	
	private PackInstrument lastInstrumentClicked = null;
	private final Map<String, ItemStack> playerInstruments = new HashMap<String, ItemStack>();
	private final Map<String, Integer[]> renderedPlayerInstrumentsBounds = new HashMap<String, Integer[]>();
	
	public GUIInstruments(EntityMultipartE_Vehicle vehicle, EntityPlayer player){
		this.vehicle = vehicle;
		this.player = player;
	}

	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		if(vehicle.isDead || player.getDistance(vehicle.posX, vehicle.posY, vehicle.posZ) > 20){
			mc.thePlayer.closeScreen();
			return;
		}
		
		//Draw the main HUD.
		this.drawDefaultBackground();
		GL11.glPushMatrix();
		GL11.glTranslatef(width/2, 0, 0);
		GL11.glScalef(0.5F, 0.5F, 0.5F);
		RenderHUD.drawMainHUD(vehicle, width, height, true);
		GL11.glDisable(GL11.GL_LIGHTING);
		mc.entityRenderer.disableLightmap();
		GL11.glPopMatrix();
		
		//Draw the engine HUD.
		GL11.glPushMatrix();
		GL11.glTranslatef(width/2, 3*height/8, 0);
		GL11.glScalef(0.5F, 0.5F, 0.5F);
		RenderHUD.drawAuxiliaryHUD(vehicle, width, height, true);
		GL11.glDisable(GL11.GL_LIGHTING);
		mc.entityRenderer.disableLightmap();
		GL11.glPopMatrix();
		
		//Draw a blank square for any instruments that aren't present to let the player know they can be clicked.
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glColor3f(54F/255F, 57F/255F, 62F/255F);
		for(byte i=0; i<vehicle.pack.motorized.instruments.size(); ++i){
			if(vehicle.getInstrumentInfoInSlot(i) == null){
				PackInstrument packInstrument = vehicle.pack.motorized.instruments.get(i);
				GL11.glPushMatrix();
		    	if(packInstrument.optionalEngineNumber == 0){
		    		GL11.glTranslated(width/2 + width*packInstrument.hudpos[0]/200F, height*packInstrument.hudpos[1]/200F, 0);
		    	}else{
		    		GL11.glTranslated(width/2 + width*packInstrument.hudpos[0]/200F, 3*height/8 + height*packInstrument.hudpos[1]/200F, 0);
		    	}
		    	GL11.glScalef(packInstrument.hudScale/2F, packInstrument.hudScale/2F, packInstrument.hudScale/2F);
		    	GL11.glBegin(GL11.GL_QUADS);
		    	GL11.glVertex2d(-64, 64);
		    	GL11.glVertex2d(64, 64);
		    	GL11.glVertex2d(64, -64);
		    	GL11.glVertex2d(-64, -64);
		    	GL11.glEnd();
				GL11.glPopMatrix();
			}
		}
		GL11.glColor3f(1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glPopMatrix();
		
		//If we clicked an instrument, highlight it.
		if(lastInstrumentClicked != null){
			GL11.glPushMatrix();
	    	GL11.glColor4f(1, 1, 1, 1);
	    	if(lastInstrumentClicked.optionalEngineNumber == 0){
	    		GL11.glTranslated(width/2 + width*lastInstrumentClicked.hudpos[0]/200F, height*lastInstrumentClicked.hudpos[1]/200F, 0);
	    	}else{
	    		GL11.glTranslated(width/2 + width*lastInstrumentClicked.hudpos[0]/200F, 3*height/8 + height*lastInstrumentClicked.hudpos[1]/200F, 0);
	    	}
	    	GL11.glScalef(lastInstrumentClicked.hudScale/2F, lastInstrumentClicked.hudScale/2F, lastInstrumentClicked.hudScale/2F);
	    	GL11.glDisable(GL11.GL_TEXTURE_2D);
	    	GL11.glEnable(GL11.GL_BLEND);
	    	GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
	    	GL11.glBegin(GL11.GL_QUADS);
	    	GL11.glVertex2d(-64, 64);
	    	GL11.glVertex2d(64, 64);
	    	GL11.glVertex2d(64, -64);
	    	GL11.glVertex2d(-64, -64);
	    	GL11.glEnd();
	    	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	    	GL11.glDisable(GL11.GL_BLEND);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glPopMatrix();
		}
		
		//Get all the instruments that fit this vehicle that the player currently has.
		playerInstruments.clear();
		renderedPlayerInstrumentsBounds.clear();
		if(!player.capabilities.isCreativeMode){
			for(ItemStack stack : player.inventory.mainInventory){
				if(stack != null){
					if(stack.getItem() instanceof ItemInstrument){
						ItemInstrument instrumentItem = (ItemInstrument) stack.getItem();
						if(PackParserSystem.getInstrument(instrumentItem.instrumentName).general.validVehicles.contains(vehicle.pack.general.type)){
							if(!playerInstruments.containsKey(instrumentItem.instrumentName)){
								byte xIndex = (byte) (playerInstruments.size()%6);
								byte yIndex = (byte) (playerInstruments.size()/6);
								renderedPlayerInstrumentsBounds.put(instrumentItem.instrumentName, new Integer[]{width/8 + xIndex*24-12, width/8 + xIndex*24+12, height/4 + yIndex*24-12, height/4 + yIndex*24+12});
								playerInstruments.put(instrumentItem.instrumentName, stack);
								
							}	
						}					
					}
				}
			}
		}else{
			for(ItemInstrument instrumentItem : MTSRegistry.instrumentItemMap.values()){
				if(PackParserSystem.getInstrument(instrumentItem.instrumentName).general.validVehicles.contains(vehicle.pack.general.type)){
					byte xIndex = (byte) (playerInstruments.size()%6);
					byte yIndex = (byte) (playerInstruments.size()/6);
					renderedPlayerInstrumentsBounds.put(instrumentItem.instrumentName, new Integer[]{width/8 + xIndex*24-12, width/8 + xIndex*24+12, height/4 + yIndex*24-12, height/4 + yIndex*24+12});
					playerInstruments.put(instrumentItem.instrumentName, new ItemStack(instrumentItem));
				}else{
					//System.out.println(PackParserSystem.getInstrument(instrumentItem.instrumentName).general.validVehicles.size());
				}
			}
		}
		
		//Now render these instruments.
		for(Entry<String, Integer[]> renderedInstrumentsEntry : renderedPlayerInstrumentsBounds.entrySet()){
			GL11.glPushMatrix();
			GL11.glTranslatef(renderedInstrumentsEntry.getValue()[0], renderedInstrumentsEntry.getValue()[2], 0);
			GL11.glScalef(1.5F, 1.5F, 1.5F);
			mc.getRenderItem().renderItemIntoGUI(playerInstruments.get(renderedInstrumentsEntry.getKey()), 0, 0);
			GL11.glPopMatrix();
		}
		
		//Render text into the GUI.
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
				
		if(lastInstrumentClicked == null){
			if(vehicle.ticksExisted%40 >= 20){
				GL11.glPushMatrix();
				GL11.glTranslatef(3*width/4, height/16, 0);
				GL11.glScalef(1.5F, 1.5F, 1.5F);
				fontRendererObj.drawString(I18n.format("gui.instruments.idle"), (int) (-fontRendererObj.getStringWidth(I18n.format("gui.instruments.idle"))/2), 0, Color.WHITE.getRGB());
				GL11.glPopMatrix();
			}
		}else{
			GL11.glPushMatrix();
			GL11.glTranslatef(width/2, height/16, 0);
			GL11.glScalef(1.5F, 1.5F, 1.5F);
			fontRendererObj.drawString(I18n.format("gui.instruments.decide"), (int) (-fontRendererObj.getStringWidth(I18n.format("gui.instruments.decide"))/2), 0, Color.WHITE.getRGB());
			GL11.glPopMatrix();
		}
		
		GL11.glPushMatrix();
		GL11.glTranslatef(width/4, 11*height/16, 0);
		fontRendererObj.drawString(I18n.format("gui.instruments.clear"), (int) (-fontRendererObj.getStringWidth(I18n.format("gui.instruments.clear"))/2), 0, Color.WHITE.getRGB());
		GL11.glPopMatrix();
		
		//Need to do mouseover after main rendering or you get rendering issues.
		for(Entry<String, Integer[]> renderedInstrumentsEntry : renderedPlayerInstrumentsBounds.entrySet()){
			if(mouseX > renderedInstrumentsEntry.getValue()[0] && mouseX < renderedInstrumentsEntry.getValue()[1] && mouseY > renderedInstrumentsEntry.getValue()[2] && mouseY < renderedInstrumentsEntry.getValue()[3]){
				renderToolTip(playerInstruments.get(renderedInstrumentsEntry.getKey()), mouseX,  mouseY);
			}
		}
	}
	
	@Override
    protected void mouseClicked(int mouseX, int mouseY, int button){
		//Check to see if we clicked an instrument on the HUD.
		for(PackInstrument instrument : vehicle.pack.motorized.instruments){
			final int xCenter = width/2 + width*instrument.hudpos[0]/200;
			final int yCenter = instrument.optionalEngineNumber == 0 ? height*instrument.hudpos[1]/200 : 3*height/8 + height*instrument.hudpos[1]/200;
			float scale = instrument.hudScale/2F;
			if(mouseX > xCenter - 64*scale && mouseX < xCenter + 64*scale && mouseY > yCenter - 64*scale && mouseY < yCenter + 64*scale){
				lastInstrumentClicked = instrument;
				return;
			}
		}

		//If we didn't click an instrument on the HUD, and have already selected one on the HUD, see if we clicked one from the inventory.
		if(lastInstrumentClicked != null){
			//Check to see if the player clicked an instrument, and if so which one.
			for(Entry<String, Integer[]> renderedInstrumentsEntry : renderedPlayerInstrumentsBounds.entrySet()){
				if(mouseX > renderedInstrumentsEntry.getValue()[0] && mouseX < renderedInstrumentsEntry.getValue()[1] && mouseY > renderedInstrumentsEntry.getValue()[2] && mouseY < renderedInstrumentsEntry.getValue()[3]){
					MTS.MTSNet.sendToServer(new PacketMultipartInstruments(vehicle, player, getIndexOfLastInstrumentClicked(), renderedInstrumentsEntry.getKey()));
					lastInstrumentClicked = null;
					return;
				}
			}
			
			//Either the player didn't click an instrument, or they clicked the CLEAR.
			if(mouseX > width/4 - 15 && mouseX < width/4 + 15 && mouseY > 11*height/16 - 15 && mouseY < 11*height/16 + 15){
				if(vehicle.getInstrumentInfoInSlot(getIndexOfLastInstrumentClicked()) != null){
					MTS.MTSNet.sendToServer(new PacketMultipartInstruments(vehicle, player, getIndexOfLastInstrumentClicked(), ""));
					lastInstrumentClicked = null;
					return;
				}
			}
		}
	}
	
	private byte getIndexOfLastInstrumentClicked(){
		for(byte i=0; i<vehicle.pack.motorized.instruments.size(); ++i){
			PackInstrument packInstrument = vehicle.pack.motorized.instruments.get(i);
			if(packInstrument.equals(lastInstrumentClicked)){
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public boolean doesGuiPauseGame(){
		return false;
	}
}
