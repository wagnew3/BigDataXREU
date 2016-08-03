package throughputPrediction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import init.IDData;

public class AllMedian extends TPHeuristic
{

	List<IDData<Double>> txnsByAmt;
	
	int minNumInMedian=10;
	int maxNumInMedian;
	
	public AllMedian(List<String[]> transactions, List<String[]> deletions, List<String[]> users, List<String[]> authIDs,
			long date) 
	{
		super(transactions, deletions, users, authIDs, date);
		txnsByAmt=new ArrayList<>();
	}

	@Override
	public float getThrougputEstimation(String srcEP, String dstEP, long date, long amount) 
	{
		if(!txnsByAmt.isEmpty())
		{
			return (float)(double)txnsByAmt.get(txnsByAmt.size()/2).data;
		}
		return 0;
	}

	@Override
	public float getThrougputEstimationWeight(String srcEP, String dstEP, long amount)
	{
		if(!txnsByAmt.isEmpty())
		{
			return (float)(1.0/Math.abs(txnsByAmt.get(txnsByAmt.size()/2).doubleData-amount));
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
			if(epPairDateThroughputs.get(srcID).get(dstID).size()>maxNumInMedian)
			{
				maxNumInMedian=epPairDateThroughputs.get(srcID).get(dstID).size();
			}
			
			IDData<Double> txnInfo=new IDData<Double>("", 1000.0*amount/(endTime-startTime));
			txnInfo.doubleData=amount;
			int ind=Collections.binarySearch(txnsByAmt, txnInfo);
			if(ind<0)
			{
				ind=-(ind+1);
			}
			else
			{
				ind--;
			}
			if(ind==txnsByAmt.size())
			{
				txnsByAmt.add(txnInfo);
			}
			else
			{
				txnsByAmt.add(ind, txnInfo);
			}
		}
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
	}
	
}