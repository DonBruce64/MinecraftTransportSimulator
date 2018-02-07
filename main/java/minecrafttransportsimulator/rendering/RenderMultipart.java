package minecrafttransportsimulator.rendering;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Controls;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Instruments;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackBeacon;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackControl;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackDisplayText;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackFileDefinitions;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackInstrument;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackLight;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackRotatableModelObject;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackWindow;
import minecrafttransportsimulator.dataclasses.MTSRegistryClient;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.systems.ClientEventSystem;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderWorldLastEvent;

/**Main render class for all multipart entities.
 * Renders the parent model, and all child models that have been registered by
 * {@link registerChildRender}.  Ensures all parts are rendered in the exact
 * location they should be in as all rendering is done in the same operation.
 * Entities don't render above 255 well due to the new chunk visibility system.
 * This code is present to be called manually from
 * {@link ClientEventSystem#on(RenderWorldLastEvent)}.
 *
 * @author don_bruce
 */
public final class RenderMultipart extends Render<EntityMultipartMoving>{
	private static final Minecraft minecraft = Minecraft.getMinecraft();
	private static final Map<String, Integer> displayLists = new HashMap<String, Integer>();
	private static final Map<String, ResourceLocation> textureMap = new HashMap<String, ResourceLocation>();
	private static final Map<EntityMultipartMoving, Byte> lastRenderPass = new HashMap<EntityMultipartMoving, Byte>();
	private static final Map<EntityMultipartMoving, Long> lastRenderTick = new HashMap<EntityMultipartMoving, Long>();
	private static final Map<EntityMultipartMoving, Float> lastRenderPartial = new HashMap<EntityMultipartMoving, Float>();
	private static final ResourceLocation vanillaGlassTexture = new ResourceLocation("minecraft", "textures/blocks/glass.png");
	private static final ResourceLocation lensFlareTexture = new ResourceLocation(MTS.MODID, "textures/parts/lensflare.png");
	
	public RenderMultipart(RenderManager renderManager){
		super(renderManager);
		RenderMultipartChild.init();
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityMultipartMoving entity){
		return null;
	}
	
	@Override
	public void doRender(EntityMultipartMoving entity, double x, double y, double z, float entityYaw, float partialTicks){
		boolean didRender = false;
		if(entity.pack != null){ 
			if(lastRenderPass.containsKey(entity)){
				//Did we render this tick?
				if(lastRenderTick.get(entity) == entity.worldObj.getTotalWorldTime() && lastRenderPartial.get(entity) == partialTicks){
					//If we rendered last on a pass of 0 or 1 this tick, don't re-render some things.
					if(lastRenderPass.get(entity) != -1 && MinecraftForgeClient.getRenderPass() == -1){
						render(entity, Minecraft.getMinecraft().thePlayer, partialTicks, true);
						didRender = true;
					}
				}
			}
			if(!didRender){
				render(entity, Minecraft.getMinecraft().thePlayer, partialTicks, false);
			}
			lastRenderPass.put(entity, (byte) MinecraftForgeClient.getRenderPass());
			lastRenderTick.put(entity, entity.worldObj.getTotalWorldTime());
			lastRenderPartial.put(entity, partialTicks);
		}
	}
	
	public static void resetDisplayLists(){
		for(Integer displayList : displayLists.values()){
			GL11.glDeleteLists(displayList, 1);
		}
		displayLists.clear();
	}
		
