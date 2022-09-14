package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.guis.instances.GUIOverlay;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Base GUI class.  This type is used in lieu of the MC GUI class to allow us to use
 * completely custom GUI code that is not associated with MC's standard GUI code.  Allows us to only
 * update the IWrapper rather than the whole GUI. In essence, this class holds the data and state of the
 * GUI, while the IWrapper chooses how to interpret and render said state.
 *
 * @author don_bruce
 */
public abstract class AGUIBase {
    public static final int STANDARD_GUI_WIDTH = 256;
    public static final int STANDARD_GUI_HEIGHT = 192;
    public static final String STANDARD_TEXTURE_NAME = "mts:textures/guis/standard.png";
    protected static final int STANDARD_COLOR_WIDTH = 20;
    protected static final int STANDARD_COLOR_HEIGHT = 20;
    protected static final int STANDARD_COLOR_WIDTH_OFFSET = 236;
    protected static final int STANDARD_RED_HEIGHT_OFFSET = 196;
    protected static final int STANDARD_YELLOW_HEIGHT_OFFSET = 216;
    protected static final int STANDARD_BLACK_HEIGHT_OFFSET = 236;

    protected GUIComponentCutout background;
    public final List<AGUIComponent> components = new ArrayList<>();

    public static final ConcurrentLinkedQueue<AGUIBase> activeGUIs = new ConcurrentLinkedQueue<>();
    public static AGUIBase activeInputGUI;

    protected int screenWidth;
    protected int screenHeight;
    protected int guiLeft;
    protected int guiTop;

    static {
        //Add the overlay GUI to the GUI listing and keep it there forever.
        new GUIOverlay();
    }

    public AGUIBase() {
        activeGUIs.add(this);
        if (capturesPlayer()) {
            activeInputGUI = this;
            InterfaceManager.clientInterface.setActiveGUI(this);
            InterfaceManager.inputInterface.setKeyboardRepeat(true);
        }
    }

    /**
     * Called to start the component setup sequence.  This sets the screen size variables.
     * If one needs to re-create the screen without changing size, call {@link #setupComponents()},
     * not this method.
     */
    public void setupComponentsInit(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.guiLeft = (screenWidth - getWidth()) / 2;
        this.guiTop = renderFlushBottom() ? screenHeight - getHeight() : (screenHeight - getHeight()) / 2;
        setupComponents();
    }

    /**
     * Called during init to allow for the creation of GUI components.  All components
     * should be created in this method, and should be added via the appropriate calls.
     * The passed-in guiLeft and guiTop parameters are the top-left of the TEXTURE of
     * this GUI, not the screen.  This allows for all objects to be created using offsets
     * that won't change, rather than screen pixels that will.  By default, this creates the
     * background component based on the various methods in here.
     */
    public void setupComponents() {
        components.clear();
        if (renderBackgroundFullTexture()) {
            addComponent(this.background = new GUIComponentCutout(guiLeft, guiTop, getWidth(), getHeight()));
        } else {
            addComponent(this.background = new GUIComponentCutout(guiLeft, guiTop, getWidth(), getHeight(), 0, 0, getWidth(), getHeight()));
        }
    }

    /**
     * Adds a rendered square of texture to the passed-in buffer based on the passed-in parameters.
     */
    public void addRenderToBuffer(FloatBuffer buffer, int offsetX, int offsetY, int width, int height, float u, float v, float U, float V, int textureWidth, int textureHeight) {
        //First convert to 0->1 UV space.
        u = u / textureWidth;
        U = U / textureWidth;
        v = v / textureHeight;
        V = V / textureHeight;

        //Now populate the buffer.
        for (int i = 0; i < 6; ++i) {
            //Normals will always be 0, 0, 1.
            buffer.put(0.0F);
            buffer.put(0.0F);
            buffer.put(1.0F);

            //Texture and vertex X/Y are based on vertex index.
            switch (i) {
                case (0):
                case (3): {//Bottom-right
                    buffer.put(U);
                    buffer.put(V);
                    buffer.put(offsetX + width);
                    buffer.put(offsetY - height);
                    break;
                }
                case (1): {//Top-right
                    buffer.put(U);
                    buffer.put(v);
                    buffer.put(offsetX + width);
                    buffer.put(offsetY);
                    break;
                }
                case (2):
                case (4): {//Top-left
                    buffer.put(u);
                    buffer.put(v);
                    buffer.put(offsetX);
                    buffer.put(offsetY);
                    break;
                }
                case (5): {//Bottom-left
                    buffer.put(u);
                    buffer.put(V);
                    buffer.put(offsetX);
                    buffer.put(offsetY - height);
                    break;
                }
            }

            //Vertex z is always 0.
            buffer.put(0.0F);
        }
    }

