import javax.vecmath.Vector3d;

/**
 * Mate off another creature similar to me.
 */
public class ActionMate
    implements Action
{
    /**
     * Who we are trying to mate with.
     */
    public final int id;

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
    public ActionMate(int id, double baseMass, double energy, Vector3d velocity)
    {
	this.id       = id;
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
	return "Mate<" + id + ":" + baseMass + ":" + energy + "@" + velocity + ">";
    }
}
