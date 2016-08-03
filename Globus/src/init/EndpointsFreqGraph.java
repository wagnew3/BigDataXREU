package init;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.swingViewer.View;
import org.graphstream.ui.swingViewer.Viewer;

import graphStreamFramework.ButtonTextBox;
import graphStreamFramework.ButtonTextBoxer;
import graphStreamFramework.ClickChange;
import graphStreamFramework.ClickChanger;

public class EndpointsFreqGraph extends ClickChange implements ButtonTextBox
{
	
	static String currentDir=System.getProperty("user.dir");
	static double visPow=0.75;
	static int numVisInitially=8000;
	static double visScale=0.0;
	Graph graph;
	JTextArea nodeDetails;
	int n=400;
	Node lastClicked=null;
	
	boolean save=false;
	
	Hashtable<String, String> machineIDToUserID;
	Hashtable<String, String> uIDToEmail;
	Hashtable<String, Object[]> pointsInfo;
	long totalSent;
	
	List<String[]> splitData=null;
	List<String[]> uIDSplitData=null;
	List<String[]> short_eps=null;
	
	View view;
	Viewer viewer;
	
	String graphInfo="Contains Sensitive Information--DO NOT RELEASE\n"
			+"\n"
			+"Graph of Globus Transfers of Top "+n+" Users\n"
			+"       and Users They Transferred With\n"
			+"\n"
			+"-Vertex Size Proportional to Transfer Amount\n"
			+"-User Vertices Approximately Closer to User\n"
			+"   Vertices Transferred More Frequently With\n"
			+"-Edge Thickness Proportional to Transfer Amount\n"
			+"-Red Vertices Sent More, Blue Vertices Received More\n"
			+"-Move with Mouse, Zoom with Mouse Wheel\n"
			+"-Click on a Vertex for More Information\n"
			+"Created and Maintained by wagnew3@gatech.edu\n"
			+"\n"
			+"\n";
	
	public static void main(String[] args) throws IOException
	{
		new EndpointsFreqGraph();
	}
	
