import java.io.Serializable;

/**
 * A gene which controls behaviour.
 */
public interface Gene
    extends Cloneable, Serializable
{
    /**
     * Mutate this gene by a certain amount. The amount should be between 0.0
     * (not at all) and 1.0 ("maximum" mutation).
     */
    public void mutate(final double factor);

    /**
     * Clone a new instance of myself.
     */
    public Gene clone();
}
