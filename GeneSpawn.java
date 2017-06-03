import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3d;

/**
 * A gene which makes us produce children by asexual reproduction.
 */
public class GeneSpawn
    implements GeneAction
{
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
     * How healthy we have to be before we can spawn.
     */
    private double myMinimumHealth;

    /**
     * Constructor.
     */
    public GeneSpawn()
    {
	myBaseMassFactor  = 1.0;
	myEnergyFactor    = 0.2;
	myEmitSpeedFactor = 0.5;
	myMinimumHealth   = 0.5;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	myBaseMassFactor  = Math.max(0.0, myBaseMassFactor  + (Math.random() - 0.5) * factor);
	myEnergyFactor    = Math.max(0.0, myEnergyFactor    + (Math.random() - 0.5) * factor);
	myEmitSpeedFactor = Math.max(0.0, myEmitSpeedFactor + (Math.random() - 0.5) * factor);
	myMinimumHealth   = Math.max(0.0, myMinimumHealth   + (Math.random() - 0.5) * factor);
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

	// Are we allowed to spawn?
	if (!self.canSpawn) {
	    return result;
	}

	// Anything now?
	if (body.getHealth() > myMinimumHealth) {
	    // Which way direction to spawn in
	    Vector3d direction = new Vector3d();
	    direction.x = Math.random() - 0.5;
	    direction.y = Math.random() - 0.5;
	    direction.z = Math.random() - 0.5;

	    // Hello juniour!
	    result.add(new ActionSpawn(body.getBaseMassKg() * myBaseMassFactor,
				       body.getEnergy()     * myEnergyFactor,
				       direction));
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
	    "EmitFactor="   + myEmitSpeedFactor         + ";" +
	    "MinHealth="    + myMinimumHealth           + ">";
    }
}
