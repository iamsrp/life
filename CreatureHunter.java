import javax.media.j3d.Appearance;

/**
 * Creature which likes to attack and then feed.
 */
public class CreatureHunter
    extends Creature
{
    /**
     * Constructor.
     */
    public CreatureHunter(Double baseMass, Integer family)
    {
	super(baseMass, family);
	getBrain().addGene(new GeneAttack());
	getBrain().addGene(new GeneFeed());
	getBrain().addGene(new GeneMutation());
	getBrain().addGene(new GeneRandomPuffAction());
	getBrain().addGene(new GeneSpawn());
    }
}
