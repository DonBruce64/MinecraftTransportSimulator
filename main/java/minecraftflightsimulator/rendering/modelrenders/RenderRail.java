package minecraftflightsimulator.rendering.modelrenders;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.blocks.TileEntityRail;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import minecraftflightsimulator.rendering.partmodels.ModelRailTie;
import minecraftflightsimulator.systems.GL11DrawSystem;
import minecraftflightsimulator.systems.RenderSystem.RenderTileBase;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class RenderRail extends RenderTileBase{
	private static final ModelRailTie model = new ModelRailTie();

	@Override
	protected void doRender(TileEntity tile, double x, double y, double z){
		TileEntityRail rail = (TileEntityRail) tile;
		//System.out.println(BlockHelper.getBlockLight(rail.getWorldObj(), rail.xCoord, rail.yCoord, rail.zCoord));
		
		//Ensure rail hasn't already been rendered.
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
		
		//Render rail.
		if(rail.curve != null){
			float[] currentPoint;
			float currentAngle;
			
			//Get connected rails.
			TileEntityRail startConnector = null;
			TileEntityRail endConnector = null;
			for(byte i=-1; i<=1; ++i){
				for(byte j=-1; j<=1; ++j){
					if(!(i == 0 && j == 0)){
						TileEntity testTile = BlockHelper.getTileEntityFromCoords(rail.getWorldObj(), (int) (rail.curve.startPoint[0] - 0.5F + i), (int) rail.curve.startPoint[1], (int) (rail.curve.startPoint[2] - 0.5F + j));
						if(testTile != null){
							if(testTile instanceof TileEntityRail){
								if(((TileEntityRail) testTile).curve != null){
									if((360 + ((TileEntityRail) testTile).curve.startAngle)%360 ==  (360 + 180 + rail.curve.startAngle)%360){
										startConnector = (TileEntityRail) testTile;
									}
								}
							}
						}
						testTile = BlockHelper.getTileEntityFromCoords(rail.getWorldObj(), (int) (rail.curve.endPoint[0] - 0.5F + i), (int) rail.curve.endPoint[1], (int) (rail.curve.endPoint[2] - 0.5F + j));
						if(testTile != null){
							if(testTile instanceof TileEntityRail){
								if(((TileEntityRail) testTile).curve != null){
									if((360 + ((TileEntityRail) testTile).curve.startAngle)%360 ==  (360 + 180 + rail.curve.endAngle)%360){
										endConnector = (TileEntityRail) testTile;
									}
								}
							}
						}
					}
				}
			}
						
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			if(!rail.isHologram()){
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glColor4f(0, 1, 0, 0.25F);
			}
				
			//First render rails.
			//Before we do that, we need to get all the points on top of the ties where the rails go.
			List<float[]> texPoints = new ArrayList<float[]>();
			final float offset = 0.65F;
			for(float f=0; f <= rail.curve.pathLength; f += offset){
				//Add extra point if start connector is present.
				if(texPoints.isEmpty() && startConnector != null){
					currentPoint = startConnector.curve.getPointAt(0);
					currentAngle = 90 + startConnector.curve.getYawAngleAt(0) - 180;
					f -= offset;
				}else{
					currentPoint = rail.curve.getPointAt(f/rail.curve.pathLength);
					currentAngle = 90 + rail.curve.getYawAngleAt(f/rail.curve.pathLength);
				}
				texPoints.add(new float[]{
					currentPoint[0] - rail.xCoord,
					currentPoint[1] - rail.yCoord + 0.1875F,
					currentPoint[2] - rail.zCoord,
					(float) Math.sin(Math.toRadians(currentAngle)),
					(float) Math.cos(Math.toRadians(currentAngle)),
					(float) ((startConnector != null ? f >= 0 : f != 0) ? texPoints.get(texPoints.size() - 1)[5] + Math.hypot(currentPoint[0] - rail.xCoord - texPoints.get(texPoints.size() - 1)[0], currentPoint[2] - rail.zCoord - texPoints.get(texPoints.size() - 1)[2]) : 0),
					BlockHelper.getRenderLight(rail.getWorldObj(), (int) Math.ceil(currentPoint[0]), (int) Math.ceil(currentPoint[1]), (int) Math.ceil(currentPoint[2]))
				});
			}
			
			//Add extra point if end connector is present.
			if(endConnector != null){
				currentPoint = endConnector.curve.getPointAt(0);
				currentAngle = 90 + endConnector.curve.getYawAngleAt(0);
				texPoints.add(new float[]{
					currentPoint[0] - rail.xCoord,
					currentPoint[1] - rail.yCoord + 0.1875F,
					currentPoint[2] - rail.zCoord,
					(float) Math.sin(Math.toRadians(currentAngle)),
					(float) Math.cos(Math.toRadians(currentAngle)),
					(float) (texPoints.get(texPoints.size() - 1)[5] + Math.hypot(currentPoint[0] - rail.xCoord - texPoints.get(texPoints.size() - 1)[0], currentPoint[2] - rail.zCoord - texPoints.get(texPoints.size() - 1)[2])),
					BlockHelper.getRenderLight(rail.getWorldObj(), (int) Math.ceil(currentPoint[0]), (int) Math.ceil(currentPoint[1]), (int) Math.ceil(currentPoint[2]))
				});
			}
			
			//Now that we have all the points, it's time to render.
			//If we have a start connector, render it now.
			if(startConnector != null){
				GL11.glPushMatrix();
				GL11.glTranslatef(texPoints.get(0)[0], texPoints.get(0)[1] - 0.1875F, texPoints.get(0)[2]);
				GL11.glRotatef(90 + startConnector.curve.getYawAngleAt(0) - 180F, 0, 1, 0);
				GL11.glRotatef(startConnector.curve.getPitchAngleAt(0), 0, 0, 1);
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, texPoints.get(0)[6]%65536, texPoints.get(0)[6]/65536);
				GL11DrawSystem.bindTexture(new ResourceLocation("textures/blocks/log_big_oak.png"));
				model.renderTieInner();
				GL11DrawSystem.bindTexture(new ResourceLocation("textures/blocks/log_big_oak_top.png"));
				model.renderTieOuter();
				GL11.glPopMatrix();
			}
			//If we have an end connector, render it now.
			if(endConnector != null){
				GL11.glPushMatrix();
				GL11.glTranslatef(texPoints.get(texPoints.size() - 1)[0], texPoints.get(texPoints.size() - 1)[1] - 0.1875F, texPoints.get(texPoints.size() - 1)[2]);
				GL11.glRotatef(90 + endConnector.curve.getYawAngleAt(0), 0, 1, 0);
				GL11.glRotatef(endConnector.curve.getPitchAngleAt(0), 0, 0, 1);
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, texPoints.get(texPoints.size() - 1)[6]%65536, texPoints.get(texPoints.size() - 1)[6]/65536);
				GL11DrawSystem.bindTexture(new ResourceLocation("textures/blocks/log_big_oak.png"));
				model.renderTieInner();
				GL11DrawSystem.bindTexture(new ResourceLocation("textures/blocks/log_big_oak_top.png"));
				model.renderTieOuter();
				GL11.glPopMatrix();
			}
			//Now render the rest of the ties.
			//Make sure NOT to render the start or end connectors!
			for(short i = (short) (startConnector != null ? 1 : 0); i < (short) (endConnector != null ? texPoints.size() - 1 : texPoints.size()); ++i){
				GL11.glPushMatrix();
				GL11.glTranslatef(texPoints.get(i)[0], texPoints.get(i)[1] - 0.1875F, texPoints.get(i)[2]);
				if(startConnector != null){
					GL11.glRotatef(90 + rail.curve.getYawAngleAt((i - 1)*offset/rail.curve.pathLength), 0, 1, 0);
					GL11.glRotatef(rail.curve.getPitchAngleAt((i - 1)*offset/rail.curve.pathLength), 0, 0, 1);
				}else{
					GL11.glRotatef(90 + rail.curve.getYawAngleAt(i*offset/rail.curve.pathLength), 0, 1, 0);
					GL11.glRotatef(rail.curve.getPitchAngleAt(i*offset/rail.curve.pathLength), 0, 0, 1);
				}
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, texPoints.get(i)[6]%65536, texPoints.get(i)[6]/65536);
				GL11DrawSystem.bindTexture(new ResourceLocation("textures/blocks/log_big_oak.png"));
				model.renderTieInner();
				GL11DrawSystem.bindTexture(new ResourceLocation("textures/blocks/log_big_oak_top.png"));
				model.renderTieOuter();
				GL11.glPopMatrix();
			}
			
			//Now to render the rails.
			//These are quad strips, which makes contiguous rails easy!
			GL11.glPushMatrix();
			GL11DrawSystem.bindTexture(new ResourceLocation("textures/blocks/iron_block.png"));
			drawRailSegment(texPoints, 12F/16F, 10F/16F, 4F/16F, 4F/16F);//Top
			drawRailSegment(texPoints, 12F/16F, 12F/16F, 3F/16F, 4F/16F);//Outer-top-side
			drawRailSegment(texPoints, 11.5F/16F, 12F/16F, 3F/16F, 3F/16F);//Outer-top-under
			drawRailSegment(texPoints, 11.5F/16F, 11.5F/16F, 1F/16F, 3F/16F);//Outer-middle
			drawRailSegment(texPoints, 13.5F/16F, 11.5F/16F, 1F/16F, 1F/16F);//Outer-bottom-top
			drawRailSegment(texPoints, 13.5F/16F, 13.5F/16F, 0F/16F, 1F/16F);//Outer-bottom-side
			drawRailSegment(texPoints, 8.5F/16F, 13.5F/16F, 0.0F, 0.0F);//Bottom
			drawRailSegment(texPoints, 8.5F/16F, 8.5F/16F, 1F/16F, 0F/16F);//Inner-bottom-side
			drawRailSegment(texPoints, 10.5F/16F, 8.5F/16F, 1F/16F, 1F/16F);//Inner-bottom-top
			drawRailSegment(texPoints, 10.5F/16F, 10.5F/16F, 3F/16F, 1F/16F);//Inner-middle
			drawRailSegment(texPoints, 10F/16F, 10.5F/16F, 3F/16F, 3F/16F);//Inner-top-under
			drawRailSegment(texPoints, 10F/16F, 10F/16F, 4F/16F, 3F/16F);//Inner-top-side
			
			//drawRailSegment(texPoints, -0.75F, -0.75F, 0.1875F, 0.0F);
			//drawRailSegment(texPoints, -0.625F, -0.625F, 0.0F, 0.1875F);
			//drawRailSegment(texPoints, -0.625F, -0.75F, 0.1875F, 0.1875F);
			//drawRailSegment(texPoints, -0.75F, -0.625F, 0.0F, 0.0F);
			GL11.glPopMatrix();

			GL11.glPopMatrix();
			if(rail.isPrimary){
				rail.renderedLastPass = true;
			}
		}
	}
	
	private static void drawRailSegment(List<float[]> texPoints, float w1, float w2, float h1, float h2){
		GL11.glBegin(GL11.GL_QUAD_STRIP);
		for(float[] point : texPoints){
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, point[6]%65536, point[6]/65536);
			GL11.glTexCoord2d(0, point[5]);
			GL11.glVertex3d(point[0] + w1*point[3], point[1] + h1, point[2] + w1*point[4]);
			GL11.glTexCoord2d(1, point[5]);
			GL11.glVertex3d(point[0] + w2*point[3], point[1] + h2, point[2] + w2*point[4]);
		}
		GL11.glEnd();
		GL11.glBegin(GL11.GL_QUAD_STRIP);
		for(float[] point : texPoints){
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, point[6]%65536, point[6]/65536);
			GL11.glTexCoord2d(0, point[5]);
			GL11.glVertex3d(point[0] - w2*point[3], point[1] + h2, point[2] - w2*point[4]);
			GL11.glTexCoord2d(1, point[5]);
			GL11.glVertex3d(point[0] - w1*point[3], point[1] + h1, point[2] - w1*point[4]);
		}
		GL11.glEnd();
	}
}
