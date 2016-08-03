package throughputPrediction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import userSrcDstPrediction.EPHeuristic;

public class NeuralNetworkDataGenerationNoHeuristics 
{
	
	static float[] scaleMaximumsInputs;
	static float[] scaleMaximumsOutputs;
	
	static int[] tfCols=new int[]{21,22,23,24,25,26, 40};
	static int[] intCols=new int[]{2, 6, 20, 27, 28, 29};
	
	static float dateDiv=3*1465833177756L;
	
	public static void generateThroughputTrainingData(List<String[]> transactions, int offset, 
			int numberExamples, String saveName, boolean train) throws ParseException
	{
		Hashtable<String, List<float[]>> inputsByEPPair=new Hashtable<>();
		Hashtable<String, List<float[]>> outputsByEPPair=new Hashtable<>();
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		long startDate=sdf.parse(transactions.get(offset)[3]).getTime();
		
		Hashtable<String, List<Double>> pastTransactionRate=new Hashtable<>();
		Hashtable<String, List<IDData<Long>>> currentTransactionsEPPair=new Hashtable<>();
		Hashtable<String, List<IDData<Long>>> currentTransactionsEP=new Hashtable<>();
		
		for(int transactionInd=0; transactionInd<offset; transactionInd++)
		{
			try 
			{
				String[] transaction=transactions.get(transactionInd);
				long startTime=sdf.parse(transaction[3]).getTime();
				long endTime=sdf.parse(transaction[13]).getTime();
				long amount=Long.parseLong(transaction[37]);
				String srcID=transaction[14];
				String dstID=transaction[15];
				
				if(!srcID.isEmpty() && !dstID.isEmpty())
				{
					if(endTime>startTime)
					{
						IDData<Long> txnInfo=new IDData<Long>("", endTime);
						txnInfo.doubleData=amount;
						txnInfo.longs=new long[]{startTime, endTime};
						
						if(currentTransactionsEPPair.get(srcID+" "+dstID)==null)
						{
							currentTransactionsEPPair.put(srcID+" "+dstID, new ArrayList<>());
						}
						int ind=Collections.binarySearch(currentTransactionsEPPair.get(srcID+" "+dstID), txnInfo);
						if(ind<0)
						{
							ind=-(ind+1);
						}
						if(ind<currentTransactionsEPPair.get(srcID+" "+dstID).size())
						{
							currentTransactionsEPPair.get(srcID+" "+dstID).add(ind, txnInfo);
						}
						else
						{
							currentTransactionsEPPair.get(srcID+" "+dstID).add(txnInfo);
						}
						
						
						if(currentTransactionsEP.get(srcID)==null)
						{
							currentTransactionsEP.put(srcID, new ArrayList<>());
						}
						currentTransactionsEP.get(srcID).add(txnInfo);
						if(currentTransactionsEP.get(dstID)==null)
						{
							currentTransactionsEP.put(dstID, new ArrayList<>());
						}
						currentTransactionsEP.get(dstID).add(txnInfo);
					}
					else
					{
						double transferRate=1000*amount/(endTime-startTime);
						if(pastTransactionRate.get(srcID+" "+dstID)==null)
						{
							pastTransactionRate.put(srcID+" "+dstID, new ArrayList<>());
						}
						pastTransactionRate.get(srcID+" "+dstID).add(transferRate);
					}
				}
			}
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
		}
		
		for(int transactionInd=offset; transactionInd<offset+numberExamples; transactionInd++)
		{
			String[] transaction=transactions.get(transactionInd);
			long startTime=sdf.parse(transaction[3]).getTime();
			long endTime=sdf.parse(transaction[13]).getTime();
			long amount=Long.parseLong(transaction[37]);
			String srcID=transaction[14];
			String dstID=transaction[15];
			
			if(!srcID.isEmpty() && !dstID.isEmpty() && endTime>startTime)
			{
				float txnsBT=0.0f;
				if(currentTransactionsEPPair.get(srcID+" "+dstID)!=null)
				{
					for(int txnInfoInd=0; 
							txnInfoInd<currentTransactionsEPPair.get(srcID+" "+dstID).size(); 
							txnInfoInd++)
					{
						if((long)currentTransactionsEPPair.get(srcID+" "+dstID).get(txnInfoInd).data<startTime)
						{
							IDData<Long> removed=currentTransactionsEPPair.get(srcID+" "+dstID).remove(txnInfoInd);
							if(pastTransactionRate.get(srcID+" "+dstID)==null)
							{
								pastTransactionRate.put(srcID+" "+dstID, new ArrayList<>());
							}
							pastTransactionRate.get(srcID+" "+dstID).add(1000*removed.doubleData/(removed.longs[1]-removed.longs[0]));
						}
						else
						{
							txnsBT+=currentTransactionsEPPair.get(srcID+" "+dstID).get(txnInfoInd).doubleData;
						}
					}
				}
				
				float txnsSrc=0.0f;
				if(currentTransactionsEP.get(srcID)!=null)
				{
					for(int txnInfoInd=0; 
							txnInfoInd<currentTransactionsEP.get(srcID).size(); 
							txnInfoInd++)
					{
						if((long)currentTransactionsEP.get(srcID).get(txnInfoInd).data<startTime)
						{
							IDData<Long> removed=currentTransactionsEP.get(srcID).remove(txnInfoInd);
						}
						else
						{
							txnsSrc+=currentTransactionsEP.get(srcID).get(txnInfoInd).doubleData;
						}
					}
				}
				
				float txnsDst=0.0f;
				if(currentTransactionsEP.get(dstID)!=null)
				{
					for(int txnInfoInd=0; 
							txnInfoInd<currentTransactionsEP.get(dstID).size(); 
							txnInfoInd++)
					{
						if((long)currentTransactionsEP.get(dstID).get(txnInfoInd).data<startTime)
						{
							IDData<Long> removed=currentTransactionsEP.get(dstID).remove(txnInfoInd);
						}
						else
						{
							txnsDst+=currentTransactionsEP.get(dstID).get(txnInfoInd).doubleData;
						}
					}
				}
				
				float prevRate=0.0f;
				if(pastTransactionRate.get(srcID+" "+dstID)!=null)
				{
					prevRate=(float)(double)pastTransactionRate.get(srcID+" "+dstID).get(pastTransactionRate.get(srcID+" "+dstID).size()-1);
				}
				
				float commandFloat=0.0f;
				String command=transaction[10];
				if(command.startsWith("transfer"))
				{
					commandFloat=0.25f;
				}
				else if(command.startsWith("transfer"))
				{
					commandFloat=0.5f;
				}
				else if(command.contains("go"))
				{
					commandFloat=0.75f;
				}
				else if(command.startsWith("API"))
				{
					commandFloat=1.0f;
				}
					
					
				int numberCustom=7;
				float[] input=new float[numberCustom+tfCols.length+intCols.length];
				input[0]=amount;
				input[1]=prevRate;
				input[2]=txnsBT;
				input[3]=txnsSrc;
				input[4]=txnsDst;
				input[5]=(float)startTime/dateDiv;
				input[6]=commandFloat;
				
				int ind=numberCustom;
				for(; ind<numberCustom+tfCols.length; ind++)
				{
					String tf=transaction[tfCols[ind-numberCustom]];
					if(tf.equals("t"))
					{
						input[ind]=1.0f;
					}
					else if(tf.equals("f"))
					{
						input[ind]=1.0f;
					}
					else
					{
						System.out.println("NN Data Gen: not t or f!");
					}
				}
				for(; ind<numberCustom+tfCols.length+intCols.length; ind++)
				{
					String val=transaction[intCols[ind-(numberCustom+tfCols.length)]];
					if(val.isEmpty())
					{
						val="0";
					}
					input[ind]=(float)(1.0/(Integer.parseInt(val)+1));
				}
				
				float[] output=new float[1];
				output[0]=(float)(1000*amount/(endTime-startTime));
				
				if(!Double.isFinite(output[0]))
				{
					int p=0;
					int c=0;
					p+=c;
				}
				
				if(inputsByEPPair.get(srcID+" "+dstID)==null)
				{
					inputsByEPPair.put(srcID+" "+dstID, new ArrayList<>());
				}
				inputsByEPPair.get(srcID+" "+dstID).add(input);
				if(outputsByEPPair.get(srcID+" "+dstID)==null)
				{
					outputsByEPPair.put(srcID+" "+dstID, new ArrayList<>());
				}
				outputsByEPPair.get(srcID+" "+dstID).add(output);
				
				IDData<Long> txnInfo=new IDData<Long>("", endTime);
				txnInfo.doubleData=amount;
				txnInfo.longs=new long[]{startTime, endTime};
				if(currentTransactionsEPPair.get(srcID+" "+dstID)==null)
				{
					currentTransactionsEPPair.put(srcID+" "+dstID, new ArrayList<>());
				}
				ind=Collections.binarySearch(currentTransactionsEPPair.get(srcID+" "+dstID), txnInfo);
				if(ind<0)
				{
					ind=-(ind+1);
				}
				if(ind<currentTransactionsEPPair.get(srcID+" "+dstID).size())
				{
					currentTransactionsEPPair.get(srcID+" "+dstID).add(ind, txnInfo);
				}
				else
				{
					currentTransactionsEPPair.get(srcID+" "+dstID).add(txnInfo);
				}
				if(currentTransactionsEP.get(srcID)==null)
				{
					currentTransactionsEP.put(srcID, new ArrayList<>());
				}
				currentTransactionsEP.get(srcID).add(txnInfo);
				if(currentTransactionsEP.get(dstID)==null)
				{
					currentTransactionsEP.put(dstID, new ArrayList<>());
				}
				currentTransactionsEP.get(dstID).add(txnInfo);
			}
		}
		
		normalize(inputsByEPPair, train, true);
		normalize(outputsByEPPair, train, false);
		
		BufferedWriter fDatasout=null;
		try 
		{
			fDatasout=new BufferedWriter(new FileWriter(new File("/home/willie/workspace/Globus/data/"+saveName)));
		} 
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		
		DecimalFormat df=new DecimalFormat("#0.000000000000000000"); 
		try
		{
			for(String userID: inputsByEPPair.keySet())
			{
				fDatasout.write("user "+userID+"\n");

				List<float[]> userInputs=inputsByEPPair.get(userID);
				fDatasout.write("in\n");
				for(float[] input: userInputs)
				{
					String inputString="";
					for(int inputInd=0; inputInd<input.length; inputInd++)
					{
						inputString+=(df.format(input[inputInd])+",");
					}
					inputString=inputString.substring(0, inputString.length()-1);
					fDatasout.write(inputString+"\n");
				}
				
				fDatasout.write("out\n");
				for(float[] input: outputsByEPPair.get(userID))
				{
					String inputString="";
					for(int inputInd=0; inputInd<input.length; inputInd++)
					{
						inputString+=(df.format(input[inputInd])+",");
					}
					inputString=inputString.substring(0, inputString.length()-1);
					fDatasout.write(inputString+"\n");
				}
			}
			fDatasout.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	static void normalize(Hashtable<String, List<float[]>> inputs, boolean train, boolean input)
	{
		float[] maximums=null;
		for(List<float[]> epPairInputs: inputs.values())
		{
			for(float[] epinput: epPairInputs)
			{
				for(int inputInd=0; inputInd<epinput.length; inputInd++)
				{
					if(maximums==null)
					{
						maximums=new float[epinput.length];
					}
					if(maximums[inputInd]<epinput[inputInd])
					{
						maximums[inputInd]=epinput[inputInd];
					}
				}
			}
		}
		
		if(train)
		{
			if(input)
			{
				scaleMaximumsInputs=maximums;
			}
			else
			{
				scaleMaximumsOutputs=maximums;
			}
		}
		else
		{
			if(input)
			{
				maximums=scaleMaximumsInputs;
			}
			else
			{
				maximums=scaleMaximumsOutputs;
			}
		}
		
		for(List<float[]> epPairInputs: inputs.values())
		{
			for(float[] epinput: epPairInputs)
			{
				for(int inputInd=0; inputInd<epinput.length; inputInd++)
				{
					epinput[inputInd]/=maximums[inputInd];
					
					if(!input && epinput[inputInd]==0)
					{
						int u=0;
					}
				}
			}
		}
	}
	
	public static void main(String[] args) throws ParseException
	{
		List<String[]> transactions=readLinesFullTransfers("full-trans-tab.csv");
		List<IDData<Long>> transactionsWithDates=new ArrayList<>();
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		for(String[] transaction: transactions)
		{
			try 
			{
				long amount=Long.parseLong(transaction[37]);
				if(amount>500000)
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
		
		generateThroughputTrainingData(transactions, 0, 7*transactions.size()/8, "throughputDataTrainSendInfo7.8", true);
		generateThroughputTrainingData(transactions, 7*transactions.size()/8, transactions.size()/8, "throughputDataValidateSendInfo1.8", false);
		
		System.out.println("max transfer rate: "+scaleMaximumsOutputs[0]);
	}
	
	static String currentDir=System.getProperty("user.dir");
	
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
