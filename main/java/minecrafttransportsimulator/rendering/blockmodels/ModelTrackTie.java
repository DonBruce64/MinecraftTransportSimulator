package minecrafttransportsimulator.rendering.blockmodels;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelTrackTie extends ModelBase{
	private static final float scale=0.0625F;
	
	ModelRenderer tie;
 
    public ModelTrackTie(){
    	textureWidth = 110;
    	textureHeight = 10;
    	tie = new ModelRenderer(this, 0, 0);
    	tie.addBox(-24F, -3F, 0F, 48, 3, 6);
    	tie.setRotationPoint(0F, 0F, 0F);
    	tie.setTextureSize(110, 10);
    }    
    
    public void render(){
        tie.render(scale);
    }    
}