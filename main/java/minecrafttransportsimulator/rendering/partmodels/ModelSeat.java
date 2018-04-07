package minecrafttransportsimulator.rendering.partmodels;

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
    	frame.addBox(-4, -10, -6, 8, 3, 12);
    	frame.addBox(-6, -10, -6, 1, 6, 12);
    	frame.addBox(5, -10, -6, 1, 6, 12);
    	frame.addBox(-6, -10, -8, 12, 18, 2);
    	frame.addBox(-6, -4, -6, 1, 12, 2);
    	frame.addBox(5, -4, -6, 1, 12, 2);
    	
    	cushion = new ModelRenderer(this, 0, 0);
    	cushion.setTextureSize(textureWidth, textureHeight);
    	cushion.addBox(-5, -7, -6, 10, 2, 12);
    	cushion.addBox(-5, -5, -6, 10, 13, 2);  
    }    
    
    public void renderFrame(){
    	frame.render(scale);
    }
    
    public void renderCushion(){  	
    	cushion.render(scale);
    }
}