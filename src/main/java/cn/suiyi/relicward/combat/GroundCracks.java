package cn.suiyi.relicward.combat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Vanilla crack overlays. No block states are replaced, harvested or damaged. */
public final class GroundCracks {
    private static final class Mark {
        final BlockPos pos;final int id,delay;int stage=-1;
        Mark(BlockPos p,int id,int delay){this.pos=p;this.id=id;this.delay=delay;}
    }
    private final List<Mark> marks=new ArrayList<>();
    private long started;
    public int size(){return marks.size();}
    public void begin(ServerLevel level,int entityId,Vec3 impact,double radius){
        clear(level);started=level.getGameTime();int index=0;
        BlockPos center=BlockPos.containing(impact).below();
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            double distance=Math.sqrt(x*x+z*z);if(distance>radius)continue;
            BlockPos pos=center.offset(x,0,z);var state=level.getBlockState(pos);
            if(state.isAir()||!state.getFluidState().isEmpty()||state.getCollisionShape(level,pos).isEmpty())continue;
            int id=-1-((entityId&0x007fffff)*64)-index++;
            marks.add(new Mark(pos,id,(int)(distance*2)));
        }
        tick(level);
    }
    public void tick(ServerLevel level){
        int age=(int)(level.getGameTime()-started);
        if(age>=36){clear(level);return;}
        for(var mark:marks){
            int local=age-mark.delay;
            int stage=local<0||local>27?-1:local<9?Math.min(9,local+1):local<19?9:Math.max(0,27-local);
            if(stage!=mark.stage){level.destroyBlockProgress(mark.id,mark.pos,stage);mark.stage=stage;}
        }
    }
    public void clear(ServerLevel level){for(var mark:marks)level.destroyBlockProgress(mark.id,mark.pos,-1);marks.clear();}
}
