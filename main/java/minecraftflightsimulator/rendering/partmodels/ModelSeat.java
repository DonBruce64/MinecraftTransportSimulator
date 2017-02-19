package minecraftflightsimulator.rendering.partmodels;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelSeat extends ModelBase{
    private final float scale=0.0625F;
    private ModelRenderer frame;
    private ModelRenderer cushion;
 
    public ModelSeat(){
    	this.textureWidth = 16;
    	this.textureHeight = 16;
    	frame = new ModelRenderer(this, 0, 0);
    	frame.setTextureSize(textureWidth, textureHeight);
    	frame.setRotationPoint(0F, 8F, 0F);
    	frame.addBox(-6, -10, -6, 12, 3, 12);
    	frame.addBox(-8, -10, -6, 2, 6, 12);
    	frame.addBox(6, -10, -6, 2, 6, 12);
    	frame.addBox(-8, -10, -8, 16, 16, 2);
    	frame.addBox(-8, -4, -6, 2, 10, 2);
    	frame.addBox(6, -4, -6, 2, 10, 2);
    	
    	cushion = new ModelRenderer(this, 0, 0);
    	cushion.setTextureSize(textureWidth, textureHeight);
    	cushion.setRotationPoint(0F, 8F, 0F);
    	cushion.addBox(-6, -7, -6, 12, 2, 12);
    	cushion.addBox(-6, -5, -6, 12, 11, 2);  
    }    
    
    public void renderFrame(){
    	frame.render(scale);
    }
    
    public void renderCushion(){  	
    	cushion.render(scale);
    }
}