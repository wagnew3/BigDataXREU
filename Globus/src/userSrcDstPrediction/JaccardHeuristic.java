package userSrcDstPrediction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import init.IDData;

public class JaccardHeuristic extends EPHeuristic 
{
	
	static Graph usersSrc;
	static Graph usersDst;
	//Hashtable<String, Integer> srcUsageFrequencies;
	//Hashtable<String, Integer> dstUsageFrequencies;
	static Hashtable<String, Hashtable<String, int[]>> usageFrequencies;

	public JaccardHeuristic(List<String[]> transactions, List<String[]> deletions, long date) 
	{
		super(transactions, deletions, date);
		
		usersSrc=new SingleGraph("usersSrc");
		usersDst=new SingleGraph("usersDst");
		usageFrequencies=new Hashtable<>();
		
		//srcUsageFrequencies=new Hashtable<>();
		//dstUsageFrequencies=new Hashtable<>();
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
				
				/*
				if(srcUsageFrequencies.get(src)==null)
				{
					srcUsageFrequencies.put(src, 0);
				}
				srcUsageFrequencies.put(src, srcUsageFrequencies.get(src)+1);
				
				if(dstUsageFrequencies.get(dst)==null)
				{
					dstUsageFrequencies.put(dst, 0);
				}
				dstUsageFrequencies.put(dst, dstUsageFrequencies.get(dst)+1);
				*/
				
				if(usageFrequencies.get("usr"+user)==null)
				{
					usageFrequencies.put("usr"+user, new Hashtable<String, int[]>());
				}
				if(usageFrequencies.get("usr"+user).get(src)==null)
				{
					usageFrequencies.get("usr"+user).put(src, new int[2]);
				}
				usageFrequencies.get("usr"+user).get(src)[0]++;
				if(usageFrequencies.get("usr"+user).get(dst)==null)
				{
					usageFrequencies.get("usr"+user).put(dst, new int[2]);
				}
				usageFrequencies.get("usr"+user).get(dst)[1]++;
				
				if(usersSrc.getNode("usr"+user)==null)
				{
					usersSrc.addNode("usr"+user);
				}
				if(usersSrc.getNode(src)==null)
				{
					usersSrc.addNode(src);
				}
				if(usersSrc.getEdge("usr"+user+" "+src)==null)
				{
					usersSrc.addEdge("usr"+user+" "+src, "usr"+user, src);
				}
				
				if(usersDst.getNode("usr"+user)==null)
				{
					usersDst.addNode("usr"+user);
				}
				if(usersDst.getNode(dst)==null)
				{
					usersDst.addNode(dst);
				}
				if(usersDst.getEdge("usr"+user+" "+dst)==null)
				{
					usersDst.addEdge("usr"+user+" "+dst, "usr"+user, dst);
				}
			} 
			catch (ParseException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public Object[] getNthBestSrc(String userID, int n, long date) 
	{
		Node startNode=usersSrc.getNode("usr"+userID);
		if(startNode!=null)
		{
			List<Object[]> recs=getJaccardNeighborNewNodesFreqWeight(
					startNode, 0);

			for(int ind=0; ind<recs.size(); ind++)
			{
				if(deletions.get(recs.get(ind)[0])!=null
						&& deletions.get(recs.get(ind)[0])<date)
				{
					recs.remove(ind);
					ind--;
				}
			}
			if(recs.size()>n)
			{
				return new Object[]{recs.get(recs.size()-1-n)[0], 
						recs.get(recs.size()-1-n)[1]};
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
			List<Object[]> recs=getJaccardNeighborNewNodesFreqWeight(
					startNode, 0);

			for(int ind=0; ind<recs.size(); ind++)
			{
				if(deletions.get(recs.get(ind)[0])!=null
						&& deletions.get(recs.get(ind)[0])<date)
				{
					recs.remove(ind);
					ind--;
				}
			}
			for(int ind=0; ind<recs.size(); ind++)
			{
				if(((String)recs.get(ind)[0]).equals(srcID))
				{
					return (double)recs.get(ind)[1];
				}
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
			List<Object[]> recs=getJaccardNeighborNewNodesFreqWeight(
					startNode, 1);

			for(int ind=0; ind<recs.size(); ind++)
			{
				if(deletions.get(recs.get(ind)[0])!=null
						&& deletions.get(recs.get(ind)[0])<date)
				{
					recs.remove(ind);
					ind--;
				}
			}
			if(recs.size()>n)
			{
				return new Object[]{recs.get(recs.size()-1-n)[0], 
						recs.get(recs.size()-1-n)[1]};
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
			List<Object[]> recs=getJaccardNeighborNewNodesFreqWeight(
					startNode, 1);

			for(int ind=0; ind<recs.size(); ind++)
			{
				if(deletions.get(recs.get(ind)[0])!=null
						&& deletions.get(recs.get(ind)[0])<date)
				{
					recs.remove(ind);
					ind--;
				}
			}
			for(int ind=0; ind<recs.size(); ind++)
			{
				if(((String)recs.get(ind)[0]).equals(dstID))
				{
					return (double)recs.get(ind)[1];
				}
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
		
		/*
		if(srcUsageFrequencies.get(src)==null)
		{
			srcUsageFrequencies.put(src, 0);
		}
		srcUsageFrequencies.put(src, srcUsageFrequencies.get(src)+1);
		
		if(dstUsageFrequencies.get(dst)==null)
		{
			dstUsageFrequencies.put(dst, 0);
		}
		dstUsageFrequencies.put(dst, dstUsageFrequencies.get(dst)+1);
		*/
		
		if(usageFrequencies.get("usr"+user)==null)
		{
			usageFrequencies.put("usr"+user, new Hashtable<String, int[]>());
		}
		if(usageFrequencies.get("usr"+user).get(src)==null)
		{
			usageFrequencies.get("usr"+user).put(src, new int[2]);
		}
		usageFrequencies.get("usr"+user).get(src)[0]++;
		if(usageFrequencies.get("usr"+user).get(dst)==null)
		{
			usageFrequencies.get("usr"+user).put(dst, new int[2]);
		}
		usageFrequencies.get("usr"+user).get(dst)[1]++;
		
		if(usersSrc.getNode("usr"+user)==null)
		{
			usersSrc.addNode("usr"+user);
		}
		if(usersSrc.getNode(src)==null)
		{
			usersSrc.addNode(src);
		}
		if(usersSrc.getEdge("usr"+user+" "+src)==null)
		{
			usersSrc.addEdge("usr"+user+" "+src, "usr"+user, src);
		}
		
		if(usersDst.getNode("usr"+user)==null)
		{
			usersDst.addNode("usr"+user);
		}
		if(usersDst.getNode(dst)==null)
		{
			usersDst.addNode(dst);
		}
		if(usersDst.getEdge("usr"+user+" "+dst)==null)
		{
			usersDst.addEdge("usr"+user+" "+dst, "usr"+user, dst);
		}
	}
	
	private static List<Object[] /*Node, weight*/> getJaccardNeighborNewNodesFreqWeight(Node user, 
			int srcDst)
	{
		List<Object[]> jaccardRankings=getJaccardRankings(user, srcDst);
		Hashtable<String, Double> recs=new Hashtable<>();
		
		List<Node> neighbors=new ArrayList<Node>();
		Iterator<Node> neighborIt=user.getNeighborNodeIterator();
		while(neighborIt.hasNext())
		{
			neighbors.add(neighborIt.next());
		}
		for(Object[] neighbor: jaccardRankings)
		{
			List<Node> neighborNeighbors=new ArrayList<Node>();
			Iterator<Node> neighborNeighborIt=((Node)neighbor[0]).getNeighborNodeIterator();
			while(neighborNeighborIt.hasNext())
			{
				neighborNeighbors.add(neighborNeighborIt.next());
			}		
			
			for(Node neighborCheck: neighborNeighbors)
			{
				if(!neighbors.contains(neighborCheck))
				{
					if(recs.get(neighborCheck.getId())==null)
					{
						recs.put(neighborCheck.getId(), 0.0);
					}
					recs.put(neighborCheck.getId(), recs.get(neighborCheck.getId())
							+((double)neighbor[1])
							*usageFrequencies.get(((Node)neighbor[0]).getId()).get(neighborCheck.getId())[srcDst]);
				}
			}
		}
		
		List<Object[]> recList=new ArrayList<Object[]>();
		for(String node: recs.keySet())
		{
			recList.add(new Object[]{node, recs.get(node)});
		}
		Collections.sort(recList, new Comparator<Object[]>() //TODO: highest at start or end?
		{
			@Override
			public int compare(Object[] o1, Object[] o2) 
			{
				return (int)Math.signum((double)o1[1]-(double)o2[1]);
			}
		});
		return recList;
	}
	
	private static List<Object[] /*Node, weight*/> getJaccardRankings(Node node, int srcDst)
	{
		List<Object[]> jaccardRankings=new ArrayList<>();
		
		List<Node> neighbors=new ArrayList<Node>();
		Iterator<Node> neighborIt=node.getNeighborNodeIterator();
		while(neighborIt.hasNext())
		{
			neighbors.add(neighborIt.next());
		}
		List<Node> neighborNeighbors=new ArrayList<Node>();
		for(Node neighbor: neighbors)
		{
			neighborIt=neighbor.getNeighborNodeIterator();
			while(neighborIt.hasNext())
			{
				Node nnn=neighborIt.next();
				if(!nnn.equals(node) && !neighborNeighbors.contains(nnn))
				{
					neighborNeighbors.add(nnn);
				}
			}
		}
		
		for(Node nn: neighborNeighbors)
		{
			List<Node> union=new ArrayList<>();
			double unionCount=0.0;
			double intersectionCount=0.0;
			
			List<Node> neighborNeighborNeighbors=new ArrayList<Node>();
			Iterator<Node> neighborNeighborIt=nn.getNeighborNodeIterator();
			while(neighborNeighborIt.hasNext())
			{
				neighborNeighborNeighbors.add(neighborNeighborIt.next());
			}
			
			for(Node neighborCheck: neighbors)
			{
				unionCount+=usageFrequencies.get(node.getId()).get(neighborCheck.getId())[srcDst];
				if(neighbors.contains(neighborCheck))
				{
					intersectionCount+=usageFrequencies.get(node.getId()).get(neighborCheck.getId())[srcDst];
					intersectionCount+=usageFrequencies.get(nn.getId()).getOrDefault(neighborCheck.getId(), new int[]{0, 0})[srcDst];
				}
			}
			for(Node neighborCheck: neighborNeighborNeighbors)
			{
				unionCount+=usageFrequencies.get(nn.getId()).get(neighborCheck.getId())[srcDst];
			}
			jaccardRankings.add(new Object[]{nn, intersectionCount/unionCount});
		}
		Collections.sort(jaccardRankings, new Comparator<Object[]>() //TODO: highest at start or end?
		{
			@Override
			public int compare(Object[] o1, Object[] o2) 
			{
				return (int)Math.signum((double)o1[1]-(double)o2[1]);
			}
		});
		return jaccardRankings;
	}
	
	
	static String currentDir=System.getProperty("user.dir");
	private static double[] testHeuristic() throws IOException
	{
		List<String> lines=Files.readAllLines(new File("/home/c/workspace/Globus/data/transfers-william.csv").toPath());
		List<String[]> splitData=new ArrayList<>();
		int amt=0;
		lines.remove(0).split(",");
		lines.remove(lines.size()-1);
		for(String line: lines)
		{
			splitData.add(line.split(","));
			amt++;
			if(amt%1000000==0)
			{
				System.out.println(amt);
				if (amt>0)
				{
					break;
				}
			}
		}
		
		List<String[]> user_eps=readDeletionLines();
		
		JaccardHeuristic jaccH=new JaccardHeuristic(splitData, user_eps, 0);
		double srcAttempts=0.0;
		double srcCorrect=0.0;
		
		double dstAttempts=0.0;
		double dstCorrect=0.0;
		
		int topN=50;
		
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
					srcRecs.add((String)(jaccH.getNthBestSrc(userID, recInd, currentDate)[0]));
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
					dstRecs.add((String)(jaccH.getNthBestDst(userID, recInd, currentDate)[0]));
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
			jaccH.nextTxn();
		}
		
		return new double[]{srcCorrect/srcAttempts, dstCorrect/dstAttempts};
	}
	
	private static double[] testHeuristicOnCorrFailures() throws IOException
	{
		List<String> lines=Files.readAllLines(new File("/home/c/workspace/Globus/data/transfers-william.csv").toPath());
		List<String[]> splitData=new ArrayList<>();
		int amt=0;
		lines.remove(0).split(",");
		lines.remove(lines.size()-1);
		for(String line: lines)
		{
			splitData.add(line.split(","));
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
		
		List<String[]> user_eps=readDeletionLines();
		
		List<Integer> failureIntegers=null;
		ObjectInputStream oIn=new ObjectInputStream(new FileInputStream(new File("/home/c/workspace/Globus/data/FailedPredictionsInds")));
		try 
		{
			failureIntegers=(List<Integer>)oIn.readObject();
		} 
		catch (ClassNotFoundException e1) 
		{
			e1.printStackTrace();
		}
		
		JaccardHeuristic jaccH=new JaccardHeuristic(splitData, user_eps, 0);
		double srcAttempts=0.0;
		double srcCorrect=0.0;
		
		double dstAttempts=0.0;
		double dstCorrect=0.0;
		
		int topN=10000;
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		
		int failInd=0;
		for(int splitInd=0; splitInd<splitData.size(); splitInd++)
		{
			String[] line=splitData.get(splitInd);
			String userID=line[1];
			String srcID=line[2];
			String dstID=line[3];
			
			if(splitInd==failureIntegers.get(failInd))
			{
				failInd++;
				long currentDate;
				try 
				{
					currentDate = sdf.parse(line[4]).getTime();
					List<String> srcRecs=new ArrayList<>();
					for(int recInd=0; recInd<topN; recInd++)
					{
						srcRecs.add((String)(jaccH.getNthBestSrc(userID, recInd, currentDate)[0]));
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
						dstRecs.add((String)(jaccH.getNthBestDst(userID, recInd, currentDate)[0]));
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
			}
			jaccH.nextTxn();
		}
		
		return new double[]{srcCorrect/srcAttempts, dstCorrect/dstAttempts};
	}
	
	static List<String[]> readDeletionLines()
	{
		List<String> lines=null;
		try
		{
			lines = Files.readAllLines(new File("/home/c/workspace/Globus/data/short-eps.csv").toPath());
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
	
	private static void testJaccMeasure()
	{
		usersSrc=new SingleGraph("test");
		usersSrc.addNode("ua");
		usersSrc.addNode("ub");
		
		usersSrc.addNode("ea");
		usersSrc.addNode("eb");
		usersSrc.addNode("ec");
		
		usersSrc.addEdge("ua-ea", "ua", "ea");
		
		usersSrc.addEdge("ub-ea", "ub", "ea");
		usersSrc.addEdge("ub-eb", "ub", "eb");
		usersSrc.addEdge("ub-ec", "ub", "ec");
		
		usageFrequencies=new Hashtable<>();
		usageFrequencies.put("ua", new Hashtable<>());
		usageFrequencies.get("ua").put("ea", new int[]{2, 0});
		
		usageFrequencies.put("ub", new Hashtable<>());
		usageFrequencies.get("ub").put("ea", new int[]{1, 0});
		usageFrequencies.get("ub").put("eb", new int[]{2, 0});
		usageFrequencies.get("ub").put("ec", new int[]{5, 0});
		
		List<Object[]> recs=getJaccardNeighborNewNodesFreqWeight(usersSrc.getNode("ua"), 0);
		
  		int u=0;
	}
	
	public static void main(String[] args) throws IOException
	{
		/*
		double[] accuracies=testHeuristic();
		System.out.println("Correlation Heuristics Accuracy src: "+accuracies[0]+" dst: "+accuracies[1]);
		*/
		
		double[] accuracies=testHeuristicOnCorrFailures();
		System.out.println("Correlation Heuristics Accuracy src: "+accuracies[0]+" dst: "+accuracies[1]);	
		
		//testJaccMeasure();
	}
	
}
