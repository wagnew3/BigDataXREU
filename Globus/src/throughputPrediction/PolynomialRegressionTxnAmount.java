package throughputPrediction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

import init.IDData;

public class PolynomialRegressionTxnAmount extends TPHeuristic
{
	
	Hashtable<String, Hashtable<String, PolynomialFunction>> models;
	Hashtable<String, Hashtable<String, List<IDData<Long>>>> epPairAmountThroughputs;
	int minNumberPoints=10;
	int maxNumberPoints;
	protected int degree;
	protected int updateWindow;
	protected int updateStart;

	public PolynomialRegressionTxnAmount(List<String[]> transactions, List<String[]> deletions, List<String[]> users, List<String[]> authIDs,
			long date, int degree, int updateStart, int updateSteps) 
	{
		super(transactions, deletions, users, authIDs, date);
		this.models=new Hashtable<>();
		this.epPairAmountThroughputs=new Hashtable<>();
		this.degree=degree;
		this.updateWindow=updateSteps;
		this.updateStart=updateStart;
	}

	@Override
	public float getThrougputEstimation(String srcEP, String dstEP, long date, long amount) 
	{
		if(models.get(srcEP)!=null && models.get(srcEP).get(dstEP)!=null)
		{
			PolynomialFunction model=models.get(srcEP).get(dstEP);
			return (float)model.value(amount);
		}
		return 0;
	}

	@Override
	public float getThrougputEstimationWeight(String srcEP, String dstEP, long amount) 
	{
		if(epPairDateThroughputs.get(srcEP)!=null && epPairDateThroughputs.get(srcEP).get(dstEP)!=null)
		{
			int size=epPairDateThroughputs.get(srcEP).get(dstEP).size();
			if(size>=updateStart)
			{
				size/=updateWindow;
				size*=updateWindow;
			}
			return (float)size/Math.max(minNumberPoints, maxNumberPoints);
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
		
			IDData<Long> txnInfo=new IDData<Long>("", amount);
			txnInfo.doubleData=1000.0*amount/(endTime-startTime);
			
			if(epPairAmountThroughputs.get(srcID)==null)
			{
				epPairAmountThroughputs.put(srcID, new Hashtable<>());
			}
			if(epPairAmountThroughputs.get(srcID).get(dstID)==null)
			{
				epPairAmountThroughputs.get(srcID).put(dstID, new ArrayList<>());
			}
			int insInd=Collections.binarySearch(epPairAmountThroughputs.get(srcID).get(dstID), txnInfo);
			if(insInd<0)
			{
				insInd=-(insInd+1);
			}
			if(insInd>=epPairAmountThroughputs.get(srcID).get(dstID).size())
			{
				epPairAmountThroughputs.get(srcID).get(dstID).add(txnInfo);
			}
			else
			{
				epPairAmountThroughputs.get(srcID).get(dstID).add(insInd, txnInfo);
			}
			
			List<IDData<Long>> txnInfos=epPairAmountThroughputs.get(srcID).get(dstID);
			if(txnInfos.size()<updateStart || txnInfos.size()%updateWindow==0)
			{
				List<WeightedObservedPoint> pastObsCorr=new ArrayList<>();
				for(int txnInfosInd=0; txnInfosInd<txnInfos.size(); txnInfosInd++)
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
				if(models.get(srcID)!=null && models.get(srcID).get(dstID)!=null)
				{
					arimaFitter.withStartPoint(models.get(srcID).get(dstID).getCoefficients());
				}
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
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
	}
	
}
