package cn.suiyi.relicward.block;

import cn.suiyi.relicward.RelicContent;
import cn.suiyi.relicward.combat.CombatMath;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TeachingBellEntity extends BlockEntity {
    private final Set<UUID> hit=new HashSet<>();
    public TeachingBellEntity(BlockPos p,BlockState s) { super(RelicContent.TEACHING_ENTITY.get(),p,s); }
    public static final int MIN_INTERVAL=100,MAX_INTERVAL=180;
    private long started=-1,nextWave=-1;
    private boolean initialized;
    public int cycle(){return level==null||started<0?33:(int)Math.min(1000,Math.max(0,level.getGameTime()-started));}
    public long nextWaveTime(){return nextWave;}
    public static int randomInterval(net.minecraft.util.RandomSource random){return MIN_INTERVAL+random.nextInt(MAX_INTERVAL-MIN_INTERVAL+1);}
    private void sync(){setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag){super.saveAdditional(tag);tag.putLong("Started",started);tag.putLong("NextWave",nextWave);}
    @Override public void load(net.minecraft.nbt.CompoundTag tag){super.load(tag);started=tag.contains("Started")?tag.getLong("Started"):-1;nextWave=tag.contains("NextWave")?tag.getLong("NextWave"):-1;initialized=false;}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(9,2,9);}
    public static void tick(Level l,BlockPos p,BlockState s,TeachingBellEntity bell) {
        if(l.isClientSide)return;
        long now=l.getGameTime();
        if(!bell.initialized){bell.initialized=true;bell.started=-1;bell.nextWave=Math.max(now+40,bell.nextWave<0?now+randomInterval(l.random):Math.min(bell.nextWave,now+MAX_INTERVAL));bell.sync();}
        if(now>=bell.nextWave){
            bell.started=now;bell.nextWave=now+randomInterval(l.random);bell.hit.clear();bell.sync();
            if(l.getNearestPlayer(p.getX(),p.getY(),p.getZ(),12,false)!=null)l.playSound(null,p,SoundEvents.BELL_BLOCK,SoundSource.BLOCKS,.5F,1.6F);
        }
        int t=bell.cycle();
        if(t<20||t>32)return;
        Vec3 center=Vec3.atBottomCenterOf(p);
        for(var player:l.getEntitiesOfClass(Player.class,new AABB(p).inflate(8))) {
            if(player.isCreative()||player.isSpectator()||bell.hit.contains(player.getUUID()))continue;
            if(CombatMath.wave(center,player.position(),p.getY(),2+(t-20)*.5)) {
                bell.hit.add(player.getUUID());player.hurt(l.damageSources().magic(),1);cn.suiyi.relicward.combat.ArmorFracture.apply(player);
            }
        }
    }
}
