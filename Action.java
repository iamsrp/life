/**
 * An action which the creature can perform.
 */
public interface Action
{
    /**
     * The cost of this action in energy.
     */
    public double energyCost();

    /**
     * The cost of this action in mass (kg).
     */
    public double massCost();
}
