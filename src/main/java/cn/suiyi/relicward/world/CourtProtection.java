package cn.suiyi.relicward.world;

import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.block.CourtAltarEntity;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Authoritative, persistent arena bounds, indexed by chunk; no per-break block scanning. */
@Mod.EventBusSubscriber(modid=RelicWard.ID)
public final class CourtProtection extends SavedData {
    private record Region(BlockPos altar,BoundingBox box){}
    private final Map<Long,Region> regions=new HashMap<>();
    private final Map<Long,List<Region>> chunks=new HashMap<>();
    private static final Map<Level,CourtProtection> CLIENT=Collections.synchronizedMap(new WeakHashMap<>());
    public static CourtProtection get(ServerLevel level){return level.getDataStorage().computeIfAbsent(CourtProtection::load,CourtProtection::new,"relicward_court_protection");}
    public static void register(CourtAltarEntity altar){
        Level level=altar.getLevel();if(level==null)return;
        var data=level instanceof ServerLevel s?get(s):CLIENT.computeIfAbsent(level,l->new CourtProtection());
        BlockPos a=altar.local(-32,-5,-24),b=altar.local(32,37,23);
        BoundingBox box=new BoundingBox(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()),Math.max(a.getX(),b.getX()),Math.max(a.getY(),b.getY()),Math.max(a.getZ(),b.getZ()));
        data.add(altar.getBlockPos(),box);
    }
    private void add(BlockPos altar,BoundingBox box){
        var old=regions.get(altar.asLong());if(old!=null&&old.box.equals(box))return;
        regions.put(altar.asLong(),new Region(altar.immutable(),box));reindex();setDirty();
    }
    private void reindex(){
        chunks.clear();for(var r:regions.values())for(int x=r.box.minX()>>4;x<=r.box.maxX()>>4;x++)for(int z=r.box.minZ()>>4;z<=r.box.maxZ()>>4;z++)
            chunks.computeIfAbsent(ChunkPos.asLong(x,z),key->new ArrayList<>()).add(r);
    }
    public static void unregister(Level level,BlockPos pos){
        var data=level instanceof ServerLevel s?get(s):CLIENT.get(level);
        if(data!=null&&data.regions.remove(pos.asLong())!=null){data.reindex();data.setDirty();}
    }
    public static CourtProtection load(CompoundTag tag){
        var data=new CourtProtection();for(var raw:tag.getList("Courts",10)){
            var t=(CompoundTag)raw;int[] b=t.getIntArray("Bounds");if(b.length==6)data.regions.put(t.getLong("Altar"),new Region(BlockPos.of(t.getLong("Altar")),new BoundingBox(b[0],b[1],b[2],b[3],b[4],b[5])));
        }data.reindex();return data;
    }
    @Override public CompoundTag save(CompoundTag tag){
        var list=new ListTag();for(var r:regions.values()){
            var t=new CompoundTag();t.putLong("Altar",r.altar.asLong());var b=r.box;t.putIntArray("Bounds",new int[]{b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ()});list.add(t);
        }tag.put("Courts",list);return tag;
    }
    public boolean contains(BlockPos pos){
        for(var r:chunks.getOrDefault(new ChunkPos(pos).toLong(),List.of()))if(r.box.isInside(pos))return true;return false;
    }
    public static boolean protects(LevelAccessor level,BlockPos pos){
        CourtProtection data=level instanceof ServerLevel s?get(s):level instanceof Level l?CLIENT.get(l):null;
        return data!=null&&data.contains(pos);
    }
    public static boolean playerBlocked(LevelAccessor level,BlockPos pos,Player player){return !player.isCreative()&&protects(level,pos);}
    private static void notice(Player p){if(!p.level().isClientSide)p.displayClientMessage(Component.translatable("message.relicward.protected"),true);}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void breaking(BlockEvent.BreakEvent e){if(playerBlocked(e.getLevel(),e.getPos(),e.getPlayer())){e.setCanceled(true);notice(e.getPlayer());}}
    @SubscribeEvent public static void speed(PlayerEvent.BreakSpeed e){e.getPosition().ifPresent(p->{if(playerBlocked(e.getEntity().level(),p,e.getEntity())){e.setNewSpeed(0);e.setCanceled(true);}});}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void placing(BlockEvent.EntityPlaceEvent e){
        if(e.getEntity() instanceof Player p&&p.isCreative())return;
        boolean blocked=protects(e.getLevel(),e.getPos());
        if(e instanceof BlockEvent.EntityMultiPlaceEvent multi)blocked|=multi.getReplacedBlockSnapshots().stream().anyMatch(s->protects(e.getLevel(),s.getPos()));
        if(blocked){e.setCanceled(true);if(e.getEntity() instanceof Player p)notice(p);}
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public static void explosion(ExplosionEvent.Detonate e){e.getAffectedBlocks().removeIf(p->protects(e.getLevel(),p));}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void piston(PistonEvent.Pre e){
        if(protects(e.getLevel(),e.getPos())||protects(e.getLevel(),e.getFaceOffsetPos())){e.setCanceled(true);return;}
        var resolver=e.getStructureHelper();if(resolver==null||!resolver.resolve())return;
        var dir=e.getPistonMoveType().isExtend?e.getDirection():e.getDirection().getOpposite();
        if(resolver.getToPush().stream().anyMatch(p->protects(e.getLevel(),p)||protects(e.getLevel(),p.relative(dir)))||resolver.getToDestroy().stream().anyMatch(p->protects(e.getLevel(),p)))e.setCanceled(true);
    }
    @SubscribeEvent public static void grief(EntityMobGriefingEvent e){if(protects(e.getEntity().level(),e.getEntity().blockPosition()))e.setResult(Event.Result.DENY);}
    @SubscribeEvent public static void tool(BlockEvent.BlockToolModificationEvent e){if(e.getPlayer()!=null&&playerBlocked(e.getLevel(),e.getPos(),e.getPlayer()))e.setCanceled(true);}
    @SubscribeEvent public static void fluids(BlockEvent.FluidPlaceBlockEvent e){if(protects(e.getLevel(),e.getPos()))e.setCanceled(true);}
    @SubscribeEvent public static void hazardousUse(PlayerInteractEvent.RightClickBlock e){
        if(e.getEntity().isCreative())return;var stack=e.getItemStack();
        if((stack.getItem() instanceof BucketItem||stack.is(Items.FLINT_AND_STEEL)||stack.is(Items.FIRE_CHARGE))&&(protects(e.getLevel(),e.getPos())||protects(e.getLevel(),e.getPos().relative(e.getFace()==null?net.minecraft.core.Direction.UP:e.getFace())))){
            e.setCanceled(true);notice(e.getEntity());
        }
    }
}
