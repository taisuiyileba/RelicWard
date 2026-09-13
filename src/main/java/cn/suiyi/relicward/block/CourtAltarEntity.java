package cn.suiyi.relicward.block;

import cn.suiyi.relicward.RelicContent;
import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.entity.BellWarden;
import cn.suiyi.relicward.reward.RewardLedger;
import cn.suiyi.relicward.reward.AutomaticRewards;
import cn.suiyi.relicward.world.CourtProtection;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CourtAltarEntity extends BlockEntity {
    private UUID courtId=UUID.randomUUID(), bossId;
    private boolean completed, puppetsPlaced,active;
    private long readyAt,respawnAt;
    private boolean floorChecked;
    private final cn.suiyi.relicward.world.CourtPopulation population=new cn.suiyi.relicward.world.CourtPopulation();
    public cn.suiyi.relicward.world.CourtPopulation population(){return population;}
    private int loadDelay=100;
    public CourtAltarEntity(BlockPos p,BlockState s) { super(RelicContent.ALTAR_ENTITY.get(),p,s); }
    public UUID courtId() { return courtId; }
    public boolean completed() { return completed; }
    public boolean active(){return active;}
    public long readyAt(){return readyAt;}
    public long respawnAt(){return respawnAt;}
    @Override public void onLoad(){super.onLoad();CourtProtection.register(this);}
    private void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public AABB getRenderBoundingBox(){return new AABB(center()).inflate(24,12,24).minmax(new AABB(worldPosition).inflate(2));}
    public BlockPos center() { return worldPosition.relative(getBlockState().getValue(CourtAltarBlock.FACING).getOpposite(),17); }
    public BlockPos local(int x,int y,int z) {
        Direction forward=getBlockState().getValue(CourtAltarBlock.FACING),right=forward.getCounterClockWise();
        return center().offset(right.getStepX()*x+forward.getStepX()*z,y,right.getStepZ()*x+forward.getStepZ()*z);
    }
    public BlockPos pillar(int index) { return switch(index) { case 0->local(0,0,-13); case 1->local(-11,0,7); default->local(11,0,7); }; }
    public int intactPillars() {
        int count=0;
        for(int i=0;i<3;i++) { var s=level.getBlockState(pillar(i)); if(s.is(RelicContent.PILLAR.get())&&!s.getValue(ResonantPillarBlock.BROKEN)) count++; }
        return count;
    }
    public boolean breakPillar(int i) {
        BlockPos p=pillar(i); var state=level.getBlockState(p);
        if(!state.is(RelicContent.PILLAR.get())||state.getValue(ResonantPillarBlock.BROKEN)) return false;
        setPillar(i,true);
        ((ServerLevel)level).sendParticles(ParticleTypes.WAX_OFF,p.getX()+.5,p.getY()+2,p.getZ()+.5,45,1,2,1,.1);
        level.playSound(null,p,SoundEvents.ANVIL_BREAK,SoundSource.HOSTILE,1.4F,.55F);
        setChanged(); return true;
    }
    private void setPillar(int i,boolean broken) {
        BlockPos anchor=pillar(i);
        for(BlockPos p:BlockPos.betweenClosed(anchor.offset(-1,0,-1),anchor.offset(1,5,1))) {
            var state=level.getBlockState(p);
            if(state.is(RelicContent.PILLAR.get())) level.setBlock(p,state.setValue(ResonantPillarBlock.BROKEN,broken),3);
            else if(!broken&&(state.isAir()||!state.getFluidState().isEmpty()))level.setBlock(p,RelicContent.PILLAR.get().defaultBlockState(),3);
        }
    }
    public void restorePillars() { for(int i=0;i<3;i++) setPillar(i,false); setChanged(); }
    @Nullable public BellWarden boss() {
        if(!(level instanceof ServerLevel sl)) return null;
        return bossId!=null && sl.getEntity(bossId) instanceof BellWarden w ? w : null;
    }
    public static void tick(Level level,BlockPos p,BlockState s,CourtAltarEntity altar) {
        if(altar.loadDelay-- >0 || level.getGameTime()%20!=0) return;
        if(!level.hasChunkAt(altar.center())||!level.hasChunkAt(altar.local(-20,0,-20))||!level.hasChunkAt(altar.local(20,0,20))) return;
        if(level.getNearestPlayer(p.getX(),p.getY(),p.getZ(),72,false)==null) return;
        if(!altar.floorChecked){altar.repairMissingFloor();altar.repairOfferingSupports();altar.repairEntrance();altar.floorChecked=true;}
        altar.advanceRespawn();
        var bounds=new AABB(altar.center()).inflate(26,12,26);
        var found=level.getEntitiesOfClass(BellWarden.class,bounds,w->p.equals(w.altarPos()));
        BellWarden current=altar.boss();
        boolean activeNow=current!=null&&current.isEncounterActive();if(activeNow!=altar.active){altar.active=activeNow;altar.sync();}
        if(current==null&&!found.isEmpty()) { current=found.get(0); altar.bossId=current.getUUID(); altar.setChanged(); }
        for(var extra:found) if(current!=extra) extra.discard();
        if(current==null&&!altar.completed) altar.ensureBoss();
        altar.population.tick(altar);
    }
    @Nullable public BellWarden ensureBoss() {
        if(completed||!(level instanceof ServerLevel sl)) return null;
        var existing=boss(); if(existing!=null) return existing;
        var mob=RelicWard.BELL_WARDEN.get().create(sl); if(mob==null) return null;
        var c=center(); mob.moveTo(c.getX()+.5,c.getY(),c.getZ()+.5,getBlockState().getValue(CourtAltarBlock.FACING).toYRot(),0);
        mob.bindCourt(worldPosition,c); restorePillars();
        sl.addFreshEntity(mob); bossId=mob.getUUID(); setChanged(); return mob;
    }
    @Nullable public BlockPos obstruction() {
        for(int x=-18;x<=18;x++) for(int z=-18;z<=18;z++) {
            if(x*x+z*z>18*18) continue;
            var p=local(x,-1,z);
            if(level.getBlockState(p).getCollisionShape(level,p).isEmpty()) return p;
        }
        for(int i=0;i<3;i++) {
            var a=pillar(i);
            for(BlockPos p:BlockPos.betweenClosed(a.offset(-1,0,-1),a.offset(1,5,1)))
                if(!level.getBlockState(p).is(RelicContent.PILLAR.get())) return p.immutable();
        }
        for(int i=-1;i<3;i++) {
            Vec3 from=Vec3.atBottomCenterOf(center());
            Vec3 to=i<0?from:Vec3.atBottomCenterOf(pillar(i));
            int steps=Math.max(1,(int)from.distanceTo(to));
            for(int j=0;j<=steps;j++) {
                var q=BlockPos.containing(from.lerp(to,(double)j/steps));
                for(BlockPos p:BlockPos.betweenClosed(q.offset(-1,0,-1),q.offset(1,5,1))) {
                    var s=level.getBlockState(p);
                    if(!s.is(RelicContent.PILLAR.get())&&!s.getCollisionShape(level,p).isEmpty()) return p.immutable();
                }
            }
        }
        return null;
    }
    public void repairEntrance(){
        if(level==null||level.isClientSide)return;
        for(int x=-6;x<=6;x++)for(int z=54;z<=56;z++){
            var step=local(x,52-z,z);
            if(!level.hasChunkAt(step)||!level.getBlockState(step).is(net.minecraft.world.level.block.Blocks.STONE_BRICK_STAIRS))continue;
            for(int dy=1;dy<=4;dy++){
                var at=step.above(dy);var s=level.getBlockState(at);
                if(s.is(net.minecraft.world.level.block.Blocks.STONE_BRICKS)||s.is(net.minecraft.world.level.block.Blocks.CRACKED_STONE_BRICKS)||s.is(net.minecraft.world.level.block.Blocks.MOSSY_STONE_BRICKS))level.setBlock(at,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            }
        }
    }
    public void repairOfferingSupports(){
        if(level==null||level.isClientSide)return;
        for(int x:new int[]{-17,-15,15,17})for(int z:new int[]{-16,16})repairSupport(local(x,1,z));
        for(int x:new int[]{-27,27})for(int z:new int[]{-12,-1,10})repairSupport(local(x,1,z));
    }
    private void repairSupport(BlockPos decoration){
        if(!level.hasChunkAt(decoration))return;
        var top=level.getBlockState(decoration);var p=decoration.below();var support=level.getBlockState(p);
        if((top.getBlock() instanceof net.minecraft.world.level.block.CandleBlock||top.getBlock() instanceof net.minecraft.world.level.block.FlowerPotBlock)
            &&support.getBlock() instanceof net.minecraft.world.level.block.SlabBlock
            &&support.getValue(net.minecraft.world.level.block.SlabBlock.TYPE)==net.minecraft.world.level.block.state.properties.SlabType.BOTTOM)
            level.setBlock(p,support.setValue(net.minecraft.world.level.block.SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.TOP),3);
    }
    public void repairMissingFloor(){
        if(level==null||level.isClientSide)return;
        for(int x=-18;x<=18;x++)for(int z=-18;z<=18;z++){
            double radius=Math.sqrt(x*x+z*z);if(radius>18)continue;
            BlockPos p=local(x,-1,z);var current=level.getBlockState(p);
            if(!current.isAir()&&current.getFluidState().isEmpty())continue;
            var replacement=net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState();
            if(radius>17.2||radius<3||Math.abs(radius-10)<.45)replacement=RelicContent.BRICKS.get().defaultBlockState();
            else if(x==0||z==0)replacement=net.minecraft.world.level.block.Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            if(radius>3.5&&radius<16&&(Math.abs(x)==Math.abs(z)||((int)radius%4==0&&(x+z+56)%7==0)))replacement=net.minecraft.world.level.block.Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            level.setBlock(p,replacement,3);
        }
        for(int i=0;i<3;i++){var a=pillar(i);for(BlockPos p:BlockPos.betweenClosed(a.offset(-1,0,-1),a.offset(1,5,1)))
            if(level.getBlockState(p).isAir()||!level.getBlockState(p).getFluidState().isEmpty())level.setBlock(p,RelicContent.PILLAR.get().defaultBlockState(),3);}
    }
    public void advanceRespawn(){
        if(!completed||level==null||level.isClientSide)return;
        long now=level.getGameTime();
        if(respawnAt==0&&now>=readyAt-60){respawnAt=now+60;sync();level.playSound(null,worldPosition,SoundEvents.BEACON_ACTIVATE,SoundSource.BLOCKS,.7F,.8F);}
        if(respawnAt>0&&now>=respawnAt){
            completed=false;respawnAt=0;bossId=null;repairMissingFloor();restorePillars();ensureBoss();sync();
            ((ServerLevel)level).sendParticles(ParticleTypes.END_ROD,center().getX()+.5,center().getY()+2,center().getZ()+.5,45,1.5,2,1.5,.025);
            level.playSound(null,center(),SoundEvents.BELL_RESONATE,SoundSource.BLOCKS,1.5F,1);
        }
    }
    public static int countFragments(Player player) {
        int count = 0;
        for(ItemStack stack : player.getInventory().items) {
            if(stack.is(RelicContent.FRAGMENT.get())) count += stack.getCount();
        }
        for(ItemStack stack : player.getInventory().offhand) {
            if(stack.is(RelicContent.FRAGMENT.get())) count += stack.getCount();
        }
        return count;
    }
    public static boolean consumeFragments(Player player, int amount) {
        if(countFragments(player) < amount) return false;
        int remaining = amount;
        ItemStack main = player.getMainHandItem();
        if(main.is(RelicContent.FRAGMENT.get())) {
            int take = Math.min(remaining, main.getCount());
            main.shrink(take);
            remaining -= take;
        }
        if(remaining > 0) {
            ItemStack off = player.getOffhandItem();
            if(off.is(RelicContent.FRAGMENT.get())) {
                int take = Math.min(remaining, off.getCount());
                off.shrink(take);
                remaining -= take;
            }
        }
        if(remaining > 0) {
            for(ItemStack stack : player.getInventory().items) {
                if(stack == main) continue;
                if(stack.is(RelicContent.FRAGMENT.get())) {
                    int take = Math.min(remaining, stack.getCount());
                    stack.shrink(take);
                    remaining -= take;
                    if(remaining <= 0) break;
                }
            }
        }
        player.containerMenu.broadcastChanges();
        return true;
    }
    public void interact(Player player) {
        if(!(player instanceof ServerPlayer sp)||!(level instanceof ServerLevel sl)) return;
        AutomaticRewards.deliver(sp);
        if(completed) {
            advanceRespawn();
            if(completed) {
                boolean holdingFragment = player.getMainHandItem().is(RelicContent.FRAGMENT.get()) || player.getOffhandItem().is(RelicContent.FRAGMENT.get());
                if(holdingFragment) {
                    if(sp.isCreative() || sp.isSpectator() || level.getDifficulty() == Difficulty.PEACEFUL) {
                        sp.displayClientMessage(Component.translatable("message.relicward.survival_required"), false);
                        return;
                    }
                    int fragments = countFragments(player);
                    if(fragments < 5) {
                        sp.displayClientMessage(Component.translatable("message.relicward.need_fragments", fragments), true);
                        return;
                    }
                    repairMissingFloor();
                    var blocked = obstruction();
                    if(blocked != null) {
                        sp.displayClientMessage(Component.translatable("message.relicward.obstructed", blocked.getX(), blocked.getY(), blocked.getZ()), false);
                        sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, blocked.getX() + .5, blocked.getY() + .5, blocked.getZ() + .5, 12, .2, .2, .2, 0);
                        return;
                    }
                    consumeFragments(player, 5);
                    completed = false;
                    respawnAt = 0;
                    readyAt = 0;
                    if(bossId != null && sl.getEntity(bossId) instanceof BellWarden oldBoss) oldBoss.discard();
                    bossId = null;
                    repairMissingFloor();
                    restorePillars();
                    var warden = ensureBoss();
                    sync();
                    sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, worldPosition.getX() + .5, worldPosition.getY() + 1.2, worldPosition.getZ() + .5, 30, .4, .4, .4, .1);
                    sl.sendParticles(ParticleTypes.END_ROD, center().getX() + .5, center().getY() + 2, center().getZ() + .5, 45, 1.5, 2, 1.5, .025);
                    level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.2F, 1.2F);
                    level.playSound(null, center(), SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.5F, 1.0F);
                    if(warden != null) {
                        warden.startEncounter();
                        sp.displayClientMessage(Component.translatable("message.relicward.fragment_restart"), false);
                    }
                    return;
                }
                long seconds = Math.max(0, (readyAt - sl.getGameTime() + 19) / 20);
                sp.displayClientMessage(Component.translatable(respawnAt > 0 ? "message.relicward.resummoning" : "message.relicward.cooldown", seconds), true);
                return;
            }
        }
        repairMissingFloor();
        var warden=ensureBoss(); if(warden==null) return;
        if(warden.isEncounterActive()) { sp.displayClientMessage(Component.translatable("message.relicward.already_active"),true); return; }
        if(sp.isCreative()||sp.isSpectator()||level.getDifficulty()==Difficulty.PEACEFUL) {
            sp.displayClientMessage(Component.translatable("message.relicward.survival_required"),false); return;
        }
        var blocked=obstruction();
        if(blocked!=null) {
            sp.displayClientMessage(Component.translatable("message.relicward.obstructed",blocked.getX(),blocked.getY(),blocked.getZ()),false);
            sl.sendParticles(ParticleTypes.ANGRY_VILLAGER,blocked.getX()+.5,blocked.getY()+.5,blocked.getZ()+.5,12,.2,.2,.2,0); return;
        }
        restorePillars(); warden.startEncounter();
    }
    public void victory(BellWarden warden) {
        if(completed||bossId==null||!bossId.equals(warden.getUUID())) return;
        completed=true; readyAt=level.getGameTime()+6000;respawnAt=0;sync();
        var sl=(ServerLevel)level; var ledger=RewardLedger.get(sl);
        for(UUID player:warden.qualifiedPlayers()){
            ledger.award(courtId,warden.encounterId(),player);
            var online=sl.getServer().getPlayerList().getPlayer(player);
            if(online==null&&sl.getPlayerByUUID(player) instanceof ServerPlayer found)online=found;
            if(online!=null)AutomaticRewards.deliver(online);
        }
        sl.playSound(null,center(),SoundEvents.BELL_RESONATE,SoundSource.BLOCKS,2,1.3F);
        for(var p:sl.players()) if(p.distanceToSqr(Vec3.atCenterOf(center()))<80*80)
            p.displayClientMessage(Component.translatable("message.relicward.victory"),false);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);population.save(tag); tag.putUUID("Court",courtId); if(bossId!=null)tag.putUUID("Boss",bossId);
        tag.putBoolean("Completed",completed);tag.putBoolean("Active",active); tag.putBoolean("Puppets",puppetsPlaced); tag.putLong("ReadyAt",readyAt);tag.putLong("RespawnAt",respawnAt);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);population.load(tag); if(tag.hasUUID("Court"))courtId=tag.getUUID("Court"); bossId=tag.hasUUID("Boss")?tag.getUUID("Boss"):null;
        completed=tag.getBoolean("Completed");active=tag.getBoolean("Active"); puppetsPlaced=tag.getBoolean("Puppets"); readyAt=tag.getLong("ReadyAt");respawnAt=tag.getLong("RespawnAt"); loadDelay=100;floorChecked=false;
    }
}
