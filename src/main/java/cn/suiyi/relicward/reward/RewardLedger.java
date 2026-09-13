package cn.suiyi.relicward.reward;

import cn.suiyi.relicward.RelicContent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Rewards survive an offline player, a full inventory and later encounters. */
public final class RewardLedger extends SavedData {
    private final Map<String,CompoundTag> entries=new LinkedHashMap<>();
    public static RewardLedger get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(RewardLedger::load,RewardLedger::new,"relicward_rewards");
    }
    public static RewardLedger load(CompoundTag tag) {
        var data=new RewardLedger();
        for (var raw:tag.getList("Entries",10)) {
            var e=(CompoundTag)raw; data.entries.put(e.getString("Key"),e);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var list=new ListTag(); entries.values().forEach(e->list.add(e.copy())); tag.put("Entries",list); return tag;
    }
    public void award(UUID court,UUID encounter,UUID player) {
        String key=court+"/"+encounter+"/"+player;
        if(entries.containsKey(key)) return;
        var e=new CompoundTag(); e.putString("Key",key); e.putUUID("Court",court); e.putUUID("Player",player);
        var items=new ListTag();
        boolean trophy=entries.values().stream().noneMatch(old->old.hasUUID("Player")&&old.getUUID("Player").equals(player)&&old.getBoolean("TrophyAwarded"));
        if(trophy){items.add(new ItemStack(RelicContent.TROPHY.get()).save(new CompoundTag()));e.putBoolean("TrophyAwarded",true);}
        e.put("Items",items); e.putInt("XP",0); entries.put(key,e); setDirty();
    }
    public boolean hasPending(UUID court,UUID player) {
        return entries.values().stream().anyMatch(e->matches(e,court,player)&&!e.getBoolean("Claimed"));
    }
    private boolean matches(CompoundTag e,UUID court,UUID player) { return (court==null||e.getUUID("Court").equals(court))&&e.getUUID("Player").equals(player); }
    public int deliverAll(ServerPlayer player){return claim(null,player);}
    public int claim(UUID court,ServerPlayer player) {
        int delivered=0;
        for(var e:entries.values()) {
            if(!matches(e,court,player.getUUID())||e.getBoolean("Claimed")) continue;
            var remaining=new ListTag();
            for(var raw:e.getList("Items",10)) {
                var stack=ItemStack.of((CompoundTag)raw); int before=stack.getCount();
                player.getInventory().add(stack); delivered+=before-stack.getCount();
                if(!stack.isEmpty()) remaining.add(stack.save(new CompoundTag()));
            }
            e.put("Items",remaining);
            if(e.getInt("XP")>0){player.giveExperiencePoints(e.getInt("XP"));e.putInt("XP",0);}
            if(!e.getBoolean("ProgressAwarded")){
                var advancement=player.server.getAdvancements().getAdvancement(new ResourceLocation("relicward","bell_silenced"));
                if(advancement!=null)player.getAdvancements().award(advancement,"victory");e.putBoolean("ProgressAwarded",true);
            }
            if(remaining.isEmpty()) {
                e.putBoolean("Claimed",true);
            }
            setDirty();
        }
        player.containerMenu.broadcastChanges(); return delivered;
    }
}
