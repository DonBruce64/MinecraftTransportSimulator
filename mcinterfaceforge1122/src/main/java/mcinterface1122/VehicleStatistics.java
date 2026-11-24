package mcinterface1122;

import java.util.EnumMap;
import java.util.Map;

import minecrafttransportsimulator.systems.VehicleStat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.IStatType;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatBasic;
import net.minecraft.stats.StatList;
import net.minecraft.util.text.TextComponentTranslation;

final class VehicleStatistics {
    private static final Map<VehicleStat, StatBase> STATS = new EnumMap<>(VehicleStat.class);

    static void initialize() {
        for (VehicleStat stat : VehicleStat.values()) {
            String id = "stat." + InterfaceLoader.MODID + "." + stat.id;
            STATS.put(stat, new StatBasic(id, new TextComponentTranslation(id), formatter(stat)).initIndependentStat().registerStat());
        }
    }

    private static IStatType formatter(VehicleStat stat) {
        switch (stat) {
            case DISTANCE_DRIVEN:
            case DISTANCE_AS_PASSENGER:
            case DISTANCE_FLOWN:
                return StatBase.distanceStatType;
            case FUEL_BURNED:
                return VehicleStat::formatFuel;
            case ENGINE_HOURS:
                return VehicleStat::formatEngineHours;
            default:
                return StatBase.simpleStatType;
        }
    }

    static void award(EntityPlayer player, VehicleStat stat, double amount) {
        if (player instanceof EntityPlayerMP && Double.isFinite(amount) && amount > 0) {
            int units = (int) amount;
            if (stat.usesFractionalUnits) {
                NBTTagCompound data = player.getEntityData();
                if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG, 10)) {
                    data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
                }
                units = stat.accumulate(amount, new WrapperNBT(data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG)));
            }
            if (units > 0) {
                player.addStat(STATS.get(stat), units);
                if (stat == VehicleStat.DISTANCE_FLOWN) {
                    player.addStat(StatList.FLY_ONE_CM, units);
                }
            }
        }
    }

    static int getDeathCount(EntityPlayer player) {
        return player instanceof EntityPlayerMP ? ((EntityPlayerMP) player).getStatFile().readStat(StatList.DEATHS) : 0;
    }
}
