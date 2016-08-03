package throughputPrediction;

import java.util.List;
import java.util.Hashtable;
import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class RecursiveLeastSquares extends TPHeuristic
{
										//t,L,P,s,pTP
	Hashtable<String, Hashtable<String, double[]>> previousParams;
	
	double P0=10.0;
	double forgettingFactor=0.7;
	float initialRate=1.0f/10000000.0f;
	int minNumberPoints=10;
	int maxNumberPoints;

	public RecursiveLeastSquares(List<String[]> transactions, List<String[]> deletions, List<String[]> users,
			List<String[]> authIDs, long date) 
	{
		super(transactions, deletions, users, authIDs, date);
		previousParams=new Hashtable<>();
	}

	@Override
	public float getThrougputEstimation(String srcEP, String dstEP, long date, long amount) 
	{
		if(previousParams.get(srcEP)!=null && previousParams.get(srcEP).get(dstEP)!=null)
		{
			double[] previousParamsArray=previousParams.get(srcEP).get(dstEP);
			double invRate=previousParamsArray[4]+previousParamsArray[1]
					*(previousParamsArray[0]-previousParamsArray[3]*previousParamsArray[4]);
			return (float)(1.0/invRate);
		}
		return 1.0f/initialRate;
	}

	@Override
	public float getThrougputEstimationWeight(String srcEP, String dstEP, long amount) 
	{
		if(epPairDateThroughputs.get(srcEP)!=null && epPairDateThroughputs.get(srcEP).get(dstEP)!=null)
		{
			return (float)epPairDateThroughputs.get(srcEP).get(dstEP).size()/Math.max(minNumberPoints, maxNumberPoints);
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
			String srcID=newTransaction[14];
			String dstID=newTransaction[15];
			
			if(previousParams.get(srcID)==null)
			{
				previousParams.put(srcID, new Hashtable<>());
			}
			if(previousParams.get(srcID).get(dstID)==null)
			{
				previousParams.get(srcID).put(dstID, new double[5]);
				previousParams.get(srcID).get(dstID)[2]=P0;
				previousParams.get(srcID).get(dstID)[4]=initialRate;
			}
			
			double[] previousParamsArray=previousParams.get(srcID).get(dstID);
			
			double predictedTP=previousParamsArray[4]+previousParamsArray[1]*(previousParamsArray[0]-previousParamsArray[3]*previousParamsArray[4]);
			
			previousParamsArray[0]=(endTime-startTime)/1000.0;
			previousParamsArray[3]=amount;
			previousParamsArray[4]=predictedTP;
			
			previousParamsArray[1]=previousParamsArray[2]*previousParamsArray[3]
					/(forgettingFactor+previousParamsArray[2]*previousParamsArray[3]*previousParamsArray[3]);
			
			previousParamsArray[2]=(1/forgettingFactor)*(previousParamsArray[2]
					-previousParamsArray[2]*previousParamsArray[3]*previousParamsArray[2]*previousParamsArray[3]
							/(forgettingFactor+previousParamsArray[2]*previousParamsArray[3]*previousParamsArray[3]));
			
			previousParams.get(srcID).put(dstID, previousParamsArray);
			
			if(epPairDateThroughputs.get(srcID).get(dstID).size()>maxNumberPoints)
			{
				maxNumberPoints=epPairDateThroughputs.get(srcID).get(dstID).size();
			}
		}
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
	}

}
