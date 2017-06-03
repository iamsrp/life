import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3d;

/**
 * A gene which creates a puff in a random direction.
 */
public class GeneSelfDestruct
    implements GeneAction
{
    /**
     * How much bigger than us one of our neighbours has to be before we self
     * destruct (ratio).
     */
    private double mySizeFactor;

    /**
     * How close the neighbour has to be before we consider them (as a factor
     * of combine radii).
     */
    private double myRadiiFactor;

    /**
     * How close the family member must be away before we consider them safe
     * (as a factor my radius).
     */
    private double myFamilyRadiusFactor;

    /**
     * Constructor.
     */
    public GeneSelfDestruct()
    {
	mySizeFactor         = 10.0;
	myRadiiFactor        = 10.0;
	myFamilyRadiusFactor = 10.0;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	mySizeFactor         = Math.max(0.0, mySizeFactor         + 5.0 * (Math.random() - 0.5) * factor);
	myRadiiFactor        = Math.max(0.0, myRadiiFactor        + 5.0 * (Math.random() - 0.5) * factor);
	myFamilyRadiusFactor = Math.max(0.0, myFamilyRadiusFactor + 5.0 * (Math.random() - 0.5) * factor);
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

	// Doing anything
	if (mySizeFactor <= 0.0 || myRadiiFactor <= 0.0) {
	    return result;
	}

	// Figure out if we want to nuke ourselves
	boolean wantNuke = false;
	for (World.VisibleCreature neighbour : neighbours.neighbours) {
	    // Ignore me
	    if (neighbour.getId() == self.self.getId()) {
		continue;
	    }

	    // Only consider things which are alive
	    if (!neighbour.isAlive()) {
		continue;
	    }

	    // Is he big enough?
	    if (neighbour.getRadius() / self.self.getRadius() < mySizeFactor) {
		continue;
	    }

	    // Get the guy's position relative to ourselves
	    Vector3d dir = new Vector3d(neighbour.getPosition());
	    dir.sub(self.self.getPosition());
	    final double distSq = dir.lengthSquared();

	    // Don't do this if we can see family members too close
	    final double familyDist = self.self.getRadius() * myFamilyRadiusFactor;
	    if (self.self.getFamily() == neighbour.getFamily() &&
		distSq < familyDist * familyDist)
	    {
		// Simply early-out here; we never want to take any action in
		// this circumstance
		return result;
	    }

	    // Is he close enough?
	    final double radiiDist = 
		(self.self.getRadius() + neighbour.getRadius()) * myRadiiFactor;
	    if (distSq > radiiDist * radiiDist) {
		continue;
	    }

	    // If we get there then we want to nuke!
	    wantNuke = true;
	    break;
	}

	// Do it?
	if (wantNuke) {
	    result.add(new ActionSelfDestruct());
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
	    "<Class="             + this.getClass().getName() + ";" +
	    "SizeFactor="         + mySizeFactor              + ";" +
	    "RadiiFactor="        + myRadiiFactor             + ";" +
	    "FamilyRadiusFactor=" + myFamilyRadiusFactor      + ">";
    }
}
