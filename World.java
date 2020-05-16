import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Behavior;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Bounds;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Group;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupOnElapsedFrames;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.behaviors.mouse.MouseBehavior;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;

/**
 * The world in which everything lives.
 */
public class World
{
    // ============================== Types ==============================

    /**
     * A creature in the world.
     */
    private static class WorldCreature
	implements KDTree.Element, Serializable
    {
	/**
	 * The deletion behaviour. This is required to remove the object from the
	 * scene in the behaviour thread.
	 *
	 * See https://java3d.dev.java.net/issues/show_bug.cgi?id=572
	 */
	private class RemoveBehavior
	    extends Behavior
	{
	    /**
	     * Where to remove from?
	     */
	    private BranchGroup myRemovalGroup = null;

	    /**
	     * Constructor.
	     */
	    public RemoveBehavior()
	    {
		// Always trigger
		setSchedulingBounds(
		    new BoundingBox(
			new Point3d(Double.NEGATIVE_INFINITY,
				    Double.NEGATIVE_INFINITY,
				    Double.NEGATIVE_INFINITY),
			new Point3d(Double.POSITIVE_INFINITY,
				    Double.POSITIVE_INFINITY,
				    Double.POSITIVE_INFINITY)
		    )
		);
	    }

	    /**
	     * {@inheritDoc}
	     */
	    public void initialize()
	    {
		// Wake up as soon as possible to check whether we need to remove ourselves
		super.wakeupOn(new WakeupOnElapsedFrames(1));
		this.wakeupOn(new WakeupOnElapsedFrames(1));
	    }

	    /**
	     * Schedule for removal.
	     */
	    public void doRemove(BranchGroup from)
	    {
		myRemovalGroup = from;
	    }

	    /**
	     * {@inheritDoc}
	     */
	    public void processStimulus(Enumeration criteria)
	    {
		// If we have a group to remove ourselves from then do so,
		// otherwise wakeup at the next frame and check again
		if (myRemovalGroup != null) {
		    myRemovalGroup.removeChild(getBranchGroup());
		    getBranchGroup().detach();
		    destroy();
		}
		else {
		    super.wakeupOn(new WakeupOnElapsedFrames(1));
		    this.wakeupOn(new WakeupOnElapsedFrames(1));
		}
	    }
	}

	/**
	 * How many neighbours we're allowed before we can no longer
	 * spawn. This prevents millions of microbes from freezing the system
	 * (in theory).
	 */
	private int MAX_SPAWN_NEIGHBOURS = MAX_NEIGHBOURS;

	/**
	 * The origin.
	 */
	private static final Point3d ORIGIN = new Point3d(0.0, 0.0 ,0.0);

	/**
	 * Up direction.
	 */
	private static final Vector3d UP = new Vector3d(0.0, 0.0, 1.0);

	/**
	 * The health bar's green appearance.
	 */
	private static final Appearance HEALTH_GREEN;
	static {
	    HEALTH_GREEN = new Appearance();
	    Material material = new Material();
	    material.setAmbientColor (new Color3f(0.0f, 0.50f, 0.0f));
	    material.setDiffuseColor (new Color3f(0.0f, 0.50f, 0.0f));
	    material.setSpecularColor(new Color3f(0.0f, 0.75f, 0.0f));
	    HEALTH_GREEN.setMaterial(material);

	    TransparencyAttributes attr =
		new TransparencyAttributes(TransparencyAttributes.BLENDED, 0.25f);
	    HEALTH_GREEN.setTransparencyAttributes(attr);
	}

	/**
	 * The health bar's red appearance.
	 */
	private static final Appearance HEALTH_RED;
	static {
	    HEALTH_RED = new Appearance();
	    Material material = new Material();
	    material.setAmbientColor (new Color3f(0.25f, 0.0f, 0.0f));
	    material.setDiffuseColor (new Color3f(0.25f, 0.0f, 0.0f));
	    material.setSpecularColor(new Color3f(0.50f, 0.0f, 0.0f));
	    HEALTH_RED.setMaterial(material);

	    TransparencyAttributes attr =
		new TransparencyAttributes(TransparencyAttributes.BLENDED, 0.50f);
	    HEALTH_RED.setTransparencyAttributes(attr);
	}

	/**
	 * The shadow's appearance.
	 */
	private static final Appearance SHADOW;
	static {
	    SHADOW = new Appearance();

	    Material material = new Material();
	    material.setAmbientColor (new Color3f(0.00f, 0.00f, 0.00f));
	    material.setDiffuseColor (new Color3f(0.05f, 0.25f, 0.05f));
	    material.setSpecularColor(new Color3f(0.00f, 0.00f, 0.00f));
	    material.setShininess(128.0f);
	    SHADOW.setMaterial(material);

	    TransparencyAttributes attr =
		new TransparencyAttributes(TransparencyAttributes.BLENDED, 0.9f);
	    SHADOW.setTransparencyAttributes(attr);
	}

	/**
	 * How much to scale the health bar by.
	 */
	private final double HEALTH_SCALE = 10.0;

	/**
	 * Density
	 */
	private final double KG_PER_M3 = 10.0;

	/**
	 * Minimum time between spawning as a factor of metabolic rate.
	 */
	private final double SPAWN_INTERVAL_METABOLIC_MULTIPLIER = 1000.0;

	/**
	 * The actual creature.
	 */
	private Creature myCreature;

	/**
	 * The remover.
	 */
	private RemoveBehavior myRemoveBehavior = new RemoveBehavior();

	/**
	 * The location of this creature in the world.
	 */
	private Vector3d myPosition = new Vector3d();

	/**
	 * The velocity of this creature.
	 */
	private Vector3d myVelocity = new Vector3d();

	/**
	 * The velocity as it appears. This purely a cosmetic thing.
	 */
	private Vector3d myDisplayVelocity = new Vector3d(0.0, 0.0, 1.0);

	/**
	 * The transform associated with this creature (which puts it in the
	 * world).
	 */
	private TransformGroup myTransformGroup = new TransformGroup();

	/**
	 * The transform associated with the health bar of this creature.
	 */
	private TransformGroup myHealthTransformGroup = new TransformGroup();

	/**
	 * The transform associated with the shadow.
	 */
	private TransformGroup myShadowTransformGroup = new TransformGroup();

	/**
	 * Branch group which the transform lives
	 */
	private BranchGroup myBranchGroup = new BranchGroup();

	/**
	 * Scratch space, used for moving the creature about etc.
	 */
	private Transform3D myTransform3D = new Transform3D();

	/**
	 * Where my ID sits.
	 */
	private TransformGroup myIdGroup;

	/**
	 * The last time we stepped this creature.
	 */
	private long myLastStepWorldMillis = 0;

	/**
	 * The last time this creature spawned a child.
	 */
	private long myLastSpawnMillis = 0;

	/**
	 * Constructor.
	 */
	public WorldCreature(Creature c)
	{
	    // Remember the creature
	    myCreature = c;
	    init3d();
	}

	/**
	 * Tell this creature to remove itself.
	 */
	public void remove(BranchGroup from)
	{
	    myRemoveBehavior.doRemove(from);
	}

	/**
	 * Tell this object to destory its internal datastructures.
	 */
	public void destroy()
	{
	    // We need to tear down the tree when we get garbage collected
	    // since Java3D doesn't seem to do this for us very well!
	    destroy(myBranchGroup);
	}

	/**
	 * Get the creature.
	 */
	public Creature getCreature()
	{
	    return myCreature;
	}

	/**
	 * Get the transform of the creature.
	 */
	public TransformGroup getTransformGroup()
	{
	    return myTransformGroup;
	}

	/**
	 * Get the branch group of the creature.
	 */
	public BranchGroup getBranchGroup()
	{
	    return myBranchGroup;
	}

	/**
	 * Get the position of this creature.
	 */
	public Vector3d getPosition()
	{
	    return myPosition;
	}

	/**
	 * Get the velocity.
	 */
	public Vector3d getVelocity()
	{
	    return myVelocity;
	}

	/**
	 * Get the speed of this creature.
	 */
	public double getSpeed()
	{
	    return myVelocity.length();
	}

	/**
	 * The radius of this creatiure.
	 */
	public double getRadius()
	{
	    // Volume of a sphere is 4/3 * pi * r^3
	    // Volume is also mass / KG_PER_M3
	    final double volume = myCreature.getPublicBody().getMassKg() / KG_PER_M3;
	    return Math.pow(volume / (4 / 3 * Math.PI), 1.0 / 3.0);
	}

	/**
	 * How far this creature can see.
	 */
	public double getViewRadius()
	{
	    return getRadius() * 10.0; // XXX a little random
	}

	/**
	 * Get the last time this was stepped.
	 */
	public long getLastStepWorldMillis()
	{
	    return myLastStepWorldMillis;
	}

	/**
	 * Set the last time this was stepped.
	 */
	public void setLastStepWorldMillis(long t)
	{
	    myLastStepWorldMillis = t;
	}

	/**
	 * Get the last time this creature spawned.
	 */
	public long getLastSpawnMillis()
	{
	    return myLastSpawnMillis;
	}

	/**
	 * Set the last time this was stepped.
	 */
	public void setLastSpawnMillis(long t)
	{
	    myLastSpawnMillis = t;
	}

	/**
	 * Set the family ID.
	 */
	public void setFamily(int f)
	{
	    myCreature.setFamily(f);
	}

	/**
	 * Update this creature's display.
	 */
	public void updateDisplay()
	{
	    try {
		// Lag the velocity so it doesn't look to jittery when we
		// display it. As we tend to 1 we move the thing more slowly.
		final double lagFactor = 0.85;
		if (myDisplayVelocity.lengthSquared() > 0.0) {
		    myDisplayVelocity.scale(lagFactor);
		    Vector3d v = new Vector3d(myVelocity);
		    v.normalize();
		    v.scale(1.0 - lagFactor);
		    myDisplayVelocity.add(v);
		}
		else {
		    myDisplayVelocity.set(myVelocity);
		}

                // And normalise
                if (myDisplayVelocity.lengthSquared() > 0.0) {
                    myDisplayVelocity.normalize();
                }

		// What we will change
		myTransformGroup.getTransform(myTransform3D);

		// Set the direction of the transform matrix to be oriented
		// around the velocity axis. This is done my making the
		// transform into a rotation matrix which will turn _z_ into
		// _v_ by rotating about the axis between _z_ and _v_ (got by
		// the cross product) by the angle between them (from the dot
		// product). Since _z_ is a unit vector along one axis this
		// makes the cross and dot products pretty simple.
		if (myDisplayVelocity.lengthSquared() > 0.0) {
		    // Handle maths badness here
		    if (Math.abs(myDisplayVelocity.z + 1.0) < 0.0001) {
			// Full flip about any axis to point downwards
			myTransform3D.set(new AxisAngle4d(1.0, 0.0, 0.0, Math.PI));
		    }
		    else if (Math.abs(myDisplayVelocity.z - 1.0) < 0.0001) {
			// Identity, will point upwards
			myTransform3D.set(new AxisAngle4d(1.0, 0.0, 0.0, 0));
		    }
		    else {
			// Specific flip about x,y axis
			myTransform3D.set(new AxisAngle4d(-myDisplayVelocity.y,
							  myDisplayVelocity.x,
							  0.0,
							  Math.acos(myDisplayVelocity.z)));
		    }
		}

		// Now move and scale the axis
		myTransform3D.setTranslation(myPosition);
		myTransform3D.setScale(getRadius());

		// And set it back
		myTransformGroup.setTransform(myTransform3D);
	    }
	    catch (javax.media.j3d.BadTransformException e) {
		// If we failed just try again next time and it will probably
		// work
		System.err.println("Failed to transform with vel " + myDisplayVelocity);
	    }

	    // And now set the health
	    myHealthTransformGroup.getTransform(myTransform3D);
	    myTransform3D.setScale(new Vector3d(1.0, 1.0, HEALTH_SCALE * myCreature.getPublicBody().getHealth()));
	    myHealthTransformGroup.setTransform(myTransform3D);

	    // And the shadow
	    myShadowTransformGroup.getTransform(myTransform3D);
	    myTransform3D.set(new AxisAngle4d(1.0, 0.0, 0.0, Math.PI / 2.0));
	    final double scale = Math.min(myPosition.z, 0.5); // so it vanishes nr the ground
	    myTransform3D.setScale(new Vector3d(getRadius() * scale, 0.0, getRadius() * scale));
	    myTransform3D.setTranslation(new Vector3d(myPosition.x, myPosition.y, 0.0));
	    myShadowTransformGroup.setTransform(myTransform3D);
	}

	/**
	 * Can this creature spawn at this time?
	 */
	public boolean canSpawn(long now, Collection<VisibleCreature> neighbours)
	{
	    if (neighbours.size() > MAX_SPAWN_NEIGHBOURS) {
		return false;
	    }
	    final double rate = myCreature.getMetabolicRate();
	    final long tickMillis = (rate <= 0.0) ? 1 : Math.max(1, (long) (1000.0 / rate));
	    final long spawnMillis = (long)(tickMillis * SPAWN_INTERVAL_METABOLIC_MULTIPLIER);
	    return (now - myLastSpawnMillis > Math.max(100, spawnMillis));
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public String toString()
	{
	    return myCreature + "@" + myPosition + "x" + myVelocity;
	}


	/**
	 * Junk the contents of a node in the tree
	 */
	private void destroy(Node n)
	{
	    if (n instanceof Group) {
		Group g = (Group)n;
		for (int i=0; i < g.numChildren(); i++) {
		    destroy(g.getChild(i));
		}
		g.removeAllChildren();
	    }
	}

	/**
	 * Set up our 3d attributes.
	 */
	private void init3d()
	{
	    // We can be removed from the scene
	    myBranchGroup.setCapability(BranchGroup.ALLOW_DETACH);

	    // How the child dies
	    myBranchGroup.addChild(myRemoveBehavior);

	    // Set up the appearance of this creature; we should be able to
	    // move it about so we need to access the trasnforms
	    myTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    myTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
	    myTransformGroup.addChild(myCreature.getPublicBody().getShape());
	    myBranchGroup.addChild(myTransformGroup);

	    // The health bar
	    Box health = new Box(0.1f, 0.1f, 0.1f, HEALTH_GREEN);
	    myHealthTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    myHealthTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
	    myHealthTransformGroup.addChild(health);
	    myHealthTransformGroup.getTransform(myTransform3D);
	    myTransform3D.setTranslation(new Vector3d(0.0, 0.0, -1.0));
	    myHealthTransformGroup.setTransform(myTransform3D);
	    myTransformGroup.addChild(myHealthTransformGroup);
	    // and its max value
	    health = new Box(0.1f, 0.1f, 0.1f, HEALTH_RED);
	    TransformGroup tg = new TransformGroup();
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
	    tg.addChild(health);
	    tg.getTransform(myTransform3D);
	    myTransform3D.setTranslation(new Vector3d(0.0, 0.0, -1.0));
	    myTransform3D.setScale(new Vector3d(0.9, 0.9, HEALTH_SCALE));
	    tg.setTransform(myTransform3D);
	    myTransformGroup.addChild(tg);

	    // The shadow
	    myShadowTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    myShadowTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
	    for (float size = 0.5f; size <= 1.0f; size += 0.1f) {
		myShadowTransformGroup.addChild(new Cylinder(1.618f * size, 0.1f, SHADOW));
	    }
	    myBranchGroup.addChild(myShadowTransformGroup);

	    // Random placement
	    myPosition.x = 0.95 * BOUNDING_BOX_SIZE_M * (Math.random() - 0.5);
	    myPosition.y = 0.95 * BOUNDING_BOX_SIZE_M * (Math.random() - 0.5);
	    myPosition.z = 0.20 * BOUNDING_BOX_SIZE_M * (Math.random() - 0.0);

	    // Random speed
	    while (myVelocity.lengthSquared() == 0.0) {
		myVelocity.x = (Math.random() - 0.5);
		myVelocity.y = (Math.random() - 0.5);
		myVelocity.z = (Math.random() - 0.5);
	    }
	    myVelocity.normalize();
	    myVelocity.scale(MAX_SPEED * 0.25);
	}

	/**
	 * {@inheritDoc}
	 */
	private void writeObject(ObjectOutputStream out)
	    throws IOException
	{
	    out.writeObject(myCreature);
	    out.writeObject(myPosition);
	    out.writeObject(myVelocity);
	    out.writeObject(myDisplayVelocity);
	}

	/**
	 * {@inheritDoc}
	 */
	private void readObject(ObjectInputStream in)
	    throws IOException, ClassNotFoundException
	{
	    // Set up everything
	    myCreature             = (Creature)in.readObject();
	    myRemoveBehavior       = new RemoveBehavior();
	    myPosition             = (Vector3d)in.readObject();
	    Vector3d velocity      = (Vector3d)in.readObject();;
	    myDisplayVelocity      = (Vector3d)in.readObject();
	    myTransformGroup       = new TransformGroup();
	    myHealthTransformGroup = new TransformGroup();
	    myShadowTransformGroup = new TransformGroup();
	    myBranchGroup          = new BranchGroup();
	    myTransform3D          = new Transform3D();
	    myIdGroup              = new TransformGroup();
	    myLastStepWorldMillis  = 0;
	    myLastSpawnMillis      = 0;

	    // Now we should fix up our appearance
	    myVelocity = new Vector3d();
	    init3d();
	    myVelocity = velocity;
	}

	/**
	 * {@inheritDoc}
	 */
	private void readObjectNoData()
	    throws ObjectStreamException
	{
	    throw new RuntimeException("Can't do this right now...");
	}

    }

    /**
     * A creature as it can be seen by other creatures.
     *
     * <p>This is a wrapper class so that user can't cast it. This is as a
     * defence against people being underhand if we ever decide to have folks
     * creating creatures in a competition or similar. If one could cast a
     * create back to it's native class then one could do underhand things
     * with it. As such we make sure this is impossible by making this wrapper
     * class the only interface between creatures.
     */
    public static class VisibleCreature
	implements KDTree.Element
    {
	/**
	 * The WorldCreature which wwe wrap around.
	 */
	private final WorldCreature myWorldCreature;

	/**
	 * The velocity of the creature (lazy creation).
	 */
	private Vector3d myVelocity;

	/**
	 * Constructor.
	 */
	public VisibleCreature(WorldCreature wc)
	{
	    myWorldCreature = wc;
	}

	/**
	 * {@inheritDoc}
	 */
	public Vector3d getPosition()
	{
	    return myWorldCreature.getPosition();
	}

	/**
	 * The ID of this creature.
	 */
	public int getId()
	{
	    return myWorldCreature.getCreature().getId();
	}

	/**
	 * Is the creature alive?
	 */
	public boolean isAlive()
	{
	    return myWorldCreature.getCreature().getPublicBody().isAlive();
	}

	/**
	 * The family of this creature.
	 */
	public int getFamily()
	{
	    return myWorldCreature.getCreature().getFamily();
	}

	/**
	 * The radius of the creature.
	 */
	public double getRadius()
	{
	    return myWorldCreature.getRadius();
	}

	/**
	 * The mass of the creature.
	 */
	public double getMassKg()
	{
	    return myWorldCreature.getCreature().getPublicBody().getMassKg();
	}

	/**
	 * The velocity of the creature.
	 */
	public Vector3d getVelocity()
	{
	    // Lazy instatiation
	    if (myVelocity == null) {
		myVelocity = new Vector3d(myWorldCreature.getVelocity());
	    }
	    return myVelocity;
	}
	/**
	 * The mass of the creature.
	 */
	public double getHealth()
	{
	    return myWorldCreature.getCreature().getPublicBody().getHealth();
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString()
	{
	    return myWorldCreature.toString();
	}
    }

    /**
     * A stepping task.
     */
    private static class Steppee
    {
        public final WorldCreature creature;
        public final Map<WorldCreature,List<Action>> actions;
        public final KDTree<VisibleCreature> neighbourhood;

        public Steppee(WorldCreature creature)
        {
            this.creature      = creature;
            this.actions       = null;
            this.neighbourhood = null;
        }

        public Steppee(WorldCreature creature,
                       Map<WorldCreature,List<Action>> actions,
                       KDTree<VisibleCreature> neighbourhood)
        {
            this.creature      = creature;
            this.actions       = actions;
            this.neighbourhood = neighbourhood;
        }
    }

    /**
     * Steps a creature.
     */
    private class Stepper
        implements Runnable
    {
        public void run()
        {
            while (true) {
                try {
                    Steppee steppee = mySteppeeQueue.take();
                    stepCreature(steppee.creature,
                                 steppee.actions,
                                 steppee.neighbourhood);
                }
                catch (InterruptedException e) {
                    // Nothing
                }
            }
        }
    }

    // ============================== Members ==============================

    /**
     * The logger for the world.
     */
    public static final Logger LOG = Logger.getLogger("Life");

    /**
     * The maximum length we allow a tick to be in millis. This prevents weird
     * granularity errors from appearing on slow machines.
     */
    private static final int MAX_TICK_TIME_MS = 50;

    /**
     * Strict population limit.
     */
    private final int MAX_POPULATION = 1000;

    /**
     * The maximum number of neighbours we allow a creature to see. This
     * prevents massive slow-down when we have a lot of creatures all very
     * close to each other who have to take action on their neighbours.
     */
    private static final int MAX_NEIGHBOURS = 20;

    /**
     * The mass of one unit of energy in kg.
     */
    public static final double MASS_PER_ENERGY = 0.01;

    /**
     * The maximum mass of a creature.
     */
    public static final double MAX_MASS = Properties.getMaxMass();

    /**
     * How quickly things decay.
     */
    private static final double DECAY_KG_PER_SECOND = 0.1;

    /**
     * The size of the bounding box in meters.
     */
    private static final double BOUNDING_BOX_SIZE_M =
	Properties.getBoundingBoxSize();

    /**
     * Maximum speed at which any in the world my travel at (in m/s).
     */
    private static final double MAX_SPEED = 5.0;

    /**
     * The threshold below which we respawn.
     */
    private static final double RESPAWN_FRACTION =
	Properties.getRespawnFraction();

    /**
     * The maximum age a creature may live for in ticks.
     */
    private static final int MAX_TICK_AGE = 10000;

    /**
     * The food "family".
     */
    private final int myFoodFamily = Creature.nextFamily();

    /**
     * The friction coeff in this world.
     */
    private double myFrictionCoefficient = Properties.getFrictionCoeff();

    /**
     * The air friction coeff in this world (effectively viscosity).
     */
    private double myAirFrictionCoefficient = Properties.getAirFrictionCoeff();

    /**
     * The maximum population this would should probably have.
     */
    private int myMaxPopulation = Properties.getMaxPopulation();

    /**
     * The gravity of this world in m/s^2.
     */
    private double myGravity = Properties.getGravity();

    /**
     * How frequently to dump food into the world.
     */
    private long myFeedIntervalMillis = Properties.getFeedIntervalMillis();

    /**
     * The minimum amount of time which must pass between respawns. This
     * prevents us spamming the world with massive amounts of food and thus
     * making lazy, big, fat creatures.
     */
    private long myRespawnIntervalMillis = Properties.getRespawnMinIntervalMillis();

    /**
     * The elasticity coeff of the ground (and probably other things). 1.0
     * means perfectly elastic; 0.0 means not at all elastic.
     */
    private final double myElasticityCoeff = 0.5;

    /**
     * The last time the world stepped in actual time.
     */
    private long myLastStepRealMillis;

    /**
     * The last time the world stepped in world time.
     */
    private long myLastStepWorldMillis;

    /**
     * The last time we respawned creatures into the world.
     */
    private long myLastRespawnMillis;

    /**
     * The last time we respawned creatures into the world.
     */
    private long myLastFeedMillis;

    /**
     * The current world time.
     */
    private long myWorldMillis;

    /**
     * The counter which says how many steps we have had.
     */
    private long myStep;

    /**
     * The queue of stepper tasks.
     */
    private final BlockingQueue<Steppee> mySteppeeQueue =
        new ArrayBlockingQueue<Steppee>(2 * MAX_POPULATION);

    /**
     * The set of creatures in this world.
     */
    private Map<Integer,WorldCreature> myCreatures =
	new HashMap<Integer,WorldCreature>();

    /**
     * The new set of creatures for the world, from readWorld().
     */
    private Map<Integer,WorldCreature> myLoadedCreatures = null;

    /**
     * A cache of VisibleCreatures. We use this since we make brazillions of
     * these things and it kinda clogs up GC stuff.
     */
    private Map<WorldCreature,VisibleCreature> myVisibleCreatures =
	new HashMap<WorldCreature,VisibleCreature>();

    /**
     * A set of stimuli for a creature to get when it steps.
     */
    private Map<Integer,Map<Class,Stimulus>> myPendingStimuli =
	new HashMap<Integer,Map<Class,Stimulus>>();

    /**
     * The set of new creatures to add.
     */
    private Set<WorldCreature> myNewCreatures = new HashSet<WorldCreature>();

    /**
     * The set of all the family IDs. Changes every tick.
     */
    private Set<Integer> myFamilies = new HashSet<Integer>();

    /**
     * Where everything is displayed
     */
    private BranchGroup myDisplay = new BranchGroup();

    /**
     * The window in which things are displayed.
     */
    private SimpleUniverse myUniverse = null;

    // =========================== Constructors ============================

    /**
     * Constructor
     */
    public World()
    {
	// Init the times to all be the same
	myLastStepRealMillis      =
	    myLastStepWorldMillis =
	    myWorldMillis         =
	    System.currentTimeMillis();

	myLastRespawnMillis  =
	    myLastFeedMillis =
	    0;

        // Spawn the worker threads
        final int threads = Properties.getNumThreads();
        for (int i=0; i < threads; i++) {
            Thread t = new Thread(new Stepper(), "Stepper"+(i+1));
            t.setDaemon(true);
            t.start();
        }

	// Set up where we put things in this world
	myDisplay.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
	myDisplay.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
	myDisplay.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

	// Params...
	System.out.println("NUM_THREADS              = " + threads);
	System.out.println("AIR_FRICTION_COEFFICIENT = " + myAirFrictionCoefficient);
	System.out.println("BOUNDING_BOX_SIZE_M      = " + BOUNDING_BOX_SIZE_M);
	System.out.println("FEED_INTERVAL_MILLIS     = " + myFeedIntervalMillis);
	System.out.println("FRICTION_COEFFICIENT     = " + myFrictionCoefficient);
	System.out.println("GRAVITY                  = " + myGravity);
	System.out.println("MAX_MASS                 = " + MAX_MASS);
	System.out.println("MAX_POPULATION           = " + myMaxPopulation);
	System.out.println("RESPAWN_FRACTION         = " + RESPAWN_FRACTION);
	System.out.println("RESPAWN_INTERVAL_MILLIS  = " + myRespawnIntervalMillis);
   }

    // ========================== Public Methods ===========================

    /**
     * Set up.
     */
    public void init()
    {
        final GraphicsConfiguration config =
           SimpleUniverse.getPreferredConfiguration();
        final Rectangle screen = config.getBounds();
	final Canvas3D canvas  = new Canvas3D(config);
        final JFrame frame     = new JFrame(config);

	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final int size = Math.min(Math.min(1024, screen.width), screen.height);
	frame.setLayout(new BorderLayout());
        frame.setSize(new Dimension(size, size));
        frame.setLocation((screen.width  - size) / 2,
                          (screen.height - size) / 2);
        frame.add(canvas);
        frame.setVisible(true);

        // This will move the ViewPlatform back a bit so the
        // objects in the scene can be viewed.
	myUniverse = new SimpleUniverse(canvas);

	// How far back to draw stuff
	myUniverse.getViewer().getView().setBackClipDistance(BOUNDING_BOX_SIZE_M * 1000.0);

	// Where I am
	final ViewingPlatform platform = myUniverse.getViewingPlatform();
	platform.setNominalViewingTransform();

	// Look from above
	final Transform3D look = new Transform3D();
	look.lookAt(new Point3d (-BOUNDING_BOX_SIZE_M*0.7,
                                 -BOUNDING_BOX_SIZE_M*1.2,
                                  BOUNDING_BOX_SIZE_M*1.2),
                    new Point3d (0.0, 0.0, 0.0),
                    new Vector3d(0.0, 0.0, 1.0));
	look.invert();
	platform.getViewPlatformTransform().setTransform(look);

	// Create a simple scene and attach it to the virtual universe
	createSceneGraph();

	// How we will move the world about
	final BranchGroup    root        = new BranchGroup();
        final TransformGroup transformer = new TransformGroup(new Transform3D());
        transformer.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        transformer.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        root.addChild(transformer);

        final MouseRotate    mouseRotate    = new MouseRotate();
        final MouseTranslate mouseTranslate = new MouseTranslate();
        final MouseZoom      mouseZoom      = new MouseZoom(MouseBehavior.INVERT_INPUT);
	final BoundingSphere bounds         = new BoundingSphere(new Point3d(0.0, 0.0, 0.0),
                                                                 BOUNDING_BOX_SIZE_M * 1.0);
        mouseRotate   .setTransformGroup  (transformer);
        mouseRotate   .setSchedulingBounds(bounds);
        mouseTranslate.setTransformGroup  (transformer);
        mouseTranslate.setSchedulingBounds(bounds);
        mouseZoom     .setTransformGroup  (transformer);
        mouseZoom     .setSchedulingBounds(bounds);

        root.addChild(mouseTranslate);
        root.addChild(mouseRotate);
        root.addChild(mouseZoom);

	// And add it all to the universe
        transformer.addChild(myDisplay);
	myUniverse.addBranchGraph(root);

	// And set up the controls
	initControls();
    }

    private void initControls()
    {
	JFrame controls = new JFrame("World Controls");
	Container pane = controls.getContentPane();
	controls.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));

        JLabel  label;
	JSlider slider;

	// ----------------------------------------------------------------------

	// Population
        label = new JLabel("Max Population", JLabel.CENTER);
	label.setAlignmentX(Component.CENTER_ALIGNMENT);
	slider = new JSlider(JSlider.HORIZONTAL, 1, MAX_POPULATION, myMaxPopulation);
	slider.addChangeListener(
	    new ChangeListener()
	    {
		public void stateChanged(ChangeEvent e) {
		    JSlider source = (JSlider)e.getSource();
		    if (!source.getValueIsAdjusting()) {
			myMaxPopulation = (int)source.getValue();
			System.out.println("Max population now " + myMaxPopulation);
		    }
		}
	    }
	);
	pane.add(label);
	pane.add(slider);

	// Gravity
        label = new JLabel("Gravity", JLabel.CENTER);
	label.setAlignmentX(Component.CENTER_ALIGNMENT);
	slider = new JSlider(JSlider.HORIZONTAL,
			     0,
			     Math.max(100, (int)myGravity*20),
			     (int)(myGravity*20));
	slider.addChangeListener(
	    new ChangeListener()
	    {
		public void stateChanged(ChangeEvent e) {
		    JSlider source = (JSlider)e.getSource();
		    if (!source.getValueIsAdjusting()) {
			myGravity = (int)source.getValue() / 20.0;
			System.out.println("Gravity now " + myGravity);
		    }
		}
	    }
	);
	pane.add(new JSeparator());
	pane.add(label);
	pane.add(slider);

	// Air friction
        label = new JLabel("Air Friction", JLabel.CENTER);
	label.setAlignmentX(Component.CENTER_ALIGNMENT);
	slider = new JSlider(JSlider.HORIZONTAL,
			     0,
			     Math.max(100, (int)myAirFrictionCoefficient),
			     (int)myAirFrictionCoefficient);
	slider.addChangeListener(
	    new ChangeListener()
	    {
		public void stateChanged(ChangeEvent e) {
		    JSlider source = (JSlider)e.getSource();
		    if (!source.getValueIsAdjusting()) {
			myAirFrictionCoefficient = (int)source.getValue();
			System.out.println("Air fiction now " + myAirFrictionCoefficient);
		    }
		}
	    }
	);
	pane.add(new JSeparator());
	pane.add(label);
	pane.add(slider);

	// Friction
        label = new JLabel("Ground Friction", JLabel.CENTER);
	label.setAlignmentX(Component.CENTER_ALIGNMENT);
	slider = new JSlider(JSlider.HORIZONTAL,
			     0,
			     Math.max(100, (int)(myFrictionCoefficient*100)),
			     (int)myFrictionCoefficient);
	slider.addChangeListener(
	    new ChangeListener()
	    {
		public void stateChanged(ChangeEvent e) {
		    JSlider source = (JSlider)e.getSource();
		    if (!source.getValueIsAdjusting()) {
			myFrictionCoefficient = ((int)source.getValue()) / 100.0;
			System.out.println("Fiction now " + myFrictionCoefficient);
		    }
		}
	    }
	);
	pane.add(new JSeparator());
	pane.add(label);
	pane.add(slider);

	// Feed interval
        label = new JLabel("Feed Interval", JLabel.CENTER);
	label.setAlignmentX(Component.CENTER_ALIGNMENT);
	slider = new JSlider(JSlider.HORIZONTAL,
			     1,
			     Math.max(100, (int)myFeedIntervalMillis/1000),
			     (int)(myFeedIntervalMillis/1000));
	slider.addChangeListener(
	    new ChangeListener()
	    {
		public void stateChanged(ChangeEvent e) {
		    JSlider source = (JSlider)e.getSource();
		    if (!source.getValueIsAdjusting()) {
			myFeedIntervalMillis = (int)source.getValue() * 1000;
			System.out.println("Feed interval now " + myFeedIntervalMillis/1000 + "s");
		    }
		}
	    }
	);
	pane.add(new JSeparator());
	pane.add(label);
	pane.add(slider);

	// Respawn interval
        label = new JLabel("Respawn Interval", JLabel.CENTER);
	label.setAlignmentX(Component.CENTER_ALIGNMENT);
	slider = new JSlider(JSlider.HORIZONTAL,
			     0,
			     Math.max(100, (int)myRespawnIntervalMillis/1000),
			     (int)(myRespawnIntervalMillis/1000));
	slider.addChangeListener(
	    new ChangeListener()
	    {
		public void stateChanged(ChangeEvent e) {
		    JSlider source = (JSlider)e.getSource();
		    if (!source.getValueIsAdjusting()) {
			myRespawnIntervalMillis = (int)source.getValue() * 1000;
			System.out.println("Respawn interval now " + myRespawnIntervalMillis/1000 + "s");
		    }
		}
	    }
	);
	pane.add(new JSeparator());
	pane.add(label);
	pane.add(slider);

	// Load and restore
	final JTextField filename = new JTextField();
	filename.setText("state");
	JButton save = new JButton("Save");
	save.addActionListener(
	    new ActionListener() {
		public void actionPerformed(ActionEvent event)
		{
		    try {
			writeWorld(filename.getText());
		    }
		    catch (Exception e) {
			System.err.println("Failed to write out world: " + e);
		    }
		}
	    }
	);
	JButton load = new JButton("Load");
	load.addActionListener(
	    new ActionListener() {
		public void actionPerformed(ActionEvent event)
		{
		    try {
			readWorld(filename.getText());
		    }
		    catch (Exception e) {
			throw new RuntimeException(e);
		    }
		}
	    }
	);

	// The bits
        label = new JLabel("State", JLabel.CENTER);
	label.setAlignmentX(Component.CENTER_ALIGNMENT);
	JPanel panel1 = new JPanel();
	panel1.setLayout(new BoxLayout(panel1, BoxLayout.LINE_AXIS));
        panel1.add(new JLabel("Filename "));
	panel1.add(filename);
	JPanel panel2 = new JPanel();
	panel2.setLayout(new BoxLayout(panel2, BoxLayout.LINE_AXIS));
	panel2.add(load);
        panel2.add(new JLabel(" "));
	panel2.add(save);

	pane.add(new JSeparator());
	pane.add(label);
	pane.add(panel1);
	pane.add(panel2);

	// ----------------------------------------------------------------------

	// Pack and display
	controls.pack();
	controls.setVisible(true);
    }

