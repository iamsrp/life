import java.util.ArrayList;
import java.util.Collection;

/**
 * Some shouting from around us.
 */
public class StimulusShouts
    extends Stimulus
{
    /**
     * The details of an attack.
     */
    public static class Details
    {
	/**
	 * Who shouted.
	 */
	public final World.VisibleCreature source;

	/**
	 * What did they say?
	 */
	public final int number;

	/**
	 * Constructor.
	 */
	public Details(World.VisibleCreature source, int number)
	{
	    this.source = source;
	    this.number = number;
	}
    }

    /**
     * The set of shouts.
     */
    public final Collection<Details> details;
    
    /**
     * Constructor.
     */
    public StimulusShouts()
    {
	this.details = new ArrayList<Details>();
    }
}
