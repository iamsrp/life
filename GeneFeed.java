import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3d;

/**
 * A gene which creates a puff in a random direction.
 */
public class GeneFeed
    implements GeneAction
{
    /**
     * How much we want to feed always.
     */
    private double myFeedFactor;

    /**
     * How much we want to feed as a factor of our health.
     */
    private double myHealthFeedFactor;

    /**
     * The speed factor of the direction vector which we puff at.
     */
    private double myPuffSpeedFactor;

    /**
     * How much to account for our current speed.
     */
    private double myVelocityAdjustmentFactor;

    /**
     * The number which we shout when we see food.
     */
    private int myShoutNumber;

    /**
     * The radius over which to shout when we see food.
     */
    private double myShoutRadius;

    /**
     * Constructor.
     */
    public GeneFeed()
    {
	myFeedFactor               = 0.5;
	myHealthFeedFactor         = 0.5;
	myPuffSpeedFactor          = 0.1;
	myVelocityAdjustmentFactor = 0.5;
	myShoutNumber              = ActionShout.nextNumber();
	myShoutRadius              = 10.0;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	myFeedFactor               = Math.max(0.0, Math.min(1.0, myFeedFactor               + (Math.random() - 0.5) * factor));
	myHealthFeedFactor         = Math.max(0.0, Math.min(1.0, myHealthFeedFactor         + (Math.random() - 0.5) * factor));
	myPuffSpeedFactor          = Math.max(0.0,               myPuffSpeedFactor          + (Math.random() - 0.5) * factor);
	myVelocityAdjustmentFactor = Math.max(0.0, Math.min(1.0, myVelocityAdjustmentFactor + (Math.random() - 0.5) * factor));
	myShoutRadius              = Math.max(0.0,               myShoutRadius              + (Math.random() - 0.5) * factor);
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

	// Find my neighbours
	StimulusNeighbours neighbours = (StimulusNeighbours)stimuli.get(StimulusNeighbours.class);
	if (neighbours.neighbours == null || neighbours.neighbours.isEmpty()) {
	    return result;
	}

	// Sane movement?
	if (myPuffSpeedFactor <= 0.0 || 
	    (myFeedFactor <= 0.0 && myHealthFeedFactor <= 0.0))
	{
	    return result;
	}

	// Figure out the desired direction we want to go in
	Vector3d direction = null;
	double closest = Double.MAX_VALUE;
	for (World.VisibleCreature neighbour : neighbours.neighbours) {
	    // Ignore me
	    if (neighbour.getId() == self.self.getId()) {
		continue;
	    }

	    // Only consider things which are dead
	    if (neighbour.isAlive()) {
		continue;
	    }

	    // Get the guy's position relative to ourselves
	    Vector3d dir = new Vector3d(neighbour.getPosition());
	    dir.sub(self.self.getPosition());

	    // Always go for the closest one
	    final double dist = dir.lengthSquared();

	    // If we are close enough then we attempt to feed
	    final double radii = self.self.getRadius() + neighbour.getRadius();
	    if (dist < radii * radii) {
		result.add(new ActionFeed(neighbour.getId()));
	    }
	    else if (dist > closest) {
		// Ignore this guy since it's too far away
		continue;
	    }
	    closest = dist;

	    // This is what we want to do
	    direction = dir;
	}

	// If we do anything then do it
	if (direction != null && direction.lengthSquared() > 0.0) {
	    // Shout that we were saw some food (since we are off to get it we
	    // must have done so)
	    if (myShoutRadius > self.self.getRadius()) {
		result.add(new ActionShout(myShoutNumber, myShoutRadius));
	    }

	    // Normalise now and then save it
	    direction.scale(myPuffSpeedFactor);

	    // Account for how fast we are already travelling. This is
	    // intended that we do less and less the more we are travelling in
	    // the right direction at the right speed.
	    if (myVelocityAdjustmentFactor > 0.0) {
		Vector3d vel = new Vector3d(self.self.getVelocity());
		vel.scale(myVelocityAdjustmentFactor);
		direction.sub(vel);
	    }

	    // Handle how much we want to chase this guy
	    direction.scale(myFeedFactor + 
			    myHealthFeedFactor * (1.0 - self.self.getHealth()));
	    
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
	    "<Class="      + this.getClass().getName()  + ";" +
	    "FeedFactor="  + myFeedFactor               + ";" +
	    "Puff="        + myPuffSpeedFactor          + ";" +
	    "VelAdj="      + myVelocityAdjustmentFactor + ";" +
	    "ShoutRadius=" + myShoutRadius              + ";" +
	    "ShoutNumber=" + myShoutNumber              + ">";
    }
}
