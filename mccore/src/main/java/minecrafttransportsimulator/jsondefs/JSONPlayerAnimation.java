package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

/**
 * Defines a vehicle-relative target for one of a player's limbs. Definitions
 * are evaluated in list order, with the first active definition winning.
 */
public class JSONPlayerAnimation {
    @JSONRequired
    @JSONDescription("The player limb controlled by this entry. Valid values are armLeft, armRight, legLeft, and legRight.")
    public PlayerLimb limb;

    @JSONDescription("The zero-based vehicle part-slot index or indices containing the seats this entry applies to. This may be either a single number or an array of numbers. Omit this for an exterior animation and specify range instead.")
    public List<Integer> seatSlot;

    @JSONRequired
    @JSONDescription("The rendering.animatedObjects entry whose animation matrix is applied before this target. The hand or foot follows that object exactly.")
    public String applyAfter;

    @JSONRequired
    @JSONDescription("The attachment coordinate in vehicle model space. For arms, this point is reached by the exact center of the hand's end face. The applyAfter object's animations are applied to this coordinate.")
    public Point3D pos;

    @JSONDescription("Optional local X/Y/Z scale applied to the controlled limb. Defaults to [1, 1, 1]. Scaling is anchored at the center of the hand or foot end face, so pos remains exact even when the limb's length is changed.")
    public Point3D limbScale;

    @JSONDescription("For an exterior animation, the maximum distance in blocks between the animated target and the center of the player's torso. Omit seatSlot and set this to a positive value. Only the targeted limb is overridden while the player remains in range.")
    public Double range;

    @JSONDescription("Normal MTS active animations controlling whether this target may be selected. Entries in playerAnimations are checked from first to last, so earlier active entries override later entries for the same limb and seat.")
    public List<JSONAnimationDefinition> activeAnimations;

    @JSONDescription("Transition time in ticks when this target is selected or released. If omitted, this defaults to 10. Set to 0 for an immediate transition.")
    public Integer duration;

    @JSONDescription("How many ticks the limb remains attached after this entry stops being active. Defaults to 0. For oneShot entries, this is the time spent resting on the target after the reach transition completes.")
    public Integer lingerDuration;

    @JSONDescription("If true, this entry triggers once when its activeAnimations change from inactive to active. It reaches the target over duration, remains there for lingerDuration, and then returns even if the controlling variable stays active. The trigger re-arms after activeAnimations become inactive.")
    public boolean oneShot;

    @JSONDescription("Only used with oneShot. If true, the one-shot fires whenever activeAnimations change in either direction, allowing one entry to handle both states of a toggle such as opening and closing a door. Initial loading does not count as a change.")
    public boolean triggerOnChange;

    @JSONDescription("If true, this entry is selected only for the player whose client-issued variable change, clickable action, GUI interaction, or custom keypress caused its activeAnimations to change. Direct variable references are matched exactly, while short-lived derived variables such as variable-modifier outputs, player_interacting, and clutch-shift pulses inherit the recent authenticated interaction on the same vehicle. Use this for shared controls such as a parking brake that either controller may operate but only the actual operator should reach for. Server-driven or automatic changes have no initiating player and will not play this entry. Defaults to false, which preserves the normal behavior where every applicable occupied seat may animate.")
    public boolean initiatingPlayerOnly;

    @JSONDescription("Optional ordered attachment overrides. The first active override replaces pos (and applyAfter when supplied), allowing a hand or foot to change grip based on additional variables.")
    public List<JSONPlayerGripOverride> gripOverrides;

    /** Returns true if this definition applies to the supplied vehicle seat slot. */
    public boolean appliesToSeat(int slot) {
        return seatSlot != null && seatSlot.contains(slot);
    }

    public static class JSONPlayerGripOverride {
        @JSONDescription("An optional replacement rendering.animatedObjects entry. If omitted, the parent player-animation applyAfter is used.")
        public String applyAfter;

        @JSONRequired
        @JSONDescription("The replacement attachment coordinate in vehicle model space.")
        public Point3D pos;

        @JSONDescription("Optional replacement local X/Y/Z limb scale. If omitted, the parent entry's limbScale is used. Scaling remains anchored at the hand or foot end-face center.")
        public Point3D limbScale;

        @JSONDescription("Normal MTS active animations controlling whether this grip override is selected. Overrides are checked from first to last.")
        public List<JSONAnimationDefinition> activeAnimations;

        @JSONDescription("Optional transition time for this grip change. If omitted, the parent entry's duration is used.")
        public Integer duration;

        @JSONDescription("Optional linger time for this grip. If omitted, the parent entry's lingerDuration is used.")
        public Integer lingerDuration;

        @JSONDescription("Optional one-shot setting for this grip. If omitted, the parent entry's oneShot setting is used.")
        public Boolean oneShot;

        @JSONDescription("Optional trigger-on-change setting for this grip. If omitted, the parent entry's triggerOnChange setting is used.")
        public Boolean triggerOnChange;
    }

    public enum PlayerLimb {
        armLeft,
        armRight,
        legLeft,
        legRight
    }
}
