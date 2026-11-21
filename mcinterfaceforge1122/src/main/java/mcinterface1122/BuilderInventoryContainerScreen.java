package mcinterface1122;

import java.io.IOException;

import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** Native 1.12 inventory screen retaining IV's pack-selectable atlas and draggable scrollbar. */
public class BuilderInventoryContainerScreen extends GuiContainer {
    private static final int GUI_WIDTH = 194;
    private static final int GUI_HEIGHT = 221;
    private static final int SCROLLBAR_X = 174;
    private static final int SCROLLBAR_TOP = 21;
    private static final int SCROLLBAR_TRAVEL = 73;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLBAR_HEIGHT = 15;

    private final BuilderInventoryContainerMenu menu;
    private final ResourceLocation texture;
    private boolean draggingScrollbar;

    public BuilderInventoryContainerScreen(BuilderInventoryContainerMenu menu) {
        super(menu);
        this.menu = menu;
        this.xSize = GUI_WIDTH;
        this.ySize = GUI_HEIGHT;
        this.texture = new ResourceLocation(menu.getTexture());
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        //The IV atlas does not reserve space for vanilla inventory labels.
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(texture);
        drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

        for (int slot = 0; slot < menu.getVisibleSlots(); ++slot) {
            int x = guiLeft + 8 + 18 * (slot % BuilderInventoryContainerMenu.COLUMNS);
            int y = guiTop + 12 + menu.getInventoryYOffset() + 18 * (slot / BuilderInventoryContainerMenu.COLUMNS);
            drawModalRectWithCustomSizedTexture(x, y, 194, 0, 18, 18, 256, 256);
        }

        if (menu.getMaxRowOffset() > 0) {
            drawModalRectWithCustomSizedTexture(guiLeft + 174, guiTop + 11, 220, 0, 12, 7, 256, 256);
            drawModalRectWithCustomSizedTexture(guiLeft + 174, guiTop + 112, 232, 0, 12, 7, 256, 256);
            drawModalRectWithCustomSizedTexture(guiLeft + 173, guiTop + 20, 242, 45, 14, 90, 256, 256);
            int thumbY = SCROLLBAR_TOP + SCROLLBAR_TRAVEL * menu.getRowOffset() / menu.getMaxRowOffset();
            drawModalRectWithCustomSizedTexture(guiLeft + SCROLLBAR_X, guiTop + thumbY, 244, 15, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 256, 256);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && menu.getMaxRowOffset() > 0) {
            if (isInside(mouseX, mouseY, SCROLLBAR_X, 11, SCROLLBAR_WIDTH, 7)) {
                changeRow(menu.getRowOffset() - 1);
                return;
            }
            if (isInside(mouseX, mouseY, SCROLLBAR_X, 112, SCROLLBAR_WIDTH, 7)) {
                changeRow(menu.getRowOffset() + 1);
                return;
            }
            if (isInside(mouseX, mouseY, SCROLLBAR_X, SCROLLBAR_TOP, SCROLLBAR_WIDTH, SCROLLBAR_TRAVEL + SCROLLBAR_HEIGHT)) {
                draggingScrollbar = true;
                updateScrollbar(mouseY);
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingScrollbar && clickedMouseButton == 0) {
            updateScrollbar(mouseY);
        } else {
            super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0 && draggingScrollbar) {
            draggingScrollbar = false;
        } else {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheelDelta = Mouse.getEventDWheel();
        if (menu.getMaxRowOffset() > 0 && wheelDelta != 0) {
            changeRow(menu.getRowOffset() + (wheelDelta < 0 ? 1 : -1));
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= guiLeft + x && mouseX < guiLeft + x + width && mouseY >= guiTop + y && mouseY < guiTop + y + height;
    }

    private void updateScrollbar(int mouseY) {
        double thumbCenter = mouseY - guiTop - SCROLLBAR_TOP - SCROLLBAR_HEIGHT / 2.0D;
        int requestedRow = (int) Math.round(thumbCenter * menu.getMaxRowOffset() / SCROLLBAR_TRAVEL);
        changeRow(requestedRow);
    }

    private void changeRow(int requestedRow) {
        int clampedRow = Math.max(0, Math.min(menu.getMaxRowOffset(), requestedRow));
        if (clampedRow != menu.getRowOffset()) {
            menu.setRowOffset(clampedRow);
            mc.playerController.sendEnchantPacket(menu.windowId, clampedRow);
        }
    }
}
