package minecraftflightsimulator.rendering.models.blocks;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelSurveyFlag extends ModelBase{
	private static final float scale=0.0625F;
	
    ModelRenderer pole;
    ModelRenderer flag;
 
    public ModelSurveyFlag(){
    	textureWidth = 16;
        textureHeight = 16;
        
        pole = new ModelRenderer(this, 0, 0);
        pole.addBox(-1F, 0F, -1F, 2, 16, 2);
        pole.setRotationPoint(0, 0, 0);
        pole.setTextureSize(16, 16);
        
        flag = new ModelRenderer(this, 0, 0);
        flag.addBox(-1F, 12F, 1F, 2, 4, 4);
        flag.setRotationPoint(0, 0, 0);
        flag.setTextureSize(16, 16);
    }    
    
    public void renderPole(){
    	pole.render(scale);
    }
    
    public void renderFlag(){
    	flag.render(scale);
    }
}