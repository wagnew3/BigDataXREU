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

import init.IDData;

public class TestEnsembleHeuristic 
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
		List<String[]> deletions=readLines("short-eps.csv");
		List<String[]> users=readLines("users.csv");
		List<String[]> authUsers=readLines("auth_users.csv");
		
		List<IDData<Long>> transactionsWithDates=new ArrayList<>();
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		for(String[] transaction: transactions)
		{
			try 
			{
				long currentDate=sdf.parse(transaction[3]).getTime();
				transactionsWithDates.add(new IDData<Long>(""+currentDate, currentDate));
				transactionsWithDates.get(transactionsWithDates.size()-1).line=transaction;
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
		
		Hashtable<String, Integer> numberOfTransactions=new Hashtable<>();
		
		srcSuccSinceStep=new double[transactions.size()];
		dstSucSincStep=new double[transactions.size()];
		
		int topN=1;
		MostUniqueUsersHeuristic mUUserH=new MostUniqueUsersHeuristic(transactions, deletions, users, authUsers, 0, topN);
		InstitutionHeuristic insH=new InstitutionHeuristic(transactions, deletions, users, authUsers, 0, topN);
		UserEPsHeuristic uEPH=new UserEPsHeuristic(transactions, deletions, users, authUsers, 0);
		CorrelationHeuristic corrH=new CorrelationHeuristic(transactions, deletions, users, authUsers, 0);
		HistoryHeuristic histH=new HistoryHeuristic(transactions, deletions, users, authUsers, 0);
		//RandomWalkHeuristic randWalkH=new RandomWalkHeuristic(transactions, deletions, users, authUsers, 0);
		//JaccardHeuristic jaccH=new JaccardHeuristic(transactions, deletions, users, authUsers, 0);
		
		EPHeuristic[] heuristics=new EPHeuristic[]{mUUserH, insH, uEPH, corrH, histH};
		
		Hashtable<String, double[]>[] heurUsrAccuracies=new Hashtable[heuristics.length+1];
		Hashtable<Integer, double[]>[] historySizeAccuracies=new Hashtable[heuristics.length+1];
		for(int huaInd=0; huaInd<heurUsrAccuracies.length; huaInd++)
		{
			heurUsrAccuracies[huaInd]=new Hashtable<String, double[]>();
			historySizeAccuracies[huaInd]=new Hashtable<Integer, double[]>();
		}
		
		double[] correct=new double[heuristics.length+1];
		double[] attempts=new double[heuristics.length+1];
		
		List<String> failedTransactionsSrc=new ArrayList<>();
		List<String> failedTransactionsDst=new ArrayList<>();
		List<Integer> failedTransactionInds=new ArrayList<>();

		long srcRecTimeTotal=0;
		long dstRecTimeTotal=0;
		
		boolean datePrinted=false;
		
		for(int ind=0; ind<transactions.size(); ind++)
		{
			String[] line=transactions.get(ind);
			if(line.length<15)
			{
				continue;
			}
			String userID=line[2];
			String srcID=line[14];
			String dstID=line[15];
			boolean added=false;
			
			long currentDate;
			try 
			{
				currentDate=sdf.parse(line[3]).getTime();
				int histSize=numberOfTransactions.getOrDefault(userID, 0);
				if(numberOfTransactions.get(userID)==null)
				{
					numberOfTransactions.put(userID, 0);
				}
				numberOfTransactions.put(userID, numberOfTransactions.get(userID)+1);
				
				if(ind>500000)
				{
					if(!srcID.isEmpty())
					{
						List<String> hRecs=null;
						for(int hInd=0; hInd<heuristics.length; hInd++)
						{
							EPHeuristic heuristic=heuristics[hInd];
							List<String> recs=new ArrayList<>();
							for(int recInd=0; recInd<topN; recInd++)
							{
								recs.add((String)(heuristic.getNthBestSrc(userID, recInd, currentDate)[0]));
							}
							
							if(hInd==4)
							{
								hRecs=recs;
							}
							
							if(recs.contains(srcID))
							{
								if(histSize==0 && hInd==1)
								{
									int u=0;
								}
								correct[hInd]++;	
								if(heurUsrAccuracies[hInd].get(userID)==null)
								{
									heurUsrAccuracies[hInd].put(userID, new double[2]);
								}
								heurUsrAccuracies[hInd].get(userID)[0]++;
								
								if(historySizeAccuracies[hInd].get(histSize)==null)
								{
									historySizeAccuracies[hInd].put(histSize, new double[2]);
								}
								historySizeAccuracies[hInd].get(histSize)[0]++;
							}
							attempts[hInd]++;
							if(heurUsrAccuracies[hInd].get(userID)==null)
							{
								heurUsrAccuracies[hInd].put(userID, new double[2]);
							}
							heurUsrAccuracies[hInd].get(userID)[1]++;
							
							if(historySizeAccuracies[hInd].get(histSize)==null)
							{
								historySizeAccuracies[hInd].put(histSize, new double[2]);
							}
							historySizeAccuracies[hInd].get(histSize)[1]++;
							

						}
						
						List<String> srcRecs=new ArrayList<>();
						for(int hInd=0; hInd<heuristics.length; hInd++)
						{
							EPHeuristic heuristic=heuristics[hInd];
							int numberAdded=0;
							int addAttempts=0;
							String previous="p";
							String current=(String)(heuristic.getNthBestSrc(userID, addAttempts, currentDate)[0]);
							while(numberAdded<topN && addAttempts<10)
							{
								if(!srcRecs.contains(current))
								{
									srcRecs.add(current);
									numberAdded++;
								}
								else if(!srcRecs.contains(srcID) && srcID.equals(current))
								{
									int q=0;
								}
								previous=current;
								current=(String)(heuristic.getNthBestSrc(userID, addAttempts, currentDate)[0]);
								addAttempts++;
							}
						}
						
						if(!srcRecs.contains(srcID) && hRecs.contains(srcID))
						{
							int u=0;
						}
						
						if(srcRecs.contains(srcID))
						{
							correct[correct.length-1]++;	
							if(heurUsrAccuracies[heurUsrAccuracies.length-1].get(userID)==null)
							{
								heurUsrAccuracies[heurUsrAccuracies.length-1].put(userID, new double[2]);
							}
							heurUsrAccuracies[heurUsrAccuracies.length-1].get(userID)[0]++;
						}
						else if(hRecs.contains(srcID))
						{
							int l=0;
						}
						if(heurUsrAccuracies[heurUsrAccuracies.length-1].get(userID)==null)
						{
							heurUsrAccuracies[heurUsrAccuracies.length-1].put(userID, new double[2]);
						}
						heurUsrAccuracies[heurUsrAccuracies.length-1].get(userID)[1]++;
						attempts[attempts.length-1]++;
						if(srcRecs.contains(srcID))
						{
							if(historySizeAccuracies[historySizeAccuracies.length-1].get(histSize)==null)
							{
								historySizeAccuracies[historySizeAccuracies.length-1].put(histSize, new double[2]);
							}
							historySizeAccuracies[historySizeAccuracies.length-1].get(histSize)[0]++;
						}
						if(historySizeAccuracies[historySizeAccuracies.length-1].get(histSize)==null)
						{
							historySizeAccuracies[historySizeAccuracies.length-1].put(histSize, new double[2]);
						}
						historySizeAccuracies[historySizeAccuracies.length-1].get(histSize)[1]++;
					}
					
					if(!dstID.isEmpty())
					{
						long dstTime=System.nanoTime();
						List<String> hRecs=null;
						for(int hInd=0; hInd<heuristics.length; hInd++)
						{
							EPHeuristic heuristic=heuristics[hInd];
							List<String> recs=new ArrayList<>();
							for(int recInd=0; recInd<topN; recInd++)
							{
								recs.add((String)(heuristic.getNthBestDst(userID, recInd, currentDate)[0]));
							}
							
							if(hInd==4)
							{
								hRecs=recs;
							}

							if(recs.contains(dstID))
							{
								if(histSize==0 && hInd==1)
								{
									int u=0;
								}
								correct[hInd]++;
								if(heurUsrAccuracies[hInd].get(userID)==null)
								{
									heurUsrAccuracies[hInd].put(userID, new double[2]);
								}
								heurUsrAccuracies[hInd].get(userID)[0]++;
							}
							attempts[hInd]++;
							if(heurUsrAccuracies[hInd].get(userID)==null)
							{
								heurUsrAccuracies[hInd].put(userID, new double[2]);
							}
							heurUsrAccuracies[hInd].get(userID)[1]++;
						}
						
						List<String> dstRecs=new ArrayList<>();
						List<String> dHRecs=new ArrayList<>();
						for(int hInd=0; hInd<heuristics.length; hInd++)
						{
							EPHeuristic heuristic=heuristics[hInd];
							int numberAdded=0;
							int addAttempts=0;
							String previous="p";
							String current=(String)(heuristic.getNthBestDst(userID, addAttempts, currentDate)[0]);
							while(numberAdded<topN && addAttempts<10)
							{
								if(hInd==4)
								{
									dHRecs.add(current);
								}
								if(!dstRecs.contains(current))
								{
									dstRecs.add(current);
									numberAdded++;
								}
								else if(dstID.equals(current))
								{
									int q=0;
								}
								previous=current;
								current=(String)(heuristic.getNthBestDst(userID, addAttempts, currentDate)[0]);
								addAttempts++;
							}
						}
						
						if(!dstRecs.contains(dstID) && hRecs.contains(dstID))
						{
							int u=0;
						}
						
						dstTime=System.nanoTime()-dstTime;
						dstRecTimeTotal+=dstTime;
						if(dstRecs.contains(dstID))
						{
							correct[correct.length-1]++;
							if(heurUsrAccuracies[heurUsrAccuracies.length-1].get(userID)==null)
							{
								heurUsrAccuracies[heurUsrAccuracies.length-1].put(userID, new double[2]);
							}
							heurUsrAccuracies[heurUsrAccuracies.length-1].get(userID)[0]++;
						}
						else if(hRecs.contains(dstID))
						{
							int e=0;
						}
						if(heurUsrAccuracies[heurUsrAccuracies.length-1].get(userID)==null)
						{
							heurUsrAccuracies[heurUsrAccuracies.length-1].put(userID, new double[2]);
						}
						heurUsrAccuracies[heurUsrAccuracies.length-1].get(userID)[1]++;
						attempts[attempts.length-1]++;
						
						if(dstRecs.contains(dstID))
						{
							if(historySizeAccuracies[historySizeAccuracies.length-1].get(histSize)==null)
							{
								historySizeAccuracies[historySizeAccuracies.length-1].put(histSize, new double[2]);
							}
							historySizeAccuracies[historySizeAccuracies.length-1].get(histSize)[0]++;
						}
						if(historySizeAccuracies[historySizeAccuracies.length-1].get(histSize)==null)
						{
							historySizeAccuracies[historySizeAccuracies.length-1].put(histSize, new double[2]);
						}
						historySizeAccuracies[historySizeAccuracies.length-1].get(histSize)[1]++;
					}
				}
				
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			
			for(EPHeuristic heuristic: heuristics)
			{
				heuristic.nextTxn();
			}
		}
		
		String sizeAccuracyMathe="{";
		for(Hashtable<Integer, double[]> historySizeAccuraciesHist: historySizeAccuracies)
		{
			List<IDData<Integer>> accuracies=new ArrayList<>();
			double totalSuccesses=0.0;
			double totalAttempts=0.0;
			for(Integer histSize: historySizeAccuraciesHist.keySet())
			{
				double[] succPair=historySizeAccuraciesHist.get(histSize);
				totalSuccesses+=succPair[0];
				totalAttempts+=succPair[1];
				if(succPair[1]>100)
				{
					accuracies.add(new IDData<Integer>(""+histSize, histSize));
					accuracies.get(accuracies.size()-1).doubles=succPair;
				}
			}
			Collections.sort(accuracies);
			
			DecimalFormat df=new DecimalFormat("#0.000000"); 
			int largest=0;
			String fracRemaining="{";
			double remaining=totalAttempts+.00001;
			String accVHistSize="{";
			accVHistSize+="{";
			for(IDData<Integer> histSize: accuracies)
			{
				double[] succPair=historySizeAccuraciesHist.get(histSize.data);
				fracRemaining+="{"+(int)histSize.data+","+df.format(remaining/totalAttempts)+"},";
				accVHistSize+="{"+(int)histSize.data+","+df.format(100*succPair[0]/succPair[1])+"},";
				if((int)histSize.data>largest)
				{
					largest=(int)histSize.data;
				}
				remaining-=succPair[1];
			}
			sizeAccuracyMathe+=accVHistSize.substring(0, accVHistSize.length()-1)+"}";
			fracRemaining=fracRemaining.substring(0, fracRemaining.length()-1)+"}";
			accVHistSize+="},"+fracRemaining;
			accVHistSize+=",{{0,"+df.format(totalSuccesses/totalAttempts)+"},{"+largest+","+df.format(totalSuccesses/totalAttempts)+"}}";
			
		}
		System.out.println();
		System.out.println(sizeAccuracyMathe);
		
		for(int accInd=0; accInd<correct.length; accInd++)
		{
			System.out.println("acc: "+(correct[accInd]/attempts[accInd]));
		}
		
		for(int accInd=0; accInd<correct.length; accInd++)
		{
			double totalUserAccuracy=0.0;
			for(double[] accuracy: heurUsrAccuracies[accInd].values())
			{
				totalUserAccuracy+=accuracy[0]/accuracy[1];
			}
			totalUserAccuracy/=heurUsrAccuracies[accInd].size();
			System.out.println("User accuracy: "+totalUserAccuracy);
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
			String[] lineParts=line.split("\t");
			if(lineParts.length>10 && lineParts[10].matches("API\\s[0-9\\.]+\\s\\w+"))
			{
				splitData.add(lineParts);
			}
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
	
}
