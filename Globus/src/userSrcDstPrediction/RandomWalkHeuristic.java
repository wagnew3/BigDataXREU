package userSrcDstPrediction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import graphStreamFramework.RandomWalk;
import init.IDData;

public class RandomWalkHeuristic extends EPHeuristic
{
	
	int walkLength=3;
	int numberWalks=25;
	Graph usersSrc;
	Graph usersDst;
	Hashtable<String, Hashtable<String, int[]>> usageFrequencies;

	public RandomWalkHeuristic(List<String[]> transactions, List<String[]> deletions, long date) 
	{
		super(transactions, deletions, date);
		
		usageFrequencies=new Hashtable<>();
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		for(String[] line: transactions)
		{
			String user=line[1];
			String src=line[2];
			String dst=line[3];
			Long currentDate=null;
			try 
			{
				currentDate=sdf.parse(line[4]).getTime();
				if(currentDate>=date)
				{
					break;
				}
				
				transactionInd++;
				if(usageFrequencies.get(user)==null)
				{
					usageFrequencies.put(user, new Hashtable<String, int[]>());
				}
				
				if(usageFrequencies.get(user).get(src)==null)
				{
					usageFrequencies.get(user).put(src, new int[2]);
				}
				usageFrequencies.get(user).get(src)[0]++;
				
				if(usageFrequencies.get(user).get(dst)==null)
				{
					usageFrequencies.get(user).put(dst, new int[2]);
				}
				usageFrequencies.get(user).get(dst)[1]++;
			} 
			catch (ParseException e)
			{
				e.printStackTrace();
			}
		}
		
		usersSrc=new SingleGraph("usersSrc");
		for(String userID: usageFrequencies.keySet())
		{
			usersSrc.addNode("usr"+userID);
			for(String epID: usageFrequencies.get(userID).keySet())
			{
				if(usageFrequencies.get(userID).get(epID)[0]>0)
				{
					if(usersSrc.getNode(epID)==null)
					{
						usersSrc.addNode(epID);
					}
					Edge newEdge=usersSrc.addEdge("usr"+userID+epID, "usr"+userID, epID);
					newEdge.setAttribute("weight", (Double)(double)(usageFrequencies.get(userID).get(epID)[0]));
				}
			}
		}
		
		usersDst=new SingleGraph("usersSrc");
		for(String userID: usageFrequencies.keySet())
		{
			usersDst.addNode("usr"+userID);
			for(String epID: usageFrequencies.get(userID).keySet())
			{
				if(usageFrequencies.get(userID).get(epID)[1]>0)
				{
					if(usersDst.getNode(epID)==null)
					{
						usersDst.addNode(epID);
					}
					Edge newEdge=usersDst.addEdge("usr"+userID+epID, "usr"+userID, epID);
					newEdge.setAttribute("weight", (Double)(double)(usageFrequencies.get(userID).get(epID)[1]));
				}
			}
		}
	}

	@Override
	public Object[] getNthBestSrc(String userID, int n, long date) 
	{
		Node startNode=usersSrc.getNode("usr"+userID);
		if(startNode!=null)
		{
			Hashtable<String, IDData<Integer>> walkResultHT
				=randomWalkFromNode(startNode, 
						numberWalks, walkLength);
			List<IDData<Integer>> walkResult=new ArrayList<>(walkResultHT.values());
			for(int wrInd=0; wrInd<walkResult.size(); wrInd++)
			{
				if(deletions.get(walkResult.get(wrInd).ID)!=null
						&& deletions.get(walkResult.get(wrInd).ID)<date)
				{
					walkResult.remove(wrInd);
					wrInd--;
				}
				else if(walkResult.get(wrInd).ID.charAt(0)=='u')
				{
					walkResult.remove(wrInd);
					wrInd--;
				}
			}
			if(walkResult.size()>n)
			{
				Collections.sort(walkResult);
				return new Object[]{walkResult.get(walkResult.size()-1-n).ID, 
						(double)(int)(walkResult.get(walkResult.size()-1-n).data)/(numberWalks*walkLength)};
			}
		}
		return new Object[]{"", -1};
	}

