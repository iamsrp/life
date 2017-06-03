import javax.media.j3d.Appearance;

/**
 * Creature which likes to feed.
 */
public class CreatureFeeder
    extends Creature
{
    /**
     * Constructor.
     */
    public CreatureFeeder(Double baseMass, Integer family)
    {
	super(baseMass, family);
	getBrain().addGene(new GeneFeed());
	getBrain().addGene(new GeneMutation());
	getBrain().addGene(new GeneSpawn());
    }
}
