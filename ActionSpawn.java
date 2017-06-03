import javax.vecmath.Vector3d;

/**
 * Spawn off another creature similar to me.
 */
public class ActionSpawn
    implements Action
{
    /**
     * The base mass of the child.
     */
    public final double baseMass;

    /**
     * The initial energy of the child.
     */
    public final double energy;

    /**
     * The velocity which is was emitted at.
     */
    public final Vector3d velocity;

    /**
     * Constructor.
     */
    public ActionSpawn(double baseMass, double energy, Vector3d velocity)
    {
	this.baseMass = baseMass;
	this.energy   = energy;
	this.velocity = velocity;
    }

    /**
     * {@inheritDoc}
     */
    public final double energyCost()
    {
	return baseMass * velocity.lengthSquared() + energy;
    }

    /**
     * {@inheritDoc}
     */
    public final double massCost()
    {
	return baseMass;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
	return "Spawn<" + baseMass + ":" + energy + "@" + velocity + ">";
    }
}
