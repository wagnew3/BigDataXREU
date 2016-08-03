package init;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import com.sun.prism.paint.Color;

public class Analysis 
{
	
	static String currentDir=System.getProperty("user.dir");
	static String[] header;
	
	public static void main(String[] args) throws IOException
	{
		List<String[]> splitData=readLinesFullTransfers(currentDir+"/data/full-trans-tab.csv");
		
		averageThroughput(splitData);
		
//		sanityCheckHistAcc(splitData);
//		
		List<String[]> short_eps=readShortEps();
//		Hashtable<String, String> machineIDToUserID=machineIDToUserID(short_eps);
//		
//		getEPActiveUserStats(splitData, short_eps);
		
		Object[] amtHeatResult=getAmountHeat(splitData);
		
		generateHeatMapOfTopN((List<String>)amtHeatResult[0], (Hashtable<String, Hashtable<String, Double>>)amtHeatResult[1]);
		
		
		/*
		Hashtable<String, IDData<Double>>[] usagesInAcc=transactionsInAccount(splitData, machineIDToUserID);
		greatestPercentEpsSingleOwner(splitData, machineIDToUserID, usagesInAcc[0],
				usagesInAcc[1], usagesInAcc[2]);
		*/
		
		//transactionsOnOwnedEps(splitData, machineIDToUserID);
		
		//userIdsInTxns(splitData, short_eps);
		
		//transactionsByUser(splitData, lines, "10253");
		
		//sortUsedEps(splitData);
		
		//unqiueUsersPerSource(splitData);
		
		/*
		List<String> uIDLines=Files.readAllLines(new File("C:\\Users\\C\\workspace\\Globus\\data\\users.csv").toPath());
		List<String[]> uIDSplitData=new ArrayList<>();
		amt=0;
		uIDLines.remove(0);
		uIDLines.remove(uIDLines.size()-1);
		for(String line: uIDLines)
		{
			uIDSplitData.add(line.split(","));
			amt++;
			if(amt%1000000==0)
			{
				System.out.println(amt);
				if (amt>0)
				{
					//break;
				}
			}
		}
		*/
		numberPointsPerUser(splitData);
		//numberPointsPerUserBins(splitData);
		//numberPointsPerUserSrcDstBoth(splitData);
		
		List<IDData<Long>> amtTransfered=amountTransfered(splitData);
		//Collections.sort(amtTransfered);
		List<IDData<Integer>> numberTransfers=numberTransfers(splitData);
		Collections.sort(numberTransfers);
		int u=0;
		
	
		
		
		//List<IDData<Double>> avgAmountTransfered=avgAmountTransfered(splitData);
		//Collections.sort(avgAmountTransfered);
		
//		List<IDData<Integer>> numberEndpoints=numberEndpoints(splitData);
//		Collections.sort(numberEndpoints);
		
		//List<IDData<Integer>> maxSameTransfers=maxSameTransfers(splitData);
		//Collections.sort(maxSameTransfers);
		
		
		//userUsagePerSource(splitData);
		
		/*
		List<IDData<Long>> sortedLines=sortByDate(splitData);
		
		Hashtable<Long, Long> earliestUserCreationDate=getEarliestUserCreationDate(splitData, uIDSplitData);
		
		userDataDataTransfer(splitData, earliestUserCreationDate);
		*/
		
		//saveDataAsMatrix(splitData);
		
		//List<IDData<Integer>> randomWalkFreqCentrality=randomWalkFreqCentrality(splitData);
		
		//randomWalkTransferCentrality(splitData);
		
		//testMathematicaGraphic(splitData);
		/*
		Hashtable<String, String> uIDToEmail=userIDToEmails(uIDSplitData);
		Object[] result=topNUsersSendEnpoints(splitData, 50);
		Hashtable<String, Hashtable<Integer, Long>> topNUsersSendEnpoints=(Hashtable<String, Hashtable<Integer, Long>>)result[0];
		Long totalSent=(Long)result[1];
		Hashtable<String, IDData<Long>> usageAmt=(Hashtable<String, IDData<Long>>)result[2];
		Hashtable<Integer, Long> endpointUsage=(Hashtable<Integer, Long>)result[3];
		//mathematicaFormatTopUsers(topNUsersSendEnpoints, usageAmt, endpointUsage, totalSent, uIDToEmail);
		mathematicaFormatTopUsersUsername(topNUsersSendEnpoints, usageAmt, endpointUsage, totalSent, uIDToEmail);
		*/
	}
	
	static Object[] getAmountHeat(List<String[]> splitData)
	{
		Hashtable<String, IDData<Long>> amountTransferred=new Hashtable<>();
		Hashtable<String, Hashtable<String, double[]>> epPairTransferRateInfo=new Hashtable<>();

		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		int ind=0;
		for(String[] line: splitData)
		{
			if(ind>7*splitData.size()/8)
			{
				try 
				{
					long startTime=sdf.parse(line[3]).getTime();
					long endTime=sdf.parse(line[13]).getTime();
					String srcID=line[14];
					String dstID=line[15];
					if(endTime-startTime>0)
					{
						long amount=Long.parseLong(line[37]);
						double rate=1000.0*amount/(endTime-startTime);
						
						if(amountTransferred.get(srcID)==null)
						{
							amountTransferred.put(srcID, new IDData<Long>(srcID, amount));
						}
						amountTransferred.put(srcID, new IDData<Long>(srcID, (long)amountTransferred.get(srcID).data+amount));
						if(amountTransferred.get(dstID)==null)
						{
							amountTransferred.put(dstID, new IDData<Long>(dstID, amount));
						}
						amountTransferred.put(dstID, new IDData<Long>(dstID, (long)amountTransferred.get(dstID).data+amount));
						
						if(epPairTransferRateInfo.get(srcID)==null)
						{
							epPairTransferRateInfo.put(srcID, new Hashtable<>());
						}
						if(epPairTransferRateInfo.get(srcID).get(dstID)==null)
						{
							epPairTransferRateInfo.get(srcID).put(dstID, new double[2]);
						}
						double[] rateData=epPairTransferRateInfo.get(srcID).get(dstID);
						rateData[0]+=rate;
						rateData[1]+=1;
						epPairTransferRateInfo.get(srcID).put(dstID, rateData);
					}
				}
				catch (ParseException e) 
				{
					e.printStackTrace();
				}
			}
			ind++;
		}
		
		List<IDData<Long>> epByAmount=new ArrayList<>(amountTransferred.values());
		Collections.sort(epByAmount);
		Collections.reverse(epByAmount);
		List<String> displayEps=new ArrayList<>();
		for(int dispNum=0; dispNum<100; dispNum++)
		{
			displayEps.add(epByAmount.get(dispNum).ID);
		}
		
		Hashtable<String, Hashtable<String, Double>> heatMap=new Hashtable<>();
		for(String srcEP: displayEps)
		{
			heatMap.put(srcEP, new Hashtable<>());
			if(epPairTransferRateInfo.get(srcEP)!=null)
			{
				for(String dstEP: displayEps)
				{
					if(epPairTransferRateInfo.get(srcEP).get(dstEP)!=null)
					{
						double[] rateInfo=epPairTransferRateInfo.get(srcEP).get(dstEP);
						double avgRate=rateInfo[0]/rateInfo[1];
						heatMap.get(srcEP).put(dstEP, avgRate);
					}	
				}
			}
		}
		return new Object[]{displayEps, heatMap};
	}
	
	static void generateHeatMapOfTopN(List<String> entities, Hashtable<String, Hashtable<String, Double>> heat)
	{
		NumberFormat formatter=new DecimalFormat("#0.0");
		String matheHeatMapData="{";
		Hashtable<String, Integer> positions=new Hashtable<>();
		int newPosition=0;
		for(String entity: entities)
		{
			if(positions.get(entity)==null)
			{
				positions.put(entity, newPosition);
				newPosition++;
			}
			for(String txnEntity: heat.get(entity).keySet())
			{
				if(positions.get(txnEntity)==null)
				{
					positions.put(txnEntity, newPosition);
					newPosition++;
				}
				matheHeatMapData+="{"+positions.get(entity)+","+positions.get(txnEntity)+","+formatter.format(heat.get(entity).get(txnEntity))+"},";
			}
		}
		matheHeatMapData+="},";
		System.out.println(matheHeatMapData);
	}
	
