package minecrafttransportsimulator.systems;

import org.lwjgl.opengl.GL11;

/**This class is responsible for most of the legwork of the custom rendering system.
 * Contains multiple methods for drawing textured quads and the like.
 * 
 * @author don_bruce
 */
@Deprecated
public final class GL11DrawSystem{
    /**
     * Draws a square with specified bounds and custom UV mapping.  Use for vertical draws only.
     */
    public static void renderSquareUV(double x1, double x2, double y1, double y2, double z1, double z2, double u, double U, double v, double V, boolean mirror){
    	renderQuadUV(x1, x1, x2, x2, y2, y1, y1, y2, z1, z1, z2, z2, u, U, v, V, mirror);
    }
    
	/**
     * Draws a quad clockwise starting from top-left point with custom UV mapping.
     */
    public static void renderQuadUV(double x1, double x2, double x3, double x4, double y1, double y2, double y3, double y4, double z1, double z2, double z3, double z4, double u, double U, double v, double V, boolean mirror){
    	renderQuadUVCustom(x1, x2, x3, x4, y1, y2, y3, y4, z1, z2, z3, z4, u, u, U, U, v, V, V, v, mirror);
    }
    
	/**
     * Draws a quad clockwise starting from top-left point with custom UV mapping for each vertex.
     */
    public static void renderQuadUVCustom(double x1, double x2, double x3, double x4, double y1, double y2, double y3, double y4, double z1, double z2, double z3, double z4, double u1, double u2, double u3, double u4, double v1, double v2, double v3, double v4, boolean mirror){
    	GL11.glPushMatrix();
		GL11.glBegin(GL11.GL_QUADS);
		
		GL11.glTexCoord2d(u1, v1);
		GL11.glVertex3d(x1, y1, z1);
		GL11.glTexCoord2d(u2, v2);
		GL11.glVertex3d(x2, y2, z2);
		GL11.glTexCoord2d(u3, v3);
		GL11.glVertex3d(x3, y3, z3);
		GL11.glTexCoord2d(u4, v4);
		GL11.glVertex3d(x4, y4, z4);
    	
    	if(mirror){
    		GL11.glTexCoord2d(u1, v1);
    		GL11.glVertex3d(x4, y4, z4);
    		GL11.glTexCoord2d(u2, v2);
    		GL11.glVertex3d(x3, y3, z3);
    		GL11.glTexCoord2d(u3, v3);
    		GL11.glVertex3d(x2, y2, z2);
    		GL11.glTexCoord2d(u4, v4);
    		GL11.glVertex3d(x1, y1, z1);
    	}
    	GL11.glEnd();
    	GL11.glPopMatrix();
    }
}
