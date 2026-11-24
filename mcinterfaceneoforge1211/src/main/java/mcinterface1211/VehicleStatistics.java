package mcinterface1211;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import minecrafttransportsimulator.systems.VehicleStat;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredRegister;

final class VehicleStatistics {
    static final DeferredRegister<ResourceLocation> REGISTER = DeferredRegister.create(Registries.CUSTOM_STAT, InterfaceLoader.MODID);
    private static final Map<VehicleStat, Supplier<ResourceLocation>> IDS = new EnumMap<>(VehicleStat.class);

    static {
        for (VehicleStat stat : VehicleStat.values()) {
            IDS.put(stat, REGISTER.register(stat.id, () -> ResourceLocation.fromNamespaceAndPath(InterfaceLoader.MODID, stat.id)));
        }
    }

    private static final Map<VehicleStat, Stat<ResourceLocation>> STATS = new EnumMap<>(VehicleStat.class);

    static void initialize() {
        for (VehicleStat stat : VehicleStat.values()) {
            ResourceLocation id = IDS.get(stat).get();
            STATS.put(stat, Stats.CUSTOM.get(id, formatter(stat)));
        }
    }

    private static StatFormatter formatter(VehicleStat stat) {
        switch (stat) {
            case DISTANCE_DRIVEN:
            case DISTANCE_AS_PASSENGER:
            case DISTANCE_FLOWN:
                return StatFormatter.DISTANCE;
            case FUEL_BURNED:
                return VehicleStat::formatFuel;
            case ENGINE_HOURS:
                return VehicleStat::formatEngineHours;
            default:
                return StatFormatter.DEFAULT;
        }
    }

    static void award(Player player, VehicleStat stat, double amount) {
        if (player instanceof ServerPlayer && Double.isFinite(amount) && amount > 0) {
            int units = (int) amount;
            if (stat.usesFractionalUnits) {
                CompoundTag data = player.getPersistentData();
                if (!data.contains(Player.PERSISTED_NBT_TAG, 10)) {
                    data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
                }
                units = stat.accumulate(amount, new WrapperNBT(data.getCompound(Player.PERSISTED_NBT_TAG)));
            }
            if (units > 0) {
                player.awardStat(STATS.get(stat), units);
                if (stat == VehicleStat.DISTANCE_FLOWN) {
                    player.awardStat(Stats.FLY_ONE_CM, units);
                }
            }
        }
    }

    static int getDeathCount(Player player) {
        return player instanceof ServerPlayer ? ((ServerPlayer) player).getStats().getValue(Stats.CUSTOM, Stats.DEATHS) : 0;
    }
}
