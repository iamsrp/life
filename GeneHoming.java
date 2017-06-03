import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3d;

/**
 * A gene which makes us home to a location
 */
public class GeneHoming
    implements GeneAction
{
    /**
     * How much we want to home to a particular location.
     */
    private Map<Vector3d,Double> myHomeFactors = new HashMap<Vector3d,Double>();

    /**
     * The speed factor of the direction vector which we puff at.
     */
    private double myPuffSpeedFactor;

    /**
     * How much to account for our current speed.
     */
    private double myVelocityAdjustmentFactor;

    /**
     * Constructor.
     */
    public GeneHoming()
    {
	myPuffSpeedFactor = 0.1;
	myVelocityAdjustmentFactor = 0.5;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	// How many right now?
	int count = myHomeFactors.size();

	// Tweak the existing ones
	Map<Vector3d,Double> newFactors = new HashMap<Vector3d,Double>();
	for (Map.Entry<Vector3d,Double> entry : myHomeFactors.entrySet()) {
	    // Only insert if we deem it likely
	    if (Math.random() > factor) {
		// And wiggle the entry upon insertion
		Vector3d where  = entry.getKey();
		Double   amount = entry.getValue();

		where.x =               where.x + 10 * (Math.random() - 0.5) * factor;
		where.y =               where.y + 10 * (Math.random() - 0.5) * factor;
		where.z = Math.max(0.0, where.z + 10 * (Math.random() - 0.5) * factor);
		amount  = amount + (Math.random() - 0.5) * factor;
		newFactors.put(where, amount);
	    }
	}
	myHomeFactors = newFactors;

	// And add new ones
	int newCount = (int)(Math.max(2, count) * (Math.random() + 0.5));
	while (myHomeFactors.size() < newCount) {
	    Vector3d where = new Vector3d(10 * (Math.random() - 0.5),
					  10 * (Math.random() - 0.5),
					   5 * (Math.random()      ));
	    Double amount = Math.random() - 0.5;
	    myHomeFactors.put(where, amount);
	}

	myPuffSpeedFactor          = Math.max(0.0,               myPuffSpeedFactor          + (Math.random() - 0.5) * factor);
	myVelocityAdjustmentFactor = Math.max(0.0, Math.min(1.0, myVelocityAdjustmentFactor + (Math.random() - 0.5) * factor));
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

	// Sane homing?
	if (myPuffSpeedFactor <= 0.0 || myHomeFactors.isEmpty()) {
	    return result;
	}

	// Get the location's position relative to ourselves
	Vector3d direction = new Vector3d();
	for (Map.Entry<Vector3d,Double> entry : myHomeFactors.entrySet()) {
	    // Any desire?
	    if (entry.getValue() == 0.0) {
		continue;
	    }

	    Vector3d dir = new Vector3d(entry.getKey());
	    dir.sub(self.self.getPosition());

	    // Are we there yet dad?
	    double distance = dir.length();
	    if (distance < self.self.getRadius()) {
		continue;
	    }
	    dir.scale(1.0 / distance);

	    // How quickly to get there
	    dir.scale(entry.getValue());

	    // Add it to the grand total
	    direction.add(dir);
	}

	// Handle the amount of homing we want to do
	direction.scale(myPuffSpeedFactor);
	    
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
	    result.add(new ActionPuff(direction));
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
	    "<Class="      + this.getClass().getName()  + ";" +
	    "HomeFactors=" + myHomeFactors              + ";" +
	    "Puff="        + myPuffSpeedFactor          + ";" +
	    "VelAdj="      + myVelocityAdjustmentFactor + ">";
    }
}
