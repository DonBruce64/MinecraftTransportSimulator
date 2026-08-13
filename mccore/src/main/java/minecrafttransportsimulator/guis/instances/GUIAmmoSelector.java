package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.IInventoryProvider;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;
import minecrafttransportsimulator.guis.components.GUIComponentIcon;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPart.HUDIconType;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartGun;
import minecrafttransportsimulator.packets.instances.PacketPartSeat;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Bottom-right weapon HUD.  The persistent panel contains detailed information for the
 * active weapon; holding ALT opens a compact weapon-only drawer immediately above it.
 * While open, the player may select with the mouse wheel or arrow keys.  The ammo-select
 * key still cycles the active gun's compatible ammunition.
 *
 * @author don_bruce
 */
public class GUIAmmoSelector extends AGUIBase {
    public static final int MAX_VISIBLE_ROWS = 9;

    private static final int HUD_WIDTH = 138;
    private static final int MAIN_PANEL_HEIGHT = 81;
    private static final int MAIN_FOOTER_HEIGHT = 21;
    private static final int DRAWER_ROW_HEIGHT = 24;
    private static final int DRAWER_ROW_GAP = 2;
    private static final int DRAWER_GAP = 9;
    private static final int DRAWER_COUNT_WIDTH = 45;
    private static final int EDGE_PADDING = 0;
    private static final int INNER_PADDING = 4;
    private static final int WEAPON_ICON_SIZE = 17;
    private static final int AMMO_ICON_SIZE = 21;
    private static final int ICON_TEXTURE_SIZE = 32;
    private static final int ICON_TEXTURE_X = 0;
    private static final int ICON_TEXTURE_Y = 0;
    private static final String INFINITE_AMMO_TEXT = "\u221E";
    private static final String FIRE_MODE_SEMI_AUTO = "SEMI";
    private static final String FIRE_MODE_FULL_AUTO = "AUTO";
    private static final float BACKDROP_ALPHA = 0.5F;
    private static final float DIVIDER_ALPHA = 0.5F;
    private static final ColorRGB PRIMARY_COLOR = ColorRGB.WHITE;
    private static final ColorRGB SECONDARY_COLOR = ColorRGB.LIGHT_GRAY;

    public static GUIAmmoSelector current;

    private final IWrapperPlayer player;
    private final List<GunGroupEntry> entries = new ArrayList<>();
    private final int selectionModifierKeyCode;
    private final int selectionPreviousKeyCode;
    private final int selectionNextKeyCode;
    private boolean selectionModifierPressedLast;
    private boolean selectionPreviousPressedLast;
    private boolean selectionNextPressedLast;
    private boolean drawerOpen;
    private int highlightedEntryIndex;
    private int visibleWindowStart;
    private int visibleRowCount = MAX_VISIBLE_ROWS;

    private GUIComponentCutout mainBackdrop;
    private GUIComponentCutout mainFooterDivider;
    private GUIComponentLabel mainWeaponName;
    private GUIComponentIcon mainAmmoIcon;
    private GUIComponentLabel mainAmmoType;
    private GUIComponentLabel mainAmmoName;
    private GUIComponentLabel mainAmmoCount;
    private GUIComponentLabel mainFireMode;

    private final List<GUIComponentCutout> drawerBackdrops = new ArrayList<>();
    private final List<GUIComponentCutout> drawerDividers = new ArrayList<>();
    private final List<GUIComponentIcon> drawerIcons = new ArrayList<>();
    private final List<GUIComponentLabel> drawerTitles = new ArrayList<>();
    private final List<GUIComponentLabel> drawerCounts = new ArrayList<>();
    private final List<GUIComponentCutout> drawerBorders = new ArrayList<>();

    private PartSeat currentSeat;

    public GUIAmmoSelector(IWrapperPlayer player) {
        super();
        this.player = player;
        selectionModifierKeyCode = InterfaceManager.inputInterface.getKeyCodeForName("LMENU");
        selectionPreviousKeyCode = InterfaceManager.inputInterface.getKeyCodeForName("UP");
        selectionNextKeyCode = InterfaceManager.inputInterface.getKeyCodeForName("DOWN");
        current = this;
    }

