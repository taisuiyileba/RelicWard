package cn.suiyi.relicward.client;

import cn.suiyi.relicward.ClientPreferences;
import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.entity.BellWarden;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid=RelicWard.ID,value=Dist.CLIENT)
public final class WardenBattleMusic {
    private static final List<Track> TRACKS=new ArrayList<>();
    private static Track current;
    private static Object lastLevel;
    private static int retryTicks;
    @SubscribeEvent public static void backgroundMusic(net.minecraftforge.client.event.sound.PlaySoundEvent event) {
        if(current!=null&&event.getSound()!=null&&event.getSound().getSource()==SoundSource.MUSIC
                &&!(event.getSound() instanceof Track))event.setSound(null);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(lastLevel!=mc.level) {
            TRACKS.forEach(t->mc.getSoundManager().stop(t));TRACKS.clear();current=null;lastLevel=mc.level;retryTicks=0;
        }
        if(mc.isPaused())return;
        BellWarden selected=null;
        if(mc.level!=null&&mc.player!=null&&mc.player.isAlive()&&ClientPreferences.BOSS_MUSIC.get()
                &&ClientPreferences.BOSS_MUSIC_VOLUME.get()>0&&mc.options.getSoundSourceVolume(SoundSource.MUSIC)>0
                &&mc.options.getSoundSourceVolume(SoundSource.MASTER)>0) {
            double nearest=48*48;
            for(var entity:mc.level.entitiesForRendering())if(entity instanceof BellWarden w&&w.isAlive()&&w.isEncounterActive()) {
                double d=w.distanceToSqr(mc.player);
                if(d<nearest){selected=w;nearest=d;}
            }
            // Keep the same encounter while it remains in range, avoiding music churn between two bosses.
            if(current!=null)for(var entity:mc.level.entitiesForRendering())
                if(entity instanceof BellWarden w&&w.getUUID().equals(current.boss)&&w.isAlive()&&w.isEncounterActive()&&w.distanceToSqr(mc.player)<48*48){selected=w;break;}
        }
        if(current!=null&&(selected==null||!selected.getUUID().equals(current.boss)||selected.phase()!=current.phase)) {
            current.fading=true;current=null;retryTicks=0;
        }
        TRACKS.removeIf(t->t.isStopped()||(t!=current&&!mc.getSoundManager().isActive(t)));
        if(selected!=null) {
            if(current!=null&&!mc.getSoundManager().isActive(current)&&--retryTicks<=0){TRACKS.remove(current);current=null;}
            if(current==null){mc.getMusicManager().stopPlaying();current=new Track(selected);TRACKS.add(current);mc.getSoundManager().play(current);retryTicks=40;}
        }
    }
    private static final class Track extends AbstractTickableSoundInstance {
        final UUID boss;final int phase;boolean fading;float gain;
        Track(BellWarden w) {
            super(SoundEvent.createVariableRangeEvent(new ResourceLocation(RelicWard.ID,"music.bell_warden_"+w.phase())),SoundSource.MUSIC,RandomSource.create());
            boss=w.getUUID();phase=w.phase();looping=true;relative=true;attenuation=Attenuation.NONE;volume=0;
        }
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){
            gain=fading?Math.max(0,gain-0.025F):Math.min(1,gain+0.02F);
            volume=gain*ClientPreferences.BOSS_MUSIC_VOLUME.get().floatValue();
            if(fading&&gain<=0)stop();
        }
    }
}
