package cn.suiyi.relicward.world;

import cn.suiyi.relicward.*;
import cn.suiyi.relicward.block.CourtAltarEntity;
import cn.suiyi.relicward.entity.CourtPuppet;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import java.util.UUID;

/** Four persistent patrol slots. Unloaded entities retain their slot and cannot duplicate. */
public final class CourtPopulation {
    public static final int RESPAWN_TICKS=6000;
    private static final int[][] OFFSETS={{-18,0,35},{18,0,35},{-7,0,49},{7,0,49}};
    private final UUID[] ids=new UUID[4];private final long[] due=new long[4];private boolean migrated;
    public void release(CourtAltarEntity altar,int slot,UUID entity){
        if(slot<0||slot>=4||!entity.equals(ids[slot]))return;
        ids[slot]=null;due[slot]=altar.getLevel().getGameTime()+RESPAWN_TICKS;altar.setChanged();
    }
    public void tick(CourtAltarEntity altar){
        if(!(altar.getLevel() instanceof ServerLevel l)||l.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return;
        if(!migrated){
            var old=l.getEntitiesOfClass(CourtPuppet.class,new AABB(altar.center()).inflate(65),m->m.isAlive()&&!m.isRemoved()&&!m.isTame()&&(m.courtHome()==null||altar.getBlockPos().equals(m.courtHome())));
            for(int i=0;i<Math.min(4,old.size());i++){ids[i]=old.get(i).getUUID();old.get(i).bindPatrol(altar.getBlockPos(),i);}
            migrated=true;altar.setChanged();
        }
        for(int i=0;i<4;i++){
            if(ids[i]!=null||l.getGameTime()<due[i])continue;
            var o=OFFSETS[i];var p=altar.local(o[0],o[1],o[2]);
            if(!l.hasChunkAt(p)||l.getNearestPlayer(p.getX(),p.getY(),p.getZ(),6,false)!=null)continue;
            var m=RelicWard.COURT_PUPPET.get().create(l);if(m==null)continue;m.moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);
            if(!l.noCollision(m)||!l.getBlockState(p.below()).isFaceSturdy(l,p.below(),net.minecraft.core.Direction.UP))continue;
            m.bindPatrol(altar.getBlockPos(),i);if(l.addFreshEntity(m)){ids[i]=m.getUUID();altar.setChanged();}
        }
    }
    public void save(CompoundTag t){var list=new ListTag();for(int i=0;i<4;i++){var e=new CompoundTag();if(ids[i]!=null)e.putUUID("Entity",ids[i]);e.putLong("Due",due[i]);list.add(e);}t.put("PatrolSlots",list);t.putBoolean("PatrolMigrated",migrated);}
    public void load(CompoundTag t){java.util.Arrays.fill(ids,null);java.util.Arrays.fill(due,0);migrated=t.getBoolean("PatrolMigrated");var list=t.getList("PatrolSlots",10);for(int i=0;i<Math.min(4,list.size());i++){var e=list.getCompound(i);ids[i]=e.hasUUID("Entity")?e.getUUID("Entity"):null;due[i]=e.getLong("Due");}}
}
