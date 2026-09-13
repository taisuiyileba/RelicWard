package cn.suiyi.relicward.block;

import cn.suiyi.relicward.RelicContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class TeachingBellBlock extends BaseEntityBlock {
    public TeachingBellBlock() { super(RelicContent.bronze().strength(-1,3600000).noLootTable().lightLevel(s->8)); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s) { return new TeachingBellEntity(p,s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type) {
        return createTickerHelper(type,RelicContent.TEACHING_ENTITY.get(),TeachingBellEntity::tick);
    }
}
