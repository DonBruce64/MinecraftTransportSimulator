package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackInstrument;
import minecrafttransportsimulator.items.core.ItemInstrument;
import minecrafttransportsimulator.mcinterface.MTSGui;
import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSRenderer;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleInstruments;
import minecrafttransportsimulator.rendering.RenderHUD;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

public class GUIInstruments extends MTSGui{
	private final EntityVehicleE_Powered vehicle;
	private final MTSPlayerInterface player;
	
	private PackInstrument lastInstrumentClicked = null;
	private final Map<String, ItemStack> playerInstruments = new HashMap<String, ItemStack>();
	private final Map<String, Integer[]> renderedPlayerInstrumentsBounds = new HashMap<String, Integer[]>();
	
	public GUIInstruments(EntityVehicleE_Powered vehicle, MTSPlayerInterface player){
		this.vehicle = vehicle;
		this.player = player;
	}
	
	@Override
	protected void handleInit(int width, int height){};

	@Override
	protected void handleDraw(int mouseX, int mouseY){
		if(vehicle.isDead){
			close();
			return;
		}
		
		renderBackground();
		
		//First scale the GUI to match the HUD scaling.
		GL11.glPushMatrix();
		GL11.glScalef(1.0F*width/RenderHUD.screenDefaultX, 1.0F*height/RenderHUD.screenDefaultY, 0);
		
		//Draw the main HUD.
		GL11.glPushMatrix();
		GL11.glTranslatef(RenderHUD.screenDefaultX/2, 0, 0);
		GL11.glScalef(0.5F, 0.5F, 0.5F);
		RenderHUD.drawMainHUD(vehicle, true);
		//If we are a plane, draw the panel.
		if(vehicle.pack.general.type.equals("plane")){
			GL11.glTranslatef(0, 3*RenderHUD.screenDefaultY/4, 0);
			RenderHUD.drawAuxiliaryHUD(vehicle, width, height, true);
		}
		GL11.glDisable(GL11.GL_LIGHTING);
		MTSRenderer.enableLightmap();
		GL11.glPopMatrix();
		
		//Draw a blank square for any instruments that aren't present to let the player know they can be clicked.
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glColor3f(54F/255F, 57F/255F, 62F/255F);
		GL11.glTranslatef(RenderHUD.screenDefaultX/2, 0, 0);
		for(byte i=0; i<vehicle.pack.motorized.instruments.size(); ++i){
			if(vehicle.getInstrumentInfoInSlot(i) == null){
				PackInstrument packInstrument = vehicle.pack.motorized.instruments.get(i);
				GL11.glPushMatrix();
		    	if(packInstrument.optionalEngineNumber == 0){
		    		GL11.glTranslated(RenderHUD.screenDefaultX*packInstrument.hudpos[0]/200F, RenderHUD.screenDefaultY*packInstrument.hudpos[1]/200F, 0);
		    	}else{
		    		GL11.glTranslated(RenderHUD.screenDefaultX*packInstrument.hudpos[0]/200F, 3*RenderHUD.screenDefaultY/8 + RenderHUD.screenDefaultY*packInstrument.hudpos[1]/200F, 0);
		    	}
		    	GL11.glScalef(packInstrument.hudScale/2F, packInstrument.hudScale/2F, 0);
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
	    		GL11.glTranslated(RenderHUD.screenDefaultX/2 + RenderHUD.screenDefaultX*lastInstrumentClicked.hudpos[0]/200F, RenderHUD.screenDefaultY*lastInstrumentClicked.hudpos[1]/200F, 0);
	    	}else{
	    		GL11.glTranslated(RenderHUD.screenDefaultX/2 + RenderHUD.screenDefaultX*lastInstrumentClicked.hudpos[0]/200F, 3*RenderHUD.screenDefaultY/8 + RenderHUD.screenDefaultY*lastInstrumentClicked.hudpos[1]/200F, 0);
	    	}
	    	GL11.glScalef(lastInstrumentClicked.hudScale/2F, lastInstrumentClicked.hudScale/2F, 0);
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
		if(!player.creative()){
			for(ItemStack stack : player.getInventory()){
				if(stack != null){
					if(stack.getItem() instanceof ItemInstrument){
						ItemInstrument instrumentItem = (ItemInstrument) stack.getItem();
						if(!playerInstruments.containsKey(instrumentItem.instrumentName)){
							if(PackParserSystem.getInstrument(instrumentItem.instrumentName).general.validVehicles.contains(vehicle.pack.general.type)){
								byte xIndex = (byte) (playerInstruments.size()%6);
								byte yIndex = (byte) (playerInstruments.size()/6);
								renderedPlayerInstrumentsBounds.put(instrumentItem.instrumentName, new Integer[]{64 + xIndex*64-32, 64 + xIndex*64+32, RenderHUD.screenDefaultY/4 + yIndex*64-32, RenderHUD.screenDefaultY/4 + yIndex*64+32});
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
					renderedPlayerInstrumentsBounds.put(instrumentItem.instrumentName, new Integer[]{64 + xIndex*64-32, 64 + xIndex*64+32, RenderHUD.screenDefaultY/4 + yIndex*64-32, RenderHUD.screenDefaultY/4 + yIndex*64+32});
					playerInstruments.put(instrumentItem.instrumentName, new ItemStack(instrumentItem));
				}
			}
		}
		
		//Now render these instruments.
		for(Entry<String, Integer[]> renderedInstrumentsEntry : renderedPlayerInstrumentsBounds.entrySet()){
			GL11.glPushMatrix();
			GL11.glTranslatef(renderedInstrumentsEntry.getValue()[0], renderedInstrumentsEntry.getValue()[2], 0);
			GL11.glScalef(3.5F, 3.5F, 0);
			MTSRenderer.renderItem(playerInstruments.get(renderedInstrumentsEntry.getKey()), 0, 0);
			GL11.glPopMatrix();
		}
		
		//Render text into the GUI.
		GL11.glPushMatrix();
		GL11.glTranslatef(3*RenderHUD.screenDefaultX/4, 2.5F*RenderHUD.screenDefaultY/16, 0);
		GL11.glScalef(2.5F, 2.5F, 2.5F);
		MTSRenderer.drawText(I18n.format("gui.instruments.main"), (int) (-MTSRenderer.getTextWidth(I18n.format("gui.instruments.main"))/2), 0, Color.WHITE, false, false);
		GL11.glPopMatrix();
		
		if(vehicle.pack.general.type.equals("plane")){
			GL11.glPushMatrix();
			GL11.glTranslatef(3*RenderHUD.screenDefaultX/4, 9*RenderHUD.screenDefaultY/16, 0);
			GL11.glScalef(2.5F, 2.5F, 2.5F);
			MTSRenderer.drawText(I18n.format("gui.instruments.control"), (int) (-MTSRenderer.getTextWidth(I18n.format("gui.instruments.control"))/2), 0, Color.WHITE, false, false);
			GL11.glPopMatrix();
		}
				
		if(lastInstrumentClicked == null){
			if(vehicle.ticksExisted%40 >= 20){
				GL11.glPushMatrix();
				GL11.glTranslatef(3*RenderHUD.screenDefaultX/4, RenderHUD.screenDefaultY/16, 0);
				GL11.glScalef(2.5F, 2.5F, 2.5F);
				MTSRenderer.drawText(I18n.format("gui.instruments.idle"), (int) (-MTSRenderer.getTextWidth(I18n.format("gui.instruments.idle"))/2), 0, Color.WHITE, false, false);
				GL11.glPopMatrix();
			}
		}else{
			GL11.glPushMatrix();
			GL11.glTranslatef(RenderHUD.screenDefaultX/2, RenderHUD.screenDefaultY/16, 0);
			GL11.glScalef(2.5F, 2.5F, 2.5F);
			MTSRenderer.drawText(I18n.format("gui.instruments.decide"), (int) (-MTSRenderer.getTextWidth(I18n.format("gui.instruments.decide"))/2), 0, Color.WHITE, false, false);
			GL11.glPopMatrix();
		}
		
		GL11.glPushMatrix();
		GL11.glTranslatef(RenderHUD.screenDefaultX/4, 11*RenderHUD.screenDefaultY/16, 0);
		GL11.glScalef(2.5F, 2.5F, 2.5F);
		MTSRenderer.drawText(I18n.format("gui.instruments.clear"), (int) (-MTSRenderer.getTextWidth(I18n.format("gui.instruments.clear"))/2), 0, Color.WHITE, false, false);
		GL11.glPopMatrix();
		
		//Need to do mouseover after main rendering or you get rendering issues.
		//Scale mouse position to the scaled GUI;
		mouseX = (int) (1.0F*mouseX/width*RenderHUD.screenDefaultX);
		mouseY = (int) (1.0F*mouseY/height*RenderHUD.screenDefaultY);
		for(Entry<String, Integer[]> renderedInstrumentsEntry : renderedPlayerInstrumentsBounds.entrySet()){
			if(mouseX > renderedInstrumentsEntry.getValue()[0] && mouseX < renderedInstrumentsEntry.getValue()[1] && mouseY > renderedInstrumentsEntry.getValue()[2] && mouseY < renderedInstrumentsEntry.getValue()[3]){
				GL11.glPushMatrix();
				GL11.glTranslatef(mouseX, mouseY, 0);
				GL11.glScalef(1.5F, 1.5F, 1.5F);
				renderToolTip(playerInstruments.get(renderedInstrumentsEntry.getKey()), 0,  0);
				GL11.glPopMatrix();
			}
		}
		
		GL11.glPopMatrix();
	}
	
	@Override
	protected void handleMouseClick(int mouseX, int mouseY){
		//Check to see if we clicked an instrument on the HUD.
		//Scale mouse position to the scaled GUI;
		mouseX = (int) (1.0F*mouseX/width*RenderHUD.screenDefaultX);
		mouseY = (int) (1.0F*mouseY/height*RenderHUD.screenDefaultY);
		for(PackInstrument instrument : vehicle.pack.motorized.instruments){
			final float xCenter = RenderHUD.screenDefaultX/2 + RenderHUD.screenDefaultX*instrument.hudpos[0]/200;
			final float yCenter = instrument.optionalEngineNumber == 0 ? RenderHUD.screenDefaultY*instrument.hudpos[1]/200 : 3*RenderHUD.screenDefaultY/8 + RenderHUD.screenDefaultY*instrument.hudpos[1]/200;
			if(mouseX > xCenter - 32 && mouseX < xCenter + 32 && mouseY > yCenter - 32 && mouseY < yCenter + 32){
				lastInstrumentClicked = instrument;
				return;
			}
		}

		//If we didn't click an instrument on the HUD, and have already selected one on the HUD, see if we clicked one from the inventory.
		if(lastInstrumentClicked != null){
			//Check to see if the player clicked an instrument, and if so which one.
			for(Entry<String, Integer[]> renderedInstrumentsEntry : renderedPlayerInstrumentsBounds.entrySet()){
				if(mouseX > renderedInstrumentsEntry.getValue()[0] && mouseX < renderedInstrumentsEntry.getValue()[1] && mouseY > renderedInstrumentsEntry.getValue()[2] && mouseY < renderedInstrumentsEntry.getValue()[3]){
					MTS.MTSNet.sendToServer(new PacketVehicleInstruments(vehicle, player, getIndexOfLastInstrumentClicked(), renderedInstrumentsEntry.getKey()));
					lastInstrumentClicked = null;
					return;
				}
			}
			
			//Either the player didn't click an instrument, or they clicked the CLEAR.
			if(mouseX > RenderHUD.screenDefaultX/4 - 30 && mouseX < RenderHUD.screenDefaultX/4 + 30 && mouseY > 11*RenderHUD.screenDefaultY/16 - 30 && mouseY < 11*RenderHUD.screenDefaultY/16 + 30){
				if(vehicle.getInstrumentInfoInSlot(getIndexOfLastInstrumentClicked()) != null){
					MTS.MTSNet.sendToServer(new PacketVehicleInstruments(vehicle, player, getIndexOfLastInstrumentClicked(), ""));
					lastInstrumentClicked = null;
					return;
				}
			}
		}
	}
	
	@Override
	protected void handleButtonClick(MTSButton buttonClicked){}
	
	@Override
	protected void handleKeyTyped(char key, int code){}
	
	@Override
	public boolean pauses(){
		return false;
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
}