	private static void render(EntityMultipartMoving mover, EntityPlayer playerRendering, float partialTicks, boolean wasRenderedPrior){
		//Calculate various things.
		double playerX;
        double playerY;
        double playerZ;
		Entity renderViewEntity = minecraft.getRenderViewEntity();
		
		boolean wasParentSpawnedAfterSeat = false;
		boolean wasParentSpawnedAfterPlayer = false;
		if(ClientEventSystem.playerLastSeat != null){
			wasParentSpawnedAfterPlayer = mover.getEntityId() > playerRendering.getEntityId();
			if(mover.equals(ClientEventSystem.playerLastSeat.parent)){
				wasParentSpawnedAfterSeat = mover.getEntityId() > ClientEventSystem.playerLastSeat.getEntityId();
				//System.out.format("%d %d %d\n", mover.getEntityId(), ClientEventSystem.playerLastSeat.getEntityId(), playerRendering.getEntityId());
			}
		}

		//FIXME figure out just what boolean goes here.
		//Last issue was with IDs 11336407 11336416 11336394 for parent, seat, and player.
		//Gotta be related to how the parent and seat link.  Rendered fine outside the plane, screwy inside.
		if(wasParentSpawnedAfterSeat){
			playerX = renderViewEntity.posX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * (double)partialTicks;
	        playerY = renderViewEntity.posY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * (double)partialTicks;
	        playerZ = renderViewEntity.posZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * (double)partialTicks;
		}else{
			playerX = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * (double)partialTicks;
	        playerY = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * (double)partialTicks;
	        playerZ = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * (double)partialTicks;
		}
        
        
        double thisX = mover.lastTickPosX + (mover.posX - mover.lastTickPosX) * (double)partialTicks;
        double thisY = mover.lastTickPosY + (mover.posY - mover.lastTickPosY) * (double)partialTicks;
        double thisZ = mover.lastTickPosZ + (mover.posZ - mover.lastTickPosZ) * (double)partialTicks;
        double rotateYaw = -mover.rotationYaw - (mover.rotationYaw - mover.prevRotationYaw)*(double)partialTicks;
        double rotatePitch = mover.rotationPitch + (mover.rotationPitch - mover.prevRotationPitch)*(double)partialTicks;
        double rotateRoll = mover.rotationRoll + (mover.rotationRoll - mover.prevRotationRoll)*(double)partialTicks;

        //Set up position and lighting.
        GL11.glPushMatrix();
        GL11.glTranslated(thisX - playerX, thisY - playerY, thisZ - playerZ);
        int lightVar = mover.getBrightnessForRender(partialTicks);
        minecraft.entityRenderer.enableLightmap();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightVar%65536, lightVar/65536);
        RenderHelper.enableStandardItemLighting();
        
		//Bind texture.  Adds new element to cache if needed.
		PackFileDefinitions definition = PackParserSystem.getDefinitionForPack(mover.name);
		if(!textureMap.containsKey(definition.modelTexture)){
			if(definition.modelTexture.contains(":")){
				textureMap.put(mover.name, new ResourceLocation(definition.modelTexture));
			}else{
				textureMap.put(mover.name, new ResourceLocation(MTS.MODID, "textures/models/" + definition.modelTexture));
			}
		}
		minecraft.getTextureManager().bindTexture(textureMap.get(mover.name));
		//Render all the model parts except windows.
		//Those need to be rendered after the player if the player is rendered manually.
		if(MinecraftForgeClient.getRenderPass() != 1 && !wasRenderedPrior){
			GL11.glPushMatrix();
			GL11.glRotated(rotateYaw, 0, 1, 0);
	        GL11.glRotated(rotatePitch, 1, 0, 0);
	        GL11.glRotated(rotateRoll, 0, 0, 1);
			renderMainModel(mover);
			renderChildren(mover, partialTicks);
			GL11.glEnable(GL11.GL_NORMALIZE);
			renderWindows(mover);
			renderTextMarkings(mover);
			if(mover instanceof EntityMultipartVehicle){
				renderInstrumentsAndControls((EntityMultipartVehicle) mover);
			}
			GL11.glDisable(GL11.GL_NORMALIZE);
			GL11.glPopMatrix();
			if(Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox()){
				renderBoundingBoxes(mover);
				if(mover instanceof EntityPlane){
					renderDebugVectors((EntityPlane) mover);
				}
			}
		}
		
		//Check to see if we need to manually render riders.
		//MC culls rendering above build height depending on the direction the player is looking.
 		//Due to inconsistent culling based on view angle, this can lead to double-renders.
 		//Better than not rendering at all I suppose.
		if(MinecraftForgeClient.getRenderPass() != 1 && !wasRenderedPrior){
			 for(EntityMultipartChild child : mover.getChildren()){
				 if(child instanceof EntitySeat){
			         Entity rider = ((EntitySeat) child).getPassenger();
			         if(rider != null && !(minecraft.thePlayer.equals(rider) && minecraft.gameSettings.thirdPersonView == 0) && rider.posY > rider.worldObj.getHeight()){
			        	 GL11.glPushMatrix();
			        	 GL11.glTranslated(rider.posX - mover.posX, rider.posY - mover.posY, rider.posZ - mover.posZ);
			        	 Minecraft.getMinecraft().getRenderManager().renderEntityStatic(rider, partialTicks, false);
			        	 GL11.glPopMatrix();
			         }
				 }
		     }
		}
		
		//Lights and beacons get rendered in two passes.
		//The first renders the cases and bulbs, the second renders the beams and effects.
		if(mover instanceof EntityMultipartVehicle){
			EntityMultipartVehicle vehicle = (EntityMultipartVehicle) mover;
			float sunLight = vehicle.worldObj.getSunBrightness(0)*vehicle.worldObj.getLightBrightness(vehicle.getPosition());
			float blockLight = vehicle.worldObj.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, vehicle.getPosition())/15F;
			float electricFactor = (float) Math.min(vehicle.electricPower > 2 ? (vehicle.electricPower-2)/6F : 0, 1);
			float lightBrightness = (float) Math.min((1 - Math.max(sunLight, blockLight))*electricFactor, 1);

