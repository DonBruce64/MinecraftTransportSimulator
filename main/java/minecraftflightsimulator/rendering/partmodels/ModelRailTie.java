package minecraftflightsimulator.rendering.partmodels;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelRailTie extends ModelBase{
	private static final float scale=0.0625F;
	
    ModelRenderer tieTopBottom;
    ModelRenderer tieFrontBack;
    ModelRenderer tieLeftRight;
 
    public ModelRailTie(){
    	textureWidth = 6;
        textureHeight = 6;
        
        tieTopBottom = new ModelRenderer(this, 0, 0);
        tieTopBottom.addBox(-3, 3, -20, 6, 0, 40);
        tieTopBottom.addBox(-3, 0, -20, 6, 0, 40);
        tieTopBottom.setRotationPoint(0F, 0F, 0F);
        tieTopBottom.setTextureSize(textureWidth, textureHeight);
        
        tieFrontBack = new ModelRenderer(this, 0, 0);
        tieFrontBack.addBox(3, -20, -3, 0, 40, 3);
        tieFrontBack.addBox(-3, -20, -3, 0, 40, 3);
        tieFrontBack.setRotationPoint(0F, 0F, 0F);
        tieFrontBack.rotateAngleX = (float) (Math.PI/2F);
        tieFrontBack.setTextureSize(textureWidth, textureHeight);
        
        tieLeftRight = new ModelRenderer(this, 0, 0);
        tieLeftRight.addBox(-3, -3, -20, 6, 3, 0);
        tieLeftRight.addBox(-3, -3, 20, 6, 3, 0);
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