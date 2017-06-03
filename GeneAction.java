import java.util.Collection;
import java.util.Map;

/**
 * A gene which creates an action.
 */
public interface GeneAction
    extends Gene
{
    /**
     * Generate a set of actions according some some stimuli.
     */
    public Collection<Action> generateActions(final Brain.BrainBody body,
					      final Map<Class,Stimulus> stimuli);
}
