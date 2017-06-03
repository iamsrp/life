import javax.media.j3d.Appearance;

/**
 * A creature which does everything!
 */
public class CreatureKitchenSink
    extends Creature
{
    /**
     * Constructor.
     */
    public CreatureKitchenSink(Double baseMass, Integer family)
    {
	super(baseMass, family);
	getBrain().addGene(new GeneAttack());
	//getBrain().addGene(new GeneAttackChaff());
	getBrain().addGene(new GeneAttackResponse());
	getBrain().addGene(new GeneChaseAttack());
	getBrain().addGene(new GeneFamilyFollow());
	getBrain().addGene(new GeneFeed());
	getBrain().addGene(new GeneFindMate());
	getBrain().addGene(new GeneFlocking());
	getBrain().addGene(new GeneHoming());
	getBrain().addGene(new GeneMate());
	getBrain().addGene(new GeneMutation());
	getBrain().addGene(new GeneMinSpeed());
	getBrain().addGene(new GeneNotFamilyFollow());
	getBrain().addGene(new GeneRandomPuffAction());
	getBrain().addGene(new GeneSelfDestruct());
	getBrain().addGene(new GeneShout());
	getBrain().addGene(new GeneShoutResponse());
	getBrain().addGene(new GeneSizeFollow());
	getBrain().addGene(new GeneSpawn());
    }
}
