import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3d;

/**
 * A gene which makes things want to flock.
 */
public class GeneShoutResponse
    implements GeneAction
{
    /**
     * How much to account for our current speed.
     */
    private double myVelocityAdjustmentFactor;

    /**
     * The set of things which we hear and how we react to them.
     */
    private Map<Integer,Double> myShoutToPuffFactor;

    /**
     * Constructor.
     */
    public GeneShoutResponse()
    {
	myShoutToPuffFactor = new HashMap<Integer,Double>();
	myVelocityAdjustmentFactor = 0.5;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	for (Integer number : myShoutToPuffFactor.keySet()) {
	    double response = 
		myShoutToPuffFactor.get(number) + (Math.random() - 0.5) * factor;
	    response = Math.max(-1.0, Math.min(1.0, response));
	    myShoutToPuffFactor.put(number, response);
	}
	myVelocityAdjustmentFactor = Math.min(1.0, Math.max(0.0, myVelocityAdjustmentFactor + (Math.random() - 0.5) * factor));
    }

    /**
     * {@inheritDoc}
     */
    public Gene clone()
    {
	try {
	    // Make sure this is a deep copy
	    GeneShoutResponse gene = (GeneShoutResponse)super.clone();
	    gene.myShoutToPuffFactor = new HashMap<Integer,Double>(myShoutToPuffFactor);
	    return gene;
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

	// Find my neighbours
	StimulusShouts shouts = (StimulusShouts)stimuli.get(StimulusShouts.class);
	if (shouts == null || shouts.details.isEmpty()) {
	    return result;
	}

	// Figure out the desired direction we want to go in
	Vector3d direction = new Vector3d();
	double totalWeight = 0.0;
	for (StimulusShouts.Details shout : shouts.details) {
	    // Get the guy's position relative to ourselves
	    Vector3d dir = new Vector3d(shout.source.getPosition());
	    dir.sub(self.self.getPosition());
	    if (dir.lengthSquared() == 0.0) {
		continue;
	    }
	    dir.normalize();

	    // Scale by our reaction to the shout
	    dir.scale(getShoutResponse(shout.number));

	    // Now we can add this on to the total distance to go
	    direction.add(dir);
	}

	// If we do anything then do it
	if (direction.lengthSquared() > 0.0) {
	    // Account for how fast we are already travelling. This is
	    // intended that we do less and less the more we are travelling in
	    // the right direction at the right speed.
	    if (myVelocityAdjustmentFactor > 0.0) {
		Vector3d vel = new Vector3d(self.self.getVelocity());
		vel.scale(myVelocityAdjustmentFactor);
		direction.sub(vel);
	    }

	    // Don't jump into the ground
	    if (self.self.getPosition().z == 0.0 && direction.z < 0.0) {
		direction.z = 0.0;
	    }

	    // Anything now?
	    if (direction.lengthSquared() > 0.0) {
		// We blow in the opposite direction to get us where we want to go
		direction.negate();

		// Add it!
		result.add(new ActionPuff(direction));
	    }
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
	    "<Class="  + this.getClass().getName()  + ";" +
	    "Puffs="   + myShoutToPuffFactor        + ";" +
	    "VelAdj="  + myVelocityAdjustmentFactor + ">";
    }

    /**
     * Get the response to a given shout number.
     */
    private double getShoutResponse(int number)
    {
	Double result = myShoutToPuffFactor.get(number);
	if (result == null) {
	    result = 0.0;
	    myShoutToPuffFactor.put(number, result);
	}
	return result;
    }

}
