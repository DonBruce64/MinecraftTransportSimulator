package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.packloading.JSONParser.JSONDefaults;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONPotionEffect {
    @JSONRequired
    @JSONDefaults(PotionDefaults.class)
    @JSONDescription("This should match one of the potion names provided by the base game (see the Minecraft Wiki). Note that many of these effects will be useless while riding in a vehicle. Also note that modded potions may be applied, provided they use the standard position registration system.  Just make sure that the mod is present before you apply these.  Pack-based activators on your packloader file are HIGHLY recommended if you decide to mess with modded potions.")
    public String name;

    @JSONRequired
    @JSONDescription("How long the effect will last, in ticks. For most effects, this just needs to be high enough that the effect doesn't wear out before the system has a chance to reapply it, meaning a value of 5 should certainly do the trick. Some effects (like night_vision), however, behave differently once below a certain value (200 ticks), so in those cases, you may want the value to be higher. Regardless of this value, riders will not keep their effects after leaving the seat or vehicle that is applying these effects.")
    public int duration;

    @JSONRequired
    @JSONDescription("Impacts the intensity of the effect (must be an integer between 0 and 255). Has no impact on many potion effects, so for those, it can be left out or set to zero. Note that Minecraft adds 1 to this value, so a strength effect with an amplifier of 1 will appear as \"Strength II\".")
    public int amplifier;

    public enum PotionDefaults {
        SPEED,
        SLOWNESS,
        HASTE,
        MINING_FATIGUE,
        STRENGTH,
        INSTANT_HEALTH,
        INSTANT_DAMAGE,
        JUMP_BOOST,
        NAUSEA,
        REGENERATION,
        RESISTANCE,
        FIRE_RESISTANCE,
        WATER_BREATHING,
        INVISIBILITY,
        BLINDNESS,
        NIGHT_VISION,
        HUNGER,
        WEAKNESS,
        POISON,
        WITHER,
        HEALTH_BOOST,
        ABSORPTION,
        SATURATION,
        GLOWING,
        LEVITATION,
        LUCK,
        UNLUCK
    }
}
