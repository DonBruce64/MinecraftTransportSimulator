package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.IInventoryProvider;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartGun;
import minecrafttransportsimulator.packets.instances.PacketPartSeat;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;

/**
 * Always-visible bottom-right HUD that lists every gun-group the seated player can control.
 * Each group is drawn as its own semi-transparent rectangle stacked upward from the screen
 * bottom.  Number keys 1-9 set the seat's active gun to the matching cell, and the
 * ammo-select key cycles the preferred bullet of the currently-active gun through the
 * compatible types available in the player inventory and connected crates.  When the seat
 * permits disabling the gun, an extra "None" cell appears at the top of the stack.
 *
 * @author don_bruce
 */
public class GUIAmmoSelector extends AGUIBase {
    public static final int MAX_GUNS = 9;

    private static final int CELL_WIDTH = 150;
    private static final int CELL_HEIGHT = 30;
    private static final int CELL_GAP = 4;
    private static final int EDGE_PADDING = 6;
    private static final int CELL_INNER_PAD = 4;
    private static final int ICON_SIZE = 16;
    private static final int RELOAD_BAR_HEIGHT = 2;
    private static final float BACKDROP_ALPHA = 0.5F;
    private static final ColorRGB INACTIVE_COLOR = ColorRGB.WHITE;

    public static GUIAmmoSelector current;

    private final IWrapperPlayer player;
    private final List<GunGroupEntry> entries = new ArrayList<>();
    private final int[] selectionKeyCodes = new int[10];
    private final boolean[] selectionKeyPressedLast = new boolean[10];

    private final List<GUIComponentCutout> cellBackdrops = new ArrayList<>();
    private final List<GUIComponentItem> cellIcons = new ArrayList<>();
    private final List<GUIComponentLabel> cellTitles = new ArrayList<>();
    private final List<GUIComponentLabel> cellBulletNames = new ArrayList<>();
    private final List<GUIComponentLabel> cellCounts = new ArrayList<>();
    private final List<GUIComponentCutout> cellReloadBars = new ArrayList<>();
    private final List<GUIComponentCutout> cellBorders = new ArrayList<>();

    private PartSeat currentSeat;

    public GUIAmmoSelector(IWrapperPlayer player) {
        super();
        this.player = player;
        for (int i = 0; i < 10; ++i) {
            selectionKeyCodes[i] = InterfaceManager.inputInterface.getKeyCodeForName(String.valueOf(i));
        }
        current = this;
    }

    @Override
    public void setupComponentsInit(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        //Bottom-right anchor: the cell stack grows upward from near the bottom-right corner.
        this.guiLeft = screenWidth - CELL_WIDTH - EDGE_PADDING;
        this.guiTop = screenHeight - EDGE_PADDING - CELL_HEIGHT;
        setupComponents();
    }

