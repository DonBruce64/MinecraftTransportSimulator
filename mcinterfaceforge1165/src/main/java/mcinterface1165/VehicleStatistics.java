package mcinterface1165;

import java.util.EnumMap;
import java.util.Map;

import minecrafttransportsimulator.systems.VehicleStat;
import net.minecraft.util.registry.Registry;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.stats.Stat;
import net.minecraft.stats.IStatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.entity.player.PlayerEntity;

final class VehicleStatistics {
    private static final Map<VehicleStat, Stat<ResourceLocation>> STATS = new EnumMap<>(VehicleStat.class);

    static void initialize() {
        for (VehicleStat stat : VehicleStat.values()) {
            ResourceLocation id = new ResourceLocation(InterfaceLoader.MODID, stat.id);
            Registry.register(Registry.CUSTOM_STAT, id, id);
            STATS.put(stat, Stats.CUSTOM.get(id, formatter(stat)));
        }
    }

    private static IStatFormatter formatter(VehicleStat stat) {
        switch (stat) {
            case DISTANCE_DRIVEN:
            case DISTANCE_AS_PASSENGER:
            case DISTANCE_FLOWN:
                return IStatFormatter.DISTANCE;
            case FUEL_BURNED:
                return VehicleStat::formatFuel;
            case ENGINE_HOURS:
                return VehicleStat::formatEngineHours;
            default:
                return IStatFormatter.DEFAULT;
        }
    }

    static void award(PlayerEntity player, VehicleStat stat, double amount) {
        if (player instanceof ServerPlayerEntity && Double.isFinite(amount) && amount > 0) {
            int units = (int) amount;
            if (stat.usesFractionalUnits) {
                CompoundNBT data = player.getPersistentData();
                if (!data.contains(PlayerEntity.PERSISTED_NBT_TAG, 10)) {
                    data.put(PlayerEntity.PERSISTED_NBT_TAG, new CompoundNBT());
                }
                units = stat.accumulate(amount, new WrapperNBT(data.getCompound(PlayerEntity.PERSISTED_NBT_TAG)));
            }
            if (units > 0) {
                player.awardStat(STATS.get(stat), units);
                if (stat == VehicleStat.DISTANCE_FLOWN) {
                    player.awardStat(Stats.FLY_ONE_CM, units);
                }
            }
        }
    }

    static int getDeathCount(PlayerEntity player) {
        return player instanceof ServerPlayerEntity ? ((ServerPlayerEntity) player).getStats().getValue(Stats.CUSTOM, Stats.DEATHS) : 0;
    }
}
