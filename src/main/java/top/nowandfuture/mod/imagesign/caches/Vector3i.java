package top.nowandfuture.mod.imagesign.caches;

public class Vector3i {
    // copy from vector3i
    private static final int NUM_X_BITS = 1 + log2(smallestEncompassingPowerOfTwo(30000000));
    private static final int NUM_Z_BITS = NUM_X_BITS;
    private static final int NUM_Y_BITS = 64 - NUM_X_BITS - NUM_Z_BITS;

    private static final int INVERSE_START_BITS_Z = NUM_Y_BITS;
    private static final int INVERSE_START_BITS_X = NUM_Y_BITS + NUM_Z_BITS;
    private static final long X_MASK = (1L << NUM_X_BITS) - 1L;
    private static final long Y_MASK = (1L << NUM_Y_BITS) - 1L;
    private static final long Z_MASK = (1L << NUM_Z_BITS) - 1L;
    private int x, y, z;

    public Vector3i(double x, double y, double z) {
        this(((int) x), ((int) y), ((int) z));
    }

    public Vector3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public static int unpackX(long packedPos) {
        return (int)(packedPos << 64 - INVERSE_START_BITS_X - NUM_X_BITS >> 64 - NUM_X_BITS);
    }

    public static int unpackY(long packedPos) {
        return (int)(packedPos << 64 - NUM_Y_BITS >> 64 - NUM_Y_BITS);
    }

    public static int unpackZ(long packedPos) {
        return (int)(packedPos << 64 - INVERSE_START_BITS_Z - NUM_Z_BITS >> 64 - NUM_Z_BITS);
    }

    public static Vector3i fromLong(long packedPos) {
        return new Vector3i(unpackX(packedPos), unpackY(packedPos), unpackZ(packedPos));
    }

    public long toLong() {
        return pack(this.getX(), this.getY(), this.getZ());
    }

    public static long pack(int x, int y, int z) {
        long i = 0L;
        i = i | ((long)x & X_MASK) << INVERSE_START_BITS_X;
        i = i | ((long) y & Y_MASK);
        return i | ((long)z & Z_MASK) << INVERSE_START_BITS_Z;
    }

    public double distanceSq(Vector3i v){
        return this.distanceSq(v.x, v.y, v.z);
    }

    public double distanceSq(double x, double y, double z) {
        return distanceSq(x, y, z, true);
    }

    public double distanceSq(double x, double y, double z, boolean useCenter) {
        double d0 = useCenter ? 0.5D : 0.0D;
        double d1 = this.getX() + d0 - x;
        double d2 = this.getY() + d0 - y;
        double d3 = this.getZ() + d0 - z;
        return d1 * d1 + d2 * d2 + d3 * d3;
    }

    public static int smallestEncompassingPowerOfTwo(int pValue) {
        int i = pValue - 1;
        i |= i >> 1;
        i |= i >> 2;
        i |= i >> 4;
        i |= i >> 8;
        i |= i >> 16;
        return i + 1;
    }

    public static int log2(int N)
    {
        int count = 0;
        while (N > 0){
            N >>= 1;
            count ++;
        }
        return count - 1;
    }
}
