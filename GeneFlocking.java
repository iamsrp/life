import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3d;

/**
 * A gene which makes things want to flock.
 */
public class GeneFlocking
    implements GeneAction
{
    /**
     * How much we want to flock.
     */
    private double myFlockFactor;

    /**
     * The speed factor of the direction vector which we puff at.
     */
    private double myPuffSpeedFactor;

    /**
     * How much to account for our current speed.
     */
    private double myVelocityAdjustmentFactor;

    /**
     * The desired fraction of the sum of the radii of me and a neighbour to
     * be apart.
     */
    private double myDesiredRadiiSeparationFactor;

    /**
     * The distance at which we take notice of neighbours.
     */
    private double myMatchingDistance;

    /**
     * Constructor.
     */
    public GeneFlocking()
    {
	myFlockFactor                  = 0.2;
	myPuffSpeedFactor              = 0.1;
	myVelocityAdjustmentFactor     = 0.5;
	myDesiredRadiiSeparationFactor = 3.0;
	myMatchingDistance             = 5.0;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	myFlockFactor                  = Math.min(1.0, Math.max(0.0, myFlockFactor                  + (Math.random() - 0.5) * factor));
	myPuffSpeedFactor              = Math.max(0.0,               myPuffSpeedFactor              + (Math.random() - 0.5) * factor);
	myVelocityAdjustmentFactor     = Math.min(1.0, Math.max(0.0, myVelocityAdjustmentFactor     + (Math.random() - 0.5) * factor));
	myDesiredRadiiSeparationFactor = Math.max(2.0,               myDesiredRadiiSeparationFactor + (Math.random() - 0.5) * factor);
	myMatchingDistance             = Math.max(1.0,               myMatchingDistance             + (Math.random() - 0.5) * factor);
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

	// Sane flocking?
	if (myPuffSpeedFactor <= 0.0 || myFlockFactor <= 0.0) {
	    return result;
	}

	// Figure out the desired direction we want to go in and the velocity
	// which we attempt to match
	Vector3d direction = new Vector3d();
	double   directionWeight = 0.0;
	for (World.VisibleCreature neighbour : neighbours.neighbours) {
	    // Ignore me
	    if (neighbour.getId() == self.self.getId()) {
		continue;
	    }

	    // Ignore dead guys
	    if (!neighbour.isAlive()) {
		continue;
	    }

	    // Only flock around other class members
	    if (neighbour.getFamily() != self.self.getFamily()) {
		continue;
	    }

	    // Get the guy's position relative to ourselves
	    Vector3d dir = new Vector3d(neighbour.getPosition());
	    dir.sub(self.self.getPosition());
	    final double radii = self.self.getRadius() + neighbour.getRadius();

	    // Are they right on top of us?
	    double distance = dir.length();
	    while (distance == 0) {
		// Get away randomly
		dir.x = Math.random() - 0.5;
		dir.y = Math.random() - 0.5;
		dir.z = Math.random() - 0.5;
		distance = dir.length();
	    }
	    dir.scale(1.0 / distance);

	    // What is the desired position to be along this vector? Make the
	    // vector than length since that's how far we have to travel. Note
	    // that this might be negative if we are too close.
	    final double desiredDistance = radii * myDesiredRadiiSeparationFactor;
	    dir.scale(distance - desiredDistance);

	    // Finally we weight this vector by how close the guy is to us
	    // since we want to pay more attention to nearer neighbours. Cap
	    // to be careful.
	    final double dw = Math.min(10.0, 1.0 / Math.max(radii, distance));
	    dir.scale(dw);
	    directionWeight += dw;

	    // Now we can add this on to the total distance to go
	    direction.add(dir);

	    // And handle the desired velocity thing
	    double vw = 1.0 / Math.pow(Math.max(1.0, myMatchingDistance - distance), 2.0);
	    Vector3d v = neighbour.getVelocity();
	    v.scale(vw);
	    direction.add(v);
	    directionWeight += vw;
	}
	if (directionWeight > 0.0) {
	    direction.scale(1.0 / directionWeight);
	}

	// If we do anything then do it
	if (direction.lengthSquared() > 0.0) {
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

	    // Handle the amount of flocking we want to do
	    direction.scale(myFlockFactor);
	    
	    // Don't jump into the ground
	    if (self.self.getPosition().z == 0.0 && direction.z < 0.0) {
		direction.z = 0.0;
	    }

	    // Anything now?
	    if (direction.lengthSquared() > 0.0) {
		// We blow in the opposite direction to get us where we want to go
		direction.negate();

		// Add it!
		direction.scale(self.self.getMassKg());
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
	    "<Class="      + this.getClass().getName()      + ";" +
	    "FlockFactor=" + myFlockFactor                  + ";" +
	    "Puff="        + myPuffSpeedFactor              + ";" +
	    "VelAdj="      + myVelocityAdjustmentFactor     + ";" +
	    "SepFactor="   + myDesiredRadiiSeparationFactor + ";" +
	    "MatchDist="   + myMatchingDistance             + ">";
    }
}
