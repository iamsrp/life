import javax.media.j3d.Appearance;

/**
 * Creature which likes to feed.
 */
public class CreatureAttackee
    extends Creature
{
    /**
     * Constructor.
     */
    public CreatureAttackee(Double baseMass, Integer family)
    {
	super(baseMass, family);
	getBrain().addGene(new GeneAttackResponse());
	getBrain().addGene(new GeneMutation());
	getBrain().addGene(new GeneSpawn());
    }
}
