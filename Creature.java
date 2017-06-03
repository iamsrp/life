import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.vecmath.Vector3d;
import javax.vecmath.Color3f;

import javax.media.j3d.Appearance;
import javax.media.j3d.Material;
import javax.media.j3d.Node;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Sphere;

/**
 * A basic creature in the world.
 */
public class Creature 
    implements Serializable
{
    // ============================== Types ==============================

    /**
     * The body as the rest of the world can access it. A wrapper class so
     * holders can't cast.
     */
    public class PublicBody
    {
	/**
	 * The body we contain.
	 */
	private final Body myBody;

	/**
	 * What the body looks like
	 */
	private final Appearance myAppearance;

	/**
	 * Constructor.
	 */
	public PublicBody(Body body, Appearance appearance)
	{
	    myBody = body;
	    myAppearance = appearance;
	}

	/**
	 * Is this creature alive?
	 */
	public boolean isAlive()
	{
	    return myBody.isAlive();
	}
	
	/**
	 * Get the mass of this body in kg.
	 */
	public double getMassKg()
	{
	    return myBody.getMassKg();
	}
	
	/**
	 * What is the current energy of the creature.
	 */
	public double getEnergy()
	{
	    return myBody.getEnergy();
	}

	/**
	 * What is the maximum energy of the creature.
	 */
	public double maxEnergy()
	{
	    return myBody.maxEnergy();
	}

	/**
	 * Add some energy.
	 */
	public void addEnergy(double energy)
	{
	    myBody.addEnergy(energy);
	}

	/**
	 * Remove some energy.
	 */
	public void removeEnergy(double energy)
	{
	    myBody.removeEnergy(energy);
	}

	/**
	 * Feed from this body by taking it's mass.
	 *
	 * @param desire How much mass we want.
	 *
	 * @return How much mass we got in kg.
	 */
	public double feedFrom(double desire)
	{
	    return myBody.feedFrom(desire);
	}

	/**
	 * How healthy are we? (In terms of energy as a fraction of max
	 * energy)
	 */
	public double getHealth()
	{
	    return (myBody.maxEnergy() > 0) ? 
		myBody.getEnergy() / myBody.maxEnergy() : 0.0;
	}

	/**
	 * Get the body shape of this creature.
	 */
	public Node getShape()
	{
	    //Sphere s = new Sphere(1.0f, myAppearance);
	    Box s = new Box(0.5f, 1.618f / 4, 1.618f / 2, myAppearance); // Golden ratio
	    return s;
	}
    }

    // ============================== Members ==============================    

    /**
     * How much energy a tick of our body needs as a proportion of mass.
     */
    private static final double ENERGY_PER_MASS_PER_TICK = 0.01;

    /**
     * The material associated with a family.
     */
    private static final Map<Integer,Material> ourFamilyColors = 
	new HashMap<Integer,Material>();

    /**
     * The next creature ID.
     */
    private static int ourNextId = 0;

    /**
     * The next family.
     */
    private static int ourNextFamily = 0;

    /**
     * The ID of this creature.
     */
    private int myId = nextId();

    /**
     * The family which this creature belongs to.
     */
    private int myFamily;

    /**
     * My brain.
     */
    private Brain myBrain = new Brain();
    
    /**
     * My body.
     */
    private Body myBody;

    /**
     * What the body looks like
     */
    private Appearance myAppearance = new Appearance();

    /**
     * The public version of my body.
     */
    private PublicBody myPublicBody;

    /**
     * The metabolic rate at which this creature lives. This is in ticks per
     * second.
     */
    private double myMetabolicRate;

    /**
     * How many ticks have we had?
     */
    private int myTickAge = 0;

    // =========================== Constructors ============================

    /**
     * Constructor.
     */
    public Creature(Double baseMass, Integer family)
    {
	myBody       = new Body(baseMass);
	myFamily     = family;
	myPublicBody = new PublicBody(myBody, myAppearance);
	myMetabolicRate = Math.min(100.0, Math.max(1, 100.0 / baseMass));

 	myAppearance.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
 	myAppearance.setMaterial(getFamilyColors(family));
    }

    // ========================== Public Methods ===========================

    /**
     * Get the next family.
     */
    public static int nextFamily()
    {
	return ourNextFamily++;
    }

    /**
     * Get my ID.
     */
    public final int getId()
    {
	return myId;
    }

    /**
     * Get the family.
     */
    public final int getFamily()
    {
	return myFamily;
    }

    /**
     * Set the family ID.
     */
    public final void setFamily(int f)
    {
	myFamily = f;
 	myAppearance.setMaterial(getFamilyColors(myFamily));
    }

    /**
     * Get the body of the creature for the public to access it.
     */
    public final PublicBody getPublicBody()
    {
	return myPublicBody;
    }

    /**
     * Get the metabolic rate of this creature.
     */
    public final double getMetabolicRate()
    {
	return myMetabolicRate;
    }

    /**
     * How old?
     */
    public int getTickAge()
    {
	return myTickAge;
    }

    /**
     * Force a mutation.
     */
    public final void forceMutation(double factor)
    {
	myBrain.mutate(factor);
    }

    /**
     * Step this creature on one.
     *
     * @return A list of actions which resulted from this step.
     */
    public final List<Action> step(final Map<Class,Stimulus> stimuli)
    {
	// We age
	myTickAge++;

	// Get the brain to do some stuff
	if (World.LOG.isLoggable(Level.FINEST)) {
	    World.LOG.finest("Creating actions with brain");
	}
	myBody.removeEnergy(
	    Math.max(0.1, myBody.getMassKg() * ENERGY_PER_MASS_PER_TICK)
	);
	return myBrain.createActions(myBody, stimuli);
    }

    /**
     * Spawn another creature like us.
     */
    public final Creature spawn(double baseMass)
    {
	try {
	    Creature creature = 
		getClass().getConstructor(Double.class, Integer.class).newInstance(baseMass, myFamily);
	    creature.getBrain().copyGenes(myBrain);
	    return creature;
	}
	catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Spawn another creature by mating.
     */
    public final Creature mate(Creature partner, double baseMass)
    {
	try {
	    Creature creature = 
		getClass().getConstructor(Double.class, Integer.class).newInstance(baseMass, myFamily);
	    creature.getBrain().blendGenes(myBrain, partner.myBrain);
	    return creature;
	}
	catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Kill this creature off.
     */
    public void kill()
    {
	// Kill the body to do this
	myBody.kill();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
	return myId;
    }

    /**
     * Dump all the details of this creature to a string.
     */
    public String allToString()
    {
	return 
	    "<Creature=" + toString()         + ";" +
	    "Brain="     + myBrain.toString() + ">";
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
	return 
	    "<Class=" + this.getClass().getName()        + ";" +
	    "ID="     + Integer.toString(myId)           + ";" + 
	    "Family=" + Integer.toString(myFamily)       + ";" +
	    "Rate="   + Double.toString(myMetabolicRate) + ";" +
	    "Body="   + myBody.toString()                + ">";
    }

    // ========================= Protected Methods =========================

    /**
     * Get our brain.
     */
    protected final Brain getBrain()
    {
	return myBrain;
    }

    // ========================== Private Methods ==========================

    /**
     * Get the material for a given family.
     */
    private static Material getFamilyColors(int family)
    {
	Material m = ourFamilyColors.get(family);
	if (m == null) {
	    m = new Material();
	    // A bit of a hack but we say family zero (food) is blackish
	    if (family == 0) {
		m.setAmbientColor (0.05f, 0.05f, 0.05f);
		m.setDiffuseColor (0.00f, 0.10f, 0.00f);
		m.setSpecularColor(0.00f, 0.20f, 0.00f);
	    }
	    else {
		// Force not-too-dark
		float r = (float)Math.random();
		float g = (float)Math.random();
		float b = (float)Math.random();
		while (Math.sqrt(r*r + g*g + b*b) < 0.5) {
		    r = (float)Math.random();
		    g = (float)Math.random();
		    b = (float)Math.random();
		}
		m.setAmbientColor(r * 0.5f, g * 0.5f, b * 0.5f);
		m.setDiffuseColor(r,        g,        b);
		m.setSpecularColor(Math.max((float)Math.random(), r),
				   Math.max((float)Math.random(), g),
				   Math.max((float)Math.random(), b));
	    }
	    ourFamilyColors.put(family, m);
	}
	return m;
    }

    /**
     * Get the next ID.
     */
    private static int nextId()
    {
	return ourNextId++;
    }





    private void writeObject(ObjectOutputStream out)
	throws IOException
    {
	out.writeInt(myId);
	out.writeInt(myFamily);
	out.writeObject(myBrain);
	out.writeObject(myBody);
	out.writeDouble(myMetabolicRate);
	out.writeInt(myTickAge);
    }
    private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException
    {
	myId                   = in.readInt();
	myFamily               = in.readInt();
	myBrain                = (Brain)in.readObject();
	myBody                 = (Body)in.readObject();
	myAppearance           = new Appearance();
	myPublicBody           = new PublicBody(myBody, myAppearance);
	myMetabolicRate        = in.readDouble();
	myTickAge              = in.readInt();

 	myAppearance.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
 	myAppearance.setMaterial(getFamilyColors(myFamily));

	// Housekeeping
	if (myId >= ourNextId) {
	    ourNextId = myId + 1;
	}
	if (myFamily >= ourNextFamily) {
	    ourNextFamily = myFamily + 1;
	}
    }
    private void readObjectNoData() 
	throws ObjectStreamException
    {
	throw new RuntimeException("Can't do this right now...");
    }
    
}