    @Override
    public void setupComponents() {
        components.clear();
        cellBackdrops.clear();
        cellIcons.clear();
        cellTitles.clear();
        cellBulletNames.clear();
        cellCounts.clear();
        cellReloadBars.clear();
        cellBorders.clear();

        for (int i = 0; i < MAX_GUNS; ++i) {
            //Stack cells bottom-up; cell 0 is at the bottom, higher indices further up.
            int cellY = guiTop - i * (CELL_HEIGHT + CELL_GAP);

            GUIComponentCutout backdrop = new GUIComponentCutout(this, guiLeft, cellY, CELL_WIDTH, CELL_HEIGHT,
                    STANDARD_COLOR_WIDTH_OFFSET, STANDARD_BLACK_HEIGHT_OFFSET, STANDARD_COLOR_WIDTH, STANDARD_COLOR_HEIGHT) {
                {
                    renderable.setAlpha(BACKDROP_ALPHA);
                    if (renderableL != null) {
                        renderableL.setAlpha(BACKDROP_ALPHA);
                    }
                }
            };
            backdrop.ignoreGUILightingState = true;
            addComponent(backdrop);
            cellBackdrops.add(backdrop);

            addBorder(guiLeft, cellY - 1, CELL_WIDTH, 1);
            addBorder(guiLeft, cellY + CELL_HEIGHT, CELL_WIDTH, 1);
            addBorder(guiLeft - 1, cellY - 1, 1, CELL_HEIGHT + 2);
            addBorder(guiLeft + CELL_WIDTH, cellY - 1, 1, CELL_HEIGHT + 2);

            GUIComponentLabel title = new GUIComponentLabel(guiLeft + CELL_INNER_PAD, cellY + 3,
                    INACTIVE_COLOR, "", TextAlignment.LEFT_ALIGNED, 0.75F);
            title.ignoreGUILightingState = true;
            addComponent(title);
            cellTitles.add(title);

            GUIComponentItem icon = new GUIComponentItem(guiLeft + CELL_INNER_PAD, cellY + 10, 1.0F);
            addComponent(icon);
            cellIcons.add(icon);

            GUIComponentLabel bulletName = new GUIComponentLabel(guiLeft + CELL_INNER_PAD + ICON_SIZE + 3, cellY + 16,
                    INACTIVE_COLOR, "", TextAlignment.LEFT_ALIGNED, 0.625F);
            bulletName.ignoreGUILightingState = true;
            addComponent(bulletName);
            cellBulletNames.add(bulletName);

            GUIComponentLabel count = new GUIComponentLabel(guiLeft + CELL_WIDTH - CELL_INNER_PAD, cellY + 14,
                    INACTIVE_COLOR, "", TextAlignment.RIGHT_ALIGNED, 0.625F);
            count.ignoreGUILightingState = true;
            addComponent(count);
            cellCounts.add(count);

            //Reload progress bar: pulls from the white swatch so the tint isn't darkened,
            //sits flush against the bottom edge of the cell, and starts hidden.
            GUIComponentCutout reloadBar = new GUIComponentCutout(this, guiLeft, cellY + CELL_HEIGHT - RELOAD_BAR_HEIGHT,
                    CELL_WIDTH, RELOAD_BAR_HEIGHT, STANDARD_WHITE_WIDTH_OFFSET, STANDARD_WHITE_HEIGHT_OFFSET,
                    STANDARD_COLOR_WIDTH, STANDARD_COLOR_HEIGHT) {
                {
                    renderable.setTransucentOverride();
                    if (renderableL != null) {
                        renderableL.setTransucentOverride();
                    }
                }
            };
            reloadBar.ignoreGUILightingState = true;
            reloadBar.visible = false;
            addComponent(reloadBar);
            cellReloadBars.add(reloadBar);
        }
    }