    /**
     * Called right before rendering to allow GUIs to set the states of their objects.
     * By default, this sets the state of the background based on the return value of
     * {@link #renderBackground()}
     */
    public void setStates() {
        background.visible = renderBackground();
    }

    /**
     * Called to render the components in this GUI.  This is a final method to discourage manual rendering
     * and instead force use of the component-based rendering that provides a state-safe framework.
     */
    public final void render(int mouseX, int mouseY, boolean blendingEnabled, float partialTicks) {
        //First set the states for things in this GUI.
        //Only do this on the normal render pass.
        if (!blendingEnabled) {
            setStates();
        }

        //If we are light-sensitive, set lighting to our position.
        boolean ignoreLightState = getGUILightMode().equals(GUILightingMode.NONE);
        if (!ignoreLightState) {
            InterfaceManager.renderingInterface.setLightingToPosition(getGUILightSource().position);
        }

        //Render main components once, but only based on translucent state.
        //While instruments might get rendered on both passes, normal components
        //only get rendered on one or the other.
        if (renderTranslucent() == blendingEnabled) {
            //Render textured components except instruments.  These choose if they render or not depending on visibility.
            for (AGUIComponent component : components) {
                if (component.visible && !(component instanceof GUIComponentInstrument)) {
                    component.render(this, mouseX, mouseY, ignoreLightState, false, blendingEnabled, partialTicks);
                }
            }

            //If we are light-sensitive, and this GUI is said to be lit up, render the lit components here.
            //This requires a re-render of all the components to ensure the lit texture portions of said components render.
            if (getGUILightMode().equals(GUILightingMode.LIT)) {
                for (AGUIComponent component : components) {
                    if (component.visible && !(component instanceof GUIComponentInstrument) && !(component instanceof GUIComponentItem)) {
                        component.render(this, mouseX, mouseY, true, true, true, partialTicks);
                    }
                }
            }

            //Now that all main rendering is done, render text.
            //This includes labels, button text, and text boxes.
            //We only need to do this once, even if we are lit, as we just change the text lighting.
            boolean isTextLit = !getGUILightMode().equals(GUILightingMode.DARK);
            for (AGUIComponent component : components) {
                if (component.visible && component.text != null) {
                    component.renderText(isTextLit);
                }
            }
        }

        //Render instruments.  These need both normal and blended passes.
        for (AGUIComponent component : components) {
            if (component.visible && component instanceof GUIComponentInstrument) {
                component.render(this, mouseX, mouseY, false, false, blendingEnabled, partialTicks);
            }
        }

        //Render any tooltips.  These only render on non-blended passes.
        if (!blendingEnabled) {
            for (AGUIComponent component : components) {
                if (component.visible && component.isMouseInBounds(mouseX, mouseY)) {
                    component.renderTooltip(this, mouseX, mouseY);
                }
            }
        }
    }

    /**
     * Closes this GUI.  Normally just removes us from the active list and
     * clears model caches, but can be extended to do other things.
     */
    public void close() {
        if (activeGUIs.contains(this)) {
            activeGUIs.remove(this);
            if (capturesPlayer()) {
                activeInputGUI = null;
                InterfaceManager.clientInterface.closeGUI();
                InterfaceManager.inputInterface.setKeyboardRepeat(false);
            }
            GUIComponent3DModel.clearModelCaches();
        }
    }

