package minecraftflightsimulator.rendering.blockmodels;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelSurveyFlag extends ModelBase{
	private static final float scale=0.0625F;
	
	ModelRenderer Shape1;
    ModelRenderer Shape2;
    ModelRenderer Shape3;
 
    public ModelSurveyFlag(){
    	textureWidth = 32;
        textureHeight = 32;
        
		Shape1 = new ModelRenderer(this, 1, 1);
		Shape1.addBox(-1F, 0F, -1F, 2, 13, 2);
		Shape1.setRotationPoint(0F, 0F, 0F);
		Shape1.setTextureSize(32, 32);
		Shape2 = new ModelRenderer(this, 10, 1);
		Shape2.addBox(-1.5F, 13F, -1.5F, 3, 3, 3);
		Shape2.setRotationPoint(0F, 0F, 0F);
		Shape2.setTextureSize(32, 32);
		Shape3 = new ModelRenderer(this, 10, 8);
		Shape3.addBox(-0.5F, 9F, -8F, 1, 4, 7);
		Shape3.setRotationPoint(0F, 0F, 0F);
		Shape3.setTextureSize(32, 32);
    }    
    
    public void render(){
    	Shape1.render(scale);
    	Shape2.render(scale);
    	Shape3.render(scale);
    }
}