import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3d;

/**
 * A gene which creates a puff in a random direction.
 */
public class GeneRandomPuffAction
    implements GeneAction
{
    /**
     * The speed we puff at.
     */
    private double myPuffSpeed;

    /**
     * The number of ticks we puff along the same path for.
     */
    private double myNumTicksSameTrack; // gets cast to an int

    /**
     * The current puff vector.
     */
    private Vector3d myDirection = null;

    /**
     * The number of puffs left before we choose a new direction.
     */
    private int myNumTicksUntilNewTrack = 0;

    /**
     * Constructor.
     */
    public GeneRandomPuffAction()
    {
	myPuffSpeed         = 0.05;
	myNumTicksSameTrack = 10;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	myPuffSpeed         = Math.max(0.0, myPuffSpeed         + (Math.random() - 0.5) * factor);
	myNumTicksSameTrack = Math.max(0.0, myNumTicksSameTrack + (Math.random() - 0.5) * factor);
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
	
	// Anything going to happen?
	if (myPuffSpeed > 0.0) {
	    // Pick a walk?
	    if (myDirection == null || myNumTicksUntilNewTrack <= 0) {
		myDirection = new Vector3d();
		while (myDirection.lengthSquared() == 0.0) {
		    myDirection.x = Math.random() - 0.5;
		    myDirection.y = Math.random() - 0.5;
		    myDirection.z = Math.random() - 0.5;
		}

		// Normalise now, save it and reset counters
		myDirection.normalize();
		myDirection.scale(myPuffSpeed);
		myNumTicksUntilNewTrack = (int)myNumTicksSameTrack;
	    }
	    else {
		myNumTicksUntilNewTrack--;
	    }

	    // Add it!
	    result.add(new ActionPuff(myDirection));
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
	    "<Class="  + this.getClass().getName() + ";" +
	    "Puff="    + myPuffSpeed               + ";" +
	    "Ticks="   + myNumTicksSameTrack       + ">";
    }
}