    /**
     * If the passed-in GUI class is currently active, this closes it.
     * Prevents the need to loop over and watch out for CMEs in generic closing code.
     */
    public static void closeIfOpen(Class<? extends AGUIBase> guiClass) {
        AGUIBase guiToClose = null;
        for (AGUIBase gui : activeGUIs) {
            if (gui.getClass().equals(guiClass)) {
                guiToClose = gui;
                break;
            }
        }
        if (guiToClose != null) {
            guiToClose.close();
        }
    }

    /**
     * If this is false, then no background texture will be rendered.
     */
    protected boolean renderBackground() {
        return true;
    }

    /**
     * If this is true, then no background texture will be fitted to the entire screen.
     * Normally false, as this stretches it, but for whole overlays this should be true.
     */
    protected boolean renderBackgroundFullTexture() {
        return false;
    }

    /**
     * Used to determine lighting for this GUI.  If {@link GUILightingMode#NONE} is returned,
     * then this GUI renders as normal.  If {@link GUILightingMode#DARK} is returned, then the
     * brightness of the GUI is set to the brightness of the player.  If {@link GUILightingMode#LIT}
     * is returned, then the GUI is rendered with lit text and items, but dark controls.  It is
     * also rendered with the lit texture (the texture name with _lit appended) as well.  This
     * can be used to make light-sensitive GUIs for vehicles as well as other things.
     */
    protected GUILightingMode getGUILightMode() {
        return GUILightingMode.NONE;
    }

    /**
     * Returns the source of where to calculate the light for this GUI.  This is required
     * if {@link #getGUILightMode()} is any value other than {@link GUILightingMode#NONE}.
     */
    protected AEntityB_Existing getGUILightSource() {
        return null;
    }

    /**
     * If this is true, then the GUI will pause the game when open.
     */
    public boolean pauseOnOpen() {
        return false;
    }

    /**
     * If this is true, then the GUI will capture the mouse when open.  This also prevents player movement.
     */
    public boolean capturesPlayer() {
        return true;
    }

    /**
     * Returns the width of this GUI.  Used for centering.
     */
    public int getWidth() {
        return STANDARD_GUI_WIDTH;
    }

    /**
     * Returns the height of this GUI.  Used for centering.
     */
    public int getHeight() {
        return STANDARD_GUI_HEIGHT;
    }

    /**
     * If true, the GUI will be rendered flush with the bottom
     * and sides of the screen rather than the center of the screen.
     * This may cause textures to not render in their proper locations,
     * so watch out!
     */
    public boolean renderFlushBottom() {
        return false;
    }

    /**
     * If true, the GUI texture will be rendered translucent.  This affects everything from this texture,
     * including buttons and switches!  Does not affect text-rendering as that's its own system.
     */
    public boolean renderTranslucent() {
        return false;
    }

    /**
     * Returns the width of this GUI's texture.  Used for rendering.
     */
    protected final int getTextureWidth() {
        return getWidth() <= 256 ? 256 : (getWidth() <= 512 ? 512 : (getWidth() <= 1024 ? 1024 : 2048));
    }

    /**
     * Returns the height of this GUI's texture.  Used for rendering.
     */
    protected final int getTextureHeight() {
        return getHeight() <= 256 ? 256 : (getHeight() <= 512 ? 512 : (getHeight() <= 1024 ? 1024 : 2048));
    }

    /**
     * Returns the texture that goes with this GUI.  This will be used for
     * all rendering operations on this cycle, but may be changed out
     * on different cycles if needed.
     */
    protected String getTexture() {
        return STANDARD_TEXTURE_NAME;
    }

    /**
     * Adds an {@link AGUIComponent} to this GUIs component set.  These are rendered
     * automatically given their current state.  Said state should be set in {@link #setStates()}.
     */
    protected void addComponent(AGUIComponent component) {
        components.add(component);
    }

    /**
     * Clock method used to make flashing text and icons on screen.  Put here
     * for all GUIs to use.  Returns true if the period is active.  Both
     * parameters are in ticks, or 1/20 a second.
     */
    protected static boolean inClockPeriod(int totalPeriod, int onPeriod) {
        return System.currentTimeMillis() * 0.02D % totalPeriod <= onPeriod;
    }

    /**
     * List of enums that define if the GUI is lit or not.
     */
    protected enum GUILightingMode {
        NONE,
        DARK,
        LIT
    }
}
