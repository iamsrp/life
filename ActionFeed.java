/**
 * Feed from another creature.
 */
public class ActionFeed
    implements Action
{
    /**
     * The ID of the creature which we are feeding from.
     */
    public final int id;

    /**
     * Constructor which gives us a null impulse.
     */
    public ActionFeed(final int id)
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
	return "Feed<" + id + ">";
    }
}
