package minecrafttransportsimulator.rendering;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Controls;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackControl;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackFileDefinitions;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackInstrument;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem.MultipartTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

public final class RenderHUD{
	private static final TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
	
	public static void drawMainHUD(EntityMultipartVehicle vehicle, int width, int height, boolean inGUI){
		final PackFileDefinitions definition = PackParserSystem.getDefinitionForPack(vehicle.name);
		final ResourceLocation backplateTexture;
		final ResourceLocation mouldingTexture;
		if(definition.backplateTexture.contains(":")){
			backplateTexture = new ResourceLocation(definition.backplateTexture);
		}else{
			backplateTexture = new ResourceLocation(MTS.MODID, "textures/hud/" + definition.backplateTexture);
		}
		if(definition.mouldingTexture.contains(":")){
			mouldingTexture = new ResourceLocation(definition.mouldingTexture);
		}else{
			mouldingTexture = new ResourceLocation(MTS.MODID, "textures/hud/" + definition.mouldingTexture);
		}
		
		GL11.glPushMatrix();
		GL11.glColor4f(1, 1, 1, 1);
		if(!inGUI){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}

		if(CameraSystem.hudMode == 3 || inGUI){
			drawLowerPanel(width, height, backplateTexture);
		}else{
			GL11.glTranslatef(0, height/4, 0);
		}
		if(CameraSystem.hudMode == 2 || CameraSystem.hudMode == 3 || inGUI){
			drawUpperPanel(width, height, backplateTexture, mouldingTexture);
		}
		if(CameraSystem.hudMode > 0 || inGUI){
			drawInstruments(vehicle, width, height, 
					CameraSystem.hudMode == 1 && !inGUI ? width*1/4 : 0,
					CameraSystem.hudMode == 1 && !inGUI  ? width*3/4 : width,
					CameraSystem.hudMode != 3 && !inGUI ? height*3/4 : height,
					inGUI, true);
		}
		if(CameraSystem.hudMode > 1 && !inGUI){
			drawControls(vehicle, width, height, inGUI);
		}

		if(!inGUI){
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}
		GL11.glPopMatrix();
	}
	
	public static void drawAuxiliaryHUD(EntityMultipartVehicle vehicle, int width, int height, boolean inGUI){
		final PackFileDefinitions definition = PackParserSystem.getDefinitionForPack(vehicle.name);
		final ResourceLocation backplateTexture;
		final ResourceLocation mouldingTexture;
		if(definition.backplateTexture.startsWith("minecraft:")){
			backplateTexture = new ResourceLocation(definition.backplateTexture);
		}else{
			backplateTexture = new ResourceLocation(MTS.MODID, "textures/hud/" + definition.backplateTexture);
		}
		if(definition.mouldingTexture.startsWith("minecraft:")){
			mouldingTexture = new ResourceLocation(definition.mouldingTexture);
		}else{
			mouldingTexture = new ResourceLocation(MTS.MODID, "textures/hud/" + definition.mouldingTexture);
		}
		
		GL11.glPushMatrix();
		GL11.glColor4f(1, 1, 1, 1);
		if(!inGUI){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}
		drawAuxiliaryPanel(width, height, backplateTexture, mouldingTexture);
		drawInstruments(vehicle, width, height, 0, width, height, inGUI, false);
		if(!inGUI){
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}
		GL11.glPopMatrix();
	}
	
	private static void drawInstruments(EntityMultipartVehicle vehicle, int width, int height, int minX, int maxX, int maxY, boolean inGUI, boolean main){
		MultipartTypes vehicleType = PackParserSystem.getMultipartType(vehicle.name);
		for(byte i=0; i<vehicle.pack.motorized.instruments.size(); ++i){
			PackInstrument packInstrument = vehicle.pack.motorized.instruments.get(i);
			//Only render instruments not in the panel
			if((packInstrument.optionalEngineNumber == 0 && main) || (packInstrument.optionalEngineNumber != 0 && !main)){
				if(packInstrument.hudpos != null){
					if(packInstrument.hudpos[0] >= 100F*minX/width && packInstrument.hudpos[0] <= 100F*maxX/width && packInstrument.hudpos[1] <= 100F*maxY/height){
						GL11.glPushMatrix();
						GL11.glTranslated(packInstrument.hudpos[0]*width/100, packInstrument.hudpos[1]*height/100, 0);
						GL11.glScalef(packInstrument.hudScale, packInstrument.hudScale, packInstrument.hudScale);
						if(inGUI){
							GL11.glRotatef(180, 0, 0, 1);
						}
						RenderInstruments.drawInstrument(vehicle, 0, 0, vehicle.getInstrumentNumber(i), !inGUI, packInstrument.optionalEngineNumber);
						GL11.glPopMatrix();
					}
				}
			}
		}
	}
	
