/**
 * A gene which controls how much we mutate.
 */
public class GeneMutation
    implements Gene
{
    /**
     * The current mutation factor.
     */
    private double myMutationFactor;

    /**
     * Constructor.
     */
    public GeneMutation()
    {
	myMutationFactor = 0.01;
    }

    /**
     * Get the mutation factor.
     */
    public double getMutationFactor()
    {
	return myMutationFactor;
    }

    /**
     * {@inheritDoc}
     */
    public void mutate(final double factor)
    {
	myMutationFactor =
	    Math.max(0.001, 
		     Math.min(1.0, 
			      myMutationFactor + (Math.random() - 0.5) * factor));
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
    public String toString()
    {
	return 
	    "<Class="         + this.getClass().getName() + ";" +
	    "MutationFactor=" + myMutationFactor          + ">";
    } 
}
