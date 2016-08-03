package throughputPrediction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import init.IDData;

public class UserMedian  extends TPHeuristic
{

	Hashtable<String, Hashtable<String, List<IDData<Double>>>> txnsByAmt;
	Hashtable<String, String> epToOwner;
	
	int minNumInMedian=10;
	int maxNumInMedian;
	
	public UserMedian(List<String[]> transactions, List<String[]> deletions, List<String[]> users, List<String[]> authIDs,
			long date) 
	{
		super(transactions, deletions, users, authIDs, date);
		txnsByAmt=new Hashtable<>();
		epToOwner=new Hashtable<>();
		
		for(String[] deletionLine: deletions)
		{
			String epID=deletionLine[0];
			String userID=deletionLine[2];
			epToOwner.put(epID, userID);;
		}
	}

	@Override
	public float getThrougputEstimation(String srcEP, String dstEP, long date, long amount) 
	{
		String srcUSer=epToOwner.get(srcEP);
		String dstUser=epToOwner.get(dstEP);
		if(srcUSer!=null && dstUser!=null && txnsByAmt.get(srcUSer)!=null 
				&& txnsByAmt.get(srcUSer).get(dstUser)!=null 
				&& !txnsByAmt.get(srcUSer).get(dstUser).isEmpty())
		{
			return (float)(double)txnsByAmt.get(srcUSer).get(dstUser).get(txnsByAmt.get(srcUSer).get(dstUser).size()/2).data;
		}
		return 0;
	}

	@Override
	public float getThrougputEstimationWeight(String srcEP, String dstEP, long amount)
	{
		String srcUser=epToOwner.get(srcEP);
		String dstUser=epToOwner.get(dstEP);
		if(srcUser!=null && dstUser!=null && txnsByAmt.get(srcUser)!=null 
				&& txnsByAmt.get(srcUser).get(dstUser)!=null && !txnsByAmt.get(srcUser).get(dstUser).isEmpty())
		{
			return (float)(1.0/Math.abs(txnsByAmt.get(srcUser).get(dstUser).get(txnsByAmt.get(srcUser).get(dstUser).size()/2).doubleData-amount));
		}
		return -1.0f;
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
			
			String srcUser=epToOwner.get(srcID);
			String dstUser=epToOwner.get(dstID);	
			if(srcUser!=null && dstUser!=null)
			{
				if(txnsByAmt.get(srcUser)==null)
				{
					txnsByAmt.put(srcUser, new Hashtable<>());
				}
				if(txnsByAmt.get(srcUser).get(dstUser)==null)
				{
					txnsByAmt.get(srcUser).put(dstUser, new ArrayList<>());
				}

				if(epPairDateThroughputs.get(srcID).get(dstID).size()>maxNumInMedian)
				{
					maxNumInMedian=epPairDateThroughputs.get(srcID).get(dstID).size();
				}
				
				IDData<Double> txnInfo=new IDData<Double>("", 1000.0*amount/(endTime-startTime));
				txnInfo.doubleData=amount;
				int ind=Collections.binarySearch(txnsByAmt.get(srcUser).get(dstUser), txnInfo);
				if(ind<0)
				{
					ind=-(ind+1);
				}
				else
				{
					ind--;
				}
				if(ind==txnsByAmt.get(srcUser).get(dstUser).size())
				{
					txnsByAmt.get(srcUser).get(dstUser).add(txnInfo);
				}
				else
				{
					txnsByAmt.get(srcUser).get(dstUser).add(ind, txnInfo);
				}
			}
		}
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
	}
	
}