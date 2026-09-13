package cn.suiyi.relicward.world;

import cn.suiyi.relicward.*;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.structure.placement.*;

/** Random-spread subclass keeps vanilla locate and generation on the same grid. */
public final class CourtPlacement extends RandomSpreadStructurePlacement {
    public static final Codec<CourtPlacement> CODEC=Codec.INT.fieldOf("salt").xmap(CourtPlacement::new,CourtPlacement::salt).codec();
    public CourtPlacement(int salt){
        super(WorldgenPreferences.spacingChunks(),Math.max(8,WorldgenPreferences.spacingChunks()/3),RandomSpreadType.LINEAR,salt);
    }
    @Override public StructurePlacementType<?> type(){return RelicContent.COURT_PLACEMENT.get();}
}
