import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.vecmath.Vector3d;

/**
 * The brain of the creature.
 */
public class Brain 
    implements Serializable
{
    /**
     * The body as parts of the brain can access it. A wrapper class so
     * holders can't cast.
     */
    protected class BrainBody
    {
	/**
	 * The body we contain
	 */
	private final Body myBody;

	/**
	 * Constructor.
	 */
	public BrainBody(Body body)
	{
	    myBody = body;
	}

	/**
	 * Get the base mass of this body in kg.
	 */
	public double getBaseMassKg()
	{
	    return myBody.getBaseMassKg();
	}

	/**
	 * Get the mass of this body in kg.
	 */
	public double getMassKg()
	{
	    return myBody.getMassKg();
	}

	/**
	 * How much energy we have.
	 */
	public final double getEnergy()
	{
	    return myBody.getEnergy();
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
    }

    /**
     * The cost of an action in energy.
     */
    protected static final double ourActionEnergyCost = 0.1;

    /**
     * The set of genes which control me.
     */
    private final Set<Gene> myGenes = new HashSet<Gene>();

    /**
     * Constructor.
     */
    public Brain()
    {
	// Nothing
    }

    /**
     * Create actions using the body.
     */
    public final List<Action> createActions(Body body, final Map<Class,Stimulus> stimuli)
    {
	// Create a version of the body which subclasses are allowed to see
	BrainBody bb = new BrainBody(body);

	// The actions we perform
	List<Action> actions = new ArrayList<Action>();

	// Walk through all the action genes and run then
	for (Gene gene : myGenes) {
	    // Use all the action genes
	    if (gene instanceof GeneAction) {
		// Attempt to do allthe actions this gene tells us to
		GeneAction geneAction = (GeneAction) gene;
		for (Action action : geneAction.generateActions(bb, stimuli)) {
		    // If we can do it then we do so
		    if (body.performAction(action)) {
			actions.add(action);
		    }
		}
	    }
	}

	// Give back any actions we performed
	if (World.LOG.isLoggable(Level.FINEST)) {
	    World.LOG.finest("Created actions: " + actions);
	}
	return actions;
    }

    /**
     * Add a new gene to us.
     */
    public void addGene(Gene gene)
    {
	myGenes.add(gene);
    }

    /**
     * Copy genes from a brain instance. This will include mutation.
     */
    public void copyGenes(Brain brain)
    {
	// Don't copy from self
	if (brain == this) {
	    return;
	}

	// Start fresh
	myGenes.clear();

	// Create the genes by copying them over
	for (Gene gene : brain.myGenes) {
	    Gene newGene = gene.clone();
	    myGenes.add(newGene);
	}

	// Look for the mutation genes (take the average of them)
	double mutation = 0.0, count = 0.0;
	for (Gene gene : myGenes) {
	    if (gene instanceof GeneMutation) {
		mutation += ((GeneMutation)gene).getMutationFactor();
		count++;
	    }
	}
	if (count > 0) {
	    mutation /= count;
	}

	// And mutate
	mutate(mutation);
    }

    /**
     * Blend genes from two brain instances. This will include mutation.
     */
    public void blendGenes(Brain brain1, Brain brain2)
    {
	// Don't copy from self
	if (brain1 == this || brain2 == this) {
	    return;
	}

	// Start fresh
	myGenes.clear();

	// Build up a picture of the two sets of genes so we can pair them up
	// and then choose one of each pair
	Map<Class,Gene>
	    set1 = new HashMap<Class,Gene>(),
	    set2 = new HashMap<Class,Gene>();
	Set<Class> all = new HashSet<Class>();
	for (Gene gene : brain1.myGenes) {
	    set1.put(gene.getClass(), gene);
	    all.add(gene.getClass());
	}
	for (Gene gene : brain2.myGenes) {
	    set2.put(gene.getClass(), gene);
	    all.add(gene.getClass());
	}

	// Now blend together
	for (Class gene : all) {
	    // The two we have to choose from
	    Gene gene1 = set1.get(gene);
	    Gene gene2 = set2.get(gene);

	    // Either both null (should never happen1)
	    // Or gene1 or gene2 is null (so we choose the other)
	    // Or gene1 and gene2 non-null (so we choose one at random)
	    if (gene1 == null && gene2 == null) {
		throw new RuntimeException("Error in gene pairing!");
	    }
	    else if (gene1 == null) {
		myGenes.add(gene2.clone());
	    }
	    else if (gene2 == null) {
		myGenes.add(gene1.clone());
	    }
	    else if (Math.random() < 0.5) {
		myGenes.add(gene1.clone());
	    }
	    else {
		myGenes.add(gene2.clone());		
	    }
	}

	// Look for the mutation genes (take the average of them)
	double mutation = 0.0, count = 0.0;
	for (Gene gene : myGenes) {
	    if (gene instanceof GeneMutation) {
		mutation += ((GeneMutation)gene).getMutationFactor();
		count++;
	    }
	}
	if (count > 0) {
	    mutation /= count;
	}

	// And mutate
	mutate(mutation);
    }

    /**
     * Mutate our genes by a factor
     */
    public void mutate(double factor)
    {
	for (Gene gene : myGenes) {
	    gene.mutate(factor);
	}
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
	List<Gene> genes = new ArrayList<Gene>(myGenes);
	Collections.sort(
	    genes, 
	    new Comparator<Gene>() {
		public int compare(Gene o1, Gene o2) {
		    return o1.getClass().toString().compareTo(o2.getClass().toString());
		}
	    }
	);
	return 
	    "<Class=" + this.getClass().getName() + ";" +	    
	    "Genes="  + genes.toString()          + ">";
    }
}
