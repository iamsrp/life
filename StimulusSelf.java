/**
 * Ourselves, as the world sees us as well as other info.
 */
public class StimulusSelf
    extends Stimulus
{
    /**
     * A pointer to me.
     */
    public final World.VisibleCreature self;
    
    /**
     * Are we allowed to spawn?
     */
    public final boolean canSpawn;
    
    /**
     * Constructor.
     */
    public StimulusSelf(World.VisibleCreature self, boolean canSpawn)
    {
	this.self     = self;
	this.canSpawn = canSpawn;
    }
}
