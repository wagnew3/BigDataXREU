package userSrcDstPrediction;

import init.IDData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
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

import validation.NActionsValidator;
import activationFunctions.RectifiedLinearActivationFunction;
import activationFunctions.Sigmoid;
import activationFunctions.SoftMax;
import costFunctions.CrossEntropy;
import costFunctions.EuclideanDistanceCostFunction;
import filters.ScaleFilter;
import layer.BInputLayer;
import layer.BLayer;
import layer.FullyConnectedBLayer;
import learningRule.BPGDUnsupervisedTraining;
import learningRule.BPGDUnsupervisedTrainingNestrov;
import learningRule.RProp;
import learningRule.RPropMultithreaded;
import nDimensionalMatrices.FDMatrix;
import nDimensionalMatrices.Matrix;
import network.SplitFeedForwardNetwork;
import network.SplitNetwork;

public class EnsembleNeuralNetwork extends EPHeuristic
{
	
	InstitutionHeuristic instH;
	
	String saveLocation=currentDir+"/data/SavedNetworks/";
	
	SplitNetwork srcNetwork;
	ScaleFilter scaleSrcInputs;
	
	EPHeuristic[] heuristics;
	int topN;
	static List<String[]> trainingTransactions;
	int amtAddInfo=0;
	float dateDiv=3*1465833177756L;
	
	float minFreq=10;
	float minVol=1000000;
	
	float maxSrcTransactionFreq=0.0f;
	Hashtable<String, Integer> srcTransactionFreqs;
	float maxSrcTransactionVol=0.0f;
	Hashtable<String, Long> srcTransactionVols;
	float maxDstTransactionFreq=0.0f;
	Hashtable<String, Integer> dstTransactionFreqs;
	float maxDstTransactionVol=0.0f;
	Hashtable<String, Long> dstTransactionVols;
	float maxUserTransactionFreq=0.0f;
	Hashtable<String, Integer> userTransactionFreqs;
	float maxUserTransactionVol=0.0f;
	Hashtable<String, Long> userTransactionVols;
	
	Hashtable<String, float[]>[] heuristicsPerformanceByUser;
	
	int inputLength=0;
	
	public EnsembleNeuralNetwork(List<String[]> transactions, List<String[]> deletions, 
			List<String[]> users, List<String[]> authIDs, long date, EPHeuristic[] heuristics,
			int topN)
	{
		super(transactions, deletions, users, authIDs, date);
		
		scaleSrcInputs=new ScaleFilter();
		
		srcTransactionFreqs=new Hashtable<>();
		srcTransactionVols=new Hashtable<>();
		dstTransactionFreqs=new Hashtable<>();
		dstTransactionVols=new Hashtable<>();
		userTransactionFreqs=new Hashtable<>();
		userTransactionVols=new Hashtable<>();
		
		this.trainingTransactions=transactions;
		
		heuristicsPerformanceByUser=new Hashtable[heuristics.length];
		for(int hpbuInd=0; hpbuInd<heuristicsPerformanceByUser.length; hpbuInd++)
		{
			heuristicsPerformanceByUser[hpbuInd]=new Hashtable<>();
		}
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		for(String[] line: transactions)
		{
			String user=line[2];
			String src=line[14];
			String dst=line[15];
			Long amt=Long.parseLong(line[37]);
			Long currentDate=null;
			try 
			{
				currentDate=sdf.parse(line[3]).getTime();
				if(currentDate>=date)
				{
					break;
				}
				
				if(srcTransactionFreqs.get(src)==null)
				{
					srcTransactionFreqs.put(src, 0);
				}
				srcTransactionFreqs.put(src, srcTransactionFreqs.get(src)+1);
				if(srcTransactionVols.get(src)==null)
				{
					srcTransactionVols.put(src, 0L);
				}
				srcTransactionVols.put(src, srcTransactionVols.get(src)+amt);
				if(dstTransactionFreqs.get(dst)==null)
				{
					dstTransactionFreqs.put(dst, 0);
				}
				dstTransactionFreqs.put(dst, dstTransactionFreqs.get(dst)+1);
				if(dstTransactionVols.get(dst)==null)
				{
					dstTransactionVols.put(dst, 0L);
				}
				dstTransactionVols.put(dst, dstTransactionVols.get(dst)+amt);
				
				if(userTransactionFreqs.get(user)==null)
				{
					userTransactionFreqs.put(user, 0);
				}
				userTransactionFreqs.put(user, userTransactionFreqs.get(user)+1);
				if(userTransactionVols.get(user)==null)
				{
					userTransactionVols.put(user, 0L);
				}
				userTransactionVols.put(user, userTransactionVols.get(user)+amt);
			}
			catch (ParseException e)
			{
				e.printStackTrace();
			}
			
//			for(EPHeuristic heuristc: heuristics)
//			{
//				heuristc.nextTxn();
//			}
		}
		
		for(String srcID: srcTransactionFreqs.keySet())
		{
			if(srcTransactionFreqs.get(srcID)>maxSrcTransactionFreq)
			{
				maxSrcTransactionFreq=srcTransactionFreqs.get(srcID);
			}
			if(srcTransactionVols.get(srcID)>maxSrcTransactionVol)
			{
				maxSrcTransactionVol=srcTransactionVols.get(srcID);
			}
		}
		for(String dstID: dstTransactionFreqs.keySet())
		{
			if(dstTransactionFreqs.get(dstID)>maxDstTransactionFreq)
			{
				maxDstTransactionFreq=dstTransactionFreqs.get(dstID);
			}
			if(dstTransactionVols.get(dstID)>maxDstTransactionVol)
			{
				maxDstTransactionVol=dstTransactionVols.get(dstID);
			}
		}
		for(String userID: userTransactionFreqs.keySet())
		{
			if(userTransactionFreqs.get(userID)>maxUserTransactionFreq)
			{
				maxUserTransactionFreq=userTransactionFreqs.get(userID);
			}
			if(userTransactionVols.get(userID)>maxUserTransactionVol)
			{
				maxUserTransactionVol=userTransactionVols.get(userID);
			}
		}
		
		this.heuristics=heuristics;
		this.topN=topN;

		for(EPHeuristic heur: heuristics)
		{
			if(heur instanceof InstitutionHeuristic)
			{
				instH=(InstitutionHeuristic)heur;
			}
		}
	}
	
	@Override
	public Object[] getNthBestSrc(String userID, int n, long date) 
	{
		//return heuristics[1].getNthBestSrc(userID, n, date);
		
		String[][] hRecs=new String[heuristics.length][topN];
		Matrix[] input=new Matrix[heuristics.length+1];
		for(int hInd=0; hInd<heuristics.length; hInd++)
		{
			input[hInd]=new FDMatrix(topN, 1);
			for(int recN=0; recN<topN; recN++)
			{
				Object[] rec=heuristics[hInd].getNthBestSrc(userID, recN, date);
				hRecs[hInd][recN]=(String)rec[0];
				input[hInd].set(recN, 0, (float)rec[1]);
			}
		}
		input[heuristics.length]=new FDMatrix(heuristics.length+1+2*heuristics.length*topN, 1);
		input[heuristics.length].set(0, 0, date/dateDiv);
		for(int addInd=0; addInd<heuristics.length; addInd++)
		{
			input[heuristics.length].set(addInd+1, 0, heuristics[addInd].getAdditionalNetInfo(userID, true));
		}
		for(int numTransfersInd=heuristics.length+1; 
				numTransfersInd<heuristics.length+1+heuristics.length*topN; numTransfersInd++)
		{
			input[heuristics.length].set(numTransfersInd, 0, 
					(float)(int)srcTransactionFreqs.getOrDefault(hRecs[(numTransfersInd-(heuristics.length+1))/topN][(numTransfersInd-(heuristics.length+1))%topN], 0)
						/Math.max(maxSrcTransactionFreq, minFreq));
		}
		for(int numTransfersInd=heuristics.length+1+heuristics.length*topN; 
				numTransfersInd<heuristics.length+1+2*heuristics.length*topN; numTransfersInd++)
		{
			input[heuristics.length].set(numTransfersInd, 0, 
					(float)(long)srcTransactionVols.getOrDefault((hRecs[(numTransfersInd-(heuristics.length+1+heuristics.length*topN))/topN]
															  [(numTransfersInd-(heuristics.length+1+heuristics.length*topN))%topN]), 0L)
							/Math.max(maxSrcTransactionVol, minVol));
		}
		
		input=scaleSrcInputs.scaleData(input);
		
		Matrix[] output=srcNetwork.getOutput(input);
		
		List<IDData<Float>> recs=new ArrayList<>();
		for(int outputInd=0; outputInd<output[0].getLen(); outputInd++)
		{
			IDData<Float> newRec=new IDData<Float>(hRecs[outputInd/topN][outputInd%topN],
					output[0].get(outputInd, 0));
			if(!recs.contains(newRec))
			{
				recs.add(new IDData<Float>(hRecs[outputInd/topN][outputInd%topN],
							output[0].get(outputInd, 0)));
			}
		}
		Collections.sort(recs);
		
		String recID="";
		float recVal=-1;
		if(n<recs.size())
		{
			recID=recs.get(recs.size()-1-n).ID;
			recVal=(float)recs.get(recs.size()-1-n).data;
		}
		
		if(recID.isEmpty())
		{
			int u=0;
			timesEmpty++;
		}
		
		//recID="";
		if(!recID.equals((String)(heuristics[0].getNthBestSrc(userID, 0, date))[0]))
		{
			int u=0;
		}
		return new Object[]{recID, recVal};
	}
	
