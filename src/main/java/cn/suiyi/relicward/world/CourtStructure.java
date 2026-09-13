package cn.suiyi.relicward.world;

import cn.suiyi.relicward.RelicContent;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;

public final class CourtStructure extends Structure {
    public static final Codec<CourtStructure> CODEC=simpleCodec(CourtStructure::new);
    public CourtStructure(StructureSettings settings) {super(settings);}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext c) {
        int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ(),min=999,max=-999;
        for(int dx:new int[]{-36,0,36})for(int dz:new int[]{-36,0,36}) {
            int y=c.chunkGenerator().getFirstFreeHeight(x+dx,z+dz,Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState());
            if(y<c.chunkGenerator().getSeaLevel()+1)return Optional.empty();
            min=Math.min(min,y);max=Math.max(max,y);
        }
        if(min<64||max>160||max-min>20)return Optional.empty();
        int height=c.chunkGenerator().getFirstFreeHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState());
        var pool=c.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL).getHolderOrThrow(
            ResourceKey.create(Registries.TEMPLATE_POOL,new ResourceLocation("relicward","court/start")));
        return JigsawPlacement.addPieces(c,pool,Optional.of(new ResourceLocation("relicward","center")),1,
            new BlockPos(x,height,z),false,Optional.empty(),80);
    }
    @Override public StructureType<?> type() { return RelicContent.COURT_STRUCTURE.get(); }
}
