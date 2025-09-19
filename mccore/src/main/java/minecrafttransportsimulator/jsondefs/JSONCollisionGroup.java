package minecrafttransportsimulator.jsondefs;

import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONCollisionGroup {

    @JSONDescription("How much armor this group has.  Values greater than 0 will make this group use armor code to block bullets from passing through it.  Leaving this value out will make all bullets pass through it (no armor).")
    public float armorThickness;

    @JSONDescription("Like normal armor thickness, but used only if a bullet is a HEAT bullet.")
    public float heatArmorThickness;

    @JSONDescription("How much to multiply the damage for bullets that hit this group by.  Is 1.0 if not specified.")
    public float damageMultiplier;

    @JSONDescription("The multiplier for forwarded damage.  If set, this collision group will forward damage onto the thing they are a part of when hit.  This will also cause any bullets to stop when they hit this collision box.")
    public float forwardsDamageMultiplier;

    @JSONDescription("How much damage to apply to MC entities that fall into this hitboxes area.  Damage will be applied constantly to the entity, though the internal damage cooldown will ensure the entity is not damaged every tick.")
    public float externalEntityDamage;

    @JSONDescription("How much damage to apply to MTS entities that fall into this hitboxes area.  Damage will be applied constantly to the entity, so ensure that this hitbox does not remain active once damaging conditions have been satisfied or health will rapidly deplete.")
    public float internalEntityDamage;

    @JSONDescription("How much health this collision group has.  When the health reaches 0, this collision group will be disabled.  Useful for armor that can only take so many hits, or parts of models that you want to break off when hit.  If this value is 0, then the collision group will always be present, and damage inflicted will be charged to the entity rather than this collision group.")
    public int health;

    @JSONDescription("If this is set, then this collision group will first use the animations for this object from the rendering section instead of the animations in this one.  If the specified object has applyAfter on it itself, then the animations will be gotten recursively until an applyAfter is not found.")
    public String applyAfter;

    @JSONRequired
    @JSONDescription("The types of collision for this group.")
    public Set<CollisionType> collisionTypes;

    @JSONRequired
    @JSONDescription("A listing of collisions for this group.")
    public List<JSONCollisionBox> collisions;

    @JSONDescription("A optional listing of animations to use to modify the collision boxes.  Translation/rotation animations do what you would expect.  Visibiity animations will completely disable the hitbox if they are false.")
    public List<JSONAnimationDefinition> animations;

    public static enum CollisionType {
        @JSONDescription("Allows collision with blocks.")
        BLOCK,
        @JSONDescription("Allows entities to collide with this box.")
        ENTITY,
        @JSONDescription("Allows vehicle wheels to ride on boxes in this group.")
        VEHICLE,
        @JSONDescription("Allows attacking from damage sources.  This includes bullets.")
        ATTACK,
        @JSONDescription("Allows bullet interaction, but not general attacks.")
        BULLET,
        @JSONDescription("Allows clicking.")
        CLICK,
        @JSONDescription("Allows effector logic.")
        EFFECTOR;
    }

    @Deprecated
    public boolean isInterior;
    @Deprecated
    public boolean isForBullets;
}
