package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/**A fake ground device that will be added to the multipart when long ground devices are present.
 * Does not render and will be removed in tandem with the ground device that's linked to it.
 * 
 * @author don_bruce
 */
public final class PartGroundDeviceFake extends APartGroundDevice{
	private final APartGroundDevice masterPart;
	
	public PartGroundDeviceFake(APartGroundDevice masterPart, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(masterPart.multipart, packPart, partName, dataTag);
		this.masterPart = masterPart;
	}
	
	@Override
	public boolean isValid(){
		return false;
	}
	
	@Override
	public void removePart(){
		//Do nothing here as we should not be removing ourselves from the multipart.
		//That is our master part's job.
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		return new NBTTagCompound();
	}
	
	@Override
	public float getWidth(){
		return masterPart.getWidth();
	}
	
	@Override
	public float getHeight(){
		return masterPart.getHeight();
	}
	
	@Override
	public Item getItemForPart(){
		return null;
	}
	
	@Override
	public ResourceLocation getModelLocation(){
		return null;
	}
	
	@Override
	public ResourceLocation getTextureLocation(){
		return null;
	}
	
	@Override
	public float getMotiveFriction(){
		return masterPart.getMotiveFriction();
	}
	
	@Override
	public float getLateralFriction(){
		return masterPart.getLateralFriction();
	}
	
	@Override
	public float getLongPartOffset(){
		return -masterPart.getLongPartOffset();
	}
	
	@Override
	public boolean canBeDrivenByEngine(){
		return masterPart.canBeDrivenByEngine();
	}
}