    /**
     * Add a creature to the world.
     */
    public synchronized void addCreature(Creature c)
    {
	addCreature(new WorldCreature(c));
    }

    /**
     * Let the world run!
     */
    public void run()
    {
	while (true) {
	    // Step things along
	    if (step()) {
		// Display all the creatures
		display();
	    }
	}
    }

    /**
     * {@inheritDoc}
     */
    public void destroy()
    {
	try {
	    myUniverse.cleanup();
	}
	catch (Throwable t) {
	    System.exit(1);
	}
    }

    // ========================== Private Methods ==========================

    /**
     * Create a creature.
     */
    private static Creature createCreature(double mass, int family)
    {
	switch (family % 20) {
	case 0:  return new CreatureAttackee   (mass, family);
	case 1:  return new CreatureAttacker   (mass, family);
	case 2:  return new CreatureDead       (mass, family);
	case 3:  return new CreatureFeeder     (mass, family);
	case 4:  return new CreatureFlocker    (mass, family);
	case 5:  return new CreatureHomer      (mass, family);
	case 6:  return new CreatureHunter     (mass, family);
	case 7:  return new CreatureRandom     (mass, family);
	case 8:  return new CreatureSheep      (mass, family);
	case 9:  return new CreatureWolf       (mass, family);
	default: return new CreatureKitchenSink(mass, family);
	}
    }

