package minecrafttransportsimulator.rendering.blockrenders;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.TileEntityTrafficSignalController;
import minecrafttransportsimulator.blocks.pole.BlockPoleAttachment;
import minecrafttransportsimulator.blocks.pole.TileEntityPoleWallConnector;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Car;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumSkyBlock;

public class RenderPoleLighted extends TileEntitySpecialRenderer<TileEntityPoleWallConnector>{
	private static final ResourceLocation vanillaGlassTexture = new ResourceLocation("minecraft", "textures/blocks/glass.png");
	private static final ResourceLocation lensFlareTexture = new ResourceLocation(MTS.MODID, "textures/rendering/lensflare.png");
	private static final ResourceLocation lightTexture = new ResourceLocation(MTS.MODID, "textures/rendering/light.png");
	private static final ResourceLocation lightBeamTexture = new ResourceLocation(MTS.MODID, "textures/rendering/lightbeam.png");
		
	public RenderPoleLighted(){}
	
	@Override
	public void render(TileEntityPoleWallConnector polePart, double x, double y, double z, float partialTicks, int destroyStage, float alpha){
		super.render(polePart, x, y, z, partialTicks, destroyStage, alpha);
		final Vec3i facingVec = EnumFacing.VALUES[polePart.rotation].getDirectionVec();
		final Block block = polePart.getWorld().getBlockState(polePart.getPos()).getBlock();
		//Check to make sure we have the right block before continuing.
		//We may not have the right one due to the TE being orphaned for some reason.
		if(!(block instanceof BlockPoleAttachment)){
			return;
		}
		final BlockPoleAttachment decorBlock = (BlockPoleAttachment) block;
		final float sunLight = polePart.getWorld().getSunBrightness(0)*polePart.getWorld().getLightBrightness(polePart.getPos());
		final float blockLight = polePart.getWorld().getLightFromNeighborsFor(EnumSkyBlock.BLOCK, polePart.getPos())/15F;
		final float lightBrightness = (float) Math.min((1 - Math.max(sunLight, blockLight)), 1);
		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glTranslatef(0.5F, 0F, 0.5F);
	
		if(facingVec.getX() == 1){
			GL11.glRotatef(90, 0, 1, 0);
		}else if(facingVec.getX() == -1){
			GL11.glRotatef(270, 0, 1, 0);
		}else if(facingVec.getZ() == -1){
			GL11.glRotatef(180, 0, 1, 0);
		}
		
		Minecraft.getMinecraft().entityRenderer.disableLightmap();
		GL11.glDisable(GL11.GL_LIGHTING);
		if(decorBlock.equals(MTSRegistry.trafficSignal)){
			renderTrafficSignal(polePart, facingVec, lightBrightness);
		}else if(decorBlock.equals(MTSRegistry.streetLight)){
			renderStreetLight(polePart, facingVec, lightBrightness);
		}
		GL11.glEnable(GL11.GL_LIGHTING);
		Minecraft.getMinecraft().entityRenderer.enableLightmap();
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glPopMatrix();
	}
	
	private void renderLightedSquare(float lightSize, float lightBrightness, Color lightColor){
		final float flareSize = lightSize*4F;
		bindTexture(lightTexture);
		GL11.glColor3f(lightColor.getRed()/255F, lightColor.getGreen()/255F, lightColor.getBlue()/255F);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(1, 0);
		GL11.glVertex3f(-lightSize/2F, -lightSize/2F, 0);
		GL11.glTexCoord2f(1, 1);
		GL11.glVertex3f(lightSize/2F, -lightSize/2F, 0);
		GL11.glTexCoord2f(0, 1);
		GL11.glVertex3f(lightSize/2F, lightSize/2F, 0);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3f(-lightSize/2F, lightSize/2F, 0);
		GL11.glEnd();
		
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		bindTexture(lensFlareTexture);
		GL11.glColor4f(lightColor.getRed()/255F, lightColor.getGreen()/255F, lightColor.getBlue()/255F, lightBrightness);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(1, 0);
		GL11.glVertex3f(-flareSize/2F, -flareSize/2F, 0);
		GL11.glTexCoord2f(1, 1);
		GL11.glVertex3f(flareSize/2F, -flareSize/2F, 0);
		GL11.glTexCoord2f(0, 1);
		GL11.glVertex3f(flareSize/2F, flareSize/2F, 0);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3f(-flareSize/2F, flareSize/2F, 0);
		GL11.glEnd();
		GL11.glDisable(GL11.GL_BLEND);
	}
	

	private static final Map<BlockPos, BlockPos> trafficSignalControllers = new HashMap<BlockPos, BlockPos>();
	private static final Map<BlockPos, Integer> checkIndexList = new HashMap<BlockPos, Integer>();
	