    private void addBorder(int x, int y, int width, int height) {
        GUIComponentCutout border = new GUIComponentCutout(this, x, y, width, height,
                STANDARD_WHITE_WIDTH_OFFSET, STANDARD_WHITE_HEIGHT_OFFSET, STANDARD_COLOR_WIDTH, STANDARD_COLOR_HEIGHT) {
            {
                renderable.setTransucentOverride();
                if (renderableL != null) {
                    renderableL.setTransucentOverride();
                }
            }
        };
        border.ignoreGUILightingState = true;
        border.visible = false;
        addComponent(border);
        cellBorders.add(border);
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

        ItemPartGun activeGunItem = currentSeat != null ? currentSeat.activeGunItem : null;

        for (int i = 0; i < MAX_GUNS; ++i) {
            GUIComponentCutout backdrop = cellBackdrops.get(i);
            GUIComponentLabel title = cellTitles.get(i);
            GUIComponentItem icon = cellIcons.get(i);
            GUIComponentLabel bulletName = cellBulletNames.get(i);
            GUIComponentLabel count = cellCounts.get(i);
            GUIComponentCutout reloadBar = cellReloadBars.get(i);

            if (i < entries.size()) {
                GunGroupEntry entry = entries.get(i);
                boolean active;
                if (entry.isNoneSlot) {
                    active = activeGunItem == null;
                } else {
                    active = activeGunItem != null && activeGunItem.equals(entry.gunItem);
                }

                backdrop.visible = true;
                title.visible = true;
                bulletName.visible = true;
                count.visible = true;
                setBorderVisible(i, active);

                title.color = INACTIVE_COLOR;
                bulletName.color = INACTIVE_COLOR;
                count.color = INACTIVE_COLOR;

                title.text = (i + 1) + ". " + entry.gunName;

                if (entry.isNoneSlot) {
                    icon.visible = false;
                    icon.stack = null;
                    bulletName.text = "";
                    count.text = "";
                    reloadBar.visible = false;
                } else {
                    icon.visible = true;
                    if (entry.displayBullet != null) {
                        icon.stack = entry.displayIconStack;
                        bulletName.text = entry.displayBullet.getItemName();
                        int totalRounds = entry.loadedCount;
                        for (Map.Entry<ItemBullet, Integer> stackEntry : entry.availableStacksByBullet.entrySet()) {
                            int perItem = Math.max(1, stackEntry.getKey().definition.bullet.quantity);
                            totalRounds += stackEntry.getValue() * perItem;
                        }
                        count.text = entry.loadedCount + "/" + totalRounds;
                    } else {
                        icon.stack = null;
                        bulletName.text = entry.compatibleBullets.isEmpty() ? "No ammo" : "Not loaded";
                        count.text = "";
                    }

                    float progress = 0F;
                    for (PartGun gun : entry.guns) {
                        float g = gun.getReloadProgress();
                        if (g > progress) {
                            progress = g;
                        }
                    }
                    if (progress > 0F) {
                        reloadBar.visible = true;
                        reloadBar.width = Math.max(1, Math.round(CELL_WIDTH * progress));
                    } else {
                        reloadBar.visible = false;
                    }
                }
            } else {
                backdrop.visible = false;
                title.visible = false;
                icon.visible = false;
                bulletName.visible = false;
                count.visible = false;
                reloadBar.visible = false;
                setBorderVisible(i, false);
                icon.stack = null;
                title.text = "";
                bulletName.text = "";
                count.text = "";
            }
        }
    }

    private void setBorderVisible(int cellIndex, boolean visible) {
        int base = cellIndex * 4;
        for (int j = 0; j < 4; ++j) {
            cellBorders.get(base + j).visible = visible;
        }
    }

    @Override
    protected boolean renderBackground() {
        return false;
    }

    /**
     * Polled each tick from ControlSystem while this HUD is visible.
     * Edge-triggered keys 1-9 select the corresponding gun group.  Separately,
     * the caller feeds the ammo-cycle trigger through {@link #cycleActiveGunAmmo()}.
     */
    public void pollSelectionKeys() {
        for (int i = 1; i <= 9; ++i) {
            int code = selectionKeyCodes[i];
            if (code <= 0) {
                continue;
            }
            boolean pressed = InterfaceManager.inputInterface.isKeyPressed(code);
            if (pressed && !selectionKeyPressedLast[i]) {
                selectGun(i - 1);
            }
            selectionKeyPressedLast[i] = pressed;
        }
    }

    /**
     * Cycles the preferred bullet of the currently-active gun group to the next compatible type.
     * Invoked from ControlSystem when the ammo-select keybind fires.
     */
    public void cycleActiveGunAmmo() {
        if (currentSeat == null || currentSeat.activeGunItem == null) {
            return;
        }
        GunGroupEntry entry = findEntryForGunItem(currentSeat.activeGunItem);
        if (entry == null || entry.compatibleBullets.size() < 2) {
            return;
        }
        int currentIndex = entry.displayBullet == null ? -1 : entry.compatibleBullets.indexOf(entry.displayBullet);
        ItemBullet next = entry.compatibleBullets.get((currentIndex + 1) % entry.compatibleBullets.size());
        for (PartGun gun : entry.guns) {
            InterfaceManager.packetInterface.sendToServer(new PacketPartGun(gun, next));
        }
    }

