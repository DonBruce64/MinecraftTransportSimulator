package minecraftflightsimulator.modelrenders;

import net.minecraft.client.renderer.Tessellator;

public class ModelRenderHelper{
	public static Tessellator tessellator = Tessellator.instance;
	
	public static void startRender(){
		tessellator.startDrawingQuads();
	}
	
	public static void endRender(){
		tessellator.draw();
	}
	
	
	/**
     * Draws a quad clockwise starting from top-left point.
     */
    public static void renderQuad(double x1, double x2, double x3, double x4, double y1, double y2, double y3, double y4, double z1, double z2, double z3, double z4, boolean mirror){
    	renderQuadUV(x1, x2, x3, x4, y1, y2, y3, y4, z1, z2, z3, z4, 0, 1, 0, 1, mirror);
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
		tessellator.addVertexWithUV(x1, y1, z1, u1, v1);
		tessellator.addVertexWithUV(x2, y2, z2, u2, v2);
    	tessellator.addVertexWithUV(x3, y3, z3, u3, v3);
		tessellator.addVertexWithUV(x4, y4, z4, u4, v4);
    	
    	if(mirror){
    		tessellator.addVertexWithUV(x4, y4, z4, u1, v1);
    		tessellator.addVertexWithUV(x3, y3, z3, u2, v2);
    		tessellator.addVertexWithUV(x2, y2, z2, u3, v3);
    		tessellator.addVertexWithUV(x1, y1, z1, u4, v4);
    	}
    }
    
    /**
     * Draws a triangle clockwise starting from top-left point
     */
    public static  void renderTriangle(double x1, double x2, double x3, double y1, double y2, double y3, double z1, double z2, double z3, boolean mirror){
    	renderTriangleUV(x1, x2, x3, y1, y2, y3, z1, z2, z3, 0, 1, 0, 1, mirror);
    }
    
    /**
     * Draws a triangle clockwise starting from top-left point with custom UV mapping.
     */
    public static  void renderTriangleUV(double x1, double x2, double x3, double y1, double y2, double y3, double z1, double z2, double z3, double u, double U, double v, double V, boolean mirror){
    	tessellator.addVertexWithUV(x3, y3, z3, U, v);
    	tessellator.addVertexWithUV(x1, y1, z1, u, v);
		tessellator.addVertexWithUV(x2, y2, z2, u, V);
    	tessellator.addVertexWithUV(x3, y3, z3, U, V);
    	
    	if(mirror){
    		tessellator.addVertexWithUV(x3, y3, z3, U, v);
    		tessellator.addVertexWithUV(x3, y3, z3, U, V);
    		tessellator.addVertexWithUV(x2, y2, z2, u, V);
    		tessellator.addVertexWithUV(x1, y1, z1, u, v);
    	}
    }
    
    /**
     * Draws a square with specified bounds.  Use for vertical draws only.
     */
    public static  void renderSquare(double x1, double x2, double y1, double y2, double z1, double z2, boolean mirror){
    	renderQuad(x1, x1, x2, x2, y2, y1, y1, y2, z1, z1, z2, z2, mirror);
    }
    
    /**
     * Draws a square with specified bounds and custom UV mapping.  Use for vertical draws only.
     */
    public static  void renderSquareUV(double x1, double x2, double y1, double y2, double z1, double z2, double u, double U, double v, double V, boolean mirror){
    	renderQuadUV(x1, x1, x2, x2, y2, y1, y1, y2, z1, z1, z2, z2, u, U, v, V, mirror);
    }
}
