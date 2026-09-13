package cn.suiyi.relicward.block;

import cn.suiyi.relicward.RelicContent;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.*;

public final class WardenTrophyBlock extends Block {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE=Shapes.or(Block.box(1,0,1,15,3,15),Block.box(1,3,2,15,13,14));
    public WardenTrophyBlock(){super(RelicContent.bronze().noOcclusion().lightLevel(s->6));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override public BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
}
