package throughputPrediction;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

import init.IDData;

public class PolynomialRecursion extends TPHeuristic
{

	protected int updateSteps;
	protected int degree;
	Hashtable<String, Hashtable<String, PolynomialFunction>> models;
	int minNumberPoints=10;
	int maxNumberPoints;
	
	public PolynomialRecursion(List<String[]> transactions, List<String[]> deletions, List<String[]> users,
			List<String[]> authIDs, long date, int updateSteps, int degree) 
	{
		super(transactions, deletions, users, authIDs, date);
		this.updateSteps=updateSteps;
		this.degree=degree;
		models=new Hashtable<>();
	}

	@Override
	public float getThrougputEstimation(String srcEP, String dstEP, long date, long amount) 
	{
		if(models.get(srcEP)!=null && models.get(srcEP).get(dstEP)!=null)
		{
			PolynomialFunction model=models.get(srcEP).get(dstEP);
			return (float)model.value(date);
		}
		return 0;
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
		String srcID=newTransaction[14];
		String dstID=newTransaction[15];
		
		if(epPairDateThroughputs.get(srcID).get(dstID).size()-1%updateSteps==0)
		{
			List<IDData<Long>> txnInfos=epPairDateThroughputs.get(srcID).get(dstID);
			List<WeightedObservedPoint> pastObsCorr=new ArrayList<>();
			for(int txnInfosInd=0; txnInfosInd<epPairDateThroughputs.size(); txnInfosInd++)
			{
				if(txnInfosInd==0)
				{
					pastObsCorr.add(new WeightedObservedPoint(1.0, 0, txnInfos.get(txnInfosInd).doubleData));
				}
				else
				{
					pastObsCorr.add(new WeightedObservedPoint(1.0, (long)txnInfos.get(txnInfosInd-1).data, txnInfos.get(txnInfosInd).doubleData));
				}
			}
			PolynomialCurveFitter arimaFitter=PolynomialCurveFitter.create(degree);
			PolynomialFunction model=new PolynomialFunction(arimaFitter.fit(pastObsCorr));
			
			if(models.get(srcID)==null)
			{
				models.put(srcID, new Hashtable<>());
			}
			models.get(srcID).put(dstID, model);
			
			if(txnInfos.size()>maxNumberPoints)
			{
				maxNumberPoints=txnInfos.size();
			}
		}
	}

}
