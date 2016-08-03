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
import java.util.List;

import init.IDData;

public class MostUniqueUsersHeuristic extends EPHeuristic
{
	int topN;
	
	Hashtable<String, Hashtable<String, String>> srcEndpointsUniqueUsers;
	List<IDData<Integer>> srcTopEPS;
	Hashtable<String, Hashtable<String, String>> dstEndpointsUniqueUsers;
	List<IDData<Integer>> dstTopEPS;
	
	List<IDData<Long>> deletionTimes;
	
	float totalUniqueUsersSrc;
	float totalUniqueUsersDst;

	public MostUniqueUsersHeuristic(List<String[]> transactions, List<String[]> deletions, List<String[]> users,
			List<String[]> authIDs, long date, int topN) 
	{
		super(transactions, deletions, users, authIDs, date);
		
		this.topN=topN;
		srcEndpointsUniqueUsers=new Hashtable<>();
		srcTopEPS=new ArrayList<>();
		dstEndpointsUniqueUsers=new Hashtable<>();
		dstTopEPS=new ArrayList<>();
		deletionTimes=new ArrayList<>();
		totalUniqueUsersSrc=0;
		totalUniqueUsersDst=0;
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		for(String[] transaction: transactions)
		{
			Long currentDate=null;
			try 
			{
				currentDate=sdf.parse(transaction[3]).getTime();
				if(currentDate>=date)
				{
					break;
				}
				String user=transaction[2];
				String src=transaction[14];
				String dst=transaction[15];
				transactionInd++;
				if(!src.isEmpty())
				{
					if(srcEndpointsUniqueUsers.get(src)==null)
					{
						srcEndpointsUniqueUsers.put(src, new Hashtable<>());
					}
					if(srcEndpointsUniqueUsers.get(src).get(user)==null)
					{
						srcEndpointsUniqueUsers.get(src).put(user, user);
						totalUniqueUsersSrc++;
					}
					updateTopEPs(srcTopEPS, src, srcEndpointsUniqueUsers.get(src).size(), topN);
				}
				if(!dst.isEmpty())
				{
					if(dstEndpointsUniqueUsers.get(dst)==null)
					{
						dstEndpointsUniqueUsers.put(dst, new Hashtable<>());
					}
					if(dstEndpointsUniqueUsers.get(dst).get(user)==null)
					{
						dstEndpointsUniqueUsers.get(dst).put(user, user);
						totalUniqueUsersDst++;
					}
					dstEndpointsUniqueUsers.get(dst).put(user, user);
					updateTopEPs(dstTopEPS, dst, dstEndpointsUniqueUsers.get(dst).size(), topN);
				}
			} 
			catch (ParseException e)
			{
				e.printStackTrace();
			}
		}
		
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
		}
		Collections.sort(deletionTimes);
	}
	
	void updateTopEPs(List<IDData<Integer>> topEPs, String ep, int numUUsers, int topN)
	{
		if(!topEPs.contains(new IDData<Integer>(ep, 0)))
		{
			if(topEPs.size()<topN)
			{
				topEPs.add(new IDData<Integer>(ep, numUUsers));
				return;
			}
			boolean added=false;
			for(int ind=0; ind<topEPs.size(); ind++)
			{
				if((int)topEPs.get(ind).data<numUUsers)
				{
					topEPs.add(ind, new IDData<Integer>(ep, numUUsers));
					added=true;
					break;
				}
			}
			if(added)
			{
				topEPs.remove(topEPs.size()-1);
			}
		}
	}
	
	void deleteFromTopEPs(List<IDData<Integer>> topEPs, 
			Hashtable<String, Hashtable<String, String>> allUniqueUsers, int ind)
	{
		topEPs.remove(ind);
		int mostUniqueUsers=-1;
		String topEP="";
		for(String ep: allUniqueUsers.keySet())
		{
			if(!topEPs.contains(new IDData<Integer>(ep, 0)) 
					&& allUniqueUsers.get(ep).size()>mostUniqueUsers)
			{
				mostUniqueUsers=allUniqueUsers.get(ep).size();
				topEP=ep;
			}
		}
		topEPs.add(new IDData<Integer>(topEP, mostUniqueUsers));
	}

	@Override
	public Object[] getNthBestSrc(String userID, int n, long date) 
	{
		while(!deletionTimes.isEmpty() && (Long)deletionTimes.get(0).data<date)
		{
			String ep=deletionTimes.get(0).ID;
			srcEndpointsUniqueUsers.remove(ep);
			int ind=srcTopEPS.indexOf(new IDData<Integer>(userID, 0));
			if(ind>-1)
			{
				deleteFromTopEPs(srcTopEPS, 
						srcEndpointsUniqueUsers, 
						ind);
			}
			
			dstEndpointsUniqueUsers.remove(ep);
			ind=dstTopEPS.indexOf(new IDData<Integer>(userID, 0));
			if(ind>-1)
			{
				deleteFromTopEPs(dstTopEPS, 
						dstEndpointsUniqueUsers, 
						ind);
			}
			
			deletionTimes.remove(0);
		}
		
		if(n<srcTopEPS.size())
		{
			return new Object[]{srcTopEPS.get(n).ID, (int)srcTopEPS.get(n).data/totalUniqueUsersSrc};
		}
		else
		{
			return new Object[]{"", -1.0f};
		}
	}

	@Override
	public float getSrcWeight(String userID, String srcID) 
	{
		int rankingInd=srcTopEPS.indexOf(srcID);
		if(rankingInd>-1)
		{
			return 1.0f/rankingInd;
		}
		else
		{
			return 0.0f;
		}
	}

	@Override
	public Object[] getNthBestDst(String userID, int n, long date)
	{
		if(n<dstTopEPS.size())
		{
			return new Object[]{dstTopEPS.get(n).ID, (int)dstTopEPS.get(n).data/totalUniqueUsersDst};
		}
		else
		{
			return new Object[]{"", -1.0f};
		}
	}

	@Override
	public float getDstWeight(String userID, String dstID) 
	{
		int rankingInd=dstTopEPS.indexOf(dstID);
		if(rankingInd>-1)
		{
			return 1.0f/rankingInd;
		}
		else
		{
			return 0.0f;
		}
	}

	@Override
	protected void updateHeuristic(String[] newTransaction) 
	{
		String user=newTransaction[2];
		String src=newTransaction[14];
		String dst=newTransaction[15];
		if(!src.isEmpty())
		{
			if(srcEndpointsUniqueUsers.get(src)==null)
			{
				srcEndpointsUniqueUsers.put(src, new Hashtable<>());
			}
			if(srcEndpointsUniqueUsers.get(src).get(user)==null)
			{
				srcEndpointsUniqueUsers.get(src).put(user, user);
				totalUniqueUsersSrc++;
			}
			updateTopEPs(srcTopEPS, src, srcEndpointsUniqueUsers.get(src).size(), topN);
		}
		if(!dst.isEmpty())
		{
			if(dstEndpointsUniqueUsers.get(dst)==null)
			{
				dstEndpointsUniqueUsers.put(dst, new Hashtable<>());
			}
			if(dstEndpointsUniqueUsers.get(dst).get(user)==null)
			{
				dstEndpointsUniqueUsers.get(dst).put(user, user);
				totalUniqueUsersDst++;
			}
			dstEndpointsUniqueUsers.get(dst).put(user, user);
			updateTopEPs(dstTopEPS, dst, dstEndpointsUniqueUsers.get(dst).size(), topN);
		}
	}
	
	@Override //true: src, false: dst
	public float getAdditionalNetInfo(String userID, boolean srcDst)
	{
		if(srcDst)
		{
			if(srcTopEPS.size()>=2)
			{
				return (float)(int)srcTopEPS.get(0).data/(int)srcTopEPS.get(1).data;
			}
			else
			{
				return 0.0f;
			}
		}
		else
		{
			if(dstTopEPS.size()>=2)
			{
				return (float)(int)dstTopEPS.get(0).data/(int)dstTopEPS.get(1).data;
			}
			else
			{
				return 0.0f;
			}
		}
	}
	
	static String currentDir=System.getProperty("user.dir");
	private static double[] testHeuristic() throws IOException
	{
		List<String[]> transactions=readLines("transfers-william.csv");
		List<String[]> deletions=readLines("short-eps.csv");
		List<String[]> users=readLines("users.csv");
		List<String[]> authUsers=readLines("auth_users.csv");
		
		int topN=1;
		
		MostUniqueUsersHeuristic mUUserH=new MostUniqueUsersHeuristic(transactions, deletions, users, authUsers, 0, topN);
		
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
					srcRecs.add((String)(mUUserH.getNthBestSrc(userID, recInd, currentDate)[0]));
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
			mUUserH.nextTxn();
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
	
	public static void main(String[] args) throws IOException
	{
		double[] accuracies=testHeuristic();
		System.out.println("EnsNet Heuristics Accuracy src: "+accuracies[0]+" dst: "+accuracies[1]);
	}

}
