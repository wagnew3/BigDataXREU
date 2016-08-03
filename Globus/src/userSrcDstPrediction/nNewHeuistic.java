/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package userSrcDstPrediction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author Fischer
 */
public class nNewHeuistic extends EPHeuristic
{   //Holds all the dstID as the key and it's sourceID users as the value in a list
    Hashtable<String, List<String>> dstUse = new Hashtable<>();
     //Holds all the sourceID as the key and it's dstID users as the value in a list
    Hashtable<String, List<String>> sourceUse = new Hashtable<>();
    //All of the association rules 
    Hashtable<String, List<String>> ruleTable = new Hashtable<>();
    
    public nNewHeuistic(List<String[]> transactions, List<String[]> deletions, long date) {
        super(transactions, deletions, date);
      //Take out no dst for renames 4.5k
      //  id	user_id  src_host_ep_id	dst_host_ep_id	request_date	request_time	complete_date	complete_time	sync_level	encrypt_data	verify_checksum	st_files_xfered	st_bytes_xfered	st_faults	st_failed	st_canceled	st_files	st_dirs
       
        //Getting sourceID for each dstID
        for(String[] id : transactions){
             String dstId = id[3];
             String sourceID = id[2];
             if(!dstUse.contains(dstId)){
                dstUse.put(dstId, new ArrayList<String>() );
                //Does this include the first element?  Should it be? 
                //dstUse.get(dstId).add(sourceID);
             }
             dstUse.get(dstId).add(sourceID);
        }
        
         //Getting dstID for the sourceID
        for(String[] id : transactions){
             String sourceId = id[2];
             String dstID = id[3];
             if(!sourceUse.contains(sourceId)){
                sourceUse.put(sourceId, new ArrayList<String>() );
                //Does this include the first element?  Should it be? 
                //sourceUse.get(sourceId).add(dstID);
             }
             sourceUse.get(sourceId).add(dstID);
        }
        
          
         
        
    }