			GL11.glPushMatrix();
			GL11.glEnable(GL11.GL_NORMALIZE);
			GL11.glRotated(rotateYaw, 0, 1, 0);
	        GL11.glRotated(rotatePitch, 1, 0, 0);
	        GL11.glRotated(rotateRoll, 0, 0, 1);

	        renderLights(vehicle, sunLight, blockLight, lightBrightness, electricFactor, wasRenderedPrior);
			if(!wasRenderedPrior){
				renderBeacons(vehicle, sunLight, blockLight, lightBrightness, electricFactor);
			}
			GL11.glDisable(GL11.GL_NORMALIZE);
			GL11.glPopMatrix();
			
			//Return all states to normal.
			minecraft.entityRenderer.enableLightmap();
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glDepthMask(true);
			GL11.glColor4f(1, 1, 1, 1);
		}
		
		//Make sure lightmaps are set correctly.
		if(MinecraftForgeClient.getRenderPass() == -1){
			RenderHelper.disableStandardItemLighting();
			minecraft.entityRenderer.disableLightmap();
		}else{
			RenderHelper.enableStandardItemLighting();
			minecraft.entityRenderer.enableLightmap();
		}
		 GL11.glPopMatrix();
	}
	
	private static void renderMainModel(EntityMultipartMoving mover){
		GL11.glPushMatrix();
		//Normally we use the pack name, but since all displaylists
		//are the same for all models, this is more appropriate.
		if(displayLists.containsKey(mover.pack.rendering.modelName)){
			GL11.glCallList(displayLists.get(mover.pack.rendering.modelName));
			//The display list only renders static parts.  We need to render dynamic ones manually.
			//Do a null check here to make sure crashes don't occur with trailing commas.
			for(byte i=0; i<mover.pack.rendering.rotatableModelObjects.size(); ++i){
				PackRotatableModelObject rotatable = mover.pack.rendering.rotatableModelObjects.get(i);
				if(rotatable != null){
					if(MTSRegistryClient.modelMap.get(mover.name).get(rotatable.partName) != null){
						GL11.glPushMatrix();
						//First translate to the center of the model for proper rotation.
						GL11.glTranslatef(rotatable.rotationPoint[0], rotatable.rotationPoint[1], rotatable.rotationPoint[2]);
						//Next rotate the part.
						rotateObject(mover, rotatable);
						//Now translate the part back to it's original position.
						GL11.glTranslatef(-rotatable.rotationPoint[0], -rotatable.rotationPoint[1], -rotatable.rotationPoint[2]);
						//Now render the part.
						GL11.glBegin(GL11.GL_TRIANGLES);
						for(Float[] vertex : MTSRegistryClient.modelMap.get(mover.name).get(rotatable.partName)){
							GL11.glTexCoord2f(vertex[3], vertex[4]);
							GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
							GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
						}
						GL11.glEnd();
						GL11.glPopMatrix();
					}else{
						MTS.MTSLog.warn("Invalid rotatable part: " + rotatable.partName + "! Check your part and model names!");
						mover.pack.rendering.rotatableModelObjects.remove(i);
					}
				}
			}
		}else{
			int displayListIndex = GL11.glGenLists(1);
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			for(Entry<String, Float[][]> entry : MTSRegistryClient.modelMap.get(mover.name).entrySet()){
				boolean isRotatable = false;
				for(PackRotatableModelObject rotatable : mover.pack.rendering.rotatableModelObjects){
					if(rotatable != null){
						if(rotatable.partName.equals(entry.getKey())){
							isRotatable = true;
							break;
						}
					}
				}
				//Don't add movable model parts to the display list.
				if(!isRotatable){
					for(Float[] vertex : entry.getValue()){
						GL11.glTexCoord2f(vertex[3], vertex[4]);
						GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
						GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
					}
				}
			}
			GL11.glEnd();
			GL11.glEndList();
			displayLists.put(mover.pack.rendering.modelName, displayListIndex);
		}
		GL11.glPopMatrix();
	}
	
	private static void rotateObject(EntityMultipartMoving mover, PackRotatableModelObject rotatable){
		if(mover instanceof EntityPlane){
			if(rotatable.rotationVariable.equals("aileron")){
				GL11.glRotatef(((EntityPlane) mover).aileronAngle/10F, rotatable.rotationAxisDir[0], rotatable.rotationAxisDir[1], rotatable.rotationAxisDir[2]);
			}else if(rotatable.rotationVariable.equals("elevator")){
				GL11.glRotatef(((EntityPlane) mover).elevatorAngle/10F, rotatable.rotationAxisDir[0], rotatable.rotationAxisDir[1], rotatable.rotationAxisDir[2]);
			}else if(rotatable.rotationVariable.equals("rudder")){
				GL11.glRotatef(((EntityPlane) mover).rudderAngle/10F, rotatable.rotationAxisDir[0], rotatable.rotationAxisDir[1], rotatable.rotationAxisDir[2]);
			}else if(rotatable.rotationVariable.equals("flap")){
				GL11.glRotatef(((EntityPlane) mover).flapAngle/10F, rotatable.rotationAxisDir[0], rotatable.rotationAxisDir[1], rotatable.rotationAxisDir[2]);
			}
		}
	}
	
	private static void renderChildren(EntityMultipartMoving mover, float partialTicks){
		for(EntityMultipartChild child : mover.getChildren()){
			GL11.glPushMatrix();
    		GL11.glTranslatef(child.offsetX, child.offsetY, child.offsetZ);
    		if(child.turnsWithSteer){
    			if(child.offsetZ >= 0){
    				GL11.glRotatef(mover.getSteerAngle(), 0, 1, 0);
    			}else{
    				GL11.glRotatef(-mover.getSteerAngle(), 0, 1, 0);
    			}
    		}
    		RenderMultipartChild.renderChildEntity(child, partialTicks);
			GL11.glPopMatrix();
        }
	}
	
	private static void renderWindows(EntityMultipartMoving mover){
		GL11.glPushMatrix();
		minecraft.getTextureManager().bindTexture(vanillaGlassTexture);
		for(byte i=0; i<mover.pack.rendering.windows.size(); ++i){
			PackWindow window = mover.pack.rendering.windows.get(i);			
			if(!mover.brokenWindows.contains(i)){
				Vector3f v1 = new Vector3f(window.pos1[0], window.pos1[1], window.pos1[2]);
				Vector3f v2 = new Vector3f(window.pos2[0], window.pos2[1], window.pos2[2]);
				Vector3f v3 = new Vector3f(window.pos3[0], window.pos3[1], window.pos3[2]);
				Vector3f norm = Vector3f.cross(Vector3f.sub(v2, v1, null), Vector3f.sub(v3, v1, null), null);
				GL11.glBegin(GL11.GL_TRIANGLES);
				GL11.glTexCoord2f(0.0F, 0.0F);
				GL11.glNormal3f(norm.x, norm.y, norm.z);
				GL11.glVertex3f(window.pos1[0], window.pos1[1], window.pos1[2]);
				GL11.glTexCoord2f(0.0F, 1.0F);
				GL11.glNormal3f(norm.x, norm.y, norm.z);
				GL11.glVertex3f(window.pos2[0], window.pos2[1], window.pos2[2]);
				GL11.glTexCoord2f(1.0F, 1.0F);
				GL11.glNormal3f(norm.x, norm.y, norm.z);
				GL11.glVertex3f(window.pos3[0], window.pos3[1], window.pos3[2]);
				if(window.pos4 != null){
					GL11.glTexCoord2f(1.0F, 1.0F);
					GL11.glNormal3f(norm.x, norm.y, norm.z);
					GL11.glVertex3f(window.pos3[0], window.pos3[1], window.pos3[2]);
					GL11.glTexCoord2f(1.0F, 0.0F);
					GL11.glNormal3f(norm.x, norm.y, norm.z);
					GL11.glVertex3f(window.pos4[0], window.pos4[1], window.pos4[2]);
					GL11.glTexCoord2f(0.0F, 0.0F);
					GL11.glNormal3f(norm.x, norm.y, norm.z);
					GL11.glVertex3f(window.pos1[0], window.pos1[1], window.pos1[2]);
				}
				
				if(ConfigSystem.getBooleanConfig("InnerWindows")){
					GL11.glTexCoord2f(1.0F, 1.0F);
					GL11.glNormal3f(norm.x, norm.y, norm.z);
					GL11.glVertex3f(window.pos3[0], window.pos3[1], window.pos3[2]);
					GL11.glTexCoord2f(1.0F, 0.0F);
					GL11.glNormal3f(norm.x, norm.y, norm.z);
					GL11.glVertex3f(window.pos2[0], window.pos2[1], window.pos2[2]);
					GL11.glTexCoord2f(0.0F, 0.0F);
					GL11.glNormal3f(norm.x, norm.y, norm.z);
					GL11.glVertex3f(window.pos1[0], window.pos1[1], window.pos1[2]);
					
					if(window.pos4 != null){
						GL11.glTexCoord2f(0.0F, 0.0F);
						GL11.glNormal3f(norm.x, norm.y, norm.z);
						GL11.glVertex3f(window.pos1[0], window.pos1[1], window.pos1[2]);
						GL11.glTexCoord2f(0.0F, 1.0F);
						GL11.glNormal3f(norm.x, norm.y, norm.z);
						GL11.glVertex3f(window.pos4[0], window.pos4[1], window.pos4[2]);
						GL11.glTexCoord2f(1.0F, 1.0F);
						GL11.glNormal3f(norm.x, norm.y, norm.z);
						GL11.glVertex3f(window.pos3[0], window.pos3[1], window.pos3[2]);
					}
				}
				GL11.glEnd();
			}
		}
		GL11.glPopMatrix();
	}
	
	private static void renderTextMarkings(EntityMultipartMoving mover){
		for(PackDisplayText text : mover.pack.rendering.textMarkings){
			GL11.glPushMatrix();
			GL11.glTranslatef(text.pos[0], text.pos[1], text.pos[2]);
			GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
			GL11.glRotatef(text.rot[0], 1, 0, 0);
			GL11.glRotatef(text.rot[1] + 180, 0, 1, 0);
			GL11.glRotatef(text.rot[2] + 180, 0, 0, 1);
			GL11.glScalef(text.scale, text.scale, text.scale);
			RenderHelper.disableStandardItemLighting();
			minecraft.fontRendererObj.drawString(mover.displayText, -minecraft.fontRendererObj.getStringWidth(mover.displayText)/2, 0, Color.decode(text.color).getRGB());
			GL11.glPopMatrix();
		}
		GL11.glColor3f(1.0F, 1.0F, 1.0F);
		RenderHelper.enableStandardItemLighting();
	}
	
	private static void renderLights(EntityMultipartVehicle vehicle, float sunLight, float blockLight, float lightBrightness, float electricFactor, boolean wasRenderedPrior){		
		for(PackLight light : vehicle.pack.rendering.lights){
			boolean lightOn = (vehicle.lightStatus>>(light.switchNumber-1) & 1) == 1;
			boolean overrideCaseBrightness = lightBrightness > Math.max(sunLight, blockLight) && lightOn && lightBrightness > 0;

			if(MinecraftForgeClient.getRenderPass() != 1 && !wasRenderedPrior){
				GL11.glPushMatrix();
				if(overrideCaseBrightness){
					GL11.glDisable(GL11.GL_LIGHTING);
					minecraft.entityRenderer.disableLightmap();
				}else{
					GL11.glEnable(GL11.GL_LIGHTING);
					minecraft.entityRenderer.enableLightmap();
				}
				GL11.glEnable(GL11.GL_TEXTURE_2D);
				GL11.glDisable(GL11.GL_BLEND);
				minecraft.getTextureManager().bindTexture(vanillaGlassTexture);
				GL11.glTranslatef(light.pos[0], light.pos[1], light.pos[2]);
				GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
				GL11.glRotatef(light.rot[0], 1, 0, 0);
				GL11.glRotatef(light.rot[1], 0, 1, 0);
				GL11.glRotatef(light.rot[2], 0, 0, 1);
				GL11.glColor3f(1.0F, 1.0F, 1.0F);
				
				//Light case cover.
				GL11.glPushMatrix();
				GL11.glTranslatef(0, 0, 0.01F);
				GL11.glRotatef(180, 0, 1, 0);
				renderQuad(light.width, light.length);
				GL11.glPopMatrix();
				
				if(lightOn){
					//Light pre-render operations
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					GL11.glDisable(GL11.GL_LIGHTING);
					GL11.glEnable(GL11.GL_BLEND);
					Color lightColor = Color.decode(light.color);
					GL11.glColor4f(lightColor.getRed(), lightColor.getGreen(), lightColor.getBlue(), electricFactor);
					
					//Light center.
					GL11.glPushMatrix();
					GL11.glTranslatef(0, 0, 0.005F);
					GL11.glRotatef(180, 1, 0, 0);
					renderQuad(light.width, light.length);
					GL11.glPopMatrix();
					
					GL11.glDisable(GL11.GL_BLEND);
					GL11.glEnable(GL11.GL_LIGHTING);
					GL11.glEnable(GL11.GL_TEXTURE_2D);
				}
				GL11.glPopMatrix();
			}
			
			//Light cone
			if(lightOn && light.beamDistance != 0 && MinecraftForgeClient.getRenderPass() == -1 && lightBrightness > 0){
				GL11.glPushMatrix();
		    	GL11.glColor4f(1, 1, 1, Math.min(vehicle.electricPower > 4 ? 1.0F : 0, lightBrightness/2F));
		    	GL11.glDisable(GL11.GL_TEXTURE_2D);
		    	GL11.glDisable(GL11.GL_LIGHTING);
		    	GL11.glEnable(GL11.GL_BLEND);
		    	//Allows changing by changing alpha value.
		    	GL11.glDepthMask(false);
		    	GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
		    	
				minecraft.entityRenderer.disableLightmap();
				GL11.glTranslatef(light.pos[0], light.pos[1], light.pos[2]);
				GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
				GL11.glRotatef(light.lightRot[0], 0, 1, 0);
				GL11.glRotatef(light.lightRot[1], 1, 0, 0);
				GL11.glRotatef(light.lightRot[2], 0, 0, 0);
				GL11.glScalef(16F, 16F, 16F);
				GL11.glDepthMask(false);
				byte lightReps = (byte) ((light.brightness/20F) + 1);
				for(byte i=0; i<=lightReps; ++i){
		    		drawCone(light.beamDiameter, light.beamDistance, false);
		    	}
		    	drawCone(light.beamDiameter, light.beamDistance, true);
		    	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		    	GL11.glDepthMask(true);
				GL11.glPopMatrix();
			}
			
			if(MinecraftForgeClient.getRenderPass() != 0 && !wasRenderedPrior){
				//Render lens flare in pass 1 or -1 for transparency.
				if(lightOn && lightBrightness > 0){
					renderLensFlare(light.pos, light.rot, light.brightness, light.color, lightBrightness, true);
				}
			}
		}
	}
	
    private static void drawCone(double r, double l, boolean reverse){
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glVertex3d(0, 0, 0);
    	if(reverse){
    		for(float theta=0; theta < 2*Math.PI + 0.1; theta += 2F*Math.PI/40F){
    			GL11.glVertex3d(r*Math.cos(theta), r*Math.sin(theta), l);
    		}
    	}else{
    		for(float theta=(float) (2*Math.PI); theta>=0 - 0.1; theta -= 2F*Math.PI/40F){
    			GL11.glVertex3d(r*Math.cos(theta), r*Math.sin(theta), l);
    		}
    	}
    	GL11.glEnd();
    }
	
	private static void renderBeacons(EntityMultipartVehicle vehicle, float sunLight, float blockLight, float lightBrightness, float electricFactor){		
		for(PackBeacon beacon : vehicle.pack.rendering.beacons){
			boolean beaconOn = (vehicle.lightStatus>>(beacon.switchNumber-1) & 1) == 1 && (!beacon.flashing || vehicle.ticksExisted%20 <= 2);
			boolean overrideCaseBrightness = lightBrightness > Math.max(sunLight, blockLight) && beaconOn;

			if(MinecraftForgeClient.getRenderPass() != 1){
				GL11.glPushMatrix();
				if(overrideCaseBrightness){
					GL11.glDisable(GL11.GL_LIGHTING);
					minecraft.entityRenderer.disableLightmap();
				}else{
					GL11.glEnable(GL11.GL_LIGHTING);
					minecraft.entityRenderer.enableLightmap();
				}
				GL11.glEnable(GL11.GL_TEXTURE_2D);
				GL11.glDisable(GL11.GL_BLEND);
				minecraft.getTextureManager().bindTexture(vanillaGlassTexture);
				GL11.glTranslatef(beacon.pos[0], beacon.pos[1], beacon.pos[2]);
				GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
				GL11.glRotatef(beacon.rot[0], 1, 0, 0);
				GL11.glRotatef(beacon.rot[1], 0, 1, 0);
				GL11.glRotatef(beacon.rot[2], 0, 0, 1);
				GL11.glColor3f(1.0F, 1.0F, 1.0F);
			
				//Light case edges.
				for(byte i=0; i<=3; ++i){
					GL11.glPushMatrix();
					GL11.glRotatef(90*i, 0, 1, 0);
					if(i == 0 || i == 2){
						GL11.glTranslatef(0, beacon.height/2F, -beacon.length/2F);
						renderQuad(beacon.width, beacon.height);
					}else{
						GL11.glTranslatef(0, beacon.height/2F, -beacon.width/2F);
						renderQuad(beacon.length, beacon.height);
					}
					GL11.glPopMatrix();
				}
				
				//Light case cover.
				GL11.glPushMatrix();
				GL11.glTranslatef(0, beacon.height, 0);
				GL11.glRotatef(90, 1, 0, 0);
				renderQuad(beacon.width, beacon.length);
				GL11.glPopMatrix();
				
				if(beaconOn){
					//Light pre-render operations
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					GL11.glDisable(GL11.GL_LIGHTING);
					GL11.glEnable(GL11.GL_BLEND);
					Color beaconColor = Color.decode(beacon.color);
					GL11.glColor4f(beaconColor.getRed(), beaconColor.getGreen(), beaconColor.getBlue(), electricFactor);
					
					//Light edges.
					for(byte i=0; i<=3; ++i){
						GL11.glPushMatrix();
						GL11.glRotatef(90*i, 0, 1, 0);
						if(i == 0 || i == 2){
							GL11.glTranslatef(0, beacon.height/2F - 0.01F, -beacon.length/2F + 0.01F);
							renderQuad(beacon.width, beacon.height);
						}else{
							GL11.glTranslatef(0, beacon.height/2F, -beacon.width/2F);
							renderQuad(beacon.length, beacon.height);
						}
						GL11.glPopMatrix();
					}
					
					
					//Light center.
					GL11.glPushMatrix();
					GL11.glTranslatef(0, beacon.height - 0.005F, 0);
					GL11.glRotatef(90, 1, 0, 0);
					renderQuad(beacon.width - 0.01F, beacon.length - 0.01F);
					GL11.glPopMatrix();
					
					GL11.glDisable(GL11.GL_BLEND);
					GL11.glEnable(GL11.GL_LIGHTING);
					GL11.glEnable(GL11.GL_TEXTURE_2D);
				}				
				GL11.glPopMatrix();
			}
			if(MinecraftForgeClient.getRenderPass() != 0){
				//Render lens flare in pass 1 or -1 for transparency.
				if(beaconOn && lightBrightness > 0){
					renderLensFlare(beacon.pos, beacon.rot, beacon.brightness, beacon.color, lightBrightness, false);
				}
			}
		}
	}
	
	private static void renderLensFlare(float[] pos, float[] rot, float size, String color, float brightness, boolean isLight){
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LIGHTING);
		minecraft.entityRenderer.disableLightmap();
		minecraft.getTextureManager().bindTexture(lensFlareTexture);
		GL11.glTranslatef(pos[0], pos[1], pos[2]);
		GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
		GL11.glRotatef(rot[0], 1, 0, 0);
		GL11.glRotatef(rot[1], 0, 1, 0);
		GL11.glRotatef(rot[2], 0, 0, 1);
		if(isLight){
			GL11.glRotatef(180, 0, 1, 0);
		}else{
			GL11.glRotatef(90, 1, 0, 0);
		}
		GL11.glTranslatef(0, 0, -0.001F);
		
		Color flareColor = Color.decode(color);
		GL11.glColor4f(flareColor.getRed()/255F, flareColor.getGreen()/255F, flareColor.getBlue()/255F, brightness);
		renderQuad(size*brightness, size*brightness);
		GL11.glPopMatrix();
	}
	
	private static void renderQuad(float width, float height){
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0, 0);
		GL11.glNormal3f(0, 0, -1);
		GL11.glVertex3f(width/2, height/2, 0);
		GL11.glTexCoord2f(0, 1);
		GL11.glNormal3f(0, 0, -1);
		GL11.glVertex3f(width/2, -height/2, 0);
		GL11.glTexCoord2f(1, 1);
		GL11.glNormal3f(0, 0, -1);
		GL11.glVertex3f(-width/2, -height/2, 0);
		GL11.glTexCoord2f(1, 0);
		GL11.glNormal3f(0, 0, -1);
		GL11.glVertex3f(-width/2, height/2, 0);
		GL11.glEnd();
	}
	
	private static void renderInstrumentsAndControls(EntityMultipartVehicle vehicle){
		GL11.glPushMatrix();
		GL11.glScalef(1F/16F/8F, 1F/16F/8F, 1F/16F/8F);
		for(byte i=0; i<vehicle.pack.motorized.instruments.size(); ++i){
			Instruments instrument = vehicle.getInstrumentNumber(i);
			PackInstrument packInstrument = vehicle.pack.motorized.instruments.get(i);
			if(instrument != null && packInstrument != null){
				GL11.glPushMatrix();
				GL11.glTranslatef(packInstrument.pos[0]*8, packInstrument.pos[1]*8, packInstrument.pos[2]*8);
				GL11.glRotatef(packInstrument.rot[0], 1, 0, 0);
				GL11.glRotatef(packInstrument.rot[1], 0, 1, 0);
				GL11.glRotatef(packInstrument.rot[2], 0, 0, 1);
				GL11.glScalef(packInstrument.scale, packInstrument.scale, packInstrument.scale);
				RenderInstruments.drawInstrument(vehicle, 0, 0, instrument, false, packInstrument.optionalEngineNumber);
				GL11.glPopMatrix();
			}
		}
		for(byte i=0; i<vehicle.pack.motorized.controls.size(); ++i){
			PackControl packControl = vehicle.pack.motorized.controls.get(i);
			GL11.glPushMatrix();
			GL11.glTranslatef(packControl.pos[0]*8, packControl.pos[1]*8, packControl.pos[2]*8);
			for(Controls control : Controls.values()){
				if(control.name().toLowerCase().equals(packControl.controlName)){
					RenderControls.drawControl(vehicle, control, false);
				}
			}
			GL11.glPopMatrix();
		}
		GL11.glPopMatrix();
	}
	
	private static void renderBoundingBoxes(EntityMultipartMoving mover){
		List<AxisAlignedBB> boxesToRender = new ArrayList<AxisAlignedBB>();
		for(EntityMultipartChild child : mover.getChildren()){
			MTSVector childOffset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, mover.rotationPitch, mover.rotationYaw, mover.rotationRoll);
			boxesToRender.add(child.getEntityBoundingBox().offset(-child.posX + childOffset.xCoord, -child.posY + childOffset.yCoord, -child.posZ + childOffset.zCoord));
		}
		
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glColor3f(0.0F, 0.0F, 0.0F);
		GL11.glLineWidth(3.0F);
		for(AxisAlignedBB box : boxesToRender){
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex3d(box.minX, box.minY, box.minZ);
			GL11.glVertex3d(box.maxX, box.minY, box.minZ);
			GL11.glVertex3d(box.minX, box.minY, box.maxZ);
			GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
			GL11.glVertex3d(box.minX, box.maxY, box.minZ);
			GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
			GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
			GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
			
			GL11.glVertex3d(box.minX, box.minY, box.minZ);
			GL11.glVertex3d(box.minX, box.minY, box.maxZ);
			GL11.glVertex3d(box.maxX, box.minY, box.minZ);
			GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
			GL11.glVertex3d(box.minX, box.maxY, box.minZ);
			GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
			GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
			GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
			
			GL11.glVertex3d(box.minX, box.minY, box.minZ);
			GL11.glVertex3d(box.minX, box.maxY, box.minZ);
			GL11.glVertex3d(box.maxX, box.minY, box.minZ);
			GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
			GL11.glVertex3d(box.minX, box.minY, box.maxZ);
			GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
			GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
			GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
			GL11.glEnd();
		}
		GL11.glLineWidth(1.0F);
		GL11.glColor3f(1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glPopMatrix();
	}
	
	private static void renderDebugVectors(EntityPlane plane){
		double[] debugForces = plane.getDebugForces();
    	GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		GL11.glLineWidth(1);
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(0, 2, 0);
		GL11.glVertex3d(plane.headingVec.xCoord*debugForces[0], 2 + plane.headingVec.yCoord*debugForces[0],  plane.headingVec.zCoord*debugForces[0]);
		
		GL11.glVertex3d(Math.cos(Math.toRadians(plane.rotationYaw)), 2, Math.sin(Math.toRadians(plane.rotationYaw)));
		GL11.glVertex3d(Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*debugForces[2]/10, 2 + plane.verticalVec.yCoord*debugForces[2]/10,  Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*debugForces[2]/10);

		GL11.glVertex3d(-Math.cos(Math.toRadians(plane.rotationYaw)), 2, -Math.sin(Math.toRadians(plane.rotationYaw)));
		GL11.glVertex3d(-Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*debugForces[2]/10, 2 + plane.verticalVec.yCoord*debugForces[2]/10,  -Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*debugForces[2]/10);
		GL11.glEnd();
		
		GL11.glLineWidth(5);
		GL11.glColor4f(1, 0, 0, 1);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(plane.headingVec.xCoord*debugForces[0], 2 + plane.headingVec.yCoord*debugForces[0],  plane.headingVec.zCoord*debugForces[0]);
		GL11.glVertex3d(plane.headingVec.xCoord*(debugForces[0] - debugForces[1]), 2 + plane.headingVec.yCoord*(debugForces[0] - debugForces[1]),  plane.headingVec.zCoord*(debugForces[0] - debugForces[1]));
		
		GL11.glVertex3d(Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*debugForces[2]/10, 2 + plane.verticalVec.yCoord*debugForces[2]/10,  Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*debugForces[2]/10);
		GL11.glVertex3d(Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*(debugForces[2] - debugForces[3])/10, 2 + plane.verticalVec.yCoord*(debugForces[2] - debugForces[3])/10,  Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*(debugForces[2] - debugForces[3])/10);
		
		GL11.glVertex3d(-Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*debugForces[2]/10, 2 + plane.verticalVec.yCoord*debugForces[2]/10,  -Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*debugForces[2]/10);
		GL11.glVertex3d(-Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*(debugForces[2] - debugForces[3])/10, 2 + plane.verticalVec.yCoord*(debugForces[2] - debugForces[3])/10,  -Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*(debugForces[2] - debugForces[3])/10);
		GL11.glEnd();
		
		GL11.glColor4f(0, 0, 1, 1);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(0, 2, 0);
		GL11.glVertex3d(plane.velocityVec.xCoord*plane.velocity, 2 + plane.velocityVec.yCoord*plane.velocity,  plane.velocityVec.zCoord*plane.velocity);
		GL11.glEnd();
		
		GL11.glColor4f(0, 1, 0, 1);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(0, 2, 0);
		GL11.glVertex3d(plane.headingVec.xCoord, 2 + plane.headingVec.yCoord,  plane.headingVec.zCoord);
		GL11.glEnd();
				
		GL11.glLineWidth(1);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
	}
}