	static List<String[]> readLinesFullTransfers(String csvFileName)
	{
		List<String> lines=null;
		try
		{
			lines = Files.readAllLines(new File(csvFileName).toPath());
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
			if(lineParts.length>37 && lineParts[3].length()>3)
			{
				try
				{
					long amount=Long.parseLong(lineParts[37]);
					int lastDotIndStart=lineParts[3].lastIndexOf('.');
					int lastDotIndEnd=lineParts[13].lastIndexOf('.');
					if(amount>500000 && lastDotIndStart>-1 && lineParts[3].length()>=lastDotIndStart+4
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
				catch(NumberFormatException e)
				{
					System.out.println(e);
				}
			}
		}
		return splitData;
	}
	
	static void averageThroughput(List<String[]> lines) throws IOException
	{
		List<String> xs=new ArrayList<>();
		List<String> ys=new ArrayList<>();
		
		int numAbsTxns=0;
		
		List<Double> amts=new ArrayList<>();
		Hashtable<Long, List<Double>> throughputsByMonthNoEnc=new Hashtable<>();
		Hashtable<Long, List<Double>> throughputsByMonthEnc=new Hashtable<>();
		double totalAvgTransferred=0;
		double numberTxns=0; 
		long totalEncTxns=0;
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		for(String[] line: lines)
		{
			try 
			{
				long startTime=sdf.parse(line[3]).getTime();
				long endTime=sdf.parse(line[13]).getTime();
				
				long month=startTime/(1000*3600*24*30);
				if(throughputsByMonthNoEnc.get(month)==null)
				{
					throughputsByMonthNoEnc.put(month, new ArrayList<>());
				}
				if(throughputsByMonthEnc.get(month)==null)
				{
					throughputsByMonthEnc.put(month, new ArrayList<>());
				}
				if(endTime-startTime>0)
				{
					long amount=Long.parseLong(line[37]);
					double throughput=1000.0*amount/(endTime-startTime);
					totalAvgTransferred+=throughput;
					amts.add(throughput);
					
					String enc=line[22];
					if(enc.equals("t"))
					{
						throughputsByMonthEnc.get(month).add(throughput);
						totalEncTxns++;
					}
					else if(enc.equals("f"))
					{
						throughputsByMonthNoEnc.get(month).add(throughput);
					}
					else
					{
						int u=0;
					}
					
					xs.add(""+amount);
					ys.add(""+(long)throughput);
					
					numberTxns++;
				}
			}
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			numAbsTxns++;
		}
		
		Files.write(new File("/home/willie/workspace/Globus/data/amounts").toPath(), xs);
		Files.write(new File("/home/willie/workspace/Globus/data/throughputs").toPath(), ys);
		
		Collections.sort(amts);
		
		double averageTP=totalAvgTransferred/numberTxns;
		System.out.println(averageTP);
		
		double medianTP=amts.get(amts.size()/2);
		System.out.println(medianTP);
		
		double totalAvgDeviation=0.0;
		numberTxns=0;
		
		List<Long> months=new ArrayList<>(throughputsByMonthEnc.keySet());
		Collections.sort(months);
		
		String matheEncMed="{";
		String matheNoEncMed="{";
		String matheEncAvg="{";
		String matheNoEncAvg="{";
		for(Long month: months)
		{
			List<Double> encMonthRate=throughputsByMonthEnc.get(month);
			Collections.sort(encMonthRate);
			if(!encMonthRate.isEmpty())
			{
				matheEncMed+=encMonthRate.get(encMonthRate.size()/2)+",";
			}
			else
			{
				matheEncMed+="0,";
			}
			
			List<Double> noEncMonthRate=throughputsByMonthNoEnc.get(month);
			Collections.sort(noEncMonthRate);
			if(!noEncMonthRate.isEmpty())
			{
				matheNoEncMed+=noEncMonthRate.get(noEncMonthRate.size()/2)+",";
			}
			else
			{
				matheNoEncMed+="0,";
			}
			
			double encTotal=0.0;
			for(Double tp: encMonthRate)
			{
				encTotal+=tp;
			}
			matheEncAvg+=(encTotal/encMonthRate.size())+",";
			
			double noEncTotal=0.0;
			for(Double tp: noEncMonthRate)
			{
				noEncTotal+=tp;
			}
			matheNoEncAvg+=(noEncTotal/noEncMonthRate.size())+",";
		}
		
		System.out.println(matheEncMed);
		System.out.println(matheNoEncMed);
		System.out.println(matheEncAvg);
		System.out.println(matheNoEncAvg);
		
		for(String[] line: lines)
		{
			try 
			{
				long startTime=sdf.parse(line[3]).getTime();
				long endTime=sdf.parse(line[13]).getTime();
				if(endTime-startTime>0)
				{
					long amount=Long.parseLong(line[37]);
					double tp=1000.0*amount/(endTime-startTime);
					totalAvgDeviation+=Math.abs(tp-medianTP);
					if(tp<averageTP && Math.abs(tp-averageTP)>averageTP)
					{
						int u=0;
					}
					else if(tp>averageTP && Math.abs(tp-0)<Math.abs(tp-averageTP))
					{
						int u=0;
					}
					numberTxns++;
				}
			}
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
		}
		System.out.println(totalAvgDeviation/numberTxns);
		int u=0;
	}
	
	static void sanityCheckHistAcc(List<String[]> lines)
	{
		Hashtable<String, String[]> firstSrcTxns=new Hashtable<>();
		for(String[] line: lines)
		{
			String userID=line[2];
			String srcID=line[14];
			String dstID=line[15];
			
			if(firstSrcTxns.get(userID)==null)
			{
				firstSrcTxns.put(userID, new String[2]);
			}
			if(firstSrcTxns.get(userID)[0]==null)
			{
				firstSrcTxns.get(userID)[0]=srcID;
			}
			else if(firstSrcTxns.get(userID)[1]==null)
			{
				firstSrcTxns.get(userID)[1]=srcID;
			}
		}
		
		String txns="";
		for(String[] userFirstTwo: firstSrcTxns.values())
		{
			if(userFirstTwo[1]!=null)
			{
				txns+=userFirstTwo[0]+","+userFirstTwo[1]+"\n";
			}
		}
		System.out.println(txns);
	}
	
	static void getEPActiveUserStats(List<String[]> splitData, List<String[]> short_eps)
	{
		Hashtable<String, String> deleted=new Hashtable<>();
		for(String[] line: short_eps)
		{
			String epID=line[0];
			if(line.length>4)
			{
				deleted.put(epID, epID);
			}
			
		}	
		
		Hashtable<String, Hashtable<String,String>> userEPPairs=new Hashtable<>();
		Hashtable<String, Hashtable<String,String>> epEPPairs=new Hashtable<>();
		Hashtable<String, String> scrEPs=new Hashtable<>();
		Hashtable<String, String> dstEPs=new Hashtable<>();
		Hashtable<String, String> allEPs=new Hashtable<>();
		for(String[] line: splitData)
		{
			String userID=line[2];
			String srcID=line[14];
			String dstID=line[15];
			
			if(userEPPairs.get(userID)==null)
			{
				userEPPairs.put(userID, new Hashtable<String,String>());
			}
			
			if(deleted.get(srcID)==null)
			{
				userEPPairs.get(userID).put(srcID, srcID);
				scrEPs.put(srcID, srcID);
				allEPs.put(srcID, srcID);
			}
			
			if(deleted.get(dstID)==null)
			{
				userEPPairs.get(userID).put(dstID, dstID);
				dstEPs.put(dstID, dstID);
				allEPs.put(dstID, dstID);
			}
			if(deleted.get(srcID)==null && deleted.get(dstID)==null)
			{
				if(epEPPairs.get(srcID)==null)
				{
					epEPPairs.put(srcID, new Hashtable<>());
				}
				epEPPairs.get(srcID).put(dstID, dstID);
			}
		}
		double numberPairs=0;
		for(Hashtable<String,String> userUsedEPs: userEPPairs.values())
		{
			numberPairs+=userUsedEPs.size();
		}	
		double numberEPPairs=0;
		for(Hashtable<String,String> epEPs: epEPPairs.values())
		{
			numberEPPairs+=epEPs.size();
		}
		System.out.println("EPEP Sparsity: "+(numberEPPairs/(allEPs.size()*allEPs.size())));
		System.out.println("UserEP Sparsity: "+(numberPairs/(allEPs.size()*userEPPairs.size())));
		System.out.println("Active Src Eps: "+scrEPs.size());
		System.out.println("Active Dst Eps: "+dstEPs.size());
		System.out.println("Active Eps: "+allEPs.size());
		System.out.println("Active Users: "+userEPPairs.size());
	}
	
	static List<IDData<Long>> amountTransfered(List<String[]> data)
	{
		Hashtable<String, IDData<Long>> ids=new Hashtable<>();
		for(String[] line: data)
		{
			String id=null;
			try
			{
			id=line[2];
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			long amount=Long.parseLong(line[37]);
			
			if(ids.get(id)==null)
			{
				ids.put(id, new IDData<Long>(id, 0L));
			}
			if(ids.get(id).data==null)
			{
				int u=0;
			}
			ids.get(id).data=((Long)ids.get(id).data)+amount;
		}
		List<IDData<Long>> result=new ArrayList<IDData<Long>>(ids.values());
		
		System.out.println("amount transferred");
		Collections.sort(result);
		String dataString="{";
		for(int idInd=result.size()-1; idInd>=0; idInd--)
		{
			dataString+=((long)result.get(idInd).data)+",";
		}
		dataString=dataString.substring(0, dataString.length()-1)+"}";
		System.out.println(dataString);
		return result;
	}
	
	static void userIdsInTxns(List<String[]> data, List<String[]> short_eps)
	{
		Hashtable<String, String> deleted=new Hashtable<>();
		for(String[] line: short_eps)
		{
			String epID=line[0];
			if(line.length>4)
			{
				deleted.put(epID, epID);
			}
		}
		
		
		Hashtable<String, String> ids=new Hashtable<>();
		Hashtable<String, String> noDelIds=new Hashtable<>();
		long numberTxnBTUndeleted=0;
		for(String[] line: data)
		{
			String id=line[1];
			String srcID=line[2];
			String dstID=line[3];
			ids.put(id, id);
			if(noDelIds.get(srcID)==null
					&& noDelIds.get(dstID)==null)
			{
				numberTxnBTUndeleted++;
				noDelIds.put(id, id);
			}
		}
		
		System.out.println("Transactions b/t undeleted: "+numberTxnBTUndeleted);
		List<String> uniqueTxnUIDs=new ArrayList<String>(ids.keySet());
		List<String> uniqueTxnUIDsUndeleted=new ArrayList<String>(noDelIds.keySet());
		System.out.println("Distinct users in transactions: "+uniqueTxnUIDs.size());
		System.out.println("Distinct users in undeleted transactions: "+uniqueTxnUIDsUndeleted.size());
		try 
		{
			Files.write(new File("/home/c/workspace/Globus/data/UIDsinTxns").toPath(), uniqueTxnUIDs);
			Files.write(new File("/home/c/workspace/Globus/data/UIDsinTxnsUndeleted").toPath(), uniqueTxnUIDsUndeleted);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	static List<IDData<Integer>> numberTransfers(List<String[]> data)
	{
		Hashtable<String, IDData<Integer>> ids=new Hashtable<>();
		for(String[] line: data)
		{
			String id=line[2];
			
			if(ids.get(id)==null)
			{
				ids.put(id, new IDData<Integer>(id, 0));
			}
			ids.get(id).data=((Integer)ids.get(id).data)+1;
		}
		
		int num=(int) ids.get("38526").data;
		
		List<IDData<Integer>> result=new ArrayList<IDData<Integer>>(ids.values());
		Collections.sort(result);
		System.out.println("number Transfers");
		String dataString="{";
		for(int idInd=result.size()-1; idInd>=0; idInd--)
		{
			dataString+=((int)result.get(idInd).data)+",";
		}
		dataString=dataString.substring(0, dataString.length()-1)+"}";
		System.out.println(dataString);
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	static List<IDData<Double>> avgAmountTransfered(List<String[]> data)
	{
		Hashtable<String, IDData<Long>> idsAmount=new Hashtable<>();
		for(String[] line: data)
		{
			String id=null;
			try
			{
			id=line[2];
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			long amount=Long.parseLong(line[10]);
			
			if(idsAmount.get(id)==null)
			{
				idsAmount.put(id, new IDData<Long>(id, 0L));
			}
			idsAmount.get(id).data=((Long)idsAmount.get(id).data)+amount;
		}
		
		Hashtable<String, IDData<Integer>> idsFreq=new Hashtable<>();
		for(String[] line: data)
		{
			String id=line[2];
			
			if(idsFreq.get(id)==null)
			{
				idsFreq.put(id, new IDData<Integer>(id, 0));
			}
			idsFreq.get(id).data=((Integer)idsFreq.get(id).data)+1;
		}
		
		List<IDData<Double>> avgTransfers=new ArrayList<>();
		for(String id: idsAmount.keySet())
		{
			avgTransfers.add(new IDData<Double>(id, ((double)(Long)idsAmount.get(id).data)/((Integer)idsFreq.get(id).data)));
		}
		
		System.out.println("avgAmountTransfered");
		Collections.sort(avgTransfers);
		String dataString="{";
		for(int idInd=0; idInd<avgTransfers.size(); idInd++)
		{
			dataString+="{"+idInd+", "+((double)avgTransfers.get(idInd).data)+"},";
		}
		dataString=dataString.substring(0, dataString.length()-1)+"}";
		System.out.println(dataString);
		
		return avgTransfers;
	}
	
	static List<IDData<Integer>> numberEndpoints(List<String[]> data)
	{
		Hashtable<String, IDData<Integer>> ids=new Hashtable<>();
		Hashtable<String, List<String>> transferedTo=new Hashtable<>();
		for(String[] line: data)
		{
			String id=line[2];
			
			if(ids.get(id)==null)
			{
				ids.put(id, new IDData<Integer>(id, 0));
				transferedTo.put(id, new ArrayList<String>());
			}
			
			if(!transferedTo.get(id).contains(line[3]))
			{
				transferedTo.get(id).add(line[3]);
				ids.get(id).data=((Integer)ids.get(id).data)+1;
			}
		}
		return new ArrayList<IDData<Integer>>(ids.values());
	}
	
	static void numberPointsPerUser(List<String[]> data)
	{
		Hashtable<String, Object[]> transferedTo=new Hashtable<>();
		Hashtable<String, IDData<Integer>> totalTransfers=new Hashtable<>();
		for(String[] line: data)
		{
			String userID=line[2];
			String srcID=line[14];
			String dstID=line[15];
			
			if(transferedTo.get(userID)==null)
			{
				Object[] srcDstPoints=new Object[]{new ArrayList<String>(), new ArrayList<String>()};
				transferedTo.put(userID, srcDstPoints);
				totalTransfers.put(userID, new IDData<Integer>(userID, 0));
			}
			if(!((List<String>)(transferedTo.get(userID)[0])).contains(srcID))
			{
				((List<String>)(transferedTo.get(userID)[0])).add(srcID);
				totalTransfers.get(userID).data=(Integer)totalTransfers.get(userID).data+1;
			}
			if(!((List<String>)(transferedTo.get(userID)[1])).contains(dstID))
			{
				((List<String>)(transferedTo.get(userID)[1])).add(dstID);
				totalTransfers.get(userID).data=(Integer)totalTransfers.get(userID).data+1;
			}
		}
		List<IDData<Integer>> transfers=new ArrayList(totalTransfers.values());
		Collections.sort(transfers);
		
		System.out.println("Unique endpoints per user");
		String srcData="{";
		for(int tInd=transfers.size()-1; tInd>=0; tInd--)
		{
			srcData+=(((List<String>)(transferedTo.get(transfers.get(tInd).ID)[0])).size()+
					+((List<String>)(transferedTo.get(transfers.get(tInd).ID)[1])).size())+",";
		}
		srcData=srcData.substring(0, srcData.length()-1)+"}";
		System.out.println(srcData);
	}
	
	static void numberPointsPerUserBins(List<String[]> data)
	{
		Hashtable<String, Object[]> transferedTo=new Hashtable<>();
		Hashtable<String, IDData<Integer>> totalTransfers=new Hashtable<>();
		for(String[] line: data)
		{
			String userID=line[1];
			String srcID=line[2];
			String dstID=line[3];
			
			if(transferedTo.get(userID)==null)
			{
				Object[] srcDstPoints=new Object[]{new ArrayList<String>(), new ArrayList<String>()};
				transferedTo.put(userID, srcDstPoints);
				totalTransfers.put(userID, new IDData<Integer>(userID, 0));
			}
			if(!((List<String>)(transferedTo.get(userID)[0])).contains(srcID))
			{
				((List<String>)(transferedTo.get(userID)[0])).add(srcID);
				totalTransfers.get(userID).data=(Integer)totalTransfers.get(userID).data+1;
			}
			if(!((List<String>)(transferedTo.get(userID)[1])).contains(dstID))
			{
				((List<String>)(transferedTo.get(userID)[1])).add(dstID);
				totalTransfers.get(userID).data=(Integer)totalTransfers.get(userID).data+1;
			}
		}
		List<IDData<Integer>> transfers=new ArrayList(totalTransfers.values());
		Collections.sort(transfers);
		int min=0;
		int max=((List<String>)(transferedTo.get(transfers.get(transfers.size()-1).ID)[0])).size()
				+((List<String>)(transferedTo.get(transfers.get(transfers.size()-1).ID)[1])).size();
		
		int scaleAmt=2;
		int[] buckets=new int[Long.toString(max, scaleAmt).length()];
		
		
		
		for(int tInd=transfers.size()-1; tInd>=0; tInd--)
		{
			int num=((List<String>)(transferedTo.get(transfers.get(tInd).ID)[0])).size()+((List<String>)(transferedTo.get(transfers.get(tInd).ID)[1])).size();
			String str=Long.toString((((List<String>)(transferedTo.get(transfers.get(tInd).ID)[0])).size()+((List<String>)(transferedTo.get(transfers.get(tInd).ID)[1])).size()), scaleAmt);
			int bucketNumber=Long.toString((((List<String>)(transferedTo.get(transfers.get(tInd).ID)[0])).size()+((List<String>)(transferedTo.get(transfers.get(tInd).ID)[1])).size()), scaleAmt).length()-1;
			if(tInd==5)
			{
				int y=0;
			}
			buckets[bucketNumber]++;
		}
		String srcData="{";
		String legendData="ChartLabels->{";
		for(int bucketInd=buckets.length-1; bucketInd>=0; bucketInd--)
		{
			srcData+=buckets[bucketInd]+",";
			if(bucketInd>0)
			{
				legendData+="\""+Math.pow(scaleAmt, bucketInd-1)+"-"+Math.pow(scaleAmt, bucketInd)+"\",";
			}
			else
			{
				legendData+="1}";
			}
		}
		srcData=srcData.substring(0, srcData.length()-1)+"}";
		System.out.println(srcData+","+legendData);
	}
	
	static void numberPointsPerUserSrcDstBoth(List<String[]> data)
	{
		Hashtable<String, Object[]> transferedTo=new Hashtable<>();
		Hashtable<String, IDData<Integer>> totalTransfers=new Hashtable<>();
		int u=0;
		for(String[] line: data)
		{
			u++;
			String userID=line[1];
			String srcID=line[2];
			String dstID=line[3];
			
			if(transferedTo.get(userID)==null)
			{
				Object[] srcDstBothPoints=new Object[]{new Hashtable<String, String>(), new Hashtable<String, String>(), new Hashtable<String, String>()};
				transferedTo.put(userID, srcDstBothPoints);
				totalTransfers.put(userID, new IDData<Integer>(userID, 0));
			}
			
			if(((Hashtable<String, String>)(transferedTo.get(userID)[2])).get(srcID)==null)
			{
				if(((Hashtable<String, String>)(transferedTo.get(userID)[0])).get(srcID)==null)
				{
					if(((Hashtable<String, String>)(transferedTo.get(userID)[1])).get(dstID)==null)
					{
						((Hashtable<String, String>)(transferedTo.get(userID)[0])).put(srcID, srcID);
						totalTransfers.get(userID).data=(Integer)totalTransfers.get(userID).data+1;
					}
					else
					{
						((Hashtable<String, String>)(transferedTo.get(userID)[1])).remove(srcID);
						((Hashtable<String, String>)(transferedTo.get(userID)[2])).put(srcID, srcID);
					}
				}
			}
			
			if(((Hashtable<String, String>)(transferedTo.get(userID)[2])).get(dstID)==null)
			{
				if(((Hashtable<String, String>)(transferedTo.get(userID)[1])).get(srcID)==null)
				{
					if(((Hashtable<String, String>)(transferedTo.get(userID)[0])).get(dstID)==null)
					{
						((Hashtable<String, String>)(transferedTo.get(userID)[1])).put(dstID, dstID);
						totalTransfers.get(userID).data=(Integer)totalTransfers.get(userID).data+1;
					}
					else
					{
						((Hashtable<String, String>)(transferedTo.get(userID)[0])).remove(dstID);
						((Hashtable<String, String>)(transferedTo.get(userID)[2])).put(dstID, dstID);
					}
				}
			}
		}
		List<IDData<Integer>> transfers=new ArrayList();
		for(String key: transferedTo.keySet())
		{
			transfers.add(new IDData<Integer>(key, ((Hashtable<String, String>)(transferedTo.get(key)[0])).size()+
					((Hashtable<String, String>)(transferedTo.get(key)[1])).size()
					+((Hashtable<String, String>)(transferedTo.get(key)[2])).size()));
		}
		Collections.sort(transfers);
		
		String srcData="{";
		for(int tInd=transfers.size()-1; tInd>=0; tInd--)
		{
			srcData+="{"+((Hashtable<String, String>)(transferedTo.get(transfers.get(tInd).ID)[0])).size()+","
					+((Hashtable<String, String>)(transferedTo.get(transfers.get(tInd).ID)[2])).size()+","
					+((Hashtable<String, String>)(transferedTo.get(transfers.get(tInd).ID)[1])).size()+"},";
		}
		srcData=srcData.substring(0, srcData.length()-1)+"}";
		System.out.println(srcData);
	}
	
	static List<IDData<Integer>> numberEndpoints(List<String[]> data)
	{
		Hashtable<String, IDData<Integer>> ids=new Hashtable<>();
		Hashtable<String, List<String>> transferedTo=new Hashtable<>();
		for(String[] line: data)
		{
			String id=line[2];
			
			if(ids.get(id)==null)
			{
				ids.put(id, new IDData<Integer>(id, 0));
				transferedTo.put(id, new ArrayList<String>());
			}
			
			if(!transferedTo.get(id).contains(line[3]))
			{
				transferedTo.get(id).add(line[3]);
				ids.get(id).data=((Integer)ids.get(id).data)+1;
			}
		}
		return new ArrayList<IDData<Integer>>(ids.values());
	}
	
	static List<IDData<Integer>> maxSameTransfers(List<String[]> data)
	{
		Hashtable<String, IDData<Integer>> ids=new Hashtable<>();
		Hashtable<String, Hashtable<String, Integer>> endpointFreqs=new Hashtable<>();
		for(String[] line: data)
		{
			String id=line[2];
			
			if(ids.get(id)==null)
			{
				ids.put(id, new IDData<Integer>(id, 0));
				endpointFreqs.put(id, new Hashtable<String, Integer>());
			}
			if(endpointFreqs.get(id).get(line[3])==null)
			{
				endpointFreqs.get(id).put(line[3], 0);
			}
			if(endpointFreqs.get(id).get(line[3])>1)
			{
				int u=0;
			}
			endpointFreqs.get(id).put(line[3], endpointFreqs.get(id).get(line[3])+1);
		}
		
		List<IDData<Integer>> maxSameTransfers=new ArrayList<>();
		for(String id: ids.keySet())
		{
			int max=0;
			IDData<Integer> toAdd=new IDData<Integer>(id, max);
			for(String endpointID: endpointFreqs.get(id).keySet())
			{
				if(max<endpointFreqs.get(id).get(endpointID))
				{
					max=endpointFreqs.get(id).get(endpointID);
					toAdd.otherID=endpointID;
				}
			}
			toAdd.data=max;
			maxSameTransfers.add(toAdd);
		}
		
		return maxSameTransfers;
	}
	
	static List<IDData<Integer>> unqiueUsersPerSource(List<String[]> data)
	{
		Hashtable<String, IDData<Integer>> ids=new Hashtable<>();
		Hashtable<String, Hashtable<String, Integer>> uniqueUsers=new Hashtable<>();
		for(String[] line: data)
		{
			String userID=line[1];
			String src=line[2];
			String dst=line[3];
			
			if(ids.get(src)==null)
			{
				ids.put(src, new IDData<Integer>(src, 0));
				uniqueUsers.put(src, new Hashtable<String, Integer>());
			}
			if(uniqueUsers.get(src).get(userID)==null)
			{
				ids.get(src).data=(Integer)ids.get(src).data+1;
				uniqueUsers.get(src).put(userID, 0);
			}
			
			if(ids.get(dst)==null)
			{
				ids.put(dst, new IDData<Integer>(src, 0));
				uniqueUsers.put(dst, new Hashtable<String, Integer>());
			}
			if(uniqueUsers.get(dst).get(userID)==null)
			{
				ids.get(dst).data=(Integer)ids.get(dst).data+1;
				uniqueUsers.get(dst).put(userID, 0);
			}
		}
		
		List<IDData<Integer>> uUsersPerSourceList=new ArrayList<>();
		for(String id: ids.keySet())
		{
			uUsersPerSourceList.add(ids.get(id));
		}
		
		Collections.sort(uUsersPerSourceList);
		System.out.println("uUsersPerSourceList");
		String dataString="{";
		for(int idInd=uUsersPerSourceList.size()-1; idInd>=0; idInd--)
		{
			dataString+=((Integer)(uUsersPerSourceList.get(idInd).data))+",";
		}
		dataString=dataString.substring(0, dataString.length()-1)+"}";
		//System.out.println(dataString);
		
		int totalEps=0;
		Hashtable<Integer, Integer> uuFreq=new Hashtable<>();
		for(IDData<Integer> epUUFreq: uUsersPerSourceList)
		{
			if(uuFreq.get((int)epUUFreq.data)==null)
			{
				uuFreq.put((int)epUUFreq.data, 0);
			}
			uuFreq.put((int)epUUFreq.data, uuFreq.get((int)epUUFreq.data)+1);
			totalEps++;
		}
		
		double remainingEps=totalEps;
		String epsLeft="{";
		String barMathePlot="{";
		List<Integer> uuList=new ArrayList<>(uuFreq.keySet());
		Collections.sort(uuList);
		DecimalFormat df=new DecimalFormat("#0.000000"); 
		for(Integer uu: uuList)
		{
			barMathePlot+="{"+uu+","+uuFreq.get(uu)+"},";
			epsLeft+="{"+uu+","+df.format(remainingEps/totalEps)+"},";
			remainingEps-=uuFreq.get(uu);
		}
		barMathePlot=barMathePlot.substring(0, barMathePlot.length()-1)+"}";
		epsLeft=epsLeft.substring(0, epsLeft.length()-1)+"}";
		System.out.println(barMathePlot);
		System.out.println(epsLeft);
		return uUsersPerSourceList;
	}
	
	static List<IDData<Integer>> userUsagePerSource(List<String[]> data)
	{
		Hashtable<String, IDData<Long>> ids=new Hashtable<>();
		Hashtable<String, Hashtable<String, Long>> sourceUsers=new Hashtable<>();
		for(String[] line: data)
		{
			String userID=line[1];
			String id=line[2];
			Long amountSent=Long.parseLong(line[10]);
			
			if(ids.get(id)==null)
			{
				ids.put(id, new IDData<Long>(id, 0L));
				sourceUsers.put(id, new Hashtable<String, Long>());
			}
			if(sourceUsers.get(id).get(userID)==null)
			{
				sourceUsers.get(id).put(userID, 0L);
			}
			ids.get(id).data=(Long)(ids.get(id).data)+amountSent;
			sourceUsers.get(id).put(userID, sourceUsers.get(id).get(userID)+amountSent);
		}
		
		List<IDData<Long>> userUsagePerSourceList=new ArrayList<>(ids.values());
		Collections.sort(userUsagePerSourceList);
		
		System.out.println("userUsagePerSourceList");
		String dataString="{";
		userUsagePerSourceList=userUsagePerSourceList.subList(37600, userUsagePerSourceList.size());
		for(IDData<Long> souceInfo: userUsagePerSourceList)
		{
			List<Long> souceUsageList=new ArrayList<>(sourceUsers.get(souceInfo.ID).values());
			Collections.sort(souceUsageList);
			String subDataString="{";
			for(int usageIndex=souceUsageList.size()-1; usageIndex>=0; usageIndex--)
			{
				subDataString+=souceUsageList.get(usageIndex)+",";
			}
			subDataString=subDataString.substring(0, subDataString.length()-1)+"},";
			dataString+=subDataString;
		}
		dataString=dataString.substring(0, dataString.length()-1)+"}";
		System.out.println(dataString);
		
		return null;
	}
	
	static long earliestDate=Long.MAX_VALUE;
	static Hashtable<Long, Long> getEarliestUserCreationDate(List<String[]> data, List<String[]> userData)
	{
		Hashtable<Long, List<Long>> usersAssociatedWithMachine=new Hashtable<>();
		for(String[] line: data)
		{
			Long userID=Long.parseLong(line[1]);
			Long machineID=Long.parseLong(line[2]);
			
			if(usersAssociatedWithMachine.get(machineID)==null)
			{
				usersAssociatedWithMachine.put(machineID, new ArrayList<Long>());
			}
			if(!usersAssociatedWithMachine.get(machineID).contains(userID))
			{
				usersAssociatedWithMachine.get(machineID).add(userID);
			}
		}
		
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		Hashtable<Long, Long> creationDateTable=new Hashtable<>();
		for(String[] line: userData)
		{
			try
			{
				Long id=Long.parseLong(line[0]);
				Date date=sdf.parse(line[3]); 
				long timeInMillisSinceEpoch = date.getTime(); 
				long timeInDaysSinceEpoch = timeInMillisSinceEpoch / (5*24*60*60*1000);
				creationDateTable.put(id, timeInDaysSinceEpoch);
				if(earliestDate>timeInDaysSinceEpoch)
				{
					earliestDate=timeInDaysSinceEpoch;
				}
			}
			catch(Exception e)
			{
				System.out.println(e);
			}
		}
		
		Hashtable<Long, Long> earliestUserCreationDate=new Hashtable<>();
		for(Long machineID: usersAssociatedWithMachine.keySet())
		{
			long earliestMachineDate=Long.MAX_VALUE;
			for(Long creationDate: usersAssociatedWithMachine.get(machineID))
			{
				if(earliestMachineDate>creationDateTable.get(creationDate))
				{
					earliestMachineDate=creationDateTable.get(creationDate);
				}
			}
			earliestUserCreationDate.put(machineID, earliestMachineDate);
		}
		return earliestUserCreationDate;
	}
	
	//Object{id, long date[], long xfer_amt[]}
	static void userDataDataTransfer(List<String[]> data, Hashtable<Long, Long> earliestUserCreationDate)
	{
		Hashtable<Long, List<Long>[]> dataTable=new Hashtable<>();//id, {day, amt}
		Hashtable<Long, List<String[]>> dateLines=new Hashtable<>();
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		long maximum=Long.MIN_VALUE;
		long maxTransfered=Long.MIN_VALUE;
		Hashtable<Long, Long> seenIDsTable=new Hashtable<>();
		long seenIDs=0;
		Hashtable<Long, String> daysToDates=new Hashtable<>();
		
		for(String[] line: data)
		{
			try
			{
				Long id=Long.parseLong(line[2]);
				if(seenIDsTable.get(id)==null)
				{
					seenIDsTable.put(id, id);
					seenIDs++;
				}
				Long rID=Long.parseLong(line[3]);
				if(seenIDsTable.get(rID)==null)
				{
					seenIDsTable.put(rID, rID);
					seenIDs++;
				}
				long amt=Long.parseLong(line[10]);
				
				if(amt>maxTransfered)
				{
					maxTransfered=amt;
				}
				
				Date date=sdf.parse(line[4]); 
				long timeInMillisSinceEpoch = date.getTime(); 
				long timeInDaysSinceEpoch = timeInMillisSinceEpoch / (5*24*60*60*1000);
				
				if(dateLines.get(timeInDaysSinceEpoch)==null)
				{
					dateLines.put(timeInDaysSinceEpoch, new ArrayList<String[]>());
				}
				dateLines.get(timeInDaysSinceEpoch).add(line);
				
				if(earliestDate>timeInDaysSinceEpoch)
				{
					earliestDate=timeInDaysSinceEpoch;
				}
				
				if(maximum<timeInDaysSinceEpoch)
				{
					maximum=timeInDaysSinceEpoch;
				}
				
				if(daysToDates.get(timeInDaysSinceEpoch)==null)
				{
					daysToDates.put(timeInDaysSinceEpoch, line[4]);
				}
				
				if(dataTable.get(id)==null)
				{
					List<Long>[] subData=new ArrayList[2];
					for(int ind=0; ind<subData.length; ind++)
					{
						subData[ind]=new ArrayList<Long>();
					}
					dataTable.put(id, subData);
				}
				dataTable.get(id)[0].add(timeInDaysSinceEpoch);
				dataTable.get(id)[1].add(rID);
			}
			catch(Exception e)
			{
				//System.out.println(e);
			}
		}
		
		for(List<Long>[] subData: dataTable.values())
		{
			for(int ind=0; ind<subData[0].size(); ind++)
			{
				subData[0].set(ind, subData[0].get(ind)-earliestDate);
			}
		}
		
		BufferedImage plot=new BufferedImage((int)seenIDs, (int)(maximum-earliestDate)+1, BufferedImage.TYPE_INT_RGB);
		
		Hashtable<Long, List<Long>> dayTransfers=new Hashtable<>();
		
		Hashtable<Long, Integer> idsToInts=new Hashtable<>();
		int idXPos=0;
		for(Long id: dataTable.keySet())
		{
			if(idsToInts.get(id)==null)
			{
				idsToInts.put(id, idXPos);
				idXPos++;
			}
			for(int pointInd=0; pointInd<dataTable.get(id)[0].size(); pointInd++)
			{
				if(dataTable.get(id)[0].size()>=2
						&& dataTable.get(id)[0].get(1)-dataTable.get(id)[0].get(0)>=365)
				{
					int u=0;
				}
				int color=(int)(Integer.MAX_VALUE*(((double)dataTable.get(id)[1].get(pointInd)))/maxTransfered);
				int yPos=(int)(long)dataTable.get(id)[0].get(pointInd);
				
				if(dayTransfers.get(dataTable.get(id)[0].get(pointInd))==null)
				{
					dayTransfers.put(dataTable.get(id)[0].get(pointInd), new ArrayList<Long>());
				}
				if(!dayTransfers.get(dataTable.get(id)[0].get(pointInd)).contains(dataTable.get(id)[1].get(pointInd)))
				{
					dayTransfers.get(dataTable.get(id)[0].get(pointInd)).add(dataTable.get(id)[1].get(pointInd));
				}

				try
				{
				plot.setRGB(idsToInts.get(id), yPos, Color.BLUE.getIntArgbPre());
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				Long recvID=dataTable.get(id)[1].get(pointInd);
				if(idsToInts.get(recvID)==null)
				{
					idsToInts.put(recvID, idXPos);
					idXPos++;
				}
				
				try
				{
				plot.setRGB(idsToInts.get(recvID), yPos, Color.RED.getIntArgbPre());
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}

			}
			
			long creationDate=earliestUserCreationDate.get(id)-earliestDate;
			if(creationDate>=0 && creationDate<=(int)(maximum-earliestDate))
			{
				int yPos=(int)creationDate;
				plot.setRGB(idsToInts.get(id), yPos, Color.WHITE.getIntArgbPre());
			}
		}
		plot.setRGB(0, 0, Color.WHITE.getIntArgbPre());
		
		long mostActivity=0;
		Long mostActivityDay=0L;
		for(Long date: dayTransfers.keySet())
		{
			if(dayTransfers.get(date).size()>mostActivity && date>340)
			{
				mostActivity=dayTransfers.get(date).size();
				mostActivityDay=date;
			}
		}
		List<String[]> linesMostActivity=dateLines.get(mostActivityDay+earliestDate);
		String csv="";
		
		String csvLine="";
		for(String item: header)
		{
			csvLine+=item+",";
		}
		csvLine=csvLine.substring(0, csvLine.length()-1)+"\n";
		csv+=csvLine;
		for(String[] line: linesMostActivity)
		{
			csvLine="";
			for(String item: line)
			{
				csvLine+=item+",";
			}
			csvLine=csvLine.substring(0, csvLine.length()-1)+"\n";
			csv+=csvLine;
		}
		System.out.println(csv);
		
		List<Long> mostActivityList=dayTransfers.get(mostActivityDay);
		String date=daysToDates.get(mostActivityDay+earliestDate);
		
		JFrame frame=new JFrame();
		frame.getContentPane().setLayout(new FlowLayout());
		frame.getContentPane().add(new JLabel(new ImageIcon(plot)));
		frame.pack();
		frame.setVisible(true);
		
		File outputfile = new File("C:\\Users\\C\\workspace\\Globus\\plots\\transfersVtime.jpeg");
	    try {
			ImageIO.write(plot, "jpeg", outputfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int u=0;
	}
	
	static List<IDData<Long>> sortByDate(List<String[]> data)
	{
		Hashtable<Long, List<Long>[]> dataTable=new Hashtable<>();//id, {day, amt}
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		List<IDData<Long>> lines=new ArrayList<>();
		
		for(String[] line: data)
		{
			try
			{
				Date date=sdf.parse(line[4]); 
				long timeInMillisSinceEpoch = date.getTime(); 
				long timeInDaysSinceEpoch = timeInMillisSinceEpoch / (5*24*60*60*1000);
				IDData<Long> iddata=new IDData<Long>("", timeInDaysSinceEpoch);
				iddata.line=line;
				lines.add(iddata);
			}
			catch(Exception e)
			{
				System.out.println(e);
			}
		}
		Collections.sort(lines);
		return lines;
	}
	
	public static void saveDataAsMatrix(List<String[]> data)
	{
		Hashtable<Integer, Hashtable<Integer, Integer>> endpointFreqs=new Hashtable<>();
		Hashtable<Integer, Integer> srcIDTranslations=new Hashtable<>();
		Hashtable<Integer, Integer> dstIDTranslations=new Hashtable<>();
		int idNumber=0;
		int srcIDs=0;
		int dstIDs=0;
		for(String[] line: data)
		{
			if(!line[2].isEmpty() && !line[3].isEmpty())
			{
				Integer srcID=Integer.parseInt(line[2]);
				Integer dstID=Integer.parseInt(line[3]);
				
				if(endpointFreqs.get(srcID)==null)
				{
					endpointFreqs.put(srcID, new Hashtable<Integer, Integer>());
				}
				
				if(endpointFreqs.get(srcID).get(dstID)==null)
				{
					endpointFreqs.get(srcID).put(dstID, 0);
				}
				
				endpointFreqs.get(srcID).put(dstID, 
						endpointFreqs.get(srcID).get(dstID)+1);
			}
		}
		
		String csv="";
		for(Integer srcID: endpointFreqs.keySet())
		{
			for(Integer dstID: endpointFreqs.get(srcID).keySet())
			{
				if(endpointFreqs.get(srcID).get(dstID)!=null)
				{
					csv+=srcID+","+dstID+","+endpointFreqs.get(srcID).get(dstID)+"\n";
				}
			}
		}
		
		ByteArrayOutputStream bOut=new ByteArrayOutputStream();
        ObjectOutputStream oOut;
		try 
		{
			oOut = new ObjectOutputStream(bOut);
			oOut.writeUnshared(srcIDTranslations);
	        oOut.close();
	        Files.write(new File("C:\\Users\\C\\workspace\\Globus\\data\\Hashtable_Integer, Integer_SRCIDToIndex").toPath(), bOut.toByteArray());
	        bOut.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		bOut=new ByteArrayOutputStream();
		try 
		{
			oOut = new ObjectOutputStream(bOut);
			oOut.writeUnshared(dstIDTranslations);
	        oOut.close();
	        Files.write(new File("C:\\Users\\C\\workspace\\Globus\\data\\Hashtable_Integer, Integer_DSTIDToIndex").toPath(), bOut.toByteArray());
	        bOut.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		try 
		{
	        Files.write(new File("C:\\Users\\C\\workspace\\Globus\\data\\int[][]_adjmatrix_transFreq").toPath(), csv.getBytes());
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
	}
	
	public static void saveDataAsEdgeList(List<String[]> data)
	{
		Hashtable<Integer, Hashtable<Integer, Integer>> endpointFreqs=new Hashtable<>();
		for(String[] line: data)
		{
			if(!line[2].isEmpty() && !line[3].isEmpty())
			{
				Integer uID=Integer.parseInt(line[1]);
				Integer srcID=Integer.parseInt(line[2]);				
				Integer dstID=Integer.parseInt(line[3]);
				
				if(endpointFreqs.get(uID)==null)
				{
					endpointFreqs.put(uID, new Hashtable<Integer, Hashtable<Integer, Integer>>());
				}
				
				if(endpointFreqs.get(srcIDTranslations.get(srcID))==null)
				{
					endpointFreqs.put(srcIDTranslations.get(srcID), new Hashtable<Integer, Integer>());
				}
				
				if(endpointFreqs.get(srcIDTranslations.get(srcID)).get(dstIDTranslations.get(dstID))==null)
				{
					endpointFreqs.get(srcIDTranslations.get(srcID)).put(dstIDTranslations.get(dstID), 0);
				}
				
				endpointFreqs.get(srcIDTranslations.get(srcID)).put(dstIDTranslations.get(dstID), 
						endpointFreqs.get(srcIDTranslations.get(srcID)).get(dstIDTranslations.get(dstID))+1);
			}
		}
		
		BufferedWriter bOut=new BufferedWriter(new FileWriter(""))
		for(int srcIDInd=0; srcIDInd<matrix.length; srcIDInd++)
		{
			for(int dstIDInd=0; dstIDInd<matrix[srcIDInd].length; dstIDInd++)
			{
				try
				{
					matrix[srcIDInd][dstIDInd]=endpointFreqs.get(srcIDInd).getOrDefault(dstIDInd, 0);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		
		ByteArrayOutputStream bOut=new ByteArrayOutputStream();
        ObjectOutputStream oOut;
		try 
		{
			oOut = new ObjectOutputStream(bOut);
			oOut.writeUnshared(srcIDTranslations);
	        oOut.close();
	        Files.write(new File("C:\\Users\\C\\workspace\\Globus\\data\\Hashtable_Integer, Integer_SRCIDToIndex").toPath(), bOut.toByteArray());
	        bOut.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		bOut=new ByteArrayOutputStream();
		try 
		{
			oOut = new ObjectOutputStream(bOut);
			oOut.writeUnshared(dstIDTranslations);
	        oOut.close();
	        Files.write(new File("C:\\Users\\C\\workspace\\Globus\\data\\Hashtable_Integer, Integer_DSTIDToIndex").toPath(), bOut.toByteArray());
	        bOut.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		bOut=new ByteArrayOutputStream();
		try 
		{
			oOut = new ObjectOutputStream(bOut);
			oOut.writeUnshared(matrix);
	        oOut.close();
	        Files.write(new File("C:\\Users\\C\\workspace\\Globus\\data\\int[][]_adjmatrix_transFreq").toPath(), bOut.toByteArray());
	        bOut.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
	}
	
	static List<IDData<Integer>> randomWalkFreqCentrality(List<String[]> data)
	{
		Hashtable<Integer, Hashtable<Integer, Integer>> endpointFreqs=new Hashtable<>();
		for(String[] line: data)
		{
			if(!line[2].isEmpty() && !line[3].isEmpty())
			{
				Integer srcID=Integer.parseInt(line[2]);
				Integer dstID=Integer.parseInt(line[3]);
				
				if(endpointFreqs.get(srcID)==null)
				{
					endpointFreqs.put(srcID, new Hashtable<Integer, Integer>());
				}
				if(endpointFreqs.get(srcID).get(dstID)==null)
				{
					endpointFreqs.get(srcID).put(dstID, 0);
				}
				endpointFreqs.get(srcID).put(dstID, 
						endpointFreqs.get(srcID).get(dstID)+1);
				
				if(endpointFreqs.get(dstID)==null)
				{
					endpointFreqs.put(dstID, new Hashtable<Integer, Integer>());
				}
				if(endpointFreqs.get(dstID).get(srcID)==null)
				{
					endpointFreqs.get(dstID).put(srcID, 0);
				}		
				endpointFreqs.get(dstID).put(srcID, 
						endpointFreqs.get(dstID).get(srcID)+1);
			}
		}
		
		Hashtable<Integer, Integer> timesVisited=new Hashtable<>();
		for(Integer id: endpointFreqs.keySet())
		{
			timesVisited.put(id, 0);
		}
		
		List<Integer> ids=new ArrayList<Integer>(endpointFreqs.keySet());
		for(int walkNumber=0; walkNumber<1000; walkNumber++)
		{
			Integer randomID=ids.get((int)Math.floor(Math.random()*ids.size()));
			randomWeightedWalk(endpointFreqs, timesVisited, 1000, randomID);
		}
		
		List<IDData<Integer>> walkData=new ArrayList<>();
		for(Integer ID: timesVisited.keySet())
		{
			walkData.add(new IDData<Integer>(""+ID, timesVisited.get(ID)));
		}
		
		Collections.sort(walkData);
		System.out.println("random walk transfer freq weighted");
		String dataString="{";
		for(int idInd=0; idInd<walkData.size(); idInd++)
		{
			dataString+=((int)walkData.get(idInd).data)+",";
		}
		dataString=dataString.substring(0, dataString.length()-1)+"}";
		System.out.println(dataString);
		
		Collections.sort(walkData);
		System.out.println("random walk transfer freq weighted data");
		dataString="";
		for(int idInd=0; idInd<walkData.size(); idInd++)
		{
			dataString+="{"+walkData.get(idInd).ID+","+((int)walkData.get(idInd).data)+"},";
		}
		dataString=dataString.substring(0, dataString.length()-1);
		System.out.println(dataString);
		
		return walkData;
	}
	
	static void randomWeightedWalk(Hashtable<Integer, Hashtable<Integer, Integer>> endpointFreqs,
			Hashtable<Integer, Integer> timesVisited, int numberSteps, Integer startID)
	{
		Integer currentNode=startID;
		for(int stepInd=0; stepInd<numberSteps; stepInd++)
		{
			double[] transitionprobabilities=new double[endpointFreqs.get(currentNode).size()];
			Integer[] transitionIDs=new Integer[endpointFreqs.get(currentNode).size()];
			int totalWeight=0;
			for(int edgeWeight: endpointFreqs.get(currentNode).values())
			{
				totalWeight+=edgeWeight;
			}
			int edgeInd=0;
			for(Integer id: endpointFreqs.get(currentNode).keySet())
			{
				transitionprobabilities[edgeInd]=(double)endpointFreqs.get(currentNode).get(id)/totalWeight;
				transitionIDs[edgeInd]=id;
				edgeInd++;
			}
			
			double random=Math.random();
			edgeInd=-1;
			while(random>0)
			{
				edgeInd++;
				random-=transitionprobabilities[edgeInd];
			}
			
			currentNode=transitionIDs[edgeInd];
			timesVisited.put(currentNode, timesVisited.get(currentNode)+1);
		}
	}
	
	static List<IDData<Integer>> randomWalkTransferCentrality(List<String[]> data)
	{
		Hashtable<Integer, Hashtable<Integer, Long>> endpointFreqs=new Hashtable<>();
		for(String[] line: data)
		{
			if(!line[2].isEmpty() && !line[3].isEmpty())
			{
				Integer srcID=Integer.parseInt(line[2]);
				Integer dstID=Integer.parseInt(line[3]);
				Long amountSent=Long.parseLong(line[10]); 
				
				if(endpointFreqs.get(srcID)==null)
				{
					endpointFreqs.put(srcID, new Hashtable<Integer, Long>());
				}
				if(endpointFreqs.get(srcID).get(dstID)==null)
				{
					endpointFreqs.get(srcID).put(dstID, 0L);
				}
				endpointFreqs.get(srcID).put(dstID, 
						endpointFreqs.get(srcID).get(dstID)+amountSent);
				
				if(endpointFreqs.get(dstID)==null)
				{
					endpointFreqs.put(dstID, new Hashtable<Integer, Long>());
				}
				if(endpointFreqs.get(dstID).get(srcID)==null)
				{
					endpointFreqs.get(dstID).put(srcID, 0L);
				}		
				endpointFreqs.get(dstID).put(srcID, 
						endpointFreqs.get(dstID).get(srcID)+amountSent);
			}
		}
		
		Hashtable<Integer, Integer> timesVisited=new Hashtable<>();
		for(Integer id: endpointFreqs.keySet())
		{
			timesVisited.put(id, 0);
		}
		
		List<Integer> ids=new ArrayList<Integer>(endpointFreqs.keySet());
		for(int walkNumber=0; walkNumber<1000; walkNumber++)
		{
			Integer randomID=ids.get((int)Math.floor(Math.random()*ids.size()));
			randomWeightedWalkLong(endpointFreqs, timesVisited, 1000, randomID);
		}
		
		List<IDData<Integer>> walkData=new ArrayList<>();
		for(Integer ID: timesVisited.keySet())
		{
			walkData.add(new IDData<Integer>(""+ID, timesVisited.get(ID)));
		}
		
		Collections.sort(walkData);
		System.out.println("random walk transfer amt weighted");
		String dataString="{";
		for(int idInd=0; idInd<walkData.size(); idInd++)
		{
			dataString+=((int)walkData.get(idInd).data)+",";
		}
		dataString=dataString.substring(0, dataString.length()-1)+"}";
		System.out.println(dataString);
		
		Collections.sort(walkData);
		System.out.println("random walk transfer amt weighted data");
		dataString="";
		for(int idInd=0; idInd<walkData.size(); idInd++)
		{
			dataString+="{"+walkData.get(idInd).ID+","+((int)walkData.get(idInd).data)+"},";
		}
		dataString=dataString.substring(0, dataString.length()-1);
		System.out.println(dataString);
		
		return walkData;
	}
	
	static void randomWeightedWalkLong(Hashtable<Integer, Hashtable<Integer, Long>> endpointFreqs,
			Hashtable<Integer, Integer> timesVisited, int numberSteps, Integer startID)
	{
		Integer currentNode=startID;
		for(int stepInd=0; stepInd<numberSteps; stepInd++)
		{
			double[] transitionprobabilities=new double[endpointFreqs.get(currentNode).size()];
			Integer[] transitionIDs=new Integer[endpointFreqs.get(currentNode).size()];
			long totalWeight=0;
			for(long edgeWeight: endpointFreqs.get(currentNode).values())
			{
				totalWeight+=edgeWeight;
			}
			int edgeInd=0;
			for(Integer id: endpointFreqs.get(currentNode).keySet())
			{
				transitionprobabilities[edgeInd]=(double)endpointFreqs.get(currentNode).get(id)/totalWeight;
				transitionIDs[edgeInd]=id;
				edgeInd++;
			}
			
			double random=Math.random();
			edgeInd=-1;
			while(random>0)
			{
				edgeInd++;
				random-=transitionprobabilities[edgeInd];
			}
			
			currentNode=transitionIDs[edgeInd];
			timesVisited.put(currentNode, timesVisited.get(currentNode)+1);
		}
	}
	
	static void testMathematicaGraphic(List<String[]> data)
	{
		Hashtable<Integer, Hashtable<Integer, Long>> endpointFreqs=new Hashtable<>();
		for(String[] line: data)
		{
			if(!line[2].isEmpty() && !line[3].isEmpty())
			{
				Integer srcID=Integer.parseInt(line[2]);
				Integer dstID=Integer.parseInt(line[3]);
				Long amountSent=Long.parseLong(line[10]); 
				
				if(endpointFreqs.get(srcID)==null)
				{
					endpointFreqs.put(srcID, new Hashtable<Integer, Long>());
				}
				if(endpointFreqs.get(srcID).get(dstID)==null)
				{
					endpointFreqs.get(srcID).put(dstID, 0L);
				}
				endpointFreqs.get(srcID).put(dstID, 
						endpointFreqs.get(srcID).get(dstID)+amountSent);
			}
		}
		
		String mathematicaPlotString="{";
		int maxNumberEdges=50000;
		int numberEdges=0;
		for(Integer srcID: endpointFreqs.keySet())
		{
			for(Integer dstID: endpointFreqs.get(srcID).keySet())
			{
				mathematicaPlotString+=srcID+"xdex"+dstID+",";
				numberEdges++;
			}
			if(numberEdges>maxNumberEdges)
			{
				break;
			}
		}
		mathematicaPlotString=mathematicaPlotString.substring(0, mathematicaPlotString.length()-1)+"}";
		System.out.println(mathematicaPlotString);
	}
	
	static Object[]/*Hashtable<String, Hashtable<Integer, Long>>*/ topNUsersSendEnpoints(List<String[]> data, int n)
	{
		Hashtable<String, Hashtable<Integer, Long>> endpointFreqs=new Hashtable<>();
		Hashtable<String, IDData<Long>> usageAmt=new Hashtable<>();
		Hashtable<Integer, Long> endpointUsage=new Hashtable<>();
		for(String[] line: data)
		{
			if(!line[2].isEmpty() && !line[3].isEmpty())
			{
				String userID=line[1];
				Integer dstID=Integer.parseInt(line[3]);
				Long amt=Long.parseLong(line[10]);
				
				if(endpointFreqs.get(userID)==null)
				{
					endpointFreqs.put(userID, new Hashtable<Integer, Long>());
				}
				if(endpointFreqs.get(userID).get(dstID)==null)
				{
					endpointFreqs.get(userID).put(dstID, 0L);
				}
				endpointFreqs.get(userID).put(dstID, 
						endpointFreqs.get(userID).get(dstID)+amt);
				
				if(usageAmt.get(userID)==null)
				{
					usageAmt.put(userID, new IDData<Long>(userID, 0L));
				}
				usageAmt.put(userID, new IDData<Long>(userID, (Long)usageAmt.get(userID).data+amt));
				
				if(endpointUsage.get(dstID)==null)
				{
					endpointUsage.put(dstID, 0L);
				}
				endpointUsage.put(dstID, endpointUsage.get(dstID)+amt);
			}
		}
		
		List<IDData<Long>> usageAmtList=new ArrayList<>(usageAmt.values());
		Collections.sort(usageAmtList);
		
		long totalSent=0;
		Hashtable<String, Hashtable<Integer, Long>> topEndpointFreqs=new Hashtable<>();
		for(int ind=usageAmtList.size()-1; ind>usageAmtList.size()-1-n && ind>0; ind--)
		{
			topEndpointFreqs.put(usageAmtList.get(ind).ID, endpointFreqs.get(usageAmtList.get(ind).ID));
			totalSent+=(Long)usageAmtList.get(ind).data;
		}
		
		return new Object[]{topEndpointFreqs, totalSent, usageAmt, endpointUsage};
	}
	
	static Hashtable<String, String> userIDToEmails(List<String[]> userInfo)
	{
		Hashtable<String, String> userIDToEmails=new Hashtable<>();
		for(String[] line: userInfo)
		{
			userIDToEmails.put(line[0], line[2]);
		}
		return userIDToEmails;
	}
	
	static void mathematicaFormatTopUsers(Hashtable<String, Hashtable<Integer, Long>> userUsage,
			Hashtable<String, IDData<Long>> usersSent, Hashtable<Integer, Long> endpointUsage,
			Long totalSent, Hashtable<String, String> idToEmail)
	{
		String matheOut="{";
		for(String user: userUsage.keySet())
		{
			matheOut+="Labeled["+user+","+idToEmail.get(user)+"], Property["
					+user+",VertexSize->"+(5*Math.sqrt(((double)(Long)usersSent.get(user).data/totalSent)))+"],";
		}
		
		Hashtable<Integer, Integer> visited=new Hashtable<>();
		for(String user: userUsage.keySet())
		{
			for(Integer dstID: userUsage.get(user).keySet())
			{
				if(visited.get(dstID)==null && userUsage.get(""+dstID)==null)
				{
					matheOut+="Property["+dstID+",VertexSize->"+(2*Math.pow(((double)endpointUsage.get(dstID)/totalSent), 0.205))+"],";
				}
			}
		}
		matheOut=matheOut.substring(0, matheOut.length()-1)+"},{";
		
		for(String user: userUsage.keySet())
		{
			for(Integer dstID: userUsage.get(user).keySet())
			{
				matheOut+=user+"*de*"+dstID+",";
			}
		}
		matheOut=matheOut.substring(0, matheOut.length()-1)+"},";
		
		matheOut+="VertexStyle->{";
		for(String user: userUsage.keySet())
		{
			matheOut+=user+"->Red,";
		}
		matheOut=matheOut.substring(0, matheOut.length()-1)+"}";
		
		System.out.println(matheOut);
	}
	
	static void mathematicaFormatTopUsersUsername(Hashtable<String, Hashtable<Integer, Long>> userUsage,
			Hashtable<String, IDData<Long>> usersSent, Hashtable<Integer, Long> endpointUsage,
			Long totalSent, Hashtable<String, String> idToEmail)
	{
		String matheOut="{";
		for(String user: userUsage.keySet())
		{
			matheOut+="Labeled["+user+",\""+idToEmail.get(user)+"_"+user+"\"], Property["
					+user+",VertexSize->"+(5*Math.sqrt(((double)(Long)usersSent.get(user).data/totalSent)))+"],";
		}
		
		Hashtable<Integer, Integer> visited=new Hashtable<>();
		for(String user: userUsage.keySet())
		{
			for(Integer dstID: userUsage.get(user).keySet())
			{
				if(visited.get(dstID)==null && userUsage.get(""+dstID)==null)
				{
					matheOut+="Property["+dstID+",VertexSize->"+(3*Math.pow(((double)endpointUsage.get(dstID)/totalSent), 0.19))+"],";
				}
			}
		}
		matheOut=matheOut.substring(0, matheOut.length()-1)+"},{";
		
		for(String user: userUsage.keySet())
		{
			for(Integer dstID: userUsage.get(user).keySet())
			{
				matheOut+=user+"*de*"+dstID+",";
			}
		}
		matheOut=matheOut.substring(0, matheOut.length()-1)+"},";
		
		matheOut+="VertexStyle->{";
		for(String user: userUsage.keySet())
		{
			matheOut+=user+"->Red,";
		}
		matheOut=matheOut.substring(0, matheOut.length()-1)+"}";
		
		System.out.println(matheOut);
	}
	
	static Hashtable<String, String> machineIDToUserID(List<String[]> short_eps)
	{
		
		Hashtable<String, String> machineIDToUserID=new Hashtable<>();
		for(String[] line: short_eps)
		{
			machineIDToUserID.put(line[0], line[2]);
		}
		return machineIDToUserID;
	}
	
	static List<String[]> readShortEps()
	{
		List<String> uIDLines=null;
		try 
		{
			uIDLines = Files.readAllLines(new File(currentDir+"/data/short-eps.csv").toPath());
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		List<String[]> uIDSplitData=new ArrayList<>();
		int amt=0;
		uIDLines.remove(0);
		uIDLines.remove(uIDLines.size()-1);
		for(String line: uIDLines)
		{
			uIDSplitData.add(line.split(","));
			amt++;
			if(amt%10000000==0)
			{
				System.out.println(amt);
				if (amt>0)
				{
					//break;
				}
			}
		}
		return uIDSplitData;
	}
	
	static Hashtable<String, IDData<Double>>[] transactionsInAccount(List<String[]> splitData, Hashtable<String, String> machineIDToUserID)
	{
		Hashtable<String, IDData<Double>> totalUsagesInAcc=new Hashtable<>();
		Hashtable<String, Long> totalUsagesInTotal=new Hashtable<>();
		
		Hashtable<String, IDData<Double>> srcUsagesInAcc=new Hashtable<>();
		Hashtable<String, Long> srcUsagesInTotal=new Hashtable<>();
		
		Hashtable<String, IDData<Double>> dstUsagesInAcc=new Hashtable<>();
		Hashtable<String, Long> dstUsagesInTotal=new Hashtable<>();
		
		for(String[] transaction: splitData)
		{
			String userID=transaction[1];
			String srcID=transaction[2];
			String dstID=transaction[3];
			
			if(!srcID.isEmpty())
			{
				if(totalUsagesInAcc.get(userID)==null)
				{
					totalUsagesInAcc.put(userID, new IDData<Double>(userID, 0.0));
					totalUsagesInTotal.put(userID, 0L);
				}
				if(srcUsagesInAcc.get(userID)==null)
				{
					srcUsagesInAcc.put(userID, new IDData<Double>(userID, 0.0));
					srcUsagesInTotal.put(userID, 0L);
				}
				if(machineIDToUserID.get(srcID).equals(userID))
				{
					totalUsagesInAcc.get(userID).data=(Double)totalUsagesInAcc.get(userID).data+1;
					srcUsagesInAcc.get(userID).data=(Double)srcUsagesInAcc.get(userID).data+1;
				}
				srcUsagesInAcc.get(userID).addLongData=srcUsagesInAcc.get(userID).addLongData+1;
				srcUsagesInTotal.put(userID, srcUsagesInTotal.get(userID)+1);
				totalUsagesInTotal.put(userID, totalUsagesInTotal.get(userID)+1);
			}
			
			if(!dstID.isEmpty())
			{
				if(totalUsagesInAcc.get(userID)==null)
				{
					totalUsagesInAcc.put(userID, new IDData<Double>(userID, 0.0));
					totalUsagesInTotal.put(userID, 0L);
				}
				if(dstUsagesInAcc.get(userID)==null)
				{
					dstUsagesInAcc.put(userID, new IDData<Double>(userID, 0.0));
					dstUsagesInTotal.put(userID, 0L);
				}
				if(machineIDToUserID.get(dstID).equals(userID))
				{
					totalUsagesInAcc.get(userID).data=(Double)totalUsagesInAcc.get(userID).data+1;
					dstUsagesInAcc.get(userID).data=(Double)dstUsagesInAcc.get(userID).data+1;
				}
				dstUsagesInAcc.get(userID).addLongData=dstUsagesInAcc.get(userID).addLongData+1;
				dstUsagesInTotal.put(userID, dstUsagesInTotal.get(userID)+1);
				totalUsagesInTotal.put(userID, totalUsagesInTotal.get(userID)+1);
			}
		}
		
		List<IDData<Double>> srcFracs=new ArrayList<>(srcUsagesInAcc.values());
		Collections.sort(srcFracs, new Comparator<IDData<Double>>()
		{

			@Override
			public int compare(IDData<Double> o1, IDData<Double> o2) 
			{
				return (int)Math.signum(o1.addLongData-o2.addLongData);
			}
	
		});
		List<IDData<Double>> dstFracs=new ArrayList<>(dstUsagesInAcc.values());
		Collections.sort(dstFracs, new Comparator<IDData<Double>>()
		{

			@Override
			public int compare(IDData<Double> o1, IDData<Double> o2) 
			{
				return (int)Math.signum(o1.addLongData-o2.addLongData);
			}
	
		});
		String srcMatheStrDB="{";
		for(int srcInd=srcFracs.size()-1; srcInd>=0; srcInd--)
		{
			srcMatheStrDB+="{"+srcFracs.get(srcInd).data+","
					+(srcUsagesInTotal.get(srcFracs.get(srcInd).ID)-(double)srcFracs.get(srcInd).data)+"},";
		}
		srcMatheStrDB=srcMatheStrDB.substring(0, srcMatheStrDB.length()-1)+"}";
		System.out.println(srcMatheStrDB);
		
		String dstMatheStrDB="{";
		for(int dstInd=dstFracs.size()-1; dstInd>=0; dstInd--)
		{
			dstMatheStrDB+="{"+dstFracs.get(dstInd).data+","
					+(dstUsagesInTotal.get(dstFracs.get(dstInd).ID)-(double)dstFracs.get(dstInd).data)+"},";
		}
		dstMatheStrDB=dstMatheStrDB.substring(0, dstMatheStrDB.length()-1)+"}";
		System.out.println(dstMatheStrDB);
		
		
		double totalSrcInUsr=0.0;
		double totalSrcTxns=0.0;
		for(String usrID: srcUsagesInAcc.keySet())
		{
			totalSrcInUsr+=(Double)srcUsagesInAcc.get(usrID).data;
			totalSrcTxns+=srcUsagesInTotal.get(usrID);
			srcUsagesInAcc.get(usrID).data=(Double)srcUsagesInAcc.get(usrID).data/srcUsagesInTotal.get(usrID);
		}
		double totalDstInUsr=0.0;
		double totalDstTxns=0.0;
		for(String usrID: dstUsagesInAcc.keySet())
		{
			totalDstInUsr+=(Double)dstUsagesInAcc.get(usrID).data;
			totalDstTxns+=dstUsagesInTotal.get(usrID);
			dstUsagesInAcc.get(usrID).data=(Double)dstUsagesInAcc.get(usrID).data/dstUsagesInTotal.get(usrID);
		}
		System.out.println("Total Txns with Src Owned by User: "+totalSrcInUsr+" Total Txns w/ Src: "+totalSrcTxns);
		System.out.println("Total Txns with Dst Owned by User: "+totalDstInUsr+" Total Txns w/ Dst: "+totalDstTxns);
		
		srcFracs=new ArrayList<>(srcUsagesInAcc.values());
		Collections.sort(srcFracs);
		dstFracs=new ArrayList<>(dstUsagesInAcc.values());
		Collections.sort(dstFracs);
		
		String srcMatheStr="{";
		for(int srcInd=srcFracs.size()-1; srcInd>=0; srcInd--)
		{
			srcMatheStr+=srcFracs.get(srcInd).data+",";
		}
		srcMatheStr=srcMatheStr.substring(0, srcMatheStr.length()-1)+"}";
		System.out.println(srcMatheStr);
		
		String dstMatheStr="{";
		for(int dstInd=dstFracs.size()-1; dstInd>=0; dstInd--)
		{
			dstMatheStr+=dstFracs.get(dstInd).data+",";
		}
		dstMatheStr=dstMatheStr.substring(0, dstMatheStr.length()-1)+"}";
		System.out.println(dstMatheStr);
		
		for(String user: totalUsagesInAcc.keySet())
		{
			totalUsagesInAcc.get(user).data=(double)totalUsagesInAcc.get(user).data/*/totalUsagesInTotal.get(user)*/;
		}
		for(String user: srcUsagesInAcc.keySet())
		{
			srcUsagesInAcc.get(user).data=(double)srcUsagesInAcc.get(user).data/*/srcUsagesInTotal.get(user)*/;
		}
		for(String user: dstUsagesInAcc.keySet())
		{
			dstUsagesInAcc.get(user).data=(double)dstUsagesInAcc.get(user).data/*/dstUsagesInTotal.get(user)*/;
		}
		
		return new Hashtable[]{totalUsagesInAcc, srcUsagesInAcc, dstUsagesInAcc};
	}
	
	public static void transactionsByUser(List<String[]> splitData, List<String> lines, String userID)
	{
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		List<IDData<Long>> userTransactions=new ArrayList<>();
		
		for(int lineInd=0; lineInd<splitData.size(); lineInd++)
		{
			String uID=splitData.get(lineInd)[1];
			if(uID.equals(userID))
			{
				long date;
				try 
				{
					date=sdf.parse(splitData.get(lineInd)[4]).getTime();
					userTransactions.add(new IDData<Long>(lines.get(lineInd), date));
				} 
				catch (ParseException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		Collections.sort(userTransactions);
		for(IDData<Long> lineData: userTransactions)
		{
			System.out.println(lineData.ID);
		}
		int u=0;
	}
	
	static void sortUsedEps(List<String[]> splitData)
	{
		Hashtable<Integer, Integer> epIDS=new Hashtable<>();
		for(String[] line: splitData)
		{
			if(!line[2].isEmpty())
			{
				Integer srcID=Integer.parseInt(line[2]);
				epIDS.put(srcID, srcID);
			}
			if(!line[3].isEmpty())
			{
				Integer dstID=Integer.parseInt(line[3]);
				epIDS.put(dstID, dstID);
			}
		}
		List<Integer> usedEPs=new ArrayList<Integer>(epIDS.values());
		Collections.sort(usedEPs);
		for(Integer ep: usedEPs)
		{
			System.out.println(ep);
		}
	}
	
	static void greatestPercentEpsSingleOwner(List<String[]> transactions,
			Hashtable<String, String> epsToUsrs, 
			Hashtable<String, IDData<Double>> totalUsagesInAcc,
			Hashtable<String, IDData<Double>> srcUsagesInAcc,
			Hashtable<String, IDData<Double>> dstUsagesInAcc)
	{
		Hashtable<String, Hashtable<String, IDData<Integer>>> usrUsedTotal=new Hashtable<>();
		Hashtable<String, Integer> usrUsedTotalAll=new Hashtable<>();
		Hashtable<String, Hashtable<String, IDData<Integer>>> usrUsedSrc=new Hashtable<>();
		Hashtable<String, Integer> usrUsedSrcAll=new Hashtable<>();
		Hashtable<String, Hashtable<String, IDData<Integer>>> usrUsedDst=new Hashtable<>();
		Hashtable<String, Integer> usrUsedDstAll=new Hashtable<>();
		for(String[] transaction: transactions)
		{
			String user=transaction[1];
			String src=transaction[2];
			String dst=transaction[3];
			
			String srcOwner=epsToUsrs.get(src);
			if(srcOwner!=null)
			{
				if(usrUsedTotal.get(user)==null)
				{
					usrUsedTotal.put(user, new Hashtable<String, IDData<Integer>>());
					usrUsedTotalAll.put(user, 0);
				}
				if(usrUsedTotal.get(user).get(srcOwner)==null)
				{
					usrUsedTotal.get(user).put(srcOwner, new IDData<Integer>(srcOwner, 0));
				}
				if(usrUsedSrc.get(user)==null)
				{
					usrUsedSrc.put(user, new Hashtable<String, IDData<Integer>>());
					usrUsedSrcAll.put(user, 0);
				}
				if(usrUsedSrc.get(user).get(srcOwner)==null)
				{
					usrUsedSrc.get(user).put(srcOwner, new IDData<Integer>(srcOwner, 0));
				}
				usrUsedTotal.get(user).get(srcOwner).data=(int)usrUsedTotal.get(user).get(srcOwner).data+1;
				usrUsedTotalAll.put(user, usrUsedTotalAll.get(user)+1);
				usrUsedSrc.get(user).get(srcOwner).data=(int)usrUsedSrc.get(user).get(srcOwner).data+1;
				usrUsedSrcAll.put(user, usrUsedSrcAll.get(user)+1);
			}
			
			String dstOwner=epsToUsrs.get(dst);
			if(dstOwner!=null)
			{
				if(usrUsedTotal.get(user)==null)
				{
					usrUsedTotal.put(user, new Hashtable<String, IDData<Integer>>());
					usrUsedTotalAll.put(user, 0);
				}
				if(usrUsedTotal.get(user).get(dstOwner)==null)
				{
					usrUsedTotal.get(user).put(dstOwner, new IDData<Integer>(dstOwner, 0));
				}
				if(usrUsedDst.get(user)==null)
				{
					usrUsedDst.put(user, new Hashtable<String, IDData<Integer>>());
					usrUsedDstAll.put(user, 0);
				}
				if(usrUsedDst.get(user).get(dstOwner)==null)
				{
					usrUsedDst.get(user).put(dstOwner, new IDData<Integer>(dstOwner, 0));
				}
				usrUsedTotal.get(user).get(dstOwner).data=(int)usrUsedTotal.get(user).get(dstOwner).data+1;
				usrUsedTotalAll.put(user, usrUsedTotalAll.get(user)+1);
				usrUsedDst.get(user).get(dstOwner).data=(int)usrUsedDst.get(user).get(dstOwner).data+1;
				usrUsedDstAll.put(user, usrUsedDstAll.get(user)+1);
			}
		}
		
		List<IDData<Double>> totalGreatestPerUsage=new ArrayList<>();
		for(String user: usrUsedTotal.keySet())
		{
			List<IDData<Integer>> usagesByOwner=new ArrayList<>(usrUsedTotal.get(user).values());
			Collections.sort(usagesByOwner);
			totalGreatestPerUsage.add(new IDData<Double>(user, 
					(double)(Integer)usagesByOwner.get(usagesByOwner.size()-1).data/*/usrUsedTotalAll.get(user)*/));
		}
		Collections.sort(totalGreatestPerUsage);
		
		List<IDData<Double>> srcGreatestPerUsage=new ArrayList<>();
		for(String user: usrUsedSrc.keySet())
		{
			List<IDData<Integer>> usagesByOwner=new ArrayList<>(usrUsedSrc.get(user).values());
			Collections.sort(usagesByOwner);
			srcGreatestPerUsage.add(new IDData<Double>(user, 
					(double)(Integer)usagesByOwner.get(usagesByOwner.size()-1).data/*/usrUsedSrcAll.get(user)*/));
		}
		Collections.sort(srcGreatestPerUsage);
		
		List<IDData<Double>> dstGreatestPerUsage=new ArrayList<>();
		for(String user: usrUsedDst.keySet())
		{
			List<IDData<Integer>> usagesByOwner=new ArrayList<>(usrUsedDst.get(user).values());
			Collections.sort(usagesByOwner);
			dstGreatestPerUsage.add(new IDData<Double>(user, 
					(double)(Integer)usagesByOwner.get(usagesByOwner.size()-1).data/*/usrUsedDstAll.get(user)*/));
		}
		Collections.sort(dstGreatestPerUsage);
		
		DecimalFormat df=new DecimalFormat("#.000"); 
		double avgTotalGU=0.0;
		double totalGU=0.0;
		String totalGUMathe="{";
		for(int ind=totalGreatestPerUsage.size()-1; ind>=0; ind--)
		{
			avgTotalGU+=(double)totalGreatestPerUsage.get(ind).data;
			totalGU+=(double)usrUsedTotalAll.get(totalGreatestPerUsage.get(ind).ID);
			totalGUMathe+="{"+df.format(totalUsagesInAcc.get(totalGreatestPerUsage.get(ind).ID).data)
				+","+df.format(((double)totalGreatestPerUsage.get(ind).data
						-(double)totalUsagesInAcc.get(totalGreatestPerUsage.get(ind).ID).data))+"},";
		}
		totalGUMathe=totalGUMathe.substring(0, totalGUMathe.length()-2)+"}";
		System.out.println(totalGUMathe);
		avgTotalGU/=totalGU;
		
		double avgSrcGU=0.0;
		String srcGUMathe="{";
		for(int ind=srcGreatestPerUsage.size()-1; ind>=0; ind--)
		{
			avgSrcGU+=(double)srcGreatestPerUsage.get(ind).data;
			srcGUMathe+="{"+df.format(srcUsagesInAcc.get(srcGreatestPerUsage.get(ind).ID).data)
					+","+df.format(((double)srcGreatestPerUsage.get(ind).data
							-(double)srcUsagesInAcc.get(srcGreatestPerUsage.get(ind).ID).data))+"},";
		}
		srcGUMathe=srcGUMathe.substring(0, srcGUMathe.length()-2)+"}";
		System.out.println(srcGUMathe);
		avgSrcGU/=srcGreatestPerUsage.size();
		
		double avgDstGU=0.0;
		String dstGUMathe="{";
		for(int ind=dstGreatestPerUsage.size()-1; ind>=0; ind--)
		{
			avgDstGU+=(double)dstGreatestPerUsage.get(ind).data;
			dstGUMathe+="{"+df.format(dstUsagesInAcc.get(dstGreatestPerUsage.get(ind).ID).data)
					+","+df.format(((double)dstGreatestPerUsage.get(ind).data
							-(double)dstUsagesInAcc.get(dstGreatestPerUsage.get(ind).ID).data))+"},";
		}
		dstGUMathe=dstGUMathe.substring(0, dstGUMathe.length()-2)+"}";
		System.out.println(dstGUMathe);
		avgDstGU/=dstGreatestPerUsage.size();
		
		System.out.println("Avg Total Greatest Fraction EP from Same Owner: "+avgTotalGU);
		System.out.println("Avg Src Greatest Fraction EP from Same Owner: "+avgSrcGU);
		System.out.println("Avg Dst Greatest Fraction EP from Same Owner: "+avgDstGU);
		
	}
	
	static void transactionsOnOwnedEps(List<String[]> splitData, Hashtable<String, String> machineIDToUserID)
	{
		Hashtable<String, Integer> numberTransactionsOwnedByUser=new Hashtable<>();
		Hashtable<String, Integer> numberTransactionsByUser=new Hashtable<>();
		
		for(String[] transaction: splitData)
		{
			String user=transaction[1];
			if(numberTransactionsByUser.get(user)==null)
			{
				numberTransactionsByUser.put(user, 0);
			}
			if(numberTransactionsOwnedByUser.get(user)==null)
			{
				numberTransactionsOwnedByUser.put(user, 0);
			}
			numberTransactionsByUser.put(user, numberTransactionsByUser.get(user)+1);
			
			String src=transaction[2];
			String dst=transaction[3];
			
			String srcOwner=machineIDToUserID.get(src);
			if(srcOwner!=null)
			{
				if(numberTransactionsOwnedByUser.get(srcOwner)==null)
				{
					numberTransactionsOwnedByUser.put(srcOwner, 0);
				}
				if(numberTransactionsByUser.get(srcOwner)==null)
				{
					numberTransactionsByUser.put(srcOwner, 0);
				}
				numberTransactionsOwnedByUser.put(srcOwner, numberTransactionsOwnedByUser.get(srcOwner)+1);
			}
			
			String dstOwner=machineIDToUserID.get(dst);
			if(dstOwner!=null)
			{
				if(numberTransactionsOwnedByUser.get(dstOwner)==null)
				{
					numberTransactionsOwnedByUser.put(dstOwner, 0);
				}
				if(numberTransactionsByUser.get(dstOwner)==null)
				{
					numberTransactionsByUser.put(dstOwner, 0);
				}
				numberTransactionsOwnedByUser.put(dstOwner, numberTransactionsOwnedByUser.get(dstOwner)+1);
			}
		}
		
		String matheStr="{";
		for(String user: numberTransactionsOwnedByUser.keySet())
		{
			matheStr+="{"+numberTransactionsOwnedByUser.get(user)+","+numberTransactionsByUser.get(user)+"},";
		}
		matheStr=matheStr.substring(0, matheStr.length()-1)+"}";
		System.out.println(matheStr);
	}
	
}