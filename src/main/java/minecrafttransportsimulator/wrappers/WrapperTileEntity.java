package minecrafttransportsimulator.wrappers;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.packets.tileentities.PacketTileEntityClientServerHandshake;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**Wrapper for the MC Tile Entity class (called BlockEntity in later MC versions cause
 * the people who maintain the mappings like to make life difficult through constant
 * re-naming of things).  This class interfaces with all the MC-specific code, and is 
 * constructed by feeding it an instance of {@link ATileEntityBase}.  This constructor
 * is package-private, as it should only be used by {@link WrapperBlock} to return
 * a Tile Entity for Minecraft to use.
 *
 * @author don_bruce
 */
public class WrapperTileEntity extends TileEntity{
	protected final ATileEntityBase tileEntity;
	
	WrapperTileEntity(ATileEntityBase tileEntity){
		this.tileEntity = tileEntity;
	}
	
	@Override
	public void setWorld(World world){
        super.setWorld(world);
        //Need to set the world wrapper here of the actual TE.
        tileEntity.world = new WrapperWorld(world);;
    }
	
	@Override
	public void setPos(BlockPos pos){
		super.setPos(pos);
		tileEntity.position = new Point3i(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
    public void onLoad(){
        if(world.isRemote){
        	//Need to fire a packet off to the server to have it send over the extra data.
        	//MC normally only sends the position.  Not acceptable for our systems!
        	MTS.MTSNet.sendToServer(new PacketTileEntityClientServerHandshake(this, null));
        }
    }
	
	@Override
	public void handleUpdateTag(NBTTagCompound tag){
		//Do nothing here now instead of calling readFromNBT.
		//MC sends incomplete data here in later versions that doesn't contain any of the tags besides the core xyz position.
		//Loading from that tag will wipe all custom data, and that's bad.
	}
	
	@Override
    public void readFromNBT(NBTTagCompound tag){
		super.readFromNBT(tag);
		tileEntity.position = new Point3i(pos.getX(), pos.getY(), pos.getZ());
        tileEntity.load(new WrapperNBT(tag));
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag){
		super.writeToNBT(tag);
		tileEntity.save(new WrapperNBT(tag));
        return tag;
    }
	
	/**Interface that tells the system this block should create an instance of a {@link ATileEntityBase} when created.
	*
	* @author don_bruce
	*/
	public static interface IProvider extends ITileEntityProvider{
		
		@Override
		public default WrapperTileEntity createNewTileEntity(World world, int meta){
			return new WrapperTileEntity(createTileEntity());
		}
		
		public abstract ATileEntityBase createTileEntity();
	}
}