    //need this
    @Override
            //[string src id, double src weight]
    public Object[] getNthBestSrc(String userID, int n, long date) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getSrcWeight(String userID, String srcID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    //[string dst id, double src weight]
    public Object[] getNthBestDst(String userID, int n, long date) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getDstWeight(String userID, String dstID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void loadRules() throws FileNotFoundException{
       File rules = new File("researchIdeas.txt");
       Scanner input = new Scanner(rules);
       
       while(input.hasNextLine()){
          String rule = input.nextLine();
          // Layout for one 103188-103186 0.75             For two   2980,2989-2974 0.8
          //Need to spilt on - as well 
         String[] ruleSlice = rule.split("-");

         //More than one clause 
         if(ruleSlice[0].contains(",")){
             String[] manyClause = ruleSlice[0].split(",");
             if(!ruleTable.contains(manyClause[0])){
                ruleTable.put(manyClause[0], new ArrayList<String>());  
                //To add the first rule in the new List created for that index
                ruleTable.get(manyClause[0]).add(ruleSlice[2]);
             }
             else{ //Place the new rule into that dst EP identifier. Can only handle 2 at this point     
                 ruleTable.get(manyClause[0]).add(manyClause[1] + " " + ruleSlice[2]); // ruleSlice[3] should be the probability if this format is incorrect
             }                                          //Using the space to spilt on later
           
             
             
             
         }else{
             
             if(!ruleTable.contains(ruleSlice[0])){
                ruleTable.put(ruleSlice[0], new ArrayList<String>() );  
             }
             else{
                 ruleTable.get(ruleSlice[0]).add(ruleSlice[1]);
             }
         }
        
        
       }  
    }
    
    public Object[] getNthBestDstUsrSrc(String usrID, String srcID)
    {
        //Copy src to dst and swap things
        //return Object[]{String nthBestdstID, double recStrength }
        
       // sourceUse dstUse
        
    }
    
    public List<String> getNthBestSrcUsrDst(String usrID, String dstID)
    {
            List<String> topNth = new ArrayList<>();  
        if(ruleTable.contains(dstID)){
    
         //All rules for that dstID    
         List<String> ruleLookUp =   ruleTable.get(dstID);
         //Need to think about the threshold desired to stop the search
         for(String individualRules : ruleLookUp){
               List<String> mutual = new ArrayList<>();
               List<String> missing = new ArrayList<>();
             //Determine spilt length as to see if second term is in the clause 
            String[] ruleFormatt =  individualRules.split(" ");
            //Need to check how similar the provided dstID is with the other term in the clause. If very close you could provide the remaining one or two scrEP they dont have in common and then you can check the condition of the two terms 
             if(ruleFormatt.length == 3){//Check range may need to change 
               
                 List<String> givenDstIDActivity = dstUse.get(dstID);
                 List<String> termActivity =  dstUse.get(ruleFormatt[0]);
                 for(String membership: givenDstIDActivity ){//This might not be the right formatt String when the source has numbers you may have to spilt 
                     //Are you comparing the right thing? Should be source activity 
                     if(termActivity.contains(membership)){
                         mutual.add(membership);
                     }missing.add(membership);
                 }//Check this threshold to determine if you want to explore the condition of the rule. 
                 if( mutual.size() / (givenDstIDActivity.size() + termActivity.size() ) >= .66   ){
                     //Could do a ranking of some sort to determine if you want to place it in the suggestions like total size and mutual and missing membership 
                 //Or you could do a vote at the end and see the most frequent suggestions from all the rules, or you could vote the rules as well 
                 topNth.addAll(missing);
                 missing.clear();//Could use missing for the conditional comparison instead of clearing                           //Could also include more info here if needed like strength of prediction 
                 //Going on to the conditional 
                 List<String> conditional  = dstUse.get(ruleFormatt[1]);   
                 
                 for(String membership: givenDstIDActivity ){
                   
                     if(conditional.contains(membership)){
                         mutual.add(membership);
                     }missing.add(membership);
                 }//Check this threshold to determine if you want to explore the condition of the rule. 
                 if( missing.size() / conditional.size()  <= .25   ){
                    
                 topNth.addAll(missing);
                 //ruleFormatt[2] would be the prob of the rule
                 }//Still need to print out the source somewhere once you find range you want. Need to use the lookup of the dst
               
                 
                
             }
             }    //Just check the src activity of the condition. Will not work with more than two terms  
              if(ruleFormatt.length == 2){
                    mutual.clear();
                    missing.clear();
                 List<String> givenDst = dstUse.get(dstID);
                 List<String> conditional =  dstUse.get(ruleFormatt[0]);
                 for(String membership: givenDst ){
                     
                     if(conditional.contains(membership)){
                         mutual.add(membership);
                     }missing.add(membership);
                 }if(missing.size()/ conditional.size()  <= .25  )// Fix / do soemthing with the  threshold 
                        topNth.addAll(missing);
                 
                 missing.clear();
              }
             //compare in dest of the two and then use the src table for output return 
         
        }
        //return Object[]{String nthBestsrcID, double recStrength }
    }
       return topNth;
     // return Object[]{String nthBestsrcID, double recStrength }
    }

    @Override
    //this
    protected void updateHeuristic(String[] newTransaction) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    static int correct;
	static String currentDir=System.getProperty("user.dir");
	private static double[] testHeuristic() throws IOException
	{
            System.out.println("testing new heuristic");
		List<String> failedTransactionsSrc=new ArrayList<>();
		List<String> failedTransactionsDst=new ArrayList<>();
		List<Integer> failedTransactionInds=new ArrayList<>();
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
		
		nNewHeuistic corrH=new nNewHeuistic(splitData, user_eps, 0);
		double srcAttempts=0.0;
		double srcCorrect=0.0;
		
		double dstAttempts=0.0;
		double dstCorrect=0.0;
		
		int topN=1;
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		
		for(int ind=0; ind<splitData.size(); ind++)
		{
                    if(ind%50000==0)
                    {
                        System.out.println(((double)ind/splitData.size())+" fraction done");
                    }
			String[] line=splitData.get(ind);
			String userID=line[1];
			String srcID=line[2];
			String dstID=line[3];
			boolean added=false;
			
			long currentDate;
			try 
			{
				currentDate = sdf.parse(line[4]).getTime();
				List<String> srcRecs=new ArrayList<>();
				for(int recInd=0; recInd<topN; recInd++)
				{
					srcRecs.add((String)(corrH.getNthBestSrc(userID, recInd, currentDate)[0]));
				}
				srcAttempts++;
				if(srcRecs.contains(srcID))
				{
					srcCorrect++;
				}
				else
				{
					failedTransactionsSrc.add(lines.get(ind));
					failedTransactionInds.add(ind);
					added=true;
				}
				
				List<String> dstRecs=new ArrayList<>();
				for(int recInd=0; recInd<topN; recInd++)
				{
					dstRecs.add((String)(corrH.getNthBestDst(userID, recInd, currentDate)[0]));
				}
				dstAttempts++;
				correct=0;
				if(dstRecs.contains(dstID))
				{
					dstCorrect++;
					correct=1;
				}
				else
				{
					failedTransactionsDst.add(lines.get(ind));
					if(!added)
					{
						failedTransactionInds.add(ind);
					}
				}
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			corrH.nextTxn();
		}
		
		Files.write(new File("/home/c/workspace/Globus/data/FailedPredictionsSrc").toPath(), failedTransactionsSrc);
		Files.write(new File("/home/c/workspace/Globus/data/FailedPredictionsDst").toPath(), failedTransactionsDst);
			
		ObjectOutputStream oOut
			=new ObjectOutputStream(new FileOutputStream(new File("/home/c/workspace/Globus/data/FailedPredictionsInds")));
		oOut.writeObject(failedTransactionInds);
		
		
		return new double[]{srcCorrect/srcAttempts, dstCorrect/dstAttempts};
	}
	
	private static double[] testHeuristicGiven() throws IOException
	{
        System.out.println("testing new heuristic");
		List<String> failedTransactionsSrc=new ArrayList<>();
		List<String> failedTransactionsDst=new ArrayList<>();
		List<Integer> failedTransactionInds=new ArrayList<>();
		List<String> lines=Files.readAllLines(new File(currentDir+"/data/transfers-william.csv").toPath());
		List<String[]> splitData=new ArrayList<>();
		int amt=0;
		lines.remove(0).split(",");
		lines.remove(lines.size()-1);
		System.out.print("Loading data...");
		for(String line: lines)
		{
			splitData.add(line.split(","));
			amt++;
			if(amt%100000==0)
			{
				System.out.print((double)amt/3300000);
				if (amt>0)
				{
					//break;
				}
			}
		}
		System.out.println();
		
		List<String[]> user_eps=readDeletionLines();
		
		nNewHeuistic mikeH=new nNewHeuistic(splitData, user_eps, 0);
		double srcAttempts=0.0;
		double srcCorrect=0.0;
		
		double dstAttempts=0.0;
		double dstCorrect=0.0;
		
		int topN=1;
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		
		for(int ind=0; ind<splitData.size(); ind++)
		{
            if(ind%50000==0)
            {
                System.out.println(((double)ind/splitData.size())+" fraction done");
            }
			String[] line=splitData.get(ind);
			String userID=line[1];
			String srcID=line[2];
			String dstID=line[3];
			boolean added=false;
			
			long currentDate;
			try 
			{
				currentDate = sdf.parse(line[4]).getTime();
				List<String> srcRecs=mikeH.getNthBestSrcUsrDst(userID, dstID);
				/*
				for(int recInd=0; recInd<topN; recInd++)
				{
					srcRecs.add((String)(mikeH.getNthBestSrc(userID, recInd, currentDate)[0]));
				}
				*/
				srcAttempts++;
				if(srcRecs.contains(srcID))
				{
					srcCorrect++;
				}
				else
				{
					failedTransactionsSrc.add(lines.get(ind));
					failedTransactionInds.add(ind);
					added=true;
				}
				/*
				List<String> dstRecs=new ArrayList<>();
				for(int recInd=0; recInd<topN; recInd++)
				{
					dstRecs.add((String)(mikeH.getNthBestDst(userID, recInd, currentDate)[0]));
				}
				dstAttempts++;
				correct=0;
				if(dstRecs.contains(dstID))
				{
					dstCorrect++;
					correct=1;
				}
				else
				{
					failedTransactionsDst.add(lines.get(ind));
					if(!added)
					{
						failedTransactionInds.add(ind);
					}
				}
				*/
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			mikeH.nextTxn();
		}
		
		//Files.write(new File("/home/c/workspace/Globus/data/FailedPredictionsSrc").toPath(), failedTransactionsSrc);
		//Files.write(new File("/home/c/workspace/Globus/data/FailedPredictionsDst").toPath(), failedTransactionsDst);
		
		/*
		ObjectOutputStream oOut
			=new ObjectOutputStream(new FileOutputStream(new File("/home/c/workspace/Globus/data/FailedPredictionsInds")));
		oOut.writeObject(failedTransactionInds);
		*/
		return new double[]{srcCorrect/srcAttempts, 0.0/*dstCorrect/dstAttempts*/};
	}
	
	static List<String[]> readDeletionLines()
	{
		List<String> lines=null;
		try
		{
			lines=Files.readAllLines(new File(currentDir+"/data/short-eps.csv").toPath());
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
	
	private static double[] testHeuristicOnEnsFailuresGiven() throws IOException
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
			if(amt%1000000==0)
			{
				System.out.println(amt);
				if (amt>0)
				{
					//break;
				}
			}
		}
		
		List<String[]> user_eps=readDeletionLines();
		
		List<Integer> failureIntegers=null;
		ObjectInputStream oIn=new ObjectInputStream(new FileInputStream(new File(currentDir+"/data/FailedPredictionsInds")));
		try 
		{
			failureIntegers=(List<Integer>)oIn.readObject();
		} 
		catch (ClassNotFoundException e1) 
		{
			e1.printStackTrace();
		}
		
		nNewHeuistic mikeH=new nNewHeuistic(splitData, user_eps, 0);
		double srcAttempts=0.0;
		double srcCorrect=0.0;
		
		double dstAttempts=0.0;
		double dstCorrect=0.0;
		
		int topN=10000;
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		
		int failInd=0;
		for(int splitInd=0; splitInd<splitData.size(); splitInd++)
		{
			String[] line=splitData.get(splitInd);
			String userID=line[1];
			String srcID=line[2];
			String dstID=line[3];
			
			if(splitInd==failureIntegers.get(failInd))
			{
				if(splitInd%50000==0)
	            {
	                System.out.println(((double)splitInd/splitData.size())+" fraction done");
	            }
				failInd++;
				long currentDate;
				try 
				{
					currentDate = sdf.parse(line[4]).getTime();
					List<String> srcRecs=mikeH.getNthBestSrcUsrDst(userID, dstID);
					/*
					for(int recInd=0; recInd<topN; recInd++)
					{
						srcRecs.add((String)(jaccH.getNthBestSrc(userID, recInd, currentDate)[0]));
					}
					*/
					srcAttempts++;
					if(srcRecs.contains(srcID))
					{
						srcCorrect++;
					}
					else
					{
						int u=0;
					}
					/*
					List<String> dstRecs=new ArrayList<>();
					for(int recInd=0; recInd<topN; recInd++)
					{
						dstRecs.add((String)(jaccH.getNthBestDst(userID, recInd, currentDate)[0]));
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
			}
			mikeH.nextTxn();
		}
		
		return new double[]{srcCorrect/srcAttempts, dstCorrect/dstAttempts};
	}
	
	public static void main(String[] args) throws IOException
	{
		/*
		double[] accuracies=testHeuristic();
		System.out.println("Correlation Heuristics Accuracy src: "+accuracies[0]+" dst: "+accuracies[1]);
		*/
		/*
		double[] accuracies=testHeuristicGiven();
		System.out.println("Correlation Heuristics Accuracy src: "+accuracies[0]+" dst: "+accuracies[1]);
		*/
		double[] accuracies=testHeuristicOnEnsFailuresGiven();
		System.out.println("Correlation Heuristics Accuracy src: "+accuracies[0]+" dst: "+accuracies[1]);
	}
    
}
