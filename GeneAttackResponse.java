import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Vector3d;

/**
 * A gene which handles the fact that we were attacked.
 */
public class GeneAttackResponse
    implements GeneAction
{
    /**
     * The set of people which have attacked us and how much energy they took.
     */
    private Map<Integer,Double> myAttackers = new HashMap<Integer,Double>();

    /**
     * The halflife of the attack details of an attacker.
     */
    private double myAttackersHalflife;

    /**
     * The factor by which we account for attackers.
     */
    private double myAttackersFactor;

    /**
     * The set of families which have attacked us.
     */
    private Map<Integer,Double> myAttackerFamilies = new HashMap<Integer,Double>();

    /**
     * The halflife of the attack details of an attacker's family.
     */
    private double myAttackerFamiliesHalflife;

    /**
     * The factor by which we account for attackers' families.
     */
    private double myAttackerFamiliesFactor;

    /**
     * The last time we were called.
     */
    private long myLasttimeMillis = -1;

    /**
     * The speed factor of the direction vector which we puff at.
     */
    private double myPuffSpeedFactor;

    /**
     * The number which we shout when attacked.
     */
    private final int myShoutNumber;

    /**
     * The radius over which to shout when attacked.
     */
    private double myShoutRadius;

    /**
     * Constructor.
     */
    public GeneAttackResponse()
    {
	myAttackersHalflife        = 0.1;
	myAttackersFactor          = 0.1;

	myAttackerFamiliesHalflife = 0.1;
	myAttackerFamiliesFactor   = 0.1;

	myPuffSpeedFactor = 0.4;

	myShoutNumber = ActionShout.nextNumber();
	myShoutRadius = 10.0;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	myAttackersHalflife = Math.max(0.0, myAttackersHalflife + (Math.random() - 0.5) * factor);
	myAttackersFactor   = Math.max(0.0, myAttackersFactor   + (Math.random() - 0.5) * factor);

	myAttackerFamiliesHalflife = Math.max(0.0, myAttackerFamiliesHalflife + (Math.random() - 0.5) * factor);
	myAttackerFamiliesFactor   = Math.max(0.0, myAttackerFamiliesFactor   + (Math.random() - 0.5) * factor);

	myPuffSpeedFactor = Math.max(0.0, myPuffSpeedFactor + (Math.random() - 0.5) * factor);

	myShoutRadius = Math.max(0.0, myShoutRadius + (Math.random() - 0.5) * factor);
    }

    /**
     * {@inheritDoc}
     */
    public Gene clone()
    {
	try {
	    // Make sure this is a deep copy
	    GeneAttackResponse gene = (GeneAttackResponse)super.clone();
	    gene.myAttackers        = new HashMap<Integer,Double>(myAttackers);
	    gene.myAttackerFamilies = new HashMap<Integer,Double>(myAttackerFamilies);
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

	// When is now?
	StimulusTimeMillis now = 
	    (StimulusTimeMillis) stimuli.get(StimulusTimeMillis.class);
	if (now == null) {
	    return result;
	}

	// Figure out how many seconds have passed since we were last called
	final double secondsPassed = (myLasttimeMillis >= 0) ?
	    (double)(now.timeMillis - myLasttimeMillis) / 1000.0 : 0.0;
	myLasttimeMillis = now.timeMillis;

	// Decay any attack stimuli
	final double 
	    attackersDecay        = Math.exp(-secondsPassed * myAttackersHalflife),
	    attackerFamiliesDecay = Math.exp(-secondsPassed * myAttackerFamiliesHalflife);
	for (Integer attackerId : myAttackers.keySet()) {
	    myAttackers.put(attackerId, myAttackers.get(attackerId) * attackersDecay);
	}
	for (Integer familyId : myAttackerFamilies.keySet()) {
	    myAttackerFamilies.put(familyId, myAttackerFamilies.get(familyId) * attackerFamiliesDecay);
	}

	// Find myself
	StimulusSelf self = (StimulusSelf)stimuli.get(StimulusSelf.class);
	if (self == null) {
	    return result;
	}

	// Remember anyone who attacked us
	StimulusAttacked attacks = (StimulusAttacked)stimuli.get(StimulusAttacked.class);
	if (attacks != null) {
	    // Figure out who
	    for (StimulusAttacked.Details details : attacks.details) {
		// Accumulate into the ID's energy taken
		Double energy = myAttackers.get(details.attacker.getId());
		energy = (energy == null) ? details.energyFraction : energy + details.energyFraction;
		myAttackers.put(details.attacker.getId(), energy);

		// And the family's
		energy = myAttackerFamilies.get(details.attacker.getFamily());
		energy = (energy == null) ? details.energyFraction : energy + details.energyFraction;
		myAttackerFamilies.put(details.attacker.getFamily(), energy);
	    }

	    // Shout that we were attacked
	    if (myShoutRadius > self.self.getRadius()) {
		result.add(new ActionShout(myShoutNumber, myShoutRadius));
	    }
	}

	// Sane motion?
	if (myPuffSpeedFactor <= 0.0) {
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

	    // See if this guy or his family have ever attacked us
	    Double attackerEnergy = myAttackers.get(neighbour.getId());
	    Double familyEnergy   = myAttackerFamilies.get(neighbour.getFamily());
	    if (attackerEnergy == null) {
		attackerEnergy = 0.0;
	    }
	    if (familyEnergy == null) {
		familyEnergy = 0.0;
	    }

	    // How much do we want to repond to this guy
	    final double responseFactor = 
		Math.tanh(attackerEnergy * myAttackersFactor +
			  familyEnergy   * myAttackerFamiliesFactor);
	    if (responseFactor <= 0.0) {
		continue;
	    }

	    // Get the guy's position relative to ourselves
	    Vector3d dir = new Vector3d(neighbour.getPosition());
	    dir.sub(self.self.getPosition());
	    while (dir.lengthSquared() == 0.0) {
		// Get away randomly
		dir.x = Math.random() - 0.5;
		dir.y = Math.random() - 0.5;
		dir.z = Math.random() - 0.5;		
	    }
	    dir.normalize();

	    // Go in the opposite direction according to our response factor
	    dir.scale(responseFactor);
	    dir.negate();

	    // This is what we want to do
	    direction.add(dir);
	}

	// Any response?
	if (direction.lengthSquared() > 0.0) {
	    // Tweak by the general fleeing factor
	    direction.scale(myPuffSpeedFactor);
	}

	// Don't run into the ground
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

	// Housekeeping
	StimulusFamilies families = (StimulusFamilies)stimuli.get(StimulusFamilies.class);
	if (families != null) {
	    Iterator<Map.Entry<Integer,Double>> itr = myAttackers.entrySet().iterator();
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
	    "<Class="            + this.getClass().getName()   + ";" +
	    "Attackers="         + myAttackers                 + ";" +
	    "AttackersHalfLife=" + myAttackersHalflife         + ";" +
	    "AttackersFactor="   + myAttackersFactor           + ";" +
	    "AttackersFailies="  + myAttackerFamilies          + ";" +
	    "FamiliesHalfLife="  + myAttackerFamiliesHalflife  + ";" +
	    "FamiliesFactor="    + myAttackerFamiliesFactor    + ";" +
	    "Puff="              + myPuffSpeedFactor           + ";" +
	    "ShoutNumber="       + myShoutNumber               + ";" +
	    "ShoutRadius="       + myShoutRadius               + ">";
    }
}
