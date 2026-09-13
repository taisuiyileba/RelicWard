package cn.suiyi.relicward.combat;

/** Server hit geometry and client telegraphs consume the same constants and windup windows. */
public final class SweepProfile {
    public static final double RADIUS=4.5,ANGLE=140;
    public static int contact(WardenAction action,int swing){return action==WardenAction.SWEEP?18:swing==0?16:29;}
    public static int warningStart(WardenAction action,int swing){return swing==0?0:20;}
    public static boolean tracks(WardenAction action,int t){
        if(action==WardenAction.SWEEP)return t<10;
        if(action==WardenAction.DOUBLE_SWEEP)return t<8||(t>=20&&t<25);
        return false;
    }
}
