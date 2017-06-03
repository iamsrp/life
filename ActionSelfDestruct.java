/**
 * Self destruct by turning our mass into energy.
 */
public class ActionSelfDestruct
    implements Action
{
    /**
     * Constructor.
     */
    public ActionSelfDestruct()
    {
	// Nothing
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
	return "SelfDestruct";
    }
}
