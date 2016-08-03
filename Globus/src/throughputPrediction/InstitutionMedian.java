package throughputPrediction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import init.IDData;

public class InstitutionMedian extends TPHeuristic
{

	Hashtable<String, Hashtable<String, List<IDData<Double>>>> txnsByAmt;
	Hashtable<String, String> epToInstitution;
	
	int minNumInMedian=10;
	int maxNumInMedian;
	
	public InstitutionMedian(List<String[]> transactions, List<String[]> deletions, List<String[]> users, List<String[]> authIDs,
			long date) 
	{
		super(transactions, deletions, users, authIDs, date);
		txnsByAmt=new Hashtable<>();
		epToInstitution=new Hashtable<>();
		
		Hashtable<String, String> userToInstitution=new Hashtable<>();
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
			
			String institution=suffix;
			longUserToInstitution.put(longID, institution);
		}
		for(String user: userIDToLongUserID.keySet())
		{
			if(longUserToInstitution.get(userIDToLongUserID.get(user))!=null)
			{
				userToInstitution.put(user, longUserToInstitution.get(userIDToLongUserID.get(user)));
			}
		}
		for(String[] dLine: deletions)
		{
			String user=dLine[2];
			String epID=dLine[0];
			String insitution=userToInstitution.get(user);
			if(insitution!=null)
			{
				epToInstitution.put(epID, insitution);
			}
		}
	}

	@Override
	public float getThrougputEstimation(String srcEP, String dstEP, long date, long amount) 
	{
		String srcInst=epToInstitution.get(srcEP);
		String dstInst=epToInstitution.get(dstEP);
		if(srcInst!=null && dstInst!=null && txnsByAmt.get(srcInst)!=null 
				&& txnsByAmt.get(srcInst).get(dstInst)!=null 
				&& !txnsByAmt.get(srcInst).get(dstInst).isEmpty())
		{
			return (float)(double)txnsByAmt.get(srcInst).get(dstInst).get(txnsByAmt.get(srcInst).get(dstInst).size()/2).data;
		}
		return 0;
	}

	@Override
	public float getThrougputEstimationWeight(String srcEP, String dstEP, long amount)
	{
		String srcInst=epToInstitution.get(srcEP);
		String dstInst=epToInstitution.get(dstEP);
		if(srcInst!=null && dstInst!=null && txnsByAmt.get(srcInst)!=null 
				&& txnsByAmt.get(srcInst).get(dstInst)!=null && !txnsByAmt.get(srcInst).get(dstInst).isEmpty())
		{
			return (float)(1.0/Math.abs(txnsByAmt.get(srcInst).get(dstInst).get(txnsByAmt.get(srcInst).get(dstInst).size()/2).doubleData-amount));
		}
		return -1.0f;
	}

	@Override
	protected void updateHeuristic(String[] newTransaction) 
	{
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		try 
		{
			long startTime=sdf.parse(newTransaction[3]).getTime();
			long endTime=sdf.parse(newTransaction[13]).getTime();
			long amount=Long.parseLong(newTransaction[37]);
			String srcID=newTransaction[14];
			String dstID=newTransaction[15];
			
			String srcInst=epToInstitution.get(srcID);
			String dstInst=epToInstitution.get(dstID);	
			if(srcInst!=null && dstInst!=null)
			{
				if(txnsByAmt.get(srcInst)==null)
				{
					txnsByAmt.put(srcInst, new Hashtable<>());
				}
				if(txnsByAmt.get(srcInst).get(dstInst)==null)
				{
					txnsByAmt.get(srcInst).put(dstInst, new ArrayList<>());
				}

				if(epPairDateThroughputs.get(srcID).get(dstID).size()>maxNumInMedian)
				{
					maxNumInMedian=epPairDateThroughputs.get(srcID).get(dstID).size();
				}
				
				IDData<Double> txnInfo=new IDData<Double>("", 1000.0*amount/(endTime-startTime));
				txnInfo.doubleData=amount;
				int ind=Collections.binarySearch(txnsByAmt.get(srcInst).get(dstInst), txnInfo);
				if(ind<0)
				{
					ind=-(ind+1);
				}
				else
				{
					ind--;
				}
				if(ind==txnsByAmt.get(srcInst).get(dstInst).size())
				{
					txnsByAmt.get(srcInst).get(dstInst).add(txnInfo);
				}
				else
				{
					txnsByAmt.get(srcInst).get(dstInst).add(ind, txnInfo);
				}
			}
		}
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
	}
	
}