	private void renderTrafficSignal(TileEntityPoleWallConnector signal, Vec3i facingVec, float lightBrightness){
		//Render the lights for the traffic signal.  What lights we render depends on the controller state.
		final boolean shouldFlash;
		final Color lightColor;
		final long worldTime = signal.getWorld().getTotalWorldTime();
		BlockPos signalPos = signal.getPos();
		
		//First get the controller for this signal if we don't have it.
		if(!trafficSignalControllers.containsKey(signalPos)){
			//Only check 1 TE at a time from the TE list to reduce overhead.
			int checkIndex = checkIndexList.containsKey(signalPos) ? checkIndexList.get(signalPos) : 0;
			if(signal.getWorld().loadedTileEntityList.size() <= checkIndex){
				checkIndex = 0;
			}
			TileEntity testTile = signal.getWorld().loadedTileEntityList.get(checkIndex);
			if(testTile instanceof TileEntityTrafficSignalController){
				if(((TileEntityTrafficSignalController) testTile).trafficSignalLocations.contains(signalPos)){
					//Found our signal.
					trafficSignalControllers.put(signalPos, testTile.getPos());
				}
			}
			checkIndexList.put(signalPos, checkIndex + 1);
		}
		
		//Now render this signal.
		if(trafficSignalControllers.containsKey(signalPos)){
			//Check to make sure this controller is still valid.
			TileEntity tile = signal.getWorld().getTileEntity(trafficSignalControllers.get(signalPos));
			if(tile instanceof TileEntityTrafficSignalController){
				TileEntityTrafficSignalController controller = (TileEntityTrafficSignalController) tile;
				//We are valid, now check to see if we are still part of the controller.
				if(controller.trafficSignalLocations.contains(signalPos)){
					//Valid controller detected, do logic.
					shouldFlash = false;
					final boolean isOnMainAxis = !(controller.orientedOnX ^ (facingVec.getX() != 0));

					if(controller.operationIndex == 0){
						//First step, main light turns green, cross light stays red.
						//If we are a signal-controlled light, we stay here until we get a signal.
						//If we are a timed light, we immediately move to state 1 as our
						//green time is an extra state to enable looping.
						lightColor = isOnMainAxis ? Color.GREEN : Color.RED;
						if(controller.triggerMode){
							//Only check every two seconds to prevent lag.
							if(worldTime%40 == 0){
								//Get a bounding box for all lights in the controller system.
								int minX = Integer.MAX_VALUE;
								int maxX = Integer.MIN_VALUE;
								int minZ = Integer.MAX_VALUE;
								int maxZ = Integer.MIN_VALUE;
								for(BlockPos controllerSignalPos : controller.trafficSignalLocations){
									minX = Math.min(minX, controllerSignalPos.getX());
									maxX = Math.max(maxX, controllerSignalPos.getX());
									minZ = Math.min(minX, controllerSignalPos.getZ());
									maxZ = Math.max(maxX, controllerSignalPos.getZ());
								}
								
								//Take 10 off to expand the detection boxes for the axis.
								if(controller.orientedOnX){
									minZ -= 10;
									maxZ += 10;
								}else{
									minX -= 10;
									maxX += 10;
								}
								
								//Now we have min-max, check for any vehicles in the area.
								//We need to check along the non-primary axis.
								for(Entity entity : controller.getWorld().loadedEntityList){
									if(entity instanceof EntityVehicleF_Car){
										if(controller.orientedOnX){
											if((entity.posZ > minZ && entity.posZ < minZ + (maxZ - minZ)/2F) || (entity.posZ < maxZ && entity.posZ > maxZ - (maxZ - minZ)/2F)){
												if(entity.posX > minX && entity.posX < maxX){
													controller.timeOperationStarted = worldTime;
													controller.operationIndex = 1;
												}
											}
										}else{
											if((entity.posX > minX && entity.posX < minX + (maxX - minX)/2F) || (entity.posX < maxX && entity.posX > maxX - (maxX - minX)/2F)){
												if(entity.posZ > minZ && entity.posZ < maxZ){
													controller.timeOperationStarted = worldTime;
													controller.operationIndex = 1;
												}
											}
										}
									}
								}
							}
						}else{
							controller.timeOperationStarted = worldTime;
							controller.operationIndex = 1;
						}
					}else if(controller.operationIndex == 1){
						//Second step, main light turns yellow, cross light stays red.
						lightColor = isOnMainAxis ? Color.YELLOW : Color.RED;
						if(controller.timeOperationStarted + controller.yellowTime <= worldTime){
							controller.timeOperationStarted = worldTime;
							controller.operationIndex = 2;
						}
					}else if(controller.operationIndex == 2){
						//Third step, main light turns red, cross light stays red.
						lightColor = Color.RED;
						if(controller.timeOperationStarted + controller.allRedTime <= worldTime){
							controller.timeOperationStarted = worldTime;
							controller.operationIndex = 3;
						}
					}else if(controller.operationIndex == 3){
						//Fourth step, main light stays red, cross light turns green.
						lightColor = isOnMainAxis ? Color.RED : Color.GREEN;
						if(controller.timeOperationStarted + controller.greenCrossTime <= worldTime){
							controller.timeOperationStarted = worldTime;
							controller.operationIndex = 4;
						}
					}else if(controller.operationIndex == 4){
						//Fifth step, main light stays red, cross light turns yellow.
						lightColor = isOnMainAxis ? Color.RED : Color.YELLOW;
						if(controller.timeOperationStarted + controller.yellowTime <= worldTime){
							controller.timeOperationStarted = worldTime;
							controller.operationIndex = 5;
						}
					}else if(controller.operationIndex == 5){
						//Sixth step, main light stays red, cross light turns red.
						lightColor = Color.RED;
						if(controller.timeOperationStarted + controller.allRedTime <= worldTime){
							controller.timeOperationStarted = worldTime;
							//If we are a triggered light, we go back to 0.
							//Otherwise, we perform another cycle and go back to 1.
							if(controller.triggerMode){
								controller.operationIndex = 0;
							}else{
								controller.timeOperationStarted = worldTime;
								controller.operationIndex = 6;
							}
						}
					}else{
						//Sixth step, main light turns green, cross light stays red.
						lightColor = isOnMainAxis ? Color.GREEN : Color.RED;
						if(controller.timeOperationStarted + controller.greenMainTime <= worldTime){
							controller.timeOperationStarted = worldTime;
							controller.operationIndex = 0;
						}
					}
				}else{
					trafficSignalControllers.remove(signalPos);
					shouldFlash = true;
					lightColor = Color.RED;
				}
			}else{
				trafficSignalControllers.remove(signalPos);
				shouldFlash = true;
				lightColor = Color.RED;
			}
		}else{
			shouldFlash = true;
			lightColor = Color.RED;
		}

		if(!shouldFlash || (shouldFlash && (worldTime%20 < 10))){
			GL11.glTranslatef(0, lightColor.equals(Color.RED) ? 13F/16F : (lightColor.equals(Color.YELLOW) ? 8F/16F : 3F/16F), 0.225F);
			renderLightedSquare(4F/16F, lightBrightness, lightColor);
		}
	}

