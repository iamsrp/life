import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3d;

/**
 * A gene which handles the fact that we were attacked by throwing out chaff.
 */
public class GeneAttackChaff
    implements GeneAction
{
    /**
     * The energy threshold of the relative attack before we throw out chaff.
     */
    private double myMinEnergyFractionThreshold;

    /**
     * The proportion of our own base mass which the child should have.
     */
    private double myBaseMassFactor;

    /**
     * The proportion of our energy which the child gets.
     */
    private double myEnergyFactor;

    /**
     * The speed factor of the direction vector which we emit at.
     */
    private double myEmitSpeedFactor;

    /**
     * The speed factor at which we attempt to slow down after we lose sight
     * of any attackers.
     */
    private double myStopFactor;

    /**
     * Constructor.
     */
    public GeneAttackChaff()
    {
	myMinEnergyFractionThreshold = 0.01;

	myBaseMassFactor  = 0.01;
	myEnergyFactor    = 0.01;

	myEmitSpeedFactor = 0.5;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	myMinEnergyFractionThreshold = Math.max(0.0, Math.min(1.0, myMinEnergyFractionThreshold + (Math.random() - 0.5) * factor));

	myBaseMassFactor = Math.max(0.0, myBaseMassFactor + (Math.random() - 0.5) * factor);
	myEnergyFactor   = Math.max(0.0, myEnergyFactor   + (Math.random() - 0.5) * factor);

	myEmitSpeedFactor = Math.max(0.0, myEmitSpeedFactor + (Math.random() - 0.5) * factor);
    }

    /**
     * {@inheritDoc}
     */
    public Gene clone()
    {
	try{
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

	// Chucking out nothing?
	if (myBaseMassFactor == 0) {
	    return result;
	}

	// Find myself
	StimulusSelf self = (StimulusSelf)stimuli.get(StimulusSelf.class);
	if (self == null) {
	    return result;
	}

	// Can we spawn?
	if (!self.canSpawn) {
	    return result;
	}

	// Any attackers?
	StimulusAttacked attacks = (StimulusAttacked)stimuli.get(StimulusAttacked.class);
	if (attacks == null) {
	    return result;
	}

	// Handle each attack
	for (StimulusAttacked.Details details : attacks.details) {
	    // Ignore light attackes
	    if (details.energyFraction < myMinEnergyFractionThreshold) {
		continue;
	    }

	    // Figure out the direction the attack came from
	    Vector3d direction = new Vector3d(details.attacker.getPosition());
	    direction.sub(self.self.getPosition());
	    if (direction.lengthSquared() > 0) {
		direction.normalize();
		
		// Don't run into the ground
		if (self.self.getPosition().z == 0.0 && direction.z > 0.0) {
		    direction.z = 0.0;
		}

		// Anything now?
		if (direction.lengthSquared() > 0.0) {
		    
		    // Tweak by the general fleeing factor
		    direction.scale(myEmitSpeedFactor);

		    // Add it!
		    result.add(new ActionSpawn(body.getBaseMassKg() * myBaseMassFactor,
					       body.getEnergy()     * myEnergyFactor,
					       direction));
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
	    "<Class="       + this.getClass().getName() + ";" +
	    "MassFactor="   + myBaseMassFactor          + ";" +
	    "EnergyFactor=" + myEnergyFactor            + ";" +
	    "EmitFactor="   + myEmitSpeedFactor         + ">";
    }
}
