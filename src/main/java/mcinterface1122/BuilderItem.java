package mcinterface1122;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**Builder for MC items.  Constructor takes a type of {@link AItemBase}, but
 * is only visible when calling {@link #createItem(AItemBase)}.  This will automatically
 * construct the item and will return the created instance of the item (not builder)
 * for use in the code.  The builder instance is cached and saved to be registered
 * in the MC systems.  When interfacing with MC systems use this class, but when
 * doing code in MTS use the item, NOT the builder!
 *
 * @author don_bruce
 */
@Mod.EventBusSubscriber()
class BuilderItem extends Item{
	/**Map of created items linked to their builder instances.  Used for interface operations.**/
	static final Map<AItemBase, BuilderItem> itemWrapperMap = new LinkedHashMap<AItemBase, BuilderItem>();
	
	/**Current entity we are built around.**/
	final AItemBase item;
	
	BuilderItem(AItemBase item){
		super();
		this.item = item;
		setFull3D();
		if(!item.canBeStacked()){
			this.setMaxStackSize(1);
		}
	}
	
	/**
	 *  This is called by the main MC system to get the displayName for the item.
	 *  Normally this is a translated version of the unlocalized name, but we
	 *  allow for use of the wrapper to decide what name we translate.
	 */
	@Override
	public String getItemStackDisplayName(ItemStack stack){
        return item.getItemName();
	}
	
	/**
	 *  This is called by the main MC system to add tooltip lines to the item.
	 *  The ItemStack is passed-in here as it contains NBT data that may be used
	 *  to change the display of the tooltip.  We convert the NBT into wrapper form
	 *  to prevent excess odd calls and allow for a more raw serialization system.
	 *  Also prevents us from using a MC class with a changing name. 
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		item.addTooltipLines(tooltipLines, stack.hasTagCompound() ? new WrapperNBT(stack.getTagCompound()) : new WrapperNBT(new NBTTagCompound()));
	}
	
	/**
	 *  Adds sub-items to the creative tab.  We override this to make custom items in the creative tab.
	 *  This is currently only vehicle engines.
	 */
	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items){
		super.getSubItems(tab, items);
		List<IWrapperNBT> dataBlocks = new ArrayList<IWrapperNBT>();
		item.getDataBlocks(dataBlocks);
		for(IWrapperNBT data : dataBlocks){
			if(this.isInCreativeTab(tab)){
				ItemStack stack = new ItemStack(this);
				stack.setTagCompound(((WrapperNBT) data).tag);
				items.add(stack);
			}
		}
	}
	
	/**
	 *  This is called by the main MC system to "use" this item on a block.
	 *  Forwards this to the main item for processing.
	 */
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		WrapperWorld wrapper = WrapperWorld.getWrapperFor(world);
		return item.onBlockClicked(wrapper, wrapper.getWrapperFor(player), new Point3i(pos.getX(), pos.getY(), pos.getZ()), Axis.valueOf(facing.name())) ? EnumActionResult.SUCCESS : EnumActionResult.FAIL;
	}
	
	/**
	 *  This is called by the main MC system to "use" this item.
	 *  Forwards this to the main item for processing.
	 */
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand){
		WrapperWorld wrapper = WrapperWorld.getWrapperFor(world);
		return item.onUsed(wrapper, wrapper.getWrapperFor(player)) ? new ActionResult<ItemStack>(EnumActionResult.SUCCESS, player.getHeldItem(hand)) : new ActionResult<ItemStack>(EnumActionResult.FAIL, player.getHeldItem(hand));
	}
	
	/**
	 * Registers all items we have created up to this point.
	 */
	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event){
		//Register all items in our wrapper map.
		for(Entry<AItemBase, BuilderItem> entry : itemWrapperMap.entrySet()){
			AItemBase item = entry.getKey();
			BuilderItem mcItem = entry.getValue();
			String tabID = item.getCreativeTabID();
			if(!BuilderCreativeTab.createdTabs.containsKey(tabID)){
				//TODO remove this when all packs use the new system.
				if(item instanceof AItemPack && PackParserSystem.getPackConfiguration(((AItemPack<?>) item).definition.packID) != null){
					 BuilderCreativeTab.createdTabs.put(tabID, new BuilderCreativeTab(PackParserSystem.getPackConfiguration(((AItemPack<?>) item).definition.packID).packName, item)); 
				}else{
					BuilderCreativeTab.createdTabs.put(tabID, new BuilderCreativeTab(Loader.instance().getIndexedModList().get(tabID).getName(), item));
				}
			}
			BuilderCreativeTab.createdTabs.get(tabID).addItem(item);
			
			//TODO remove when packs don't register their own items.
			if(tabID.equals(MasterInterface.MODID)){
				event.getRegistry().register(mcItem.setRegistryName(item.getRegistrationName()).setTranslationKey(item.getRegistrationName()));
			}else if(item instanceof AItemPack){
				if(PackParserSystem.getPackConfiguration(((AItemPack<?>) item).definition.packID) != null){
					event.getRegistry().register(mcItem.setRegistryName(item.getRegistrationName()).setTranslationKey(item.getRegistrationName()));
				}
			}
		}
	}
}
