package minecraftflightsimulator.rendering.modelrenders;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.blocks.TileEntityRail;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import minecraftflightsimulator.rendering.partmodels.ModelRail;
import minecraftflightsimulator.systems.GL11DrawSystem;
import minecraftflightsimulator.systems.RenderSystem.RenderTileBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class RenderRail extends RenderTileBase{
	public static final ModelRail model = new ModelRail();

	@Override
	protected void doRender(TileEntity tile, double x, double y, double z){
		TileEntityRail rail = (TileEntityRail) tile;
		if(!rail.isPrimary){
			if(rail.curve != null){
				TileEntityRail otherEnd = (TileEntityRail) BlockHelper.getTileEntityFromCoords(rail.getWorldObj(), (int) (rail.curve.endPoint[0] - 0.5F), (int) rail.curve.endPoint[1], (int) (rail.curve.endPoint[2] - 0.5F));
				if(otherEnd != null){
					if(otherEnd.renderedLastPass){
						return;
					}
				}
			}
		}
		if(rail.curve != null){
			float[] currentPoint;
			float[] nextPoint;
			float[] currentOffset = new float[3];
			float[] nextOffset = new float[3];
			
			float currentAngle;
			float currentPitch;
			float currentSin;
			float currentCos;
			float nextAngle;
			float nextPitch;
			float nextSin;
			float nextCos;
			
			TileEntityRail startConnector = null;
			TileEntityRail endConnector = null;
			
			for(byte i=-1; i<=1; ++i){
				for(byte j=-1; j<=1; ++j){
					if(!(i == 0 && j == 0)){
						TileEntity testTileStart = BlockHelper.getTileEntityFromCoords(rail.getWorldObj(), (int) (rail.curve.startPoint[0] - 0.5F + i), (int) rail.curve.startPoint[1], (int) (rail.curve.startPoint[2] - 0.5F + j));
						TileEntity testTileEnd = BlockHelper.getTileEntityFromCoords(rail.getWorldObj(), (int) (rail.curve.endPoint[0] - 0.5F + i), (int) rail.curve.endPoint[1], (int) (rail.curve.endPoint[2] - 0.5F + j));
						
						if(testTileStart != null){
							if(testTileStart instanceof TileEntityRail){
								if(((TileEntityRail) testTileStart).curve != null){
									if((360 + ((TileEntityRail) testTileStart).curve.startAngle)%360 ==  (360 + 180 + rail.curve.startAngle)%360){
										startConnector = (TileEntityRail) testTileStart;
									}
								}
							}
						}
						
						if(testTileEnd != null){
							if(testTileEnd instanceof TileEntityRail){
								if(((TileEntityRail) testTileEnd).curve != null){
									if((360 + ((TileEntityRail) testTileEnd).curve.startAngle)%360 ==  (360 + 180 + rail.curve.endAngle)%360){
										endConnector = (TileEntityRail) testTileEnd;
									}
								}
							}
						}
					}
				}
			}
						
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			for(int i=0; i<=Math.floor(rail.curve.pathLength); ++i){
				if(i==0 && startConnector != null){
					currentPoint = startConnector.curve.getPointAt(0);
					currentAngle = 90 + startConnector.curve.getYawAngleAt(0) - 180;
					currentPitch = startConnector.curve.getPitchAngleAt(0);
					startConnector = null;
					i = -1;
				}else{
					currentPoint = rail.curve.getPointAt(i/rail.curve.pathLength);
					currentAngle = 90 + rail.curve.getYawAngleAt(i/rail.curve.pathLength);
					currentPitch = rail.curve.getPitchAngleAt(i/rail.curve.pathLength);
				}
				
				if(i!=-1){
					GL11.glPushMatrix();
					GL11.glTranslatef(currentPoint[0] - rail.xCoord, currentPoint[1] - rail.yCoord, currentPoint[2] - rail.zCoord);
					GL11.glRotatef(currentAngle, 0, 1, 0);
					GL11.glRotatef(currentPitch, 0, 0, 1);		
					 
					GL11DrawSystem.bindTexture(new ResourceLocation("textures/blocks/log_big_oak.png"));
					model.renderTieInner();
					GL11DrawSystem.bindTexture(new ResourceLocation("textures/blocks/log_big_oak_top.png"));
					model.renderTieOuter();
					GL11.glPopMatrix();
					
				}
				
				if((i + 1) <= Math.floor(rail.curve.pathLength)){
					nextPoint = rail.curve.getPointAt((i + 1)/rail.curve.pathLength);
					nextAngle = 90 + rail.curve.getYawAngleAt((i + 1)/rail.curve.pathLength);
					nextPitch = rail.curve.getPitchAngleAt((i + 1)/rail.curve.pathLength);
				}else{
					if(endConnector != null){
						nextPoint = endConnector.curve.getPointAt(0);
						nextAngle = 90 + endConnector.curve.getYawAngleAt(0);
						nextPitch = endConnector.curve.getPitchAngleAt(0);
					}else{
						break;
					}
				}
				
				currentOffset[0] = currentPoint[0] - rail.xCoord;
				currentOffset[1] = currentPoint[1] - rail.yCoord + 0.25F;
				currentOffset[2] = currentPoint[2] - rail.zCoord;
				currentSin = (float) Math.sin(Math.toRadians(currentAngle));
				currentCos = (float) Math.cos(Math.toRadians(currentAngle));
				
				nextOffset[0] = nextPoint[0] - rail.xCoord;
				nextOffset[1] = nextPoint[1] - rail.yCoord + 0.25F;
				nextOffset[2] = nextPoint[2] - rail.zCoord;
				nextSin = (float) Math.sin(Math.toRadians(nextAngle));
				nextCos = (float) Math.cos(Math.toRadians(nextAngle));
				
				GL11.glPushMatrix();
				GL11DrawSystem.bindTexture(new ResourceLocation("textures/blocks/iron_block.png"));
				//Inner and outer sides.
				GL11DrawSystem.renderQuadUV(currentOffset[0] + 0.75*currentSin, nextOffset[0] + 0.75*nextSin, nextOffset[0] + 0.75*nextSin, currentOffset[0] + 0.75*currentSin, 
						currentOffset[1] + 0.1875, nextOffset[1] + 0.1875, nextOffset[1], currentOffset[1], 
						currentOffset[2] + 0.75*currentCos, nextOffset[2] + 0.75*nextCos, nextOffset[2] + 0.75*nextCos, currentOffset[2] + 0.75*currentCos, 
						0.0625, 0.9375, 0.0625, 0.9375, false);
				GL11DrawSystem.renderQuadUV(nextOffset[0] + 0.625*nextSin, currentOffset[0] + 0.625*currentSin, currentOffset[0] + 0.625*currentSin, nextOffset[0] + 0.625*nextSin, 
						nextOffset[1] + 0.1875, currentOffset[1] + 0.1875, currentOffset[1], nextOffset[1], 
						nextOffset[2] + 0.625*nextCos, currentOffset[2] + 0.625*currentCos, currentOffset[2] + 0.625*currentCos, nextOffset[2] + 0.625*nextCos,  
						0.0625, 0.9375, 0.0625, 0.9375, false);
				GL11DrawSystem.renderQuadUV(nextOffset[0] - 0.75*nextSin, currentOffset[0] - 0.75*currentSin, currentOffset[0] - 0.75*currentSin, nextOffset[0] - 0.75*nextSin, 
						nextOffset[1] + 0.1875, currentOffset[1] + 0.1875, currentOffset[1], nextOffset[1], 
						nextOffset[2] - 0.75*nextCos, currentOffset[2] - 0.75*currentCos, currentOffset[2] - 0.75*currentCos, nextOffset[2] - 0.75*nextCos,  
						0.0625, 0.9375, 0.0625, 0.9375, false);
				GL11DrawSystem.renderQuadUV(currentOffset[0] - 0.625*currentSin, nextOffset[0] - 0.625*nextSin, nextOffset[0] - 0.625*nextSin, currentOffset[0] - 0.625*currentSin, 
						currentOffset[1] + 0.1875, nextOffset[1] + 0.1875, nextOffset[1], currentOffset[1], 
						currentOffset[2] - 0.625*currentCos, nextOffset[2] - 0.625*nextCos, nextOffset[2] - 0.625*nextCos, currentOffset[2] - 0.625*currentCos, 
						0.0625, 0.9375, 0.0625, 0.9375, false);
				
				//Top and bottom.
				GL11DrawSystem.renderQuadUV(currentOffset[0] + 0.625*currentSin, nextOffset[0] + 0.625*nextSin, nextOffset[0] + 0.75*nextSin, currentOffset[0] + 0.75*currentSin, 
						currentOffset[1] + 0.1875, nextOffset[1] + 0.1875, nextOffset[1] + 0.1875, currentOffset[1] + 0.1875, 
						currentOffset[2] + 0.625*currentCos, nextOffset[2] + 0.625*nextCos, nextOffset[2] + 0.75*nextCos, currentOffset[2] + 0.75*currentCos, 
						0.0625, 0.9375, 0.0625, 0.9375, false);
				GL11DrawSystem.renderQuadUV(currentOffset[0] + 0.75*currentSin, nextOffset[0] + 0.75*nextSin, nextOffset[0] + 0.625*nextSin, currentOffset[0] + 0.625*currentSin, 
						currentOffset[1], nextOffset[1], nextOffset[1], currentOffset[1], 
						currentOffset[2] + 0.75*currentCos, nextOffset[2] + 0.75*nextCos, nextOffset[2] + 0.625*nextCos, currentOffset[2] + 0.625*currentCos, 
						0.0625, 0.9375, 0.0625, 0.9375, false);
				GL11DrawSystem.renderQuadUV(currentOffset[0] - 0.75*currentSin, nextOffset[0] - 0.75*nextSin, nextOffset[0] - 0.625*nextSin, currentOffset[0] - 0.625*currentSin, 
						currentOffset[1] + 0.1875, nextOffset[1] + 0.1875, nextOffset[1] + 0.1875, currentOffset[1] + 0.1875, 
						currentOffset[2] - 0.75*currentCos, nextOffset[2] - 0.75*nextCos, nextOffset[2] - 0.625*nextCos, currentOffset[2] - 0.625*currentCos, 
						0.0625, 0.9375, 0.0625, 0.9375, false);
				GL11DrawSystem.renderQuadUV(currentOffset[0] - 0.625*currentSin, nextOffset[0] - 0.625*nextSin, nextOffset[0] - 0.75*nextSin, currentOffset[0] - 0.75*currentSin, 
						currentOffset[1], nextOffset[1], nextOffset[1], currentOffset[1], 
						currentOffset[2] - 0.625*currentCos, nextOffset[2] - 0.625*nextCos, nextOffset[2] - 0.75*nextCos, currentOffset[2] - 0.75*currentCos, 
						0.0625, 0.9375, 0.0625, 0.9375, false);
				GL11.glPopMatrix();
			}
			GL11.glPopMatrix();
			if(rail.isPrimary){
				rail.renderedLastPass = true;
			}
		}
	}
}