	int timesEmpty=0;

	@Override
	public float getSrcWeight(String userID, String srcID) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object[] getNthBestDst(String userID, int n, long date) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getDstWeight(String userID, String dstID) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void updateHeuristic(String[] newTransaction) 
	{
		String user=newTransaction[2];
		String src=newTransaction[14];
		String dst=newTransaction[15];
		Long amt=Long.parseLong(newTransaction[37]);
		if(srcTransactionFreqs.get(src)==null)
		{
			srcTransactionFreqs.put(src, 0);
		}
		srcTransactionFreqs.put(src, srcTransactionFreqs.get(src)+1);
		if(srcTransactionVols.get(src)==null)
		{
			srcTransactionVols.put(src, 0L);
		}
		srcTransactionVols.put(src, srcTransactionVols.get(src)+amt);
		if(dstTransactionFreqs.get(dst)==null)
		{
			dstTransactionFreqs.put(dst, 0);
		}
		dstTransactionFreqs.put(dst, dstTransactionFreqs.get(dst)+1);
		if(dstTransactionVols.get(dst)==null)
		{
			dstTransactionVols.put(dst, 0L);
		}
		dstTransactionVols.put(dst, dstTransactionVols.get(dst)+amt);
		if(userTransactionFreqs.get(user)==null)
		{
			userTransactionFreqs.put(user, 0);
		}
		userTransactionFreqs.put(user, userTransactionFreqs.get(user)+1);
		if(userTransactionVols.get(user)==null)
		{
			userTransactionVols.put(user, 0L);
		}
		userTransactionVols.put(user, userTransactionVols.get(user)+amt);
		
		if(srcTransactionFreqs.get(src)>maxSrcTransactionFreq)
		{
			maxSrcTransactionFreq=srcTransactionFreqs.get(src);
		}
		if(srcTransactionVols.get(src)>maxSrcTransactionVol)
		{
			maxSrcTransactionVol=srcTransactionVols.get(src);
		}
		if(dstTransactionFreqs.get(dst)>maxDstTransactionFreq)
		{
			maxDstTransactionFreq=dstTransactionFreqs.get(dst);
		}
		if(dstTransactionVols.get(dst)>maxDstTransactionVol)
		{
			maxDstTransactionVol=dstTransactionVols.get(dst);
		}
		if(userTransactionFreqs.get(user)>maxUserTransactionFreq)
		{
			maxUserTransactionFreq=userTransactionFreqs.get(user);
		}
		if(userTransactionVols.get(user)>maxUserTransactionVol)
		{
			maxUserTransactionVol=userTransactionVols.get(user);
		}
		
		for(EPHeuristic heuristc: heuristics)
		{
			heuristc.nextTxn();
		}
	}
	
	public static String networkName="";
	
	public void init(int amtTData, List<String[]> trainingTransactions, boolean ldData, boolean ldNet)
	{
		networkName="savedNetEPPred"+amtTData+"t"+topN+"MCHISTINSMUUEPUEPSplit2.3";
		String filterName="savedFilterEPPred"+amtTData+"t"+topN+"MCHISTINSMUUEPUEP";
		String dataName="savedDataEPPred"+amtTData+"t"+topN+"MCHISTINSMUUEPUEPIndOutputs";
		this.trainingTransactions=trainingTransactions;
		inputLength=heuristics.length*heuristics.length*topN
				+heuristics.length+7+2*heuristics.length*topN+heuristics.length+heuristics.length*topN;
		
//		Matrix[][][] trainingData=null;
//		Matrix[][][] validationData=null;
//		if(!ldData)
//		{
//			trainingData=generateTrainingDataSrcByUser(heuristics, amtTData, 0, "", false);
//			saveData(dataName+"train", trainingData);
//			validationData=generateTrainingDataSrcByUser(heuristics, trainingTransactions.size()-amtTData, amtTData, "", false);
//			saveData(dataName+"validate", validationData);
//		}
//		else
//		{
//			trainingData=loadData(dataName+"train");
//			validationData=loadData(dataName+"validate");
//		}
//		Matrix[][] inputs=trainingData[0];
//		Matrix[][] outputs=trainingData[1];                                   //two dates in bad format
//		Matrix[][] validationInputs=validationData[0];
//		Matrix[][] validationOutputs=validationData[1];
//		
//		if(!ldNet)
//		{
//			srcNetwork=createNetwork();		
//		}
//		else
//		{
//			srcNetwork=SplitNetwork.loadNetwork(new File(saveLocation+networkName));
//			scaleSrcInputs=ScaleFilter.loadFilter(new File(saveLocation+filterName));
//		}
//		
//		trainNetwork(inputs, outputs, validationInputs, validationOutputs, srcNetwork);
//		SplitNetwork.saveNetwork(new File(saveLocation+networkName), srcNetwork);
//		ScaleFilter.saveFilter(new File(saveLocation+filterName), scaleSrcInputs);
//		System.out.println("saved");
//		System.out.println("sfi: "+scaleSrcInputs.scaleFactor);
	}
	
	private void saveData(String dataName, Matrix[][][] data)
	{
		try 
		{
			ObjectOutputStream oOut=new ObjectOutputStream(new FileOutputStream(new File(saveLocation+dataName)));
			oOut.writeObject(data);
			oOut.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		} 
	}
	
	private Matrix[][][] loadData(String dataName)
	{
		try 
		{
			ObjectInputStream oIn=new ObjectInputStream(new FileInputStream(new File(saveLocation+dataName)));
			Matrix[][][] data=(Matrix[][][])oIn.readObject();
			oIn.close();
			return data;
		} 
		catch (IOException | ClassNotFoundException e) 
		{
			e.printStackTrace();
			return null;
		} 
	}
	
	Hashtable<String, Integer> historySizes=new Hashtable<>();
	
