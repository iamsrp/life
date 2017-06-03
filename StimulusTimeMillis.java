/**
 * The current time in milliseconds
 */
public class StimulusTimeMillis
    extends Stimulus
{
    /**
     * The set of people around us.
     */
    public final long timeMillis;
    
    /**
     * Constructor.
     */
    public StimulusTimeMillis(final long timeMillis)
    {
	this.timeMillis = timeMillis;
    }
}
