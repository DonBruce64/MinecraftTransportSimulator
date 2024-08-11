package mcinterface1165;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.IItemFood;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.potion.Potion;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for MC items.  Constructing a new item with this builder This will automatically
 * construct the item and will add it to the appropriate maps for automatic registration.
 * When interfacing with MC systems use this class, but when doing code in MTS use the item,
 * NOT the builder!
 *
 * @author don_bruce
 */
public class BuilderItem extends Item implements IBuilderItemInterface {
    protected static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, InterfaceLoader.MODID);

    /**
     * Map of created items linked to their builder instances.  Used for interface operations.
     **/
    protected static final Map<AItemBase, BuilderItem> itemMap = new LinkedHashMap<>();

    /**
     * Current item we are built around.
     **/
    private final AItemBase item;

    /**
     * Modifiers applied when the item is in the mainhand of a user.
     */
    private final Multimap<EntityAttribute, EntityAttributeModifier> defaultModifiers;

    public BuilderItem(Item.Settings settings, AItemBase item) {
        super(settings);
        if (group != null) {
            ((BuilderCreativeTab) group).addItem(item, this);
        }
        this.item = item;
        itemMap.put(item, this);

        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        if (item instanceof ItemItem && ((ItemItem) item).definition.weapon != null) {
            ItemItem weapon = (ItemItem) item;
            if (weapon.definition.weapon.attackDamage != 0) {
                builder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Weapon modifier", weapon.definition.weapon.attackDamage - 1, EntityAttributeModifier.Operation.ADDITION));
            }
            if (weapon.definition.weapon.attackCooldown != 0) {
                builder.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Weapon modifier", 20D / weapon.definition.weapon.attackCooldown - 4.0, EntityAttributeModifier.Operation.ADDITION));
            }
        }
        this.defaultModifiers = builder.build();
    }

    @Override
    public AItemBase getWrappedItem() {
        return item;
    }

    /**
     * This is called by the main MC system to get the displayName for the item.
     * Normally this is a translated version of the unlocalized name, but we
     * allow for use of the wrapper to decide what name we translate.
     */
    @Override
    protected String getOrCreateTranslationKey() {
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
    @OnlyIn(Dist.CLIENT)
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        List<String> textLines = new ArrayList<>();
        //tooltipLines.forEach(line -> textLines.add(line.getString()));
        if (stack.hasTag()) {
            item.addTooltipLines(textLines, new WrapperNBT(stack.getTag()));
        } else {
            item.addTooltipLines(textLines, InterfaceManager.coreInterface.getNewNBTWrapper());
        }
        textLines.forEach(line -> tooltip.add(new LiteralText(line)));
    }

    /**
     * This is called by the main MC system to determine how long it takes to eat it.
     * If we are a food item, this should match our eating time.
     */
    @Override
    public int getMaxUseTime(ItemStack stack) {
        return item instanceof IItemFood ? ((IItemFood) item).getTimeToEat() : 0;
    }

    /**
     * This is called by the main MC system do do item use actions.
     * If we are a food item, and can be eaten, return eating here.
     */
    @Override
    public UseAction getUseAction(ItemStack stack) {
        if (item instanceof IItemFood) {
            IItemFood food = (IItemFood) item;
            if (food.getTimeToEat() > 0) {
                return food.isDrink() ? UseAction.DRINK : UseAction.EAT;
            }
        }
        return UseAction.NONE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getAttributeModifiers(slot);
    }

    /**
     * This is called by the main MC system to "use" this item on a block.
     * Forwards this to the main item for processing.
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getHand() == Hand.MAIN_HAND) {
            ActionResult result = item.onBlockClicked(WrapperWorld.getWrapperFor(context.getWorld()), WrapperPlayer.getWrapperFor(context.getPlayer()), new Point3D(context.getBlockPos().getX(), context.getBlockPos().getY(), context.getBlockPos().getZ()), Axis.valueOf(context.getSide().name())) ? ActionResult.SUCCESS : ActionResult.FAIL;
            if (result == ActionResult.FAIL && context.getPlayer().isInSneakingPose()) {
                //Forward sneak click too, since blocks don't get these.
                if (!context.getWorld().isClient) {
                    BlockEntity tile = context.getWorld().getBlockEntity(context.getBlockPos());
                    if (tile instanceof BuilderBlockEntity) {
                        if (((BuilderBlockEntity) tile).tileEntity != null) {
                            return ((BuilderBlockEntity) tile).tileEntity.interact(WrapperPlayer.getWrapperFor(context.getPlayer())) ? ActionResult.SUCCESS : ActionResult.FAIL;
                        }
                    }
                }
                return ActionResult.FAIL;
            }
            return result;
        } else {
            return super.useOnBlock(context);
        }
    }

    /**
     * This is called by the main MC system to "use" this item.
     * Forwards this to the main item for processing.
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (hand == Hand.MAIN_HAND) {
            //If we are a food item, set our hand to start eating.
            //If we are a gun item, set our hand to prevent attacking.
            if ((item instanceof IItemFood && ((IItemFood) item).getTimeToEat() > 0 && player.canConsume(true)) || (item instanceof ItemPartGun && ((ItemPartGun) item).definition.gun.handHeld)) {
                player.setCurrentHand(hand);
            }
            return item.onUsed(WrapperWorld.getWrapperFor(world), WrapperPlayer.getWrapperFor(player)) ? TypedActionResult.success(player.getStackInHand(hand)) : TypedActionResult.fail(player.getStackInHand(hand));
        } else {
            return super.use(world, player, hand);
        }
    }

    /**
     * This is called by the main MC system after the item's use timer has expired.
     * This is normally instant, as {@link #getMaxItemUseDuration(ItemStack)} is 0.
     * If this item is food, and a player is holding the item, have it apply to them.
     */
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity entityLiving) {
        if (item instanceof IItemFood) {
            if (entityLiving instanceof PlayerEntity) {
                IItemFood food = ((IItemFood) item);
                PlayerEntity player = (PlayerEntity) entityLiving;

                //Add hunger and saturation.
                player.getHungerManager().add(food.getHungerAmount(), food.getSaturationAmount());

                //Add effects.
                List<JSONPotionEffect> effects = food.getEffects();
                if (!world.isClient && effects != null) {
                    for (JSONPotionEffect effect : effects) {
                        Potion potion = Potion.byId(effect.name);
                        if (potion != null) {
                            potion.getEffects().forEach(mcEffect -> {
                                entityLiving.addStatusEffect(new StatusEffectInstance(mcEffect.getEffectType(), effect.duration, effect.amplifier, false, true));
                            });
                        } else {
                            throw new NullPointerException("Potion " + effect.name + " does not exist.");
                        }
                    }
                }

                //Play sound of food being eaten and add stats.
                world.playSound(player, player.getPos().x, player.getPos().y, player.getPos().z, SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
                if (player instanceof ServerPlayerEntity) {
                    Criteria.CONSUME_ITEM.trigger((ServerPlayerEntity) player, stack);
                }
            }
            //Remove 1 item due to it being eaten.
            stack.decrement(1);
        }
        return stack;
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        return item.canBreakBlocks();
    }
}
