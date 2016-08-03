package throughputPrediction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;

public class LastSeen extends TPHeuristic
{
	
	protected Hashtable<String, Hashtable<String, Float>> lastSeenThroughput;

	public LastSeen(List<String[]> transactions, List<String[]> deletions, List<String[]> users, List<String[]> authIDs,
			long date) 
	{
		super(transactions, deletions, users, authIDs, date);
		lastSeenThroughput=new Hashtable<>();
	}

	@Override
	public float getThrougputEstimation(String srcEP, String dstEP, long date, long amount) 
	{
		if(lastSeenThroughput.get(srcEP)!=null && lastSeenThroughput.get(srcEP).get(dstEP)!=null)
		{
			return lastSeenThroughput.get(srcEP).get(dstEP);
		}
		return 0;
	}

	@Override
	public float getThrougputEstimationWeight(String srcEP, String dstEP, long amount) 
	{
		if(lastSeenThroughput.get(srcEP)!=null && lastSeenThroughput.get(srcEP).get(dstEP)!=null)
		{
			return 1.0f;
		}
		else
		{
			return -1.0f;
		}
	}

	@Override
	protected void updateHeuristic(String[] newTransaction) 
	{
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		try 
		{
			long startTime=sdf.parse(newTransaction[3]).getTime();
			long endTime=sdf.parse(newTransaction[13]).getTime();
			long amount=Long.parseLong(newTransaction[37]);
			String srcID=newTransaction[14];
			String dstID=newTransaction[15];
			if(lastSeenThroughput.get(srcID)==null)
			{
				lastSeenThroughput.put(srcID, new Hashtable<>());
			}
			lastSeenThroughput.get(srcID).put(dstID, (float)1000*amount/(endTime-startTime));
		}
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
	}

}
