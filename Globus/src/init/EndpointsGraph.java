package init;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.graphstream.graph.Graph;

public class EndpointsGraph extends UserTransfersGraph
{
	
	public static void main(String[] args)
	{
		new EndpointsGraph();
	}
	
	public Graph createGraph(List<String[]> splitData, List<String[]> uIDSplitData, List<String[]> short_eps)
	{
		Object[] result;
		if(save)
		{
			machineIDToUserID=machineIDToUserID(short_eps);
			uIDToEmail=userIDToEmails(uIDSplitData);
			result=topNUPpoints(splitData, machineIDToUserID, n);
			saveRestore(new Object[]{result, machineIDToUserID, uIDToEmail}, save, "endpointstoendpoints");
		}
		else
		{
			Object[] data=saveRestore(null, save, "endpointstoendpoints");
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
	
	static Object[]/*Hashtable<String, Object[]{Hashtable<String, Long>(point, amt xfered), Hashtable<String, Long>(point, # xfers), Long(total flow), Long(net flow)>, long total flow*/ 
			topNUPpoints(List<String[]> data, Hashtable<String, String> machineIDToUserID, int n)
	{
		Hashtable<String, Object[]> info=new Hashtable<>();
		for(String[] line: data)
		{
			if(!line[2].isEmpty() && !line[3].isEmpty())
			{
				String srcUserID=line[2];
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
				
				if(info.get(dstUserID)==null)
				{
					Object[] userInfo=new Object[]{new Hashtable<String, Long>(), 
							new Hashtable<String, Long>(), 0L, 0L};
					info.put(dstUserID, userInfo);
				}
				if(((Hashtable<String, Long>)info.get(dstUserID)[0]).get(srcUserID)==null)
				{
					((Hashtable<String, Long>)info.get(dstUserID)[0]).put(srcUserID, 0L);
					((Hashtable<String, Long>)info.get(dstUserID)[1]).put(srcUserID, 0L);
				}
				((Hashtable<String, Long>)info.get(dstUserID)[0]).put(srcUserID,
						((Hashtable<String, Long>)info.get(dstUserID)[0]).get(srcUserID)+amt);
				((Hashtable<String, Long>)info.get(dstUserID)[1]).put(srcUserID,
						((Hashtable<String, Long>)info.get(dstUserID)[1]).get(srcUserID)+1);
				info.get(dstUserID)[2]=((Long)info.get(dstUserID)[2])+amt;
				info.get(dstUserID)[3]=((Long)info.get(dstUserID)[3])-amt;
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
		
		return new Object[]{topUsers, info, totalSent, mostIn, mostOut};
	}
	
}
