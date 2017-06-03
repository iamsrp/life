import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3d;

/**
 * A gene which keeps a creature moving.
 */
public class GeneMinSpeed
    implements GeneAction
{
    /**
     * Our minimum speed.
     */
    private double myMinSpeed;

    /**
     * Our puff speed.
     */
    private double myPuffSpeed;

    /**
     * Constructor.
     */
    public GeneMinSpeed()
    {
	myMinSpeed  = 1.0;
	myPuffSpeed = 0.05;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	myMinSpeed  = Math.max(0.0, myMinSpeed  + (Math.random() - 0.5) * factor);
	myPuffSpeed = Math.max(0.0, myPuffSpeed + (Math.random() - 0.5) * factor);
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
	
	// Anything going to happen?
	if (myMinSpeed > 0.0) {
            Vector3d vel = self.self.getVelocity();
            if (vel.lengthSquared() < myMinSpeed * myMinSpeed) {
                final Vector3d puff;
                if (vel.lengthSquared() > 0) {
                    // Got faster in this direction by puffin in the opposite one
                    puff = new Vector3d(vel);
                    puff.negate();
                }
                else {
                    // Do something random to get us going
                    puff = new Vector3d();
                    while (puff.lengthSquared() == 0.0) {
                        puff.x = Math.random() - 0.5;
                        puff.y = Math.random() - 0.5;
                        puff.z = Math.random() - 0.5;
                    }
                }

		// Normalise now, save it and reset counters
		puff.normalize();
		puff.scale(myPuffSpeed);

                // Add it!
                result.add(new ActionPuff(puff));
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
	    "<Class="  + this.getClass().getName() + ";" +
	    "Min="     + myMinSpeed                + ";" +
	    "Puff="    + myPuffSpeed               + ">";
    }
}
