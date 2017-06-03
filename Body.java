import java.io.Serializable;

import java.util.List;

import javax.vecmath.Vector3d;

/**
 * The body of the creature.
 */
public class Body 
    implements Serializable
{
    /**
     * The amount of energy we are allowed to have as a multiplier of our
     * base mass.
     */
    public static final double ourMassEnergyLimitMultiple = 1000.0;

    /**
     * The amount of energy this body has. Zero means we're dead. How much
     * energy we have is limited by our base mass.
     */
    private double myEnergy = 0.0;

    /**
     * The base mass of this creature in kg. Zero means we can be removed from
     * the world.
     */
    private double myBaseMassKg = 0.0;

    /**
     * Constructor.
     */
    public Body(double baseMass)
    {
	myBaseMassKg = baseMass;
    }

    /**
     * The base mass of this body in kg.
     */
    public final double getBaseMassKg()
    {
	return myBaseMassKg;
    }

    /**
     * The mass of this body in kg.
     */
    public final double getMassKg()
    {
	// Energy makes up some of our mass too
	return myBaseMassKg + myEnergy * World.MASS_PER_ENERGY;
    }

    /**
     * The amount of energy we have.
     */
    public final double getEnergy()
    {
	return myEnergy;
    }
	
    /**
     * The limit on how much energy we can have.
     */
    public final double maxEnergy()
    {
	return ourMassEnergyLimitMultiple * myBaseMassKg;
    }

    /**
     * Add some energy.
     */
    public void addEnergy(double energy)
    {
	myEnergy = Math.min(maxEnergy(), myEnergy + energy);
    }

    /**
     * Remove some energy.
     */
    public void removeEnergy(double energy)
    {
	myEnergy = Math.max(0.0, myEnergy - energy);
    }

    /**
     * Is this guy alive?
     */
    public final boolean isAlive()
    {
	return (myEnergy > 0.0);
    }

    /**
     * Kill this body.
     */
    public void kill()
    {
	// Turn all our mass into base mass and zero out the energy to do
	// this.
	if (isAlive()) {
	    double mass = getMassKg();
	    myEnergy = 0.0;
	    myBaseMassKg = mass;
	}
    }

    /**
     * Feed from this body.
     */
    public final double feedFrom(double desire)
    {
	// Cannot feed from living things
	if (isAlive()) {
	    return 0.0;
	}

	// Figure out how much and give it back
	double gets = Math.min(myBaseMassKg, Math.max(0.0, desire));
	myBaseMassKg -= gets;
	return gets;
    }

    /**
     * Perform an action; this will cost us.
     */
    public boolean performAction(Action action)
    {
	// Make sure we have energy to do this
	final double cost = 
	    action.energyCost() + action.massCost() / World.MASS_PER_ENERGY;
	if (cost > myEnergy) {
	    return false;
	}

	// Deduct it and we're okay
	myEnergy -= cost;
	return true;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
	return
	    "<Class=" + this.getClass().getName() + ";" +
	    "Energy=" + myEnergy                  + ";" +
	    "Mass="   + myBaseMassKg              + "kg>";
    }
}
