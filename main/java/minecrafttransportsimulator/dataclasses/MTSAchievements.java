package minecrafttransportsimulator.dataclasses;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

@Mod.EventBusSubscriber
public final class MTSAchievements{
	public static final Achievement rtfm = new Achievement("achievement.rtfm", "rtfm", 0, 0, MTSRegistry.manual, null);
	public static final Achievement engineAircraftSmall = new Achievement("achievement.engine_aircraft_small", "engine_aircraft_small", 2, 0, MTSRegistry.engineAircraftSmall, rtfm);
	public static final Achievement engineAircraftOverhaul = new Achievement("achievement.engine_aircraft_overhaul", "engine_aircraft_overhaul", 4, 0, Blocks.OBSIDIAN, engineAircraftSmall);
	public static final Achievement engineAircraftLarge = new Achievement("achievement.engine_aircraft_large", "engine_aircraft_large", 1, 2, MTSRegistry.engineAircraftLarge, engineAircraftSmall);
	public static final Achievement propellerBench = new Achievement("achievement.propeller_bench", "propeller_bench", 3, 2, MTSRegistry.itemBlockPropellerBench, engineAircraftSmall);
	public static final Achievement propeller = new Achievement("achievement.propeller", "propeller", 3, 4, MTSRegistry.propeller, propellerBench);
	public static final Achievement propellerTooBig = new Achievement("achievement.propeller_too_big", "propeller_too_big", 1, 4, new ItemStack(MTSRegistry.propeller, 1, 2), propeller);
	public static final Achievement propellerFits = new Achievement("achievement.propeller_fits", "propeller_fits", 5, 4, new ItemStack(MTSRegistry.propeller, 1, 1), propeller);
	public static final Achievement wheel = new Achievement("achievement.wheel", "wheel", 6, -1, MTSRegistry.wheelLarge, rtfm);
	public static final Achievement pontoon = new Achievement("achievement.pontoon", "pontoon", 8, -1, MTSRegistry.pontoon, wheel);
	public static final Achievement wheelPop = new Achievement("achievement.wheel_pop", "wheel_pop", 6, 1, Items.ARROW, wheel);
	public static final Achievement wrench = new Achievement("achievement.wrench", "wrench", 2, -2, MTSRegistry.wrench, rtfm);
	public static final Achievement instrument = new Achievement("achievement.instrument", "instrument", 4, -2, new ItemStack(MTSRegistry.instrument, 0, 1), wrench);
	public static final Achievement key = new Achievement("achievement.key", "key", 6, -3, MTSRegistry.key, rtfm);
	public static final Achievement fuelPump = new Achievement("achievement.fuel_pump", "fuel_pump", 2, -4, MTSRegistry.itemBlockFuelPump, rtfm);
	public static final Achievement fuel = new Achievement("achievement.fuel", "fuel", 4, -4, Items.LAVA_BUCKET, fuelPump);
	
	public static void init(){
		List<Achievement> achievements = new ArrayList<Achievement>();
		for(Field field : MTSAchievements.class.getFields()){
			if(field.getType().equals(Achievement.class)){
				try{
					Achievement achievement = (Achievement) field.get(Achievement.class);
					achievement.registerStat();
					achievements.add(achievement);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		AchievementPage.registerAchievementPage(new AchievementPage(MTS.MODID.toUpperCase(), achievements.toArray(new Achievement[achievements.size()])));
	}
	
	public void onCraftingEvent(PlayerEvent.ItemCraftedEvent event){
		if(event.crafting != null){
			if(event.crafting.getItem().equals(MTSRegistry.manual)){
				event.player.addStat(rtfm);
			}else if(event.crafting.getItem().equals(MTSRegistry.engineAircraftSmall)){
				for(byte slot=0; slot < event.craftMatrix.getSizeInventory(); ++slot){
					ItemStack stack = event.craftMatrix.getStackInSlot(slot);
					if(stack != null){
						if(MTSRegistry.engineAircraftSmall.equals(stack.getItem())){
							event.player.addStat(engineAircraftOverhaul);
						}
					}
				}
				event.player.addStat(engineAircraftSmall);
			}else if(event.crafting.getItem().equals(MTSRegistry.engineAircraftLarge)){
				for(byte slot=0; slot < event.craftMatrix.getSizeInventory(); ++slot){
					ItemStack stack = event.craftMatrix.getStackInSlot(slot);
					if(stack != null){
						if(MTSRegistry.engineAircraftLarge.equals(stack.getItem())){
							event.player.addStat(engineAircraftOverhaul);
						}
					}
				}
				event.player.addStat(engineAircraftLarge);
			}else if(event.crafting.getItem().equals(MTSRegistry.itemBlockPropellerBench)){
				event.player.addStat(propellerBench);
			}else if(event.crafting.getItem().equals(MTSRegistry.wheelSmall) || event.crafting.getItem().equals(MTSRegistry.wheelLarge)){
				event.player.addStat(wheel);
			}else if(event.crafting.getItem().equals(MTSRegistry.pontoon)){
				event.player.addStat(pontoon);
			}else if(event.crafting.getItem().equals(MTSRegistry.wrench)){
				event.player.addStat(wrench);
			}else if(event.crafting.getItem().equals(MTSRegistry.itemBlockFuelPump)){
				event.player.addStat(fuelPump);
			}
		}
	}
}
