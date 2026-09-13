package cn.suiyi.relicward.test;

import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.block.CourtAltarEntity;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(RelicWard.ID)
@PrefixGameTestTemplate(false)
public final class WorldgenTests {
    private static void court(GameTestHelper h) {
        var altar=h.getBlockEntity(new BlockPos(32,6,41));
        h.assertTrue(altar instanceof CourtAltarEntity,"Template contains altar after rotation");
        var c=(CourtAltarEntity)altar;
        h.assertTrue(c.intactPillars()==3,"Rotated altar must point at all three pillars");
        h.assertTrue(c.obstruction()==null,"Rotated template must be a valid arena: "+c.obstruction());
        for(int x:new int[]{15,17,47,49})for(int z:new int[]{8,40}){
            var p=new BlockPos(x,7,z);var state=h.getBlockState(p);
            h.assertTrue(state.getBlock() instanceof net.minecraft.world.level.block.CandleBlock,"Candle retained");
            h.assertTrue(state.canSurvive(h.getLevel(),h.absolutePos(p)),"Candle must have a solid support after rotation");
            h.assertTrue(h.getBlockState(p.below()).getValue(net.minecraft.world.level.block.SlabBlock.TYPE)==net.minecraft.world.level.block.state.properties.SlabType.TOP,"No half-block gap");
        }
        var oldSupport=new BlockPos(15,6,8);
        h.getLevel().setBlock(h.absolutePos(oldSupport),h.getBlockState(oldSupport).setValue(net.minecraft.world.level.block.SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.BOTTOM),2);
        c.repairOfferingSupports();
        h.assertTrue(h.getBlockState(oldSupport).getValue(net.minecraft.world.level.block.SlabBlock.TYPE)==net.minecraft.world.level.block.state.properties.SlabType.TOP,"Existing court candle support migrates in each orientation");
        h.succeed();
    }
    @GameTest(template="resonant_court",timeoutTicks=60,batch="structures") public static void northTemplate(GameTestHelper h){court(h);}
    @GameTest(template="resonant_court",rotationSteps=1,timeoutTicks=60,batch="structures") public static void eastTemplate(GameTestHelper h){court(h);}
    @GameTest(template="resonant_court",rotationSteps=2,timeoutTicks=60,batch="structures") public static void southTemplate(GameTestHelper h){court(h);}
    @GameTest(template="resonant_court",rotationSteps=3,timeoutTicks=60,batch="structures") public static void westTemplate(GameTestHelper h){court(h);}

    @GameTest(template="creature_room",timeoutTicks=1800,batch="worldgen")
    public static void twentyNaturalGenerationSites(GameTestHelper h) {
        var registry=h.getLevel().registryAccess();
        var generator=(NoiseBasedChunkGenerator)registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions().overworld();
        var randomState=RandomState.create(generator.generatorSettings().value(),registry.registryOrThrow(Registries.NOISE).asLookup(),8172L);
        var structure=registry.registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("relicward","resonant_court"));
        var set=registry.registryOrThrow(Registries.STRUCTURE_SET).get(new ResourceLocation("relicward","resonant_court"));
        var placement=(RandomSpreadStructurePlacement)set.placement();
        var random=RandomSource.create(912783);
        int[] counts={0,0};StringBuilder csv=new StringBuilder("seed,chunk_x,chunk_z,min_x,min_y,min_z,max_x,max_y,max_z\n");
        h.onEachTick(()->{
            if(counts[1]>=20)return;
            for(int sample=0;sample<4;sample++) {
                counts[0]++;
                var candidate=placement.getPotentialStructureChunk(8172,(random.nextInt(200)-100)*placement.spacing(),(random.nextInt(200)-100)*placement.spacing());
                var biome=generator.getBiomeSource().getNoiseBiome(candidate.getMiddleBlockX()>>2,20,candidate.getMiddleBlockZ()>>2,randomState.sampler());
                if(!structure.biomes().contains(biome))continue;
                var start=structure.generate(registry,generator,generator.getBiomeSource(),randomState,h.getLevel().getStructureManager(),8172,candidate,0,h.getLevel(),structure.biomes()::contains);
                if(!start.isValid())continue;
                var b=start.getBoundingBox();
                h.assertTrue(b.getYSpan()>=42,"Generation must include full tower");
                h.assertTrue(start.getPieces().size()==1,"Jigsaw start must produce the whole template");
                csv.append("8172,").append(candidate.x).append(',').append(candidate.z).append(',').append(b.minX()).append(',').append(b.minY()).append(',').append(b.minZ()).append(',').append(b.maxX()).append(',').append(b.maxY()).append(',').append(b.maxZ()).append('\n');
                if(++counts[1]>=20) {
                    try {Path path=Path.of("..","art","validation","worldgen_sites.csv");Files.createDirectories(path.getParent());Files.writeString(path,csv);}
                    catch(Exception e){throw new RuntimeException(e);}
                    LogUtils.getLogger().info("RELICWARD_WORLDGEN_OK: 20 sites from {} candidate regions",counts[0]);h.succeed();return;
                }
            }
        });
    }
}
