/**
 * How we set properties associated with the world.
 */
public class Properties
{
    /**
     * The number of threads to use
     */
    public static final int getNumThreads()
    {
        // One thread for drawing, the rest for us
	return getInteger("threads",
                          Runtime.getRuntime().availableProcessors() - 1);
    }

    /**
     * The size of the world's bounding box.
     */
    public static final double getBoundingBoxSize()
    {
	return getDouble("boundingBoxSize", 200.0);
    }

    /**
     * The maximum population size.
     */
    public static final int getMaxPopulation()
    {
	return getInteger("maxPopulation", 600);
    }

    /**
     * The maximum mass of a creature.
     */
    public static final double getMaxMass()
    {
	return getDouble("maxMass", 1000.0);
    }

    /**
     * The friction coefficient.
     */
    public static final double getFrictionCoeff()
    {
	return getDouble("frictionCoeff", 0.2);
    }

    /**
     * The air friction coefficient.
     */
    public static final double getAirFrictionCoeff()
    {
	return getDouble("airFrictionCoeff", 50.0);
    }

    /**
     * The gravity of the world in m/s^2.
     */
    public static final double getGravity()
    {
	return getDouble("gravity", 2.0);
    }

    /**
     * The fraction of the max population at which we can respawn.
     */
    public static final double getRespawnFraction()
    {
	return getDouble("respawnFraction", 0.1);
    }

    /**
     * The minimum amount of time between respawns of more creatures, in
     * milliseconds.
     */
    public static final long getRespawnMinIntervalMillis()
    {
	return getLong("respawnMinInterval", 90 * 1000);
    }

    /**
     * The time between feedings, in milliseconds.
     */
    public static final long getFeedIntervalMillis()
    {
	return getLong("feedInterval", 15 * 1000);
    }


    /**
     * Get a double property, with a default.
     */
    private static double getDouble(String key, double def)
    {
	String prop = System.getProperty(key);
	if (prop == null) {
	    return def;
	}
	else {
	    try {
		return Double.parseDouble(prop);
	    }
	    catch (NumberFormatException e) {
		throw new RuntimeException(
		    "Failed to parse double property: \"" + prop + "\"",
		    e
		);
	    }
	}
    }

    /**
     * Get an int property, with a default.
     */
    private static int getInteger(String key, int def)
    {
	String prop = System.getProperty(key);
	if (prop == null) {
	    return def;
	}
	else {
	    try {
		return Integer.parseInt(prop);
	    }
	    catch (NumberFormatException e) {
		throw new RuntimeException(
		    "Failed to parse integer property: \"" + prop + "\"",
		    e
		);
	    }
	}
    }

    /**
     * Get a long property, with a default.
     */
    private static long getLong(String key, long def)
    {
	String prop = System.getProperty(key);
	if (prop == null) {
	    return def;
	}
	else {
	    try {
		return Long.parseLong(prop);
	    }
	    catch (NumberFormatException e) {
		throw new RuntimeException(
		    "Failed to parse long property: \"" + prop + "\"",
		    e
		);
	    }
	}
    }
}
