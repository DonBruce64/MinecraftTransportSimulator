package mcinterface1201;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Native inventory screen that keeps IV's pack-selectable inventory texture and scrollbar. */
public class BuilderInventoryContainerScreen extends AbstractContainerScreen<BuilderInventoryContainerMenu> {
    private static final int GUI_WIDTH = 194;
    private static final int GUI_HEIGHT = 221;
    private static final int SCROLLBAR_X = 174;
    private static final int SCROLLBAR_TOP = 21;
    private static final int SCROLLBAR_TRAVEL = 73;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLBAR_HEIGHT = 15;

    private final ResourceLocation texture;
    private boolean draggingScrollbar;

    public BuilderInventoryContainerScreen(BuilderInventoryContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.texture = new ResourceLocation(menu.getTexture());
        this.titleLabelX = -10000;
        this.inventoryLabelX = -10000;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(texture, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

        //Container slot backgrounds are atlas tiles because their count is JSON-defined.
        for (int slot = 0; slot < menu.getVisibleSlots(); ++slot) {
            int x = leftPos + 8 + 18 * (slot % BuilderInventoryContainerMenu.COLUMNS);
            int y = topPos + 12 + menu.getInventoryYOffset() + 18 * (slot / BuilderInventoryContainerMenu.COLUMNS);
            graphics.blit(texture, x, y, 194, 0, 18, 18, 256, 256);
        }

        if (menu.getMaxRowOffset() > 0) {
            graphics.blit(texture, leftPos + 174, topPos + 11, 220, 0, 12, 7, 256, 256);
            graphics.blit(texture, leftPos + 174, topPos + 112, 232, 0, 12, 7, 256, 256);
            graphics.blit(texture, leftPos + 173, topPos + 20, 242, 45, 14, 90, 256, 256);
            int thumbY = SCROLLBAR_TOP + SCROLLBAR_TRAVEL * menu.getRowOffset() / menu.getMaxRowOffset();
            graphics.blit(texture, leftPos + SCROLLBAR_X, topPos + thumbY, 244, 15, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 256, 256);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.getMaxRowOffset() > 0) {
            if (isInside(mouseX, mouseY, SCROLLBAR_X, 11, SCROLLBAR_WIDTH, 7)) {
                changeRow(menu.getRowOffset() - 1);
                return true;
            }
            if (isInside(mouseX, mouseY, SCROLLBAR_X, 112, SCROLLBAR_WIDTH, 7)) {
                changeRow(menu.getRowOffset() + 1);
                return true;
            }
            if (isInside(mouseX, mouseY, SCROLLBAR_X, SCROLLBAR_TOP, SCROLLBAR_WIDTH, SCROLLBAR_TRAVEL + SCROLLBAR_HEIGHT)) {
                draggingScrollbar = true;
                updateScrollbar(mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            updateScrollbar(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double verticalDelta) {
        if (menu.getMaxRowOffset() > 0 && verticalDelta != 0) {
            changeRow(menu.getRowOffset() + (verticalDelta < 0 ? 1 : -1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, verticalDelta);
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private void updateScrollbar(double mouseY) {
        double thumbCenter = mouseY - topPos - SCROLLBAR_TOP - SCROLLBAR_HEIGHT / 2.0D;
        int requestedRow = (int) Math.round(thumbCenter * menu.getMaxRowOffset() / SCROLLBAR_TRAVEL);
        changeRow(requestedRow);
    }

    private void changeRow(int requestedRow) {
        int clampedRow = Math.max(0, Math.min(menu.getMaxRowOffset(), requestedRow));
        if (clampedRow != menu.getRowOffset()) {
            menu.setRowOffset(clampedRow);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, clampedRow);
        }
    }
}

