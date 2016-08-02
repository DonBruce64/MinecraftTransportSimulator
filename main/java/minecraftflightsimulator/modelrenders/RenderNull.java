package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.utilities.RenderHelper.RenderEntityBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;

public class RenderNull extends RenderEntityBase{
	
    public RenderNull(RenderManager manager){
        super(manager);
    }

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw,float pitch){}
}