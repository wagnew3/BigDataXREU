package throughputPrediction;

import java.util.List;

import init.IDData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Hashtable;

public class Average extends TPHeuristic
{
	
	Hashtable<String, Hashtable<String, Float>> averages;
	Hashtable<String, Hashtable<String, Integer>> numInAverage;
	float minTP=1000000;
	float maxTP;
	int minNum=10;
	int maxInAvg=0;
	long windowLength;

	public Average(List<String[]> transactions, List<String[]> deletions, List<String[]> users, List<String[]> authIDs,
			long date, long windowLength) 
	{
		super(transactions, deletions, users, authIDs, date);
		this.windowLength=windowLength;
		this.averages=new Hashtable<>();
		this.numInAverage=new Hashtable<>();
	}
	
	@Override
	public float getThrougputEstimation(String srcEP, String dstEP, long date, long amount) 
	{
		if(averages.get(srcEP)!=null && averages.get(srcEP).get(dstEP)!=null)
		{
			return (float)averages.get(srcEP).get(dstEP);
		}
		return 0;
	}

	@Override
	public float getThrougputEstimationWeight(String srcEP, String dstEP, long amount) 
	{
		if(numInAverage.get(srcEP)!=null && numInAverage.get(srcEP).get(dstEP)!=null)
		{
			return (float)numInAverage.get(srcEP).get(dstEP)/Math.max(minNum, maxInAvg);
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
			long endTime=sdf.parse(newTransaction[13]).getTime();
			float total=0.0f;
			int numberTxns=0;
			String srcID=newTransaction[14];
			String dstID=newTransaction[15];
			for(IDData<Long> txnInfo: epPairDateThroughputs.get(srcID).get(dstID))
			{
				if((long)txnInfo.data>endTime-windowLength)
				{
					total+=txnInfo.doubleData;
					numberTxns++;
				}
			}
			
			if(averages.get(srcID)==null)
			{
				averages.put(srcID, new Hashtable<String, Float>());
				numInAverage.put(srcID, new Hashtable<String, Integer>());
			}
			if(averages.get(srcID).get(dstID)==null)
			{
				averages.get(srcID).put(dstID, -1.0f);
				numInAverage.get(srcID).put(dstID, 0);
			}
			if(numberTxns>0)
			{
				averages.get(srcID).put(dstID, total/numberTxns);
				numInAverage.get(srcID).put(dstID, numberTxns);
				if(total/numberTxns>maxTP)
				{
					maxTP=total/numberTxns;
				}
				if(numberTxns>maxInAvg)
				{
					maxInAvg=numberTxns;
				}
			}
		}
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
	}
	
}
