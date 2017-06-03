import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

/**
 * A tree which divides up objects in a 3-dimensional space for quick lookup
 * like a hash table.
 */
public class KDTree<T extends KDTree.Element>
{
    // =======================================================================

    /**
     * An object in the KDTree.
     */
    public interface Element
    {
	/**
	 * The get the position of this element.
	 */
	public Vector3d getPosition();

	/**
	 * Equality operator, as defined in Object.
	 */
	public boolean equals(Object o);
    }

    // =======================================================================

    /**
     * The bottom left part of the box. This defines inclusive containment.
     */
    private final Point3d myLowerLeft;

    /**
     * The upper right of the box. This defines exclusive containment.
     */
    private final Point3d myUpperRight;

    /**
     * The children we have (these are guaranteed to all be contained
     * within this bounding box). This will be <code>null</code> if we are
     * a leaf.
     */
    private List<KDTree<T>> myChildren = null;

    /**
     * The object contained within this node. This will be <code>null</code>
     * if we are not a leaf. This will be greater than one iff all the
     * elements have the same position.
     */
    private List<T> myElements = null;

    // =======================================================================

    /**
     * Default constructor.
     */
    public KDTree()
    {
	myLowerLeft = new Point3d(Double.NEGATIVE_INFINITY,
				   Double.NEGATIVE_INFINITY,
				   Double.NEGATIVE_INFINITY);
	myUpperRight = new Point3d(Double.POSITIVE_INFINITY,
				   Double.POSITIVE_INFINITY,
				   Double.POSITIVE_INFINITY);
    }

    /**
     * Constructor for making sub-nodes; only called by an existing instance
     * of the class.
     */
    private KDTree(Point3d lowerLeft, Point3d upperRight)
    {
	myLowerLeft  = lowerLeft;
	myUpperRight = upperRight;
    }

    // =======================================================================

    /**
     * Add an element to the tree.
     */
    public void add(T element)
    {
	// Ignore elements with NaNs in
	if (Double.isNaN(element.getPosition().x) ||
	    Double.isNaN(element.getPosition().y) ||
	    Double.isNaN(element.getPosition().z))
	{
	    return;
	}

	// We need to figure out if we contain an element already. If so then
	// we probably need to break up the tree
	if (myChildren != null) {
	    // Recurse into children
	    for (int i=0, length = myChildren.size(); i < length; i++) {
		KDTree<T> child = myChildren.get(i);
		if (child.contains(element.getPosition())) {
		    child.add(element);
		    break;
		}
	    }
	}
	else if (myElements == null || myElements.isEmpty()) {
	    // Very easy
	    if (myElements == null) {
		myElements = new ArrayList<T>();
	    }
	    myElements.add(element);
	}
	else {
	    // We already have something inside us; do we need to break up the tree?
	    boolean needBreak = false;
	    for (int i=0, length = myElements.size(); i < length; i++) {
		final T e = myElements.get(i);
		if (!tupleEquals(e.getPosition(), element.getPosition())) {
		    needBreak = true;
		    break;
		}
	    }

	    // Okay, add this element to the list or break up the tree if needbe
	    if (needBreak) {
		breakTree(element);
	    }
	    else {
		myElements.add(element);
	    }
	}
    }

    /**
     * Add all the elements from a collection.
     */
    public void addAll(Collection<T> c)
    {
	// Pretty simple
	for (T e : c) {
	    add(e);
	}
    }

    /**
     * Find any alements contained by a bounding sphere.
     */
    public List<T> get(Tuple3d center, double radius)
    {
	// Hand off to and pass down where we put the result
	return get(center, radius, new ArrayList<T>());
    }

