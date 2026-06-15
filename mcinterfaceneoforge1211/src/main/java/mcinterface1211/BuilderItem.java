package mcinterface1211;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemFood;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Builder for MC items.  Constructing a new item with this builder This will automatically
 * construct the item and will add it to the appropriate maps for automatic registration.
 * When interfacing with MC systems use this class, but when doing code in MTS use the item,
 * NOT the builder!
 *
 * @author don_bruce
 */
public class BuilderItem extends Item implements IBuilderItemInterface {
    protected static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, InterfaceLoader.MODID);

    /**
     * Map of created items linked to their builder instances.  Used for interface operations.
     **/
    protected static final Map<AItemBase, BuilderItem> itemMap = new LinkedHashMap<>();

    /**
     * Current item we are built around.
     **/
    private final AItemBase item;

    /** Modifiers applied when the item is in the mainhand of a user. */
    private final ItemAttributeModifiers defaultModifiers;

    public BuilderItem(Item.Properties properties, AItemBase item) {
        super(properties);
        this.item = item;
        itemMap.put(item, this);

        //If the item is for OreDict, make it a fake tag, since we are forced to use JSON otherwise.
        //Stupid JSON everything without code hooks.
        if (item instanceof AItemPack) {
            AItemPack<?> packItem = (AItemPack<?>) item;
            if (packItem.definition.general.oreDict != null) {
                String lowerCaseOre = packItem.definition.general.oreDict.toLowerCase(Locale.ROOT);
                List<BuilderItem> items = InterfaceCore.taggedItems.get(lowerCaseOre);
                if (items == null) {
                    items = new ArrayList<>();
                    InterfaceCore.taggedItems.put(lowerCaseOre, items);
                }
                items.add(this);
            }
        }

        //Add weapon modifiers.
        ItemAttributeModifiers.Builder attrBuilder = ItemAttributeModifiers.builder();
        if (item instanceof ItemItem && ((ItemItem) item).definition.weapon != null) {
            ItemItem weapon = (ItemItem) item;
            if (weapon.definition.weapon.attackDamage != 0) {
                attrBuilder.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath("mts", "attack_damage_modifier"), weapon.definition.weapon.attackDamage - 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            }
            if (weapon.definition.weapon.attackCooldown != 0) {
                attrBuilder.add(Attributes.ATTACK_SPEED, new AttributeModifier(ResourceLocation.fromNamespaceAndPath("mts", "attack_speed_modifier"), 20D / weapon.definition.weapon.attackCooldown - 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            }
        }
        this.defaultModifiers = attrBuilder.build();
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
    protected String getOrCreateDescriptionId() {
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipLines, TooltipFlag flagIn) {
        List<String> textLines = new ArrayList<>();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            item.addTooltipLines(textLines, new WrapperNBT(customData.copyTag()));
        } else {
            item.addTooltipLines(textLines, InterfaceManager.coreInterface.getNewNBTWrapper());
        }
        textLines.forEach(line -> tooltipLines.add(Component.literal(line)));
    }

    /**
     * This is called by the main MC system to determine how long it takes to eat it.
     * If we are a food item, this should match our eating time.
     */
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return item instanceof IItemFood ? ((IItemFood) item).getTimeToEat() : 0;
    }

    /**
     * This is called by the main MC system do do item use actions.
     * If we are a food item, and can be eaten, return eating here.
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if (item instanceof IItemFood) {
            IItemFood food = (IItemFood) item;
            if (food.getTimeToEat() > 0) {
                return food.isDrink() ? UseAnim.DRINK : UseAnim.EAT;
            }
        }
        return UseAnim.NONE;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return this.defaultModifiers;
    }

    /**
     * This is called by the main MC system to "use" this item on a block.
     * Forwards this to the main item for processing.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() == InteractionHand.MAIN_HAND) {
            if (item.onBlockClicked(WrapperWorld.getWrapperFor(context.getLevel()), WrapperPlayer.getWrapperFor(context.getPlayer()), new Point3D(context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ()), Axis.valueOf(context.getClickedFace().name()))) {
                return InteractionResult.SUCCESS;
            } else if (context.getPlayer() != null && context.getPlayer().isCrouching()) {
                //Forward sneak click too, since blocks don't get these.
                if (!context.getLevel().isClientSide) {
                    BlockEntity tile = context.getLevel().getBlockEntity(context.getClickedPos());
                    if (tile instanceof BuilderTileEntity) {
                        if (((BuilderTileEntity) tile).tileEntity != null) {
                            return ((BuilderTileEntity) tile).tileEntity.interact(WrapperPlayer.getWrapperFor(context.getPlayer())) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
                        }
                    }
                }
                return InteractionResult.FAIL;
            }
            return item instanceof IItemFood && ((IItemFood) item).getTimeToEat() > 0 ? InteractionResult.PASS : InteractionResult.FAIL;
        } else {
            return super.useOn(context);
        }
    }

    /**
     * This is called by the main MC system to "use" this item.
     * Forwards this to the main item for processing.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            //If we are a food item, set our hand to start eating.
            //If we are a gun item, set our hand to prevent attacking.
            if ((item instanceof IItemFood && ((IItemFood) item).getTimeToEat() > 0 && player.canEat(true)) || (item instanceof ItemPartGun && ((ItemPartGun) item).definition.gun.handHeld)) {
                player.startUsingItem(hand);
            }
            return item.onUsed(WrapperWorld.getWrapperFor(world), WrapperPlayer.getWrapperFor(player)) ? InteractionResultHolder.success(player.getItemInHand(hand)) : InteractionResultHolder.fail(player.getItemInHand(hand));
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
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entityLiving) {
        if (item instanceof IItemFood) {
            if (entityLiving instanceof Player) {
                IItemFood food = ((IItemFood) item);
                Player player = (Player) entityLiving;

                //Add hunger and saturation.
                player.getFoodData().eat(food.getHungerAmount(), food.getSaturationAmount());

                //Add effects.
                List<JSONPotionEffect> effects = food.getEffects();
                if (!world.isClientSide && effects != null) {
                    for (JSONPotionEffect effect : effects) {
                        Potion potion = BuiltInRegistries.POTION.get(ResourceLocation.parse(effect.name));
                        if (potion != null) {
                            potion.getEffects().forEach(mcEffect -> {
                                entityLiving.addEffect(new MobEffectInstance(mcEffect.getEffect(), effect.duration, effect.amplifier, false, true));
                            });
                        } else {
                            throw new NullPointerException("Potion " + effect.name + " does not exist.");
                        }
                    }
                }

                //Play sound of food being eaten and add stats.
                world.playSound(player, player.position().x, player.position().y, player.position().z, SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
                if (player instanceof ServerPlayer) {
                    CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer) player, stack);
                }
            }
            //Remove 1 item due to it being eaten.
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player player) {
        return item.canBreakBlocks();
    }
}
