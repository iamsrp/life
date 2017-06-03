import javax.media.j3d.Appearance;

/**
 * A dead creature which doesn't really do a lot aside from sit around waiting
 * to be eaten.
 */
public class CreatureDead
    extends Creature
{
    /**
     * Constructor.
     */
    public CreatureDead(Double baseMass, Integer family)
    {
	super(baseMass, family);
	getBrain().addGene(new GeneRandomPuffAction());
    }
}
