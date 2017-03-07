package minecraftflightsimulator.rendering.partmodels;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelRail extends ModelBase{
	private static final float scale=0.0625F;
	
    ModelRenderer tieTopBottom;
    ModelRenderer tieFrontBack;
    ModelRenderer tieLeftRight;
 
    public ModelRail(){
    	textureWidth = 8;
        textureHeight = 8;
        
        tieTopBottom = new ModelRenderer(this, 0, 0);
        tieTopBottom.addBox(-4, 4, -20, 8, 0, 40);
        tieTopBottom.addBox(-4, 0, -20, 8, 0, 40);
        tieTopBottom.setRotationPoint(0F, 0F, 0F);
        tieTopBottom.setTextureSize(textureWidth, textureHeight);
        
        tieFrontBack = new ModelRenderer(this, 0, 0);
        tieFrontBack.addBox(4, -20, -4, 0, 40, 4);
        tieFrontBack.addBox(-4, -20, -4, 0, 40, 4);
        tieFrontBack.setRotationPoint(0F, 0F, 0F);
        tieFrontBack.rotateAngleX = (float) (Math.PI/2F);
        tieFrontBack.setTextureSize(textureWidth, textureHeight);
        
        tieLeftRight = new ModelRenderer(this, 0, 0);
        tieLeftRight.addBox(-4, -4, -20, 8, 4, 0);
        tieLeftRight.addBox(-4, -4, 20, 8, 4, 0);
        tieLeftRight.setRotationPoint(0F, 0F, 0F);
        tieLeftRight.rotateAngleZ = (float) Math.PI;
        tieLeftRight.setTextureSize(textureWidth, textureHeight);
    }    
    
    public void renderTieInner(){
    	tieTopBottom.render(scale);
	    tieFrontBack.render(scale);
    }
    
    public void renderTieOuter(){
    	tieLeftRight.render(scale);
    }
}