	@Override
	public double getSrcWeight(String userID, String srcID) 
	{
		Node startNode=usersSrc.getNode("usr"+userID);
		if(startNode!=null)
		{
			Hashtable<String, IDData<Integer>> walkResultHT
				=randomWalkFromNode(startNode, 
						numberWalks, walkLength);
			List<IDData<Integer>> walkResult=new ArrayList<>(walkResultHT.values());
			IDData<Integer> srcIDData=new IDData<Integer>(srcID, 0);
			int ind=walkResult.indexOf(srcIDData);
			if(ind>=0)
			{
				return (double)(int)walkResult.get(ind).data/(numberWalks*walkLength);
			}
		}
		return 0.0;
	}

	@Override
	public Object[] getNthBestDst(String userID, int n, long date) 
	{
		Node startNode=usersDst.getNode("usr"+userID);
		if(startNode!=null)
		{
			Hashtable<String, IDData<Integer>> walkResultHT
				=randomWalkFromNode(startNode, 
						numberWalks, walkLength);
			List<IDData<Integer>> walkResult=new ArrayList<>(walkResultHT.values());
			for(int wrInd=0; wrInd<walkResult.size(); wrInd++)
			{
				try
				{
				if(deletions.get(walkResult.get(wrInd).ID)!=null
						&& deletions.get(walkResult.get(wrInd).ID)<date)
				{
					walkResult.remove(wrInd);
					wrInd--;
				}
				else if(walkResult.get(wrInd).ID.charAt(0)=='u')
				{
					walkResult.remove(wrInd);
					wrInd--;
				}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			if(walkResult.size()>n)
			{
				Collections.sort(walkResult);
				return new Object[]{walkResult.get(walkResult.size()-1-n).ID, 
						(double)(int)(walkResult.get(walkResult.size()-1-n).data)/(numberWalks*walkLength)};
			}
		}
		return new Object[]{"", -1};
	}

	@Override
	public double getDstWeight(String userID, String dstID) 
	{
		Node startNode=usersDst.getNode("usr"+userID);
		if(startNode!=null)
		{
			Hashtable<String, IDData<Integer>> walkResultHT
				=randomWalkFromNode(startNode, 
						numberWalks, walkLength);
			List<IDData<Integer>> walkResult=new ArrayList<>(walkResultHT.values());
			IDData<Integer> dstIDData=new IDData<Integer>(dstID, 0);
			int ind=walkResult.indexOf(dstIDData);
			if(ind>=0)
			{
				return (double)(int)walkResult.get(ind).data/(numberWalks*walkLength);
			}
		}
		return 0.0;
	}

	Hashtable<String, String> srcDstPairs=new Hashtable<>();
	int numberFirstSrcPairs=0;
	int numberSrcPaths=0;
	int numConnected=0;
	double totalPathLength=0;
	
	@Override
	protected void updateHeuristic(String[] newTransaction) 
	{
		String user=newTransaction[1];
		String src=newTransaction[2];
		String dst=newTransaction[3];
		
		transactionInd++;
		if(usageFrequencies.get(user)==null)
		{
			usageFrequencies.put(user, new Hashtable<String, int[]>());
		}
		
		if(!src.isEmpty())
		{
			if(usageFrequencies.get(user).get(src)==null)
			{
				usageFrequencies.get(user).put(src, new int[2]);
			}
			usageFrequencies.get(user).get(src)[0]++;
			if(usersSrc.getNode("usr"+user)==null)
			{
				usersSrc.addNode("usr"+user);
			}
			for(String epID: usageFrequencies.get(user).keySet())
			{
				if(usageFrequencies.get(user).get(epID)[0]>0)
				{
					if(usersSrc.getNode(epID)==null)
					{
						usersSrc.addNode(epID);
					}
					if(usersSrc.getEdge("usr"+user+epID)==null)
					{
						usersSrc.addEdge("usr"+user+epID, "usr"+user, epID);
					}
					usersSrc.getEdge("usr"+user+epID).setAttribute("weight", (Double)(double)(usageFrequencies.get(user).get(epID)[0]));
				}
			}
		}
		
		if(!dst.isEmpty())
		{
			if(usageFrequencies.get(user).get(dst)==null)
			{
				usageFrequencies.get(user).put(dst, new int[2]);
			}
			usageFrequencies.get(user).get(dst)[1]++;
			
			if(usersDst.getNode("usr"+user)==null)
			{
				usersDst.addNode("usr"+user);
			}
			for(String epID: usageFrequencies.get(user).keySet())
			{
				if(usageFrequencies.get(user).get(epID)[1]>0)
				{
					if(usersDst.getNode(epID)==null)
					{
						usersDst.addNode(epID);
					}
					if(usersDst.getEdge("usr"+user+epID)==null)
					{
						usersDst.addEdge("usr"+user+epID, "usr"+user, epID);
					}
					usersDst.getEdge("usr"+user+epID).setAttribute("weight", (Double)(double)(usageFrequencies.get(user).get(epID)[1]));
				}
			}
		}
	}

	Hashtable<String, IDData<Integer>> randomWalkFromNode(Node startNode, int numberWalks, int walkLength)
	{
		Hashtable<String, IDData<Integer>> visits=new Hashtable<>();
		for(int walkNumber=0; walkNumber<numberWalks; walkNumber++)
		{
			Node currentNode=startNode;
			List<Node> neighbors=new ArrayList<>();
			List<Double> weights=new ArrayList<>();
			for(int stepNumber=0; stepNumber<walkLength; stepNumber++)
			{
				double weightSum=0.0;
				for(Edge edge: currentNode.getEachEdge())
				{
					Node neighbor;
					if(!currentNode.equals(edge.getNode0()))
					{
						neighbor=edge.getNode0();
					}
					else
					{
						neighbor=edge.getNode1();
					}
					neighbors.add(neighbor);
					weights.add(edge.getAttribute("weight"));
					weightSum+=weights.get(weights.size()-1);
				}
				double rand=Math.random();
				int ind=0;
				while(rand>0 && ind<neighbors.size())
				{
					rand-=weights.get(ind)/weightSum;
					ind++;
				}
				currentNode=neighbors.get(ind-1);
			}
			if(visits.get(currentNode.getId())==null)
			{
				visits.put(currentNode.getId(), new IDData<Integer>(currentNode.getId(), 0));
			}
			visits.get(currentNode.getId()).data=(Integer)visits.get(currentNode.getId()).data+1;
		}
		return visits;
	}
	
	static String currentDir=System.getProperty("user.dir");
	private static double[] testHeuristic() throws IOException
	{
		List<String> lines=Files.readAllLines(new File("C:\\Users\\C\\workspace\\Globus\\data\\transfers-william.csv").toPath());
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
					break;
				}
			}
		}
		
