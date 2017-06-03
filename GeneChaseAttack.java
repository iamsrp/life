import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Vector3d;

/**
 * A gene which causes us to attack someone and chase the weakest thing we
 * see.
 */
public class GeneChaseAttack
    implements GeneAction
{
    /**
     * How much we want to attack a particular family as a factor of our
     * health.
     */
    private Map<Integer,Double> myAttackFactors = new HashMap<Integer,Double>();

    /**
     * The speed factor of the direction vector which we puff at.
     */
    private double myPuffSpeedFactor;

    /**
     * How much to account for our current speed.
     */
    private double myVelocityAdjustmentFactor;

    /**
     * The number which we shout when attacking.
     */
    private final int myShoutNumber;

    /**
     * The radius over which to shout when attacking.
     */
    private double myShoutRadius;

    /**
     * Constructor.
     */
    public GeneChaseAttack()
    {
	myPuffSpeedFactor          = 0.1; // Math.random() / 10.0;
	myVelocityAdjustmentFactor = 0.75; // Math.random()
	myShoutNumber              = ActionShout.nextNumber();
	myShoutRadius              = 0.0;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	myPuffSpeedFactor          = Math.max(0.01,               myPuffSpeedFactor          * (Math.random() * factor + 0.5));
	myVelocityAdjustmentFactor = Math.max(0.01, Math.min(1.0, myVelocityAdjustmentFactor * (Math.random() * factor + 0.5)));
	myShoutRadius              = Math.max(0.00,               myShoutRadius + (Math.random() - 0.5) * factor);
    }

    /**
     * {@inheritDoc}
     */
    public Gene clone()
    {
	// Make sure this is a deep copy
	try {
	    GeneChaseAttack gene = (GeneChaseAttack)super.clone();
	    gene.myAttackFactors = new HashMap<Integer,Double>(myAttackFactors);
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

	// Sane motion?
	if (myPuffSpeedFactor <= 0.0) {
	    return result;
	}

	// Figure out the desired direction we want to go in
	double victimHealth = Double.MAX_VALUE;
	boolean shout = false;
	World.VisibleCreature victim = null;
	for (World.VisibleCreature neighbour : neighbours.neighbours) {
	    // Ignore me
	    if (neighbour.getId() == self.self.getId()) {
		continue;
	    }

	    // Never attack our own lot
	    if (self.self.getFamily() == neighbour.getFamily()) {
		continue;
	    }

	    // Only consider things which are alive
	    if (!neighbour.isAlive()) {
		continue;
	    }

	    // How much do we want to attack this guy
	    final double attackFactor = getAttackFactor(neighbour.getFamily());
	    if (attackFactor <= 0.0) {
		continue;
	    }

	    // Only attack if we feel we need some health
	    if (attackFactor < self.self.getHealth()) {
		continue;
	    }

	    // Get the guy's position relative to ourselves
	    Vector3d dir = new Vector3d(neighbour.getPosition());
	    dir.sub(self.self.getPosition());

	    // The "health" of this guy. We factor in size too since we want
	    // to avoid attacking big things; we use our size as a baseline to
	    // prevent us from chasing little guys.
	    final double health = 
		neighbour.getHealth() * Math.max(self.self.getRadius(), 
						 neighbour.getRadius());

	    // If we are close enough then we attempt to attack
	    final double radii = self.self.getRadius() + neighbour.getRadius();
	    if (dir.lengthSquared() < radii * radii) {
		// Figure out how likely we are to attack this guy
		result.add(new ActionAttack(neighbour.getId()));
		shout = true;
	    }
	    else if (health < victimHealth) {
		// Choose this guy as a weak victim
		victim       = neighbour;
		victimHealth = neighbour.getHealth();
	    }
	}

	// Do we want to shout?
	if (shout && myShoutRadius > self.self.getRadius()) {
	    result.add(new ActionShout(myShoutNumber, myShoutRadius));
	}

	// If we do anything then do it
	if (victim != null) {
	    // Get the guy's position relative to ourselves
	    Vector3d direction = new Vector3d(victim.getPosition());
	    direction.sub(self.self.getPosition());
	    if (direction.lengthSquared() > 0.0) {
		// Normalise now and then save it
		direction.scale(myPuffSpeedFactor);

		// Account for how fast we are already travelling. This is
		// intended that we do less and less the more we are
		// travelling in the right direction at the right speed.
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
		    // We blow in the opposite direction to get us where we
		    // want to go
		    direction.negate();

		    // Add it!
		    result.add(new ActionPuff(direction));
		}
	    }
	}

	// Housekeeping
	StimulusFamilies families = (StimulusFamilies)stimuli.get(StimulusFamilies.class);
	if (families != null) {
	    Iterator<Map.Entry<Integer,Double>> itr = myAttackFactors.entrySet().iterator();
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
	    "<Class="      + this.getClass().getName()  + ";" +
	    "Factors="     + myAttackFactors            + ";" +
	    "Puff="        + myPuffSpeedFactor          + ";" +
	    "VelAdj="      + myVelocityAdjustmentFactor + ";" +
	    "ShoutNumber=" + myShoutNumber              + ";" +
	    "ShoutRadius=" + myShoutRadius              + ">";
    }

    /**
     * The attack factor for a family.
     */
    private double getAttackFactor(Integer f)
    {
	Double factor = myAttackFactors.get(f);
	if (factor == null) {
	    factor = Math.random();
	    myAttackFactors.put(f, factor);
	}
	return factor;
    }
}
