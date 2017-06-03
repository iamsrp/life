import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3d;

/**
 * A gene which causes us to follow (or run away from) a particular family.
 */
public class GeneFamilyFollow
    implements GeneAction
{
    /**
     * The puff factor for each family which we encounter.
     */
    private Map<Integer,Double> myFamilyPuffFactors = new HashMap<Integer,Double>();

    /**
     * Constructor.
     */
    public GeneFamilyFollow()
    {
	// Nothing
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	for (Integer key : myFamilyPuffFactors.keySet()) {
	    double puff = getFactor(key);
	    puff = puff + (Math.random() - 0.5) * factor;
	    myFamilyPuffFactors.put(key, factor);
	}
    }

    /**
     * {@inheritDoc}
     */
    public Gene clone()
    {
	try {
	    // Make sure this is a deep copy
	    GeneFamilyFollow gene = (GeneFamilyFollow)super.clone();
	    gene.myFamilyPuffFactors = new HashMap<Integer,Double>(myFamilyPuffFactors);
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

	    // How much do we want to repond to this guy
	    final double responseFactor = getFactor(neighbour.getFamily());

	    // Get the guy's position relative to ourselves
	    Vector3d dir = new Vector3d(neighbour.getPosition());
	    dir.sub(self.self.getPosition());
	    if (dir.lengthSquared() == 0.0) {
		continue;
	    }
	    dir.normalize();

	    // Scale by the response factor
	    dir.scale(responseFactor);

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

	// Housekeeping
	StimulusFamilies families = (StimulusFamilies)stimuli.get(StimulusFamilies.class);
	if (families != null) {
	    Iterator<Map.Entry<Integer,Double>> itr = myFamilyPuffFactors.entrySet().iterator();
	    while (itr.hasNext()) {
		if (!families.families.contains(itr.next().getKey())) {
		    itr.remove();
		}
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
	    "<Class=" + this.getClass().getName() + ";" +
	    "Puffs="  + myFamilyPuffFactors       + ">";
    }

    /**
     * Get a factor for a given family.
     */
    private double getFactor(int family)
    {
	Double factor = myFamilyPuffFactors.get(family);
	if (factor == null) {
	    factor = (Math.random() - 0.5) * 0.1;
	    myFamilyPuffFactors.put(family, factor);
	}
	return factor;
    }
}