	private void renderStreetLight(TileEntityPoleWallConnector light, Vec3i facingVec, float lightBrightness){
		//Render light square
		GL11.glTranslatef(0, 6.45F/16F, 6F/16F);
		GL11.glRotatef(90, 1, 0, 0);
		renderLightedSquare(4F/16F, lightBrightness,  Color.WHITE);
		
		//Render light beam
		GL11.glPushMatrix();
    	GL11.glDisable(GL11.GL_LIGHTING);
    	GL11.glEnable(GL11.GL_BLEND);
		bindTexture(lightBeamTexture);
    	GL11.glColor4f(1, 1, 1, Math.min(1.0F, lightBrightness/2F));
    	//Allows making things brighter by using alpha blending.
    	GL11.glDepthMask(false);
    	GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 0, -0.15F);
		Vec3d endpointVec = new Vec3d(0, 0, 6);
		GL11.glDepthMask(false);
		for(byte j=0; j<=2; ++j){
			drawLightCone(endpointVec, false);
    	}
		drawLightCone(endpointVec, true);
		GL11.glPopMatrix();
		
    	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    	GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glPopMatrix();
	}
	
	private static void drawLightCone(Vec3d endPoint, boolean reverse){
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3d(0, 0, 0);
		if(reverse){
			for(float theta=0; theta < 2*Math.PI + 0.1; theta += 2F*Math.PI/40F){
				GL11.glTexCoord2f(theta, 1);
				GL11.glVertex3d(endPoint.x + 3.0F*Math.cos(theta), endPoint.y + 3.0F*Math.sin(theta), endPoint.z);
			}
		}else{
			for(float theta=(float) (2*Math.PI); theta>=0 - 0.1; theta -= 2F*Math.PI/40F){
				GL11.glTexCoord2f(theta, 1);
				GL11.glVertex3d(endPoint.x + 3.0F*Math.cos(theta), endPoint.y + 3.0F*Math.sin(theta), endPoint.z);
			}
		}
		GL11.glEnd();
	}
}
