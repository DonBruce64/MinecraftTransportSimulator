package minecrafttransportsimulator.guis.components;

import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;

/**
 * Custom item render class.  This class is designed to render a {@link IWrapperItemStack}
 * or list of stacks into the GUI.  This allows us to use a simple string
 * name for the render rather than a bunch of MC calls.
 * Note that this component does not get a stack assigned during construction: you must
 * assign it manually either by setting {@link #stack} for a single stack rendering operation,
 * or {@link #stacks} for a cyclclic rendering operation.  This allows for switching items in GUIs.
 * This is especially useful in crafting GUIs, where you want a static set of item components
 * that switch their states depending on other selections.  The scale is based on the assumption that
 * a single item is 16x16px.
 *
 * @author don_bruce
 */
public class GUIComponentItem extends AGUIComponent {
    public final float scale;
    public IWrapperItemStack stack;
    public List<IWrapperItemStack> stacks;
    private IWrapperItemStack stackToRender;
    private final Point3D translation = new Point3D();

    /**
     * Default item constructor.
     **/
    public GUIComponentItem(int x, int y, float scale) {
        super(x, y, (int) (16 * scale), (int) (16 * scale));
        this.scale = scale;
        //Items are normally rendered with origin at bottom-right like normal models.
        //The 16 y-offset moves them to top-left orientation.
        this.textPosition.set(position.x + scale * 16, position.y - 16F * scale + scale * 8, textPosition.z);
    }

    /**
     * Constructor for an item linked with a button.  Button is assumed to be 18x18px so item will be offset 1px to center.
     **/
    public GUIComponentItem(GUIComponentButton linkedButton) {
        this(linkedButton.constructedX + 1, linkedButton.constructedY + 1, 1.0F);
    }

    @Override
    public int getZOffset() {
        return MODEL_DEFAULT_ZOFFSET;
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        if (stack != null) {
            stackToRender = stack;
        } else if (stacks != null && !stacks.isEmpty()) {
            stackToRender = stacks.get((int) (System.currentTimeMillis() % (stacks.size() * 500) / 500));
        } else {
            stackToRender = null;
            text = null;
        }

        if (stackToRender != null) {
            translation.set(position).scale(scale);
            InterfaceManager.renderingInterface.renderItemModel(stackToRender, translation);

            if (stackToRender.getSize() > 1) {
                text = RenderText.FORMATTING_CHAR + String.valueOf(RenderText.BOLD_FORMATTING_CHAR) + stackToRender.getSize();
            } else {
                text = null;
            }
        }
    }

    @Override
    public void renderText(boolean renderTextLit) {
        RenderText.drawText(text, null, textPosition, ColorRGB.WHITE, TextAlignment.RIGHT_ALIGNED, scale, false, 0, renderTextLit);
    }

    @Override
    public List<String> getTooltipText() {
        if (stackToRender != null && !stackToRender.isEmpty()) {
            return InterfaceManager.clientInterface.getTooltipLines(stackToRender);
        } else {
            return null;
        }
    }
}
