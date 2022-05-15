package top.nowandfuture.mod.imagesign.caches;

public class Vector3d {
    private double x, y, z;

    public Vector3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double distanceSq(Vector3d v){
        return this.distanceSq(v.x, v.y, v.z, true);
    }

    public double distanceSq(double x, double y, double z, boolean useCenter) {
        double d0 = useCenter ? 0.5D : 0.0D;
        double d1 = this.getX() + d0 - x;
        double d2 = this.getY() + d0 - y;
        double d3 = this.getZ() + d0 - z;
        return d1 * d1 + d2 * d2 + d3 * d3;
    }

    public double distance(Vector3d v){
        return Math.sqrt(distanceSq(v.x, v.y, v.z, false));
    }
}
