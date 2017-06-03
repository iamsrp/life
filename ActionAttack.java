/**
 * Attack another creature.
 */
public class ActionAttack
    implements Action
{
    /**
     * The ID of the creature which we are attacking.
     */
    public final int id;

    /**
     * Constructor which gives us a null impulse.
     */
    public ActionAttack(final int id)
    {
	this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    public final double energyCost()
    {
	return 0.0;
    }

    /**
     * {@inheritDoc}
     */
    public final double massCost()
    {
	return 0.0;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
	return "Attack<" + id + ">";
    }
}
