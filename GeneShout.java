import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A gene which just shouts a lot.
 */
public class GeneShout
    implements GeneAction
{
    /**
     * The number which we shout.
     */
    private int myShoutNumber;

    /**
     * The radius over which to shout.
     */
    private double myShoutRadius;

    /**
     * Constructor.
     */
    public GeneShout()
    {
	myShoutNumber = ActionShout.nextNumber();
	myShoutRadius = 0.0;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	myShoutRadius = Math.max(0.0, myShoutRadius + (Math.random() - 0.5) * factor);
    }

    /**
     * {@inheritDoc}
     */
    public Gene clone()
    {
	try {
	    return (Gene)super.clone();
	}
	catch (CloneNotSupportedException e) {
	    throw new RuntimeException();
	}
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Action> generateActions(final Brain.BrainBody body,
					      final Map<Class,Stimulus> stimuli)
    {
	// What we give back
	List<Action> result = new ArrayList<Action>();
	
	// Find myself
	StimulusSelf self = (StimulusSelf)stimuli.get(StimulusSelf.class);
	if (self == null) {
	    return result;
	}

	// Shout?
	if (myShoutRadius > self.self.getRadius()) {
	    result.add(new ActionShout(myShoutNumber, myShoutRadius));
	}

	// Done
	return result;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
	return 
	    "<Class="      + this.getClass().getName() + ";" +
	    "ShoutRadius=" + myShoutRadius             + ";" +
	    "ShoutNumber=" + myShoutNumber             + ">";
    }
}
