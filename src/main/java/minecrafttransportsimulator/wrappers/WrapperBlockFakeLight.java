package minecrafttransportsimulator.wrappers;

import java.util.Random;

import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**Wrapper for the MC Block class. In this case, the ONLY use of the block is to have a fake
 * light be present on the ground.  Used for dynamic lighting for shader compat where
 * shaders need vehicles to be lit by a block light to light up at night.  We extend
 * the air block as it's most akin to the block type we need.
 *
 * @author don_bruce
 */
public class WrapperBlockFakeLight extends BlockAir{
	//TODO make this package-private when vehicles get wrapped.
	public static WrapperBlockFakeLight instance = new WrapperBlockFakeLight();
	
    WrapperBlockFakeLight(){
		super();
		setLightLevel(12F/15F);
	}
    
    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand){
    	//Destroy ourselves if there's no vehicle on top of us.
    	//This prevents abandoned fake blocks.
    	for(Entity entity : world.loadedEntityList){
    		if(entity instanceof EntityVehicleF_Physics){
    			if(entity.getPosition().equals(pos)){
    				return;
    			}
    		}
    	}
    	world.setBlockToAir(pos);
    }
}