    /**
     * Create the scene which things live in.
     */
    private BranchGroup createSceneGraph()
    {
	// -------------------------------------------------------------------

	// Lights
	final double bb = BOUNDING_BOX_SIZE_M;
	BoundingBox bounds =
	    new BoundingBox(new Point3d(-bb, -bb, -bb),
			    new Point3d( bb,  bb,  bb));

	Color3f lightColour = new Color3f(1.0f, 1.0f, 1.0f);
	DirectionalLight directionalLight =
	    new DirectionalLight(lightColour,
				 new Vector3f(-0.2f, -0.2f, -1.0f));
	directionalLight.setInfluencingBounds(bounds);
	AmbientLight ambientLight = new AmbientLight(lightColour);
	ambientLight.setInfluencingBounds(bounds);

	BranchGroup bg = new BranchGroup();
	bg.addChild(directionalLight);
	bg.addChild(ambientLight);
	myDisplay.addChild(bg);

	// -------------------------------------------------------------------

	// Draw the floor
	Transform3D            floorPs = new Transform3D();
	TransformGroup         floorTg = new TransformGroup();
	BranchGroup            floorBg = new BranchGroup();
	Appearance             floorAp = new Appearance();
	ColoringAttributes     floorCa = new ColoringAttributes();
        TransparencyAttributes floorTa =
            new TransparencyAttributes(TransparencyAttributes.BLENDED, 0.25f);

	floorPs.setTranslation(new Vector3d(0.0, 0.0, -5.0));
	floorCa.setColor(0.1f, 0.5f, 0.1f);
	floorAp.setColoringAttributes(floorCa);
        floorAp.setTransparencyAttributes(floorTa);
	floorTg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	floorTg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
	floorTg.addChild(new Box((float)BOUNDING_BOX_SIZE_M / 2.0f,
				 (float)BOUNDING_BOX_SIZE_M / 2.0f,
				 (float)1.0f,
				 floorAp));
	floorTg.setTransform(floorPs);
	floorBg.addChild(floorTg);
	myDisplay.addChild(floorBg);

	// -------------------------------------------------------------------

	// Done
	return myDisplay;
    }

