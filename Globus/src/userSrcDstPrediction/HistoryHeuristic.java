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

public class HistoryHeuristic extends EPHeuristic 
{
	
	public Hashtable<String, Object[]> transactionHistories;
	float maxHistSizeSrc=0.0f;
	float maxHistSizeDst=0.0f;

	public HistoryHeuristic(List<String[]> transactions, List<String[]> deletions, 
			List<String[]> users, List<String[]> authIDs, long date)
	{
		super(transactions, deletions, users, authIDs, date);
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		transactionHistories=new Hashtable<>();
		
		for(String[] line: transactions)
		{
			String user=line[2];
			String src=line[14];
			String dst=line[15];
			Long currentDate=null;
			try 
			{
				currentDate=sdf.parse(line[3]).getTime();
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
			if(((List<IDData<Long>>)(history[0])).size()>maxHistSizeSrc)
			{
				maxHistSizeSrc=((List<IDData<Long>>)(history[0])).size();
			}
			Collections.sort(((List<IDData<Long>>)(history[1])));
			if(((List<IDData<Long>>)(history[1])).size()>maxHistSizeDst)
			{
				maxHistSizeDst=((List<IDData<Long>>)(history[1])).size();
			}
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
			int histInd=srcHistory.size()-1;
			for(int deleteInd=0; deleteInd<srcHistory.size(); deleteInd++)
			{
				if((deletions.get(srcHistory.get(deleteInd))!=null && deletions.get(srcHistory.get(deleteInd))<=date))
				{
					srcHistory.remove(deleteInd);
					deleteInd--;
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
			if(history.size()>=n+1)
			{
				return new Object[]{history.get(n), 1.0f/(n+1)};
			}
			else
			{
				int y=0;
			}
		}
		return new Object[]{"", 0.0f};
	}

	@Override
	public float getSrcWeight(String userID, String srcID) 
	{
		if(!srcID.isEmpty() && transactionHistories.get(userID)!=null)
		{
			List<IDData<Long>> srcHistory=(List<IDData<Long>>)(transactionHistories.get(userID)[0]);
			Hashtable<String, String> history=new Hashtable<>();
			int histInd=srcHistory.size()-1;
			for(int deleteInd=0; deleteInd<srcHistory.size(); deleteInd++)
			{
				if((deletions.get(srcHistory.get(deleteInd))!=null && deletions.get(srcHistory.get(deleteInd))<=date))
				{
					srcHistory.remove(deleteInd);
					deleteInd--;
				}
			}
			while(histInd>=0)
			{
				String considerSrcID=srcHistory.get(histInd).ID;
				if(history.get(considerSrcID)==null)
				{
					history.put(considerSrcID, considerSrcID);
				}
				if(considerSrcID.equals(srcID))
				{
					return 1.0f/history.size();
				}
				histInd--;
			}
		}
		return 0.0f;
	}

	@Override
	public Object[] getNthBestDst(String userID, int n, long date) 
	{
		if(transactionHistories.get(userID)!=null)
		{
			List<IDData<Long>> dstHistory=(List<IDData<Long>>)(transactionHistories.get(userID)[1]);
			List<String> history=new ArrayList<>();
			int histInd=dstHistory.size()-1;
			for(int deleteInd=0; deleteInd<dstHistory.size(); deleteInd++)
			{
				if((deletions.get(dstHistory.get(deleteInd))!=null && deletions.get(dstHistory.get(deleteInd))<=date))
				{
					dstHistory.remove(deleteInd);
					deleteInd--;
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
			if(history.size()>=n+1)
			{
				return new Object[]{history.get(n), 1.0f/(n+1)};
			}
			else
			{
				int u=0;
			}
		}
		return new Object[]{"", 0.0f};
	}

	@Override
	public float getDstWeight(String userID, String dstID) 
	{
		if(!dstID.isEmpty() && transactionHistories.get(userID)!=null)
		{
			List<IDData<Long>> dstHistory=(List<IDData<Long>>)(transactionHistories.get(userID)[1]);
			Hashtable<String, String> history=new Hashtable<>();
			List<String> toremove=new ArrayList<>();
			int histInd=dstHistory.size()-1;
//			for(int deleteInd=0; deleteInd<dstHistory.size(); deleteInd++)
//			{
//				if((deletions.get(dstHistory.get(deleteInd))!=null && deletions.get(dstHistory.get(deleteInd))<=date))
//				{
//					dstHistory.remove(deleteInd);
//					deleteInd--;
//				}
//			}
			while(histInd>=0)
			{
				String considerSrcID=dstHistory.get(histInd).ID;
				if(history.get(considerSrcID)==null)
				{
					history.put(considerSrcID, considerSrcID);
				}
				if(considerSrcID.equals(dstID))
				{
					return 1.0f/history.size();
				}
				histInd--;
			}
		}
		return 0.0f;
	}

	@Override
	protected void updateHeuristic(String[] newTransaction)
	{
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		String user=newTransaction[2];
		String src=newTransaction[14];
		String dst=newTransaction[15];
		Long currentDate=null;
		try 
		{
			currentDate=sdf.parse(newTransaction[3]).getTime();
			IDData<Long> srcDate=new IDData<Long>(src, currentDate);
			IDData<Long> dstDate=new IDData<Long>(dst, currentDate);
			if(transactionHistories.get(user)==null)
			{
				Object[] histories=new Object[]{new ArrayList<IDData<Long>>(), new ArrayList<IDData<Long>>()};
				transactionHistories.put(user, histories);
			}
			List<IDData<Long>> transHists=((List<IDData<Long>>)(transactionHistories.get(user)[0]));
			for(int ind=0; ind<transHists.size(); ind++)
			{
				if(transHists.get(ind).ID.equals(src))
				{
					transHists.remove(ind);
					ind--;
				}
			}
			//((List<IDData<Long>>)(transactionHistories.get(user)[0])).remove(srcDate);
			transHists.add(srcDate);
			
			transHists=((List<IDData<Long>>)(transactionHistories.get(user)[1]));
			for(int ind=0; ind<transHists.size(); ind++)
			{
				if(transHists.get(ind).ID.equals(dst))
				{
					transHists.remove(ind);
					ind--;
				}
			}
			((List<IDData<Long>>)(transactionHistories.get(user)[1])).add(dstDate);
			
			if(((List<IDData<Long>>)(transactionHistories.get(user)[0])).size()>maxHistSizeSrc)
			{
				maxHistSizeSrc=((List<IDData<Long>>)(transactionHistories.get(user)[0])).size();
			}
			if(((List<IDData<Long>>)(transactionHistories.get(user)[1])).size()>maxHistSizeDst)
			{
				maxHistSizeDst=((List<IDData<Long>>)(transactionHistories.get(user)[1])).size();
			}
		} 
		catch (ParseException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override //true: src, false: dst
	public float getAdditionalNetInfo(String userID, boolean srcDst)
	{
		if(srcDst)
		{
			if(transactionHistories.get(userID)==null)
			{
				return 0.0f;
			}
			else
			{
				if( (float)(((List<IDData<Long>>)(transactionHistories.get(userID)[0])).size()/Math.max(500.0, maxHistSizeSrc))>1)
				{
					int u=0;
				}
				return (float)(((List<IDData<Long>>)(transactionHistories.get(userID)[0])).size()/Math.max(500.0, maxHistSizeSrc));
			}
		}
		else
		{
			if(transactionHistories.get(userID)==null)
			{
				return 0.0f;
			}
			else
			{
				return (float)(((List<IDData<Long>>)(transactionHistories.get(userID)[1])).size()/Math.max(500.0, maxHistSizeDst));
			}
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
