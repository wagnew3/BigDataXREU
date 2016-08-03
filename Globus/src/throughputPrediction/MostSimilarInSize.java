package throughputPrediction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import init.IDData;

public class MostSimilarInSize extends TPHeuristic
{
	protected List<IDData<Long>> recentTransactions;
	protected long timeFrame;

	public MostSimilarInSize(List<String[]> transactions, List<String[]> deletions, List<String[]> users,
			List<String[]> authIDs, long date, long timeFrame) 
	{
		super(transactions, deletions, users, authIDs, date);
		recentTransactions=new ArrayList<>();
		this.timeFrame=timeFrame;
	}

	@Override
	public float getThrougputEstimation(String srcEP, String dstEP, long date, long amount) 
	{
		if(!recentTransactions.isEmpty())
		{
			while(!recentTransactions.isEmpty()
					&& (long)recentTransactions.get(0).data<date-timeFrame)
			{
				recentTransactions.remove(0);
			}
			if(recentTransactions.isEmpty())
			{
				return 0.0f;
			}
			
			IDData<Long> txnInfo=new IDData<Long>("", amount);
			int mostSimilarInd=Collections.binarySearch(recentTransactions, txnInfo);
			if(mostSimilarInd<0)
			{
				mostSimilarInd=-(mostSimilarInd+1);
			}
			if(mostSimilarInd>=recentTransactions.size())
			{
				mostSimilarInd--;
			}
			return (float)recentTransactions.get(mostSimilarInd).doubleData;
		}
		return 0;
	}

	@Override
	public float getThrougputEstimationWeight(String srcEP, String dstEP, long amount) 
	{
		if(!recentTransactions.isEmpty())
		{
			IDData<Long> txnInfo=new IDData<Long>("", amount);
			int mostSimilarInd=Collections.binarySearch(recentTransactions, txnInfo);
			if(mostSimilarInd<0)
			{
				mostSimilarInd=-(mostSimilarInd+1);
			}
			if(mostSimilarInd>=recentTransactions.size())
			{
				mostSimilarInd--;
			}
			return (float)(1/(Math.abs(amount-(long)recentTransactions.get(mostSimilarInd).data)+1));
		}
		return 0;
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
			
			IDData<Long> txnInfo=new IDData<Long>("", amount);
			txnInfo.doubleData=1000.0*amount/(endTime-startTime);
			
			int insInd=Collections.binarySearch(recentTransactions, txnInfo);
			
			if(insInd<0)
			{
				insInd=-(insInd+1);
			}
			if(insInd>=recentTransactions.size())
			{
				recentTransactions.add(txnInfo);
			}
			else
			{
				recentTransactions.add(insInd, txnInfo);
			}
		}
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
	}

}