    /**
     * Remove an element. This removes all elements with the same position
     * for which equals() returns true.
     *
     * @return the element(s) removed.
     */
    public List<T> remove(T element)
    {
	// Hand off with a place for the result
	return remove(element, new ArrayList<T>());
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public String toString()
    {
	return 
	    "<" +
	    ((myChildren != null) ? "Node" : "Leaf") + 
	    ";LowerLeft:" + myLowerLeft + 
	    ";upperRight" + myUpperRight + 
	    ">";
    }

    // =======================================================================

    /**
     * Find any alements contained by a bounding sphere.
     */
    protected List<T> get(Tuple3d center, double radius, List<T> result)
    {
	// Are we a leaf?
	if (myElements != null) {
	    assert(myChildren == null);

	    // Look at all the elements we contain
	    for (int i=0, length = myElements.size(); i < length; i++) {
		final T element = myElements.get(i);
		// If the thing contains it then add it to the result
		if (contains(center, radius, element.getPosition())) {
		    result.add(element);
		}
	    }
	}
	else if (myChildren != null) {
	    // Hand off to children which the sphere intersects with
	    assert(myElements == null);
	    for (int i=0, length = myChildren.size(); i < length; i++) {
		KDTree<T> child = myChildren.get(i);
		if (intersects(center, radius)) {
		    child.get(center, radius, result);
		}
	    }
	}
	return result;
    }

    /**
     * Remove an element. This removes all elements with the same position
     * for which equals() returns true.
     *
     * @return the element(s) removed.
     */
    protected List<T> remove(T element, List<T> result)
    {
	// Are we a leaf?
	if (myElements != null) {
	    assert(myChildren == null);

	    // Look at all the elements we contain
	    Iterator<T> itr = myElements.iterator();
	    while (itr.hasNext()) {
		// The element we are looking at
		T e = itr.next();
		
		// If the thing contains it then add it to the result
		if (tupleEquals(element.getPosition(), e.getPosition()) &&
		    element.equals(e)) 
		{
		    // Save it in the result and remove it from the list of
		    // children
		    result.add(e);
		    itr.remove();
		}
	    }
	}
	else if (myChildren != null) {
	    assert(myElements == null);

	    // Hand off to children
	    for (int i=0, length = myChildren.size(); i < length; i++) {
		KDTree<T> child = myChildren.get(i);
		if (child.contains(element.getPosition())) {
		    child.remove(element, result);
		}
	    }
	}
	return result;
    }

    /**
     * Does this tree contain a point?
     */
    protected boolean contains(Tuple3d point)
    {
	final boolean result =
	    (myLowerLeft.x <= point.x && point.x < myUpperRight.x &&
	     myLowerLeft.y <= point.y && point.y < myUpperRight.y &&
	     myLowerLeft.z <= point.z && point.z < myUpperRight.z);
	return result;		
    }

    /**
     * Does a bounding sphere contain a point?
     */
    protected boolean contains(Tuple3d center, double radius, Tuple3d point)
    {
	return (radius * radius > diffSquared(center, point));
    }

    /**
     * Does the bounding box intersect with a bounding sphere?
     */
    protected boolean intersects(Tuple3d center, double radius)
    {
	// Figure out the closest point in the box to the sphere and see if
	// it's within the sphere
	return diffSquared(center, bound(center)) < (radius*radius);
    }

    /**
     * Give back a Tuple3d which is the given Tuple3d bounded by our
     * bounding-box. Slightly sketchy in that the upper right is exclusive so
     * the point will not be, strictly, bounded by the box.
     */
    protected Tuple3d bound(Tuple3d point)
    {
	Tuple3d bounded = (Tuple3d)point.clone();
	bounded.x = Math.max(Math.min(bounded.x, myUpperRight.x), myLowerLeft.x);
	bounded.y = Math.max(Math.min(bounded.y, myUpperRight.y), myLowerLeft.y);
	bounded.z = Math.max(Math.min(bounded.z, myUpperRight.z), myLowerLeft.z);
	return bounded;
    }

    // =======================================================================

    /**
     * Break up the tree to have two children and place all the elements in
     * them.
     */
    private void breakTree(T newElement)
    {
	// Get the other element (it should always exist); there may be more
	T cur = myElements.get(0);

	// We break the box so that the distance between the two elements is
	// maximised; they should not be on the same point!
	Tuple3d posNew = newElement.getPosition();
	Tuple3d posCur = cur.getPosition();
	assert(!tupleEquals(posNew, posCur));
	final double 
	    distX = Math.abs(posCur.x - posNew.x),
	    distY = Math.abs(posCur.y - posNew.y),
	    distZ = Math.abs(posCur.z - posNew.z);

	// Create the children
	final Point3d 
	    child1LowerLeft  = new Point3d(myLowerLeft), 
	    child1UpperRight = new Point3d(myUpperRight), 
	    child2LowerLeft  = new Point3d(myLowerLeft), 
	    child2UpperRight = new Point3d(myUpperRight);
	if (distX > distY) {
	    if (distX > distZ) {
		// X biggest
		final double mid = (posNew.x + posCur.x) / 2.0;
		child1UpperRight.x = mid;
		child2LowerLeft.x  = mid;
	    }
	    else {
		// Z biggest
		final double mid = (posNew.z + posCur.z) / 2.0;
		child1UpperRight.z = mid;
		child2LowerLeft.z  = mid;
	    }
	}
	else {
	    if (distY > distZ) {
		// Y biggest
		final double mid = (posNew.y + posCur.y) / 2.0;
		child1UpperRight.y = mid;
		child2LowerLeft.y  = mid;
	    }
	    else {
		// Z biggest
		final double mid = (posNew.z + posCur.z) / 2.0;
		child1UpperRight.z = mid;
		child2LowerLeft.z  = mid;
	    }
	}

	// And create
	final KDTree<T> 
	    child1 = new KDTree<T>(child1LowerLeft, child1UpperRight),
	    child2 = new KDTree<T>(child2LowerLeft, child2UpperRight);

	// Now we add all the elements to the children; we pop the new element
	// in the list just to make the code simpler
	myElements.add(newElement);
	for (int i=0, length = myElements.size(); i < length; i++) {
	    final T element = myElements.get(i);
	    if (child1.contains(element.getPosition())) {
		child1.add(element);
	    }
	    else if (child2.contains(element.getPosition())) {
		child2.add(element);
	    }
	    else {
		throw new RuntimeException("Tree " + 
					   toString() + "=>" +
					   child1 + "," + child2 + " " +
					   "had holes in when placing " +
					   element);
	    }
	}

	// We now have children
	assert(myChildren == null);
	myChildren = new ArrayList<KDTree<T>>();
	myChildren.add(child1);
	myChildren.add(child2);

	// We no longer look after these elements
	myElements = null;
    }

    /**
     * Are two objects equal in distance?
     */
    private boolean tupleEquals(Tuple3d t1, Tuple3d t2)
    {
	return (diffSquared(t1, t2) < 1e-20);
    }

    /**
     * The distance between two tuples (squared)
     */
    private double diffSquared(Tuple3d t1, Tuple3d t2)
    {
	final double x = t1.x - t2.x;
	final double y = t1.y - t2.y;
	final double z = t1.z - t2.z;
	return (x*x + y*y + z*z);
    }

    // =======================================================================
    
    /**
     * Hacky class for testing.
     */
    private static final class TestClass
	extends Vector3d
	implements Element
    {
	public TestClass(double x, double y, double z)
	{
	    super(x, y, z);
	}
	
	public Vector3d getPosition()
	{
	    return this;
	}
    }

    /**
     * Test method
     */
    public static void main(String[] args)
    {
	final int dim = 10;

	KDTree<TestClass> tree = new KDTree<TestClass>();
	for (int x=0; x <= dim; x++) {
	    for (int y=0; y <= dim; y++) {
		for (int z=0; z <= dim; z++) {
		    TestClass p = new TestClass(x, y, z);
		    tree.add(p);
		}
	    }
	}

	for (int x=0; x <= dim; x++) {
	    for (int y=0; y <= dim; y++) {
		for (int z=0; z <= dim; z++) {
		    TestClass p = new TestClass(x, y, z);
		    double r = 1.5;
		}
	    }
	}

	for (int i=0; i < 100; i++) {
	    TestClass p = new TestClass(Math.random() * dim,
					Math.random() * dim,
					Math.random() * dim);
	    double r = 1.5;
	    System.out.println("Getting " + p + " x " + r + " = " + tree.get(p, r)); 
	}
    }
}
