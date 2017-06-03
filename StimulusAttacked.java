import java.util.ArrayList;
import java.util.Collection;

/**
 * A set of people who have attacked this creature.
 */
public class StimulusAttacked
    extends Stimulus
{
    /**
     * The details of an attack.
     */
    public static class Details
    {
	/**
	 * Who was it?
	 */
	World.VisibleCreature attacker;

	/**
	 * How much energy did they take as a fraction of the total.
	 */
	double energyFraction;

	/**
	 * Constructor.
	 */
	public Details(World.VisibleCreature attacker, double energyFraction)
	{
	    this.attacker       = attacker;
	    this.energyFraction = energyFraction;
	}
    }

    /**
     * The set of people around us.
     */
    public final Collection<Details> details;
    
    /**
     * Constructor.
     */
    public StimulusAttacked()
    {
	details = new ArrayList<Details>();
    }
}
