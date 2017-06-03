import java.util.Collection;

/**
 * The set of things around us.
 */
public class StimulusNeighbours
    extends Stimulus
{
    /**
     * The set of people around us.
     */
    public final Collection<World.VisibleCreature> neighbours;
    
    /**
     * Constructor.
     */
    public StimulusNeighbours(Collection<World.VisibleCreature> neighbours)
    {
	this.neighbours = neighbours;
    }
}