    /**
     * Add a world creature to the world.
     */
    private void addCreature(WorldCreature wc)
    {
	myCreatures.put(wc.getCreature().getId(), wc);
	wc.updateDisplay();
	myDisplay.addChild(wc.getBranchGroup());
    }

    /**
     * Remove a world creature from the world.
     */
    private void removeCreature(WorldCreature wc)
    {
	removeCreature(wc.getCreature().getId());
    }

    /**
     * Remove a world creature from the world.
     */
    private synchronized void removeCreature(int id)
    {
	if (World.LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Trashing creature");
        }
	WorldCreature creature = myCreatures.remove(id);
	if (creature != null) {
	    // Remove visible cached object
	    myVisibleCreatures.remove(creature);
	    creature.remove(myDisplay);
	}
    }

    /**
     * Get a visible creature instance from the cache for a creature.
     */
    private VisibleCreature getVisibleCreature(WorldCreature creature)
    {
	VisibleCreature result = myVisibleCreatures.get(creature);
	if (result == null) {
	    result = new VisibleCreature(creature);
	    myVisibleCreatures.put(creature, result);
	}
	return result;
    }

    /**
     * Write out the state of the world to disk.
     */
    private synchronized void writeWorld(String filename)
	throws FileNotFoundException, IOException
    {
	FileOutputStream   fos = new FileOutputStream(filename);
	ObjectOutputStream oos = new ObjectOutputStream(fos);
	oos.writeObject(myCreatures);
    }

