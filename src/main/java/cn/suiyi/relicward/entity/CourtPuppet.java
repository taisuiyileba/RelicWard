package cn.suiyi.relicward.entity;

import cn.suiyi.relicward.combat.CombatMath;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

public final class CourtPuppet extends TamableAnimal {
    private static final EntityDataAccessor<Integer> ATTACK=SynchedEntityData.defineId(CourtPuppet.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> START=SynchedEntityData.defineId(CourtPuppet.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> REINFORCEMENT=SynchedEntityData.defineId(CourtPuppet.class,EntityDataSerializers.BOOLEAN);
    private UUID commander,encounter;
    private net.minecraft.core.BlockPos courtHome;private int courtSlot=-1;
    private UUID victim;private boolean nextBash;private long lastTick=Long.MIN_VALUE;
    public CourtPuppet(EntityType<? extends TamableAnimal> type,Level level){super(type,level);xpReward=5;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.MOVEMENT_SPEED,.22).add(Attributes.FOLLOW_RANGE,14);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ATTACK,0);entityData.define(START,0L);entityData.define(REINFORCEMENT,false);}
    public boolean isReinforcement(){return entityData.get(REINFORCEMENT);}
    @Override public boolean isCurrentlyGlowing(){return false;}
    public boolean isReinforcementOf(BellWarden boss){return isReinforcement()&&boss.getUUID().equals(commander)&&boss.encounterId().equals(encounter);}
    public void bindReinforcement(BellWarden boss){
        commander=boss.getUUID();encounter=boss.encounterId();entityData.set(REINFORCEMENT,true);setPersistenceRequired();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(36);setHealth(36);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.28);
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(24);restrictTo(net.minecraft.core.BlockPos.containing(boss.arenaCenter()),22);
        setTarget(boss.getTarget());
    }
    public int attackKind(){return entityData.get(ATTACK);}
    public float attackTick(){return Math.max(0,level().getGameTime()-entityData.get(START));}
    public int contactTick(){return attackKind()==2?16:12;}
    public void beginStrike(LivingEntity target,boolean bash){
        if(level().isClientSide||attackKind()!=0)return;
        victim=target.getUUID();entityData.set(ATTACK,bash?2:1);entityData.set(START,level().getGameTime());navigation.stop();
        playSound(SoundEvents.ARMOR_EQUIP_IRON,.45F,1.1F);
    }
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(0,new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(1,new Goal(){
            {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
            @Override public boolean canUse(){return attackKind()!=0;}
            @Override public void tick(){navigation.stop();}
        });
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1,true){
            @Override protected void checkAndPerformAttack(LivingEntity target,double distance){
                if(attackKind()==0&&distance<=getAttackReachSqr(target)&&isTimeToAttack()){
                    resetAttackCooldown();beginStrike(target,nextBash);nextBash=!nextBash;
                }
            }
        });
        goalSelector.addGoal(3,new FollowOwnerGoal(this,1.2,6,2,false));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.6){@Override public boolean canUse(){return !isOrderedToSit()&&super.canUse();}});
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,10));
        targetSelector.addGoal(1,new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2,new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3,new HurtByTargetGoal(this));
        targetSelector.addGoal(4,new NearestAttackableTargetGoal<>(this,Player.class,10,true,false,p->!isTame()));
    }
    @Override public void tick(){
        if(!level().isClientSide&&isReinforcement()){
            var owner=commander==null?null:((net.minecraft.server.level.ServerLevel)level()).getEntity(commander);
            if(!(owner instanceof BellWarden boss)||!isReinforcementOf(boss)||!boss.isAlive()||!boss.isEncounterActive()||boss.phase()!=2){discard();return;}
            if(CombatMath.horizontalSquared(position(),boss.arenaCenter())>24*24){discard();return;}
            if(getTarget()==null||!canAttack(getTarget()))setTarget(boss.getTarget()!=null&&canAttack(boss.getTarget())?boss.getTarget():null);
        }
        super.tick();if(level().isClientSide)return;
        long now=level().getGameTime();
        if(lastTick!=Long.MIN_VALUE&&now-lastTick>1)entityData.set(ATTACK,0);lastTick=now;
        if(isOrderedToSit()){entityData.set(ATTACK,0);setTarget(null);return;}
        if(attackKind()==0)return;
        navigation.stop();setDeltaMovement(0,getDeltaMovement().y,0);
        var target=victim==null?null:((net.minecraft.server.level.ServerLevel)level()).getEntity(victim);
        if(target instanceof LivingEntity living&&living.isAlive()&&!isAlliedTo(living)&&(!isReinforcement()||canAttack(living))){
            if(attackTick()<contactTick()-4){
                float yaw=(float)(Math.atan2(living.getZ()-getZ(),living.getX()-getX())*180/Math.PI)-90;
                setYRot(Mth.approachDegrees(getYRot(),yaw,10));setYBodyRot(getYRot());setYHeadRot(getYRot());
            }
            if(attackTick()==contactTick()&&hasLineOfSight(living)&&Math.abs(living.getY()-getY())<2&&CombatMath.cone(position(),getYRot(),living.position(),attackKind()==2?2.25:2.8,100)){
                doHurtTarget(living);
                if(attackKind()==2)living.knockback(.65,getX()-living.getX(),getZ()-living.getZ());
                playSound(attackKind()==2?SoundEvents.SHIELD_BLOCK:SoundEvents.PLAYER_ATTACK_SWEEP,.65F,1.1F);
            }
        }
        if(attackTick()>=(attackKind()==2?36:30))entityData.set(ATTACK,0);
    }

    public void bindPatrol(net.minecraft.core.BlockPos home,int slot){courtHome=home.immutable();courtSlot=slot;setPersistenceRequired();restrictTo(blockPosition(),24);}
    public net.minecraft.core.BlockPos courtHome(){return courtHome;}
    private void releasePatrol(){
        if(courtHome==null||level().isClientSide)return;
        var home=courtHome;int slot=courtSlot;courtHome=null;courtSlot=-1;
        var server=(net.minecraft.server.level.ServerLevel)level();server.getChunkAt(home);
        if(server.getBlockEntity(home) instanceof cn.suiyi.relicward.block.CourtAltarEntity altar)altar.population().release(altar,slot,getUUID());
        clearRestriction();
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){releasePatrol();super.die(source);}
    @Override public void remove(RemovalReason reason){if(reason.shouldDestroy())releasePatrol();super.remove(reason);}
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag t){super.addAdditionalSaveData(t);if(courtHome!=null){t.putLong("PatrolHome",courtHome.asLong());t.putInt("PatrolSlot",courtSlot);}if(isReinforcement()){t.putUUID("Commander",commander);t.putUUID("Encounter",encounter);}}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag t){super.readAdditionalSaveData(t);if(t.contains("PatrolHome")){courtHome=net.minecraft.core.BlockPos.of(t.getLong("PatrolHome"));courtSlot=t.getInt("PatrolSlot");}if(isTame()){setPersistenceRequired();getAttribute(Attributes.MAX_HEALTH).setBaseValue(32);}if(t.hasUUID("Commander")&&t.hasUUID("Encounter")){commander=t.getUUID("Commander");encounter=t.getUUID("Encounter");entityData.set(REINFORCEMENT,true);}}
    @Override public AgeableMob getBreedOffspring(net.minecraft.server.level.ServerLevel l,AgeableMob other){return null;}
    @Override public boolean isFood(net.minecraft.world.item.ItemStack stack){return false;}
    @Override public boolean canMate(net.minecraft.world.entity.animal.Animal other){return false;}
    @Override public boolean removeWhenFarAway(double distance){return !isTame()&&courtHome==null;}
    @Override protected boolean shouldDespawnInPeaceful(){return !isTame();}
    @Override public int getExperienceReward(){return isTame()||isReinforcement()?0:5;}
    @Override protected boolean shouldDropLoot(){return !isTame()&&!isReinforcement();}
    @Override public boolean canAttack(LivingEntity other){
        if(isReinforcement()){
            var boss=commander==null||!(level() instanceof net.minecraft.server.level.ServerLevel server)?null:server.getEntity(commander);
            if(!(boss instanceof BellWarden w)||!isReinforcementOf(w)||!w.isEncounterActive()||!w.isAlive()
                    ||CombatMath.horizontalSquared(other.position(),w.arenaCenter())>22*22||Math.abs(other.getY()-w.arenaCenter().y)>=10
                    ||!(other instanceof Player p&&!p.isCreative()&&!p.isSpectator()||other instanceof CourtPuppet pet&&pet.isTame()))return false;
        }
        return !isAlliedTo(other)&&super.canAttack(other);
    }
    @Override public boolean wantsToAttack(LivingEntity target,LivingEntity owner){
        if(target instanceof Player victim&&owner instanceof Player p&&!p.canHarmPlayer(victim))return false;
        return !isAlliedTo(target)&&!(target instanceof TamableAnimal pet&&pet.isTame()&&java.util.Objects.equals(pet.getOwnerUUID(),getOwnerUUID()));
    }
    @Override public boolean isAlliedTo(Entity other){
        if(isReinforcement()&&(other.getUUID().equals(commander)||other instanceof CourtPuppet p&&p.isReinforcement()&&java.util.Objects.equals(encounter,p.encounter)))return true;
        if(isTame()&&(other.getUUID().equals(getOwnerUUID())||other instanceof TamableAnimal pet&&java.util.Objects.equals(getOwnerUUID(),pet.getOwnerUUID())))return true;
        return super.isAlliedTo(other);
    }
    @Override public net.minecraft.world.InteractionResult mobInteract(Player player,net.minecraft.world.InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(stack.is(cn.suiyi.relicward.ChapterContent.BINDING_SEAL.get())&&!isTame()&&!isReinforcement()){
            if(!level().isClientSide){
                tame(player);releasePatrol();setPersistenceRequired();setTarget(null);setLastHurtByMob(null);victim=null;entityData.set(ATTACK,0);navigation.stop();
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(32);setHealth(32);if(!player.getAbilities().instabuild)stack.shrink(1);
                level().broadcastEntityEvent(this,(byte)7);player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.relicward.bound"),true);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }
        if(isOwnedBy(player)){
            if(stack.is(cn.suiyi.relicward.ChapterContent.REPAIR_PASTE.get())){
                if(getHealth()>=getMaxHealth())return net.minecraft.world.InteractionResult.PASS;
                if(!level().isClientSide){heal(12);if(!player.getAbilities().instabuild)stack.shrink(1);playSound(SoundEvents.IRON_GOLEM_REPAIR,.8F,1.2F);}
                return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
            }
            if(stack.isEmpty()){
                if(!level().isClientSide){setOrderedToSit(!isOrderedToSit());setTarget(null);navigation.stop();victim=null;entityData.set(ATTACK,0);player.displayClientMessage(net.minecraft.network.chat.Component.translatable(isOrderedToSit()?"message.relicward.stay":"message.relicward.follow"),true);}
                return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        return super.mobInteract(player,hand);
    }
}