		List<String[]> user_eps=readDeletionLines();
		
		RandomWalkHeuristic randWalkH=new RandomWalkHeuristic(splitData, user_eps, 0);
		double srcAttempts=0.0;
		double srcCorrect=0.0;
		
		double dstAttempts=0.0;
		double dstCorrect=0.0;
		
		int topN=1;
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		
		for(String[] line: splitData)
		{
			String userID=line[1];
			String srcID=line[2];
			String dstID=line[3];
			
			long currentDate;
			try 
			{
				currentDate = sdf.parse(line[4]).getTime();
				List<String> srcRecs=new ArrayList<>();
				for(int recInd=0; recInd<topN; recInd++)
				{
					srcRecs.add((String)(randWalkH.getNthBestSrc(userID, recInd, currentDate)[0]));
				}
				srcAttempts++;
				if(srcRecs.contains(srcID))
				{
					srcCorrect++;
				}
				else
				{
					int u=0;
				}
				
				List<String> dstRecs=new ArrayList<>();
				for(int recInd=0; recInd<topN; recInd++)
				{
					dstRecs.add((String)(randWalkH.getNthBestDst(userID, recInd, currentDate)[0]));
				}
				dstAttempts++;
				if(dstRecs.contains(dstID))
				{
					dstCorrect++;
				}
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			randWalkH.nextTxn();
		}
		
		return new double[]{srcCorrect/srcAttempts, dstCorrect/dstAttempts};
	}
	
	static List<String[]> readDeletionLines()
	{
		List<String> lines=null;
		try
		{
			lines = Files.readAllLines(new File("C:\\Users\\C\\workspace\\Globus\\data\\short-eps.csv").toPath());
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