    /**
     * Read the state of the world in from disk.
     */
    @SuppressWarnings("unchecked")
    private void readWorld(String filename)
	throws ClassCastException, ClassNotFoundException, FileNotFoundException, IOException
    {
	FileInputStream   fis = new FileInputStream(filename);
	ObjectInputStream ois = new ObjectInputStream(fis);

	// We simply set the loaded creatures here since it makes the whole
	// thread thing a lot easier
	myLoadedCreatures = (Map<Integer,WorldCreature>)ois.readObject();
    }

    /**
     * Move the world on a step.
     */
    private boolean step()
    {
	// New set of creatures given?
	if (myLoadedCreatures != null) {
	    Map<Integer,WorldCreature> newCreatures = myLoadedCreatures;
	    myLoadedCreatures = null;

	    // Swap the two sets around
	    List<Integer> ids = new ArrayList<Integer>();
	    for (WorldCreature creature : myCreatures.values()) {
		ids.add(creature.getCreature().getId());
	    }
	    for (Integer id : ids) {
		removeCreature(id);
	    }
	    for (WorldCreature creature : newCreatures.values()) {
		addCreature(creature);
	    }
	    myNewCreatures.clear();

	    // Done, so display
	    return true;
	}

	// How much time has passed. We see how much time has passed in the
	// real world but cap it. As such we need to keep an internal clock
	// too.
        final long wantMillis = 10;
        long now = System.currentTimeMillis();
        long millisPast = Math.min(MAX_TICK_TIME_MS, now - myLastStepRealMillis);
        while (millisPast <= wantMillis) {
            // Ensure some time has passed when we are running really fast
            try {
                Thread.sleep(wantMillis - millisPast);
            }
            catch (InterruptedException e) {
                // Nothing
            }

            // Try again
            now = System.currentTimeMillis();
            millisPast = Math.min(MAX_TICK_TIME_MS, now - myLastStepRealMillis);
        }

	// Increment the step counter
	myStep++;

	// Safe to set these now
	myLastStepRealMillis = now;
	myWorldMillis += millisPast;

	// The set of actions we perform when everyone has been stepped.
	// We rely on the fact that hashes are vaguely random in ordering
	// to make things fair.
	Map<WorldCreature,List<Action>> actions =
	    new ConcurrentHashMap<WorldCreature,List<Action>>();

	// Create the kdtree with everyone's position in it; we also figure
	// out the set of families here
	myFamilies.clear();
	myFamilies.add(myFoodFamily);
	KDTree<VisibleCreature> tree = new KDTree<VisibleCreature>();
	for (Map.Entry<Integer,WorldCreature> entry : myCreatures.entrySet()) {
	    tree.add(getVisibleCreature(entry.getValue()));
	    myFamilies.add(entry.getValue().getCreature().getFamily());
	}

	// Handle stuff for each creature
	Set<Integer> toRemove = new HashSet<Integer>();
	for (Map.Entry<Integer,WorldCreature> entry : myCreatures.entrySet()) {
	    // Get the creature
	    WorldCreature creature = entry.getValue();
	    if (LOG.isLoggable(Level.FINEST)) {
		LOG.finest(myWorldMillis + ": Working for " + creature);
	    }

	    // Is the creature alive?
	    if (!creature.getCreature().getPublicBody().isAlive()) {
		// Make sure this is marked as food
		if (creature.getCreature().getFamily() != myFoodFamily) {
		    creature.getCreature().setFamily(myFoodFamily);
		}

		// Decay the creature; all things die
		decayCreature(creature);
	    }

	    // If the creature is no more then remove it from the world
	    if (creature.getCreature().getPublicBody().getMassKg() <= 0.0) {
		// Trash empty creatures
		toRemove.add(entry.getKey());
	    }
	    else {
		// Time to kill?
		if (creature.getCreature().getPublicBody().isAlive() &&
		    creature.getCreature().getTickAge() > MAX_TICK_AGE)
		{
		    creature.getCreature().kill();
		}

                // Keep trying to add this creature to the action queue
                boolean added = false;
                while (!added) {
                    try {
                        // If the creature's alive then we add it to
                        // be stepped with actions, else we don't
                        if (creature.getCreature().getPublicBody().isAlive()) {
                            mySteppeeQueue.put(new Steppee(creature, actions, tree));
                        }
                        else {
                            mySteppeeQueue.put(new Steppee(creature));
                        }
                        added = true;
                    }
                    catch (InterruptedException e) {
                        // Will retry
                    }
                }
	    }
	}

        // Busyish wait for the queue to be done
        while (!mySteppeeQueue.isEmpty()) {
            Thread.yield();
        }

	// Remove any dead creatures
	for (Integer id : toRemove) {
	    removeCreature(id);
	}

	// Handle any pending actions
	for (Map.Entry<WorldCreature,List<Action>> entry : actions.entrySet()) {
	    handleActions(entry.getKey(), entry.getValue(), tree);
	}

	// Add some food
	populateWorld();

	// Split families now and again
	if (myStep % 100 == 0) {
	    breakMonopolies();
	}

	// Add any pending creatures to the world
	if (!myNewCreatures.isEmpty()) {
	    for (WorldCreature c : myNewCreatures) {
		addCreature(c);
	    }
	    myNewCreatures.clear();
	}

	// Every now and again do stuff
	if (myStep % 1000 == 0) {
	    // Let the user know what's going on
	    printStats();
	}

	// We stepped
	myLastStepWorldMillis = myWorldMillis;
	return true;
    }

    /**
     * Step a creature (in a different thread).
     */
    private void stepCreature(WorldCreature creature,
                              Map<WorldCreature,List<Action>> actions,
                              KDTree<VisibleCreature> neighbourhood)
    {
        if (actions != null && neighbourhood != null) {
            // Step the creature (i.e. give it a chance to do
            // something)
            Collection<VisibleCreature> neighbours =
                neighbourhood.get(creature.getPosition(),
                                  creature.getViewRadius());

            // Restrict the view to a "random" subset?
            if (neighbours.size() > MAX_NEIGHBOURS) {
                // Remove every Xth node until we have a managable size
                final int which = neighbours.size() / MAX_NEIGHBOURS + 2;
                while (neighbours.size() > MAX_NEIGHBOURS) {
                    Iterator<VisibleCreature> itr = neighbours.iterator();
                    int count = 0;
                    while (itr.hasNext()) {
                        if (neighbours.size() <= MAX_NEIGHBOURS) {
                            break;
                        }
                        VisibleCreature c = itr.next();
                        if (count++ % which == 0) {
                            itr.remove();
                        }
                    }
                }
            }

            // Step the creature and remember the actions
            actions.put(creature, stepCreature(creature, neighbours));
        }

        // Apply any forces on the creature
        applyWorldForces(creature);

        // Move the creature according to its velocity
        moveCreature(creature);
    }

