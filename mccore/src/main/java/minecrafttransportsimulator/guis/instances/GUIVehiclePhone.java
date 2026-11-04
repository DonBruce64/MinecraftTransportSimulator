package minecrafttransportsimulator.guis.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.jsondefs.JSONItem.JSONPhone;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketVehiclePhoneAction;
import minecrafttransportsimulator.packets.instances.PacketVehiclePhoneAction.PhoneAction;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.summoning.VehiclePhone;
import minecrafttransportsimulator.systems.summoning.VehiclePhone.PhoneEntry;

/**
 * GUI for vehicle phones.  Lists the vehicles registered on the phone, with buttons to call one,
 * stop a call in progress, or remove it from the phone.  The texture, size, layout and colors
 * come from the phone item's {@link JSONPhone} section.
 */
public class GUIVehiclePhone extends AGUIBase {
    private static final int ROW_HEIGHT = 18;

    private final ItemItem item;
    private final JSONPhone phone;
    private final IWrapperPlayer player;
    private final List<PhoneEntry> entries;
    private GUIComponentLabel[] distanceLabels;

    public GUIVehiclePhone(ItemItem item, IWrapperPlayer player) {
        super();
        this.item = item;
        this.phone = item.definition.phone;
        this.player = player;
        this.entries = VehiclePhone.getEntries(player.getHeldStack());
    }

    @Override
    public void setupComponents() {
        super.setupComponents();
        int listLeft = guiLeft + getListX();
        int listTop = guiTop + getListY();
        int listWidth = phone.listWidth > 0 ? phone.listWidth : getWidth() - 2 * getListX();
        int listRight = listLeft + listWidth;
        addComponent(new GUIComponentLabel(guiLeft + getWidth() / 2, guiTop + 10, getColor(phone.titleColor, ColorRGB.BLACK), phone.title != null ? phone.title : item.getItemName(), TextAlignment.CENTERED, 1.5F));
        if (entries.isEmpty()) {
            addComponent(new GUIComponentLabel(listLeft + 3, listTop, getColor(phone.textColor, ColorRGB.BLACK), LanguageSystem.GUI_PHONE_EMPTY.getCurrentValue(), TextAlignment.LEFT_ALIGNED, 1.0F, listWidth - 6));
            distanceLabels = new GUIComponentLabel[0];
            return;
        }

        //Buttons are lined up on the right of the list, with the distance just before them.
        int distanceX = listRight - 129;
        distanceLabels = new GUIComponentLabel[entries.size()];
        for (int i = 0; i < entries.size(); ++i) {
            PhoneEntry entry = entries.get(i);
            int rowY = listTop + i * ROW_HEIGHT;
            addComponent(new GUIComponentLabel(listLeft, rowY + 5, getColor(phone.textColor, ColorRGB.BLACK), entry.name, TextAlignment.LEFT_ALIGNED, 1.0F, Math.max(0, distanceX - 3 - listLeft)));
            addComponent(distanceLabels[i] = new GUIComponentLabel(distanceX, rowY + 5, getColor(phone.hintColor, ColorRGB.DARK_GRAY), "", TextAlignment.LEFT_ALIGNED, 1.0F));
            addComponent(new PhoneButton(listRight - 94, rowY, 36, LanguageSystem.GUI_PHONE_CALL.getCurrentValue(), PhoneAction.SUMMON, entry));
            addComponent(new PhoneButton(listRight - 56, rowY, 36, LanguageSystem.GUI_PHONE_STOP.getCurrentValue(), PhoneAction.STOP, entry));
            addComponent(new PhoneButton(listRight - 18, rowY, 18, "X", PhoneAction.REMOVE, entry));
        }
        addComponent(new GUIComponentLabel(listLeft, guiTop + getHeight() - 16, getColor(phone.hintColor, ColorRGB.DARK_GRAY), LanguageSystem.GUI_PHONE_HINT.getCurrentValue(), TextAlignment.LEFT_ALIGNED, 0.75F));
    }

    @Override
    public void setStates() {
        super.setStates();
        //Show the distance to vehicles this client has loaded.  Others are too far away to know.
        for (int i = 0; i < distanceLabels.length; ++i) {
            EntityVehicleF_Physics vehicle = player.getWorld().getEntity(entries.get(i).vehicleID);
            distanceLabels[i].text = vehicle != null ? (int) vehicle.position.distanceTo(player.getPosition()) + "m" : LanguageSystem.GUI_PHONE_FAR.getCurrentValue();
        }
    }

    @Override
    public int getWidth() {
        return phone.width > 0 ? phone.width : super.getWidth();
    }

    @Override
    public int getHeight() {
        return phone.height > 0 ? phone.height : super.getHeight();
    }

    @Override
    protected String getTexture() {
        return phone.texture != null ? phone.texture : super.getTexture();
    }

    private int getListX() {
        return phone.listX > 0 ? phone.listX : 12;
    }

    private int getListY() {
        return phone.listY > 0 ? phone.listY : 34;
    }

    private static ColorRGB getColor(ColorRGB color, ColorRGB defaultColor) {
        return color != null ? color : defaultColor;
    }

    private class PhoneButton extends GUIComponentButton {
        private final PhoneAction action;
        private final PhoneEntry entry;

        private PhoneButton(int x, int y, int width, String text, PhoneAction action, PhoneEntry entry) {
            super(GUIVehiclePhone.this, x, y, width, ROW_HEIGHT - 2, text, true, getColor(phone.buttonTextColor, ColorRGB.DARK_GRAY), true);
            this.action = action;
            this.entry = entry;
        }

        @Override
        public void onClicked(boolean leftSide) {
            InterfaceManager.packetInterface.sendToServer(new PacketVehiclePhoneAction(player, action, entry.vehicleID));
            if (action != PhoneAction.STOP) {
                close();
            }
        }
    }
}