	private static void drawControls(EntityMultipartVehicle vehicle, int width, int height, boolean inGUI){
		MultipartTypes vehicleType = PackParserSystem.getMultipartType(vehicle.name);
		for(byte i=0; i<vehicle.pack.motorized.controls.size(); ++i){
			PackControl packControl = vehicle.pack.motorized.controls.get(i);
			for(Controls control : Controls.values()){
				if(control.name().toLowerCase().equals(packControl.controlName)){
					GL11.glPushMatrix();
					GL11.glTranslated(packControl.hudpos[0]*width/100, packControl.hudpos[1]*height/100, 0);
					RenderControls.drawControl(vehicle, control, !inGUI);
					GL11.glPopMatrix();
				}
			}
		}
	}
	
	private static void drawUpperPanel(int width, int height, ResourceLocation backplateTexture, ResourceLocation mouldingTexture){
		textureManager.bindTexture(backplateTexture);
		GL11.glBegin(GL11.GL_QUAD_STRIP);
		GL11.glTexCoord2f(0.0F, 0.5F);
		GL11.glVertex2d(0, 5*height/8);
		GL11.glTexCoord2f(0, 1.0F);
		GL11.glVertex2d(0.0F, 3*height/4);
		GL11.glTexCoord2f(1.5F, 0.0F);
		GL11.glVertex2d(width/4, height/2);
		GL11.glTexCoord2f(1.5F, 1.0F);
		GL11.glVertex2d(width/4, 3*height/4);
		GL11.glTexCoord2f(4.5F, 0.0F);
		GL11.glVertex2d(3*width/4, height/2);
		GL11.glTexCoord2f(4.5F, 1.0F);
		GL11.glVertex2d(3*width/4, 3*height/4);
		GL11.glTexCoord2f(6.0F, 0.5F);
		GL11.glVertex2d(width, 5*height/8);
		GL11.glTexCoord2f(6.0F, 1.0F);
		GL11.glVertex2d(width, 3*height/4);		
		GL11.glEnd();
    	
		textureManager.bindTexture(mouldingTexture);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0.0F, 0.0F);
		GL11.glVertex2d(width/4, height/2 - 16);
		GL11.glTexCoord2f(0.0F, 4.0F);
		GL11.glVertex2d(0, 5*height/8 - 16);
		GL11.glTexCoord2f(1.0F, 4.0F);
		GL11.glVertex2d(0, 5*height/8);
		GL11.glTexCoord2f(1.0F, 0.0F);
		GL11.glVertex2d(width/4, height/2);
		
		GL11.glTexCoord2f(0.0F, 8.0F);
		GL11.glVertex2d(3*width/4, height/2 - 16);
		GL11.glTexCoord2f(0.0F, 0.0F);
		GL11.glVertex2d(width/4, height/2 - 16);
		GL11.glTexCoord2f(1.0F, 0.0F);
		GL11.glVertex2d(width/4, height/2);
		GL11.glTexCoord2f(1.0F, 8.0F);
		GL11.glVertex2d(3*width/4, height/2);
		
		GL11.glTexCoord2f(0.0F, 0.0F);
		GL11.glVertex2d(width, 5*height/8 - 16);
		GL11.glTexCoord2f(0.0F, 4.0F);
		GL11.glVertex2d(3*width/4, height/2 - 16);
		GL11.glTexCoord2f(1.0F, 4.0F);
		GL11.glVertex2d(3*width/4, height/2);
		GL11.glTexCoord2f(1.0F, 0.0F);
		GL11.glVertex2d(width, 5*height/8);
		GL11.glEnd();
    }
    
	private static void drawLowerPanel(int width, int height, ResourceLocation backplateTexture){
		textureManager.bindTexture(backplateTexture);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0.0F, 0.0F);
		GL11.glVertex2d(0, 3*height/4);
		GL11.glTexCoord2f(0.0F, 1.0F);
		GL11.glVertex2d(0, height);
		GL11.glTexCoord2f(6.0F, 1.0F);
		GL11.glVertex2d(width, height);
		GL11.glTexCoord2f(6.0F, 0.0F);
		GL11.glVertex2d(width, 3*height/4);
		GL11.glEnd();
    }

	private static void drawAuxiliaryPanel(int width, int height, ResourceLocation backplateTexture, ResourceLocation mouldingTexture){
		textureManager.bindTexture(backplateTexture);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0.0F, 0.0F);
		GL11.glVertex2d(0, height/2+16);
		GL11.glTexCoord2f(0.0F, 1.75F);
		GL11.glVertex2d(0, height);
		GL11.glTexCoord2f(6.0F, 1.75F);
		GL11.glVertex2d(width, height);
		GL11.glTexCoord2f(6.0F, 0.0F);
		GL11.glVertex2d(width, height/2+16);
		GL11.glEnd();
    	
		textureManager.bindTexture(mouldingTexture);
		GL11.glBegin(GL11.GL_QUADS);
    	GL11.glTexCoord2f(0.0F, 16.0F);
		GL11.glVertex2d(width, height/2);
		GL11.glTexCoord2f(0.0F, 0.0F);
		GL11.glVertex2d(0, height/2);
		GL11.glTexCoord2f(1.0F, 0.0F);
		GL11.glVertex2d(0, height/2 + 16);
		GL11.glTexCoord2f(1.0F, 16.0F);
		GL11.glVertex2d(width, height/2 + 16);
		GL11.glEnd();
    }
}
