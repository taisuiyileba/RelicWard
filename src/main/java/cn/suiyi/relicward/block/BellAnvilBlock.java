package cn.suiyi.relicward.block;

import cn.suiyi.relicward.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Anchored anvil: vanilla repair/rename/enchant costs, no falling or wear. */
@Mod.EventBusSubscriber(modid=RelicWard.ID)
public final class BellAnvilBlock extends Block {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public BellAnvilBlock(){super(RelicContent.bronze().strength(5,1200).requiresCorrectToolForDrops().noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection());}
    @Override public BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override public BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Blocks.ANVIL.defaultBlockState().setValue(AnvilBlock.FACING,s.getValue(FACING)).getShape(l,p,c);}
    @Override public MenuProvider getMenuProvider(BlockState s,Level l,BlockPos p){return new SimpleMenuProvider((id,inv,player)->new BellAnvilMenu(id,inv,ContainerLevelAccess.create(l,p)),Component.translatable("block.relicward.bell_anvil"));}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(!l.isClientSide)player.openMenu(getMenuProvider(s,l,p));return InteractionResult.sidedSuccess(l.isClientSide);}
    public static final class BellAnvilMenu extends AnvilMenu {public BellAnvilMenu(int id,Inventory inv,ContainerLevelAccess access){super(id,inv,access);}}
    @SubscribeEvent public static void repaired(AnvilRepairEvent e){if(e.getEntity().containerMenu instanceof BellAnvilMenu)e.setBreakChance(0);}
}
