package throughputPrediction;

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
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import init.IDData;

public class TestEnsemble
{
	
	public static void main(String[] args) throws IOException
	{
		double[] accuracies=testHistCorrEnsembleHeuristics();
		System.out.println("Correlation and Hist Heuristics Accuracy src: "+accuracies[0]+" dst: "+accuracies[1]+" total: "+accuracies[2]);
	}
	
	static String currentDir=System.getProperty("user.dir");
	static double[] srcSuccSinceStep;
	static double[] dstSucSincStep;
	static Hashtable<String, double[]> userAccuracy=new Hashtable<>();
	
	public static double[] testHistCorrEnsembleHeuristics() throws IOException
	{
		List<String[]> transactions=readLinesFullTransfers("full-trans-tab.csv");
		List<String[]> deletions=new ArrayList<>();//readLines("short-eps.csv");
		List<String[]> users=new ArrayList<>();//readLines("users.csv");
		List<String[]> authUsers=new ArrayList<>();//readLines("auth_users.csv");
		
		List<IDData<Long>> transactionsWithDates=new ArrayList<>();
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		for(String[] transaction: transactions)
		{
			try 
			{
				long amount=Long.parseLong(transaction[37]);
				if(amount>1000000)
				{
					long currentDate=sdf.parse(transaction[3]).getTime();
					transactionsWithDates.add(new IDData<Long>(""+currentDate, currentDate));
					transactionsWithDates.get(transactionsWithDates.size()-1).line=transaction;
				}
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
		}
		transactions.clear();
		Collections.sort(transactionsWithDates);
		for(IDData<Long> transactionWithDate: transactionsWithDates)
		{
			transactions.add(transactionWithDate.line);
		}
		
		
		
		srcSuccSinceStep=new double[transactions.size()];
		dstSucSincStep=new double[transactions.size()];
		
		Average avg=new Average(transactions, deletions, users, authUsers, 0, Long.MAX_VALUE);
		Average avg1D=new Average(transactions, deletions, users, authUsers, 0, 86400000L);
		Median med=new Median(transactions, deletions, users, authUsers, 0, Long.MAX_VALUE);
		Median med1D=new Median(transactions, deletions, users, authUsers, 0, 86400000L);
		LastSeen lastSeen=new LastSeen(transactions, deletions, users, authUsers, 0);
		ARIMA arima=new ARIMA(transactions, deletions, users, authUsers, 0, 5, 10);
		RecursiveLeastSquares rLeastSquares=new RecursiveLeastSquares(transactions, deletions, users, authUsers, 0);
		KalmanFiltering kalmanFiltering=new KalmanFiltering(transactions, deletions, users, authUsers, 0);
		PolynomialRegression linear=new PolynomialRegression(transactions, deletions, users, authUsers, 0, 1, 5, 10);
		PolynomialRegression quadratic=new PolynomialRegression(transactions, deletions, users, authUsers, 0, 2, 5, 10);
		PolynomialRegression cubic=new PolynomialRegression(transactions, deletions, users, authUsers, 0, 3, 5, 10);
		PolynomialRegression quartic=new PolynomialRegression(transactions, deletions, users, authUsers, 0, 4, 5, 10);
		CDFMatching cdfMatching=new CDFMatching(transactions, deletions, users, authUsers, 0, 5, 10);
		PolynomialRegressionTxnAmount linearAmount=new PolynomialRegressionTxnAmount(transactions, deletions, users, authUsers, 0, 1, 5, 10);
		PolynomialRegressionTxnAmount quadraticAmount=new PolynomialRegressionTxnAmount(transactions, deletions, users, authUsers, 0, 2, 5, 10);
		AllMedian allMedian=new AllMedian(transactions, deletions, users, authUsers, 0);
		InstitutionMedian institutionMedian=new InstitutionMedian(transactions, deletions, users, authUsers, 0);
		UserMedian userMedian=new UserMedian(transactions, deletions, users, authUsers, 0);
		TPHeuristic[] heuristics=new TPHeuristic[]{avg, avg1D, med, med1D, lastSeen, arima, rLeastSquares,
				kalmanFiltering, linear, quadratic, cdfMatching, linearAmount, quadraticAmount,
				allMedian, institutionMedian, userMedian};
		
		double[] errorRelative=new double[heuristics.length+1];
		
		double[] errorAbsolute=new double[heuristics.length+1];
		Hashtable<String, Double>[] heurEPErrors=new Hashtable[heuristics.length+1];
		Hashtable<Integer, Double>[] historySizeErrors=new Hashtable[heuristics.length+1];
		Hashtable<Integer, Integer> historySizeCounts=new Hashtable<>();
		for(int huaInd=0; huaInd<heurEPErrors.length; huaInd++)
		{
			heurEPErrors[huaInd]=new Hashtable<String, Double>();
			historySizeErrors[huaInd]=new Hashtable<Integer, Double>();
		}
		
		Hashtable<String, Hashtable<String, Integer>> numberOfTransactions=new Hashtable<>();
		
		Hashtable<String, Hashtable<String,String>> epPairs=new Hashtable<>();
		
		double[] attempts=new double[heuristics.length+1];
		long srcRecTimeTotal=0;
		long dstRecTimeTotal=0;
		
		boolean datePrinted=false;
		int completedInd=0;
		
		for(int ind=0; ind<transactions.size(); ind++)
		{
			String[] line=transactions.get(ind);
			if(line.length<15)
			{
				continue;
			}
			long currentDate;
			try 
			{
				currentDate=sdf.parse(line[3]).getTime();
				String userID=line[2];
				String srcID=line[14];
				String dstID=line[15];
				long startTime=sdf.parse(line[3]).getTime();
				long endTime=sdf.parse(line[13]).getTime();
				long amount=Long.parseLong(line[37]);
				float actualThroughput=(float)1000*amount/(endTime-startTime);
				
				String[] nextTxn=transactions.get(completedInd);
				long nxtTxnDate=0;
				while(nxtTxnDate<startTime)
				{
					if(completedInd%1000==0)
					{
						System.out.println(ind);
					}
					try
					{
						nxtTxnDate=sdf.parse(nextTxn[13]).getTime();
						if(nxtTxnDate<startTime)
						{
							for(TPHeuristic heuristic: heuristics)
							{
								heuristic.nextTxn(nextTxn);
							}
						}
						else
						{
							break;
						}
					} 
					catch (ParseException e) 
					{
						e.printStackTrace();
					}	
					completedInd++;
					nextTxn=transactions.get(completedInd);
				}

				if(ind>7*transactions.size()/8)
				{
					if(epPairs.get(srcID)==null)
					{
						epPairs.put(srcID, new Hashtable<>());
					}
					epPairs.get(srcID).put(dstID, dstID);
					
					if(endTime<startTime)
					{
						String linePrint="";
						for(String linePart: line)
						{
							linePrint+=linePart+", ";
						}
						Date startDate=sdf.parse(line[3]);
						Date endDate=sdf.parse(line[13]);
						System.out.println(linePrint);
					}
					
					if(!srcID.isEmpty() && ! dstID.isEmpty() && amount>0)
					{	
						if(numberOfTransactions.get(srcID)==null)
						{
							numberOfTransactions.put(srcID, new Hashtable<>());
						}
						if(numberOfTransactions.get(srcID).get(dstID)==null)
						{
							numberOfTransactions.get(srcID).put(dstID, 0);
						}
						int histSize=numberOfTransactions.get(srcID).get(dstID);
						numberOfTransactions.get(srcID).put(dstID, histSize+1);
						if(historySizeCounts.get(histSize)==null)
						{
							historySizeCounts.put(histSize, 0);
						}
						historySizeCounts.put(histSize, historySizeCounts.get(histSize)+1);
						
						double minRecErrorAbsolute=Double.MAX_VALUE;
						double minRecErrorRelative=Double.MAX_VALUE;
						for(int hInd=0; hInd<heuristics.length; hInd++)
						{
							TPHeuristic heuristic=heuristics[hInd];
							float recWeights=heuristic.getThrougputEstimationWeight(srcID, dstID, amount);
//							if(recWeights>-1.0f)
//							{
								float tpEst=heuristic.getThrougputEstimation(srcID, dstID, currentDate, amount);
								double recErrorAbsolute=Math.abs(tpEst-actualThroughput);
								errorAbsolute[hInd]+=recErrorAbsolute;
								if(historySizeErrors[hInd].get(histSize)==null)
								{
									historySizeErrors[hInd].put(histSize, 0.0);
								}
								historySizeErrors[hInd].put(histSize, historySizeErrors[hInd].get(histSize)+recErrorAbsolute);
								if(minRecErrorAbsolute>recErrorAbsolute)
								{
									minRecErrorAbsolute=recErrorAbsolute;
								}
								
								double recErrorRelative=100*Math.abs(tpEst-actualThroughput)/actualThroughput;
								errorRelative[hInd]+=recErrorRelative;
								if(minRecErrorRelative>recErrorRelative)
								{
									minRecErrorRelative=recErrorRelative;
								}
								
								attempts[hInd]++;
//							}
						}
						if(minRecErrorAbsolute<Double.MAX_VALUE)
						{
							errorAbsolute[heuristics.length]+=minRecErrorAbsolute;
							if(historySizeErrors[heuristics.length].get(histSize)==null)
							{
								historySizeErrors[heuristics.length].put(histSize, 0.0);
							}
							historySizeErrors[heuristics.length].put(histSize, historySizeErrors[heuristics.length].get(histSize)+minRecErrorAbsolute);
							
							errorRelative[heuristics.length]+=minRecErrorRelative;
							
							attempts[heuristics.length]++;
						}
					}
				}
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}	
		}
		
		long numberUniquePairs=0;
		for(Hashtable<String, String> dsts: epPairs.values())
		{
			numberUniquePairs+=dsts.size();
		}
		System.out.println("Unique EP Pairs:"+numberUniquePairs);
		
		String sizeAccuracyMathe="{";
		for(Hashtable<Integer, Double> historySizeAccuraciesHist: historySizeErrors)
		{
			List<IDData<Integer>> accuracies=new ArrayList<>();
			double totalSuccesses=0.0;
			double totalAttempts=0.0;
			for(Integer histSize: historySizeAccuraciesHist.keySet())
			{
				double totalError=historySizeAccuraciesHist.get(histSize);
				int histSizeFreq=historySizeCounts.get(histSize);
				if(histSizeFreq>100)
				{
					accuracies.add(new IDData<Integer>(""+histSize, histSize));
					accuracies.get(accuracies.size()-1).doubleData=totalError/histSizeFreq;
				}
			}
			Collections.sort(accuracies);
			
			DecimalFormat df=new DecimalFormat("#0.000000"); 
			int largest=0;
			double remaining=totalAttempts+.00001;
			String accVHistSize="{";
			accVHistSize+="{";
			for(IDData<Integer> histSize: accuracies)
			{
				accVHistSize+="{"+(int)histSize.data+","+df.format(histSize.doubleData)+"},";
				if((int)histSize.data>largest)
				{
					largest=(int)histSize.data;
				}
			}
			sizeAccuracyMathe+=accVHistSize.substring(0, accVHistSize.length()-1)+"}";
			accVHistSize+=",{{0,"+df.format(totalSuccesses/totalAttempts)+"},{"+largest+","+df.format(totalSuccesses/totalAttempts)+"}}";
		}
		System.out.println(sizeAccuracyMathe);
		
		for(int accInd=0; accInd<errorAbsolute.length; accInd++)
		{
			System.out.println("acc abs: "+(errorAbsolute[accInd]/attempts[accInd]));
		}
		
		for(int accInd=0; accInd<errorAbsolute.length; accInd++)
		{
			System.out.println("acc rel: "+(errorRelative[accInd]/attempts[accInd]));
		}
			
		//System.out.println("avg dst time: "+(dstRecTimeTotal/dstAttempts));
		
		//Files.write(new File("/home/c/workspace/Globus/data/FailedPredictionsSrc").toPath(), failedTransactionsSrc);
		//Files.write(new File("/home/c/workspace/Globus/data/FailedPredictionsDst").toPath(), failedTransactionsDst);
			
//		ObjectOutputStream oOut
//			=new ObjectOutputStream(new FileOutputStream(new File("/home/c/workspace/Globus/data/FailedPredictionsInds")));
//		oOut.writeObject(failedTransactionInds);
		
		
		
//		double culmulativeSucc=0.0;
//		double culmulativeSuccSrc=0.0;
//		String matheOut="";
//		for(int i=dstSucSincStep.length-1; i>=0; i--)
//		{
//			culmulativeSucc+=dstSucSincStep[i];
//			if(i%10000==0 && i>=10000 && (dstSucSincStep.length-1-i)!=0)
//			{
//				matheOut+="{"+i+","+(culmulativeSucc/(dstSucSincStep.length-1-i))+"},";
//			}	
//		}
//		String matheOut2="";
//		for(int i=srcSuccSinceStep.length-1; i>=0; i--)
//		{
//			culmulativeSuccSrc+=srcSuccSinceStep[i];
//			if(i%10000==0 && i>=10000 && (srcSuccSinceStep.length-1-i)!=0)
//			{
//				matheOut2+="{"+i+","+(culmulativeSuccSrc/(srcSuccSinceStep.length-1-i))+"},";
//			}
//		}
//		matheOut2=matheOut2.substring(0, matheOut2.length()-2)+"";
//		System.out.println("{"+matheOut+","+matheOut2+"}");
		
		return null;
	}
		
	static List<String[]> readLines(String csvFileName)
	{
		List<String> lines=null;
		try
		{
			lines = Files.readAllLines(new File(currentDir+"/data/"+csvFileName).toPath());
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
			if(amt%500000==0)
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
	
	static List<String[]> readLinesFullTransfers(String csvFileName)
	{
		List<String> lines=null;
		try
		{
			lines = Files.readAllLines(new File("/home/willie/workspace/Globus/data/"+csvFileName).toPath());
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
			String[] lineParts=line.split("\t");
			if(lineParts.length>37)
			{
				int lastDotIndStart=lineParts[3].lastIndexOf('.');
				int lastDotIndEnd=lineParts[13].lastIndexOf('.');
				if(lastDotIndStart>-1 && lineParts[3].length()>=lastDotIndStart+4
						&& lastDotIndEnd>-1 && lineParts[13].length()>=lastDotIndEnd+4)
				{
					lineParts[3]=lineParts[3].substring(0, lastDotIndStart+4);
					lineParts[13]=lineParts[13].substring(0, lastDotIndEnd+4);
//					if(!lineParts[3].equals(lineParts[13]))
//					{
						splitData.add(lineParts);
						amt++;
						if(amt%500000==0)
						{
							NumberFormat formatter=new DecimalFormat("#0.00");
							System.out.print(formatter.format(100*(double)amt/3300000.0)+"% ");
							if (amt>0)
							{
								//break;
							}
						}
//					}
				}
			}
		}
		return splitData;
	}
	
}
