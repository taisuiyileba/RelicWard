package cn.suiyi.relicward.combat;

import net.minecraft.world.phys.Vec3;

public final class CombatMath {
    private CombatMath() {}
    public static double horizontalSquared(Vec3 a, Vec3 b) {
        double x=a.x-b.x, z=a.z-b.z; return x*x+z*z;
    }
    public static Vec3 forward(float yaw) {
        double r=Math.toRadians(yaw); return new Vec3(-Math.sin(r),0,Math.cos(r));
    }
    public static boolean cone(Vec3 origin, float yaw, Vec3 point, double radius, double degrees) {
        Vec3 delta=new Vec3(point.x-origin.x,0,point.z-origin.z);
        if (delta.lengthSqr()>radius*radius) return false;
        return delta.lengthSqr()<0.04 || delta.normalize().dot(forward(yaw)) >= Math.cos(Math.toRadians(degrees/2));
    }
    public static boolean wave(Vec3 origin, Vec3 feet, double floor, double radius) {
        double d=Math.sqrt(horizontalSquared(origin,feet));
        return Math.abs(d-radius)<=0.65 && feet.y>=floor-0.5 && feet.y<floor+0.65;
    }
    public static double segmentDistanceSquared(Vec3 a, Vec3 b, Vec3 point) {
        double dx=b.x-a.x, dz=b.z-a.z, length=dx*dx+dz*dz;
        double t=length<1e-8 ? 0 : Math.max(0,Math.min(1,((point.x-a.x)*dx+(point.z-a.z)*dz)/length));
        double x=a.x+dx*t-point.x, z=a.z+dz*t-point.z; return x*x+z*z;
    }
    public static final float WARDEN_HEALTH=480;
    public static double healthForPlayers(int count) { return WARDEN_HEALTH*(1+0.6*(Math.max(1,Math.min(count,4))-1)); }
}
