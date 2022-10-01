package mcinterface1122;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.google.common.collect.Multimap;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemFood;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Builder for MC items.  Constructing a new item with this builder This will automatically
 * construct the item and will add it to the appropriate maps for automatic registration.
 * When interfacing with MC systems use this class, but when doing code in MTS use the item,
 * NOT the builder!
 *
 * @author don_bruce
 */
@EventBusSubscriber
public class BuilderItem extends Item implements IBuilderItemInterface {
    /**
     * Map of created items linked to their builder instances.  Used for interface operations.
     **/
    protected static final Map<AItemBase, BuilderItem> itemMap = new LinkedHashMap<>();

    /**
     * Current item we are built around.
     **/
    private final AItemBase item;

    public BuilderItem(AItemBase item) {
        super();
        this.item = item;
        setFull3D();
        this.setMaxStackSize(item.getStackSize());
        itemMap.put(item, this);
    }

    @Override
    public AItemBase getItem() {
        return item;
    }

    /**
     * This is called by the main MC system to get the displayName for the item.
     * Normally this is a translated version of the unlocalized name, but we
     * allow for use of the wrapper to decide what name we translate.
     */
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return item.getItemName();
    }

    /**
     * This is called by the main MC system to add tooltip lines to the item.
     * The ItemStack is passed-in here as it contains NBT data that may be used
     * to change the display of the tooltip.  We convert the NBT into wrapper form
     * to prevent excess odd calls and allow for a more raw serialization system.
     * Also prevents us from using a MC class with a changing name.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn) {
        if (stack.hasTagCompound()) {
            item.addTooltipLines(tooltipLines, new WrapperNBT(stack.getTagCompound()));
        } else {
            item.addTooltipLines(tooltipLines, InterfaceManager.coreInterface.getNewNBTWrapper());
        }
    }

    /**
     * This is called by the main MC system to determine how long it takes to eat it.
     * If we are a food item, this should match our eating time.
     */
    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return item instanceof IItemFood ? ((IItemFood) item).getTimeToEat() : 0;
    }

    /**
     * This is called by the main MC system do do item use actions.
     * If we are a food item, and can be eaten, return eating here.
     */
    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        if (item instanceof IItemFood) {
            IItemFood food = (IItemFood) item;
            if (food.getTimeToEat() > 0) {
                return food.isDrink() ? EnumAction.DRINK : EnumAction.EAT;
            }
        }
        return EnumAction.NONE;
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> multimap = super.getAttributeModifiers(slot, stack);
        if (item instanceof ItemItem && ((ItemItem) item).definition.weapon != null && slot.equals(EntityEquipmentSlot.MAINHAND)) {
            ItemItem weapon = (ItemItem) item;
            if (weapon.definition.weapon.attackDamage != 0) {
                multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", weapon.definition.weapon.attackDamage - 1, 0));
            }
            if (weapon.definition.weapon.attackCooldown != 0) {
                multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", 20 / weapon.definition.weapon.attackCooldown, 0));
            }
        }
        return multimap;
    }

    /**
     * This is called by the main MC system to "use" this item on a block.
     * Forwards this to the main item for processing.
     */
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand.equals(EnumHand.MAIN_HAND)) {
            return item.onBlockClicked(WrapperWorld.getWrapperFor(world), WrapperPlayer.getWrapperFor(player), new Point3D(pos.getX(), pos.getY(), pos.getZ()), Axis.valueOf(facing.name())) ? EnumActionResult.SUCCESS : EnumActionResult.FAIL;
        } else {
            return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
        }
    }

    /**
     * This is called by the main MC system to "use" this item.
     * Forwards this to the main item for processing.
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (hand.equals(EnumHand.MAIN_HAND)) {
            //If we are a food item, set our hand to start eating.
            //If we are a gun item, set our hand to prevent attacking.
            if ((item instanceof IItemFood && ((IItemFood) item).getTimeToEat() > 0 && player.canEat(true)) || (item instanceof ItemPartGun && ((ItemPartGun) item).definition.gun.handHeld)) {
                player.setActiveHand(hand);
            }
            return item.onUsed(WrapperWorld.getWrapperFor(world), WrapperPlayer.getWrapperFor(player)) ? new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand)) : new ActionResult<>(EnumActionResult.FAIL, player.getHeldItem(hand));
        } else {
            return super.onItemRightClick(world, player, hand);
        }
    }

    /**
     * This is called by the main MC system after the item's use timer has expired.
     * This is normally instant, as {@link #getMaxItemUseDuration(ItemStack)} is 0.
     * If this item is food, and a player is holding the item, have it apply to them.
     */
    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entityLiving) {
        if (item instanceof IItemFood) {
            if (entityLiving instanceof EntityPlayer) {
                IItemFood food = ((IItemFood) item);
                EntityPlayer player = (EntityPlayer) entityLiving;

                //Add hunger and saturation.
                player.getFoodStats().addStats(food.getHungerAmount(), food.getSaturationAmount());

                //Add effects.
                List<JSONPotionEffect> effects = food.getEffects();
                if (!world.isRemote && effects != null) {
                    for (JSONPotionEffect effect : effects) {
                        Potion potion = Potion.getPotionFromResourceLocation(effect.name);
                        if (potion != null) {
                            player.addPotionEffect(new PotionEffect(potion, effect.duration, effect.amplifier, false, false));
                        } else {
                            throw new NullPointerException("Potion " + effect.name + " does not exist.");
                        }
                    }
                }

                //Play sound of food being eaten and add stats.
                world.playSound(player, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5F, world.rand.nextFloat() * 0.1F + 0.9F);
                player.addStat(StatList.getObjectUseStats(this));
                if (player instanceof EntityPlayerMP) {
                    CriteriaTriggers.CONSUME_ITEM.trigger((EntityPlayerMP) player, stack);
                }
            }
            //Remove 1 item due to it being eaten.
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public boolean canDestroyBlockInCreative(World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
        return item.canBreakBlocks();
    }

    /**
     * Registers all items we have created up to this point.
     */
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        //Register all items in our wrapper map.
        for (Entry<AItemBase, BuilderItem> entry : itemMap.entrySet()) {
            AItemPack<?> item = (AItemPack<?>) entry.getKey();
            BuilderItem mcItem = entry.getValue();

            //First check if the creative tab is set/created.
            //The only except is for "invisible" parts of the core mod, these are internal.
            if (!item.definition.packID.equals(InterfaceManager.coreModID) || !item.definition.systemName.contains("invisible")) {
                String tabID = item.getCreativeTabID();
                if (!BuilderCreativeTab.createdTabs.containsKey(tabID)) {
                    JSONPack packConfiguration = PackParser.getPackConfiguration(tabID);
                    BuilderCreativeTab.createdTabs.put(tabID, new BuilderCreativeTab(packConfiguration.packName, itemMap.get(PackParser.getItem(packConfiguration.packID, packConfiguration.packItem))));
                }
                BuilderCreativeTab.createdTabs.get(tabID).addItem(item, mcItem);
            }

            //Register the item.
            event.getRegistry().register(mcItem.setRegistryName(item.getRegistrationName()).setTranslationKey(item.getRegistrationName()));

            //If the item is for OreDict, add it.
            if (item.definition.general.oreDict != null) {
                OreDictionary.registerOre(item.definition.general.oreDict, mcItem);
            }
        }
    }
}
