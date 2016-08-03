package init;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import graphStreamFramework.RandomWalk;

public class UsersEndPointsGraph extends UserTransfersGraph
{

	public static void main(String[] args)
	{
		new UsersEndPointsGraph();
	}
	
	public UsersEndPointsGraph()
	{
		super();
	}
	
	public Graph createGraph(List<String[]> splitData, List<String[]> uIDSplitData, List<String[]> short_eps)
	{
		try 
		{
			InputStream in=getClass().getResourceAsStream("usersEndPointsGraph.css");
			byte[] cssBytes=new byte[in.available()];
			in.read(cssBytes);
			styleSheet=new String(cssBytes);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		Object[] result;
		if(save)
		{
			machineIDToUserID=machineIDToUserID(short_eps);
			uIDToEmail=userIDToEmails(uIDSplitData);
			result=topNUPpoints(splitData, machineIDToUserID, n);
			saveRestore(new Object[]{result, machineIDToUserID, uIDToEmail}, save, "userEndpoints");
		}
		else
		{
			Object[] data=saveRestore(null, save, "userEndpoints");
			result=(Object[])(data[0]);
			machineIDToUserID=(Hashtable<String, String>)(data[1]);
			uIDToEmail=(Hashtable<String, String>)(data[2]);
		}
		List<String> topPoints=(List<String>)result[0];
		pointsInfo=(Hashtable<String, Object[]>)result[1];
		totalSent=(Long)result[2];
		Long mostIn=(Long)result[3];
		Long mostOut=(Long)result[4];
		Hashtable<String, Long> dstTransferred=(Hashtable<String, Long>)result[5];
		
		System.out.println("Generating Graph");
		setVisParams(pointsInfo, totalSent, uIDToEmail);
		graph=createGraph(pointsInfo, totalSent, uIDToEmail, mostIn, mostOut, dstTransferred);
		
		graph=randomWalkCluster(pointsInfo, totalSent, uIDToEmail, mostIn, mostOut, dstTransferred);
		
		return graph;
	}
	
	static Object[]/*Hashtable<String, Object[]{Hashtable<String, Long>(point, amt xfered), Hashtable<String, Long>(point, # xfers), Long(total flow), Long(net flow)>, long total flow, Hashtable<Dst Endpoint, amtXferred>*/ 
			topNUPpoints(List<String[]> data, Hashtable<String, String> machineIDToUserID, int n)
	{
		Hashtable<String, Object[]> info=new Hashtable<>();
		Hashtable<String, Long> dstTransferred=new Hashtable<>();
		for(String[] line: data)
		{
			if(!line[2].isEmpty() && !line[3].isEmpty())
			{
				String srcUserID=line[1];
				String dstUserID=line[3];
				Long amt=Long.parseLong(line[10]);
				
				if(info.get(srcUserID)==null)
				{
					Object[] userInfo=new Object[]{new Hashtable<String, Long>(), 
							new Hashtable<String, Long>(), 0L, 0L};
					info.put(srcUserID, userInfo);
				}
				if(((Hashtable<String, Long>)info.get(srcUserID)[0]).get(dstUserID)==null)
				{
					((Hashtable<String, Long>)info.get(srcUserID)[0]).put(dstUserID, 0L);
					((Hashtable<String, Long>)info.get(srcUserID)[1]).put(dstUserID, 0L);
				}
				((Hashtable<String, Long>)info.get(srcUserID)[0]).put(dstUserID,
						((Hashtable<String, Long>)info.get(srcUserID)[0]).get(dstUserID)+amt);
				((Hashtable<String, Long>)info.get(srcUserID)[1]).put(dstUserID,
						((Hashtable<String, Long>)info.get(srcUserID)[1]).get(dstUserID)+1);
				info.get(srcUserID)[2]=((Long)info.get(srcUserID)[2])+amt;
				info.get(srcUserID)[3]=((Long)info.get(srcUserID)[3])+amt;
				
				if(dstTransferred.get(dstUserID)==null)
				{
					dstTransferred.put(dstUserID, 0L);
				}
				dstTransferred.put(dstUserID, dstTransferred.get(dstUserID)+amt);
			}
		}
		
		List<IDData<Long>> usageAmtList=new ArrayList<>();
		for(String userID: info.keySet())
		{
			usageAmtList.add(new IDData<Long>(userID, ((Long)info.get(userID)[2])));
		}
		Collections.sort(usageAmtList);
		
		long totalSent=0;
		long mostIn=0;
		long mostOut=0;
		List<String> topUsers=new ArrayList<>();
		for(int ind=usageAmtList.size()-1; ind>usageAmtList.size()-1-n && ind>0; ind--)
		{
			topUsers.add(usageAmtList.get(ind).ID);
			totalSent+=(Long)usageAmtList.get(ind).data;
			if(mostIn>((Long)info.get(usageAmtList.get(ind).ID)[3]))
			{
				mostIn=((Long)info.get(usageAmtList.get(ind).ID)[3]);
			}
			if(mostOut<((Long)info.get(usageAmtList.get(ind).ID)[3]))
			{
				mostOut=((Long)info.get(usageAmtList.get(ind).ID)[3]);
			}
		}
		
		return new Object[]{topUsers, info, totalSent, mostIn, mostOut, dstTransferred};
	}
	
	Graph createGraph(Hashtable<String, Object[]> pointsInfo,
			Long totalSent, Hashtable<String, String> idToEmail, 
			Long mostIn, Long mostOut, Hashtable<String, Long> dstTransferred)
	{
		Graph graph=new SingleGraph("TopNUsersAndDests");
		graph.addAttribute("ui.stylesheet", styleSheet);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");
		
		Hashtable<String, Node> nodes=new Hashtable<>();
		
		NumberFormat formatter=new DecimalFormat("#0.0000"); 
		for(String user: pointsInfo.keySet())
		{
			if(nodes.get("User "+user)==null)
			{
				double vis=visScale*Math.pow(((double)(Long)(pointsInfo.get(user)[2])/totalSent), visPow);
				if(vis>minVis)
				{
					Node userNode=graph.addNode("User "+user);
					if(idToEmail.get(user)!=null)
					{
						userNode.addAttribute("ui.label", idToEmail.get(user));
					}
					else
					{
						userNode.addAttribute("ui.label", user);
					}
					userNode.addAttribute("ui.class", "src");
					userNode.setAttribute("ui.size", (int)(100*Math.sqrt(((double)(Long)(pointsInfo.get(user)[2])/totalSent))));
					
					userNode.setAttribute("ui.style", "visibility: "+formatter.format(vis)+";");
					userNode.addAttribute("ui.color", "red");
					nodes.put(user, userNode);
				}
			}
		}
		
		for(String dst: dstTransferred.keySet())
		{
			if(nodes.get("Dst "+dst)==null)
			{
				double vis=visScale*Math.pow(((double)dstTransferred.get(dst)/totalSent), visPow);
				if(vis>minVis)
				{
					Node userNode=graph.addNode("Dst "+dst);
					if(idToEmail.get(dst)!=null)
					{
						userNode.addAttribute("ui.label", idToEmail.get(dst));
					}
					else
					{
						userNode.addAttribute("ui.label", dst);
					}
					userNode.addAttribute("ui.class", "dst");
					userNode.setAttribute("ui.size", (int)(100*Math.sqrt(((double)(dstTransferred.get(dst))/totalSent))));
					
					userNode.setAttribute("ui.style", "visibility: "+formatter.format(vis)+";");
					userNode.addAttribute("ui.color", "blue");
					nodes.put(dst, userNode);
				}
			}
		}
		
		for(String user: pointsInfo.keySet())
		{
			for(String dstID: ((Hashtable<String, Long>)(pointsInfo.get(user)[0])).keySet())
			{
				try
				{
					if(graph.getEdge(user+"_"+dstID)==null && graph.getEdge(dstID+"_"+user)==null)
					{
						double visSrc=visScale*Math.pow(((double)(Long)(pointsInfo.get(user)[2])/totalSent), visPow);
						double visDst=visScale*Math.pow((double)dstTransferred.get(dstID)/totalSent, visPow);
						double visibility=Math.min(visSrc, visDst);
						if(visibility>minVis)
						{
							Edge newEdge=graph.addEdge("User "+user+"_Dst "+dstID, "User "+user, "Dst "+dstID);
							
							newEdge.setAttribute("ui.style", "visibility: "+formatter.format(visibility)+";");
							newEdge.setAttribute("layout.weight", 0.333/Math.pow(((Hashtable<String, Long>)(pointsInfo.get(user)[1])).get(dstID)+1, 0.25));
							newEdge.addAttribute("ui.class", "norm");
							newEdge.setAttribute("ui.size", Math.pow(1.0+400*((Hashtable<String, Long>)(pointsInfo.get(user)[0])).get(dstID)/totalSent, 0.75));
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		
		return graph;
		/*
		Viewer viewer = graph.display(true);
		ViewPanel view = viewer.getDefaultView();
		view.getCamera().setViewPercent(0.5);
		//view.resizeFrame(800, 600);
		*/
	}
	
	@Override
	public void updateClicked(String clickedNode) 
	{
		if(lastClicked!=null)
		{
			freeLastClicked(lastClicked);
		}
		Node clicked=graph.getNode(clickedNode);
		
		String infoText="Email: "+uIDToEmail.get(clickedNode)+"\n"
			+"User ID: "+clickedNode+"\n"
			+"Total Transferred: "+(Long)(pointsInfo.get(clickedNode)[2])+" bytes\n"
			+"---Users Transferred With---\n";
		clicked.setAttribute("ui.class", "clicked");
		Collection<Edge> edges=clicked.getEdgeSet();
		Edge[] edgesArray=edges.toArray(new Edge[0]);
		List<IDData<Long>> transferredToUsers=new ArrayList<>();
		for(Edge edge: edgesArray)
		{
			edge.setAttribute("ui.class", "clicked");
			if(!edge.getNode0().equals(clicked))
			{
				edge.getNode0().setAttribute("ui.class", "adjClicked");
			}
			else
			{
				edge.getNode1().setAttribute("ui.class", "adjClicked");
			}
		}
		
		for(String dst: ((Hashtable<String, Long>)(pointsInfo.get(clickedNode)[0])).keySet())
		{
			transferredToUsers.add(new IDData<Long>(dst, ((Hashtable<String, Long>)(pointsInfo.get(clickedNode)[0])).get(dst)));
		}
		
		Collections.sort(transferredToUsers);
		for(int ind=transferredToUsers.size()-1; ind>=0; ind--)
		{
			IDData<Long> transferredToUser=transferredToUsers.get(ind);
			infoText+="Email: "+uIDToEmail.get(transferredToUser.ID)+" ID: "+transferredToUser.ID+"\n"
					+"   Transferred: "+((Hashtable<String, Long>)(pointsInfo.get(clickedNode)[0])).get(transferredToUser.ID)+" bytes\n"
					+"   Transfers: "+((Hashtable<String, Long>)(pointsInfo.get(clickedNode)[1])).get(transferredToUser.ID)+"\n";
		}
		Object[] o=pointsInfo.get(clickedNode);
		nodeDetails.setText(graphInfo+infoText);
		lastClicked=clicked;
	}
	
	private void freeLastClicked(Node lastClicked)
	{
		String infoText="";
		nodeDetails.setText(graphInfo+infoText);
		lastClicked.setAttribute("ui.class", "");
		Collection<Edge> edges=lastClicked.getEdgeSet();
		Edge[] edgesArray=edges.toArray(new Edge[0]);
		for(Edge edge: edgesArray)
		{
			edge.setAttribute("ui.class", "norm");
			if(!edge.getNode0().equals(lastClicked))
			{
				edge.getNode0().setAttribute("ui.class", "");
			}
			else
			{
				edge.getNode1().setAttribute("ui.class", "");
			}
		}
	}
	
	//double clusteringStart
	Graph randomWalkCluster(Hashtable<String, Object[]> pointsInfo,
			Long totalSent, Hashtable<String, String> idToEmail, 
			Long mostIn, Long mostOut, Hashtable<String, Long> dstTransferred)
	{
		Hashtable<Edge, Double> edgeWeights=new Hashtable<>();
		Graph graph=new SingleGraph("TopNUsersAndDests");
		graph.addAttribute("ui.stylesheet", styleSheet);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");
		
		Hashtable<String, Node> nodes=new Hashtable<>();
		
		NumberFormat formatter=new DecimalFormat("#0.0000"); 
		for(String user: pointsInfo.keySet())
		{
			if(nodes.get("User "+user)==null)
			{
				Node userNode=graph.addNode("User "+user);
			}
		}
		
		for(String dst: dstTransferred.keySet())
		{
			if(nodes.get("Dst "+dst)==null)
			{
				Node userNode=graph.addNode("Dst "+dst);
			}
		}
		
		for(String user: pointsInfo.keySet())
		{
			for(String dstID: ((Hashtable<String, Long>)(pointsInfo.get(user)[0])).keySet())
			{
				try
				{
					if(graph.getEdge(user+"_"+dstID)==null && graph.getEdge(dstID+"_"+user)==null)
					{
						Edge newEdge=graph.addEdge("User "+user+"_Dst "+dstID, "User "+user, "Dst "+dstID);
						edgeWeights.put(newEdge, (double)((Hashtable<String, Long>)(pointsInfo.get(user)[0])).get(dstID)/totalSent);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		
		RandomWalk randomWalker=new RandomWalk(graph, edgeWeights);
		int[][] walkResult=randomWalker.timesVisitedFromRandomVertexSeq(10, 3);
		int u=0;
		
		return null;
	}

}
