package cn.suiyi.relicward.block;

import cn.suiyi.relicward.RelicContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class CourtAltarBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public CourtAltarBlock() {
        super(RelicContent.bronze().strength(-1,3600000).noLootTable().lightLevel(s->7));
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()); }
    @Override public BlockState rotate(BlockState s,Rotation r) { return s.setValue(FACING,r.rotate(s.getValue(FACING))); }
    @Override public BlockState mirror(BlockState s,Mirror m) { return rotate(s,m.getRotation(s.getValue(FACING))); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s) { return new CourtAltarEntity(p,s); }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock()))cn.suiyi.relicward.world.CourtProtection.unregister(level,pos);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type) {
        return l.isClientSide?null:createTickerHelper(type,RelicContent.ALTAR_ENTITY.get(),CourtAltarEntity::tick);
    }
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit) {
        if (!l.isClientSide && hand==InteractionHand.MAIN_HAND && l.getBlockEntity(p) instanceof CourtAltarEntity altar) altar.interact(player);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
