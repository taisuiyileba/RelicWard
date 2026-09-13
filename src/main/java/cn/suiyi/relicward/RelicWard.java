package cn.suiyi.relicward;

import cn.suiyi.relicward.entity.BellWarden;
import cn.suiyi.relicward.entity.CourtPuppet;
import cn.suiyi.relicward.entity.MaulPulse;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.registries.Registries;

@Mod(RelicWard.ID)
public final class RelicWard {
    public static final String ID = "relicward";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ID);
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> MUSIC_ONE = SOUNDS.register("music.bell_warden_1", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(ID,"music.bell_warden_1")));
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> MUSIC_TWO = SOUNDS.register("music.bell_warden_2", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(ID,"music.bell_warden_2")));
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    public static final RegistryObject<EntityType<BellWarden>> BELL_WARDEN = ENTITIES.register("bell_warden",
            () -> EntityType.Builder.of(BellWarden::new, MobCategory.CREATURE)
                    .sized(2.8F, 5.2F).clientTrackingRange(12).updateInterval(3)
                    .build(ID + ":bell_warden"));
    public static final RegistryObject<EntityType<CourtPuppet>> COURT_PUPPET=ENTITIES.register("court_puppet",
            ()->EntityType.Builder.of(CourtPuppet::new,MobCategory.MONSTER).sized(.8F,1.5F).clientTrackingRange(8).build(ID+":court_puppet"));
    public static final RegistryObject<EntityType<MaulPulse>> MAUL_PULSE=ENTITIES.register("maul_pulse",
            ()->EntityType.Builder.<MaulPulse>of(MaulPulse::new,MobCategory.MISC).sized(.1F,.1F).clientTrackingRange(8).noSave().noSummon().build(ID+":maul_pulse"));
    public static final RegistryObject<EntityType<cn.suiyi.relicward.entity.ResonantWave>> RESONANT_WAVE=ENTITIES.register("resonant_wave",
            ()->EntityType.Builder.<cn.suiyi.relicward.entity.ResonantWave>of(cn.suiyi.relicward.entity.ResonantWave::new,MobCategory.MISC).sized(.1F,.1F).clientTrackingRange(12).updateInterval(1).noSave().noSummon().build(ID+":resonant_wave"));
    public static final RegistryObject<Item> PUPPET_EGG=ITEMS.register("court_puppet_spawn_egg",
            ()->new ForgeSpawnEggItem(COURT_PUPPET,0x354E46,0xA7844B,new Item.Properties()));

    public static final RegistryObject<Item> BELL_WARDEN_SPAWN_EGG = ITEMS.register("bell_warden_spawn_egg",
            () -> new ForgeSpawnEggItem(BELL_WARDEN, 0x436D60, 0xCEA35B, new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, level, tooltip, flag);
                    tooltip.add(Component.translatable("tooltip.relicward.model_preview").withStyle(ChatFormatting.GRAY));
                }
            });

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("relicward", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.relicward"))
            .icon(() -> new ItemStack(BELL_WARDEN_SPAWN_EGG.get()))
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item->output.accept(item.get()))).build());

    public RelicWard() {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT,ClientPreferences.SPEC);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,WorldgenPreferences.SPEC);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ChapterContent.register();
        RelicContent.register(bus);
        SOUNDS.register(bus);
        cn.suiyi.relicward.combat.ArmorFracture.register();
        cn.suiyi.relicward.combat.SuperGravity.EFFECTS.register(bus);
        ENTITIES.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::creativeContents);
    }

    private void attributes(EntityAttributeCreationEvent event) {
        event.put(BELL_WARDEN.get(), BellWarden.createAttributes().build());
        event.put(COURT_PUPPET.get(),CourtPuppet.attributes().build());
    }

    private void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {event.accept(BELL_WARDEN_SPAWN_EGG.get());event.accept(PUPPET_EGG.get());}
    }
}
