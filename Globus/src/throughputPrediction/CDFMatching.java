package throughputPrediction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.regression.MillerUpdatingRegression;
import org.apache.commons.math3.stat.regression.RegressionResults;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import init.IDData;

public class CDFMatching extends TPHeuristic
{
																	//sum x, sum x^2, sum xy, regressionCoeff
	protected Hashtable<String, Hashtable<String, double[]>> corrData;
	protected Hashtable<String, List<IDData<Long>>> epPairThroughputs;
	protected Hashtable<String, Double> epPairCDFPositions;
	protected int updateSteps;
	protected int updateStart;

	public CDFMatching(List<String[]> transactions, List<String[]> deletions, List<String[]> users,
			List<String[]> authIDs, long date, int updateStart, int updateSteps) 
	{
		super(transactions, deletions, users, authIDs, date);
		this.updateSteps=updateSteps;
		this.updateStart=updateStart;
		this.corrData=new Hashtable<>();
		this.epPairThroughputs=new Hashtable<>();
		this.epPairCDFPositions=new Hashtable<>();
	}

	@Override
	public float getThrougputEstimation(String srcEP, String dstEP, long date, long amount) 
	{
		double weightedTPSum=0.0;
		double weightsSum=0.0;
		String srcDstEP=srcEP+" "+dstEP;
		if(corrData.get(srcDstEP)!=null)
		{
			Hashtable<String, double[]> cdfCorrCoeffs=corrData.get(srcDstEP);
			for(String cdfSrcDstEP: cdfCorrCoeffs.keySet())
			{
				double cdfCorrCoeff=cdfCorrCoeffs.get(cdfSrcDstEP)[3];
				if(cdfCorrCoeff>0)
				{
					weightsSum+=cdfCorrCoeff;
					double lastTPCDFPos=epPairCDFPositions.get(cdfSrcDstEP);
					weightedTPSum+=lastTPCDFPos*cdfCorrCoeff;
				}
			}
			if(Math.abs(weightsSum)>0)
			{
				double cdfFrac=weightedTPSum/weightsSum;
				int cdfPos=(int)Math.round(epPairThroughputs.get(srcDstEP).size()*cdfFrac);
				if(cdfPos==epPairThroughputs.get(srcDstEP).size())
				{
					cdfPos--;
				}
				long tp=(long)epPairThroughputs.get(srcDstEP).get(cdfPos).data;
				
				return tp;
			}
		}
		return 0.0f;
	}

	@Override
	public float getThrougputEstimationWeight(String srcEP, String dstEP, long amount) 
	{
		if(epPairDateThroughputs.get(srcEP)!=null && epPairDateThroughputs.get(srcEP).get(dstEP)!=null)
		{
			return Float.POSITIVE_INFINITY;
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
			
			String srcDstEP=srcID+" "+dstID;
			
			IDData<Long> txnInfo=new IDData<Long>("", (long)((double)1000.0*amount/(endTime-startTime)));
			if(epPairThroughputs.get(srcDstEP)==null)
			{
				epPairThroughputs.put(srcDstEP, new ArrayList<>());
			}
			int insInd=Collections.binarySearch(epPairThroughputs.get(srcDstEP), txnInfo);
			if(insInd<0)
			{
				insInd=-(insInd+1);
			}
			if(insInd<epPairThroughputs.get(srcDstEP).size())
			{
				epPairThroughputs.get(srcDstEP).add(insInd, txnInfo);
			}
			else
			{
				epPairThroughputs.get(srcDstEP).add(txnInfo);
			}
			
			double cdfPos=(double)insInd/epPairThroughputs.get(srcDstEP).size();
			epPairCDFPositions.put(srcDstEP, cdfPos);
			
			if(epPairDateThroughputs.get(srcID).get(dstID).size()>=2
					&& ((epPairDateThroughputs.get(srcID).get(dstID).size()-2)%updateSteps==0
							|| (epPairDateThroughputs.get(srcID).get(dstID).size()-2)<updateStart))
			{
				Hashtable<String, Hashtable<String, Double>> epPairsToWeights=new Hashtable<>();

				List<IDData<Long>> txnInfoList=epPairDateThroughputs.get(srcID).get(dstID);
				double sumY=0;
				double sumY2=0;
				for(int obsInd=0; obsInd<txnInfoList.size(); obsInd++)
				{
					IDData<Long> currentDatePoint=txnInfoList.get(obsInd);
					sumY+=currentDatePoint.doubleData;
					sumY2+=currentDatePoint.doubleData*currentDatePoint.doubleData;
				}
				
				if(corrData.get(srcDstEP)==null)
				{
					corrData.put(srcDstEP, new Hashtable<>());
				}
				IDData<Long> currentDatePoint=txnInfoList.get(txnInfoList.size()-1);
				
				Hashtable<String, double[]> corrDataTable=corrData.get(srcDstEP);
				for(String rSrcID: epPairDateThroughputs.keySet())
				{
					epPairsToWeights.put(rSrcID, new Hashtable<>());
					Hashtable<String, List<IDData<Long>>> rDstDateThroughputs=epPairDateThroughputs.get(rSrcID);
					for(String rDstID: rDstDateThroughputs.keySet())
					{
						double sumX=0.0;
						double sumX2=0.0;
						double sumXY=0.0;
						List<IDData<Long>> corrTxnInfoList=rDstDateThroughputs.get(rDstID);
						
						String rSrcDstID=rSrcID+" "+rDstID;
						if(corrDataTable.get(rSrcDstID)!=null)
						{
							double[] corrDataInfo=corrDataTable.get(rSrcDstID);
							sumX=corrDataInfo[0];
							sumX2=corrDataInfo[1];
							sumXY=corrDataInfo[2];
						}
						
						double x=corrTxnInfoList.get(corrTxnInfoList.size()-1).doubleData;	
						sumX+=x;
						sumX2+=x*x;
						sumXY+=x*currentDatePoint.doubleData;
						int n=txnInfoList.size();
						double correlationCoeff=(n*sumXY-sumX*sumY)
													/Math.pow((n*sumX2-sumX*sumX)*(n*sumY2-sumY*sumY), 0.5);
						
						double[] newCorrDataInfo=new double[]{sumX, sumX2, sumXY, correlationCoeff};
						corrDataTable.put(rSrcDstID, newCorrDataInfo);
					}
				}
			}
		}
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
	}

}
