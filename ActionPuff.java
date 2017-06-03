import javax.vecmath.Vector3d;

/**
 * The creature blows.
 */
public class ActionPuff
    implements Action
{
    /**
     * The directed momentum off the puff will be capped at length 1.0.
     */
    public final Vector3d velocity;

    /**
     * Constructor.
     */
    public ActionPuff(Vector3d velocity)
    {
	this.velocity = velocity;
	if (this.velocity.lengthSquared() > 1.0) {
	    this.velocity.normalize();
	}
    }

    /**
     * {@inheritDoc}
     */
    public final double energyCost()
    {
	// This is a bit of a hack in that it's handled by the world
	return 0.0;
    }

    /**
     * {@inheritDoc}
     */
    public final double massCost()
    {
	// We don't cost any mass since we use the aether to puff
	return 0.0;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
	return "Puff<" + velocity + "m/s>";
    }
}
