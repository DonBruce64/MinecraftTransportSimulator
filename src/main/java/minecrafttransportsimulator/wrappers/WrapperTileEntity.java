package minecrafttransportsimulator.wrappers;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**Wrapper for the MC Tile Entity class (called BlockEntity in later MC versions cause
 * the people who maintain the mappings like to make life difficult through constant
 * re-naming of things).  This class interfaces with all the MC-specific code, and is 
 * constructed by feeding it an instance of {@link ATileEntityBase}.  This constructor
 * is package-private, as it should only be used by {@link WrapperBlock} to return
 * a Tile Entity for Minecraft to use.  Note that MC re-constructs this class with an
 * empty constructor, so the TE variable may be null for a bit after construction..
 *
 * @author don_bruce
 */
public class WrapperTileEntity extends TileEntity{
	protected ATileEntityBase tileEntity;
	
	public WrapperTileEntity(){
		//Blank constructor for MC.  We set the TE variable in NBT instead.
	}
	
	WrapperTileEntity(ATileEntityBase tileEntity){
		this.tileEntity = tileEntity;
	}
	
	@Override
	public void setWorld(World world){
        super.setWorld(world);
        //Need to set the world wrapper here of the actual TE.
        tileEntity.world = new WrapperWorld(world);
    }
	
	@Override
	public void setPos(BlockPos pos){
		super.setPos(pos);
		//Need to set the position here of the actual TE.
		tileEntity.position = new Point3i(pos.getX(), pos.getY(), pos.getZ());
	}
	
	@Override
	public NBTTagCompound getUpdateTag(){
		//Get the full NBT tag, not just the position!
        return this.writeToNBT(new NBTTagCompound());
    }
	
	@Override
    public void readFromNBT(NBTTagCompound tag){
		super.readFromNBT(tag);
		if(tileEntity == null){
			//Get the block that makes this TE and restore it from saved state.
			tileEntity = WrapperBlock.tileEntityMap.get(tag.getString("teid")).createTileEntity();
		}
		tileEntity.position = new Point3i(pos.getX(), pos.getY(), pos.getZ());
        tileEntity.load(new WrapperNBT(tag));
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag){
		super.writeToNBT(tag);
		tileEntity.save(new WrapperNBT(tag));
		//Also save the class ID so we know what to construct when we load from the world.
		tag.setString("teid", tileEntity.getClass().getSimpleName());
        return tag;
    }
}
