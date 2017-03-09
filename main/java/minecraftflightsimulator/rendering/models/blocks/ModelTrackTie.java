package minecraftflightsimulator.rendering.models.blocks;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelTrackTie extends ModelBase{
	private static final float scale=0.0625F;
	
    ModelRenderer tie;
 
    public ModelTrackTie(){
    	textureWidth = 128;
        textureHeight = 16;
        
        tie = new ModelRenderer(this, 0, 0);
        tie.addBox(-20F, 0F, -3F, 40, 3, 6);
        tie.setRotationPoint(0, 0, 0);
        tie.setTextureSize(128, 16);
    }    
    
    public void render(){
    	tie.render(scale);
    }
}