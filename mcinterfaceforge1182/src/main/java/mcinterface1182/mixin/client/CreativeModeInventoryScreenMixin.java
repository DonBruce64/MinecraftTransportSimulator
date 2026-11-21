package mcinterface1182.mixin.client;

import java.util.Collections;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import mcinterface1182.BuilderCreativeTab;
import mcinterface1182.BuilderItem;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONPack.JSONCustomInventoryTab;
import minecrafttransportsimulator.systems.CustomInventoryTabSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Adds ordered, scrollable pack-defined side tabs to the 1.18 creative screen. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Unique private static final int mts$VISIBLE_TABS = 4;
    @Unique private static final int mts$TAB_WIDTH = 30;
    @Unique private static final int mts$TAB_HEIGHT = 27;
    @Unique private static final int mts$TAB_STEP = 28;
    @Unique private static final int mts$ARROW_WIDTH = 12;
    @Unique private static final int mts$ARROW_HEIGHT = 12;
    @Unique private static final ResourceLocation mts$TAB_SELECTED_TEXTURE = new ResourceLocation("mts", "textures/guis/custom_inventory_tabs/tab_selected.png");
    @Unique private static final ResourceLocation mts$TAB_UNSELECTED_TEXTURE = new ResourceLocation("mts", "textures/guis/custom_inventory_tabs/tab_unselected.png");
    @Unique private static final ResourceLocation mts$ARROW_UP_TEXTURE = new ResourceLocation("mts", "textures/guis/custom_inventory_tabs/arrow_up.png");
    @Unique private static final ResourceLocation mts$ARROW_DOWN_TEXTURE = new ResourceLocation("mts", "textures/guis/custom_inventory_tabs/arrow_down.png");
    @Unique private static final ResourceLocation mts$ARROW_UP_SELECTED_TEXTURE = new ResourceLocation("mts", "textures/guis/custom_inventory_tabs/arrow_up_selected.png");
    @Unique private static final ResourceLocation mts$ARROW_DOWN_SELECTED_TEXTURE = new ResourceLocation("mts", "textures/guis/custom_inventory_tabs/arrow_down_selected.png");

    @Shadow private static int selectedTab;

    @Unique private String mts$packID;
    @Unique private int mts$selectedSideTab = CustomInventoryTabSystem.DEFAULT_TAB;
    @Unique private int mts$firstVisibleSideTab;
    @Unique private boolean mts$refreshingSideTab;

    @Shadow private void selectTab(CreativeModeTab tab) {}

    @Inject(method = "selectTab", at = @At("HEAD"))
    private void mts$beforeSelectTab(CreativeModeTab tab, CallbackInfo ci) {
        mts$packID = tab instanceof BuilderCreativeTab ? ((BuilderCreativeTab) tab).getPackID() : null;
        if (!mts$refreshingSideTab) {
            mts$selectedSideTab = CustomInventoryTabSystem.DEFAULT_TAB;
            mts$firstVisibleSideTab = 0;
        }
    }

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void mts$afterSelectTab(CreativeModeTab tab, CallbackInfo ci) {
        if (mts$packID != null && CustomInventoryTabSystem.isEnabled()) {
            CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
            screen.getMenu().items.removeIf(stack -> !mts$isVisiblePackStack(stack));
            screen.getMenu().scrollTo(0.0F);
        }
    }

    @Unique
    private boolean mts$isVisiblePackStack(ItemStack stack) {
        if (stack.getItem() instanceof BuilderItem) {
            AItemPack<?> item = (AItemPack<?>) ((BuilderItem) stack.getItem()).getWrappedItem();
            return CustomInventoryTabSystem.isItemVisible(item, mts$packID, mts$selectedSideTab);
        }
        return true;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mts$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        List<JSONCustomInventoryTab> tabs = mts$getActiveTabs();
        if (button != 0 || tabs.isEmpty()) return;
        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
        int x = screen.getGuiLeft() - mts$TAB_WIDTH;
        boolean scrolling = tabs.size() > mts$VISIBLE_TABS;
        int startY = screen.getGuiTop() + (scrolling ? 4 : 8);
        if (scrolling) {
            int arrowX = x + (mts$TAB_WIDTH - mts$ARROW_WIDTH) / 2;
            int upY = startY - mts$ARROW_HEIGHT - 1;
            int downY = startY + mts$VISIBLE_TABS * mts$TAB_STEP;
            if (mts$isArrowHovered(mouseX, mouseY, arrowX, upY) && mts$firstVisibleSideTab > 0) {
                --mts$firstVisibleSideTab;
                cir.setReturnValue(true);
                return;
            }
            if (mts$isArrowHovered(mouseX, mouseY, arrowX, downY) && mts$firstVisibleSideTab + mts$VISIBLE_TABS < tabs.size()) {
                ++mts$firstVisibleSideTab;
                cir.setReturnValue(true);
                return;
            }
        }
        int count = Math.min(scrolling ? mts$VISIBLE_TABS : tabs.size(), tabs.size() - mts$firstVisibleSideTab);
        for (int visibleIndex = 0; visibleIndex < count; ++visibleIndex) {
            int tabIndex = mts$firstVisibleSideTab + visibleIndex;
            if (mts$inside(mouseX, mouseY, x, startY + visibleIndex * mts$TAB_STEP, mts$TAB_WIDTH, mts$TAB_HEIGHT)) {
                mts$selectedSideTab = tabIndex;
                mts$refreshingSideTab = true;
                try {
                    selectTab(CreativeModeTab.TABS[selectedTab]);
                } finally {
                    mts$refreshingSideTab = false;
                }
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void mts$render(PoseStack pose, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        List<JSONCustomInventoryTab> tabs = mts$getActiveTabs();
        if (tabs.isEmpty()) return;
        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
        int x = screen.getGuiLeft() - mts$TAB_WIDTH;
        boolean scrolling = tabs.size() > mts$VISIBLE_TABS;
        int startY = screen.getGuiTop() + (scrolling ? 4 : 8);
        if (scrolling) {
            int arrowX = x + (mts$TAB_WIDTH - mts$ARROW_WIDTH) / 2;
            int upY = startY - mts$ARROW_HEIGHT - 1;
            int downY = startY + mts$VISIBLE_TABS * mts$TAB_STEP;
            mts$drawArrow(pose, arrowX, upY, true, mts$firstVisibleSideTab > 0 && mts$isArrowHovered(mouseX, mouseY, arrowX, upY));
            mts$drawArrow(pose, arrowX, downY, false, mts$firstVisibleSideTab + mts$VISIBLE_TABS < tabs.size() && mts$isArrowHovered(mouseX, mouseY, arrowX, downY));
        }
        int count = Math.min(scrolling ? mts$VISIBLE_TABS : tabs.size(), tabs.size() - mts$firstVisibleSideTab);
        for (int visibleIndex = 0; visibleIndex < count; ++visibleIndex) {
            int tabIndex = mts$firstVisibleSideTab + visibleIndex;
            int y = startY + visibleIndex * mts$TAB_STEP;
            boolean selected = tabIndex == mts$selectedSideTab;
            RenderSystem.setShaderTexture(0, selected ? mts$TAB_SELECTED_TEXTURE : mts$TAB_UNSELECTED_TEXTURE);
            GuiComponent.blit(pose, x, y, 0.0F, 0.0F, mts$TAB_WIDTH, mts$TAB_HEIGHT, mts$TAB_WIDTH, mts$TAB_HEIGHT);
            JSONCustomInventoryTab tab = tabs.get(tabIndex);
            int iconX = x + (selected ? 7 : 9);
            AItemPack<?> iconItem = CustomInventoryTabSystem.getIconItem(mts$packID, tab);
            BuilderItem iconBuilder = iconItem != null ? BuilderItem.getBuilderItem(iconItem) : null;
            if (iconBuilder != null) {
                Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(new ItemStack(iconBuilder), iconX, y + 5);
            } else if (tab.icon != null && !tab.icon.trim().isEmpty()) {
                String[] icon = CustomInventoryTabSystem.getIconResource(mts$packID, tab);
                RenderSystem.setShaderTexture(0, new ResourceLocation(icon[0], icon[1]));
                GuiComponent.blit(pose, iconX, y + 5, 0.0F, 0.0F, 16, 16, 16, 16);
            }
            if (mts$inside(mouseX, mouseY, x, y, mts$TAB_WIDTH, mts$TAB_HEIGHT)) screen.renderTooltip(pose, new TextComponent(tab.name), mouseX, mouseY);
        }
    }

    @Unique private List<JSONCustomInventoryTab> mts$getActiveTabs() {
        return mts$packID != null && CustomInventoryTabSystem.isEnabled() ? CustomInventoryTabSystem.getTabs(mts$packID) : Collections.emptyList();
    }

    @Unique private static boolean mts$inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Unique private static boolean mts$isArrowHovered(double mouseX, double mouseY, int x, int y) {
        return mts$inside(mouseX, mouseY, x - 4, y - 2, mts$ARROW_WIDTH + 8, mts$ARROW_HEIGHT + 4);
    }

    @Unique private static void mts$drawArrow(PoseStack pose, int x, int y, boolean up, boolean hovered) {
        ResourceLocation texture = up ? (hovered ? mts$ARROW_UP_SELECTED_TEXTURE : mts$ARROW_UP_TEXTURE) : (hovered ? mts$ARROW_DOWN_SELECTED_TEXTURE : mts$ARROW_DOWN_TEXTURE);
        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(pose, x, y, 0.0F, 0.0F, mts$ARROW_WIDTH, mts$ARROW_HEIGHT, mts$ARROW_WIDTH, mts$ARROW_HEIGHT);
    }
}