    /**
     * Step this creature on one (or more, depending on how much time has
     * passed).
     */
    private void decayCreature(final WorldCreature creature)
    {
	assert(!creature.getCreature().getPublicBody().isAlive());

	// How much mass to take
	double secondsPassed = (double)(myWorldMillis - myLastStepWorldMillis) / 1000.0;
	double mass = secondsPassed * DECAY_KG_PER_SECOND;

	// Take it
	if (LOG.isLoggable(Level.FINEST)) {
	    LOG.finest(myWorldMillis + ": Taking " + mass + "kg from " + creature);
	}
	creature.getCreature().getPublicBody().feedFrom(mass);
    }

    /**
     * Step this creature on one (or more, depending on how much time has
     * passed).
     */
    private List<Action> stepCreature(final WorldCreature creature,
				      final Collection<VisibleCreature> neighbours)
    {
	// We step this creature only if enough time has passed for its
	// metabolic rate
	final double rate = creature.getCreature().getMetabolicRate();
	final long tickMillis = (rate <= 0.0) ? 1 : Math.max(1, (long) (1000.0 / rate));
	if (myWorldMillis - creature.getLastStepWorldMillis() < tickMillis) {
	    return new ArrayList<Action>();
	}

	// What's the deal?
	if (LOG.isLoggable(Level.FINEST)) {
	    LOG.finest(myWorldMillis + ": Stepping creature");
	}

	// Set set of stimuli for this creature
	Map<Class,Stimulus> stimuli = new HashMap<Class,Stimulus>();
	stimuli.put(StimulusTimeMillis.class, new StimulusTimeMillis(myWorldMillis));
	stimuli.put(StimulusNeighbours.class, new StimulusNeighbours(neighbours));
	stimuli.put(StimulusSelf.class,       new StimulusSelf(getVisibleCreature(creature),
							       canSpawn(creature, neighbours)));
	stimuli.put(StimulusFamilies.class,   new StimulusFamilies(myFamilies));

	// And anything waiting
	Map<Class,Stimulus> pending = myPendingStimuli.get(creature.getCreature().getId());
	if (pending != null) {
	    stimuli.putAll(pending);
	    pending.clear();
	}

	// Step it on one and handle any actions
	return creature.getCreature().step(stimuli);
    }

    /**
     * Apply forces to the creature. This will cause it to accelerate etc.
     */
    private void applyWorldForces(final WorldCreature creature)
    {
	if (LOG.isLoggable(Level.FINEST)) {
	    LOG.finest(myWorldMillis + ": Applying forces to " + creature);
	}

	// How many millis have elapsed since the last time we ticked
	final double secondsPassed =
	    (double)(myWorldMillis - myLastStepWorldMillis) / 1000.0;

	// The mass and weight of the creature
	final double mass   = creature.getCreature().getPublicBody().getMassKg();
	final double weight = mass * myGravity;

	// Round to ground; we figure out how far something can travel owing
	// to gravity from rest and then clamp accordingly. This is here to
	// prevent things wiggling about, owing to maths and granularity
	// errors, when they are supposed to be stationary and sitting on the
	// ground.
	if (creature.getVelocity().z < myGravity * MAX_TICK_TIME_MS / 1000) {
	    final double gravStep =
		myGravity * MAX_TICK_TIME_MS * MAX_TICK_TIME_MS / 1000 / 1000;
	    if (Math.abs(creature.getPosition().z) < gravStep) {
		creature.getPosition().z = 0.0;
	    }
	}

	// First we figure out if we need to apply any friction
	Vector3d frictionForceVector = new Vector3d();
	if (myFrictionCoefficient > 0.0 &&
	    creature.getPosition().z == 0.0 &&
	    creature.getSpeed() > 0.0)
	{
	    // Yes, we can apply friction
	    final double frictionForce = myFrictionCoefficient * weight;

	    // The friction will act in the opposite direction to the velocity
	    // but only along the ground
	    Vector3d friction = new Vector3d(creature.getVelocity());
	    friction.z = 0.0;

	    // Anything to do?
	    if (friction.lengthSquared() > 0.0 ) {
		// Now figure out what the change in speed will be; create the
		// force (in the opposite direction)
		friction.normalize();
		friction.scale(-frictionForce);
		frictionForceVector.add(friction);
	    }
	}

	// Now air frcition
	if (myAirFrictionCoefficient > 0.0 && creature.getSpeed() > 0.0) {
	    // Yes, we can apply friction; this will be proportional to our
	    // cross-sectional area and how fast we are going relative to the
	    // air (which is currently stationary)
	    final double area = Math.PI * creature.getRadius() * creature.getRadius();
	    final double density = getAirDensity(creature.getPosition().z);
	    final double frictionForce =
		myAirFrictionCoefficient * density * area * creature.getSpeed();

	    // The friction will act in the opposite direction to the velocity
	    // so we base it off that
	    Vector3d friction = new Vector3d(creature.getVelocity());

	    // Anything to do?
	    if (friction.lengthSquared() > 0.0) {
		// Now figure out what the change in speed will be; create the
		// force (in the opposite direction)
		friction.normalize();
		friction.scale(-frictionForce);
		frictionForceVector.add(friction);
	    }
	}

	// Any friction to add?
	if (frictionForceVector.lengthSquared() > 0.0) {
	    // Now we turn it into acceleration
	    frictionForceVector.scale(1.0 / mass);

	    // Now we turn that into a velocity (which we will apply as a difference)
	    frictionForceVector.scale(secondsPassed);

	    // However, we make sure not to push the thing backwards! That
	    // would just be silly. Owing to the discrete nature of time in
	    // this world this is a problem though.
	    if (frictionForceVector.length() > creature.getVelocity().length()) {
		// Stop the thing
		creature.getVelocity().scale(0.0);
	    }
	    else {
		// Change the velocity accordingly
		creature.getVelocity().add(frictionForceVector);
	    }
	}

	// Now we apply gravity; first figure it out as an acceleration. Some
	// bloke in dropping things off the Tower of Piza tells us that this
	// is constant for any mass.
	if (creature.getPosition().z > 0.0) {
	    Vector3d gravity = new Vector3d(0.0, 0.0, -myGravity);

	    // Now turn it into a velocity change
	    gravity.scale(secondsPassed);

	    // And apply it to the velocity
	    creature.getVelocity().add(gravity);
	}
    }

    /**
     * Move the creature according to its velocity.
     */
    private void moveCreature(final WorldCreature creature)
    {
	// Does the world have a donut topology?
	final boolean donut = false;

	// Cap the velocity to a sane value
	if (creature.getVelocity().lengthSquared() > MAX_SPEED * MAX_SPEED) {
	    creature.getVelocity().normalize();
	    creature.getVelocity().scale(MAX_SPEED);
	}

	if (LOG.isLoggable(Level.FINEST)) {
	    LOG.finest(myWorldMillis + ": Moving " + creature);
	}

	// Apply the velocity to the creature
	creature.getPosition().add(creature.getVelocity());

	// Make sure we don't go below ground
	if (creature.getPosition().z < 0.0) {
	    // Bounce!
	    if (LOG.isLoggable(Level.FINEST)) {
		LOG.finest(myWorldMillis + ": Z-bouncing " + creature);
	    }
	    creature.getPosition().z = -creature.getPosition().z;
	    creature.getVelocity().z = -myElasticityCoeff * creature.getVelocity().z;
	}

	// Bounding box
	if (creature.getPosition().x < -BOUNDING_BOX_SIZE_M / 2.0) {
	    if (donut) {
		if (LOG.isLoggable(Level.FINEST)) {
		    LOG.finest(myWorldMillis + ": X-Wrapping " + creature);
		}
		creature.getPosition().x += BOUNDING_BOX_SIZE_M;
	    }
	    else {
		if (LOG.isLoggable(Level.FINEST)) {
		    LOG.finest(myWorldMillis + ": X-Bouncing " + creature);
		}
		creature.getPosition().x = -BOUNDING_BOX_SIZE_M - creature.getPosition().x;
		creature.getVelocity().x = -myElasticityCoeff * creature.getVelocity().x;
	    }
	}
	if (creature.getPosition().x > BOUNDING_BOX_SIZE_M / 2.0) {
	    if (donut) {
		if (LOG.isLoggable(Level.FINEST)) {
		    LOG.finest(myWorldMillis + ": X-Wrapping " + creature);
		}
		creature.getPosition().x -= BOUNDING_BOX_SIZE_M;
	    }
	    else {
		if (LOG.isLoggable(Level.FINEST)) {
		    LOG.finest(myWorldMillis + ": X-Bouncing " + creature);
		}
		creature.getPosition().x = BOUNDING_BOX_SIZE_M - creature.getPosition().x;
		creature.getVelocity().x = -myElasticityCoeff * creature.getVelocity().x;
	    }
	}
	if (creature.getPosition().y < -BOUNDING_BOX_SIZE_M / 2.0) {
	    if (donut) {
		if (LOG.isLoggable(Level.FINEST)) {
		    LOG.finest(myWorldMillis + ": Y-Wrapping " + creature);
		}
		creature.getPosition().y += BOUNDING_BOX_SIZE_M;
	    }
	    else {
		if (LOG.isLoggable(Level.FINEST)) {
		    LOG.finest(myWorldMillis + ": Y-Bouncing " + creature);
		}
		creature.getPosition().y = -BOUNDING_BOX_SIZE_M - creature.getPosition().y;
		creature.getVelocity().y = -myElasticityCoeff * creature.getVelocity().y;
	    }
	}
	if (creature.getPosition().y > BOUNDING_BOX_SIZE_M / 2.0) {
	    if (donut) {
		if (LOG.isLoggable(Level.FINEST)) {
		    LOG.finest(myWorldMillis + ": Y-Wrapping " + creature);
		}
		creature.getPosition().y -= BOUNDING_BOX_SIZE_M;
	    }
	    else {
		if (LOG.isLoggable(Level.FINEST)) {
		    LOG.finest(myWorldMillis + ": Y-Bouncing " + creature);
		}
		creature.getPosition().y = BOUNDING_BOX_SIZE_M - creature.getPosition().y;
		creature.getVelocity().y = -myElasticityCoeff * creature.getVelocity().y;
	    }
	}

	// How far did we go?
	if (LOG.isLoggable(Level.FINEST)) {
	    LOG.finest(myWorldMillis + ": Moved " + creature + " to " + creature.getPosition());
	}
    }

