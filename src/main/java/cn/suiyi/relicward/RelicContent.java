package cn.suiyi.relicward;

import cn.suiyi.relicward.block.CourtAltarBlock;
import cn.suiyi.relicward.block.CourtAltarEntity;
import cn.suiyi.relicward.block.ResonantPillarBlock;
import cn.suiyi.relicward.block.TeachingBellBlock;
import cn.suiyi.relicward.block.TeachingBellEntity;
import cn.suiyi.relicward.item.EchoMaulItem;
import cn.suiyi.relicward.item.WardenPendantItem;
import cn.suiyi.relicward.world.CourtStructure;
import cn.suiyi.relicward.world.CourtPlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class RelicContent {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,RelicWard.ID);
    public static final DeferredRegister<StructureType<?>> STRUCTURES=DeferredRegister.create(Registries.STRUCTURE_TYPE,RelicWard.ID);
    public static final RegistryObject<StructureType<CourtStructure>> COURT_STRUCTURE=STRUCTURES.register("resonant_court",()->()->CourtStructure.CODEC);
    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS=DeferredRegister.create(Registries.STRUCTURE_PLACEMENT,RelicWard.ID);
    public static final RegistryObject<StructurePlacementType<CourtPlacement>> COURT_PLACEMENT=PLACEMENTS.register("court_spread",()->()->CourtPlacement.CODEC);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,RelicWard.ID);
    public static BlockBehaviour.Properties bronze() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(3,8).sound(SoundType.COPPER);
    }
    public static final RegistryObject<Block> BRICKS=BLOCKS.register("bronze_bricks",()->new Block(bronze()));
    public static final RegistryObject<Block> LAMP=BLOCKS.register("echo_lamp",()->new Block(bronze().lightLevel(s->12)));
    public static final RegistryObject<Block> PILLAR=BLOCKS.register("resonant_pillar",ResonantPillarBlock::new);
    public static final RegistryObject<Block> ALTAR=BLOCKS.register("court_altar",CourtAltarBlock::new);
    public static final RegistryObject<Block> TEACHING_BELL=BLOCKS.register("teaching_bell",TeachingBellBlock::new);
    public static final RegistryObject<Block> TROPHY=BLOCKS.register("warden_trophy",cn.suiyi.relicward.block.WardenTrophyBlock::new);
    public static final RegistryObject<BlockEntityType<CourtAltarEntity>> ALTAR_ENTITY=BLOCK_ENTITIES.register("court_altar",
        ()->BlockEntityType.Builder.of(CourtAltarEntity::new,ALTAR.get()).build(null));
    public static final RegistryObject<BlockEntityType<TeachingBellEntity>> TEACHING_ENTITY=BLOCK_ENTITIES.register("teaching_bell",
        ()->BlockEntityType.Builder.of(TeachingBellEntity::new,TEACHING_BELL.get()).build(null));
    public static final RegistryObject<Item> CORE=RelicWard.ITEMS.register("bell_core",()->new Item(new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> FRAGMENT=RelicWard.ITEMS.register("bronze_fragment",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAUL=RelicWard.ITEMS.register("echo_maul",EchoMaulItem::new);
    public static final RegistryObject<Item> WAVE_BELL=RelicWard.ITEMS.register("resonant_bell",cn.suiyi.relicward.item.ResonantBellItem::new);
    public static final RegistryObject<Item> PENDANT=RelicWard.ITEMS.register("warden_pendant",WardenPendantItem::new);
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); BLOCK_ENTITIES.register(bus);STRUCTURES.register(bus);PLACEMENTS.register(bus);
        for (var block : new RegistryObject[]{BRICKS,LAMP,PILLAR,ALTAR,TEACHING_BELL,TROPHY}) {
            @SuppressWarnings("unchecked") RegistryObject<Block> typed=(RegistryObject<Block>)block;
            RelicWard.ITEMS.register(typed.getId().getPath(),()->new BlockItem(typed.get(),new Item.Properties()));
        }
    }
}
