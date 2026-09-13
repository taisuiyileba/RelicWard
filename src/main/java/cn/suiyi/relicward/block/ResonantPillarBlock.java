package cn.suiyi.relicward.block;

import cn.suiyi.relicward.RelicContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ResonantPillarBlock extends Block implements net.minecraft.world.level.block.LiquidBlockContainer {
    public static final BooleanProperty BROKEN=BooleanProperty.create("broken");
    public ResonantPillarBlock() {
        super(RelicContent.bronze().strength(-1,3600000).noLootTable().noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(BROKEN,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(BROKEN); }
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return s.getValue(BROKEN)?Shapes.empty():Shapes.block(); }
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return getShape(s,l,p,c); }
    @Override public boolean canBeReplaced(BlockState state,net.minecraft.world.level.material.Fluid fluid){return false;}
    @Override public boolean canPlaceLiquid(BlockGetter level,BlockPos pos,BlockState state,net.minecraft.world.level.material.Fluid fluid){return false;}
    @Override public boolean placeLiquid(net.minecraft.world.level.LevelAccessor level,BlockPos pos,BlockState state,net.minecraft.world.level.material.FluidState fluid){return false;}
}
