import javax.media.j3d.Appearance;

/**
 * A creature which mainly hunts in a pack.
 */
public class CreatureWolf
    extends Creature
{
    /**
     * Constructor.
     */
    public CreatureWolf(Double baseMass, Integer family)
    {
	super(baseMass, family);
	getBrain().addGene(new GeneAttack());
	getBrain().addGene(new GeneFamilyFollow());
	getBrain().addGene(new GeneFeed());
	getBrain().addGene(new GeneFlocking());
	getBrain().addGene(new GeneMutation());
	getBrain().addGene(new GeneRandomPuffAction());
	getBrain().addGene(new GeneShout());
	getBrain().addGene(new GeneShoutResponse());
	getBrain().addGene(new GeneSpawn());
    }
}
