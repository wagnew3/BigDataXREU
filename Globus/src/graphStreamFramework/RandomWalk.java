package graphStreamFramework;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.Iterator;

public class RandomWalk 
{
	
	Graph graph;
	Hashtable<Edge, Double> edgeWeights;
	Hashtable<Node, Integer> nodeMapping;
	
	public RandomWalk(Graph graph, Hashtable<Edge, Double> edgeWeights)
	{
		this.graph=graph;
		this.edgeWeights=edgeWeights;
	}
	
	public int[][] timesVisitedFromRandomVertex(int numberWalks, int walkLength)
	{
		nodeMapping=new Hashtable<>();
		int nodeInd=0;
		for(Node node: graph.getEachNode())
		{
			nodeMapping.put(node, nodeInd);
			nodeInd++;
		}
		int[][] timesVisitedFromRandomVertex=new int[graph.getNodeCount()][graph.getNodeCount()];
		for(int walkNumber=0; walkNumber<numberWalks; walkNumber++)
		{
			Node startNode;
			Node currentNode;
			startNode=graph.getNode((int)(Math.random()*graph.getNodeCount()));
			currentNode=startNode;
			List<Node> visited=new ArrayList<>();
			visited.add(currentNode);
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
					weights.add(edgeWeights.get(edge));
					weightSum+=weights.get(weights.size()-1);
				}
				double rand=Math.random();
				int ind=0;
				while(rand>0 && ind<neighbors.size())
				{
					rand-=weights.get(ind)/weightSum;
					ind++;
				}
				visited.add(currentNode);
				currentNode=neighbors.get(ind-1);
				for(Node prevNode: visited)
				{
					timesVisitedFromRandomVertex[nodeMapping.get(prevNode)][nodeMapping.get(currentNode)]++;
				}
				
			}
		}
		return timesVisitedFromRandomVertex;
	}
	
	public int[][] timesVisitedFromRandomVertexSeq(int numberWalks, int walkLength)
	{
		nodeMapping=new Hashtable<>();
		int nodeInd=0;
		for(Node node: graph.getEachNode())
		{
			nodeMapping.put(node, nodeInd);
			nodeInd++;
		}
		int[][] timesVisitedFromRandomVertex=new int[graph.getNodeCount()][graph.getNodeCount()];
		
		for(Node startNode: graph.getNodeSet())
		{
			for(int walkNumber=0; walkNumber<numberWalks; walkNumber++)
			{
				Node currentNode;
				startNode=graph.getNode((int)(Math.random()*graph.getNodeCount()));
				currentNode=startNode;
				List<Node> visited=new ArrayList<>();
				visited.add(currentNode);
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
						weights.add(edgeWeights.get(edge));
						weightSum+=weights.get(weights.size()-1);
					}
					double rand=Math.random();
					int ind=0;
					while(rand>0 && ind<neighbors.size())
					{
						rand-=weights.get(ind)/weightSum;
						ind++;
					}
					visited.add(currentNode);
					currentNode=neighbors.get(ind-1);
					for(Node prevNode: visited)
					{
						timesVisitedFromRandomVertex[nodeMapping.get(prevNode)][nodeMapping.get(currentNode)]++;
					}
					
				}
			}
		}
		return timesVisitedFromRandomVertex;
	}

}
