import java.util.Collections;
import java.util.Set;

/**
 * The set of families in the world. This is mainly so that we can do
 * housekeeping on genes which look at families in various ways. (E.g. purge
 * genes for dead families from Maps.)
 */
public class StimulusFamilies
    extends Stimulus
{
    /**
     * The set of families.
     */
    public final Set<Integer> families;
    
    /**
     * Constructor.
     */
    public StimulusFamilies(Set<Integer> families)
    {
	this.families = Collections.unmodifiableSet(families);
    }
}
