package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

@JSONDescription("Settings for camera shake from a gunshot or bullet explosion.")
public class JSONCameraShake {
    @JSONDescription("The shake strength before the server multiplier and distance attenuation. If omitted, the gun or bullet calculates its usual strength. Set to 0 to disable this shake. The initial pitch amplitude in degrees is strength * server factor * 0.1 / max(1, distance in blocks), capped at 10 degrees.")
    public Double strength;

    @JSONDescription("The maximum distance, in blocks, at which the shake can be felt. Defaults to 128 when omitted or non-positive. Strength still decreases with distance, and very weak shakes are skipped.")
    public double radius;

    @JSONDescription("The duration of the shake in ticks (20 ticks = 1 second), measured in real time. Defaults to 10 ticks (0.5 seconds) when omitted or non-positive.")
    public int duration;

    @JSONDescription("The speed multiplier for camera oscillations. 1 is normal speed, 2 is twice as fast, and 0.5 is half as fast. Defaults to 1 when omitted or non-positive. Does not change shake strength or duration.")
    public double intensity;
}
