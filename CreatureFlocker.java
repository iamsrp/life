import javax.media.j3d.Appearance;

/**
 * A flocking creature in the world.
 */
public class CreatureFlocker
    extends Creature
{
    /**
     * Constructor.
     */
    public CreatureFlocker(Double baseMass, Integer family)
    {
	super(baseMass, family);
	getBrain().addGene(new GeneFlocking());
	getBrain().addGene(new GeneMinSpeed());
	getBrain().addGene(new GeneRandomPuffAction());
    }
}
