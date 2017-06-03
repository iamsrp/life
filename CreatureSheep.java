import javax.media.j3d.Appearance;

/**
 * A flocking creature in the world which feeds and stuff.
 */
public class CreatureSheep
    extends Creature
{
    /**
     * Constructor.
     */
    public CreatureSheep(Double baseMass, Integer family)
    {
	super(baseMass, family);
	getBrain().addGene(new GeneAttackResponse());
	getBrain().addGene(new GeneFlocking());
	getBrain().addGene(new GeneFamilyFollow());
	getBrain().addGene(new GeneFeed());
	getBrain().addGene(new GeneMutation());
	getBrain().addGene(new GeneRandomPuffAction());
	getBrain().addGene(new GeneShout());
	getBrain().addGene(new GeneShoutResponse());
	getBrain().addGene(new GeneSpawn());
    }
}
