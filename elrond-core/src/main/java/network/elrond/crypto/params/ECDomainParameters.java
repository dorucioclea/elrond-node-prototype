package network.elrond.crypto.params;

import network.elrond.crypto.ecmath.ECAlgorithms;
import network.elrond.crypto.ecmath.ECConstants;
import network.elrond.crypto.ecmath.ECCurve;
import network.elrond.crypto.ecmath.ECPoint;
import network.elrond.crypto.util.Arrays;

import java.math.BigInteger;

public class ECDomainParameters
    implements ECConstants
{
    private ECCurve     curve;
    private byte[]      seed;
    private ECPoint     G;
    private BigInteger  n;
    private BigInteger  h;

    public ECDomainParameters(
        ECCurve     curve,
        ECPoint     G,
        BigInteger  n)
    {
        this(curve, G, n, ONE, null);
    }

    public ECDomainParameters(
        ECCurve     curve,
        ECPoint     G,
        BigInteger  n,
        BigInteger  h)
    {
        this(curve, G, n, h, null);
    }

    public ECDomainParameters(
        ECCurve     curve,
        ECPoint     G,
        BigInteger  n,
        BigInteger  h,
        byte[]      seed)
    {
        this.curve = curve;
        this.G = validate(curve, G);
        this.n = n;
        this.h = h;
        this.seed = seed;
    }

    public ECCurve getCurve()
    {
        return curve;
    }

    public ECPoint getG()
    {
        return G;
    }

    public BigInteger getN()
    {
        return n;
    }

    public BigInteger getH()
    {
        return h;
    }

    public byte[] getSeed()
    {
        return Arrays.clone(seed);
    }

    public boolean equals(
        Object  obj)
    {
        if (this == obj)
        {
            return true;
        }

        if ((obj instanceof ECDomainParameters))
        {
            ECDomainParameters other = (ECDomainParameters)obj;

            return this.curve.equals(other.curve) && this.G.equals(other.G) && this.n.equals(other.n) && this.h.equals(other.h);
        }

        return false;
    }

    public int hashCode()
    {
        int hc = curve.hashCode();
        hc *= 37;
        hc ^= G.hashCode();
        hc *= 37;
        hc ^= n.hashCode();
        hc *= 37;
        hc ^= h.hashCode();
        return hc;
    }

    static ECPoint validate(ECCurve c, ECPoint q)
    {
        if (q == null)
        {
            throw new IllegalArgumentException("point has null value");
        }

        if (q.isInfinity())
        {
            throw new IllegalArgumentException("point at infinity");
        }

        q = q.normalize();

        if (!q.isValid())
        {
            throw new IllegalArgumentException("point not on curve");
        }

        return ECAlgorithms.importPoint(c, q);
    }
}
