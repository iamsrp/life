import javax.media.j3d.Appearance;

/**
 * A homing creature in the world.
 */
public class CreatureHomer
    extends Creature
{
    /**
     * Constructor.
     */
    public CreatureHomer(Double baseMass, Integer family)
    {
	super(baseMass, family);
	getBrain().addGene(new GeneHoming());
	getBrain().addGene(new GeneMutation());
	getBrain().addGene(new GeneSpawn());
    }
}
