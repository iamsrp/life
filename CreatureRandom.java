import javax.media.j3d.Appearance;

/**
 * A random walking creature in the world.
 */
public class CreatureRandom
    extends Creature
{
    /**
     * Constructor.
     */
    public CreatureRandom(Double baseMass, Integer family)
    {
	super(baseMass, family);
	getBrain().addGene(new GeneMutation());
	getBrain().addGene(new GeneMinSpeed());
	getBrain().addGene(new GeneRandomPuffAction());
	getBrain().addGene(new GeneSpawn());
    }
}