    private void selectGun(int slot) {
        if (currentSeat == null || slot < 0 || slot >= entries.size()) {
            return;
        }
        GunGroupEntry entry = entries.get(slot);
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

        AEntityB_Existing riding = player.getEntityRiding();
        if (!(riding instanceof PartSeat)) {
            return;
        }
        currentSeat = (PartSeat) riding;

        IWrapperInventory playerInv = player.getInventory();

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

            GunGroupEntry entry = new GunGroupEntry();
            entry.gunItem = groupEntry.getKey();
            entry.guns = groupGuns;
            entry.groupIndex = groupIndex;
            entry.gunName = entry.gunItem != null ? entry.gunItem.getItemName() : groupGuns.get(0).definition.general.name;

            int totalLoaded = 0;
            for (PartGun gun : groupGuns) {
                totalLoaded += gun.getLoadedBulletCount();
            }
            entry.loadedCount = totalLoaded;

            Map<ItemBullet, Integer> stackCounts = new LinkedHashMap<>();
            PartGun firstGun = groupGuns.get(0);
            tallyCompatible(stackCounts, playerInv, firstGun);
            for (PartGun gun : groupGuns) {
                for (PartInteractable crate : gun.connectedCrates) {
                    if (crate.isActiveVar.isActive && crate.inventory != null) {
                        tallyCompatible(stackCounts, crate.inventory, firstGun);
                    }
                }
            }
            entry.availableStacksByBullet = stackCounts;
            entry.compatibleBullets = new ArrayList<>(stackCounts.keySet());

            ItemBullet active = firstGun.preferredBullet != null ? firstGun.preferredBullet : firstGun.lastLoadedBullet;
            if (active != null && !entry.compatibleBullets.contains(active)) {
                entry.compatibleBullets.add(active);
            }
            entry.compatibleBullets.sort(Comparator.comparing(ItemBullet::getItemName));

            entry.displayBullet = active != null ? active : (entry.compatibleBullets.isEmpty() ? null : entry.compatibleBullets.get(0));
            entry.displayIconStack = entry.displayBullet != null ? entry.displayBullet.getNewStack(null) : null;

            entries.add(entry);
            ++groupIndex;
            if (entries.size() >= MAX_GUNS) {
                break;
            }
        }

        //Append the "None" slot at the top of the stack when the seat permits disabling the gun.
        if (currentSeat.placementDefinition.canDisableGun && entries.size() < MAX_GUNS) {
            GunGroupEntry noneEntry = new GunGroupEntry();
            noneEntry.isNoneSlot = true;
            noneEntry.gunName = "None";
            noneEntry.guns = new ArrayList<>();
            noneEntry.compatibleBullets = new ArrayList<>();
            noneEntry.availableStacksByBullet = new LinkedHashMap<>();
            entries.add(noneEntry);
        }
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
            if (bullet.definition.bullet == null) {
                continue;
            }
            if (bullet.definition.bullet.diameter != def.diameter
                    || bullet.definition.bullet.caseLength < def.minCaseLength
                    || bullet.definition.bullet.caseLength > def.maxCaseLength) {
                continue;
            }
            counts.merge(bullet, stack.getSize(), Integer::sum);
        }
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
        return super.canStayOpen();
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
    public int getWidth() {
        return CELL_WIDTH;
    }

    @Override
    public int getHeight() {
        return CELL_HEIGHT;
    }

    private static class GunGroupEntry {
        ItemPartGun gunItem;
        List<PartGun> guns;
        String gunName;
        int loadedCount;
        int groupIndex;
        boolean isNoneSlot;
        List<ItemBullet> compatibleBullets;
        Map<ItemBullet, Integer> availableStacksByBullet;
        ItemBullet displayBullet;
        IWrapperItemStack displayIconStack;
    }
}
