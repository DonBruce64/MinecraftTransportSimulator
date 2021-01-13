package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONPotionEffect{
	@JSONRequired
	public String name;
	public int duration;
	public int amplifier;
}
