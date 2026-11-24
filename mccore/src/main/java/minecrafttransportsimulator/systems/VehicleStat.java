package minecrafttransportsimulator.systems;

import java.util.Locale;

import minecrafttransportsimulator.mcinterface.IWrapperNBT;

/** The units stored in Minecraft's integer statistics. */
public enum VehicleStat {
    DISTANCE_DRIVEN("distance_driven", true),
    DISTANCE_AS_PASSENGER("distance_as_passenger", true),
    DISTANCE_FLOWN("distance_flown", true),
    VEHICLES_DESTROYED("vehicles_destroyed", false),
    FUEL_BURNED("fuel_burned", true),
    ENGINE_HOURS("engine_hours", false),
    HORRIFIC_ACCIDENTS("horrific_accidents", false),
    MOBS_RUN_OVER("mobs_run_over", false),
    PLAYERS_RUN_OVER("players_run_over", false);

    public final String id;
    public final boolean usesFractionalUnits;

    VehicleStat(String id, boolean usesFractionalUnits) {
        this.id = id;
        this.usesFractionalUnits = usesFractionalUnits;
    }

    public int accumulate(double amount, IWrapperNBT playerData) {
        String key = "mts_stat_remainder_" + id;
        double total = playerData.getDouble(key) + amount;
        int wholeUnits = (int) total;
        playerData.setDouble(key, total - wholeUnits);
        return wholeUnits;
    }

    public static String formatFuel(int millibuckets) {
        return String.format(Locale.ROOT, "%.1f B", millibuckets / 1000D);
    }

    public static String formatEngineHours(int ticks) {
        return String.format(Locale.ROOT, "%.2f h", ticks / 72000D);
    }
}
