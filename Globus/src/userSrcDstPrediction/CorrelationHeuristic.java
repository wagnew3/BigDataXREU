package userSrcDstPrediction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.SingleGraph;

import init.IDData;

public class CorrelationHeuristic extends EPHeuristic
{

	Hashtable<String, List<IDData<Double>>> srcCorrelations;
	Hashtable<String, Hashtable<String, Double>> srcCorrAmounts;
	Hashtable<String, List<IDData<Double>>> dstCorrelations;
	Hashtable<String, Hashtable<String, Double>> dstCorrAmounts;
	
	Hashtable<String, String[]> userHistories;
	float maxCorrSizeSrc=0.0f;
	float maxCorrSizeDst=0.0f;
	
	float maxSrcCorr=0.0f;
	float maxDstCorr=0.0f;
	
	float minCorr=1.0f;
	
	public CorrelationHeuristic(List<String[]> transactions, List<String[]> deletions, 
			List<String[]> users, List<String[]> authIDs, long date)
	{
		super(transactions, deletions, users, authIDs, date);
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		srcCorrelations=new Hashtable<>();
		srcCorrAmounts=new Hashtable<>();
		dstCorrelations=new Hashtable<>();
		dstCorrAmounts=new Hashtable<>();
		userHistories=new Hashtable<>();
		for(String[] line: transactions)
		{
			try 
			{
				long currentDate=sdf.parse(line[3]).getTime();
				if(currentDate>=date)
				{
					break;
				}
				transactionInd++;
				
				String userID=line[2];
				String srcID=line[14];
				String dstID=line[15];
				
				if(userHistories.get(userID)!=null)
				{
					String lastSrcID=userHistories.get(userID)[0];
					String lastDstID=userHistories.get(userID)[1];
					
					if(srcCorrelations.get(lastSrcID)==null)
					{
						srcCorrelations.put(lastSrcID, new ArrayList<IDData<Double>>());
						srcCorrAmounts.put(lastSrcID, new Hashtable<>());
					}
					if(!srcCorrelations.get(lastSrcID).contains(srcID))
					{
						srcCorrelations.get(lastSrcID).add(new IDData<Double>(srcID, 1.0));
						srcCorrAmounts.get(lastSrcID).put(srcID, 0.0);
					}
					int srcInd=srcCorrelations.get(lastSrcID).indexOf(srcID);
					srcCorrelations.get(lastSrcID).get(srcInd).data
						=(Double)srcCorrelations.get(lastSrcID).get(srcInd).data+1;
					srcCorrAmounts.get(lastSrcID).put(srcID, srcCorrAmounts.get(lastSrcID).get(srcID)+1.0);
					
					if(dstCorrelations.get(lastDstID)==null)
					{
						dstCorrelations.put(lastDstID, new ArrayList<IDData<Double>>());
						dstCorrAmounts.put(lastDstID, new Hashtable<>());
					}
					if(!dstCorrelations.get(lastDstID).contains(dstID))
					{
						dstCorrelations.get(lastDstID).add(new IDData<Double>(dstID, 1.0));
						dstCorrAmounts.get(lastDstID).put(dstID, 0.0);
					}
					int dstInd=dstCorrelations.get(lastDstID).indexOf(dstID);
					dstCorrelations.get(lastDstID).get(dstInd).data
						=(Double)dstCorrelations.get(lastDstID).get(dstInd).data+1;
					dstCorrAmounts.get(lastDstID).put(dstID, dstCorrAmounts.get(lastDstID).get(dstID)+1.0);
				}
				
				userHistories.put(userID, new String[]{srcID, dstID});
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
		}
		
		for(String srcID: srcCorrelations.keySet())
		{
			List<IDData<Double>> correlations=srcCorrelations.get(srcID);
			float corrSum=0.0f;
			for(IDData<Double> corr: correlations)
			{
				if(maxSrcCorr<(float)(double)(Double)corr.data)
				{
					maxSrcCorr=(float)(double)(Double)corr.data;
				}
				corrSum+=(float)(double)(Double)corr.data;
			}
			if(corrSum>maxCorrSizeSrc)
			{
				maxCorrSizeSrc=corrSum;
			}
		}
		
		for(String dstID: dstCorrelations.keySet())
		{
			List<IDData<Double>> correlations=dstCorrelations.get(dstID);
			float corrSum=0.0f;
			for(IDData<Double> corr: correlations)
			{
				if(maxDstCorr<(float)(double)(Double)corr.data)
				{
					maxDstCorr=(float)(double)(Double)corr.data;
				}
				corrSum+=(float)(double)(Double)corr.data;
			}
			if(corrSum>maxCorrSizeSrc)
			{
				maxCorrSizeDst=corrSum;
			}
		}
	}

	@Override
	public Object[] getNthBestSrc(String userID, int n, long date) 
	{
		if(userHistories.get(userID)!=null)
		{
			String lastSrcID=userHistories.get(userID)[0];
			List<IDData<Double>> sortedSrcIDs=srcCorrelations.get(lastSrcID);
			double totalTxns=0.0;
			if(sortedSrcIDs!=null)
			{
				for(int srcIDInd=0; srcIDInd<sortedSrcIDs.size(); srcIDInd++)
				{
					if(deletions.get(sortedSrcIDs.get(srcIDInd))!=null
							&& deletions.get(sortedSrcIDs.get(srcIDInd))<date)
					{
						sortedSrcIDs.remove(srcIDInd);
						srcIDInd--;
					}
					totalTxns+=(double)sortedSrcIDs.get(srcIDInd).data;
				}
				Collections.sort(sortedSrcIDs);
				if(n<sortedSrcIDs.size())
				{
					return new Object[]{sortedSrcIDs.get(sortedSrcIDs.size()-1-n).ID,
							(float)((double)sortedSrcIDs.get(sortedSrcIDs.size()-1-n).data/Math.max(totalTxns, minCorr))};
				}
			}
		}
		return new Object[]{"", 0.0f};
	}

	@Override
	public float getSrcWeight(String userID, String srcID) 
	{
		if(!srcID.isEmpty() && userHistories.get(userID)!=null)
		{
			String lastSrcID=userHistories.get(userID)[0];
			if(srcCorrelations.get(lastSrcID)!=null)
			{
				List<IDData<Double>> srcCorrelation=srcCorrelations.get(lastSrcID);
				double totalTxns=0.0;
				for(int srcIDInd=0; srcIDInd<srcCorrelation.size(); srcIDInd++)
				{
					totalTxns+=(double)srcCorrelation.get(srcIDInd).data;
				}
				int ind=srcCorrelations.get(lastSrcID).indexOf(new IDData<Double>(srcID, 0.0));
				if(ind>=0)
				{
					return (float)((double)srcCorrelation.get(ind).data
							/Math.max(totalTxns, minCorr));
				}
				else
				{
					return 0.0f;
				}
			}
		}
		return 0.0f;
	}

	@Override
	public Object[] getNthBestDst(String userID, int n, long date) 
	{
		if(userHistories.get(userID)!=null)
		{
			String lastDstID=userHistories.get(userID)[1];
			List<IDData<Double>> sortedDstIDs=dstCorrelations.get(lastDstID);
			double totalTxns=0.0;
			if(sortedDstIDs!=null)
			{
				for(int dstIDInd=0; dstIDInd<sortedDstIDs.size(); dstIDInd++)
				{
					if(deletions.get(sortedDstIDs.get(dstIDInd))!=null
							&& deletions.get(sortedDstIDs.get(dstIDInd))<date)
					{
						sortedDstIDs.remove(dstIDInd);
						dstIDInd--;
					}
					totalTxns+=(double)sortedDstIDs.get(dstIDInd).data;
				}
				Collections.sort(sortedDstIDs);
				if(n<sortedDstIDs.size())
				{
					return new Object[]{sortedDstIDs.get(sortedDstIDs.size()-1-n).ID, 
							(float)((double)sortedDstIDs.get(sortedDstIDs.size()-1-n).data/Math.max(totalTxns, minCorr))};
				}
			}
		}
		return new Object[]{"", 0.0f};
	}

	@Override
	public float getDstWeight(String userID, String dstID) 
	{
		if(!dstID.isEmpty() && userHistories.get(userID)!=null)
		{
			String lastDstID=userHistories.get(userID)[1];
			if(dstCorrelations.get(lastDstID)!=null)
			{
				List<IDData<Double>> dstCorrelation=dstCorrelations.get(lastDstID);
				double totalTxns=0.0;
				for(int dstIDInd=0; dstIDInd<dstCorrelation.size(); dstIDInd++)
				{
					totalTxns+=(double)dstCorrelation.get(dstIDInd).data;
				}
				int ind=dstCorrelations.get(lastDstID).indexOf(new IDData<Long>(dstID, 0.0));
				if(ind>=0)
				{
					return (float)((double)dstCorrelation.get(ind).data
							/Math.max(totalTxns, minCorr));
				}
				else
				{
					return 0.0f;
				}
			}
		}
		return 0.0f;
	}
	
	int numberFirstTxns=0;
	Hashtable<String, String> srcPairs=new Hashtable<>();
	int numberFirstSrcPairs=0;
	
	Hashtable<String, String> dstPairs=new Hashtable<>();
	int numberFirstDstPairs=0;
	
	Graph srcGraph=new SingleGraph("src");
	Graph dstGraph=new SingleGraph("dst");
	
	long totalSrcPathLength=0;
	int numberNewSrcWithPath=0;

	@Override
	protected void updateHeuristic(String[] newTransaction) 
	{
		String userID=newTransaction[2];
		String srcID=newTransaction[14];
		String dstID=newTransaction[15];

		if(userHistories.get(userID)!=null)
		{
			numberFirstTxns++;
			String lastSrcID=userHistories.get(userID)[0];
			String lastDstID=userHistories.get(userID)[1];
			
			
			if(srcPairs.get(srcID+" "+lastSrcID)==null)
			{
				numberFirstSrcPairs++;
				srcPairs.put(srcID+" "+lastSrcID, srcID+" "+lastSrcID);
			}
			
			
			if(dstPairs.get(dstID+" "+lastDstID)==null)
			{
				numberFirstDstPairs++;
				if(correct==1)
				{
					int u=0;
				}
				dstPairs.put(dstID+" "+lastDstID, dstID+" "+lastDstID);
			}
			
			if(srcCorrelations.get(lastSrcID)==null)
			{
				srcCorrelations.put(lastSrcID, new ArrayList<IDData<Double>>());
				srcCorrAmounts.put(lastSrcID, new Hashtable<>());
			}
			List<IDData<Double>> srcList=srcCorrelations.get(lastSrcID);
			IDData<Double> srcIDData=new IDData<Double>(srcID, 1.0);
			if(!srcList.contains(srcIDData))
			{
				srcList.add(srcIDData);
				srcCorrAmounts.get(lastSrcID).put(srcID, 0.0);
			}
			int srcInd=srcList.indexOf(srcIDData);
			srcList.get(srcInd).data=(Double)srcList.get(srcInd).data+1;
			srcCorrAmounts.get(lastSrcID).put(srcID, srcCorrAmounts.get(lastSrcID).get(srcID)+1.0);
			
			if(dstCorrelations.get(lastDstID)==null)
			{
				dstCorrelations.put(lastDstID, new ArrayList<IDData<Double>>());
				dstCorrAmounts.put(lastDstID, new Hashtable<>());
			}
			List<IDData<Double>> dstList=dstCorrelations.get(lastDstID);
			IDData<Double> dstIDData=new IDData<Double>(dstID, 1.0);
			if(!dstList.contains(dstIDData))
			{
				dstList.add(dstIDData);
				dstCorrAmounts.get(lastDstID).put(dstID, 0.0);
			}
			int dstInd=dstList.indexOf(dstIDData);
			dstList.get(dstInd).data=(Double)dstList.get(dstInd).data+1;
			dstCorrAmounts.get(lastDstID).put(dstID, dstCorrAmounts.get(lastDstID).get(dstID)+1.0);
			
			List<IDData<Double>> correlations=srcCorrelations.get(lastSrcID);
			float corrSum=0.0f;
			for(IDData<Double> corr: correlations)
			{
				if(maxSrcCorr<(float)(double)(Double)corr.data)
				{
					maxSrcCorr=(float)(double)(Double)corr.data;
				}
				corrSum+=(float)(double)(Double)corr.data;
			}
			if(corrSum>maxCorrSizeSrc)
			{
				maxCorrSizeSrc=corrSum;
			}
			correlations=dstCorrelations.get(lastDstID);
			corrSum=0.0f;
			for(IDData<Double> corr: correlations)
			{
				if(maxDstCorr<(float)(double)(Double)corr.data)
				{
					maxDstCorr=(float)(double)(Double)corr.data;
				}
				corrSum+=(float)(double)(Double)corr.data;
			}
			if(corrSum>maxCorrSizeSrc)
			{
				maxCorrSizeDst=corrSum;
			}
		}
		
		userHistories.put(userID, new String[]{srcID, dstID});
	}
													//true: src, false: dst
	public float getAdditionalNetInfo(String userID, boolean srcDst)
	{
		if(userHistories.get(userID)==null)
		{
			return 0.0f;
		}
		if(srcDst)
		{
			String lastSrcID=userHistories.get(userID)[0];
			if(srcCorrelations.get(lastSrcID)==null)
			{
				return 0.0f;
			}
			else
			{
				return (float)(srcCorrelations.get(lastSrcID).size()/Math.max(500.0, maxCorrSizeSrc));
			}
		}
		else
		{
			String lastDstID=userHistories.get(userID)[0];
			if(dstCorrelations.get(lastDstID)==null)
			{
				return 0.0f;
			}
			else
			{
				return (float)(dstCorrelations.get(lastDstID).size()/Math.max(500.0, maxCorrSizeDst));
			}
		}
	}
	
	
	
	static int correct;
	static String currentDir=System.getProperty("user.dir");
	private static double[] testHeuristic() throws IOException
	{
		List<String> failedTransactionsSrc=new ArrayList<>();
		List<String> failedTransactionsDst=new ArrayList<>();
		List<Integer> failedTransactionInds=new ArrayList<>();
		List<String> lines=Files.readAllLines(new File(currentDir+"/data/transfers-william.csv").toPath());
		List<String[]> splitData=new ArrayList<>();
		int amt=0;
		lines.remove(0).split(",");
		lines.remove(lines.size()-1);
		for(String line: lines)
		{
			splitData.add(line.split(","));
			amt++;
			if(amt%100000==0)
			{
				System.out.println(amt);
				if (amt>0)
				{
					//break;
				}
			}
		}
		
		List<String[]> user_eps=readDeletionLines();
		
		CorrelationHeuristic corrH=new CorrelationHeuristic(splitData, user_eps, 0);
		double srcAttempts=0.0;
		double srcCorrect=0.0;
		
		double dstAttempts=0.0;
		double dstCorrect=0.0;
		
		int topN=1;
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		
		for(int ind=0; ind<splitData.size(); ind++)
		{
			String[] line=splitData.get(ind);
			String userID=line[1];
			String srcID=line[2];
			String dstID=line[3];
			boolean added=false;
			
			long currentDate;
			try 
			{
				currentDate = sdf.parse(line[4]).getTime();
				List<String> srcRecs=new ArrayList<>();
				for(int recInd=0; recInd<topN; recInd++)
				{
					srcRecs.add((String)(corrH.getNthBestSrc(userID, recInd, currentDate)[0]));
				}
				srcAttempts++;
				if(srcRecs.contains(srcID))
				{
					srcCorrect++;
				}
				else
				{
					failedTransactionsSrc.add(lines.get(ind));
					failedTransactionInds.add(ind);
					added=true;
				}
				
				List<String> dstRecs=new ArrayList<>();
				for(int recInd=0; recInd<topN; recInd++)
				{
					dstRecs.add((String)(corrH.getNthBestDst(userID, recInd, currentDate)[0]));
				}
				dstAttempts++;
				correct=0;
				if(dstRecs.contains(dstID))
				{
					dstCorrect++;
					correct=1;
				}
				else
				{
					failedTransactionsDst.add(lines.get(ind));
					if(!added)
					{
						failedTransactionInds.add(ind);
					}
				}
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			corrH.nextTxn();
		}
		
		Files.write(new File("/home/c/workspace/Globus/data/FailedPredictionsSrc").toPath(), failedTransactionsSrc);
		Files.write(new File("/home/c/workspace/Globus/data/FailedPredictionsDst").toPath(), failedTransactionsDst);
			
		ObjectOutputStream oOut
			=new ObjectOutputStream(new FileOutputStream(new File("/home/c/workspace/Globus/data/FailedPredictionsInds")));
		oOut.writeObject(failedTransactionInds);
		
		
		return new double[]{srcCorrect/srcAttempts, dstCorrect/dstAttempts};
	}
	
	static List<String[]> readDeletionLines()
	{
		List<String> lines=null;
		try
		{
			lines=Files.readAllLines(new File(currentDir+"/data/short-eps.csv").toPath());
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		List<String[]> splitData=new ArrayList<>();
		int amt=0;
		lines.remove(0);
		lines.remove(lines.size()-1);
		for(String line: lines)
		{
			splitData.add(line.split(","));
			amt++;
			if(amt%100000==0)
			{
				NumberFormat formatter = new DecimalFormat("#0.00");
				System.out.print(formatter.format(100*(double)amt/3300000.0)+"% ");
				if (amt>0)
				{
					//break;
				}
			}
		}
		return splitData;
	}
	
	public static void main(String[] args) throws IOException
	{
		double[] accuracies=testHeuristic();
		System.out.println("Correlation Heuristics Accuracy src: "+accuracies[0]+" dst: "+accuracies[1]);
	}

}
