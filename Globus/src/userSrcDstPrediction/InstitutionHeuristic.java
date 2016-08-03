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

public class InstitutionHeuristic extends EPHeuristic
{
	
	Hashtable<String, IDData<Integer>> usageFreq;
	Hashtable<String, List<String>> numberUniqueInsUsers;

	Hashtable<String, List<IDData<Integer>>> topInstitutionEPs;
	Hashtable<String, List<String>> institutionEPs;
	public Hashtable<String, String> userToInstitution;
	Hashtable<String, String> epToInstitution;
	public Hashtable<String, Integer> userInstitutionType;//0=.com, 1=.edu, 2=.gov, 3=other
	public Hashtable<String, Integer> institutionSize;//0=.com, 1=.edu, 2=.gov, 3=other
	public double maxInstSize;
	List<IDData<Long>> deletionTimes;
	int topN;
	

	public InstitutionHeuristic(List<String[]> transactions, List<String[]> deletions, 
			List<String[]> users, List<String[]> authIDs, long date, int topN) 
	{
		super(transactions, deletions, users, authIDs, date);
		usageFreq=new Hashtable<>();
		numberUniqueInsUsers=new Hashtable<>();
		this.topN=topN;
		
		topInstitutionEPs=new Hashtable<>();
		userToInstitution=new Hashtable<>();
		epToInstitution=new Hashtable<>();
		deletionTimes=new ArrayList<>();
		userInstitutionType=new Hashtable<>();
		institutionEPs=new Hashtable<>();
		institutionSize=new Hashtable<>();
		maxInstSize=0;
		
		Hashtable<String, String> userIDToLongUserID=new Hashtable<>();
		Hashtable<String, String> longUserIDToUserID=new Hashtable<>();
		for(String[] line: users)
		{
			String user=line[0];
			String longID=line[6];
			userIDToLongUserID.put(user, longID);
			longUserIDToUserID.put(longID, user);
		}
		
		Hashtable<String, String> longUserToInstitution=new Hashtable<>();
		for(String[] line: authIDs)
		{
			String longID=line[0];
			String email=line[2];
			if(email.isEmpty() || !email.contains("@"))
			{
				email=line[1];
			}
			String suffix=email.substring(email.indexOf('@'));
			String host=null;
			if(suffix.contains("."))
			{
				host=suffix.substring(suffix.lastIndexOf('.'));
			}
			else
			{
				host=suffix.substring(1);
			}
			if(host.equals(".edu"))
			{
				suffix=suffix.substring(suffix.lastIndexOf('.', suffix.lastIndexOf('.')));
			}
			if(longUserIDToUserID.get(longID)!=null)
			{
				switch(host)
				{
					case "com":
						userInstitutionType.put(longUserIDToUserID.get(longID), 0);
					case "edu":
						userInstitutionType.put(longUserIDToUserID.get(longID), 1);
					case "gov":
						userInstitutionType.put(longUserIDToUserID.get(longID), 2);
					default:
						userInstitutionType.put(longUserIDToUserID.get(longID), 3);
				}
			}
			
			String institution=suffix;
			longUserToInstitution.put(longID, institution);
			if(institutionSize.get(institution)==null)
			{
				institutionSize.put(institution, 0);
			}
			institutionSize.put(institution, institutionSize.get(institution)+1);
			if(maxInstSize<institutionSize.get(institution))
			{
				maxInstSize=institutionSize.get(institution);
			}
		}
		
		for(String user: userIDToLongUserID.keySet())
		{
			if(longUserToInstitution.get(userIDToLongUserID.get(user))!=null)
			{
				userToInstitution.put(user, longUserToInstitution.get(userIDToLongUserID.get(user)));
			}
		}
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		
		for(String[] dLine: deletions)
		{
			String user=dLine[2];
			String epID=dLine[0];
			String insitution=userToInstitution.get(user);
			if(usageFreq.get(epID)==null)
			{
				usageFreq.put(epID, new IDData<Integer>(epID, 0));
			}
			if(numberUniqueInsUsers.get(epID)==null)
			{
				numberUniqueInsUsers.put(epID, new ArrayList<String>());
			}
			if(insitution!=null)
			{
				if(topInstitutionEPs.get(insitution)==null)
				{
					topInstitutionEPs.put(insitution, new ArrayList<IDData<Integer>>());
					institutionEPs.put(insitution, new ArrayList<String>());
				}
				institutionEPs.get(insitution).add(epID);
				epToInstitution.put(epID, insitution);
			}
			
			if(dLine.length>4)
			{
				try
				{
					Long deletionTime=sdf.parse(dLine[4]).getTime();
					deletionTimes.add(new IDData<Long>(epID, deletionTime));
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		Collections.sort(deletionTimes);
		
		sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
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
				
				usageFreq.get(src).data=(int)usageFreq.get(src).data+1;
				usageFreq.get(dst).data=(int)usageFreq.get(dst).data+1;
				
				if(numberUniqueInsUsers.get(src)!=null && !numberUniqueInsUsers.get(src).contains(user))
				{
					numberUniqueInsUsers.get(src).add(user);
				}
				if(numberUniqueInsUsers.get(dst)!=null && !numberUniqueInsUsers.get(dst).contains(user))
				{
					numberUniqueInsUsers.get(dst).add(user);
				}
			} 
			catch (ParseException e)
			{
				e.printStackTrace();
			}
		}
		
		for(String ep: usageFreq.keySet())
		{
			String inst=epToInstitution.get(ep);
			if(inst!=null)
			{
				updateTopEPs(topInstitutionEPs.get(inst), ep, (int)usageFreq.get(ep).data, topN);
			}
		}
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
	
	void deleteFromTopEPs(List<IDData<Integer>> topEPs, String inst, int ind)
	{
		topEPs.remove(ind);
		int mostUniqueUsers=-1;
		String topEP="";
		for(String ep: numberUniqueInsUsers.get(inst))
		{
			if(!topEPs.contains(new IDData<Integer>(ep, 0)) 
					&& numberUniqueInsUsers.get(ep).size()>mostUniqueUsers)
			{
				mostUniqueUsers=numberUniqueInsUsers.get(ep).size();
				topEP=ep;
			}
		}
		topEPs.add(new IDData<Integer>(topEP, mostUniqueUsers));
	}

	@Override
	public Object[] getNthBestSrc(String userID, int n, long date)
	{
		if(userToInstitution.get(userID)!=null
				&& topInstitutionEPs.get(userToInstitution.get(userID))!=null)
		{
			while(!deletionTimes.isEmpty() && (Long)deletionTimes.get(0).data<date)
			{
				String ep=deletionTimes.get(0).ID;
				String inst=epToInstitution.get(ep);
				if(inst!=null)
				{
					int ind=topInstitutionEPs.get(inst).indexOf(ep);
					if(ind>-1)
					{
						deleteFromTopEPs(topInstitutionEPs.get(inst), inst, ind);
					}
				}
				deletionTimes.remove(0);
			}
			List<IDData<Integer>> institutionEPsSortedList=topInstitutionEPs.get(userToInstitution.get(userID));
			if(n<institutionEPsSortedList.size())
			{
				return new Object[]{institutionEPsSortedList.get(institutionEPsSortedList.size()-1-n).ID, 1.0f/(1.0f+n)};
			}
		}
		return new Object[]{"", -1.0f};
	}

	@Override
	public float getSrcWeight(String userID, String srcID) 
	{
		String inst=userToInstitution.get(userID);
		if(inst!=null && topInstitutionEPs.get(inst)!=null)
		{
			int ind=topInstitutionEPs.get(inst).indexOf(new IDData<Integer>(srcID, 0));
			if(ind>-1)
			{
				return 1.0f/(1+ind);
			}
			else
			{
				String epInst=epToInstitution.get(srcID);
				ind=institutionEPs.get(inst).indexOf(srcID);
				if(inst.equals(epInst))
				{
					return 1.0f/institutionEPs.get(inst).size();
				}
			}
		}
		return 0.0f;
	}

	@Override
	public Object[] getNthBestDst(String userID, int n, long date) 
	{
		if(userToInstitution.get(userID)!=null
				&& topInstitutionEPs.get(userToInstitution.get(userID))!=null)
		{
			while(!deletionTimes.isEmpty() && (Long)deletionTimes.get(0).data<date)
			{
				String ep=deletionTimes.get(0).ID;
				String inst=epToInstitution.get(ep);
				if(inst!=null)
				{
					int ind=topInstitutionEPs.get(inst).indexOf(ep);
					if(ind>-1)
					{
						deleteFromTopEPs(topInstitutionEPs.get(inst), inst, ind);
					}
				}
				deletionTimes.remove(0);
			}
			List<IDData<Integer>> institutionEPsSortedList=topInstitutionEPs.get(userToInstitution.get(userID));
			if(n<institutionEPsSortedList.size())
			{
				return new Object[]{institutionEPsSortedList.get(institutionEPsSortedList.size()-1-n).ID, 1.0f/(1.0f+n)};
			}
		}
		return new Object[]{"", -1.0f};
	}

	@Override
	public float getDstWeight(String userID, String dstID) 
	{
		String inst=userToInstitution.get(userID);
		if(inst!=null && topInstitutionEPs.get(inst)!=null)
		{
			int ind=topInstitutionEPs.get(inst).indexOf(new IDData<Integer>(dstID, 0));
			if(ind>-1)
			{
				return 1.0f/(1+ind);
			}
			else
			{
				String epInst=epToInstitution.get(dstID);
				ind=institutionEPs.get(inst).indexOf(dstID);
				if(inst.equals(epInst))
				{
					return 1.0f/institutionEPs.get(inst).size();
				}
			}
		}
		return 0.0f;
	}

	@Override
	protected void updateHeuristic(String[] newTransaction) 
	{
		String user=newTransaction[2];
		String src=newTransaction[14];
		String dst=newTransaction[15];
		
		if(numberUniqueInsUsers.get(src)!=null && !numberUniqueInsUsers.get(src).contains(user))
		{
			numberUniqueInsUsers.get(src).add(user);
		}
		if(numberUniqueInsUsers.get(dst)!=null && !numberUniqueInsUsers.get(dst).contains(user))
		{
			numberUniqueInsUsers.get(dst).add(user);
		}
		
		if(epToInstitution.get(src)!=null)
		{
			updateTopEPs(topInstitutionEPs.get(epToInstitution.get(src)), src, 
					numberUniqueInsUsers.get(src).size(), topN);
		}
		
		if(epToInstitution.get(dst)!=null)
		{
			updateTopEPs(topInstitutionEPs.get(epToInstitution.get(dst)), dst, 
					numberUniqueInsUsers.get(dst).size(), topN);
		}
	}
	
	@Override //true: src, false: dst
	public float getAdditionalNetInfo(String userID, boolean srcDst)
	{
		if(srcDst)
		{
			if(userToInstitution.get(userID)!=null
					&& topInstitutionEPs.get(userToInstitution.get(userID))!=null)
			{
				return (float)(1.0/topInstitutionEPs.get(userToInstitution.get(userID)).size());
			}
			else
			{
				return 0.0f;
			}
		}
		else
		{
			if(userToInstitution.get(userID)!=null
					&& topInstitutionEPs.get(userToInstitution.get(userID))!=null)
			{
				return (float)(1.0/topInstitutionEPs.get(userToInstitution.get(userID)).size());
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
		
		InstitutionHeuristic insH=new InstitutionHeuristic(transactions, deletions, 
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
			
			if(insH.userToInstitution.get(userID)!=null && insH.epToInstitution.get(srcID)!=null
					&& insH.userToInstitution.get(userID).equals(insH.epToInstitution.get(srcID)))
			{
				numberSrcInIns++;
			}
			if(insH.userToInstitution.get(userID)!=null && insH.epToInstitution.get(dstID)!=null
					&& insH.userToInstitution.get(userID).equals(insH.epToInstitution.get(dstID)))
			{
				numberDstInIns++;
			}
			
			long currentDate;
			try 
			{
				currentDate = sdf.parse(line[4]).getTime();
				List<String> srcRecs=new ArrayList<>();
				for(int recInd=0; recInd<topN; recInd++)
				{
					srcRecs.add((String)(insH.getNthBestSrc(userID, recInd, currentDate)[0]));
				}
				srcAttempts++;
				if(srcRecs.contains(srcID))
				{
					srcCorrect++;
				}
				
				List<String> dstRecs=new ArrayList<>();
				for(int recInd=0; recInd<topN; recInd++)
				{
					dstRecs.add((String)(insH.getNthBestDst(userID, recInd, currentDate)[0]));
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
			insH.nextTxn();
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
