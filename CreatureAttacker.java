import javax.media.j3d.Appearance;

/**
 * Creature which likes to feed.
 */
public class CreatureAttacker
    extends Creature
{
    /**
     * Constructor.
     */
    public CreatureAttacker(Double baseMass, Integer family)
    {
	super(baseMass, family);
	getBrain().addGene(new GeneAttack());
	getBrain().addGene(new GeneChaseAttack());
	getBrain().addGene(new GeneMutation());
	getBrain().addGene(new GeneSpawn());
    }
}
