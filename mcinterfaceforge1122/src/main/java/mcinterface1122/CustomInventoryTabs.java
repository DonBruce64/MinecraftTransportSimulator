package mcinterface1122;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.lwjgl.input.Mouse;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONPack.JSONCustomInventoryTab;
import minecrafttransportsimulator.systems.CustomInventoryTabSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiContainerCreative.ContainerCreative;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Client event adapter for pack-authored creative side tabs on 1.12.2. */
@EventBusSubscriber(value = Side.CLIENT, modid = InterfaceLoader.MODID)
public final class CustomInventoryTabs {
    private static final int VISIBLE_TABS = 4;
    private static final int TAB_WIDTH = 30;
    private static final int TAB_HEIGHT = 27;
    private static final int TAB_STEP = 28;
    private static final int ARROW_WIDTH = 12;
    private static final int ARROW_HEIGHT = 12;
    private static final ResourceLocation TAB_SELECTED_TEXTURE = new ResourceLocation("mts", "textures/guis/custom_inventory_tabs/tab_selected.png");
    private static final ResourceLocation TAB_UNSELECTED_TEXTURE = new ResourceLocation("mts", "textures/guis/custom_inventory_tabs/tab_unselected.png");
    private static final ResourceLocation ARROW_UP_TEXTURE = new ResourceLocation("mts", "textures/guis/custom_inventory_tabs/arrow_up.png");
    private static final ResourceLocation ARROW_DOWN_TEXTURE = new ResourceLocation("mts", "textures/guis/custom_inventory_tabs/arrow_down.png");
    private static final ResourceLocation ARROW_UP_SELECTED_TEXTURE = new ResourceLocation("mts", "textures/guis/custom_inventory_tabs/arrow_up_selected.png");
    private static final ResourceLocation ARROW_DOWN_SELECTED_TEXTURE = new ResourceLocation("mts", "textures/guis/custom_inventory_tabs/arrow_down_selected.png");
    private static final Method SET_CURRENT_TAB = ObfuscationReflectionHelper.findMethod(GuiContainerCreative.class, "func_147050_b", void.class, CreativeTabs.class);
    private static final Method IS_MOUSE_OVER_TAB = ObfuscationReflectionHelper.findMethod(GuiContainerCreative.class, "func_147049_a", boolean.class, CreativeTabs.class, int.class, int.class);

    private static GuiContainerCreative activeScreen;
    private static int activeNativeTab = -1;
    private static String activePackID;
    private static int selectedSideTab = CustomInventoryTabSystem.DEFAULT_TAB;
    private static int firstVisibleSideTab;

    private CustomInventoryTabs() {}