	public EndpointsFreqGraph()
	{
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		try 
		{
			InputStream in=new FileInputStream("/home/willie/workspace/Globus/src/userTransferStyle.css");//getClass().getResourceAsStream("");
			byte[] cssBytes=new byte[in.available()];
			in.read(cssBytes);
			styleSheet=new String(cssBytes);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		System.out.println("Creating Graph of Globus Transfers");
		
		Object[] result;
		
		if(save)
		{
			System.out.print("Loading Data...");
			splitData=readLinesFullTransfers("full-trans-tab.csv");
			uIDSplitData=readUIDs();
			short_eps=readShortEps();
			System.out.println();
			System.out.println("Analyzing Data");
		}
		else
		{
			System.out.print("Loading Data...\n");
		}
		
		createGraph(splitData, uIDSplitData, short_eps);
		
		displayGraph(graph);
	}
	
	public Graph createGraph(List<String[]> splitData, List<String[]> uIDSplitData, List<String[]> short_eps)
	{
		Object[] result;
		if(save)
		{
			machineIDToUserID=machineIDToUserID(short_eps);
			uIDToEmail=userIDToEmails(uIDSplitData);
			result=topNUPpoints(splitData, machineIDToUserID, n);
			saveRestore(new Object[]{result, machineIDToUserID, uIDToEmail}, save, "");
		}
		else
		{
			Object[] data=saveRestore(null, save, "");
			result=(Object[])(data[0]);
			machineIDToUserID=(Hashtable<String, String>)(data[1]);
			uIDToEmail=(Hashtable<String, String>)(data[2]);
		}
		List<String> topPoints=(List<String>)result[0];
		pointsInfo=(Hashtable<String, Object[]>)result[1];
		totalSent=(Long)result[2];
		Long mostIn=(Long)result[3];
		Long mostOut=(Long)result[4];
		
		System.out.println("Generating Graph");
		setVisParams(pointsInfo, totalSent, uIDToEmail);
		graph=createGraph(pointsInfo, totalSent, uIDToEmail, mostIn, mostOut);
		return graph;
	}
	
	public Object[] saveRestore(Object[] data, boolean save, String ID)
	{
		if(save)
		{
			try 
			{
				FileOutputStream fOut=new FileOutputStream(currentDir+"/src/graphData"+ID);
				ObjectOutputStream oOut=new ObjectOutputStream(fOut);
				oOut.writeObject(data);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		else
		{
			try 
			{
				InputStream fIn=new FileInputStream(currentDir+"/src/graphData"+ID);//getClass().getResourceAsStream(currentDir+"/src/graphData"+ID);
				ObjectInputStream oIn=new ObjectInputStream(fIn);
				Object[] restoredData=(Object[])oIn.readObject();
				return restoredData;
			} 
			catch (IOException | ClassNotFoundException e) 
			{
				e.printStackTrace();
			}
		}
		return null;
	}
	
	void displayGraph(Graph graph)
	{
		JFrame frame = new JFrame();
        frame.setSize(320, 240);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        graph.addAttribute("ui.quality");
        graph.addAttribute("ui.antialias");
        
        viewer=new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_SWING_THREAD);
        viewer.enableAutoLayout();
        view=viewer.addDefaultView(false);
        new ClickChanger(this, viewer, graph);
        //new UIUpdater(view, graph).start();
        
        frame.setLayout(new BorderLayout());
        frame.addMouseWheelListener(new GraphMouseWheelListener(view));
        new MousePositionListener(view, (Component)view).start();
        frame.add((Component)view, BorderLayout.CENTER);
        nodeDetails=new JTextArea(30, 30);
        nodeDetails.setText(graphInfo);
        nodeDetails.setFont(new Font("Serif", Font.PLAIN, 12));
        nodeDetails.setWrapStyleWord(true);
        
        JPanel listPane = new JPanel();
        listPane.setLayout(new BorderLayout());
        
        /*
        JScrollPane jScrollPane = new JScrollPane(nodeDetails);
        listPane.add(jScrollPane, BorderLayout.NORTH);
        JTextArea searchBox=new JTextArea(20, 1);
        JButton button=new JButton("Find");
        listPane.add(searchBox, BorderLayout.CENTER);
        listPane.add(button, BorderLayout.SOUTH);
        new ButtonTextBoxer(button, searchBox, this);
        */
        
        frame.add(listPane, BorderLayout.EAST);
        frame.setVisible(true);
        
        //explore(graph, view);
	}
	
	List<String[]> readLines()
	{
		List<String> lines=null;
		try
		{
			lines = Files.readAllLines(new File(currentDir+"/data/full-transfers.csv").toPath());
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
	
	List<String[]> readUIDs()
	{
		List<String> uIDLines=null;
		try 
		{
			uIDLines = Files.readAllLines(new File(currentDir+"/data/users.csv").toPath());
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
	
	List<String[]> readShortEps()
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
	
	static Object[]/*Hashtable<String, Hashtable<Integer, Long>>*/ topNUsersSendEnpoints(List<String[]> data, int n)
	{
		Hashtable<String, Hashtable<String, Long>> endpointTransferAmt=new Hashtable<>();
		Hashtable<String, Hashtable<String, Long>> endpointFreqs=new Hashtable<>();
		Hashtable<String, IDData<Long>> usageAmt=new Hashtable<>();
		Hashtable<String, Long> netFlow=new Hashtable<>();
		Hashtable<String, Long> endpointUsage=new Hashtable<>();
		for(String[] line: data)
		{
			if(!line[2].isEmpty() && !line[3].isEmpty())
			{
				String userID=line[1];
				String dstID=line[3];
				Long amt=Long.parseLong(line[10]);
				
				if(endpointTransferAmt.get(userID)==null)
				{
					endpointTransferAmt.put(userID, new Hashtable<String, Long>());
					endpointFreqs.put(userID, new Hashtable<String, Long>());
					netFlow.put(userID, 0L);
				}
				if(netFlow.get(dstID)==null)
				{
					netFlow.put(dstID, 0L);
				}
				if(endpointTransferAmt.get(userID).get(dstID)==null)
				{
					endpointTransferAmt.get(userID).put(dstID, 0L);
					endpointFreqs.get(userID).put(dstID, 0L);
				}
				endpointTransferAmt.get(userID).put(dstID, 
						endpointTransferAmt.get(userID).get(dstID)+amt);
				endpointFreqs.get(userID).put(dstID, 
						endpointFreqs.get(userID).get(dstID)+1);
				
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
		Hashtable<String, Hashtable<String, Long>> topEndpointUsage=new Hashtable<>();
		Hashtable<String, Hashtable<String, Long>> topEndpointFreqs=new Hashtable<>();
		for(int ind=usageAmtList.size()-1; ind>usageAmtList.size()-1-n && ind>0; ind--)
		{
			topEndpointUsage.put(usageAmtList.get(ind).ID, endpointTransferAmt.get(usageAmtList.get(ind).ID));
			topEndpointFreqs.put(usageAmtList.get(ind).ID, endpointFreqs.get(usageAmtList.get(ind).ID));
			totalSent+=(Long)usageAmtList.get(ind).data;
		}
		
		return new Object[]{topEndpointUsage, totalSent, usageAmt, endpointUsage, topEndpointFreqs};
	}
	
	Hashtable<String, String> machineIDToUserID(List<String[]> short_eps)
	{
		Hashtable<String, String> machineIDToUserID=new Hashtable<>();
		for(String[] line: short_eps)
		{
			machineIDToUserID.put(line[0], line[2]);
		}
		return machineIDToUserID;
	}
	
	static Object[]/*Hashtable<String, Object[]{Hashtable<String, Long>(point, amt xfered), Hashtable<String, Long>(point, # xfers), Long(total flow), Long(net flow)>, long total flow, long totalTransfers*/ 
			topNUPpoints(List<String[]> data, Hashtable<String, String> machineIDToUserID, int n)
	{
		Hashtable<String, Object[]> info=new Hashtable<>();
		for(String[] line: data)
		{
			if(!line[14].isEmpty() && !line[15].isEmpty())
			{
				String srcID=line[14];
				String dstID=line[15];
				Long amt=Long.parseLong(line[37]);
				
				if(info.get(srcID)==null)
				{
					Object[] userInfo=new Object[]{new Hashtable<String, Long>(), 
							new Hashtable<String, Long>(), 0L, 0L, 0L};
					info.put(srcID, userInfo);
				}
				if(((Hashtable<String, Long>)info.get(srcID)[0]).get(dstID)==null)
				{
					((Hashtable<String, Long>)info.get(srcID)[0]).put(dstID, 0L);
					((Hashtable<String, Long>)info.get(srcID)[1]).put(dstID, 0L);
				}
				((Hashtable<String, Long>)info.get(srcID)[0]).put(dstID,
						((Hashtable<String, Long>)info.get(srcID)[0]).get(dstID)+amt);
				((Hashtable<String, Long>)info.get(srcID)[1]).put(dstID,
						((Hashtable<String, Long>)info.get(srcID)[1]).get(dstID)+1);
				info.get(srcID)[2]=((Long)info.get(srcID)[2])+amt;
				info.get(srcID)[3]=((Long)info.get(srcID)[3])+amt;
				info.get(srcID)[4]=((Long)info.get(srcID)[3])+1;
				
				if(info.get(dstID)==null)
				{
					Object[] userInfo=new Object[]{new Hashtable<String, Long>(), 
							new Hashtable<String, Long>(), 0L, 0L, 0L};
					info.put(dstID, userInfo);
				}
				if(((Hashtable<String, Long>)info.get(dstID)[0]).get(srcID)==null)
				{
					((Hashtable<String, Long>)info.get(dstID)[0]).put(srcID, 0L);
					((Hashtable<String, Long>)info.get(dstID)[1]).put(srcID, 0L);
				}
				((Hashtable<String, Long>)info.get(dstID)[0]).put(srcID,
						((Hashtable<String, Long>)info.get(dstID)[0]).get(srcID)+amt);
				((Hashtable<String, Long>)info.get(dstID)[1]).put(srcID,
						((Hashtable<String, Long>)info.get(dstID)[1]).get(srcID)+1);
				info.get(dstID)[2]=((Long)info.get(dstID)[2])+amt;
				info.get(dstID)[3]=((Long)info.get(dstID)[3])-amt;
				info.get(dstID)[4]=((Long)info.get(dstID)[2])+1;
			}
		}
		
		List<IDData<Long>> usageAmtList=new ArrayList<>();
		for(String userID: info.keySet())
		{
			usageAmtList.add(new IDData<Long>(userID, ((Long)info.get(userID)[4])*((Hashtable<String, Long>)info.get(userID)[0]).size()));
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
		
		return new Object[]{topUsers, info, totalSent, mostIn, mostOut};
	}
	
	List<String[]> readLinesFullTransfers(String csvFileName)
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
	
	static Hashtable<String, String> userIDToEmails(List<String[]> userInfo)
	{
		Hashtable<String, String> userIDToEmails=new Hashtable<>();
		for(String[] line: userInfo)
		{
			userIDToEmails.put(line[0], line[2]);
		}
		return userIDToEmails;
	}
	
	static void setVisParams(Hashtable<String, Object[]> pointsInfo,
			Long totalSent, Hashtable<String, String> idToEmail)
	{
		List<Double> nodeVis=new ArrayList<>();
		Hashtable<String, Node> nodes=new Hashtable<>();
		
		for(String user: pointsInfo.keySet())
		{
			if(nodes.get(user)==null)
			{
				double vis=Math.pow(((double)(Long)(pointsInfo.get(user)[2])/totalSent), visPow);
				nodeVis.add(vis);
			}
		}
		
		Collections.sort(nodeVis);
		int minDefDisInd=Math.min(nodeVis.size(), numVisInitially);
		visScale=1.0/nodeVis.get(nodeVis.size()-minDefDisInd);
	}
	
	double minVis=1.6;
	
	Graph createGraph(Hashtable<String, Object[]> pointsInfo,
			Long totalSent, Hashtable<String, String> idToEmail, 
			Long mostIn, Long mostOut)
	{
		Graph graph=new SingleGraph("TopNUsersAndDests");
		graph.addAttribute("ui.stylesheet", styleSheet);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");
		graph.setAttribute("layout.quality", 4);
		
		double mostUniqueUsers=0;
		double mostTransactions=0.0;
		double mostEdgeTransactions=0;
		for(Object[] userInfo: pointsInfo.values())
		{
			Hashtable<String, Long> userFreq=(Hashtable<String, Long>)(userInfo[1]);
			if(userFreq.size()>mostUniqueUsers)
			{
				mostUniqueUsers=userFreq.size();
			}
			double numberTransactions=0.0;
			for(Long numTxns: userFreq.values())
			{
				numberTransactions+=numTxns;
				if(numTxns>mostEdgeTransactions)
				{
					mostEdgeTransactions=numTxns;
				}
			}
			if(numberTransactions>mostTransactions)
			{
				mostTransactions=numberTransactions;
			}
		}

		Hashtable<String, Node> nodes=new Hashtable<>();
		
		NumberFormat formatter=new DecimalFormat("#0.0000"); 
		for(String user: pointsInfo.keySet())
		{
			if(nodes.get(user)==null)
			{
				double vis=visScale*Math.pow(((double)(Long)(pointsInfo.get(user)[4])*((Hashtable<String, Long>)(pointsInfo.get(user)[1])).size()/totalSent), visPow);
				if(vis>minVis)
				{
					Node userNode=graph.addNode(user);
					if(idToEmail.get(user)!=null)
					{
						userNode.addAttribute("ui.label", idToEmail.get(user));
					}
					else
					{
						userNode.addAttribute("ui.label", user);
					}
					userNode.addAttribute("ui.class", "user");
					
					int totalTxns=0;
					for(Long txnNum: ((Hashtable<String, Long>)(pointsInfo.get(user)[1])).values())
					{
						totalTxns+=txnNum;
					}
					
					
					userNode.setAttribute("ui.size", 24*Math.pow(((double)(Long)(pointsInfo.get(user)[4])*((Hashtable<String, Long>)(pointsInfo.get(user)[1])).size()/totalSent), 0.15));
					
					userNode.setAttribute("ui.style", "visibility: "+formatter.format(vis)+";");
					//userNode.addAttribute("ui.color", Math.pow(totalTxns/mostTransactions, 0.5));
					nodes.put(user, userNode);
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
						double visSrc=visScale*Math.pow(((double)(Long)(pointsInfo.get(user)[4])*((Hashtable<String, Long>)(pointsInfo.get(user)[1])).size()/totalSent), visPow);
						double visDst=visScale*Math.pow((double)(Long)(pointsInfo.get(dstID)[4])*((Hashtable<String, Long>)(pointsInfo.get(dstID)[1])).size()/totalSent, visPow);
						double visibility=Math.min(visSrc, visDst);
						if(visibility>minVis)
						{
							Edge newEdge=graph.addEdge(user+"_"+dstID, user, dstID);
							
							newEdge.setAttribute("ui.style", "visibility: "+formatter.format(visibility)+";");
							newEdge.setAttribute("layout.weight", 0.0001/Math.log(((Hashtable<String, Long>)(pointsInfo.get(user)[1])).get(dstID)+1));
							newEdge.addAttribute("ui.class", "norm");
							newEdge.setAttribute("ui.size", 0.25*Math.log(((Hashtable<String, Long>)(pointsInfo.get(user)[1])).get(dstID)+1));
							newEdge.addAttribute("ui.color", Math.pow(((Hashtable<String, Long>)(pointsInfo.get(user)[1])).get(dstID)/mostEdgeTransactions, 0.5));
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
	
	protected static String styleSheet;

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

	@Override
	public void updateReleased(String releaseNode) 
	{
		
	}

	@Override
	public void buttonPressed(String textString) 
	{
		textString=textString.trim();
		GraphicNode selected=viewer.getGraphicGraph().getNode(textString);
		if(selected!=null)
		{
			double x=selected.getX();
			double y=selected.getY();
			view.getCamera().setViewCenter(x,
    				y,
    				visScale*Math.pow(((double)(Long)(pointsInfo.get(textString)[2])/totalSent), visPow));
			updateClicked(textString);
		}
	}

	@Override
	public void buttonReleased(String textString) 
	{
		// TODO Auto-generated method stub
		
	}
	
}

//class UIUpdater extends Thread
//{
//	
//	View view;
//	Graph graph;
//	
//	UIUpdater(View view, Graph graph)
//	{
//		this.view=view;
//		this.graph=graph;
//	}
//	
//	public void run() 
//	{
//        while(true)
//        {
//        	double zoomLevel=view.getCamera().getViewPercent();
//        	for(Node node: graph.getEachNode())
//        	{
//        		if((double)node.getAttribute("willie_visLevel")<zoomLevel)
//        		{
//        			node.addAttribute("ui.hide");
//        		}
//        		else
//        		{
//        			node.removeAttribute("ui.hide");
//        		}
//        	}
//        }
//	}
//	
//}

//class MousePositionListener extends Thread
//{
//	
//	double moveBounds=0.5;
//	double moveSpeed=1.0;
//	double moveGradient=1.75;
//	View view;
//	Component frame;
//	
//	MousePositionListener(View view, Component frame)
//	{
//		this.view=view;
//		this.frame=frame;
//	}
//	
//	public void run() 
//	{
//        while(true)
//        {
//        	Point mousePoint=MouseInfo.getPointerInfo().getLocation();
//        	SwingUtilities.convertPointFromScreen(mousePoint, frame);
//        	Rectangle graphFrame=frame.getBounds();
//        	
//        	int minX=(int)Math.min(mousePoint.getX()-graphFrame.getMinX(), graphFrame.getMaxX()-mousePoint.getX());
//        	int minY=(int)Math.min(mousePoint.getY()-graphFrame.getMinY(), graphFrame.getMaxY()-mousePoint.getY());
//        	
//        	if(minX>=0 && minY>=0
//        			&& (minX<moveBounds*graphFrame.getWidth() || minY<moveBounds*graphFrame.getHeight()))
//        	{
//        		double diag=Math.sqrt(Math.pow(graphFrame.getWidth(), 2)+Math.pow(graphFrame.getHeight(), 2));
//        		double pxToGU=view.getCamera().getGraphDimension()/diag;
//        		double deltaX=view.getCamera().getViewPercent()*pxToGU*moveSpeed*Math.signum(mousePoint.getX()-graphFrame.getCenterX())*Math.pow(Math.max(Math.abs(mousePoint.getX()-graphFrame.getCenterX())-Math.abs((1.0-moveBounds)*graphFrame.getCenterX()), 0.0), moveGradient)/graphFrame.getWidth();
//        		double deltaY=view.getCamera().getViewPercent()*pxToGU*moveSpeed*Math.signum(graphFrame.getCenterY()-mousePoint.getY())*Math.pow(Math.max(Math.abs(graphFrame.getCenterY()-mousePoint.getY())-Math.abs((1.0-moveBounds)*graphFrame.getCenterY()), 0.0), moveGradient)/graphFrame.getHeight();
//        		
//        		view.getCamera().setViewCenter(view.getCamera().getViewCenter().x+deltaX,
//        				view.getCamera().getViewCenter().y+deltaY,
//        				view.getCamera().getViewCenter().z);
//        	}
//        	try 
//        	{
//				Thread.sleep(5);
//			} 
//        	catch (InterruptedException e) 
//        	{
//				e.printStackTrace();
//			}
//        }
//    }
//	
//}
//
//class GraphMouseWheelListener implements MouseWheelListener
//{
//	
//	View view;
//	double viewPercent=1.0;
//	double viewScale=0.9;
//	
//	public GraphMouseWheelListener(View view)
//	{
//		this.view=view;
//	}
//
//	@Override
//	public void mouseWheelMoved(MouseWheelEvent e)
//	{
//		if(e.getWheelRotation()<0)
//		{
//			viewPercent*=Math.pow(viewScale, -e.getWheelRotation());
//			view.getCamera().setViewPercent(viewPercent);
//		}
//		else if(e.getWheelRotation()>0)
//		{
//			viewPercent/=Math.pow(viewScale, e.getWheelRotation());
//			view.getCamera().setViewPercent(viewPercent);
//		}
//		
//	}
//	
//}
//
//class GraphMouseListener implements MouseListener
//{
//	
//	View view;
//
//	public GraphMouseListener(View view)
//	{
//		this.view=view;
//	}
//	
//	@Override
//	public void mouseClicked(MouseEvent e) 
//	{
//		// TODO Auto-generated method stub
//		
//	}
//
//	double moveFactor=1.5;
//	@Override
//	public void mousePressed(MouseEvent e) 
//	{
//		int x=e.getX();
//		int y=e.getY();
//		int curX=(int)view.getCamera().getViewCenter().x;
//		int curY=(int)view.getCamera().getViewCenter().y;
//		
//		int newX=(int)(curX+(x-curX)*moveFactor);
//		int newY=(int)(curY+(y-curY)*moveFactor);
//		
//		view.getCamera().setViewCenter(newX, newY, view.getCamera().getViewCenter().z);
//	}
//
//	@Override
//	public void mouseReleased(MouseEvent e) 
//	{
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void mouseEntered(MouseEvent e) 
//	{
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void mouseExited(MouseEvent e) 
//	{
//		// TODO Auto-generated method stub
//		
//	}
//	
//}

