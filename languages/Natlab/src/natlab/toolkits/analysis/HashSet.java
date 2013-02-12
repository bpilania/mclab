package natlab.toolkits.analysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Implementation of the FlowSet based on HashSets.
 *
 * @see java.util.HashSet
 */
public class HashSet<D> extends AbstractFlowSet<D>
{
    protected HashSet<D> set;

    /**
     * Creates a new HashSet.
     */
    public HashSet(){
        set = Sets.newHashSet();
    }

    /**
     * Takes in a HashSet to use.
     */
    public HashSet( HashSet<D> set){
        this.set = set;
    }
    
    /**
     * Copies the underlying HashSet
     */
    public HashSet<D> copy()
    {
        return new HashSet<D>(Sets.newHashSet(set));
    }
    
    public HashSet<D> emptySet()
    {
        return new HashSet<D>(Sets.<D>newHashSet());
    }

    /**
     * Clears the underlying HashSet
     */
    public void clear()
    {
        set.clear();
    }

    public boolean isEmpty()
    {
        return set.isEmpty();
    }
    public int size()
    {
        return set.size();
    }
    public void addAll(HashSet<? extends D> fs)
    {
        set.addAll( fs.set );
    }
    public void addAll( Collection<? extends D> c )
    {
        set.addAll( c );
    }
    public void add(D obj)
    {
        set.add(obj);
    }
    public boolean remove( Object obj )
    {
        return set.remove( obj );
    }
    public boolean contains( Object obj )
    {
        return set.contains( obj );
    }
    public List<D> toList()
    {
        return Lists.newArrayList(set);
    }

    /**
     * Creates a set containing the contents of this flow-set.
     *
     * @return a new Set with the contents of this flow-set
     */
    public Set<D> getSet()
    {
        return Sets.newHashSet(set);
    }
    
    @Override
    public Iterator<D> iterator()
    {
        return set.iterator();
    }

    @Override
    public boolean equals(Object o){
        if( o instanceof HashSet )
            return set.equals( ((HashSet<?>)o).set );
        else
            return super.equals(o);
    }
    
    @Override
    public int hashCode() {
        return set.hashCode();
    }
}