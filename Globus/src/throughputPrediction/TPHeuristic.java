package throughputPrediction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;

import init.IDData;
import userSrcDstPrediction.EPHeuristic;

public abstract class TPHeuristic 
{
	
	List<String[]> transactions; //sorted by date
	protected Hashtable<String, Hashtable<String, List<IDData<Long>>>> epPairDateThroughputs;
	int transactionInd; //need to set this
	long date;
	Hashtable<String, Long> deletions;
	
	public TPHeuristic(List<String[]> transactions, List<String[]> deletions, 
			List<String[]> users, List<String[]> authIDs, long date)
	{
		this.transactions=transactions;
		this.date=date;
		this.deletions=parseUserEpsToDeletions(deletions);
		this.epPairDateThroughputs=new Hashtable<>();
	}

	public abstract float getThrougputEstimation(String srcEP, String dstEP, long date, long amount);
	
	public abstract float getThrougputEstimationWeight(String srcEP, String dstEP, long amount);
	
	public void nextTxn(String[] transaction)
	{	
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		try 
		{
			long startTime=sdf.parse(transaction[3]).getTime();
			long endTime=sdf.parse(transaction[13]).getTime();
			long amount=Long.parseLong(transaction[37]);
			String srcID=transaction[14];
			String dstID=transaction[15];
			
			if(epPairDateThroughputs.get(srcID)==null)
			{
				epPairDateThroughputs.put(srcID, new Hashtable<>());
			}
			if(epPairDateThroughputs.get(srcID).get(dstID)==null)
			{
				epPairDateThroughputs.get(srcID).put(dstID, new ArrayList<IDData<Long>>());
			}
			IDData<Long> txnInfo=new IDData<Long>("", endTime);
			txnInfo.doubleData=(double)1000.0*amount/(endTime-startTime);
			epPairDateThroughputs.get(srcID).get(dstID).add(txnInfo);
		}
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
		updateHeuristic(transaction);
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
	
	public float getNetLinkLoadEstmiation(String userID, boolean srcDst)
	{
		return Float.NaN;
	}

}
