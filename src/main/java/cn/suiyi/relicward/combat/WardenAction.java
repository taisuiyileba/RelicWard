package cn.suiyi.relicward.combat;

/** All combat durations are server ticks. Windup ends before the first damaging tick. */
public enum WardenAction {
    DORMANT(0,0,0), AWAKEN(60,60,0), IDLE(0,0,0),
    SWEEP(18,44,80), SLAM(24,60,140), WAVE(22,88,160), CHARGE(30,104,200),
    DOUBLE_SWEEP(16,65,140), RESONANCE(32,126,240),
    STAGGER(0,100,0), TRANSFORM(50,50,0), DYING(0,80,0), HIGH_STRIKE(24,60,120), RECOVER(0,40,0);
    public final int windup, duration, cooldown;
    WardenAction(int windup, int duration, int cooldown) {
        this.windup=windup; this.duration=duration; this.cooldown=cooldown;
    }
    public boolean attack() { return cooldown>0; }
}
