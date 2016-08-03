package init;

public class IDData<T> implements Comparable<IDData<T>>
{
	
	public String ID;
	public Comparable<T> data;
	public String otherID;
	public String[] line;
	public long addLongData;
	public double[] doubles;
	public double doubleData;
	public long[] longs;
	
	public IDData(String newID, Comparable newData)
	{
		ID=newID;
		data=newData;
	}

	@Override
	public int compareTo(IDData<T> o) 
	{
		int comp=data.compareTo((T)o.data);
		return comp;
	}
	
	@Override
	public int hashCode()
	{
		return ID.hashCode();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof IDData)
		{
			return ((IDData)o).ID.equals(ID);
		}
		else if(o instanceof String)
		{
			return ((String)o).equals(ID);
		}
		else
		{
			return false;
		}
	}

	
}