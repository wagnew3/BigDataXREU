package throughputPrediction;

import java.util.List;

import init.IDData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

public class Median extends TPHeuristic
{

	long windowSize;
	int minNumInMedian=10;
	int maxNumInMedian;
	
	public Median(List<String[]> transactions, List<String[]> deletions, List<String[]> users, List<String[]> authIDs,
			long date, long windowSize) 
	{
		super(transactions, deletions, users, authIDs, date);
		this.windowSize=windowSize;
	}

	@Override
	public float getThrougputEstimation(String srcEP, String dstEP, long date, long amount) 
	{
		if(epPairDateThroughputs.get(srcEP)!=null && epPairDateThroughputs.get(srcEP).get(dstEP)!=null)
		{
			List<IDData<Long>> epPairDateThroughputList=epPairDateThroughputs.get(srcEP).get(dstEP);
			while(!epPairDateThroughputList.isEmpty() 
					&& (long)epPairDateThroughputList.get(0).data<date-windowSize)
			{
				epPairDateThroughputList.remove(0);
			}
			
			List<IDData<Long>> txnInfos=new ArrayList<>();
			int ind=Collections.binarySearch(epPairDateThroughputs.get(srcEP).get(dstEP), new IDData<Long>("", date-windowSize));
			if(ind<0)
			{
				ind=-(ind+1);
			}
			int medianInd=(epPairDateThroughputs.get(srcEP).get(dstEP).size()+ind)/2;
			if(medianInd<epPairDateThroughputs.get(srcEP).get(dstEP).size())
			{
				for(int addInd=medianInd; addInd<epPairDateThroughputs.get(srcEP).get(dstEP).size(); addInd++)
				{
					txnInfos.add(epPairDateThroughputs.get(srcEP).get(dstEP).get(medianInd));
				}
				Collections.sort(txnInfos, new Comparator<IDData<Long>>() 
				{
					@Override
					public int compare(IDData<Long> o1, IDData<Long> o2) 
					{
						return (int)Math.signum(o1.doubleData-o2.doubleData);
					}
				});
				return (float)txnInfos.get(txnInfos.size()/2).doubleData;
			}
		}
		return 0;
	}

	@Override
	public float getThrougputEstimationWeight(String srcEP, String dstEP, long amount)
	{
		if(epPairDateThroughputs.get(srcEP)!=null && epPairDateThroughputs.get(srcEP).get(dstEP)!=null)
		{
			return (float)epPairDateThroughputs.get(srcEP).get(dstEP).size()/Math.max(minNumInMedian, maxNumInMedian);
		}
		return -1.0f;
	}

	@Override
	protected void updateHeuristic(String[] newTransaction) 
	{
		String srcID=newTransaction[14];
		String dstID=newTransaction[15];
		if(epPairDateThroughputs.get(srcID).get(dstID).size()>maxNumInMedian)
		{
			maxNumInMedian=epPairDateThroughputs.get(srcID).get(dstID).size();
		}
	}

}
