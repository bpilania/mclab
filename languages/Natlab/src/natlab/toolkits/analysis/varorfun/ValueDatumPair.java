package natlab.toolkits.analysis.varorfun;

/**
 * A simple pair class used by the VFFlowset. All comparison or
 * hashing is done on the value or first member of the pair. 
 */

public class ValueDatumPair< V, D extends VFDatum>
{
    protected V value;
    protected D datum;

    public ValueDatumPair( V v, D d )
    {
        value = v;
        datum = d;
    }

    public V getValue()
    {
        return value;
    }
    public D getDatum()
    {
        return datum;
    }
    public boolean equals( Object o )
    {
        try{
            return value.equals( ((ValueDatumPair)o).getValue() );
        }catch( Exception e){
            return false;
        }
    }
    public int hashCode()
    {
        return value.hashCode();
    }

    public D setDatum( D d )
    {
        return datum = d;
    }
}
