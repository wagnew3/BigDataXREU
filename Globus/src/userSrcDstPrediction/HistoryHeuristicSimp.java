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

public class HistoryHeuristicSimp extends EPHeuristic 
{

	Hashtable<String, Object[]> transactionHistories;

	public HistoryHeuristicSimp(List<String[]> transactions, List<String[]> deletions, long date) 
	{
		super(transactions, deletions, date);
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		transactionHistories=new Hashtable<>();
		
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
				IDData<Long> srcDate=new IDData<Long>(src, currentDate);
				IDData<Long> dstDate=new IDData<Long>(dst, currentDate);
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
	}

	@Override
	public Object[] getNthBestSrc(String userID, int n, long date) 
	{
		if(transactionHistories.get(userID)!=null)
		{
			List<IDData<Long>> srcHistory=(List<IDData<Long>>)(transactionHistories.get(userID)[0]);
			List<String> history=new ArrayList<>();
			List<String> toremove=new ArrayList<>();
			int histInd=0;
			for(int deleteInd=0; deleteInd<srcHistory.size(); deleteInd++)
			{
				if((deletions.get(srcHistory.get(deleteInd))!=null && deletions.get(srcHistory.get(deleteInd))<=date))
				{
					srcHistory.remove(deleteInd);
					deleteInd--;
				} 
				if(date<(Long)srcHistory.get(deleteInd).data)
				{
					histInd=deleteInd-1;
					break;
				}
			}
	
			while(history.size()<=n && histInd>=0)
			{
				String srcID=srcHistory.get(histInd).ID;
				if(!history.contains(srcID))
				{
					history.add(srcID);
				}
				histInd--;
			}
			
			if(history.size()==n+1)
			{
				return new Object[]{history.get(n), 1.0/n};
			}
		}
		return new Object[]{"", -1.0};
	}

	@Override
	public double getSrcWeight(String userID, String srcID) 
	{
		List<IDData<Long>> srcHistory=(List<IDData<Long>>)(transactionHistories.get(userID)[0]);
		List<String> history=new ArrayList<>();
		int histInd=0;
		for(int deleteInd=0; deleteInd<srcHistory.size(); deleteInd++)
		{
			if((deletions.get(srcHistory.get(deleteInd))!=null && deletions.get(srcHistory.get(deleteInd))<=date))
			{
				srcHistory.remove(deleteInd);
				deleteInd--;
			} 
			if(date<(Long)srcHistory.get(deleteInd).data)
			{
				histInd=deleteInd-1;
				break;
			}
		}

		while(!history.contains(srcID) && histInd>=0)
		{
			String considerSrcID=srcHistory.get(histInd).ID;
			if(!history.contains(considerSrcID))
			{
				history.add(considerSrcID);
			}
			histInd--;
		}
		
		if(history.contains(srcID))
		{
			return 1.0/history.size();
		}
		else
		{
			return 0.0;
		}
	}

	@Override
	public Object[] getNthBestDst(String userID, int n, long date) 
	{
		if(transactionHistories.get(userID)!=null)
		{
			List<IDData<Long>> dstHistory=(List<IDData<Long>>)(transactionHistories.get(userID)[1]);
			List<String> history=new ArrayList<>();
			int histInd=0;
			for(int deleteInd=0; deleteInd<dstHistory.size(); deleteInd++)
			{
				if((deletions.get(dstHistory.get(deleteInd))!=null && deletions.get(dstHistory.get(deleteInd))<=date))
				{
					dstHistory.remove(deleteInd);
					deleteInd--;
				} 
				if(date<(Long)dstHistory.get(deleteInd).data)
				{
					histInd=deleteInd-1;
					break;
				}
			}
	
			while(history.size()<=n && histInd>=0)
			{
				String srcID=dstHistory.get(histInd).ID;
				if(!history.contains(srcID))
				{
					history.add(srcID);
				}
				histInd--;
			}
			
			if(history.size()==n+1)
			{
				return new Object[]{history.get(n), 1.0/n};
			}
		}
		return new Object[]{"", -1.0};
	}

	@Override
	public double getDstWeight(String userID, String dstID) 
	{
		List<IDData<Long>> dstHistory=(List<IDData<Long>>)(transactionHistories.get(userID)[1]);
		List<String> history=new ArrayList<>();
		List<String> toremove=new ArrayList<>();
		int histInd=0;
		for(int deleteInd=0; deleteInd<dstHistory.size(); deleteInd++)
		{
			if((deletions.get(dstHistory.get(deleteInd))!=null && deletions.get(dstHistory.get(deleteInd))<=date))
			{
				dstHistory.remove(deleteInd);
				deleteInd--;
			} 
			if(date<(Long)dstHistory.get(deleteInd).data)
			{
				histInd=deleteInd-1;
				break;
			}
		}

		while(!history.contains(dstID) && histInd>=0)
		{
			String considerSrcID=dstHistory.get(histInd).ID;
			if(!history.contains(considerSrcID))
			{
				history.add(considerSrcID);
			}
			histInd--;
		}
		
		if(history.contains(dstID))
		{
			return 1.0/history.size();
		}
		else
		{
			return 0.0;
		}
	}

	@Override
	protected void updateHeuristic(String[] newTransaction)
	{
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		String user=newTransaction[1];
		String src=newTransaction[2];
		String dst=newTransaction[3];
		Long currentDate=null;
		try 
		{
			currentDate=sdf.parse(newTransaction[4]).getTime();
			transactionInd++;
			IDData<Long> srcDate=new IDData<Long>(src, currentDate);
			IDData<Long> dstDate=new IDData<Long>(dst, currentDate);
			if(transactionHistories.get(user)==null)
			{
				Object[] histories=new Object[]{new ArrayList<IDData<Long>>(), new ArrayList<IDData<Long>>()};
				transactionHistories.put(user, histories);
			}
			((List<IDData<Long>>)(transactionHistories.get(user)[0])).remove(srcDate);
			((List<IDData<Long>>)(transactionHistories.get(user)[0])).add(srcDate);
			((List<IDData<Long>>)(transactionHistories.get(user)[1])).remove(dstDate);
			((List<IDData<Long>>)(transactionHistories.get(user)[1])).add(dstDate);
		} 
		catch (ParseException e)
		{
			e.printStackTrace();
		}
	}
	
	static String currentDir=System.getProperty("user.dir");
	private static double[] testHeuristic() throws IOException
	{
		List<String> lines=Files.readAllLines(new File(currentDir+"/data/transfers-william.csv").toPath());
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
					//break;
				}
			}
		}
		
		List<String[]> user_eps=readDeletionLines();
		
		HistoryHeuristic histH=new HistoryHeuristic(splitData, user_eps, 0);
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
					srcRecs.add((String)(histH.getNthBestSrc(userID, recInd, currentDate)[0]));
				}
				srcAttempts++;
				if(srcRecs.contains(srcID))
				{
					srcCorrect++;
				}
				
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
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			histH.nextTxn();
		}
		
		return new double[]{srcCorrect/srcAttempts, dstCorrect/dstAttempts};
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
	
	public static void main(String[] args) throws IOException
	{
		double[] accuracies=testHeuristic();
		System.out.println("History Heuristics Accuracy src: "+accuracies[0]+" dst: "+accuracies[1]);
	}
	
}