	Matrix[][][] generateTrainingDataSrcByUser(EPHeuristic[] heuristics, int amtTData, int offset, 
			String saveName, boolean save)
	{
		if(trainingTransactions.size()<amtTData)
		{
			System.out.println("not enough data to generate training data!");
		}
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		
		Hashtable<String, List<float[]>> inputsByUser=new Hashtable<>();
		Hashtable<String, List<float[]>> outputsByUser=new Hashtable<>();
		Hashtable<String, List<String[]>> recsByUser=new Hashtable<>();
		Hashtable<String, List<String>> historySizesByUser=new Hashtable<>();
				
		int sampleAddedInd=0;
		int tDataInd=offset;
		long date;
		while(sampleAddedInd<amtTData)
		{
			try 
			{
				date=sdf.parse(trainingTransactions.get(tDataInd)[3]).getTime();
				
				sampleAddedInd++;
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			tDataInd++;
		}
		
		Matrix[][] inputs=new Matrix[amtTData][];
		Matrix[][] outputs=new Matrix[amtTData][];
		sampleAddedInd=0;
		tDataInd=offset;
		
		while(sampleAddedInd<amtTData
				&& tDataInd<trainingTransactions.size())
		{
			String userID=trainingTransactions.get(tDataInd)[2];
			String srcID=trainingTransactions.get(tDataInd)[14];
			int historySize=historySizes.getOrDefault(userID, 0);
			historySizes.put(userID, historySizes.getOrDefault(userID, 0)+1);
			if(!srcID.isEmpty())
			{
				try 
				{
					date=sdf.parse(trainingTransactions.get(tDataInd)[3]).getTime();
					String[][] hRecs=new String[heuristics.length][topN];
					List<String> recEpsArray=new ArrayList<String>();
					
					Matrix[] input=new Matrix[heuristics.length+1];
					for(int hInd=0; hInd<heuristics.length; hInd++)
					{
						input[hInd]=new FDMatrix(topN*heuristics.length, 1);
						int numberAdded=0;
						int n=0;
						Object[] rec=heuristics[hInd].getNthBestSrc(userID, n, date);
						String prevRec="\n";
						while(numberAdded<topN && !prevRec.equals((String)rec[0]))
						{
							if(!recEpsArray.contains(((String)rec[0])))
							{
								hRecs[hInd][numberAdded]=(String)rec[0];
								recEpsArray.add((String)rec[0]);
								input[hInd].set(hInd*topN+numberAdded, 0, (float)rec[1]);
								numberAdded++;
								if(numberAdded>=topN)
								{
									break;
								}
							}
							prevRec=(String)rec[0];
							n++;
							rec=heuristics[hInd].getNthBestSrc(userID, n, date);
						}
					}
					if(recsByUser.get(userID)==null)
					{
						recsByUser.put(userID, new ArrayList<>());
					}
					recsByUser.get(userID).add(recEpsArray.toArray(new String[0]));
					
					for(int hInd=0; hInd<heuristics.length; hInd++)
					{
						for(int oHInd=0; oHInd<heuristics.length; oHInd++)
						{
							if(hInd!=oHInd)
							{
								for(int recN=0; recN<topN; recN++)
								{
									if(hRecs[oHInd][recN]!=null)
									{
										input[hInd].set(oHInd*topN+recN, 0, heuristics[hInd].getSrcWeight(userID, hRecs[oHInd][recN]));
										if(hInd==0 && heuristics[hInd].getSrcWeight(userID, hRecs[oHInd][recN])==0.5)
										{
											heuristics[hInd].getSrcWeight(userID, hRecs[oHInd][recN]);
										}
									}
								}
							}
						}
					}
					
					input[heuristics.length]=new FDMatrix(heuristics.length+7+2*heuristics.length*topN+heuristics.length+heuristics.length*topN, 1);
					input[heuristics.length].set(0, 0, date/dateDiv);
					if(instH.userInstitutionType.get(userID)!=null)
					{
						input[heuristics.length].set(instH.userInstitutionType.get(userID)+1, 0, 1); //institution type
					}
					if(instH.userToInstitution.get(userID)!=null)
					{
						input[heuristics.length].set(5, 0, (float)(instH.institutionSize.get(instH.userToInstitution.get(userID))/instH.maxInstSize)); //institution size (number users)
					}
					input[heuristics.length].set(6, 0, (float)(userTransactionFreqs.getOrDefault(userID, 0)/Math.max(maxUserTransactionFreq, minFreq)));
					input[heuristics.length].set(7, 0, (float)(userTransactionVols.getOrDefault(userID, 0L)/Math.max(maxUserTransactionVol, minVol)));
					for(int addInd=0; addInd<heuristics.length; addInd++)
					{
						input[heuristics.length].set(addInd+7, 0, heuristics[addInd].getAdditionalNetInfo(userID, true));
					}
					for(int numTransfersInd=heuristics.length+7; 
							numTransfersInd<heuristics.length+7+heuristics.length*topN; numTransfersInd++)
					{
						if(hRecs[(numTransfersInd-(heuristics.length+7))/topN][(numTransfersInd-(heuristics.length+1))%topN]!=null)
						{
							input[heuristics.length].set(numTransfersInd, 0, 
									(float)(int)srcTransactionFreqs.getOrDefault(hRecs[(numTransfersInd-(heuristics.length+7))/topN][(numTransfersInd-(heuristics.length+1))%topN], 0)
										/Math.max(maxSrcTransactionFreq, minFreq));
						}
					}
					for(int numTransfersInd=heuristics.length+7+heuristics.length*topN; 
							numTransfersInd<heuristics.length+7+2*heuristics.length*topN; numTransfersInd++)
					{
						if(hRecs[(numTransfersInd-(heuristics.length+7+heuristics.length*topN))/topN]
								  [(numTransfersInd-(heuristics.length+7+heuristics.length*topN))%topN]!=null)
						{
							input[heuristics.length].set(numTransfersInd, 0, 
									(float)(long)srcTransactionVols.getOrDefault((hRecs[(numTransfersInd-(heuristics.length+7+heuristics.length*topN))/topN]
																			  [(numTransfersInd-(heuristics.length+7+heuristics.length*topN))%topN]), 0L)
											/Math.max(maxSrcTransactionVol, minVol));
						}
					}
					for(int numTransfersInd=heuristics.length+7+2*heuristics.length*topN; 
							numTransfersInd<heuristics.length+7+2*heuristics.length*topN+heuristics.length;
							numTransfersInd++)
					{
						input[heuristics.length].set(numTransfersInd, 0, 
								heuristicsPerformanceByUser[(numTransfersInd-(heuristics.length+7+2*heuristics.length*topN))]
										.getOrDefault(userID, new float[]{0.0f, 1.0f})[0]
								/
								heuristicsPerformanceByUser[(numTransfersInd-(heuristics.length+7+2*heuristics.length*topN))]
										.getOrDefault(userID, new float[]{0.0f, 1.0f})[1]);
					}
					if(outputsByUser.get(userID)!=null)
					{
						float[] lastOutput=outputsByUser.get(userID).get(outputsByUser.get(userID).size()-1);
						for(int numTransfersInd=heuristics.length+7+2*heuristics.length*topN+heuristics.length; 
								numTransfersInd<heuristics.length+7+2*heuristics.length*topN+heuristics.length+heuristics.length*topN;
								numTransfersInd++)
						{
							input[heuristics.length].set(numTransfersInd, 0, 
									lastOutput[numTransfersInd-(heuristics.length+7+2*heuristics.length*topN+heuristics.length)]);
						}
					}
					
					inputs[sampleAddedInd]=input;
					
					boolean[] set=new boolean[heuristics.length];

					outputs[sampleAddedInd]=new Matrix[1];
					//outputs[sampleAddedInd][0]=new FDMatrix(heuristics.length*topN+1, 1);
					outputs[sampleAddedInd][0]=new FDMatrix(heuristics.length*topN, 1);
					float numberSet=0.0f;
					for(int recLevel=0; recLevel<topN; recLevel++)
					{
						for(int hInd=0; hInd<heuristics.length; hInd++)
						{
							if(hRecs[hInd][recLevel]!=null && hRecs[hInd][recLevel].equals(srcID))
							{
								outputs[sampleAddedInd][0].set(hInd*topN+recLevel, 0,
										1.0f);
								numberSet++;
								
								if(!set[hInd])
								{
									set[hInd]=true;
									if(heuristicsPerformanceByUser[hInd].get(userID)==null)
									{
										heuristicsPerformanceByUser[hInd].put(userID, new float[2]);
									}
									heuristicsPerformanceByUser[hInd].get(userID)[0]++;
								}
							}
						}
					}
					
					
					/*
					outputs[sampleAddedInd]=new Matrix[heuristics.length];
					//outputs[sampleAddedInd][0]=new FDMatrix(heuristics.length*topN+1, 1);
					
					float numberSet=0.0f;
					for(int hInd=0; hInd<heuristics.length; hInd++)
					{
						outputs[sampleAddedInd][hInd]=new FDMatrix(topN, 1);
						for(int recLevel=0; recLevel<topN; recLevel++)
						{
							if(hRecs[hInd][recLevel].equals(srcID))
							{
								outputs[sampleAddedInd][hInd].set(recLevel, 0,
										1.0f);
								numberSet++;
								
								if(!set[hInd])
								{
									set[hInd]=true;
									if(heuristicsPerformanceByUser[hInd].get(userID)==null)
									{
										heuristicsPerformanceByUser[hInd].put(userID, new float[2]);
									}
									heuristicsPerformanceByUser[hInd].get(userID)[0]++;
								}
							}
						}
					}
					*/
					
					if(numberSet>0 && save)
					{
						for(int outputInd=0; outputInd<outputs[sampleAddedInd][0].getLen(); outputInd++)
						{
							outputs[sampleAddedInd][0].set(outputInd, 0,
									outputs[sampleAddedInd][0].get(outputInd, 0)/numberSet);
						}
					}
					else
					{
						/*
						for(int outputInd=0; outputInd<outputs[sampleAddedInd][0].getLen(); outputInd++)
						{
							outputs[sampleAddedInd][0].set(heuristics.length*topN, 0, 1.0f);
						}
						*/
					}
					
					if(numberSet>0)
					{
						for(int hInd=0; hInd<heuristics.length; hInd++)
						{
							if(heuristicsPerformanceByUser[hInd].get(userID)==null)
							{
								heuristicsPerformanceByUser[hInd].put(userID, new float[2]);
							}
							heuristicsPerformanceByUser[hInd].get(userID)[1]++;
						}			
					}
					
					int inputSize=0;
					for(int inputInd=0; inputInd<inputs[sampleAddedInd].length; inputInd++)
					{
						inputSize+=inputs[sampleAddedInd][inputInd].getLen();
					}
					float[] inputsArray=new float[inputSize];
					inputSize=0;
					for(int inputInd=0; inputInd<inputs[sampleAddedInd].length; inputInd++)
					{
						for(int matInd=0; matInd<inputs[sampleAddedInd][inputInd].getLen(); matInd++)
						{
							inputsArray[inputSize]=inputs[sampleAddedInd][inputInd].get(matInd, 0);
							inputSize++;
						}
					}
					if(inputsByUser.get(userID)==null)
					{
						inputsByUser.put(userID, new ArrayList<float[]>());
					}
					inputsByUser.get(userID).add(inputsArray);
					
					float[] outputsArray=new float[outputs[sampleAddedInd][0].getLen()];
					for(int matInd=0; matInd<outputs[sampleAddedInd][0].getLen(); matInd++)
					{
						outputsArray[matInd]=outputs[sampleAddedInd][0].get(matInd, 0);
					}
					if(outputsByUser.get(userID)==null)
					{
						outputsByUser.put(userID, new ArrayList<float[]>());
					}
					outputsByUser.get(userID).add(outputsArray);
					for(int inInd=0; inInd<outputsArray.length; inInd++)
					{
						if(!Float.isFinite(outputsArray[inInd]))
						{
							int y=0;
						}
					}
					
					//outputs[sampleAddedInd][0].set(0, 0, 1.0f);
					//outputs[sampleAddedInd][0].set(1, 0, 0.0f);
	
					if(historySizesByUser.get(userID)==null)
					{
						historySizesByUser.put(userID, new ArrayList<String>());
					}
					historySizesByUser.get(userID).add(""+historySize);
					
					sampleAddedInd++;
					if(sampleAddedInd%10000==0)
					{
						System.out.println(sampleAddedInd);
					}
				} 
				catch (ParseException e) 
				{
					e.printStackTrace();
				}
			}
			nextTxn();	
			tDataInd++;
		}
		
		inputs=scaleSrcInputs.scaleData(inputs, true);
		for(List<float[]> userInputs: inputsByUser.values())
		{
			for(float[] input: userInputs)
			{
				for(int inputInd=0; inputInd<input.length; inputInd++)
				{
					input[inputInd]*=scaleSrcInputs.scaleFactor;
				}
			}
		}

		System.out.println("Generated "+amtTData);
		
		if(save)
		{
			BufferedWriter fDatasout=null;
			BufferedWriter fRecsout=null;
			BufferedWriter fHistOut=null;
			try 
			{
				fDatasout=new BufferedWriter(new FileWriter(new File("/home/willie/workspace/Globus/data/heuristicsData"+saveName)));
				fRecsout=new BufferedWriter(new FileWriter(new File("/home/willie/workspace/Globus/data/heuristicsRecs"+saveName)));
				fHistOut=new BufferedWriter(new FileWriter(new File("/home/willie/workspace/Globus/data/heuristicsHistSizes"+saveName)));
			} 
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
			
			DecimalFormat df=new DecimalFormat("#0.000000"); 
			try
			{
				for(String userID: inputsByUser.keySet())
				{
					fDatasout.write("user "+userID+"\n");
					fRecsout.write("user "+userID+"\n");
					fHistOut.write("user "+userID+"\n");
					
					List<String> userHistSizes=historySizesByUser.get(userID);
					for(String histSize: userHistSizes)
					{
						fHistOut.write(histSize+"\n");
					}
					
					List<String[]> userRecs=recsByUser.get(userID);
					for(String[] recs: userRecs)
					{
						String recString="";
						for(String rec: recs)
						{
							recString+=rec+",";
						}
						recString=recString.substring(0, recString.length()-1);
						fRecsout.write(recString+"\n");
					}
					
					List<float[]> userInputs=inputsByUser.get(userID);
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
					for(float[] input: outputsByUser.get(userID))
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
				fRecsout.close();
				fDatasout.close();
				fHistOut.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		return new Matrix[][][]{inputs, outputs};
	}
	
	Matrix[][][] generateTrainingDataDstByUser(EPHeuristic[] heuristics, int amtTData, int offset, 
			String saveName, boolean save)
	{
		if(trainingTransactions.size()<amtTData)
		{
			System.out.println("not enough data to generate training data!");
		}
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		
		Hashtable<String, List<float[]>> inputsByUser=new Hashtable<>();
		Hashtable<String, List<float[]>> outputsByUser=new Hashtable<>();
		Hashtable<String, List<String[]>> recsByUser=new Hashtable<>();
		Hashtable<String, List<String>> historySizesByUser=new Hashtable<>();
		
		int sampleAddedInd=0;
		int tDataInd=offset;
		long date;
		while(sampleAddedInd<amtTData)
		{
			try 
			{
				date=sdf.parse(trainingTransactions.get(tDataInd)[3]).getTime();
				sampleAddedInd++;
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			tDataInd++;
		}
		
		Matrix[][] inputs=new Matrix[amtTData][];
		Matrix[][] outputs=new Matrix[amtTData][];
		sampleAddedInd=0;
		tDataInd=offset;
		
		while(tDataInd<amtTData+offset
				&& tDataInd<trainingTransactions.size())
		{
			String userID=trainingTransactions.get(tDataInd)[2];
			String dstID=trainingTransactions.get(tDataInd)[15];
			int historySize=historySizes.getOrDefault(userID, 0);
			historySizes.put(userID, historySizes.getOrDefault(userID, 0)+1);
			if(!dstID.isEmpty())
			{
				try 
				{
					date=sdf.parse(trainingTransactions.get(tDataInd)[3]).getTime();					
					
					String[][] hRecs=new String[heuristics.length][topN];
					List<String> recEpsArray=new ArrayList<String>();
					Matrix[] input=new Matrix[heuristics.length+1];
					for(int hInd=0; hInd<heuristics.length; hInd++)
					{
						input[hInd]=new FDMatrix(topN*heuristics.length, 1);
						int numberAdded=0;
						int n=0;
						Object[] rec=heuristics[hInd].getNthBestDst(userID, n, date);
						String prevRec="\n";
						while(numberAdded<topN && !prevRec.equals((String)rec[0]))
						{
							if(!recEpsArray.contains(((String)rec[0])))
							{
								hRecs[hInd][numberAdded]=(String)rec[0];
								recEpsArray.add((String)rec[0]);
								input[hInd].set(hInd*topN+numberAdded, 0, (float)rec[1]);
								numberAdded++;
								if(numberAdded>=topN)
								{
									break;
								}
							}
							prevRec=(String)rec[0];
							n++;
							//rec=heuristics[hInd].getNthBestDst(userID, n, date);
							if(hInd==0)
							{
								rec=histH.getNthBestDst(userID, n, date);
							}
							else
							{
								rec=heuristics[hInd].getNthBestDst(userID, n, date);
							}
						}
					}
					if(recsByUser.get(userID)==null)
					{
						recsByUser.put(userID, new ArrayList<>());
					}
					recsByUser.get(userID).add(recEpsArray.toArray(new String[0]));
					
					for(int hInd=0; hInd<heuristics.length; hInd++)
					{
						for(int oHInd=0; oHInd<heuristics.length; oHInd++)
						{
							if(hInd!=oHInd)
							{
								for(int recN=0; recN<topN; recN++)
								{
									if(hRecs[oHInd][recN]!=null)
									{
										input[hInd].set(oHInd*topN+recN, 0, heuristics[hInd].getDstWeight(userID, hRecs[oHInd][recN]));
										if(hInd==0 && heuristics[hInd].getDstWeight(userID, hRecs[oHInd][recN])==0.5)
										{
											heuristics[hInd].getDstWeight(userID, hRecs[oHInd][recN]);
										}
									}
								}
							}
						}
					}
					
					input[heuristics.length]=new FDMatrix(heuristics.length+7+2*heuristics.length*topN+heuristics.length+heuristics.length*topN, 1);
					input[heuristics.length].set(0, 0, date/dateDiv);
					if(instH.userInstitutionType.get(userID)!=null)
					{
						input[heuristics.length].set(instH.userInstitutionType.get(userID)+1, 0, 1); //institution type
					}
					if(instH.userToInstitution.get(userID)!=null)
					{
						input[heuristics.length].set(5, 0, (float)(instH.institutionSize.get(instH.userToInstitution.get(userID))/instH.maxInstSize)); //institution size (number users)
					}
					input[heuristics.length].set(6, 0, (float)(userTransactionFreqs.getOrDefault(userID, 0)/Math.max(maxUserTransactionFreq, minFreq)));
					input[heuristics.length].set(7, 0, (float)(userTransactionVols.getOrDefault(userID, 0L)/Math.max(maxUserTransactionVol, minVol)));
					for(int addInd=0; addInd<heuristics.length; addInd++)
					{
						input[heuristics.length].set(addInd+7, 0, heuristics[addInd].getAdditionalNetInfo(userID, true));
					}
					for(int numTransfersInd=heuristics.length+7; 
							numTransfersInd<heuristics.length+7+heuristics.length*topN; numTransfersInd++)
					{
						if(hRecs[(numTransfersInd-(heuristics.length+7))/topN][(numTransfersInd-(heuristics.length+1))%topN]!=null)
						{
							input[heuristics.length].set(numTransfersInd, 0, 
									(float)(int)dstTransactionFreqs.getOrDefault(hRecs[(numTransfersInd-(heuristics.length+7))/topN][(numTransfersInd-(heuristics.length+1))%topN], 0)
										/Math.max(maxDstTransactionFreq, minFreq));
						}
					}
					for(int numTransfersInd=heuristics.length+7+heuristics.length*topN; 
							numTransfersInd<heuristics.length+7+2*heuristics.length*topN; numTransfersInd++)
					{
						if(hRecs[(numTransfersInd-(heuristics.length+7+heuristics.length*topN))/topN]
																		  [(numTransfersInd-(heuristics.length+7+heuristics.length*topN))%topN]!=null)
						{
							input[heuristics.length].set(numTransfersInd, 0, 
									(float)(long)dstTransactionVols.getOrDefault((hRecs[(numTransfersInd-(heuristics.length+7+heuristics.length*topN))/topN]
																			  [(numTransfersInd-(heuristics.length+7+heuristics.length*topN))%topN]), 0L)
											/Math.max(maxDstTransactionVol, minVol));
						}
					}
					for(int numTransfersInd=heuristics.length+7+2*heuristics.length*topN; 
							numTransfersInd<heuristics.length+7+2*heuristics.length*topN+heuristics.length;
							numTransfersInd++)
					{
						input[heuristics.length].set(numTransfersInd, 0, 
								heuristicsPerformanceByUser[(numTransfersInd-(heuristics.length+7+2*heuristics.length*topN))]
										.getOrDefault(userID, new float[]{0.0f, 1.0f})[0]
								/
								heuristicsPerformanceByUser[(numTransfersInd-(heuristics.length+7+2*heuristics.length*topN))]
										.getOrDefault(userID, new float[]{0.0f, 1.0f})[1]);
					}
					if(outputsByUser.get(userID)!=null)
					{
						float[] lastOutput=outputsByUser.get(userID).get(outputsByUser.get(userID).size()-1);
						for(int numTransfersInd=heuristics.length+7+2*heuristics.length*topN+heuristics.length; 
								numTransfersInd<heuristics.length+7+2*heuristics.length*topN+heuristics.length+heuristics.length*topN;
								numTransfersInd++)
						{
							input[heuristics.length].set(numTransfersInd, 0, 
									lastOutput[numTransfersInd-(heuristics.length+7+2*heuristics.length*topN+heuristics.length)]);
						}
					}
					
					inputs[sampleAddedInd]=input;
					
					boolean[] set=new boolean[heuristics.length];
					
					boolean oneHistIsCorrect=false;
					boolean oneHistIsCorrectOther=false;
					outputs[sampleAddedInd]=new Matrix[1];
					//outputs[sampleAddedInd][0]=new FDMatrix(heuristics.length*topN+1, 1);
					outputs[sampleAddedInd][0]=new FDMatrix(heuristics.length*topN, 1);
					float numberSet=0.0f;
					
					
					
					for(int recLevel=0; recLevel<topN; recLevel++)
					{
						for(int hInd=0; hInd<heuristics.length; hInd++)
						{
							String hRec="";
							if(hInd==0 && hRecs[hInd][recLevel]!=null)
							{
								hRec+=hRecs[hInd][recLevel]+" ";
							}
							
							if(hRecs[hInd][recLevel]!=null && hRecs[hInd][recLevel].equals(dstID))
							{
								outputs[sampleAddedInd][0].set(hInd*topN+recLevel, 0,
										1.0f);
								numberSet++;							
								
								if(!set[hInd])
								{
									set[hInd]=true;
									if(heuristicsPerformanceByUser[hInd].get(userID)==null)
									{
										heuristicsPerformanceByUser[hInd].put(userID, new float[2]);
									}
									heuristicsPerformanceByUser[hInd].get(userID)[0]++;
								}
							}
							if(hInd==0)
							{
								//System.out.println(hRec);
							}
						}
					}
					if(numberSet>0 && save)
					{
						for(int outputInd=0; outputInd<outputs[sampleAddedInd][0].getLen(); outputInd++)
						{
							outputs[sampleAddedInd][0].set(outputInd, 0,
									outputs[sampleAddedInd][0].get(outputInd, 0)/numberSet);
						}
					}
					else
					{
						/*
						for(int outputInd=0; outputInd<outputs[sampleAddedInd][0].getLen(); outputInd++)
						{
							outputs[sampleAddedInd][0].set(heuristics.length*topN, 0, 1.0f);
						}
						*/
					}
					
					if(numberSet>0)
					{
						for(int hInd=0; hInd<heuristics.length; hInd++)
						{
							if(heuristicsPerformanceByUser[hInd].get(userID)==null)
							{
								heuristicsPerformanceByUser[hInd].put(userID, new float[2]);
							}
							heuristicsPerformanceByUser[hInd].get(userID)[1]++;
						}			
					}
					
					int inputSize=0;
					for(int inputInd=0; inputInd<inputs[sampleAddedInd].length; inputInd++)
					{
						inputSize+=inputs[sampleAddedInd][inputInd].getLen();
					}
					float[] inputsArray=new float[inputSize];
					inputSize=0;
					for(int inputInd=0; inputInd<inputs[sampleAddedInd].length; inputInd++)
					{
						for(int matInd=0; matInd<inputs[sampleAddedInd][inputInd].getLen(); matInd++)
						{
							inputsArray[inputSize]=inputs[sampleAddedInd][inputInd].get(matInd, 0);
							inputSize++;
						}
					}
					if(inputsByUser.get(userID)==null)
					{
						inputsByUser.put(userID, new ArrayList<float[]>());
					}
					inputsByUser.get(userID).add(inputsArray);
					
					float[] outputsArray=new float[outputs[sampleAddedInd][0].getLen()];
					for(int matInd=0; matInd<outputs[sampleAddedInd][0].getLen(); matInd++)
					{
						outputsArray[matInd]=outputs[sampleAddedInd][0].get(matInd, 0);
					}
					if(outputsByUser.get(userID)==null)
					{
						outputsByUser.put(userID, new ArrayList<float[]>());
					}
					outputsByUser.get(userID).add(outputsArray);
					for(int inInd=0; inInd<outputsArray.length; inInd++)
					{
						if(!Float.isFinite(outputsArray[inInd]))
						{
							int y=0;
						}
					}
					
					//outputs[sampleAddedInd][0].set(0, 0, 1.0f);
					//outputs[sampleAddedInd][0].set(1, 0, 0.0f);
					
					if(historySizesByUser.get(userID)==null)
					{
						historySizesByUser.put(userID, new ArrayList<String>());
					}
					historySizesByUser.get(userID).add(""+historySize);
	
					sampleAddedInd++;
					if(sampleAddedInd%10000==0)
					{
						System.out.println(sampleAddedInd);
					}
				} 
				catch (ParseException e) 
				{
					e.printStackTrace();
				}
			}
			histH.nextTxn();
			nextTxn();	
			tDataInd++;
		}

		Matrix[][] newInputs=new Matrix[sampleAddedInd][];
		System.arraycopy(inputs, 0, newInputs, 0, sampleAddedInd);
		inputs=newInputs;
		inputs=scaleSrcInputs.scaleData(inputs, true);
		for(List<float[]> userInputs: inputsByUser.values())
		{
			for(float[] input: userInputs)
			{
				for(int inputInd=0; inputInd<input.length; inputInd++)
				{
					input[inputInd]*=scaleSrcInputs.scaleFactor;
				}
			}
		}

		System.out.println("Generated "+amtTData);

		if(save)
		{
			BufferedWriter fDatasout=null;
			BufferedWriter fRecsout=null;
			BufferedWriter fHistOut=null;
			try 
			{
				fDatasout=new BufferedWriter(new FileWriter(new File("/home/willie/workspace/Globus/data/heuristicsData"+saveName)));
				fRecsout=new BufferedWriter(new FileWriter(new File("/home/willie/workspace/Globus/data/heuristicsRecs"+saveName)));
				fHistOut=new BufferedWriter(new FileWriter(new File("/home/willie/workspace/Globus/data/heuristicsHistSizes"+saveName)));
			} 
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
			
			DecimalFormat df=new DecimalFormat("#0.000000"); 
			try
			{
				for(String userID: inputsByUser.keySet())
				{
					fDatasout.write("user "+userID+"\n");
					fRecsout.write("user "+userID+"\n");
					fHistOut.write("user "+userID+"\n");
					
					List<String> userHistSizes=historySizesByUser.get(userID);
					for(String histSize: userHistSizes)
					{
						fHistOut.write(histSize+"\n");
					}
					
					List<String[]> userRecs=recsByUser.get(userID);
					for(String[] recs: userRecs)
					{
						String recString="";
						for(String rec: recs)
						{
							recString+=rec+",";
						}
						recString=recString.substring(0, recString.length()-1);
						fRecsout.write(recString+"\n");
					}
					
					List<float[]> userInputs=inputsByUser.get(userID);
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
					for(float[] input: outputsByUser.get(userID))
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
				fRecsout.close();
				fDatasout.close();
				fHistOut.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		return new Matrix[][][]{inputs, outputs};
	}
	
	int tDataInd=0;
	
	Matrix[][][] generateTrainingDataSrcDstByUser(EPHeuristic[] heuristics, int amtTData, int offset, 
			String saveName, boolean save)
	{
		if(trainingTransactions.size()<amtTData)
		{
			System.out.println("not enough data to generate training data!");
		}
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		
		Hashtable<String, List<float[]>> inputsByUser=new Hashtable<>();
		Hashtable<String, List<float[]>> outputsByUser=new Hashtable<>();
		
		int sampleAddedInd=0;
		
		long date;
		while(tDataInd<offset)
		{
			nextTxn();	
			tDataInd++;
		}
		
		Matrix[][] inputs=new Matrix[2*amtTData][];
		Matrix[][] outputs=new Matrix[2*amtTData][];
		sampleAddedInd=0;

		while(sampleAddedInd<amtTData
				&& tDataInd<trainingTransactions.size())
		{
			String userID=trainingTransactions.get(tDataInd)[2];
			String srcID=trainingTransactions.get(tDataInd)[14];
			if(!srcID.isEmpty())
			{
				try 
				{
					date=sdf.parse(trainingTransactions.get(tDataInd)[4]).getTime();
					String[][] hRecs=new String[heuristics.length][topN];
					Matrix[] input=new Matrix[heuristics.length+1];
					for(int hInd=0; hInd<heuristics.length; hInd++)
					{
						input[hInd]=new FDMatrix(topN*heuristics.length, 1);
						//input[hInd]=new FDMatrix(topN, 1);
						for(int recN=0; recN<topN; recN++)
						{
							Object[] rec=heuristics[hInd].getNthBestSrc(userID, recN, date);
							hRecs[hInd][recN]=(String)rec[0];
							input[hInd].set(hInd*topN+recN, 0, (float)rec[1]);
							//input[hInd].set(recN, 0, (float)rec[1]);
						}
					}
					for(int hInd=0; hInd<heuristics.length; hInd++)
					{
						for(int oHInd=0; oHInd<heuristics.length; oHInd++)
						{
							if(hInd!=oHInd)
							{
								for(int recN=0; recN<topN; recN++)
								{
									input[hInd].set(oHInd*topN+recN, 0, heuristics[hInd].getSrcWeight(userID, hRecs[oHInd][recN]));
									if(hInd==0 && heuristics[hInd].getSrcWeight(userID, hRecs[oHInd][recN])==0.5)
									{
										heuristics[hInd].getSrcWeight(userID, hRecs[oHInd][recN]);
									}
								}
							}
						}
					}
					
					input[heuristics.length]=new FDMatrix(heuristics.length+7+2*heuristics.length*topN+heuristics.length+heuristics.length*topN, 1);
					input[heuristics.length].set(0, 0, date/dateDiv);
					if(instH.userInstitutionType.get(userID)!=null)
					{
						input[heuristics.length].set(instH.userInstitutionType.get(userID)+1, 0, 1); //institution type
					}
					if(instH.userToInstitution.get(userID)!=null)
					{
						input[heuristics.length].set(5, 0, (float)(instH.institutionSize.get(instH.userToInstitution.get(userID))/instH.maxInstSize)); //institution size (number users)
					}
					input[heuristics.length].set(6, 0, (float)(userTransactionFreqs.getOrDefault(userID, 0)/Math.max(maxUserTransactionFreq, minFreq)));
					input[heuristics.length].set(7, 0, (float)(userTransactionVols.getOrDefault(userID, 0L)/Math.max(maxUserTransactionVol, minVol)));
					for(int addInd=0; addInd<heuristics.length; addInd++)
					{
						input[heuristics.length].set(addInd+7, 0, heuristics[addInd].getAdditionalNetInfo(userID, true));
					}
					for(int numTransfersInd=heuristics.length+7; 
							numTransfersInd<heuristics.length+7+heuristics.length*topN; numTransfersInd++)
					{
						input[heuristics.length].set(numTransfersInd, 0, 
								(float)(int)srcTransactionFreqs.getOrDefault(hRecs[(numTransfersInd-(heuristics.length+7))/topN][(numTransfersInd-(heuristics.length+1))%topN], 0)
									/Math.max(maxSrcTransactionFreq, minFreq));
					}
					for(int numTransfersInd=heuristics.length+7+heuristics.length*topN; 
							numTransfersInd<heuristics.length+7+2*heuristics.length*topN; numTransfersInd++)
					{
						input[heuristics.length].set(numTransfersInd, 0, 
								(float)(long)srcTransactionVols.getOrDefault((hRecs[(numTransfersInd-(heuristics.length+7+heuristics.length*topN))/topN]
																		  [(numTransfersInd-(heuristics.length+7+heuristics.length*topN))%topN]), 0L)
										/Math.max(maxSrcTransactionVol, minVol));
					}
					for(int numTransfersInd=heuristics.length+7+2*heuristics.length*topN; 
							numTransfersInd<heuristics.length+7+2*heuristics.length*topN+heuristics.length;
							numTransfersInd++)
					{
						input[heuristics.length].set(numTransfersInd, 0, 
								heuristicsPerformanceByUser[(numTransfersInd-(heuristics.length+7+2*heuristics.length*topN))]
										.getOrDefault(userID, new float[]{0.0f, 1.0f})[0]
								/
								heuristicsPerformanceByUser[(numTransfersInd-(heuristics.length+7+2*heuristics.length*topN))]
										.getOrDefault(userID, new float[]{0.0f, 1.0f})[1]);
					}
					if(outputsByUser.get(userID)!=null && outputsByUser.get(userID).size()>1)
					{
						float[] lastOutput=outputsByUser.get(userID).get(outputsByUser.get(userID).size()-2);
						for(int numTransfersInd=heuristics.length+7+2*heuristics.length*topN+heuristics.length; 
								numTransfersInd<heuristics.length+7+2*heuristics.length*topN+heuristics.length+heuristics.length*topN;
								numTransfersInd++)
						{
							input[heuristics.length].set(numTransfersInd, 0, 
									lastOutput[numTransfersInd-(heuristics.length+7+2*heuristics.length*topN+heuristics.length)]);
						}
					}
					
					inputs[2*sampleAddedInd]=input;
					
					boolean[] set=new boolean[heuristics.length];

					outputs[2*sampleAddedInd]=new Matrix[1];
					//outputs[sampleAddedInd][0]=new FDMatrix(heuristics.length*topN+1, 1);
					outputs[2*sampleAddedInd][0]=new FDMatrix(heuristics.length*topN, 1);
					float numberSet=0.0f;
					for(int recLevel=0; recLevel<topN; recLevel++)
					{
						for(int hInd=0; hInd<heuristics.length; hInd++)
						{
							if(hRecs[hInd][recLevel].equals(srcID))
							{
								outputs[2*sampleAddedInd][0].set(hInd*topN+recLevel, 0,
										1.0f);
								numberSet++;
								
								if(!set[hInd])
								{
									set[hInd]=true;
									if(heuristicsPerformanceByUser[hInd].get(userID)==null)
									{
										heuristicsPerformanceByUser[hInd].put(userID, new float[2]);
									}
									heuristicsPerformanceByUser[hInd].get(userID)[0]++;
								}
							}
						}
					}

					if(numberSet>0 && save)
					{
						for(int outputInd=0; outputInd<outputs[2*sampleAddedInd][0].getLen(); outputInd++)
						{
							outputs[2*sampleAddedInd][0].set(outputInd, 0,
									outputs[2*sampleAddedInd][0].get(outputInd, 0)/numberSet);
						}
					}
					else
					{
					}
					
					if(numberSet>0)
					{
						for(int hInd=0; hInd<heuristics.length; hInd++)
						{
							if(heuristicsPerformanceByUser[hInd].get(userID)==null)
							{
								heuristicsPerformanceByUser[hInd].put(userID, new float[2]);
							}
							heuristicsPerformanceByUser[hInd].get(userID)[1]++;
						}			
					}
					
					int inputSize=0;
					for(int inputInd=0; inputInd<inputs[2*sampleAddedInd].length; inputInd++)
					{
						inputSize+=inputs[2*sampleAddedInd][inputInd].getLen();
					}
					float[] inputsArray=new float[inputSize];
					inputSize=0;
					for(int inputInd=0; inputInd<inputs[2*sampleAddedInd].length; inputInd++)
					{
						for(int matInd=0; matInd<inputs[2*sampleAddedInd][inputInd].getLen(); matInd++)
						{
							inputsArray[inputSize]=inputs[2*sampleAddedInd][inputInd].get(matInd, 0);
							inputSize++;
						}
					}
					if(inputsByUser.get(userID)==null)
					{
						inputsByUser.put(userID, new ArrayList<float[]>());
					}
					inputsByUser.get(userID).add(inputsArray);
					
					float[] outputsArray=new float[outputs[2*sampleAddedInd][0].getLen()];
					for(int matInd=0; matInd<outputs[2*sampleAddedInd][0].getLen(); matInd++)
					{
						outputsArray[matInd]=outputs[2*sampleAddedInd][0].get(matInd, 0);
					}
					if(outputsByUser.get(userID)==null)
					{
						outputsByUser.put(userID, new ArrayList<float[]>());
					}
					outputsByUser.get(userID).add(outputsArray);
					for(int inInd=0; inInd<outputsArray.length; inInd++)
					{
						if(!Float.isFinite(outputsArray[inInd]))
						{
							int y=0;
						}
					}
					if(sampleAddedInd%10000==0)
					{
						System.out.println(sampleAddedInd);
					}
				} 
				catch (ParseException e) 
				{
					e.printStackTrace();
				}
			}
			String dstID=trainingTransactions.get(tDataInd)[15];
			if(!dstID.isEmpty())
			{
				try 
				{
					date=sdf.parse(trainingTransactions.get(tDataInd)[4]).getTime();
					String[][] hRecs=new String[heuristics.length][topN];
					Matrix[] input=new Matrix[heuristics.length+1];
					for(int hInd=0; hInd<heuristics.length; hInd++)
					{
						input[hInd]=new FDMatrix(topN*heuristics.length, 1);
						//input[hInd]=new FDMatrix(topN, 1);
						for(int recN=0; recN<topN; recN++)
						{
							Object[] rec=heuristics[hInd].getNthBestDst(userID, recN, date);
							hRecs[hInd][recN]=(String)rec[0];
							input[hInd].set(hInd*topN+recN, 0, (float)rec[1]);
							//input[hInd].set(recN, 0, (float)rec[1]);
						}
					}
					for(int hInd=0; hInd<heuristics.length; hInd++)
					{
						for(int oHInd=0; oHInd<heuristics.length; oHInd++)
						{
							if(hInd!=oHInd)
							{
								for(int recN=0; recN<topN; recN++)
								{
									input[hInd].set(oHInd*topN+recN, 0, heuristics[hInd].getDstWeight(userID, hRecs[oHInd][recN]));
									if(hInd==0 && heuristics[hInd].getDstWeight(userID, hRecs[oHInd][recN])==0.5)
									{
										heuristics[hInd].getDstWeight(userID, hRecs[oHInd][recN]);
									}
								}
							}
						}
					}
					
					input[heuristics.length]=new FDMatrix(heuristics.length+7+2*heuristics.length*topN+heuristics.length+heuristics.length*topN, 1);
					input[heuristics.length].set(0, 0, date/dateDiv);
					if(instH.userInstitutionType.get(userID)!=null)
					{
						input[heuristics.length].set(instH.userInstitutionType.get(userID)+1, 0, 1); //institution type
					}
					if(instH.userToInstitution.get(userID)!=null)
					{
						input[heuristics.length].set(5, 0, (float)(instH.institutionSize.get(instH.userToInstitution.get(userID))/instH.maxInstSize)); //institution size (number users)
					}
					input[heuristics.length].set(6, 0, (float)(userTransactionFreqs.getOrDefault(userID, 0)/Math.max(maxUserTransactionFreq, minFreq)));
					input[heuristics.length].set(7, 0, (float)(userTransactionVols.getOrDefault(userID, 0L)/Math.max(maxUserTransactionVol, minVol)));
					for(int addInd=0; addInd<heuristics.length; addInd++)
					{
						input[heuristics.length].set(addInd+7, 0, heuristics[addInd].getAdditionalNetInfo(userID, true));
					}
					for(int numTransfersInd=heuristics.length+7; 
							numTransfersInd<heuristics.length+7+heuristics.length*topN; numTransfersInd++)
					{
						input[heuristics.length].set(numTransfersInd, 0, 
								(float)(int)dstTransactionFreqs.getOrDefault(hRecs[(numTransfersInd-(heuristics.length+7))/topN][(numTransfersInd-(heuristics.length+1))%topN], 0)
									/Math.max(maxDstTransactionFreq, minFreq));
					}
					for(int numTransfersInd=heuristics.length+7+heuristics.length*topN; 
							numTransfersInd<heuristics.length+7+2*heuristics.length*topN; numTransfersInd++)
					{
						input[heuristics.length].set(numTransfersInd, 0, 
								(float)(long)dstTransactionVols.getOrDefault((hRecs[(numTransfersInd-(heuristics.length+7+heuristics.length*topN))/topN]
																		  [(numTransfersInd-(heuristics.length+7+heuristics.length*topN))%topN]), 0L)
										/Math.max(maxDstTransactionVol, minVol));
					}
					for(int numTransfersInd=heuristics.length+7+2*heuristics.length*topN; 
							numTransfersInd<heuristics.length+7+2*heuristics.length*topN+heuristics.length;
							numTransfersInd++)
					{
						input[heuristics.length].set(numTransfersInd, 0, 
								heuristicsPerformanceByUser[(numTransfersInd-(heuristics.length+7+2*heuristics.length*topN))]
										.getOrDefault(userID, new float[]{0.0f, 1.0f})[0]
								/
								heuristicsPerformanceByUser[(numTransfersInd-(heuristics.length+7+2*heuristics.length*topN))]
										.getOrDefault(userID, new float[]{0.0f, 1.0f})[1]);
					}
					if(outputsByUser.get(userID)!=null && outputsByUser.get(userID).size()>1)
					{
						float[] lastOutput=outputsByUser.get(userID).get(outputsByUser.get(userID).size()-2);
						for(int numTransfersInd=heuristics.length+7+2*heuristics.length*topN+heuristics.length; 
								numTransfersInd<heuristics.length+7+2*heuristics.length*topN+heuristics.length+heuristics.length*topN;
								numTransfersInd++)
						{
							input[heuristics.length].set(numTransfersInd, 0, 
									lastOutput[numTransfersInd-(heuristics.length+7+2*heuristics.length*topN+heuristics.length)]);
						}
					}
					
					inputs[2*sampleAddedInd+1]=input;
					
					boolean[] set=new boolean[heuristics.length];

					outputs[2*sampleAddedInd+1]=new Matrix[1];
					//outputs[sampleAddedInd][0]=new FDMatrix(heuristics.length*topN+1, 1);
					outputs[2*sampleAddedInd+1][0]=new FDMatrix(heuristics.length*topN, 1);
					float numberSet=0.0f;
					for(int recLevel=0; recLevel<topN; recLevel++)
					{
						for(int hInd=0; hInd<heuristics.length; hInd++)
						{
							if(hRecs[hInd][recLevel].equals(dstID))
							{
								outputs[2*sampleAddedInd+1][0].set(hInd*topN+recLevel, 0,
										1.0f);
								numberSet++;
								
								if(!set[hInd])
								{
									set[hInd]=true;
									if(heuristicsPerformanceByUser[hInd].get(userID)==null)
									{
										heuristicsPerformanceByUser[hInd].put(userID, new float[2]);
									}
									heuristicsPerformanceByUser[hInd].get(userID)[0]++;
								}
							}
						}
					}
					if(numberSet>0 && save)
					{
						for(int outputInd=0; outputInd<outputs[2*sampleAddedInd+1][0].getLen(); outputInd++)
						{
							outputs[2*sampleAddedInd+1][0].set(outputInd, 0,
									outputs[2*sampleAddedInd+1][0].get(outputInd, 0)/numberSet);
						}
					}
					else
					{
						/*
						for(int outputInd=0; outputInd<outputs[sampleAddedInd][0].getLen(); outputInd++)
						{
							outputs[sampleAddedInd][0].set(heuristics.length*topN, 0, 1.0f);
						}
						*/
					}
					
					if(numberSet>0)
					{
						for(int hInd=0; hInd<heuristics.length; hInd++)
						{
							if(heuristicsPerformanceByUser[hInd].get(userID)==null)
							{
								heuristicsPerformanceByUser[hInd].put(userID, new float[2]);
							}
							heuristicsPerformanceByUser[hInd].get(userID)[1]++;
						}			
					}
					
					int inputSize=0;
					for(int inputInd=0; inputInd<inputs[2*sampleAddedInd+1].length; inputInd++)
					{
						inputSize+=inputs[2*sampleAddedInd+1][inputInd].getLen();
					}
					float[] inputsArray=new float[inputSize];
					inputSize=0;
					for(int inputInd=0; inputInd<inputs[2*sampleAddedInd+1].length; inputInd++)
					{
						for(int matInd=0; matInd<inputs[2*sampleAddedInd+1][inputInd].getLen(); matInd++)
						{
							inputsArray[inputSize]=inputs[2*sampleAddedInd+1][inputInd].get(matInd, 0);
							inputSize++;
						}
					}
					if(inputsByUser.get(userID)==null)
					{
						inputsByUser.put(userID, new ArrayList<float[]>());
					}
					inputsByUser.get(userID).add(inputsArray);
					
					float[] outputsArray=new float[outputs[2*sampleAddedInd+1][0].getLen()];
					for(int matInd=0; matInd<outputs[2*sampleAddedInd+1][0].getLen(); matInd++)
					{
						outputsArray[matInd]=outputs[2*sampleAddedInd+1][0].get(matInd, 0);
					}
					if(outputsByUser.get(userID)==null)
					{
						outputsByUser.put(userID, new ArrayList<float[]>());
					}
					outputsByUser.get(userID).add(outputsArray);
					for(int inInd=0; inInd<outputsArray.length; inInd++)
					{
						if(!Float.isFinite(outputsArray[inInd]))
						{
							int y=0;
						}
					}
					
					//outputs[sampleAddedInd][0].set(0, 0, 1.0f);
					//outputs[sampleAddedInd][0].set(1, 0, 0.0f);
	
					
					if(sampleAddedInd%10000==0)
					{
						System.out.println(sampleAddedInd);
					}
				} 
				catch (ParseException e) 
				{
					e.printStackTrace();
				}
			}
			sampleAddedInd++;
			nextTxn();	
			tDataInd++;
		}
		
		/*
		inputs=scaleSrcInputs.scaleData(inputs, true);
		for(List<float[]> userInputs: inputsByUser.values())
		{
			for(float[] input: userInputs)
			{
				for(int inputInd=0; inputInd<input.length; inputInd++)
				{
					input[inputInd]*=scaleSrcInputs.scaleFactor;
				}
			}
		}
		*/
		
		System.out.println("Generated "+amtTData);
		if(save)
		{
			BufferedWriter fout=null;
			try 
			{
				fout=new BufferedWriter(new FileWriter(new File("/home/willie/workspace/Globus/data/heuristicsData"+saveName)));
			} 
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
			
			DecimalFormat df=new DecimalFormat("#0.000000"); 
			try
			{
				for(String userID: inputsByUser.keySet())
				{
					List<float[]> userInputs=inputsByUser.get(userID);
					fout.write("user "+userID+"\n");
					
					fout.write("in\n");
					for(float[] input: userInputs)
					{
						String inputString="";
						for(int inputInd=0; inputInd<input.length; inputInd++)
						{
							inputString+=(df.format(input[inputInd])+",");
						}
						inputString=inputString.substring(0, inputString.length()-1);
						fout.write(inputString+"\n");
					}
					
					fout.write("out\n");
					for(float[] input: outputsByUser.get(userID))
					{
						String inputString="";
						for(int inputInd=0; inputInd<input.length; inputInd++)
						{
							inputString+=(df.format(input[inputInd])+",");
						}
						inputString=inputString.substring(0, inputString.length()-1);
						fout.write(inputString+"\n");
					}
				}
				fout.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		return new Matrix[][][]{inputs, outputs};
	}
	
	SplitNetwork createNetwork()
	{
		/*
		BInputLayer[] inputs=new BInputLayer[heuristics.length+1];
		for(int hInd=0; hInd<heuristics.length; hInd++)
		{
			inputs[hInd]=new BInputLayer(null, null, topN*heuristics.length);
		}
		//current date, size of user src/dst history, size of prev src/dst correlation, number transfers per rec, transfer amt per rec  
		inputs[heuristics.length]=new BInputLayer(null, null, inputLength-heuristics.length*heuristics.length*topN);
		
		FullyConnectedBLayer[] hidden1s=new FullyConnectedBLayer[heuristics.length];
		for(int hInd=0; hInd<heuristics.length; hInd++)
		{
			hidden1s[hInd]=new FullyConnectedBLayer(new Sigmoid(), new BLayer[]{inputs[hInd], inputs[heuristics.length]}, 5*topN*heuristics.length);
		}
		
		//FullyConnectedBLayer hidden1a=new FullyConnectedBLayer(new Sigmoid(), inputs, 16*heuristics.length*topN);
		FullyConnectedBLayer hiddenLayer2a=new FullyConnectedBLayer(new Sigmoid(), hidden1s, 4*heuristics.length*topN);
		FullyConnectedBLayer outputLayer=new FullyConnectedBLayer(new Sigmoid(), new BLayer[]{hiddenLayer2a}, topN*heuristics.length);
		SplitNetwork network=new SplitFeedForwardNetwork(new BLayer[][]{inputs, hidden1s, new BLayer[]{hiddenLayer2a}, new BLayer[]{outputLayer}});
		return network;
		*/
		
		/*
		BInputLayer[] inputs=new BInputLayer[heuristics.length+1];
		for(int hInd=0; hInd<heuristics.length; hInd++)
		{
			inputs[hInd]=new BInputLayer(null, null, topN*heuristics.length);
		}
		//current date, size of user src/dst history, size of prev src/dst correlation, number transfers per rec, transfer amt per rec  
		inputs[heuristics.length]=new BInputLayer(null, null, inputLength-heuristics.length*heuristics.length*topN);
		
		FullyConnectedBLayer[] hidden1s=new FullyConnectedBLayer[heuristics.length];
		for(int hInd=0; hInd<heuristics.length; hInd++)
		{
			hidden1s[hInd]=new FullyConnectedBLayer(new Sigmoid(), new BLayer[]{inputs[hInd], inputs[heuristics.length]}, 8*topN*heuristics.length);
		}
		
		FullyConnectedBLayer[] hidden2s=new FullyConnectedBLayer[heuristics.length];
		for(int hInd=0; hInd<heuristics.length; hInd++)
		{
			hidden2s[hInd]=new FullyConnectedBLayer(new Sigmoid(), new BLayer[]{hidden1s[hInd]}, 2*topN*heuristics.length);
		}
		
		FullyConnectedBLayer outputLayer=new FullyConnectedBLayer(new Sigmoid(), hidden2s, topN*heuristics.length);
		SplitNetwork network=new SplitFeedForwardNetwork(new BLayer[][]{inputs, hidden1s, hidden2s, new BLayer[]{outputLayer}});
		return network;
		*/
		
		BInputLayer[] inputs=new BInputLayer[heuristics.length+1];
		for(int hInd=0; hInd<heuristics.length; hInd++)
		{
			inputs[hInd]=new BInputLayer(null, null, topN*heuristics.length);
		}
		//current date, size of user src/dst history, size of prev src/dst correlation, number transfers per rec, transfer amt per rec  
		inputs[heuristics.length]=new BInputLayer(null, null, inputLength-heuristics.length*heuristics.length*topN);
		
		FullyConnectedBLayer[] hidden1s=new FullyConnectedBLayer[heuristics.length];
		for(int hInd=0; hInd<heuristics.length; hInd++)
		{
			hidden1s[hInd]=new FullyConnectedBLayer(new Sigmoid(), new BLayer[]{inputs[hInd], inputs[heuristics.length]}, 8*topN*heuristics.length);
		}
		
		FullyConnectedBLayer[] hidden2s=new FullyConnectedBLayer[heuristics.length];
		for(int hInd=0; hInd<heuristics.length; hInd++)
		{
			hidden2s[hInd]=new FullyConnectedBLayer(new Sigmoid(), new BLayer[]{hidden1s[hInd]}, 2*topN*heuristics.length);
		}
		
		FullyConnectedBLayer[] outputLayers=new FullyConnectedBLayer[heuristics.length];
		for(int hInd=0; hInd<heuristics.length; hInd++)
		{
			outputLayers[hInd]=new FullyConnectedBLayer(new Sigmoid(), new BLayer[]{hidden2s[hInd]}, 1);
		}

		SplitNetwork network=new SplitFeedForwardNetwork(new BLayer[][]{inputs, hidden1s, hidden2s, outputLayers});
		return network;
	}
	
	void trainNetwork(Matrix[][] inputs, Matrix[][] outputs, Matrix[][] validationInputs,
			Matrix[][] validationOutputs, SplitNetwork trainNetwork)
	{
		float lambda=0.01f;
		BPGDUnsupervisedTraining bpgd=new BPGDUnsupervisedTraining(new RPropMultithreaded(100000, 400, 0.01f));
		//BPGDUnsupervisedTraining bpgd=new BPGDUnsupervisedTraining(new RProp(1000, 50, 0.01f));
		//BPGDUnsupervisedTrainingNestrov bpgd=new BPGDUnsupervisedTrainingNestrov(1000, 50, 50, lambda, 0.9f);
		//bpgd.unsupervisedTrain(trainNetwork, inputs,
		//		outputs, new CrossEntropy());
		bpgd.trainNetwork(trainNetwork, inputs,
				outputs, new CrossEntropy(), new NActionsValidator(validationInputs,
						validationOutputs, topN));
	}

	static HistoryHeuristic histH=null;
	static String currentDir=System.getProperty("user.dir");
	private static double[] testHeuristic() throws IOException
	{
		boolean ldData=false;
		
		EPHeuristic[] heuristcs=null;
		List<String[]> transactions=new ArrayList<>();
		List<String[]> deletions=new ArrayList<>();
		List<String[]> users=new ArrayList<>();
		List<String[]> authUsers=new ArrayList<>();
		int topN=3;
		if(!ldData)
		{
			transactions=readLinesFullTransfers("full-trans-tab.csv");
			deletions=readLines("short-eps.csv");
			users=readLines("users.csv");
			authUsers=readLines("auth_users.csv");
			
			HistoryHeuristic histH=new HistoryHeuristic(transactions, deletions, users, authUsers, 0);
			CorrelationHeuristic corrH=new CorrelationHeuristic(transactions, deletions, users, authUsers, 0);
			UserEPsHeuristic userEPsHeuristic=new UserEPsHeuristic(transactions, deletions, users, authUsers, 0);
			InstitutionHeuristic insH=new InstitutionHeuristic(transactions, deletions, users, authUsers, 0, topN);
			MostUniqueUsersHeuristic mUUserH=new MostUniqueUsersHeuristic(transactions, deletions, users, authUsers, 0, topN);
			
			heuristcs=new EPHeuristic[]{histH, corrH, userEPsHeuristic, insH, mUUserH};
		}
		else
		{
			heuristcs=new EPHeuristic[5];
		}
		
		histH=new HistoryHeuristic(transactions, deletions, users, authUsers, 0);
		
		EnsembleNeuralNetwork ensNet=new EnsembleNeuralNetwork(transactions, deletions, users, authUsers,
				0, heuristcs, topN);
		ensNet.init(500000, transactions, ldData, false);
		
		
		ensNet.generateTrainingDataSrcByUser(ensNet.heuristics, 500000, 0, "trainAllFixedSrc", true);
		ensNet.generateTrainingDataSrcByUser(ensNet.heuristics, transactions.size()-500000, 500000, "validateAllFixedSrc", true);
		
		
		/*
		ensNet.generateTrainingDataDstByUser(ensNet.heuristics, 500000, 0, "trainAllFixedDst", true);
		ensNet.generateTrainingDataDstByUser(ensNet.heuristics, transactions.size()-500000, 500000, "validateAllFixedDst", true);
		*/
		
		/*
		ensNet.generateTrainingDataSrcDstByUser(ensNet.heuristics, 500000, 0, "trainAllFixedSrcDst", true);
		ensNet.generateTrainingDataSrcDstByUser(ensNet.heuristics, transactions.size()-500000, 500000, "validateAllFixedSrcDst", true);
		*/
//		corrH=new CorrelationHeuristic(transactions, deletions, users, authUsers, 0);
//		histH=new HistoryHeuristic(transactions, deletions, users, authUsers, 0);
//		mUUserH=new MostUniqueUsersHeuristic(transactions, deletions, users, authUsers, 0, topN);
//		insH=new InstitutionHeuristic(transactions, deletions, users, authUsers, 0, topN);
//		userEPsHeuristic=new UserEPsHeuristic(transactions, deletions, users, authUsers, 0);
//		ensNet.heuristics=new EPHeuristic[]{corrH, histH, mUUserH, insH, userEPsHeuristic};
		ensNet.transactionInd=0;
		
		double srcAttempts=0.0;
		double srcCorrect=0.0;
		
		double dstAttempts=0.0;
		double dstCorrect=0.0;
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		
		for(String[] line: transactions)
		{
			String userID=line[1];
			String srcID=line[2];
			String dstID=line[3];
			
			long currentDate;
			try 
			{ 
				currentDate=sdf.parse(line[4]).getTime();
				List<String> srcRecs=new ArrayList<>();
				for(int recInd=0; recInd<topN; recInd++)
				{
					srcRecs.add((String)(ensNet.getNthBestSrc(userID, recInd, currentDate)[0]));
					//srcRecs.add((String)(corrH.getNthBestSrc(userID, recInd, currentDate)[0]));
				}
				srcAttempts++;
				if(srcRecs.contains(srcID))
				{
					srcCorrect++;
				}
				
				/*
				List<String> dstRecs=new ArrayList<>();
				for(int recInd=0; recInd<topN; recInd++)
				{
					dstRecs.add((String)(histH.getNthBestDst(userID, recInd, currentDate)[0]));
				}
				dstAttempts++;
				if(dstRecs.contains(dstID))
				{
					dstCorrect++;
				}
				*/
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			ensNet.nextTxn();
		}
		
		return new double[]{srcCorrect/srcAttempts, dstCorrect/dstAttempts};
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
		
		List<IDData<Long>> transactionsWithDates=new ArrayList<>();
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		for(String[] transaction: splitData)
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
		splitData.clear();
		Collections.sort(transactionsWithDates);
		for(IDData<Long> transactionWithDate: transactionsWithDates)
		{
			splitData.add(transactionWithDate.line);
		}
		
		return splitData;
	}
	
	public static void main(String[] args) throws IOException
	{
		double[] accuracies=testHeuristic();
		System.out.println("EnsNet Heuristics Accuracy src: "+accuracies[0]+" dst: "+accuracies[1]);
	}

}
