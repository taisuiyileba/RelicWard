package cn.suiyi.relicward;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientPreferences {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue IMPACT_SHAKE;
    public static final ForgeConfigSpec.BooleanValue CUSTOM_BOSS_BAR, BOSS_MUSIC;
    public static final ForgeConfigSpec.DoubleValue BOSS_MUSIC_VOLUME;
    static {
        var b=new ForgeConfigSpec.Builder();
        IMPACT_SHAKE=b.comment("Small camera impulse on nearby heavy impacts. Disable for a steady camera.").define("impactCameraShake",true);
        CUSTOM_BOSS_BAR=b.comment("Bronze Bell Warden boss HUD.").define("customBossBar",true);
        BOSS_MUSIC=b.comment("Play original battle music near an active Bell Warden.").define("bossMusic",true);
        BOSS_MUSIC_VOLUME=b.comment("Battle music gain, also controlled by Minecraft's Music slider.").defineInRange("bossMusicVolume",0.65,0.0,1.0);
        SPEC=b.build();
    }
}
