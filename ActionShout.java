/**
 * Shout out a number to the world.
 */
public class ActionShout
    implements Action
{
    /**
     * The next unique number which could be shouted.
     */
    private static int ourNextNumber = 0;

    /**
     * The number to shout.
     */
    public final int number;

    /**
     * The radius over which to shout it (essentially volume).
     */
    public final double radius;

    /**
     * A factory method to get the next unique shoutable number.
     */
    public static int nextNumber()
    {
	return ourNextNumber++;
    }

    /**
     * Constructor.
     */
    public ActionShout(int number, double radius)
    {
	this.number = number;
	this.radius = radius;
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
	return "Shout<" + number + "@" + radius + ">";
    }
}
