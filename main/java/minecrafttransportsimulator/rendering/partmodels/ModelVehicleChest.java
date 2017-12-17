package minecrafttransportsimulator.rendering.partmodels;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelVehicleChest extends ModelBase{
    public ModelRenderer chestLid;
    public ModelRenderer chestBase;
    public ModelRenderer chestKnob;

    public ModelVehicleChest(){
    	chestLid = (new ModelRenderer(this, 0, 0)).setTextureSize(64, 64);
    	chestLid.setRotationPoint(-7F, -7F, 7F);
        chestLid.addBox(0.0F, -5.0F, -14.0F, 14, 5, 14);
        
        chestKnob = (new ModelRenderer(this, 0, 0)).setTextureSize(64, 64);
        chestKnob.setRotationPoint(0F, -7F, 7F);
        chestKnob.addBox(-1.0F, -2.0F, -15.0F, 2, 4, 1);
        
        chestBase = (new ModelRenderer(this, 0, 19)).setTextureSize(64, 64);
        chestBase.setRotationPoint(-7F, -8F, -7F);
        chestBase.addBox(0.0F, 0.0F, 0.0F, 14, 10, 14);
    }

    public void renderAll(float lidAngle){
    	chestLid.rotateAngleX = lidAngle;
    	chestKnob.rotateAngleX = lidAngle;
        chestLid.render(0.0625F);
        chestKnob.render(0.0625F);
        chestBase.render(0.0625F);
    }
}