    @Override
    public void setupComponentsInit(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.guiLeft = screenWidth - HUD_WIDTH - EDGE_PADDING;
        this.guiTop = screenHeight - MAIN_PANEL_HEIGHT - EDGE_PADDING;
        int drawerHeight = Math.max(0, guiTop - DRAWER_GAP);
        visibleRowCount = Math.max(1, Math.min(MAX_VISIBLE_ROWS, (drawerHeight + DRAWER_ROW_GAP) / (DRAWER_ROW_HEIGHT + DRAWER_ROW_GAP)));
        setupComponents();
    }

    @Override
    public void setupComponents() {
        components.clear();
        drawerBackdrops.clear();
        drawerDividers.clear();
        drawerIcons.clear();
        drawerTitles.clear();
        drawerCounts.clear();
        drawerBorders.clear();

        mainBackdrop = createPanel(guiLeft, guiTop, HUD_WIDTH, MAIN_PANEL_HEIGHT);
        mainFooterDivider = createLine(guiLeft, guiTop + MAIN_PANEL_HEIGHT - MAIN_FOOTER_HEIGHT, HUD_WIDTH, 1, DIVIDER_ALPHA);

        mainWeaponName = new GUIComponentLabel(guiLeft + INNER_PADDING, guiTop + 3, PRIMARY_COLOR, "",
                TextAlignment.LEFT_ALIGNED, 0.75F, HUD_WIDTH - 2 * INNER_PADDING, null, true);
        configureLabel(mainWeaponName);

        mainAmmoIcon = new GUIComponentIcon(this, guiLeft + HUD_WIDTH - INNER_PADDING - AMMO_ICON_SIZE, guiTop + 11,
                AMMO_ICON_SIZE, AMMO_ICON_SIZE, ICON_TEXTURE_X, ICON_TEXTURE_Y, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
        mainAmmoIcon.ignoreGUILightingState = true;
        addComponent(mainAmmoIcon);

        mainAmmoType = new GUIComponentLabel(guiLeft + HUD_WIDTH - INNER_PADDING - AMMO_ICON_SIZE / 2, guiTop + 33,
                SECONDARY_COLOR, "", TextAlignment.CENTERED, 0.5625F);
        configureLabel(mainAmmoType);

        mainAmmoCount = new GUIComponentLabel(guiLeft + HUD_WIDTH - INNER_PADDING, guiTop + 38,
                PRIMARY_COLOR, "", TextAlignment.RIGHT_ALIGNED, 1.5F, HUD_WIDTH - 2 * INNER_PADDING, null, true);
        configureLabel(mainAmmoCount);

        mainAmmoName = new GUIComponentLabel(guiLeft + HUD_WIDTH - INNER_PADDING, guiTop + 51,
                PRIMARY_COLOR, "", TextAlignment.RIGHT_ALIGNED, 0.75F, HUD_WIDTH - 2 * INNER_PADDING, null, true);
        configureLabel(mainAmmoName);

        mainFireMode = new GUIComponentLabel(guiLeft + INNER_PADDING, guiTop + MAIN_PANEL_HEIGHT - 8,
                PRIMARY_COLOR, "", TextAlignment.LEFT_ALIGNED, 0.75F);
        configureLabel(mainFireMode);

        for (int i = 0; i < visibleRowCount; ++i) {
            int rowY = guiTop - DRAWER_GAP - DRAWER_ROW_HEIGHT - i * (DRAWER_ROW_HEIGHT + DRAWER_ROW_GAP);
            drawerBackdrops.add(createPanel(guiLeft, rowY, HUD_WIDTH, DRAWER_ROW_HEIGHT));
            drawerDividers.add(createLine(guiLeft + HUD_WIDTH - DRAWER_COUNT_WIDTH, rowY, 1, DRAWER_ROW_HEIGHT, DIVIDER_ALPHA));

            GUIComponentIcon icon = new GUIComponentIcon(this, guiLeft + 2, rowY + 4,
                    WEAPON_ICON_SIZE, WEAPON_ICON_SIZE, ICON_TEXTURE_X, ICON_TEXTURE_Y, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
            icon.ignoreGUILightingState = true;
            addComponent(icon);
            drawerIcons.add(icon);

            GUIComponentLabel title = new GUIComponentLabel(guiLeft + 18, rowY + 9,
                    PRIMARY_COLOR, "", TextAlignment.LEFT_ALIGNED, 0.75F,
                    HUD_WIDTH - DRAWER_COUNT_WIDTH - 18 - INNER_PADDING, null, true);
            configureLabel(title);
            drawerTitles.add(title);

            GUIComponentLabel count = new GUIComponentLabel(guiLeft + HUD_WIDTH - 6, rowY + 9,
                    PRIMARY_COLOR, "", TextAlignment.RIGHT_ALIGNED, 0.75F, DRAWER_COUNT_WIDTH - 2 * INNER_PADDING, null, true);
            configureLabel(count);
            drawerCounts.add(count);

            addDrawerBorder(rowY);
        }
    }

    private GUIComponentCutout createPanel(int x, int y, int width, int height) {
        GUIComponentCutout panel = new GUIComponentCutout(this, x, y, width, height,
                STANDARD_COLOR_WIDTH_OFFSET, STANDARD_BLACK_HEIGHT_OFFSET, STANDARD_COLOR_WIDTH, STANDARD_COLOR_HEIGHT) {
            {
                renderable.setAlpha(BACKDROP_ALPHA);
                if (renderableL != null) {
                    renderableL.setAlpha(BACKDROP_ALPHA);
                }
            }
        };
        panel.ignoreGUILightingState = true;
        addComponent(panel);
        return panel;
    }

    private GUIComponentCutout createLine(int x, int y, int width, int height, float alpha) {
        GUIComponentCutout line = new GUIComponentCutout(this, x, y, width, height,
                STANDARD_WHITE_WIDTH_OFFSET, STANDARD_WHITE_HEIGHT_OFFSET, STANDARD_COLOR_WIDTH, STANDARD_COLOR_HEIGHT) {
            {
                renderable.setAlpha(alpha);
                if (renderableL != null) {
                    renderableL.setAlpha(alpha);
                }
            }

            @Override
            public int getZOffset() {
                return 1;
            }
        };
        line.ignoreGUILightingState = true;
        addComponent(line);
        return line;
    }

    private void configureLabel(GUIComponentLabel label) {
        label.ignoreGUILightingState = true;
        addComponent(label);
    }

    private void addDrawerBorder(int rowY) {
        drawerBorders.add(createLine(guiLeft, rowY, HUD_WIDTH, 1, 1F));
        drawerBorders.add(createLine(guiLeft, rowY + DRAWER_ROW_HEIGHT - 1, HUD_WIDTH, 1, 1F));
        drawerBorders.add(createLine(guiLeft, rowY + 1, 1, DRAWER_ROW_HEIGHT - 2, 1F));
        drawerBorders.add(createLine(guiLeft + HUD_WIDTH - 1, rowY + 1, 1, DRAWER_ROW_HEIGHT - 2, 1F));
    }

    @Override
    public void setStates() {
        //Do NOT call super.setStates() — we don't maintain the single `background` field
        //and don't want the base class poking a null reference.
        if (!canStayOpen()) {
            close();
            return;
        }
        updateSeatAndEntries();
        if (drawerOpen && entries.size() <= 1) {
            drawerOpen = false;
        }

        int activeEntryIndex = getActiveEntryIndex();
        if (!drawerOpen) {
            highlightedEntryIndex = activeEntryIndex >= 0 ? activeEntryIndex : 0;
        } else if (!entries.isEmpty()) {
            highlightedEntryIndex = Math.max(0, Math.min(highlightedEntryIndex, entries.size() - 1));
            updateVisibleWindow();
        }
        int panelEntryIndex = drawerOpen && !entries.isEmpty() ? highlightedEntryIndex : activeEntryIndex;
        setMainPanelState(panelEntryIndex >= 0 && panelEntryIndex < entries.size() ? entries.get(panelEntryIndex) : (entries.isEmpty() ? null : entries.get(0)));

        int displayedRows = drawerOpen ? Math.min(visibleRowCount, entries.size() - visibleWindowStart) : 0;
        for (int i = 0; i < visibleRowCount; ++i) {
            GUIComponentCutout backdrop = drawerBackdrops.get(i);
            GUIComponentCutout divider = drawerDividers.get(i);
            GUIComponentIcon icon = drawerIcons.get(i);
            GUIComponentLabel title = drawerTitles.get(i);
            GUIComponentLabel count = drawerCounts.get(i);
            int entryIndex = i < displayedRows ? visibleWindowStart + displayedRows - 1 - i : -1;
            boolean rowVisible = entryIndex >= 0 && entryIndex < entries.size();
            backdrop.visible = rowVisible;
            divider.visible = rowVisible;
            icon.visible = rowVisible;
            title.visible = rowVisible;
            count.visible = rowVisible;
            setDrawerBorderVisible(i, rowVisible && entryIndex == highlightedEntryIndex);

            if (rowVisible) {
                GunGroupEntry entry = entries.get(entryIndex);
                title.text = entry.gunName;
                if (entry.isNoneSlot) {
                    setWeaponIcon(icon, HUDIconType.NONE);
                    count.text = "";
                } else {
                    setWeaponIcon(icon, entry.weaponIconType);
                    count.text = getRoundCountText(entry);
                }
            } else {
                title.text = "";
                count.text = "";
            }
        }
    }

    private void setMainPanelState(GunGroupEntry entry) {
        boolean visible = entry != null;
        mainBackdrop.visible = visible;
        mainFooterDivider.visible = visible;
        mainWeaponName.visible = visible;
        mainAmmoCount.visible = visible && !entry.isNoneSlot;
        mainAmmoName.visible = visible && !entry.isNoneSlot;
        mainFireMode.visible = visible && !entry.isNoneSlot;

        if (!visible) {
            mainAmmoIcon.visible = false;
            mainAmmoType.visible = false;
            return;
        }

        mainWeaponName.text = entry.gunName;
        if (entry.isNoneSlot) {
            mainAmmoIcon.visible = false;
            mainAmmoType.visible = false;
            mainAmmoCount.text = "";
            mainAmmoName.text = "";
            mainFireMode.text = "";
            return;
        }

        mainAmmoCount.text = getRoundCountText(entry);
        mainFireMode.text = entry.fireModeText;
        if (entry.displayBullet != null) {
            setAmmoIcon(mainAmmoIcon, entry.ammoIconType);
            mainAmmoType.visible = true;
            mainAmmoType.text = entry.ammoIconType.label;
            mainAmmoName.text = entry.displayBullet.getItemName();
        } else {
            mainAmmoIcon.visible = false;
            mainAmmoType.visible = false;
            mainAmmoType.text = "";
            mainAmmoName.text = entry.compatibleBullets.isEmpty() ? "No ammo" : "Not loaded";
        }
    }

    private void setWeaponIcon(GUIComponentIcon icon, HUDIconType iconType) {
        icon.textureXOffset = ICON_TEXTURE_X + iconType.ordinal() * ICON_TEXTURE_SIZE;
        icon.textureYOffset = ICON_TEXTURE_Y;
        icon.visible = true;
    }

    private void setAmmoIcon(GUIComponentIcon icon, AmmoIconType iconType) {
        icon.textureXOffset = ICON_TEXTURE_X + (iconType.textureIndex - 8) * ICON_TEXTURE_SIZE;
        icon.textureYOffset = ICON_TEXTURE_Y + ICON_TEXTURE_SIZE;
        icon.visible = true;
    }

    private String getRoundCountText(GunGroupEntry entry) {
        return entry.loadedCount + "/" + getAvailableRoundsText(entry);
    }

    private String getAvailableRoundsText(GunGroupEntry entry) {
        if (ConfigSystem.settings.general.devMode.value) {
            return INFINITE_AMMO_TEXT;
        }
        if (entry.displayBullet != null) {
            int stackCount = entry.availableStacksByBullet.getOrDefault(entry.displayBullet, 0);
            int perItem = Math.max(1, entry.displayBullet.definition.bullet.quantity);
            return String.valueOf(stackCount * perItem);
        }
        return "0";
    }

    private void setDrawerBorderVisible(int rowIndex, boolean visible) {
        int base = rowIndex * 4;
        for (int j = 0; j < 4; ++j) {
            drawerBorders.get(base + j).visible = visible;
        }
    }

    @Override
    protected boolean renderBackground() {
        return false;
    }

    /**
     * Polled each tick from ControlSystem while this HUD is visible.  ALT exposes the
     * list; arrow keys select while it is held.  Releasing ALT closes the drawer.
     */
    public boolean pollSelectionKeys() {
        boolean modifierPressed = selectionModifierKeyCode > 0 && InterfaceManager.inputInterface.isKeyPressed(selectionModifierKeyCode);
        boolean modifierJustPressed = modifierPressed && !selectionModifierPressedLast;
        boolean modifierJustReleased = !modifierPressed && selectionModifierPressedLast;
        boolean consumedModifierPress = false;

        if (modifierJustPressed) {
            updateSeatAndEntries();
            drawerOpen = currentSeat != null && entries.size() > 1;
            int activeEntryIndex = getActiveEntryIndex();
            highlightedEntryIndex = activeEntryIndex >= 0 ? activeEntryIndex : 0;
            updateVisibleWindow();
            consumedModifierPress = drawerOpen;
        }

        boolean previousPressed = drawerOpen && modifierPressed && InterfaceManager.inputInterface.isKeyPressed(selectionPreviousKeyCode);
        boolean nextPressed = drawerOpen && modifierPressed && InterfaceManager.inputInterface.isKeyPressed(selectionNextKeyCode);
        if (previousPressed && !selectionPreviousPressedLast) {
            moveHighlight(-1);
        }
        if (nextPressed && !selectionNextPressedLast) {
            moveHighlight(1);
        }
        selectionPreviousPressedLast = previousPressed;
        selectionNextPressedLast = nextPressed;

        if (modifierJustReleased) {
            drawerOpen = false;
        }
        selectionModifierPressedLast = modifierPressed;
        return consumedModifierPress;
    }

    /**
     * Handles an already-captured gameplay wheel step.  Returning true tells the
     * platform adapter to prevent vanilla hotbar scrolling.
     */
    public boolean onMouseWheel(int direction) {
        if (!drawerOpen || direction == 0) {
            return false;
        }
        moveHighlight(direction > 0 ? -1 : 1);
        return true;
    }

    private void moveHighlight(int direction) {
        updateSeatAndEntries();
        if (!entries.isEmpty()) {
            highlightedEntryIndex = Math.floorMod(highlightedEntryIndex + direction, entries.size());
            updateVisibleWindow();
            selectGun(highlightedEntryIndex);
        }
    }

    private void updateVisibleWindow() {
        int maxStart = Math.max(0, entries.size() - visibleRowCount);
        visibleWindowStart = Math.max(0, Math.min(visibleWindowStart, maxStart));
        if (highlightedEntryIndex < visibleWindowStart) {
            visibleWindowStart = highlightedEntryIndex;
        } else if (highlightedEntryIndex >= visibleWindowStart + visibleRowCount) {
            visibleWindowStart = highlightedEntryIndex - visibleRowCount + 1;
        }
        visibleWindowStart = Math.max(0, Math.min(visibleWindowStart, maxStart));
    }

    /**
     * Cycles the preferred bullet of the currently-active gun group to the next compatible type.
     * Invoked from ControlSystem when the ammo-select keybind fires.
     */
    public void cycleActiveGunAmmo() {
        GunGroupEntry entry = getActiveGunEntry();
        if (entry == null || entry.compatibleBullets.size() < 2) {
            return;
        }
        int currentIndex = entry.selectedBullet == null ? -1 : entry.compatibleBullets.indexOf(entry.selectedBullet);
        ItemBullet next = entry.compatibleBullets.get((currentIndex + 1) % entry.compatibleBullets.size());
        if (entry.isFireSolo) {
            InterfaceManager.packetInterface.sendToServer(new PacketPartGun(entry.guns.get(entry.soloIndex - 1), next));
        } else {
            for (PartGun gun : entry.guns) {
                InterfaceManager.packetInterface.sendToServer(new PacketPartGun(gun, next));
            }
        }
    }

    private void selectGun(int slot) {
        if (slot < 0 || slot >= entries.size()) {
            return;
        }
        GunGroupEntry entry = entries.get(slot);
        if (entry.isHandHeld || currentSeat == null) {
            return;
        }
        if (entry.isNoneSlot) {
            //Seat-side code only honours -1 when canDisableGun is true, so this is safe
            //even if the client ever drifts out of sync with the definition.
            InterfaceManager.packetInterface.sendToServer(new PacketPartSeat(currentSeat, -1));
        } else {
            //The index we send refers to the position in the seat's gunGroups LinkedHashMap,
            //not our own entries list — so pass the entry's stored groupIndex.
            InterfaceManager.packetInterface.sendToServer(new PacketPartSeat(currentSeat, entry.groupIndex));
        }
    }

    private int getActiveEntryIndex() {
        if (entries.isEmpty()) {
            return -1;
        }
        if (currentSeat == null) {
            return 0;
        }
        for (int i = 0; i < entries.size(); ++i) {
            GunGroupEntry entry = entries.get(i);
            if (entry.isNoneSlot ? currentSeat.activeGunItem == null : entry.gunItem.equals(currentSeat.activeGunItem)) {
                return i;
            }
        }
        return -1;
    }

    private GunGroupEntry findEntryForGunItem(ItemPartGun gunItem) {
        for (GunGroupEntry entry : entries) {
            if (entry.isNoneSlot) {
                continue;
            }
            if (gunItem.equals(entry.gunItem)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Rebuilds per-group display state each frame so counts and icons track inventory changes.
     */
    private void updateSeatAndEntries() {
        entries.clear();
        currentSeat = null;
        EntityPlayerGun playerGun = EntityPlayerGun.playerClientGuns.get(player.getID());
        if (playerGun != null && playerGun.activeGun != null && player.equals(playerGun.activeGun.getGunController())) {
            addGunEntry((ItemPartGun) playerGun.activeGun.cachedItem, Collections.singletonList(playerGun.activeGun), 0, true);
            return;
        }

        AEntityB_Existing riding = player.getEntityRiding();
        if (!(riding instanceof PartSeat)) {
            return;
        }
        currentSeat = (PartSeat) riding;

        int groupIndex = 0;
        for (Map.Entry<ItemPartGun, List<PartGun>> groupEntry : currentSeat.gunGroups.entrySet()) {
            List<PartGun> groupGuns = new ArrayList<>();
            for (PartGun gun : groupEntry.getValue()) {
                if (gun.isValid && player.equals(gun.getGunController())) {
                    groupGuns.add(gun);
                }
            }
            if (groupGuns.isEmpty()) {
                ++groupIndex;
                continue;
            }

            addGunEntry(groupEntry.getKey(), groupGuns, groupIndex, false);
            ++groupIndex;
        }

        //Append the "None" slot at the top of the stack when the seat permits disabling the gun.
        if (currentSeat.placementDefinition.canDisableGun) {
            GunGroupEntry noneEntry = new GunGroupEntry();
            noneEntry.isNoneSlot = true;
            noneEntry.gunName = "None";
            noneEntry.guns = new ArrayList<>();
            noneEntry.compatibleBullets = new ArrayList<>();
            noneEntry.availableStacksByBullet = new LinkedHashMap<>();
            noneEntry.weaponIconType = HUDIconType.NONE;
            noneEntry.ammoIconType = AmmoIconType.STANDARD;
            entries.add(noneEntry);
        }
    }

    private void addGunEntry(ItemPartGun gunItem, List<PartGun> groupGuns, int groupIndex, boolean isHandHeld) {
        GunGroupEntry entry = new GunGroupEntry();
        entry.gunItem = gunItem;
        entry.guns = groupGuns;
        entry.groupIndex = groupIndex;
        entry.isHandHeld = isHandHeld;
        entry.gunName = entry.gunItem != null ? entry.gunItem.getItemName() : groupGuns.get(0).definition.general.name;
        entry.isFireSolo = !isHandHeld && entry.gunItem != null && entry.gunItem.definition.gun.fireSolo && groupGuns.size() > 1;
        entry.soloCount = groupGuns.size();
        entry.soloIndex = 1;
        if (entry.isFireSolo && currentSeat != null && entry.gunItem.equals(currentSeat.activeGunItem)) {
            entry.soloIndex = Math.min(Math.max(currentSeat.gunIndex, 0), entry.soloCount - 1) + 1;
        }

        Map<ItemBullet, Integer> stackCounts = new LinkedHashMap<>();
        PartGun displayGun = entry.isFireSolo ? groupGuns.get(entry.soloIndex - 1) : groupGuns.get(0);
        int totalLoaded = 0;
        if (entry.isFireSolo) {
            totalLoaded = displayGun.getLoadedBulletCount();
        } else {
            for (PartGun gun : groupGuns) {
                totalLoaded += gun.getLoadedBulletCount();
            }
        }
        entry.loadedCount = totalLoaded;

        tallyCompatible(stackCounts, player.getInventory(), displayGun);
        Set<PartInteractable> talliedCrates = new HashSet<>();
        for (PartGun gun : groupGuns) {
            for (PartInteractable crate : gun.connectedCrates) {
                if (talliedCrates.add(crate) && crate.isActiveVar.isActive && crate.inventory != null) {
                    tallyCompatible(stackCounts, crate.inventory, displayGun);
                }
            }
        }
        entry.availableStacksByBullet = stackCounts;
        entry.compatibleBullets = new ArrayList<>(stackCounts.keySet());

        ItemBullet display = displayGun.getLoadedBulletCount() > 0 ? displayGun.lastLoadedBullet : (displayGun.preferredBullet != null ? displayGun.preferredBullet : displayGun.lastLoadedBullet);
        if (isHandHeld && display != null && !entry.compatibleBullets.contains(display) && !displayGun.hasLoadedOrReloadingBullet(display)) {
            display = displayGun.lastLoadedBullet != null && (entry.compatibleBullets.contains(displayGun.lastLoadedBullet) || displayGun.hasLoadedOrReloadingBullet(displayGun.lastLoadedBullet)) ? displayGun.lastLoadedBullet : null;
        }
        ItemBullet selected = displayGun.preferredBullet != null ? displayGun.preferredBullet : display;
        if (display != null && !entry.compatibleBullets.contains(display)) {
            entry.compatibleBullets.add(display);
        }
        if (selected != null && !entry.compatibleBullets.contains(selected) && (entry.availableStacksByBullet.containsKey(selected) || displayGun.hasLoadedOrReloadingBullet(selected))) {
            entry.compatibleBullets.add(selected);
        }
        entry.compatibleBullets.sort(Comparator.comparing(ItemBullet::getItemName));

        ItemBullet fallbackDisplay = null;
        for (ItemBullet bullet : entry.compatibleBullets) {
            if (entry.availableStacksByBullet.containsKey(bullet)) {
                fallbackDisplay = bullet;
                break;
            }
        }
        entry.displayBullet = display != null ? display : fallbackDisplay;
        entry.selectedBullet = selected != null ? selected : entry.displayBullet;
        entry.fireModeText = displayGun.isSemiAutoFireMode() ? FIRE_MODE_SEMI_AUTO : FIRE_MODE_FULL_AUTO;
        entry.weaponIconType = getWeaponIconType(gunItem, entry.displayBullet);
        entry.ammoIconType = getAmmoIconType(entry.displayBullet);

        entries.add(entry);
    }

    private static HUDIconType getWeaponIconType(ItemPartGun gunItem, ItemBullet bullet) {
        JSONPart definition = gunItem.definition;
        if (definition.gun.hudIconType != null) {
            return definition.gun.hudIconType;
        }

        String type = definition.generic.type == null ? "" : definition.generic.type.toLowerCase(java.util.Locale.ROOT);
        if (containsAny(type, "bomb", "bomblet")) {
            return HUDIconType.BOMB;
        }
        if (containsAny(type, "missile", "atgm", "sam", "torpedo")) {
            return HUDIconType.MISSILE;
        }
        if (containsAny(type, "rocket", "rpod")) {
            return HUDIconType.ROCKET;
        }
        if (containsAny(type, "water", "foam", "extinguisher", "spray", "flame", "smoke", "flare", "confetti")) {
            return HUDIconType.UTILITY;
        }
        if (definition.gun.handHeld) {
            return HUDIconType.HANDHELD;
        }
        if (containsAny(type, "cannon", "artillery", "howitzer", "mortar") || definition.gun.diameter >= 20F) {
            return HUDIconType.CANNON;
        }
        if (bullet != null) {
            if (bullet.definition.bullet.turnRate > 0F) {
                return HUDIconType.MISSILE;
            }
            if (bullet.definition.bullet.burnTime > 0 || bullet.definition.bullet.accelerationTime > 0) {
                return HUDIconType.ROCKET;
            }
        }
        return HUDIconType.GENERIC;
    }

    private static AmmoIconType getAmmoIconType(ItemBullet bullet) {
        if (bullet == null || bullet.definition.bullet == null) {
            return AmmoIconType.STANDARD;
        }
        if (bullet.definition.bullet.isBlank) {
            return AmmoIconType.BLANK;
        }
        if (bullet.definition.bullet.turnRate > 0F) {
            return AmmoIconType.GUIDED;
        }
        if (bullet.definition.bullet.pellets > 1) {
            return AmmoIconType.PELLET;
        }
        if (bullet.definition.bullet.isHeat) {
            return AmmoIconType.HEAT;
        }
        if (bullet.definition.bullet.types != null) {
            if (bullet.definition.bullet.types.contains(BulletType.WATER)) {
                return AmmoIconType.WATER;
            }
            if (bullet.definition.bullet.types.contains(BulletType.EXPLOSIVE)) {
                return AmmoIconType.EXPLOSIVE;
            }
            if (bullet.definition.bullet.types.contains(BulletType.INCENDIARY)) {
                return AmmoIconType.INCENDIARY;
            }
            if (bullet.definition.bullet.types.contains(BulletType.ARMOR_PIERCING)) {
                return AmmoIconType.ARMOR_PIERCING;
            }
            if (bullet.definition.bullet.types.contains(BulletType.CUSTOM)) {
                return AmmoIconType.CUSTOM;
            }
        }
        return AmmoIconType.STANDARD;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static void tallyCompatible(Map<ItemBullet, Integer> counts, IInventoryProvider inv, PartGun gun) {
        JSONPart.JSONPartGun def = gun.definition.gun;
        for (int i = 0; i < inv.getSize(); ++i) {
            IWrapperItemStack stack = inv.getStack(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            AItemBase item = stack.getItem();
            if (!(item instanceof ItemBullet)) {
                continue;
            }
            ItemBullet bullet = (ItemBullet) item;
            if (!isCompatibleBullet(bullet, def)) {
                continue;
            }
            counts.merge(bullet, stack.getSize(), Integer::sum);
        }
    }

    private static boolean isCompatibleBullet(ItemBullet bullet, JSONPart.JSONPartGun def) {
        return bullet.definition.bullet != null
                && bullet.definition.bullet.diameter == def.diameter
                && bullet.definition.bullet.caseLength >= def.minCaseLength
                && bullet.definition.bullet.caseLength <= def.maxCaseLength;
    }

    @Override
    public void close() {
        super.close();
        if (current == this) {
            current = null;
        }
    }

    @Override
    protected boolean canStayOpen() {
        return super.canStayOpen() && !InterfaceManager.clientInterface.isGUIOpen() && !InterfaceManager.clientInterface.isChatOpen();
    }

    @Override
    public boolean capturesPlayer() {
        return false;
    }

    @Override
    public boolean renderTranslucent() {
        return true;
    }

    @Override
    protected String getTexture() {
        return "mts:textures/guis/weapon_selector.png";
    }

    @Override
    public int getWidth() {
        return HUD_WIDTH;
    }

    @Override
    public int getHeight() {
        return MAIN_PANEL_HEIGHT;
    }

    private GunGroupEntry getActiveGunEntry() {
        if (currentSeat != null && currentSeat.activeGunItem != null) {
            return findEntryForGunItem(currentSeat.activeGunItem);
        }
        for (GunGroupEntry entry : entries) {
            if (entry.isHandHeld) {
                return entry;
            }
        }
        return null;
    }

    private static class GunGroupEntry {
        ItemPartGun gunItem;
        List<PartGun> guns;
        String gunName;
        int loadedCount;
        int groupIndex;
        boolean isNoneSlot;
        boolean isHandHeld;
        boolean isFireSolo;
        int soloIndex;
        int soloCount;
        List<ItemBullet> compatibleBullets;
        Map<ItemBullet, Integer> availableStacksByBullet;
        ItemBullet displayBullet;
        ItemBullet selectedBullet;
        HUDIconType weaponIconType;
        AmmoIconType ammoIconType;
        String fireModeText;
    }

    private enum AmmoIconType {
        STANDARD(8, ""),
        ARMOR_PIERCING(9, "AP"),
        EXPLOSIVE(10, "HE"),
        HEAT(10, "HEAT"),
        INCENDIARY(11, "INC"),
        WATER(12, "H2O"),
        GUIDED(13, "GUIDED"),
        PELLET(14, "PELLET"),
        BLANK(15, "BLANK"),
        CUSTOM(15, "CUSTOM");

        private final int textureIndex;
        private final String label;

        AmmoIconType(int textureIndex, String label) {
            this.textureIndex = textureIndex;
            this.label = label;
        }
    }
}
