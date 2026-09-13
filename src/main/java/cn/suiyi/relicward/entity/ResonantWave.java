package cn.suiyi.relicward.entity;

import cn.suiyi.relicward.combat.*;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** A server-owned, once-per-target expanding ground ring, sharing the boss geometry. */
public final class ResonantWave extends Entity {
    private static final EntityDataAccessor<Long> BORN=SynchedEntityData.defineId(ResonantWave.class,EntityDataSerializers.LONG);
    private UUID owner;
    private final Set<UUID> hit=new HashSet<>();
    public ResonantWave(EntityType<?> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(){entityData.define(BORN,0L);}
    public void initialize(Player player){owner=player.getUUID();entityData.set(BORN,level().getGameTime());setPos(player.position());}
    public int age(){return (int)Math.max(0,level().getGameTime()-entityData.get(BORN));}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel server))return;
        if(age()>36){discard();return;}
        Entity source=owner==null?null:server.getEntity(owner);
        if(!(source instanceof Player player)||!player.isAlive()){discard();return;}
        double radius=2+age()*.5;
        for(var target:server.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(radius+1,3,radius+1))){
            if(hit.contains(target.getUUID())||!PlayerCombat.canHit(player,target)||!CombatMath.wave(position(),target.position(),getY(),radius))continue;
            if(server.clip(new ClipContext(position().add(0,.5,0),target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS)continue;
            hit.add(target.getUUID());
            if(target.hurt(server.damageSources().playerAttack(player),8))ArmorFracture.apply(target);
        }
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag){}
    @Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
    @Override public AABB getBoundingBoxForCulling(){return getBoundingBox().inflate(22);}
}