    @SubscribeEvent
    public static void onDraw(GuiContainerEvent.DrawForeground event) {
        if (!(event.getGuiContainer() instanceof GuiContainerCreative)) return;
        GuiContainerCreative screen = (GuiContainerCreative) event.getGuiContainer();
        int nativeTab = screen.getSelectedTabIndex();
        if (screen != activeScreen || nativeTab != activeNativeTab) {
            activeScreen = screen;
            activeNativeTab = nativeTab;
            selectedSideTab = CustomInventoryTabSystem.DEFAULT_TAB;
            firstVisibleSideTab = 0;
            CreativeTabs selectedTab = nativeTab >= 0 && nativeTab < CreativeTabs.CREATIVE_TAB_ARRAY.length ? CreativeTabs.CREATIVE_TAB_ARRAY[nativeTab] : null;
            activePackID = selectedTab instanceof BuilderCreativeTab ? ((BuilderCreativeTab) selectedTab).getPackID() : null;
            filterActiveTab();
        }

        List<JSONCustomInventoryTab> tabs = getActiveTabs();
        if (tabs.isEmpty()) return;
        // DrawForeground runs while GuiContainer's matrix is translated to guiLeft/guiTop.
        // Restore screen-space coordinates so rendering and hit-testing share one layout.
        GlStateManager.pushMatrix();
        GlStateManager.translate(-screen.getGuiLeft(), -screen.getGuiTop(), 0.0F);
        try {
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int x = screen.getGuiLeft() - TAB_WIDTH;
        boolean scrolling = tabs.size() > VISIBLE_TABS;
        int startY = screen.getGuiTop() + (scrolling ? 4 : 8);
        if (scrolling) {
            int arrowX = x + (TAB_WIDTH - ARROW_WIDTH) / 2;
            int upY = startY - ARROW_HEIGHT - 1;
            int downY = startY + VISIBLE_TABS * TAB_STEP;
            drawArrow(arrowX, upY, true, firstVisibleSideTab > 0 && isArrowHovered(event.getMouseX(), event.getMouseY(), arrowX, upY));
            drawArrow(arrowX, downY, false, firstVisibleSideTab + VISIBLE_TABS < tabs.size() && isArrowHovered(event.getMouseX(), event.getMouseY(), arrowX, downY));
        }
        int count = Math.min(scrolling ? VISIBLE_TABS : tabs.size(), tabs.size() - firstVisibleSideTab);
        String hoveredTabName = null;
        for (int visibleIndex = 0; visibleIndex < count; ++visibleIndex) {
            int tabIndex = firstVisibleSideTab + visibleIndex;
            int y = startY + visibleIndex * TAB_STEP;
            boolean selected = tabIndex == selectedSideTab;
            Minecraft.getMinecraft().getTextureManager().bindTexture(selected ? TAB_SELECTED_TEXTURE : TAB_UNSELECTED_TEXTURE);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, TAB_WIDTH, TAB_HEIGHT, TAB_WIDTH, TAB_HEIGHT);
            JSONCustomInventoryTab tab = tabs.get(tabIndex);
            int iconX = x + (selected ? 7 : 9);
            AItemPack<?> iconItem = CustomInventoryTabSystem.getIconItem(activePackID, tab);
            BuilderItem iconBuilder = iconItem != null ? BuilderItem.getBuilderItem(iconItem) : null;
            if (iconBuilder != null) {
                GlStateManager.enableDepth();
                RenderHelper.enableGUIStandardItemLighting();
                Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(new ItemStack(iconBuilder), iconX, y + 5);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            } else if (tab.icon != null && !tab.icon.trim().isEmpty()) {
                String[] icon = CustomInventoryTabSystem.getIconResource(activePackID, tab);
                Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(icon[0], icon[1]));
                Gui.drawModalRectWithCustomSizedTexture(iconX, y + 5, 0.0F, 0.0F, 16, 16, 16.0F, 16.0F);
            }
            if (inside(event.getMouseX(), event.getMouseY(), x, y, TAB_WIDTH, TAB_HEIGHT)) {
                hoveredTabName = tab.name;
            }
        }
        if (hoveredTabName != null) {
            drawTooltip(hoveredTabName, event.getMouseX(), event.getMouseY());
        }
        } finally {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.popMatrix();
        }
    }

    @SubscribeEvent
    public static void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiContainerCreative) || Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return;
        GuiContainerCreative screen = (GuiContainerCreative) event.getGui();
        if (screen != activeScreen) return;
        int mouseX = Mouse.getEventX() * screen.width / Minecraft.getMinecraft().displayWidth;
        int mouseY = screen.height - Mouse.getEventY() * screen.height / Minecraft.getMinecraft().displayHeight - 1;
        List<JSONCustomInventoryTab> tabs = getActiveTabs();
        if (tabs.isEmpty()) return;

        CreativeTabs nativeTab = CreativeTabs.CREATIVE_TAB_ARRAY[screen.getSelectedTabIndex()];
        try {
            if ((Boolean) IS_MOUSE_OVER_TAB.invoke(screen, nativeTab, mouseX, mouseY)) {
                selectedSideTab = CustomInventoryTabSystem.DEFAULT_TAB;
                firstVisibleSideTab = 0;
                SET_CURRENT_TAB.invoke(screen, nativeTab);
                filterActiveTab();
                event.setCanceled(true);
                return;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not test the active creative tab.", exception);
        }

        int x = screen.getGuiLeft() - TAB_WIDTH;
        boolean scrolling = tabs.size() > VISIBLE_TABS;
        int startY = screen.getGuiTop() + (scrolling ? 4 : 8);
        if (scrolling) {
            int arrowX = x + (TAB_WIDTH - ARROW_WIDTH) / 2;
            int upY = startY - ARROW_HEIGHT - 1;
            int downY = startY + VISIBLE_TABS * TAB_STEP;
            if (isArrowHovered(mouseX, mouseY, arrowX, upY) && firstVisibleSideTab > 0) {
                --firstVisibleSideTab;
                event.setCanceled(true);
                return;
            }
            if (isArrowHovered(mouseX, mouseY, arrowX, downY) && firstVisibleSideTab + VISIBLE_TABS < tabs.size()) {
                ++firstVisibleSideTab;
                event.setCanceled(true);
                return;
            }
        }
        int count = Math.min(scrolling ? VISIBLE_TABS : tabs.size(), tabs.size() - firstVisibleSideTab);
        for (int visibleIndex = 0; visibleIndex < count; ++visibleIndex) {
            int tabIndex = firstVisibleSideTab + visibleIndex;
            if (inside(mouseX, mouseY, x, startY + visibleIndex * TAB_STEP, TAB_WIDTH, TAB_HEIGHT)) {
                selectedSideTab = tabIndex;
                try {
                    SET_CURRENT_TAB.invoke(screen, nativeTab);
                } catch (Exception exception) {
                    throw new IllegalStateException("Could not refresh the creative tab.", exception);
                }
                filterActiveTab();
                event.setCanceled(true);
                return;
            }
        }
    }

    private static void filterActiveTab() {
        if (activeScreen == null || activePackID == null || !CustomInventoryTabSystem.isEnabled()) return;
        ContainerCreative container = (ContainerCreative) activeScreen.inventorySlots;
        container.itemList.removeIf(CustomInventoryTabs::isHiddenPackStack);
        container.scrollTo(0.0F);
    }

    private static boolean isHiddenPackStack(ItemStack stack) {
        if (stack.getItem() instanceof BuilderItem) {
            AItemPack<?> item = (AItemPack<?>) ((BuilderItem) stack.getItem()).getItem();
            return !CustomInventoryTabSystem.isItemVisible(item, activePackID, selectedSideTab);
        }
        return false;
    }

    private static List<JSONCustomInventoryTab> getActiveTabs() {
        return activePackID != null && CustomInventoryTabSystem.isEnabled() ? CustomInventoryTabSystem.getTabs(activePackID) : Collections.emptyList();
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static boolean isArrowHovered(double mouseX, double mouseY, int x, int y) {
        return inside(mouseX, mouseY, x - 4, y - 2, ARROW_WIDTH + 8, ARROW_HEIGHT + 4);
    }

    private static void drawArrow(int x, int y, boolean up, boolean hovered) {
        ResourceLocation texture = up ? (hovered ? ARROW_UP_SELECTED_TEXTURE : ARROW_UP_TEXTURE) : (hovered ? ARROW_DOWN_SELECTED_TEXTURE : ARROW_DOWN_TEXTURE);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, ARROW_WIDTH, ARROW_HEIGHT, ARROW_WIDTH, ARROW_HEIGHT);
    }

    private static void drawTooltip(String text, int mouseX, int mouseY) {
        int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(text);
        int x = mouseX + 10;
        int y = mouseY - 12;
        Gui.drawRect(x - 3, y - 3, x + width + 3, y + 11, 0xF0100010);
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, x, y, 0xFFFFFFFF);
    }
}
