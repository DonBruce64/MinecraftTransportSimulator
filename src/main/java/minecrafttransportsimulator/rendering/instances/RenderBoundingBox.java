package minecrafttransportsimulator.rendering.instances;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;

/**Main render class for bounding boxes.  Split from the box class to prevent OpenGL from being called on servers.
 * Contains both holographic and outline renders.
 *
 * @author don_bruce
 */
public final class RenderBoundingBox{
	
	
	/**
     * Renders the passed-in box as solid.
     */
	public static void renderSolid(BoundingBox box){
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex3d(+box.widthRadius, +box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, +box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, -box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, -box.heightRadius, +box.depthRadius);
		
		GL11.glVertex3d(+box.widthRadius, +box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, +box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, -box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, -box.heightRadius, -box.depthRadius);
		
		GL11.glVertex3d(+box.widthRadius, -box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, -box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, +box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, +box.heightRadius, -box.depthRadius);
		
		GL11.glVertex3d(-box.widthRadius, -box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, -box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, +box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, +box.heightRadius, -box.depthRadius);
		
		GL11.glVertex3d(+box.widthRadius, +box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, +box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, +box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, +box.heightRadius, +box.depthRadius);
		
		GL11.glVertex3d(-box.widthRadius, -box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, -box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, -box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, -box.heightRadius, +box.depthRadius);
		GL11.glEnd();
	}
	
	/**
     * Renders the passed-in box as a wireframe model.
     */
	public static void renderWireframe(BoundingBox box){
		GL11.glBegin(GL11.GL_LINES);
		//Bottom
		GL11.glVertex3d(-box.widthRadius, -box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, -box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, -box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, -box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, -box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, -box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, -box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, -box.heightRadius, +box.depthRadius);
		
		//Top
		GL11.glVertex3d(-box.widthRadius, +box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, +box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, +box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, +box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, +box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, +box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, +box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, +box.heightRadius, +box.depthRadius);
		
		//Vertical sides.
		GL11.glVertex3d(-box.widthRadius, -box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, +box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, -box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, +box.heightRadius, -box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, -box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(-box.widthRadius, +box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, -box.heightRadius, +box.depthRadius);
		GL11.glVertex3d(+box.widthRadius, +box.heightRadius, +box.depthRadius);
		GL11.glEnd();
	}
}

