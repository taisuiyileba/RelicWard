package cn.suiyi.relicward;

import net.minecraftforge.common.ForgeConfigSpec;

/** Loaded before dynamic worldgen registries; changes require a restart. */
public final class WorldgenPreferences {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue COURT_SPACING;
    static {
        var b=new ForgeConfigSpec.Builder();
        b.push("worldgen");
        COURT_SPACING=b.comment("Approximate distance target between successful courts, in blocks.",
            "Default 2000. Calibrated candidate grid; terrain/biomes cause substantial local variation.",
            "Restart required. Only affects newly generated chunks; existing courts remain.")
            .worldRestart().defineInRange("courtDistanceBlocks",2000,512,16000);
        b.pop();SPEC=b.build();
    }
    // About 5% of sampled candidate regions passed biome + terrain screening.
    // For that density, mean nearest-neighbour distance is roughly grid / (2*sqrt(.05)).
    // A 0.45 grid factor targets that mean; it is not a guarantee for any seed/location.
    public static int spacingChunks(){return Math.max(8,(int)Math.ceil(COURT_SPACING.get()*.45/16));}
}
