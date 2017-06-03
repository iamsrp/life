import javax.media.j3d.Appearance;

/**
 * A cerature which does stuff.
 */
public class CreatureMisc
    extends Creature
{
    /**
     * Constructor.
     */
    public CreatureMisc(Double baseMass, Integer family)
    {
	super(baseMass, family);
	getBrain().addGene(new GeneAttack());
	//getBrain().addGene(new GeneAttackChaff());
	//getBrain().addGene(new GeneAttackResponse());
	//getBrain().addGene(new GeneFamilyFollow());
	getBrain().addGene(new GeneFeed());
	//getBrain().addGene(new GeneFindMate());
	getBrain().addGene(new GeneFlocking());
	//getBrain().addGene(new GeneMate());
	getBrain().addGene(new GeneRandomPuffAction());
	getBrain().addGene(new GeneSelfDestruct());
	getBrain().addGene(new GeneSpawn());
    }
}
