package minecraftflightsimulator.modelrenders;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderNull extends Render{
	
    public RenderNull(){
        super();
    }

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw,float pitch){		
	}
    
	@Override
	protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
		return null;
	}
}