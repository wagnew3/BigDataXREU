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
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import init.IDData;

public class MostRecentlyUsedN 
{
	
	static String currentDir=System.getProperty("user.dir");

	public static void main(String[] args)
	{
		List<String[]> transactions=readLines();
		List<String[]> user_eps=readDeletionLines();
		
		Hashtable<String, Long> deleteDate=parseUserEpsToDeletions(user_eps);
		Hashtable<String, Object[] /*List<IDData<Long>>, List<IDData<Long>>*/> transactionHistories=parseTransactionsHistories(transactions);
		
		double[] result=mostRecentNAccuracy(deleteDate,
				transactionHistories,
				3);
		
		System.out.println("src accuracy: "+result[0]+" dst accuracy "+result[1]);
	}
	
	static Hashtable<String, Long> parseUserEpsToDeletions(List<String[]> user_eps)
	{
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		Hashtable<String, Long> deletions=new Hashtable<>();
		
		for(String[] line: user_eps)
		{
			if(line.length>4 && !line[4].equals(""))
			{
				int dotInd=line[4].lastIndexOf('.');
				long date=0;
				try 
				{
					date = sdf.parse(line[4].substring(0, dotInd)).getTime();
					String machineID=line[0];
					deletions.put(machineID, date);
				} 
				catch (ParseException e) 
				{
					e.printStackTrace();
				}
			}
		}
		
		return deletions;
	}
	
	static Hashtable<String, Object[]/*String src, String dst, Long date*/> parseTransactionsHistories(List<String[]> transactions)
	{
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		Hashtable<String, Object[]> transactionHistories=new Hashtable<>();
		
		for(String[] line: transactions)
		{
			String user=line[1];
			String src=line[2];
			String dst=line[3];
			Long date=null;
			try 
			{
				date = sdf.parse(line[4]).getTime();
				IDData<Long> srcDate=new IDData<Long>(src, date);
				IDData<Long> dstDate=new IDData<Long>(dst, date);
				if(transactionHistories.get(user)==null)
				{
					Object[] histories=new Object[]{new ArrayList<IDData<Long>>(), new ArrayList<IDData<Long>>()};
					transactionHistories.put(user, histories);
				}
				((List<IDData<Long>>)(transactionHistories.get(user)[0])).add(srcDate);
				((List<IDData<Long>>)(transactionHistories.get(user)[1])).add(dstDate);
			} 
			catch (ParseException e)
			{
				e.printStackTrace();
			}
		}
		
		for(Object[] history: transactionHistories.values())
		{
			Collections.sort(((List<IDData<Long>>)(history[0])));
			Collections.sort(((List<IDData<Long>>)(history[1])));
		}
		
		return transactionHistories;
	}
	
	static double[] mostRecentNAccuracy(Hashtable<String, Long> deleteDate,
										Hashtable<String, Object[]> transactionHistories,
										int n)
	{
		double srcSuccess=0.0;
		double srcTotal=0.0;
		double dstSuccess=0.0;
		double dstTotal=0.0;
		
		for(String user: transactionHistories.keySet())
		{
			int stopInd=0;
			List<IDData<Long>> srcHistory=(List<IDData<Long>>)(transactionHistories.get(user)[0]);
			Hashtable<String, IDData<Long>> history=new Hashtable<>();
			for(int tInd=0; tInd<srcHistory.size(); tInd++)
			{
				srcTotal++;
				if(srcTotal%10000==0)
				{
					System.out.println("Src Total: "+srcTotal);
				}
				Long date=(Long)srcHistory.get(tInd).data;
				
				List<String> toremove=new ArrayList<>();
				for(String srcID: history.keySet())
				{
					if((deleteDate.get(srcID)!=null && deleteDate.get(srcID)<=date))
					{
						toremove.add(srcID);
					}
				}
				for(String srcID: toremove)
				{
					history.remove(srcID);
				}
				int histInd=tInd-1;
				while(history.size()<n && histInd>=0 && histInd>=stopInd)
				{
					String srcID=srcHistory.get(histInd).ID;
					if(deleteDate.get(srcID)!=null && deleteDate.get(srcID)<=date)
					{
						srcHistory.remove(histInd);
						tInd--;
						histInd--;
						continue;
					}
					if(history.get(srcID)==null)
					{
						history.put(srcID, srcHistory.get(histInd));
					}
					histInd--;
				}
				
				if(histInd==-1)
				{
					stopInd=tInd;
				}
				
				if(history.get(srcHistory.get(tInd).ID)!=null)
				{
					srcSuccess++;
				}
				
				if(history.get(srcHistory.get(tInd).ID)==null)
				{
					String oldest="";
					Long oldestDate=Long.MAX_VALUE;
					for(String srcID: history.keySet())
					{
						Long tempDate=(Long)history.get(srcID).data;
						if(tempDate<oldestDate)
						{
							oldestDate=tempDate;
							oldest=srcID;
						}
					}
					history.remove(oldest);
					history.put(srcHistory.get(tInd).ID, srcHistory.get(tInd));
				}
			}
			
			stopInd=0;
			List<IDData<Long>> dstHistory=(List<IDData<Long>>)(transactionHistories.get(user)[1]);
			history=new Hashtable<>();
			for(int tInd=0; tInd<dstHistory.size(); tInd++)
			{
				dstTotal++;
				Long date=(Long)dstHistory.get(tInd).data;
				
				List<String> toremove=new ArrayList<>();
				for(String srcID: history.keySet())
				{
					if((deleteDate.get(srcID)!=null && deleteDate.get(srcID)<=date))
					{
						toremove.add(srcID);
					}
				}
				for(String srcID: toremove)
				{
					history.remove(srcID);
				}
				
				int histInd=tInd-1;
				while(history.size()<n && histInd>=0
						&& histInd>=stopInd)
				{
					String srcID=dstHistory.get(histInd).ID;
					if(deleteDate.get(srcID)!=null && deleteDate.get(srcID)<=date)
					{
						dstHistory.remove(histInd);
						tInd--;
						histInd--;
						continue;
					}
					if(history.get(srcID)==null)
					{
						history.put(srcID, dstHistory.get(histInd));
					}
					histInd--;
				}
				if(histInd==-1)
				{
					stopInd=tInd;
				}
				
				if(history.get(dstHistory.get(tInd).ID)!=null)
				{
					dstSuccess++;
				}
				
				if(history.get(dstHistory.get(tInd).ID)==null)
				{
					String oldest="";
					Long oldestDate=Long.MAX_VALUE;
					for(String srcID: history.keySet())
					{
						Long tempDate=(Long)history.get(srcID).data;
						if(tempDate<oldestDate)
						{
							oldestDate=tempDate;
							oldest=srcID;
						}
					}
					history.remove(oldest);
					history.put(dstHistory.get(tInd).ID, dstHistory.get(tInd));
				}
			}
		}
		
		return new double[]{dstSuccess/dstTotal, srcSuccess/srcTotal};
	}
	
	static List<String[]> readLines()
	{
		List<String> lines=null;
		try
		{
			lines = Files.readAllLines(new File(currentDir+"/data/transfers-william.csv").toPath());
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
	
	static List<String[]> readDeletionLines()
	{
		List<String> lines=null;
		try
		{
			lines = Files.readAllLines(new File(currentDir+"/data/short-eps.csv").toPath());
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
	
}