    /**
     * Handle any actions from this creature.
     */
    private void handleActions(final WorldCreature creature,
			       List<Action> actions,
			       KDTree<VisibleCreature> tree)
    {
	// This creature may have been killed before it got a chance to do
	// anything so we check for that
	if (!creature.getCreature().getPublicBody().isAlive()) {
	    return;
	}

	// Don't allow attacks more than once on a victim
	Set<Integer> victims = new HashSet<Integer>();

	// Handle each action
	if (LOG.isLoggable(Level.FINEST)) {
	    LOG.finest(myWorldMillis + ": Handling actions for " + creature);
	}
	for (Action action : actions) {
	    // Test each casting case
	    if (LOG.isLoggable(Level.FINEST)) {
		LOG.finest(myWorldMillis + ": Handling action " + action);
	    }
	    if (action instanceof ActionAttack) {
		// How we attack
		ActionAttack attack = (ActionAttack)action;

		// Only attack once
		if (victims.contains(attack.id)) {
		    continue;
		}
		victims.add(attack.id);

		// Find our victim which should be alive right now
		WorldCreature victim = myCreatures.get(attack.id);
		if (victim != null && victim.getCreature().getPublicBody().isAlive() &&
		    victim.getCreature().getId() != creature.getCreature().getId())
		{
		    // Make sure it's within range
		    final Vector3d dist = new Vector3d(victim.getPosition());
		    dist.sub(creature.getPosition());
		    final double radii = creature.getRadius() + victim.getRadius();
		    if (dist.lengthSquared() < radii * radii) {
			// Okay we can attack this guy; this will remove some
			// energy from them
			final double creatureMass =
			    creature.getCreature().getPublicBody().getMassKg();
			final double victimMass =
			    victim.getCreature().getPublicBody().getMassKg();

			// The heavier we are the more we can attack someone
			double energy = creatureMass / 100.0 / MASS_PER_ENERGY;
			energy *= Math.pow(creatureMass / victimMass, 1.0);
			if (false) {
			    LOG.severe(creature + " attacks " +
				       victim + " with energy " +
				       energy);
			}

			if (energy > 0.0) {
			    victim.getCreature().getPublicBody().removeEnergy(energy);

			    // This creature should remember it was attacked now
			    sendAttackStimulus(victim, creature,
					       energy / victim.getCreature().getPublicBody().getEnergy());
			}
		    }
		}
	    }
	    else if (action instanceof ActionFeed) {
		// How we feed
		ActionFeed feed = (ActionFeed)action;

		// Find our victim
		WorldCreature food = myCreatures.get(feed.id);
		if (food != null && !food.getCreature().getPublicBody().isAlive() &&
		    food.getCreature().getId() != creature.getCreature().getId())
		{
		    // Make sure it's within range
		    final Vector3d dist = new Vector3d(food.getPosition());
		    dist.sub(creature.getPosition());
		    final double radii = creature.getRadius() + food.getRadius();
		    if (dist.lengthSquared() < radii * radii) {
			// Okay we can feed from this guy, figure out how much
			// food we can take
			final double needEnergy =
			    creature.getCreature().getPublicBody().maxEnergy() -
			    creature.getCreature().getPublicBody().getEnergy();
			final double mass =
			    food.getCreature().getPublicBody().feedFrom(needEnergy * MASS_PER_ENERGY);
			creature.getCreature().getPublicBody().addEnergy(mass / MASS_PER_ENERGY);
		    }
		}
	    }
	    else if (action instanceof ActionMate) {
		// Who's around this guy
		Collection<VisibleCreature> neighbours =
		    tree.get(creature.getPosition(), creature.getViewRadius());
		if (canSpawn(creature, neighbours)) {
		    // How we shag!
		    ActionMate mate = (ActionMate)action;

		    // Find our partner which should be alive right now and a
		    // member of the same family
		    WorldCreature partner = myCreatures.get(mate.id);
		    if (partner != null && partner.getCreature().getPublicBody().isAlive() &&
			partner.getCreature().getId() != creature.getCreature().getId() &&
			partner.getCreature().getFamily() == creature.getCreature().getFamily())
		    {
			// Make sure it's within range
			final Vector3d dist = new Vector3d(partner.getPosition());
			dist.sub(creature.getPosition());
			final double radii = creature.getRadius() + partner.getRadius();
			if (dist.lengthSquared() < radii * radii) {
			    // Attempt to create it
			    Creature child =
				creature.getCreature().mate(partner.getCreature(),
							    mate.baseMass);
			    child.getPublicBody().addEnergy(mate.energy);

			    // Conservation of momentum
			    final double momentum =
				child.getPublicBody().getMassKg() * mate.velocity.length();
			    if (!Double.isNaN(momentum) && momentum > 0.0) {
				// This essentially becomes a force in the opposite
				// direction which we handle by preserving momentum
				Vector3d velChange = new Vector3d(mate.velocity);
				velChange.normalize();
				velChange.negate();

				// The speed imparted to the creature depends on its mass
				final double speed =
				    momentum / creature.getCreature().getPublicBody().getMassKg();
				velChange.scale(speed);

				// Now apply it
				creature.getVelocity().add(velChange);
			    }

			    // And queue the emitted thing for addition to the world
			    WorldCreature wc = new WorldCreature(child);
			    wc.getVelocity().set(mate.velocity);
			    wc.getPosition().set(creature.getPosition());
			    myNewCreatures.add(wc);
			}
		    }
		}
	    }
	    else if (action instanceof ActionPuff) {
		// What was emitted
		ActionPuff puff = (ActionPuff)action;

		// Puffing becomes less effective the higher we go (less air :)
		final double density = getAirDensity(creature.getPosition().z);

		// How fast do we want to go?
		double speed = Math.min(1.0 , puff.velocity.length());

		// How fast do we allow? This means bigger creatures find it
		// harder to change direction.
		final double mass = creature.getCreature().getPublicBody().getMassKg();
		final double massFactor = Math.min(1.0, Math.max(0.0, MAX_MASS - mass) / MAX_MASS);
		speed = Math.min(1.0, speed * massFactor) * density;

		// Anything to do?
		if (!Double.isNaN(speed) && speed > 0.0) {
		    // This essentially becomes a force in the opposite
		    // direction which we handle by preserving momentum
		    Vector3d velChange = new Vector3d(puff.velocity);
		    velChange.normalize();
		    velChange.negate();

		    // Let the change be some factor of the max speed
		    velChange.scale(MAX_SPEED / 100.0 * speed);

		    // Now apply it
		    creature.getVelocity().add(velChange);

		    // We remove the energy required to do this from the
		    // creature here; this formula is a little random but
		    // tries to vaguely penalise people for being fat bastards
		    final double energy = speed * creature.getRadius() * creature.getRadius();
		    creature.getCreature().getPublicBody().removeEnergy(energy);
		}
	    }
	    else if (action instanceof ActionSelfDestruct) {
		// How we explode
		ActionSelfDestruct nuke = (ActionSelfDestruct)action;

		// What is the minimum amount of energy this will take? We
		// decide that anything which will be hit for less than this
		// amount is outside the radius.
		final double minEnergy = 1.0; // XXX NEVER ZERO!

		// How much energy this will release; we convert all our mass
		// into energy
		final double energy =
		    creature.getCreature().getPublicBody().getMassKg() / MASS_PER_ENERGY;

		// Figure out the blast radius. Since we decide that the
		// amount we take away, e, defined by
		//   e = energy / (dist^2)
		// then it will be sqrt(energy/min).
		final double radius = Math.sqrt(Math.max(0.0, energy) / minEnergy);

		// Now we can send the nuke to these guys
		for (VisibleCreature dest : tree.get(creature.getPosition(), radius)) {
		    // Find it and make sure it's alive and not me
		    WorldCreature victim = myCreatures.get(dest.getId());
		    if (victim != null && victim.getCreature().getPublicBody().isAlive() &&
			victim.getCreature().getId() != creature.getCreature().getId())
		    {
			// Figure out the distance away
			Vector3d dir = new Vector3d(victim.getPosition());
			dir.sub(creature.getPosition());

			// How much energy it loses
			final double amount = energy / Math.max(1.0, dir.lengthSquared());

			// Now we can remove this amount of energy
			victim.getCreature().getPublicBody().removeEnergy(amount);

			// Let this creature know that it was attacked
			sendAttackStimulus(victim, creature,
					   energy / victim.getCreature().getPublicBody().getEnergy());

			// Impart kinetic energy too (a little random I guess!); e=mv^2
			if (dir.lengthSquared() > 0.0) {
			    dir.normalize();
			    final double speed =
				Math.sqrt(amount / creature.getCreature().getPublicBody().getMassKg());
			    dir.scale(speed);

			    // Now apply it
			    creature.getVelocity().add(dir);
			}
		    }
		}

		// Finally we effectively remove this creature from the
		// universe by zeroing out everything
		creature.getCreature().getPublicBody().removeEnergy(Double.MAX_VALUE);
		creature.getCreature().getPublicBody().feedFrom(Double.MAX_VALUE);
	    }
	    else if (action instanceof ActionShout) {
 		// What was emitted
 		ActionShout shout = (ActionShout)action;

 		// Figure out who will hear this
 		for (VisibleCreature dest : tree.get(creature.getPosition(), shout.radius)) {
 		    sendShoutStimulus(creature, dest.getId(), shout.number);
 		}
	    }
	    else if (action instanceof ActionSpawn) {
		// Who's around this guy
		Collection<VisibleCreature> neighbours =
		    tree.get(creature.getPosition(), creature.getViewRadius());
		if (canSpawn(creature, neighbours)) {
		    // How we want to spawn
		    ActionSpawn spawn = (ActionSpawn)action;

		    // Attempt to create it
		    Creature child =
			creature.getCreature().spawn(Math.min(MAX_MASS, spawn.baseMass));
		    child.getPublicBody().addEnergy(spawn.energy);

		    // Conservation of momentum
		    final double momentum =
			child.getPublicBody().getMassKg() * spawn.velocity.length();
		    if (!Double.isNaN(momentum) && momentum > 0.0) {
			// This essentially becomes a force in the opposite
			// direction which we handle by preserving momentum
			Vector3d velChange = new Vector3d(spawn.velocity);
			velChange.normalize();
			velChange.negate();

			// The speed imparted to the creature depends on its mass
			final double speed =
			    momentum / creature.getCreature().getPublicBody().getMassKg();
			velChange.scale(speed);

			// Now apply it
			creature.getVelocity().add(velChange);
		    }

		    // And queue the emitted thing for addition to the world
		    WorldCreature wc = new WorldCreature(child);
		    wc.getVelocity().set(spawn.velocity);
		    wc.getPosition().set(creature.getPosition());
		    myNewCreatures.add(wc);
		}
	    }
	    else {
		// Huh?
		throw new RuntimeException("Unknown action: " + action);
	    }
	}
    }

