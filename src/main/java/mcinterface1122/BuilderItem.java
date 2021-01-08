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
import minecrafttransportsimulator.items.components.IItemFood;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
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
@EventBusSubscriber
class BuilderItem extends Item{
	/**Map of created items linked to their builder instances.  Used for interface operations.**/
	static final Map<AItemBase, BuilderItem> itemMap = new LinkedHashMap<AItemBase, BuilderItem>();
	
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
		List<WrapperNBT> dataBlocks = new ArrayList<WrapperNBT>();
		item.getDataBlocks(dataBlocks);
		for(WrapperNBT data : dataBlocks){
			if(this.isInCreativeTab(tab)){
				ItemStack stack = new ItemStack(this);
				stack.setTagCompound(((WrapperNBT) data).tag);
				items.add(stack);
			}
		}
	}
	
	/**
	 *  This is called by the main MC system to determine how long it takes to eat it.
	 *  If we are a food item, this should match our eating time.
	 */
	@Override
	public int getMaxItemUseDuration(ItemStack stack){
		if(item instanceof IItemFood){
			return ((IItemFood) item).getTimeToEat();
		}else if(item instanceof ItemPart && ((ItemPart) item).isHandHeldGun()){
			return Integer.MAX_VALUE;
		}else{
			return 0;
		}
    }
	
	/**
     * This is called by the main MC system do do item use actions.
     * If we are a food item, and can be eaten, return eating here.
     */
	@Override
    public EnumAction getItemUseAction(ItemStack stack){
    	if(item instanceof IItemFood){
    		IItemFood food = (IItemFood) item;
    		if(food.getTimeToEat() > 0){
    			return food.isDrink() ? EnumAction.DRINK : EnumAction.EAT;
    		}
		}
    	return EnumAction.NONE;
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
		//If we are a food item, set our hand to start eating.
		if((item instanceof IItemFood && ((IItemFood) item).getTimeToEat() > 0 && player.canEat(true)) || (item instanceof ItemPart && ((ItemPart) item).isHandHeldGun())){
			player.setActiveHand(hand);
		}
		return item.onUsed(wrapper, wrapper.getWrapperFor(player)) ? new ActionResult<ItemStack>(EnumActionResult.SUCCESS, player.getHeldItem(hand)) : new ActionResult<ItemStack>(EnumActionResult.FAIL, player.getHeldItem(hand));
	}
	
	/**
	 *  This is called by the main MC system to stop using this item.
	 *  Forwards this to the main item for processing.
	 */
	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entityLiving, int timeLeft){
		WrapperWorld wrapper = WrapperWorld.getWrapperFor(world);
		if(entityLiving instanceof EntityPlayer){
			item.onStoppedUsing(wrapper, wrapper.getWrapperFor((EntityPlayer) entityLiving));
		}
	}
	
	/**
	 *  This is called by the main MC system after the item's use timer has expired.
	 *  This is normally instant, as {@link #getMaxItemUseDuration(ItemStack)} is 0.
	 *  If this item is food, and a player is holding the item, have it apply to them. 
	 */
	@Override
	public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entityLiving){
		if(item instanceof IItemFood){
			if(entityLiving instanceof EntityPlayer){
				IItemFood food = ((IItemFood) item);
	            EntityPlayer player = (EntityPlayer) entityLiving;
	            
	            //Add hunger and saturation.
	            player.getFoodStats().addStats(food.getHungerAmount(), food.getSaturationAmount());
	            
	            //Add effects.
	            List<JSONPotionEffect> effects = food.getEffects();
	            if(!world.isRemote && effects != null){
	            	for(JSONPotionEffect effect : effects){
		            	Potion potion = Potion.getPotionFromResourceLocation(effect.name);
		    			if(potion != null){
		    				player.addPotionEffect(new PotionEffect(potion, effect.duration, effect.amplifier, false, false));
		    			}else{
		    				throw new NullPointerException("Potion " + effect.name + " does not exist.");
		    			}
	            	}
	            }
	            
	            //Play sound of food being eaten and add stats.
	            world.playSound(player, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5F, world.rand.nextFloat() * 0.1F + 0.9F);
	            player.addStat(StatList.getObjectUseStats(this));
	            if(player instanceof EntityPlayerMP){
	                CriteriaTriggers.CONSUME_ITEM.trigger((EntityPlayerMP)player, stack);
	            }
	        }
			//Remove 1 item due to it being eaten.
	        stack.shrink(1);
		}
		return stack;
	}
	
	/**
	 * Registers all items we have created up to this point.
	 */
	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event){
		//Register all items in our wrapper map.
		for(Entry<AItemBase, BuilderItem> entry : itemMap.entrySet()){
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
