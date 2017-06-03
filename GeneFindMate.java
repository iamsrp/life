import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3d;

/**
 * A gene which creates a puff in a random direction.
 */
public class GeneFindMate
    implements GeneAction
{
    /**
     * How much we want to mate as a factor of our health.
     */
    private double myFindMateFactor;

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
    public GeneFindMate()
    {
	myFindMateFactor           = 0.5;
	myPuffSpeedFactor          = 0.1;
	myVelocityAdjustmentFactor = 0.5;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	myFindMateFactor           = Math.max(0.0, Math.min(1.0, myFindMateFactor               + (Math.random() - 0.5) * factor));
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

	// Find my neighbours
	StimulusNeighbours neighbours = (StimulusNeighbours)stimuli.get(StimulusNeighbours.class);
	if (neighbours.neighbours == null || neighbours.neighbours.isEmpty()) {
	    return result;
	}

	// Sane movement?
	if (myPuffSpeedFactor <= 0.0 || myFindMateFactor <= 0.0) {
	    return result;
	}

	// Figure out the desired direction we want to go in; we chase the
	// healthiest one
	Vector3d direction = null;
	World.VisibleCreature mate = null;
	for (World.VisibleCreature neighbour : neighbours.neighbours) {
	    // Ignore me
	    if (neighbour.getId() == self.self.getId()) {
		continue;
	    }

	    // We can only mate with our own family
	    if (neighbour.getFamily() != self.self.getFamily()) {
		continue;
	    }

	    // Only consider things which are alive (we're not weird)
	    if (!neighbour.isAlive()) {
		continue;
	    }

	    // Healthiest?
	    if (mate == null || mate.getHealth() < neighbour.getHealth()) {
		mate = neighbour;

		// And get the guy's direction relative us
		if (direction == null) {
		    direction = new Vector3d();
		}
		direction.set(neighbour.getPosition());
		direction.sub(self.self.getPosition());
	    }
	}

	// If we do anything then do it
	if (direction != null && direction.lengthSquared() > 0.0) {
	    // Normalise now and then save it
	    direction.normalize();
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
	    direction.scale(myFindMateFactor * self.self.getHealth());
	    
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
	    "<Class="          + this.getClass().getName()  + ";" +
	    "FindMateFactor="  + myFindMateFactor               + ";" +
	    "Puff="            + myPuffSpeedFactor          + ";" +
	    "VelAdj="          + myVelocityAdjustmentFactor + ">";
    }
}
