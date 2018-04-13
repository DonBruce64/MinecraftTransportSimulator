package minecrafttransportsimulator.items;

import minecrafttransportsimulator.entities.parts.EntityCrate;

public class ItemCrate extends ItemPart{
	public ItemCrate(){
		super(EntityCrate.class);
		setFull3D();
		this.setMaxStackSize(1);
	}
	
	//TODO add crate placement code here.
	/*
	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand){
        return super.onItemRightClick(stack, world, player, hand)
    }*/
}
