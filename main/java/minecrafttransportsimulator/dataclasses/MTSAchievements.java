package minecrafttransportsimulator.dataclasses;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

@Mod.EventBusSubscriber
public final class MTSAchievements{
	private static final MTSAchievement rtfm = new MTSAchievement("achievement.rtfm", "rtfm", 0, 0, MTSRegistry.manual, null);
	private static final MTSAchievement engineAircraftSmall = new MTSAchievement("achievement.engine_aircraft_small", "engine_aircraft_small", 2, 0, MTSRegistry.engineLycoming0360, rtfm);
	private static final MTSAchievement engineAircraftOverhaul = new MTSAchievement("achievement.engine_aircraft_overhaul", "engine_aircraft_overhaul", 4, 0, new ItemStack(Blocks.OBSIDIAN), engineAircraftSmall);
	private static final MTSAchievement engineAircraftLarge = new MTSAchievement("achievement.engine_aircraft_large", "engine_aircraft_large", 1, 2, MTSRegistry.engineBristolMercury, engineAircraftSmall);
	private static final MTSAchievement propellerBench = new MTSAchievement("achievement.propeller_bench", "propeller_bench", 3, 2, MTSRegistry.itemBlockPropellerBench, engineAircraftSmall);
	private static final MTSAchievement propeller = new MTSAchievement("achievement.propeller", "propeller", 3, 4, MTSRegistry.propeller, propellerBench);
	private static final MTSAchievement propellerTooBig = new MTSAchievement("achievement.propeller_too_big", "propeller_too_big", 1, 4, new ItemStack(MTSRegistry.propeller, 1, 2), propeller);
	private static final MTSAchievement propellerFits = new MTSAchievement("achievement.propeller_fits", "propeller_fits", 5, 4, new ItemStack(MTSRegistry.propeller, 1, 1), propeller);
	private static final MTSAchievement wheel = new MTSAchievement("achievement.wheel", "wheel", 6, -1, MTSRegistry.wheelLarge, rtfm);
	private static final MTSAchievement pontoon = new MTSAchievement("achievement.pontoon", "pontoon", 8, -1, MTSRegistry.pontoon, wheel);
	private static final MTSAchievement wheelPop = new MTSAchievement("achievement.wheel_pop", "wheel_pop", 6, 1, Items.ARROW, wheel);
	private static final MTSAchievement wrench = new MTSAchievement("achievement.wrench", "wrench", 2, -2, MTSRegistry.wrench, rtfm);
	private static final MTSAchievement instrument = new MTSAchievement("achievement.instrument", "instrument", 4, -2, new ItemStack(MTSRegistry.instrument, 0, 1), wrench);
	private static final MTSAchievement key = new MTSAchievement("achievement.key", "key", 6, -3, MTSRegistry.key, rtfm);
	private static final MTSAchievement fuelPump = new MTSAchievement("achievement.fuel_pump", "fuel_pump", 2, -4, MTSRegistry.itemBlockFuelPump, rtfm);
	private static final MTSAchievement fuel = new MTSAchievement("achievement.fuel", "fuel", 4, -4, Items.LAVA_BUCKET, fuelPump);
	
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
	
	private static class MTSAchievement extends Achievement{		
		public MTSAchievement(String statIdIn, String unlocalizedName, int column, int row, ItemStack stackIn, Achievement parent){
			super(statIdIn, unlocalizedName, column, row, stackIn, parent);
		}
		
		public MTSAchievement(String statIdIn, String unlocalizedName, int column, int row, Item itemIn, Achievement parent){
			this(statIdIn, unlocalizedName, column, row, new ItemStack(itemIn), parent);
		}
		
		public void trigger(EntityPlayer player){
			player.addStat(this);
		}
	}
		
	public static void triggerPropeller(EntityPlayer player){propeller.trigger(player);}
	public static void triggerPropellerTooBig(EntityPlayer player){propellerTooBig.trigger(player);}
	public static void triggerPropellerFits(EntityPlayer player){propellerFits.trigger(player);}
	public static void triggerWheelPop(EntityPlayer player){wheelPop.trigger(player);}
	public static void triggerInstrument(EntityPlayer player){instrument.trigger(player);}
	public static void triggerKey(EntityPlayer player){key.trigger(player);}
	public static void triggerFuel(EntityPlayer player){fuel.trigger(player);}
	
	public void onCraftingEvent(PlayerEvent.ItemCraftedEvent event){
		if(event.crafting != null){
			if(event.crafting.getItem().equals(MTSRegistry.manual)){
				rtfm.trigger(event.player);
			}else if(event.crafting.getItem().equals(MTSRegistry.engineLycoming0360)){
				for(byte slot=0; slot < event.craftMatrix.getSizeInventory(); ++slot){
					ItemStack stack = event.craftMatrix.getStackInSlot(slot);
					if(stack != null){
						if(MTSRegistry.engineLycoming0360.equals(stack.getItem())){
							engineAircraftOverhaul.trigger(event.player);
						}
					}
				}
				engineAircraftSmall.trigger(event.player);
			}else if(event.crafting.getItem().equals(MTSRegistry.engineBristolMercury)){
				for(byte slot=0; slot < event.craftMatrix.getSizeInventory(); ++slot){
					ItemStack stack = event.craftMatrix.getStackInSlot(slot);
					if(stack != null){
						if(MTSRegistry.engineBristolMercury.equals(stack.getItem())){
							engineAircraftOverhaul.trigger(event.player);
						}
					}
				}
				engineAircraftLarge.trigger(event.player);
			}else if(event.crafting.getItem().equals(MTSRegistry.itemBlockPropellerBench)){
				propellerBench.trigger(event.player);
			}else if(event.crafting.getItem().equals(MTSRegistry.wheelSmall) || event.crafting.getItem().equals(MTSRegistry.wheelLarge)){
				wheel.trigger(event.player);
			}else if(event.crafting.getItem().equals(MTSRegistry.pontoon)){
				pontoon.trigger(event.player);
			}else if(event.crafting.getItem().equals(MTSRegistry.wrench)){
				wrench.trigger(event.player);
			}else if(event.crafting.getItem().equals(MTSRegistry.itemBlockFuelPump)){
				fuelPump.trigger(event.player);
			}
		}
	}
}