    /**
     * Display the creatures.
     */
    private void display()
    {
	// Update the display for all creatures
	for (Map.Entry<Integer,WorldCreature> entry : myCreatures.entrySet()) {
	    entry.getValue().updateDisplay();
	}
    }

    /**
     * Get pending stimuli for a creature
     */
    private Map<Class,Stimulus> getPendingStimuli(int id)
    {
	Map<Class,Stimulus> stimuli = myPendingStimuli.get(id);
	if (stimuli == null) {
	    stimuli = new HashMap<Class,Stimulus>();
	    myPendingStimuli.put(id, stimuli);
	}
	return stimuli;
    }

    /**
     * Send an attack stimulus to a creature.
     */
    private void sendAttackStimulus(WorldCreature attacked,
				    WorldCreature attacker, double energy)
    {
	// Get the set of stimuli
	Map<Class,Stimulus> stimuli = getPendingStimuli(attacked.getCreature().getId());

	// Get the attack stimulus
	StimulusAttacked stimulus = (StimulusAttacked)stimuli.get(StimulusAttacked.class);
	if (stimulus == null) {
	    stimulus = new StimulusAttacked();
	    stimuli.put(StimulusAttacked.class, stimulus);
	}

	// Now add it to the list
	stimulus.details.add(
            new StimulusAttacked.Details(getVisibleCreature(attacker), energy)
	);
    }

    /**
     * Send a shout stimulus to a creature.
     */
    private void sendShoutStimulus(WorldCreature src, int destId, int number)
    {
	// Get the set of stimuli
	Map<Class,Stimulus> stimuli = getPendingStimuli(destId);

	// Get the stimulus
	StimulusShouts stimulus = (StimulusShouts)stimuli.get(StimulusShouts.class);
	if (stimulus == null) {
	    stimulus = new StimulusShouts();
	    stimuli.put(StimulusShouts.class, stimulus);
	}

	// Now add it to the list
	stimulus.details.add(
            new StimulusShouts.Details(getVisibleCreature(src), number)
	);
    }

    /**
     * Can a creature spawn?
     */
    private boolean canSpawn(WorldCreature creature,
			     Collection<VisibleCreature> neighbours)
    {
	return (myCreatures.size() < myMaxPopulation * 5 &&
		creature.canSpawn(myWorldMillis, neighbours));
    }

    /**
     * Get the air density at a given altitude.
     */
    private double getAirDensity(double alt)
    {
	// Gets thinner above a certain height
	final double bb = BOUNDING_BOX_SIZE_M;
	return Math.min(1.0, Math.max(bb * 0.5, bb - alt) / bb) * 10;
    }

    /**
     * Put food into the system.
     */
    private void addFood(int count, double mass)
    {
	for (int i=0; i < count; i++) {
	    Creature c = new CreatureDead(mass, myFoodFamily);
	    addCreature(c);
	}
	myLastFeedMillis = myWorldMillis;
    }

    /**
     * Populate the world with creatures.
     */
    private void addCreatures(int num)
    {
	// Add some creatures
	for (int i=0; i < num;) {
	    int family = Creature.nextFamily();
            for (int j=0; j < 3 && i < num; i++, j++) {
                Creature c = createCreature(Math.random() * MAX_MASS * 0.1 + 1.0, family);
                c.getPublicBody().addEnergy(c.getPublicBody().maxEnergy());
                c.forceMutation(0.5);
                addCreature(c);
            }
	}
    }

    /**
     * Break any monopololies.
     */
    private void breakMonopolies()
    {
	// Split the family up?
	int total = 0;
	int max   = 0;
	int which = -1;
	Map<Integer,Integer> histo = new HashMap<Integer,Integer>();
	for (WorldCreature c : myCreatures.values()) {
	    if (c.getCreature().getFamily() == myFoodFamily) {
		continue;
	    }
	    total++;

	    int family = c.getCreature().getFamily();
	    Integer count = histo.get(family);
	    if (count == null) count = 0;
	    count++;
	    histo.put(family, count);
	    if (count > max) {
		max = count;
		which = family;
	    }
	}

	// Break up the dominating family?
	if (total > 0 && which > 0 &&
	    total > (int)(myMaxPopulation * 0.1) &&
	    (double)max/(double)total > 0.9)
	{
	    final int numNew = 3;
	    int families[] = new int[numNew];
	    for (int i=0; i < numNew; i++) {
		families[i] = Creature.nextFamily();
	    }
	    int i=0;
	    for (WorldCreature c : myCreatures.values()) {
		if (c.getCreature().getFamily() == which) {
		    c.setFamily(families[i++ % numNew]);
		}
	    }

	    // Add food to kick start them
	    addFood((myMaxPopulation - myCreatures.size()) / 2, 100);
	}
    }

    /**
     * Throw creatures and food into the system.
     */
    private void populateWorld()
    {
	// Need to add food to the world when the population gets too low
	int count = 0;
	for (WorldCreature creature : myCreatures.values()) {
	    if (creature.getCreature().getFamily() != myFoodFamily) {
		count++;
	    }
	}

	// How much room for critters
	final int space = (myMaxPopulation - count);
	if (count < (int)(myMaxPopulation * RESPAWN_FRACTION) &&
	    myWorldMillis - myLastRespawnMillis > myRespawnIntervalMillis)
	{
	    // Repopulate with other creatures
	    addCreatures(space / 10);
	    myLastRespawnMillis = myWorldMillis;
	}

	// Feed every now and again too
	if (myWorldMillis - myLastFeedMillis > myFeedIntervalMillis) {
	    // Add a load of food!
	    addFood(space / 10, 100.0);
	}
    }

    /**
     * Print stats for the world.
     */
    private void printStats()
    {
	// Now dump
	System.out.println("Now: " +
			   "Iteration " + myStep + " " +
			   "at " + new Date(myWorldMillis) + " (world time)");

	// ------------------------------------------------------------

	// Count up the populations
	Map<Integer,Integer> pop  = new TreeMap<Integer,Integer>();
	for (WorldCreature c : myCreatures.values()) {
            int family = c.getCreature().getFamily();
            if (family == myFoodFamily) {
                continue;
            }
	    Integer count = pop.get(family);
	    if (count == null) {
		count = 0;
	    }
	    count++;
	    pop.put(family, count);
	}

	// Now dump
	System.out.println("Population: " + myCreatures.size());
	for (Integer family : pop.keySet()) {
	    // For each family find the strongest guy and dump his info
	    double health = -1.0;
	    WorldCreature wc = null;
	    for (WorldCreature c : myCreatures.values()) {
		if (c.getCreature().getFamily() == family) {
		    double h = c.getCreature().getPublicBody().getHealth();
		    if (h > health) {
			wc = c;
		    }
		}
	    }

	    System.out.println("  " +
			       "Family " + family + " " +
			       "count " + pop.get(family) + ": " +
			       ((wc == null) ? "???" : wc.getCreature().allToString()));
	}
    }

    // ======================================================================

    /**
     * Start the world doing stuff.
     */
    public static void main(String argv[])
    {
	// World.LOG.setLevel(Level.FINEST);

	Handler logHandler = new ConsoleHandler();
	logHandler.setFormatter(
	    new Formatter() {
		public String format(LogRecord r) {
		    return
			r.getLevel() + " " +
			r.getSourceClassName() + "#" + r.getSourceMethodName() + ": " +
			r.getMessage();
		}
	    }
	);

	for (Handler h : World.LOG.getHandlers()) {
	    World.LOG.removeHandler(h);
	}
	//World.LOG.addHandler(logHandler);

	// Let's go!
	World world = new World();
        world.init();
	world.run();
    }
}
