package cn.suiyi.relicward.entity;

import cn.suiyi.relicward.block.CourtAltarEntity;
import cn.suiyi.relicward.combat.CombatMath;
import cn.suiyi.relicward.combat.WardenAction;
import cn.suiyi.relicward.combat.GroundCracks;
import cn.suiyi.relicward.combat.SweepProfile;
import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BellWarden extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> BOSS_BAR_ID=SynchedEntityData.defineId(BellWarden.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(BellWarden.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ACTION_TIME=SynchedEntityData.defineId(BellWarden.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(BellWarden.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PILLARS=SynchedEntityData.defineId(BellWarden.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ATTACK_YAW=SynchedEntityData.defineId(BellWarden.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_X=SynchedEntityData.defineId(BellWarden.class,EntityDataSerializers.FLOAT),
        AIM_Y=SynchedEntityData.defineId(BellWarden.class,EntityDataSerializers.FLOAT), AIM_Z=SynchedEntityData.defineId(BellWarden.class,EntityDataSerializers.FLOAT);
    private final ServerBossEvent bar=new ServerBossEvent(Component.translatable("entity.relicward.bell_warden"),BossEvent.BossBarColor.YELLOW,BossEvent.BossBarOverlay.PROGRESS);
    private BlockPos altar,arena;
    private UUID encounter=UUID.randomUUID();
    private final Map<UUID,Float> damage=new HashMap<>();
    private final Map<UUID,Integer> participation=new HashMap<>();
    private final EnumMap<WardenAction,Long> cooldowns=new EnumMap<>(WardenAction.class);
    private final Set<UUID> hitThisPulse=new HashSet<>(),hitSecondWave=new HashSet<>();
    private long nextAction,lastCharge,lastServerTick=Long.MIN_VALUE;
    private int absentTicks,highTicks,repeated;
    private WardenAction lastAttack=WardenAction.DORMANT;
    private boolean firstAttack,legitimateDeath;
    private final GroundCracks groundCracks=new GroundCracks();
    public int activeCrackCount(){return groundCracks.size();}

    public BellWarden(EntityType<? extends PathfinderMob> type,Level level) {
        super(type,level); setPersistenceRequired(); setMaxUpStep(1); xpReward=120; bar.setVisible(false);
        if(!level.isClientSide) entityData.set(BOSS_BAR_ID,Optional.of(bar.getId()));
    }
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH,CombatMath.WARDEN_HEALTH).add(Attributes.MOVEMENT_SPEED,.26)
            .add(Attributes.ARMOR,10).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,32);
    }
    @Override protected void defineSynchedData() {
        super.defineSynchedData(); entityData.define(BOSS_BAR_ID,Optional.empty()); entityData.define(ACTION,0); entityData.define(ACTION_TIME,0L);
        entityData.define(PHASE,1); entityData.define(PILLARS,0);
        entityData.define(ATTACK_YAW,0F);
        entityData.define(AIM_X,0F); entityData.define(AIM_Y,0F); entityData.define(AIM_Z,0F);
    }
    @Override protected void registerGoals() { /* Movement and damage share one authoritative timeline. */ }
    public WardenAction action() { return WardenAction.values()[entityData.get(ACTION)]; }
    public int actionTick() { return (int)Math.max(0,level().getGameTime()-entityData.get(ACTION_TIME)); }
    public int phase() { return entityData.get(PHASE); }
    public Optional<UUID> bossBarId() { return entityData.get(BOSS_BAR_ID); }
    public int pillars() { return entityData.get(PILLARS); }
    public float attackYaw(){return entityData.get(ATTACK_YAW);}
    public Vec3 aim() { return new Vec3(entityData.get(AIM_X),entityData.get(AIM_Y),entityData.get(AIM_Z)); }
    private void aimAt(Vec3 p) { entityData.set(AIM_X,(float)p.x); entityData.set(AIM_Y,(float)p.y); entityData.set(AIM_Z,(float)p.z); }
    public boolean isEncounterActive() { return action()!=WardenAction.DORMANT && action()!=WardenAction.DYING; }
    @Nullable public BlockPos altarPos() { return altar; }
    public UUID encounterId() { return encounter; }
    public Vec3 arenaCenter() { return arena==null?position():Vec3.atBottomCenterOf(arena); }
    @Nullable public CourtAltarEntity court() { return altar!=null&&level().getBlockEntity(altar) instanceof CourtAltarEntity c ? c:null; }
    public void bindCourt(BlockPos altar,BlockPos center) { this.altar=altar.immutable(); arena=center.immutable(); entityData.set(PILLARS,3); }
    private List<Player> participants() {
        return level().getEntitiesOfClass(Player.class,new AABB(arenaCenter(),arenaCenter()).inflate(22,12,22),
            p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&CombatMath.horizontalSquared(p.position(),arenaCenter())<=22*22&&Math.abs(p.getY()-arenaCenter().y)<10);
    }
    public boolean startEncounter() {
        if(level().isClientSide||isEncounterActive()||!isAlive()||level().getDifficulty()==Difficulty.PEACEFUL) return false;
        if(arena==null) arena=blockPosition();
        var players=participants(); if(players.isEmpty()) return false;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(CombatMath.healthForPlayers(players.size())); setHealth(getMaxHealth());
        damage.clear(); participation.clear(); cooldowns.clear(); hitThisPulse.clear(); encounter=UUID.randomUUID();
        entityData.set(PHASE,1); entityData.set(PILLARS,court()==null?0:court().intactPillars());
        firstAttack=true; absentTicks=0; highTicks=0; repeated=0; lastAttack=WardenAction.DORMANT;
        lastCharge=level().getGameTime(); setTarget(players.get(0)); setAction(WardenAction.AWAKEN); bar.setVisible(true);
        players.forEach(p->p.displayClientMessage(Component.translatable("message.relicward.begin"),false)); return true;
    }
    public void resetEncounter() {
        clearReinforcements();
        if(level() instanceof net.minecraft.server.level.ServerLevel sl)groundCracks.clear(sl);
        navigation.stop(); setTarget(null); setDeltaMovement(0,getDeltaMovement().y,0); entityData.set(PHASE,1);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(CombatMath.WARDEN_HEALTH); setHealth(getMaxHealth());
        if(arena!=null) { Vec3 p=arenaCenter(); moveTo(p.x,p.y,p.z,getYRot(),0); }
        if(court()!=null) court().restorePillars(); entityData.set(PILLARS,court()==null?0:court().intactPillars());
        damage.clear(); participation.clear(); setAction(WardenAction.DORMANT); bar.setVisible(false);
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand) {
        if(hand==InteractionHand.MAIN_HAND&&!level().isClientSide) {
            if(court()!=null) player.displayClientMessage(Component.translatable("message.relicward.use_altar"),true);
            else if(!player.isCreative()&&!player.isSpectator()) startEncounter();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    public void setAction(WardenAction next) {
        entityData.set(ACTION,next.ordinal()); entityData.set(ACTION_TIME,level().getGameTime());
        entityData.set(ATTACK_YAW,getYRot());
        hitThisPulse.clear();hitSecondWave.clear(); navigation.stop();
        if(next.attack()) {
            cooldowns.put(next,level().getGameTime()+(phase()==2?Math.round(next.cooldown*.72F):next.cooldown));
            repeated=lastAttack==next?repeated+1:1; lastAttack=next;
            if(getTarget()!=null) aimAt(new Vec3(getTarget().getX(),arenaCenter().y,getTarget().getZ()));
            if(next==WardenAction.WAVE||next==WardenAction.RESONANCE) aimAt(position());
            if(next==WardenAction.CHARGE)lastCharge=level().getGameTime();
            playSound(next==WardenAction.WAVE||next==WardenAction.RESONANCE?SoundEvents.BELL_BLOCK:SoundEvents.ARMOR_EQUIP_IRON,.85F,.65F);
        }
    }
    public void stagger() {
        entityData.set(PILLARS,court()==null?0:court().intactPillars()); setAction(WardenAction.STAGGER);
        playSound(SoundEvents.IRON_GOLEM_DAMAGE,1.3F,.55F);
    }
    @Override public void tick() {
        super.tick();
        if(level().isClientSide)return;
        groundCracks.tick((net.minecraft.server.level.ServerLevel)level());
        if(!isAlive()||isNoAi())return;
        long now=level().getGameTime();
        if(isEncounterActive()&&lastServerTick!=Long.MIN_VALUE&&now-lastServerTick>1) {
            setAction(WardenAction.IDLE);nextAction=now+40;
        }
        lastServerTick=now;
        if(altar!=null&&level().hasChunkAt(altar)&&court()==null) { discard(); return; }
        if(!isEncounterActive()) {
            if(court()!=null)entityData.set(PILLARS,court().intactPillars());
            var p=level().getNearestPlayer(this,16); if(p!=null)getLookControl().setLookAt(p,12,12);
            return;
        }
        if(level().getDifficulty()==Difficulty.PEACEFUL) { resetEncounter(); return; }
        if(CombatMath.horizontalSquared(position(),arenaCenter())>18*18) {
            Vec3 outward=new Vec3(getX()-arenaCenter().x,0,getZ()-arenaCenter().z).normalize().scale(17.9);
            setPos(arenaCenter().x+outward.x,getY(),arenaCenter().z+outward.z);navigation.stop();
        }
        var players=participants();
        for(var player:players) participation.merge(player.getUUID(),1,Integer::sum);
        boolean reachable=players.stream().anyMatch(this::hasLineOfSight);
        absentTicks=players.isEmpty()||!reachable?absentTicks+1:0;
        if(absentTicks>=200) { resetEncounter(); return; }
        bar.setProgress(Mth.clamp(getHealth()/getMaxHealth(),0,1));
        bar.setName(Component.translatable("entity.relicward.bell_warden").append(Component.translatable(
            action()==WardenAction.TRANSFORM?"boss.relicward.transform":action()==WardenAction.STAGGER?"boss.relicward.stagger":phase()==2?"boss.relicward.phase_two":"boss.relicward.phase_one",pillars())));
        if(action()==WardenAction.IDLE&&(getTarget()==null||!getTarget().isAlive()||tickCount%100==0||!players.contains(getTarget()))) {
            setTarget(players.stream().filter(this::hasLineOfSight).max(Comparator.comparingDouble(p->damage.getOrDefault(p.getUUID(),0F)-distanceTo(p)*.8)).orElse(null));
        }
        var target=getTarget();
        if(target!=null&&action()!=WardenAction.CHARGE&&action()!=WardenAction.STAGGER&&action()!=WardenAction.DYING)trackHead(target);
        if(target!=null&&target.getY()>arenaCenter().y+2)highTicks++; else highTicks=0;
        if(action()==WardenAction.IDLE) {
            if(getHealth()<=getMaxHealth()*.5F&&phase()==1) { setAction(WardenAction.TRANSFORM); return; }
            if(target!=null) {
                face(target.position());
                trackHead(target);
                if(CombatMath.horizontalSquared(position(),arenaCenter())>18*18) navigation.moveTo(arenaCenter().x,arenaCenter().y,arenaCenter().z,1);
                else if(distanceTo(target)>3.8)navigation.moveTo(target,phase()==2?1.2:1);
                else navigation.stop();
                if(level().getGameTime()>=nextAction) chooseAttack(target);
            }
            return;
        }
        navigation.stop(); setDeltaMovement(0,getDeltaMovement().y,0);
        var move=action(); int t=actionTick();
        if(move.attack()&&target!=null&&move!=WardenAction.WAVE&&move!=WardenAction.RESONANCE) {
            boolean sweep=move==WardenAction.SWEEP||move==WardenAction.DOUBLE_SWEEP;
            if(sweep?SweepProfile.tracks(move,t):t<move.windup-8) {
                face(target.position());
                trackHead(target);
                entityData.set(ATTACK_YAW,getYRot());
                Vec3 impact=move==WardenAction.HIGH_STRIKE?target.position():new Vec3(target.getX(),arenaCenter().y,target.getZ());
                if(move==WardenAction.SLAM){
                    Vec3 f=CombatMath.forward(getYRot());impact=position().add(f.scale(1.8)).add(-f.z*.9,0,f.x*.9);
                }
                aimAt(impact);
            }
        }
        switch(move) {
            case SWEEP -> { if(t==SweepProfile.contact(move,0))sweep(10); }
            case DOUBLE_SWEEP -> { if(t==SweepProfile.contact(move,0)||t==SweepProfile.contact(move,1)){hitThisPulse.clear();sweep(9);} }
            case SLAM -> { if(t==24)blast(aim(),3.5,14); }
            case HIGH_STRIKE -> { if(t==24)blast(aim(),2.5,8); }
            case WAVE -> wave(t-22,hitThisPulse);
            case RESONANCE -> { wave(t-32,hitThisPulse);wave(t-50,hitSecondWave); }
            case CHARGE -> { setYRot(attackYaw());setYBodyRot(attackYaw());setYHeadRot(attackYaw());if(t>=30&&t<64)chargeStep(); }
            case TRANSFORM -> { if(t>=50&&phase()==1){entityData.set(PHASE,2);cooldowns.clear();summonReinforcements();} }
            default -> {}
        }
        if(action()!=move)return;
        // Keep contact/telegraph ticks intact; shorten only recovery after the last hit.
        int duration=phase()==2&&move.attack()?move.duration-10:move.duration;
        if(t>=duration) { setAction(WardenAction.IDLE); nextAction=level().getGameTime()+(phase()==2?8:16); }
    }
    private boolean available(WardenAction action) { return level().getGameTime()>=cooldowns.getOrDefault(action,0L)&&!(lastAttack==action&&repeated>=2); }
    private void chooseAttack(LivingEntity target) {
        if(firstAttack) { firstAttack=false; setAction(WardenAction.WAVE);return; }
        if(highTicks>=60&&available(WardenAction.HIGH_STRIKE)&&hasLineOfSight(target)) { setAction(WardenAction.HIGH_STRIKE);return; }
        double distance=Math.sqrt(CombatMath.horizontalSquared(position(),target.position()));
        if(phase()==2&&distance<=SweepProfile.RADIUS&&available(WardenAction.DOUBLE_SWEEP)
                &&(lastAttack==WardenAction.SLAM||lastAttack==WardenAction.CHARGE)) {setAction(WardenAction.DOUBLE_SWEEP);return;}
        if(distance<=5&&available(WardenAction.SLAM)&&lastAttack!=WardenAction.SLAM){setAction(WardenAction.SLAM);return;}
        if(distance>6&&distance<=22&&available(WardenAction.CHARGE)&&hasLineOfSight(target)
                &&(level().getGameTime()-lastCharge>=(phase()==2?120:180)||random.nextInt(3)==0)) { setAction(WardenAction.CHARGE);return; }
        var pool=new ArrayList<WardenAction>();
        if(distance<=SweepProfile.RADIUS)pool.add(WardenAction.SWEEP);
        if(distance<=5)pool.add(WardenAction.SLAM);
        pool.add(WardenAction.WAVE);
        if(phase()==2&&lastAttack!=WardenAction.DOUBLE_SWEEP&&lastAttack!=WardenAction.RESONANCE) {
            pool.add(WardenAction.RESONANCE);if(distance<=SweepProfile.RADIUS)pool.add(WardenAction.DOUBLE_SWEEP);
        }
        pool.removeIf(a->!available(a)); if(!pool.isEmpty())setAction(pool.get(random.nextInt(pool.size())));
    }
    public void trackHead(LivingEntity target) {
        getLookControl().setLookAt(target.getX(),target.getEyeY(),target.getZ(),10,8);
    }
    private void summonReinforcements(){
        var server=(net.minecraft.server.level.ServerLevel)level();
        int spawned=0;
        for(int slot=0;slot<4;slot++){
            var puppet=cn.suiyi.relicward.RelicWard.COURT_PUPPET.get().create(server);
            if(puppet==null)continue;
            boolean placed=false;
            for(int attempt=0;attempt<32&&!placed;attempt++){
                double angle=slot*Math.PI/2+(attempt%8)*Math.PI/16;
                double radius=5+(attempt/8)*3;
                Vec3 point=arenaCenter().add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
                for(int dy:new int[]{0,1,-1,2,-2,3,-3}){
                    BlockPos feet=BlockPos.containing(point).offset(0,dy,0);
                    if(!server.hasChunkAt(feet)||!server.getBlockState(feet.below()).isFaceSturdy(server,feet.below(),net.minecraft.core.Direction.UP))continue;
                    puppet.moveTo(feet.getX()+.5,feet.getY(),feet.getZ()+.5,getYRot(),0);
                    if(!server.noCollision(puppet)||server.containsAnyLiquid(puppet.getBoundingBox())
                            ||!server.getEntities(puppet,puppet.getBoundingBox()).isEmpty())continue;
                    puppet.bindReinforcement(this);
                    if(server.addFreshEntity(puppet)){
                        spawned++;placed=true;
                        server.sendParticles(net.minecraft.core.particles.ParticleTypes.WAX_ON,puppet.getX(),puppet.getY()+.6,puppet.getZ(),24,.5,.6,.5,.1);
                    }
                    break;
                }
            }
        }
        playSound(SoundEvents.IRON_GOLEM_REPAIR,1.5F,.65F);
        final int count=spawned;
        participants().forEach(p->p.displayClientMessage(Component.translatable("message.relicward.reinforcements",count),false));
    }
    private void clearReinforcements(){
        if(level() instanceof net.minecraft.server.level.ServerLevel server){
            // Iterate a snapshot because discard mutates the entity collection.
            List<CourtPuppet> summons=new ArrayList<>();
            for(Entity e:server.getAllEntities())if(e instanceof CourtPuppet p&&p.isReinforcementOf(this))summons.add(p);
            summons.forEach(Entity::discard);
        }
    }
    @Override public int getMaxHeadYRot(){return 55;}
    @Override public int getMaxHeadXRot(){return 35;}
    private void face(Vec3 target) {
        float yaw=(float)(Mth.atan2(target.z-getZ(),target.x-getX())*180/Math.PI)-90;
        setYRot(Mth.approachDegrees(getYRot(),yaw,8)); setYBodyRot(getYRot());
    }
    private List<LivingEntity> combatants(){
        var players=participants();List<LivingEntity> all=new ArrayList<>(players);
        all.addAll(level().getEntitiesOfClass(CourtPuppet.class,new AABB(arenaCenter(),arenaCenter()).inflate(22,12,22),p->p.isTame()&&p.isAlive()&&players.contains(p.getOwner())));
        return all;
    }
    public DamageSource resonanceDamage() {
        return namedDamage("resonance");
    }
    private DamageSource namedDamage(String name) {
        return new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("relicward",name))),this);
    }
    private void hit(LivingEntity player,float amount,boolean blockable) { hit(player,amount,blockable,hitThisPulse); }
    private void hit(LivingEntity player,float amount,boolean blockable,Set<UUID> hits) {
        if(!hits.add(player.getUUID()))return;
        float difficulty=level().getDifficulty()==Difficulty.EASY?.75F:level().getDifficulty()==Difficulty.HARD?1.2F:1;
        float pressure=amount*(phase()==2?1.6F:1.3F)+Math.min(6,player.getMaxHealth()*(phase()==2?.05F:.03F));
        if(player.hurt(blockable?namedDamage("impact"):resonanceDamage(),pressure*difficulty)){
            player.knockback(.45,getX()-player.getX(),getZ()-player.getZ());
            if(action()==WardenAction.WAVE||action()==WardenAction.RESONANCE)cn.suiyi.relicward.combat.ArmorFracture.apply(player);
        }
    }
    private void sweep(float amount) {
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP,1.5F,.6F);
        Vec3 spark=position().add(CombatMath.forward(getYRot()).scale(3));
        ((net.minecraft.server.level.ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,spark.x,spark.y+1.6,spark.z,2,.4,.2,.4,0);
        for(var p:combatants()) if(Math.abs(p.getY()-getY())<3.2&&hasLineOfSight(p)&&CombatMath.cone(position(),attackYaw(),p.position(),SweepProfile.RADIUS,SweepProfile.ANGLE))hit(p,amount,true);
    }
    private void blast(Vec3 pos,double radius,float amount) {
        if(action()==WardenAction.SLAM)groundCracks.begin((net.minecraft.server.level.ServerLevel)level(),getId(),pos,radius);
        playSound(SoundEvents.ANVIL_LAND,1.2F,.55F);
        var ground=level().getBlockState(BlockPos.containing(pos).below());
        ((net.minecraft.server.level.ServerLevel)level()).sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,ground),pos.x,pos.y+.1,pos.z,40,1.5,.1,1.5,.12);
        for(var p:combatants()) if(CombatMath.horizontalSquared(pos,p.position())<radius*radius&&Math.abs(p.getY()-pos.y)<3&&hasLineOfSight(p))hit(p,amount,false);
    }
    private void wave(int t,Set<UUID> hits) {
        if(t<0||t>36)return;
        if(t==0)playSound(SoundEvents.BELL_RESONATE,2,.65F);
        for(var p:combatants()) if(CombatMath.wave(aim(),p.position(),arenaCenter().y,2+t*.5)&&hasLineOfSight(p))hit(p,8,false,hits);
    }
    private void chargeStep() {
        Vec3 velocity=CombatMath.forward(getYRot()).scale(.65);
        for(int step=0;step<3;step++) {
            Vec3 from=position(),to=from.add(velocity.scale(1.0/3));
            var c=court();
            if(c!=null)for(int i=0;i<3;i++) {
                var pillarBox=new AABB(c.pillar(i).offset(-1,0,-1),c.pillar(i).offset(2,6,2));
                if(getBoundingBox().move(to.subtract(from)).intersects(pillarBox)&&c.breakPillar(i)) { stagger(); return; }
            }
            if(CombatMath.horizontalSquared(to,arenaCenter())>18*18||!level().noCollision(this,getBoundingBox().move(to.subtract(from)))) {
                setAction(WardenAction.RECOVER);return;
            }
            move(MoverType.SELF,to.subtract(from));
            for(var p:combatants()) if(Math.abs(p.getY()-getY())<5&&CombatMath.segmentDistanceSquared(from,to,p.position())<2.0*2.0)hit(p,12,false);
        }
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        boolean admin=source.is(DamageTypes.GENERIC_KILL);
        if(!admin&&(action()==WardenAction.DORMANT||action()==WardenAction.AWAKEN||action()==WardenAction.TRANSFORM))return false;
        float adjusted=action()==WardenAction.STAGGER?amount*1.25F:amount*(1-.1F*pillars());
        if(!admin){
            if(action()!=WardenAction.STAGGER)adjusted=Math.min(adjusted,phase()==2?24:30);

        }
        if(!admin&&adjusted<=0)return false;
        float before=getHealth(); boolean hit=super.hurt(source,admin?amount:adjusted);
        var attacker=source.getEntity();
        Player credited=attacker instanceof Player p?p:attacker instanceof CourtPuppet pet&&pet.isTame()&&pet.getOwner() instanceof Player owner?owner:null;
        if(hit&&credited!=null&&participants().contains(credited))damage.merge(credited.getUUID(),Math.max(0,before-getHealth()),Float::sum);
        return hit;
    }
    @Override protected float getDamageAfterArmorAbsorb(DamageSource source,float amount){
        return action()==WardenAction.STAGGER?amount:super.getDamageAfterArmorAbsorb(source,amount);
    }
    @Override protected float getDamageAfterMagicAbsorb(DamageSource source,float amount){
        float reduced=super.getDamageAfterMagicAbsorb(source,amount);
        // Clamp after armor/resistance so reduced damage still reaches the phase threshold.
        return phase()==1&&!source.is(DamageTypes.GENERIC_KILL)?Math.min(reduced,Math.max(0,getHealth()-getMaxHealth()*.5F)):reduced;
    }
    public Set<UUID> qualifiedPlayers() {
        Set<UUID> result=new HashSet<>();
        damage.forEach((id,d)->{if(d>=getMaxHealth()*.05F||(d>=1&&participation.getOrDefault(id,0)>=600))result.add(id);});
        return result;
    }
    @Override public void die(DamageSource source) {
        clearReinforcements();
        legitimateDeath=isEncounterActive()&&!source.is(DamageTypes.GENERIC_KILL);setAction(WardenAction.DYING);bar.setProgress(0);super.die(source);
    }
    @Override protected boolean shouldDropLoot(){return legitimateDeath;}
    @Override public int getExperienceReward(){return legitimateDeath?120:0;}
    @Override public boolean isCurrentlyGlowing(){return false;}
    @Override protected void tickDeath() {
        ++deathTime;
        if(deathTime>=80&&!level().isClientSide) {
            if(legitimateDeath&&court()!=null)court().victory(this);
            bar.removeAllPlayers();remove(RemovalReason.KILLED);
        }
    }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p);bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p);bar.removePlayer(p); }
    @Override public void remove(RemovalReason reason) { if(!level().isClientSide){if(reason.shouldDestroy())clearReinforcements();groundCracks.clear((net.minecraft.server.level.ServerLevel)level());bar.removeAllPlayers();}super.remove(reason); }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); if(altar!=null)tag.putLong("CourtAltar",altar.asLong()); if(arena!=null)tag.putLong("Arena",arena.asLong());
        tag.putBoolean("InterruptedEncounter",isEncounterActive()||action()==WardenAction.DYING);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); altar=tag.contains("CourtAltar")?BlockPos.of(tag.getLong("CourtAltar")):null;
        arena=tag.contains("Arena")?BlockPos.of(tag.getLong("Arena")):null;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(CombatMath.WARDEN_HEALTH);
        getAttribute(Attributes.ARMOR).setBaseValue(10);
        if(tag.getBoolean("InterruptedEncounter")){setHealth(getMaxHealth());deathTime=0;}
        entityData.set(ACTION,0);entityData.set(PHASE,1);bar.setVisible(false);
    }
    @Override protected float getStandingEyeHeight(Pose p,EntityDimensions d) { return 4.55F; }
    @Override public AABB getBoundingBoxForCulling() { return super.getBoundingBoxForCulling().inflate(24); }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
    @Override protected void playStepSound(BlockPos p,BlockState b) { playSound(SoundEvents.IRON_GOLEM_STEP,.7F,.65F); }
    @Override public boolean removeWhenFarAway(double d) { return false; }
}
