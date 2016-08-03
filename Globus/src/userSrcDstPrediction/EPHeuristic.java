package userSrcDstPrediction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.List;

public abstract class EPHeuristic 
{
	
	List<String[]> transactions; //sorted by date
	int transactionInd; //need to set this
	long date;
	Hashtable<String, Long> deletions;
	boolean addNetInfo;
	
	public EPHeuristic(List<String[]> transactions, List<String[]> deletions, 
			List<String[]> users, List<String[]> authIDs, long date)
	{
		this.transactions=transactions;
		this.date=date;
		this.deletions=parseUserEpsToDeletions(deletions);
		try 
		{
			if(EPHeuristic.class.getMethod("getAdditionalNetInfo", String.class, boolean.class).getDeclaringClass().equals(EPHeuristic.class))
			{
				addNetInfo=false;
			}
			else
			{
				addNetInfo=true;
			}
		} 
		catch (NoSuchMethodException | SecurityException e) 
		{
			e.printStackTrace();
		}
	}
	
					/*String srcID, double weight*/
	public abstract Object[] getNthBestSrc(String userID, int n, long date);
	
	public abstract float getSrcWeight(String userID, String srcID);
	
					/*String srcID, double weight*/
	public abstract Object[] getNthBestDst(String userID, int n, long date);
	
	public abstract float getDstWeight(String userID, String dstID);
	
	public void nextTxn()
	{	
		if(transactionInd<transactions.size())
		{
			updateHeuristic(transactions.get(transactionInd));
		}
		transactionInd++;
	}
	
	protected abstract void updateHeuristic(String[] newTransaction);
	
	Hashtable<String, Long> parseUserEpsToDeletions(List<String[]> user_eps)
	{
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		Hashtable<String, Long> deletions=new Hashtable<>();
		
		for(String[] line: user_eps)
		{
			if(line.length>4 && !line[4].equals(""))
			{
				int dotInd=line[4].lastIndexOf('.');
				long date=0;
				try 
				{
					date = sdf.parse(line[4].substring(0, dotInd)).getTime();
					String machineID=line[0];
					deletions.put(machineID, date);
				} 
				catch (ParseException e) 
				{
					e.printStackTrace();
				}
			}
		}
		
		return deletions;
	}
	
	public float getAdditionalNetInfo(String userID, boolean srcDst)
	{
		return Float.NaN;
	}

}