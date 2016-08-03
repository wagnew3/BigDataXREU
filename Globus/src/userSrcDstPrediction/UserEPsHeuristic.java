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
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import init.IDData;

public class UserEPsHeuristic extends EPHeuristic
{
	
	Hashtable<String, List<String>> allUserEPs;
	List<IDData<Long>> deletionTimes;
	Hashtable<String, String> epToOwner;
	long highestSeenEP=-1L;
	

	public UserEPsHeuristic(List<String[]> transactions, List<String[]> deletions, 
			List<String[]> users, List<String[]> authIDs, long date) 
	{
		super(transactions, deletions, users, authIDs, date);
		deletionTimes=new ArrayList<>();
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		allUserEPs=new Hashtable<>();
		epToOwner=new Hashtable<>();
		int txnInd=0;
		
		for(String[] deletionLine: deletions)
		{
			
			String epID=deletionLine[0];
			String userID=deletionLine[2];
			
			if(deletionLine.length>4)
			{
				try
				{
					Long deletionTime=sdf.parse(deletionLine[4]).getTime();
					deletionTimes.add(new IDData<Long>(epID, deletionTime));
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			
			if(allUserEPs.get(userID)==null)
			{
				allUserEPs.put(userID, new ArrayList<>());
			}
			allUserEPs.get(userID).add(0, epID);
			
			epToOwner.put(epID, userID);
			
			txnInd++;
		}
		Collections.sort(deletionTimes);
		
		for(List<String> userEPs: allUserEPs.values())
		{
			Collections.sort(userEPs, new Comparator<String>() 
			{
				@Override
				public int compare(String o1, String o2)
				{
					if(o1.isEmpty())
					{
						if(o2.isEmpty())
						{
							return 0;
						}
						else
						{
							return -1;
						}
					}
					else if(o2.isEmpty())
					{
						return 1;
					}
					return (int)Math.signum(Long.parseLong(o1)-Long.parseLong(o2));
				}
			});
		}
	}

	@Override
	public Object[] getNthBestSrc(String userID, int n, long date)
	{
		while(!deletionTimes.isEmpty() && (Long)deletionTimes.get(0).data<date)
		{
			String ep=deletionTimes.get(0).ID;
			String user=epToOwner.get(ep);
			if(user!=null)
			{
				allUserEPs.get(user).remove(ep);
			}
			deletionTimes.remove(0);
		}
		List<String> userEPs=allUserEPs.get(userID);
		long newestEPNum=-1;
		int newestEPInd=-1;
		if(userEPs!=null)
		{
			
			for(int epInd=0; epInd<userEPs.size(); epInd++)
			{
				long epNum=Long.parseLong(userEPs.get(epInd));
				if(epNum>newestEPNum && newestEPNum<=highestSeenEP)
				{
					newestEPNum=epNum;
					newestEPInd=epInd;
				}
			}	
		}
		if(newestEPInd>=n)
		{
			return new Object[]{userEPs.get(newestEPInd-n), 1.0f/(float)Math.max(allUserEPs.get(userID).size(), 1.0f)};
		}
		else
		{
			if(userEPs!=null && !userEPs.isEmpty())
			{
				int ind=(int)Math.floor(Math.random()*userEPs.size());
				return new Object[]{userEPs.get(ind), 1.0f/(float)Math.max(allUserEPs.get(userID).size(), 1.0f)};
			}
			else
			{
				return new Object[]{"", -1.0f};
			}
		}
	}

	@Override
	public float getSrcWeight(String userID, String srcID) 
	{
		if(srcID.isEmpty() || allUserEPs.get(userID)==null || allUserEPs.get(userID).isEmpty() || !allUserEPs.get(userID).contains(srcID))
		{
			return 0.0f;
		}
		else
		{
			return 1.0f/Math.max(allUserEPs.get(userID).size(), 1.0f);
		}
	}

	@Override
	public Object[] getNthBestDst(String userID, int n, long date) 
	{
		while(!deletionTimes.isEmpty() && (Long)deletionTimes.get(0).data<date)
		{
			String ep=deletionTimes.get(0).ID;
			String user=epToOwner.get(ep);
			if(user!=null)
			{
				allUserEPs.get(user).remove(ep);
			}
			deletionTimes.remove(0);
		}
		
		List<String> userEPs=allUserEPs.get(userID);
		long newestEPNum=-1;
		int newestEPInd=-1;
		if(userEPs!=null)
		{
			
			for(int epInd=0; epInd<userEPs.size(); epInd++)
			{
				long epNum=Long.parseLong(userEPs.get(epInd));
				if(epNum>newestEPNum && newestEPNum<=highestSeenEP)
				{
					newestEPNum=epNum;
					newestEPInd=epInd;
				}
			}
		}
		if(newestEPInd>=n)
		{
			return new Object[]{userEPs.get(newestEPInd-n), 1.0f/(float)Math.max(allUserEPs.get(userID).size(), 1.0f)};
		}
		else
		{
			if(userEPs!=null && !userEPs.isEmpty())
			{
				int ind=(int)Math.floor(Math.random()*userEPs.size());
				return new Object[]{userEPs.get(ind), 1.0f/(float)Math.max(allUserEPs.get(userID).size(), 1.0f)};
			}
			else
			{
				return new Object[]{"", -1.0f};
			}
		}
	}

	@Override
	public float getDstWeight(String userID, String dstID)
	{
		if(dstID.isEmpty() || allUserEPs.get(userID)==null || allUserEPs.get(userID).isEmpty() || !allUserEPs.get(userID).contains(dstID))
		{
			return 0.0f;
		}
		else
		{
			return 1.0f/Math.max(allUserEPs.get(userID).size(), 1.0f);
		}
	}

	@Override
	protected void updateHeuristic(String[] newTransaction) 
	{
		String srcID=newTransaction[14];
		if(!srcID.isEmpty() && highestSeenEP<Long.parseLong(srcID))
		{
			highestSeenEP=Long.parseLong(srcID);
		}
		
		String dstID=newTransaction[15];
		if(!dstID.isEmpty() && highestSeenEP<Long.parseLong(dstID))
		{
			highestSeenEP=Long.parseLong(dstID);
		}
	}
	
	@Override //true: src, false: dst
	public float getAdditionalNetInfo(String userID, boolean srcDst)
	{
		if(allUserEPs.get(userID)==null)
		{
			return 0.0f;
		}
		else
		{
			return 1.0f/(float)Math.max(allUserEPs.get(userID).size(), 1.0f);
		}
	}
	
	static String currentDir=System.getProperty("user.dir");
	private static double[] testHeuristic() throws IOException
	{
		List<String[]> transactions=readLines("transfers-william.csv");
		List<String[]> deletions=readLines("short-eps.csv");
		List<String[]> users=readLines("users.csv");
		List<String[]> authUsers=readLines("auth_users.csv");
		
		UserEPsHeuristic userEPH=new UserEPsHeuristic(transactions, deletions, 
				users, authUsers, -1); 
		
		double srcAttempts=0.0;
		double srcCorrect=0.0;
		
		double dstAttempts=0.0;
		double dstCorrect=0.0;
		
		int topN=1;
		
		int numberSrcInIns=0;
		int numberDstInIns=0;
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		
		for(String[] line: transactions)
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
					srcRecs.add((String)(userEPH.getNthBestSrc(userID, recInd, currentDate)[0]));
				}
				srcAttempts++;
				if(srcRecs.contains(srcID))
				{
					srcCorrect++;
				}
				
				List<String> dstRecs=new ArrayList<>();
				for(int recInd=0; recInd<topN; recInd++)
				{
					dstRecs.add((String)(userEPH.getNthBestDst(userID, recInd, currentDate)[0]));
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
			userEPH.nextTxn();
		}
		
		System.out.println("Src in inst: "+numberSrcInIns);
		System.out.println("Dst in inst: "+numberDstInIns);
		
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
		System.out.println("History Heuristics Accuracy src: "+accuracies[0]+" dst: "+accuracies[1]);
	}

}
