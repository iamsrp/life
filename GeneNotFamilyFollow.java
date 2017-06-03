import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3d;

/**
 * A gene which causes us to follow (or run away from) anyoue who is not
 * family.
 */
public class GeneNotFamilyFollow
    implements GeneAction
{
    /**
     * The puff factor.
     */
    private double myPuffSpeedFactor;

    /**
     * Constructor.
     */
    public GeneNotFamilyFollow()
    {
	myPuffSpeedFactor = 0.0;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	myPuffSpeedFactor = myPuffSpeedFactor + (Math.random() - 0.5) * factor;
    }

    /**
     * {@inheritDoc}
     */
    public Gene clone()
    {
	try {
	    return (GeneNotFamilyFollow)super.clone();
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

	// Figure out the desired direction we want to go in given the set of
	// (possible) attackers around us
	Vector3d direction = new Vector3d();
	for (World.VisibleCreature neighbour : neighbours.neighbours) {
	    // Ignore me
	    if (neighbour.getId() == self.self.getId()) {
		continue;
	    }

	    // Only consider things which are alive
	    if (!neighbour.isAlive()) {
		continue;
	    }

	    // Ignore my family
	    if (self.self.getFamily() == neighbour.getFamily()) {
		continue;
	    }

	    // Get the guy's position relative to ourselves
	    Vector3d dir = new Vector3d(neighbour.getPosition());
	    dir.sub(self.self.getPosition());
	    if (dir.lengthSquared() == 0.0) {
		continue;
	    }
	    dir.normalize();

	    // Scale by the response factor
	    dir.scale(myPuffSpeedFactor);

	    // This is what we want to do
	    direction.add(dir);
	}

	// Don't run into the ground
	if (self.self.getPosition().z == 0.0 && direction.z < 0.0) {
	    direction.z = 0.0;
	}

	// Anything now?
	if (direction.lengthSquared() > 0.0) {
	    // Add it!
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
	    "<Class=" + this.getClass().getName() + ";" +
	    "Puff="   + myPuffSpeedFactor         + ">";
    }
}
