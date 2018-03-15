package minecrafttransportsimulator.rendering.partmodels;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelPropeller extends ModelBase{
    private final float scale=0.0625F;
    private ModelRenderer shaft;
    private ModelRenderer shortBlades[][] = new ModelRenderer[7][2];
    private ModelRenderer longBlades[][] = new ModelRenderer[7][3];
 
    public ModelPropeller(){
    	textureWidth = 64;
        textureHeight = 12;
        
        shaft = new ModelRenderer(this, 0, 0);
        shaft.addBox(-2.5F, -1.5F, -1F, 5, 3, 5);
        shaft.addBox(-1.5F, -2.5F, -1F, 3, 1, 5);
        shaft.addBox(-1.5F, 1.5F, -1F, 3, 1, 5);
        shaft.addBox(-1.5F, -1.5F, 4F, 3, 3, 5);
        shaft.setTextureSize(textureWidth, textureHeight);
        
        for(byte i=6; i<13; ++i){            
        	shortBlades[i-6][0] = new ModelRenderer(this, 8, 1);
        	shortBlades[i-6][0].addBox(0F, -1F, 0F, i+4, 2, 1);
        	shortBlades[i-6][0].setTextureSize(textureWidth, textureHeight);
        	shortBlades[i-6][0].rotateAngleX = 0.5235988F;
            
        	shortBlades[i-6][1] = new ModelRenderer(this, 12, 4);
        	shortBlades[i-6][1].addBox(2F, 1F, 0F, i, 1, 1);
        	shortBlades[i-6][1].setTextureSize(textureWidth, textureHeight);
        	shortBlades[i-6][1].rotateAngleX = 0.5235988F;
        }
        
        for(byte i=13; i<20; ++i){
            longBlades[i-13][0] = new ModelRenderer(this, 8, 1);
            longBlades[i-13][0].addBox(0F, -1.5F, 0F, i+4, 3, 1);
            longBlades[i-13][0].setTextureSize(textureWidth, textureHeight);
            longBlades[i-13][0].rotateAngleX = 0.5235988F;
            
            longBlades[i-13][1] = new ModelRenderer(this, 12, 5);
            longBlades[i-13][1].addBox(2F, 1.5F, 0F, i, 1, 1);
            longBlades[i-13][1].setTextureSize(textureWidth, textureHeight);
            longBlades[i-13][1].rotateAngleX = 0.5235988F;
            
            longBlades[i-13][2] = new ModelRenderer(this, 12, 7);
            longBlades[i-13][2].addBox(i+4, -0.5F, 0F, 1, 1, 1);
            longBlades[i-13][2].setTextureSize(textureWidth, textureHeight);
            longBlades[i-13][2].rotateAngleX = 0.5235988F;
        }
    }    
    
    public void renderPropeller(int numberBlades, int diameter, float propellerRotation){
    	shaft.rotateAngleZ = propellerRotation;
    	shaft.render(scale);
    	byte bladeSize = (byte) Math.floor(diameter*0.0254*16/2);
    	if(bladeSize < 19){
    		for(byte i=0; i<numberBlades; ++i){
    			shortBlades[bladeSize - 12][0].rotateAngleZ = propellerRotation + i*6.283185312F/numberBlades;
    			shortBlades[bladeSize - 12][1].rotateAngleZ = propellerRotation + i*6.283185312F/numberBlades;
    			shortBlades[bladeSize - 12][0].render(scale);
    			shortBlades[bladeSize - 12][1].render(scale);
    		}
    	}else if(bladeSize <= 25){
    		for(byte i=0; i<numberBlades; ++i){
	    		longBlades[bladeSize - 19][0].rotateAngleZ = propellerRotation + i*6.283185312F/numberBlades;
	    		longBlades[bladeSize - 19][1].rotateAngleZ = propellerRotation + i*6.283185312F/numberBlades;
	    		longBlades[bladeSize - 19][2].rotateAngleZ = propellerRotation + i*6.283185312F/numberBlades;
	    		longBlades[bladeSize - 19][0].render(scale);
	    		longBlades[bladeSize - 19][1].render(scale);
	    		longBlades[bladeSize - 19][2].render(scale);
    		}
    	}
    }
}