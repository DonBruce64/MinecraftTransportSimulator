package minecrafttransportsimulator.rendering.blockrenders;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSCurve;
import minecrafttransportsimulator.blocks.TileEntityTrack;
import minecrafttransportsimulator.minecrafthelpers.BlockHelper;
import minecrafttransportsimulator.rendering.blockmodels.ModelTrackTie;
import minecrafttransportsimulator.systems.GL11DrawSystem;
import minecrafttransportsimulator.systems.RenderSystem.RenderTileBase;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class RenderTrack extends RenderTileBase{
	private static final ModelTrackTie model = new ModelTrackTie();
	private static final ResourceLocation tieTexture = new ResourceLocation(MTS.MODID, "textures/blockmodels/tie.png");
	private static final ResourceLocation railTexture = new ResourceLocation(MTS.MODID, "textures/blockmodels/rail.png");
	private static final ResourceLocation ballastTexture = new ResourceLocation(MTS.MODID, "textures/blocks/ballast.png");
	
	@Override
	protected void doRender(TileEntity tile, double x, double y, double z){
		TileEntityTrack track = (TileEntityTrack) tile;
		
		//Ensure rail hasn't already been rendered.
		if(!track.isPrimary){
			if(track.curve != null){
				TileEntityTrack otherEnd = (TileEntityTrack) BlockHelper.getTileEntityFromCoords(track.getWorldObj(), track.curve.blockEndPoint[0], track.curve.blockEndPoint[1], track.curve.blockEndPoint[2]);
				if(otherEnd != null){
					if(otherEnd.renderedLastPass){
						return;
					}
				}
			}
		}
		
		if(track.curve != null){
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			
			GL11.glPushMatrix();
			renderTrackSegmentFromCurve(track.getWorldObj(), track.curve, false);
			GL11.glPopMatrix();
			
			if(track.isPrimary){
				track.renderedLastPass = true;
			}
			
			/*
			//CAUSES OVER 20FPS LOSS.  DO NOT USE EXCEPT FOR TESTING!
			GL11DrawSystem.bindTexture(ballastTexture);
			//Render master tracks with ballast.
			GL11.glPushMatrix();
			float lightValue = BlockHelper.getRenderLight(track.getWorldObj(), (int) Math.ceil(track.curve.blockStartPoint[0]), (int) Math.ceil(track.curve.blockStartPoint[1]), (int) Math.ceil(track.curve.blockStartPoint[2]));
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightValue%65536, lightValue/65536);
			drawBallastBox(1F/16F);
			GL11.glTranslatef(track.curve.blockEndPoint[0] - track.curve.blockStartPoint[0], track.curve.blockEndPoint[1] - track.curve.blockStartPoint[1], track.curve.blockEndPoint[2] - track.curve.blockStartPoint[2]);
			lightValue = BlockHelper.getRenderLight(track.getWorldObj(), (int) Math.ceil(track.curve.blockEndPoint[0]), (int) Math.ceil(track.curve.blockEndPoint[1]), (int) Math.ceil(track.curve.blockEndPoint[2]));
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightValue%65536, lightValue/65536);
			drawBallastBox(1F/16F);
			GL11.glPopMatrix();
			
			//Render fake tracks with ballast.
			for(int[] fakeData : track.getFakeTracks()){
				GL11.glPushMatrix();
				GL11.glTranslatef(fakeData[0] - track.curve.blockStartPoint[0], fakeData[1] - track.curve.blockStartPoint[1], fakeData[2] - track.curve.blockStartPoint[2]);
				lightValue = BlockHelper.getRenderLight(track.getWorldObj(), (int) Math.ceil(fakeData[0]), (int) Math.ceil(fakeData[1]), (int) Math.ceil(fakeData[2]));
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightValue%65536, lightValue/65536);
				drawBallastBox(fakeData[3]/16F);
				GL11.glPopMatrix();
			}*/
			GL11.glPopMatrix();
		}
	}
	
	/**
	 * This can be called to render track anywhere in the code, not just from this class.
	 */
	public static void renderTrackSegmentFromCurve(World world, MTSCurve curve, boolean holographic){
		float[] currentPoint;
		float currentAngle;
		
		//Get connected rails.
		TileEntityTrack startConnector = null;
		TileEntityTrack endConnector = null;
		for(byte i=-1; i<=1; ++i){
			for(byte j=-1; j<=1; ++j){
				if(!(i == 0 && j == 0)){
					TileEntity testTile = BlockHelper.getTileEntityFromCoords(world, curve.blockStartPoint[0] + i, curve.blockStartPoint[1], curve.blockStartPoint[2] + j);
					if(testTile != null){
						if(testTile instanceof TileEntityTrack){
							if(((TileEntityTrack) testTile).curve != null){
								if((360 + ((TileEntityTrack) testTile).curve.startAngle)%360 == (360 + 180 + curve.startAngle)%360){
									if(!(curve.blockStartPoint[0] + i == curve.blockEndPoint[0] && curve.blockStartPoint[1] == curve.blockEndPoint[1] && curve.blockStartPoint[2] + j == curve.blockEndPoint[2])){
										startConnector = (TileEntityTrack) testTile;
									}
								}
							}
						}
					}
					testTile = BlockHelper.getTileEntityFromCoords(world, curve.blockEndPoint[0] + i, curve.blockEndPoint[1], curve.blockEndPoint[2] + j);
					if(testTile != null){
						if(testTile instanceof TileEntityTrack){
							if(((TileEntityTrack) testTile).curve != null){
								if((360 + ((TileEntityTrack) testTile).curve.startAngle)%360 == (360 + 180 + curve.endAngle)%360){
									if(!(curve.blockEndPoint[0] + i == curve.blockStartPoint[0] && curve.blockEndPoint[1] == curve.blockStartPoint[1] && curve.blockEndPoint[2] + j == curve.blockStartPoint[2])){
										endConnector = (TileEntityTrack) testTile;
									}
								}
							}
						}
					}
				}
			}
		}

		//Before we can render, we need to get all the points on top of the ties where the rails go.
		List<float[]> texPoints = new ArrayList<float[]>();
		final float offset = 0.65F;
		for(float f=0; f <= curve.pathLength; f += offset){
			//Add extra point if start connector is present.
			if(texPoints.isEmpty() && startConnector != null){
				currentPoint = startConnector.curve.getPointAt(0);
				currentAngle = 90 + startConnector.curve.getYawAngleAt(0) - 180;
				f -= offset;
			}else{
				currentPoint = curve.getPointAt(f/curve.pathLength);
				currentAngle = 90 + curve.getYawAngleAt(f/curve.pathLength);
			}
			texPoints.add(new float[]{
				currentPoint[0] - curve.blockStartPoint[0],
				currentPoint[1] - curve.blockStartPoint[1] + 0.1875F,
				currentPoint[2] - curve.blockStartPoint[2],
				(float) Math.sin(Math.toRadians(currentAngle)),
				(float) Math.cos(Math.toRadians(currentAngle)),
				(float) ((startConnector != null ? f >= 0 : f != 0) ? texPoints.get(texPoints.size() - 1)[5] + Math.hypot(currentPoint[0] - curve.blockStartPoint[0] - texPoints.get(texPoints.size() - 1)[0], currentPoint[2] - curve.blockStartPoint[2] - texPoints.get(texPoints.size() - 1)[2]) : 0),
				BlockHelper.getRenderLight(world, (int) Math.ceil(currentPoint[0]), (int) Math.ceil(currentPoint[1]), (int) Math.ceil(currentPoint[2]))
			});
		}
		
		//Add extra point if end connector is present.
		if(endConnector != null){
			currentPoint = endConnector.curve.getPointAt(0);
			currentAngle = 90 + endConnector.curve.getYawAngleAt(0);
			texPoints.add(new float[]{
				currentPoint[0] - curve.blockStartPoint[0],
				currentPoint[1] - curve.blockStartPoint[1] + 0.1875F,
				currentPoint[2] - curve.blockStartPoint[2],
				(float) Math.sin(Math.toRadians(currentAngle)),
				(float) Math.cos(Math.toRadians(currentAngle)),
				(float) (texPoints.get(texPoints.size() - 1)[5] + Math.hypot(currentPoint[0] - curve.blockStartPoint[0] - texPoints.get(texPoints.size() - 1)[0], currentPoint[2] - curve.blockStartPoint[2] - texPoints.get(texPoints.size() - 1)[2])),
				BlockHelper.getRenderLight(world, (int) Math.ceil(currentPoint[0]), (int) Math.ceil(currentPoint[1]), (int) Math.ceil(currentPoint[2]))
			});
		}
		
		//Now that we have all the points, it's time to render.
		if(holographic){
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glColor4f(0, 1, 0, 0.25F);
		}
		//If we have a start connector, render it now.
		if(startConnector != null){
			GL11.glPushMatrix();
			GL11.glTranslatef(texPoints.get(0)[0], texPoints.get(0)[1] - 0.1875F, texPoints.get(0)[2]);
			GL11.glRotatef(startConnector.curve.getYawAngleAt(0) - 180F, 0, 1, 0);
			GL11.glRotatef(startConnector.curve.getPitchAngleAt(0), 1, 0, 0);
			renderTie(texPoints.get(0)[6], holographic);
			GL11.glPopMatrix();
		}
		//If we have an end connector, render it now.
		if(endConnector != null){
			GL11.glPushMatrix();
			GL11.glTranslatef(texPoints.get(texPoints.size() - 1)[0], texPoints.get(texPoints.size() - 1)[1] - 0.1875F, texPoints.get(texPoints.size() - 1)[2]);
			GL11.glRotatef(endConnector.curve.getYawAngleAt(0), 0, 1, 0);
			GL11.glRotatef(endConnector.curve.getPitchAngleAt(0), 1, 0, 0);
			renderTie(texPoints.get(texPoints.size() - 1)[6], holographic);
			GL11.glPopMatrix();
		}
		//Now render the rest of the ties.
		//Make sure NOT to render the start or end connectors!
		for(short i = (short) (startConnector != null ? 1 : 0); i < (short) (endConnector != null ? texPoints.size() - 1 : texPoints.size()); ++i){
			GL11.glPushMatrix();
			GL11.glTranslatef(texPoints.get(i)[0], texPoints.get(i)[1] - 0.1875F, texPoints.get(i)[2]);
			if(startConnector != null){
				GL11.glRotatef(curve.getYawAngleAt((i - 1)*offset/curve.pathLength), 0, 1, 0);
				GL11.glRotatef(curve.getPitchAngleAt((i - 1)*offset/curve.pathLength), 1, 0, 0);
			}else{
				GL11.glRotatef(curve.getYawAngleAt(i*offset/curve.pathLength), 0, 1, 0);
				GL11.glRotatef(curve.getPitchAngleAt(i*offset/curve.pathLength), 1, 0, 0);
			}
			renderTie(texPoints.get(i)[6], holographic);
			GL11.glPopMatrix();
		}
		
		//Now to render the rails.
		//These are quad strips, which makes contiguous rails easy!
		GL11.glPushMatrix();
		GL11DrawSystem.bindTexture(railTexture);
		drawRailSegment(texPoints, 16F/16F, 14F/16F, 4F/16F, 4F/16F, 10F/16F, 12F/16F, holographic);//Top
		drawRailSegment(texPoints, 16F/16F, 16F/16F, 3F/16F, 4F/16F, 9F/16F, 10F/16F, holographic);//Outer-top-side
		drawRailSegment(texPoints, 15.5F/16F, 16F/16F, 3F/16F, 3F/16F, 8F/16F, 8.5F/16F, holographic);//Outer-top-under
		drawRailSegment(texPoints, 15.5F/16F, 15.5F/16F, 1F/16F, 3F/16F, 6F/16F, 8F/16F, holographic);//Outer-middle
		drawRailSegment(texPoints, 17.5F/16F, 15.5F/16F, 1F/16F, 1F/16F, 4F/16F, 5.5F/16F, holographic);//Outer-bottom-top
		drawRailSegment(texPoints, 17.5F/16F, 17.5F/16F, 0F/16F, 1F/16F, 3F/16F, 4F/16F, holographic);//Outer-bottom-side
		drawRailSegment(texPoints, 12.5F/16F, 17.5F/16F, 0.0F, 0.0F, 0.0F, 3F/16F, holographic);//Bottom
		drawRailSegment(texPoints, 12.5F/16F, 12.5F/16F, 1F/16F, 0F/16F, 3F/16F, 4F/16F, holographic);//Inner-bottom-side
		drawRailSegment(texPoints, 14.5F/16F, 12.5F/16F, 1F/16F, 1F/16F, 4F/16F, 5.5F/16F, holographic);//Inner-bottom-top
		drawRailSegment(texPoints, 14.5F/16F, 14.5F/16F, 3F/16F, 1F/16F, 6F/16F, 8F/16F, holographic);//Inner-middle
		drawRailSegment(texPoints, 14F/16F, 14.5F/16F, 3F/16F, 3F/16F, 8F/16F, 8.5F/16F, holographic);//Inner-top-under
		drawRailSegment(texPoints, 14F/16F, 14F/16F, 4F/16F, 3F/16F, 9F/16F, 10F/16F, holographic);//Inner-top-side
		GL11.glPopMatrix();
	}
	
	private static void renderTie(float brightness, boolean holographic){
		if(!holographic){
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness%65536, brightness/65536);
		}
		GL11DrawSystem.bindTexture(tieTexture);
		model.render();
	}
	
	private static void drawRailSegment(List<float[]> texPoints, float w1, float w2, float h1, float h2, float t1, float t2, boolean holographic){
		GL11.glBegin(GL11.GL_QUAD_STRIP);
		for(float[] point : texPoints){
			if(!holographic)OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, point[6]%65536, point[6]/65536);
			GL11.glTexCoord2d(point[5], t2);
			GL11.glVertex3d(point[0] + w1*point[3], point[1] + h1, point[2] + w1*point[4]);
			GL11.glTexCoord2d(point[5], t1);
			GL11.glVertex3d(point[0] + w2*point[3], point[1] + h2, point[2] + w2*point[4]);
		}
		GL11.glEnd();
		GL11.glBegin(GL11.GL_QUAD_STRIP);
		for(float[] point : texPoints){
			if(!holographic)OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, point[6]%65536, point[6]/65536);
            GL11.glTexCoord2d(point[5], t1);
			GL11.glVertex3d(point[0] - w2*point[3], point[1] + h2, point[2] - w2*point[4]);
			GL11.glTexCoord2d(point[5], t2);
			GL11.glVertex3d(point[0] - w1*point[3], point[1] + h1, point[2] - w1*point[4]);
		}
		GL11.glEnd();
	}
	
	private static void drawBallastBox(float height){
		GL11DrawSystem.renderSquare(0, 0, -0.01, height, 0, 1, false);
		GL11DrawSystem.renderSquare(1, 0, -0.01, height, 0, 0, false);
		GL11DrawSystem.renderSquare(0, 1, -0.01, height, 1, 1, false);
		GL11DrawSystem.renderSquare(1, 1, -0.01, height, 1, 0, false);
		GL11DrawSystem.renderQuad(0, 0, 1, 1, height, height, height, height, 0, 1, 1, 0, false);
		GL11DrawSystem.renderQuad(1, 1, 0, 0, -0.01, -0.01, -0.01, -0.01, 0, 1, 1, 0, false);
	}
}