package eu.su.mas.dedaleEtu.mas.agents.Agents2;

import bdi4jade.core.SingleCapabilityAgent;
import bdi4jade.plan.planbody.AbstractPlanBody;
import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.Agents.CustomMapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.agents.Agents.CustomMapRepresentation;
import bdi4jade.core.*;
import bdi4jade.plan.*;
import bdi4jade.goal.*;
import bdi4jade.plan.planbody.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import bdi4jade.belief.*;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.util.Logger;

public class BDIAgent2 extends SingleCapabilityAgent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6965314195585071240L;
	private Logger myLogger = Logger.getMyLogger(getClass().getName());
	static final long seconds = 1000000000;
	
	public class DFRegisterGoal implements Goal {
	    
        /**
		 * 
		 */
		private static final long serialVersionUID = 4053121889746128230L;

		public DFRegisterGoal() {}
    }

    public class DFRegisterPlan extends AbstractPlanBody {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2464345031371830771L;

		@Override
        public void action() {
	
	        System.out.println("Plan is executing.");
	        DFAgentDescription dfd = new DFAgentDescription();
	        ServiceDescription sd = new ServiceDescription();   
	        sd.setType("BDI"); 
	        sd.setName(getName());
	        dfd.setName(getAID());
	        dfd.addServices(sd);
	        try {
	            DFService.register(this.myAgent,dfd);         
	            System.out.println("Agent "+ getLocalName() +" registered correctly.");            
	            setEndState(Plan.EndState.SUCCESSFUL);
	        } catch (FIPAException e) {
	            myLogger.log(Logger.SEVERE, "Agent "+ getLocalName() +" - Cannot register with DF", e);
	            setEndState(Plan.EndState.FAILED);
	            doDelete();
	        }    
        }
    }
    
    
    public class WaitSituatedGoal implements Goal{
    	
    	/**
		 * 
		 */
		private static final long serialVersionUID = -6056249213170671249L;
		private final int nack;
        private int acks;

        public WaitSituatedGoal(){
        	this.nack = 2;
            this.acks = 0;
        }
        public Integer getTotalACKs(){
            return this.nack;
        }
        
        public Integer getNumOfACKs(){
            return this.acks;
        }
        
        public void incNumOfACKs(){
            this.acks += 1;
        }

    }
    
    public class WaitSituatedPlan extends AbstractPlanBody{
    	/**
		 * 
		 */
		private static final long serialVersionUID = -5890651233376590317L;

		@Override
    	public void action() {
    		WaitSituatedGoal goal = (WaitSituatedGoal)getGoal();
    		
    		Capability cap = this.getCapability();
    		BeliefBase beliefBase = cap.getBeliefBase();
    		
    		//AID aid = (AID)beliefBase.getBelief("AIDSituated").getValue();
    		//String rol = (String)beliefBase.getBelief("rolSituated").getValue();
    	
            if(goal.getNumOfACKs() < goal.getTotalACKs()){
                ACLMessage  msg = myAgent.receive();
                
                if(msg != null){              
                	ACLMessage reply = msg.createReply();
                    if(msg.getPerformative()== ACLMessage.INFORM){
                        //String content = msg.getContent();
                    	
                    	System.out.println("ACK 1 confirmed. Sending confirmation...");
                        reply.setPerformative(ACLMessage.CONFIRM);
                        //reply.setContent(s);
                        send(reply);
                        goal.incNumOfACKs();
               
                    }
                    else if(msg.getPerformative()== ACLMessage.CONFIRM) {
                    	
                    	
                    	ArrayList<String> info = null;
						try {
							info = (ArrayList<String> )msg.getContentObject();
						} catch (UnreadableException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                    	
                    	int lockPickingSkill = Integer.parseInt(info.get(0));
                    	String TreasureType = info.get(1);
                    	
                    	String rol;
                    	if(lockPickingSkill > 0) rol = "collectAgent";
                    	else rol = "tankerAgent";
                    	
                    	System.out.println("AGENT TYPE: " + rol + "   lockPickingSkill: " + lockPickingSkill + "   TreasureType: " + TreasureType);
                    	
                    	
                    	AID aid = msg.getSender();
                    	beliefBase.updateBelief("AIDSituated", aid);
                    	beliefBase.updateBelief("rolSituated", rol);
                    	beliefBase.updateBelief("LockPickingSkill", lockPickingSkill);
                    	beliefBase.updateBelief("TreasureType", TreasureType);
                    	
                    	
                    	
                    	System.out.println("Synchro end.");
                    	goal.incNumOfACKs();
                    }
                    else {
                        myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Unexpected message ["+ACLMessage.getPerformative(msg.getPerformative())+"] received from "+msg.getSender().getLocalName());
                    }
                }
                else {
                    block();
                }
            }
            else {
            	System.out.println("End of synchronization ...");
            	
                Plan MessagesPlan = new DefaultPlan(ReceiveMessagesGoal.class, ReceiveMessagesPlan.class);
                addGoal(new ReceiveMessagesGoal());
                getCapability().getPlanLibrary().addPlan(MessagesPlan);
                
            	setEndState(Plan.EndState.SUCCESSFUL);
            }
    	}
    }

    public class ReceiveMessagesGoal implements Goal{
    	/**
		 * 
		 */
		private static final long serialVersionUID = 8574893829207503443L;
		

		public ReceiveMessagesGoal() {}
    }
    
    public class ReceiveMessagesPlan extends AbstractPlanBody{
    	/**
		 * 
		 */
		private static final long serialVersionUID = 6413484035673244966L;

		@Override
    	public void action() {

    		ACLMessage msg = myAgent.receive();
    		if(msg != null){              
    			
    			
                
            }
            else {
                block();
            }
    	}
    }

    public class TankerGoal implements Goal{
    	/**
		 * 
		 */
		private static final long serialVersionUID = 5167598322094008593L;
		private int steps;
		private int stepi;
		public TankerGoal(){
			this.steps = 10;
			this.stepi = 0;
		}
		
		public Integer getTotalSteps(){
	       	return this.steps;
		}
	        
	    public Integer getNumOfSteps(){
	    	return this.stepi;
	    }
	    
	    public void resetNumOfSteps(){
	    	this.stepi = 0;
	    }
	        
	    public void incNumOfSteps(){
	       this.stepi += 1;
	    } 
    }
    
    public class TankerPlan extends AbstractPlanBody{
    	/**
		 * 
		 */
		private static final long serialVersionUID = 3390328270971545672L;

		@Override
    	public void action() {
			TankerGoal goal = (TankerGoal)getGoal();
			Capability cap = this.getCapability();
			BeliefBase beliefBase = cap.getBeliefBase();
			
			int capacity = (int)beliefBase.getBelief("capTanker").getValue();
			String state = (String)beliefBase.getBelief("stateTanker").getValue();
			
			CustomMapRepresentation myMap = (CustomMapRepresentation)beliefBase.getBelief("map").getValue();
			HashMap<AID, Couple<Long, String>> agPos = (HashMap<AID, Couple<Long, String>>)beliefBase.getBelief("agPositions").getValue();
			AID aid = (AID)beliefBase.getBelief("AIDSituated").getValue();
			String currentPos = agPos.get(aid).getRight();
			
			switch(state) {
			case "ROAMING":
				//After movement
				Set<String> openNodes = (HashSet<String>)beliefBase.getBelief("openNodes").getValue();
				
				
				
				if(goal.getNumOfSteps() < goal.getTotalSteps()) {
					if(!openNodes.isEmpty()) {
						String nextNode = openNodes.iterator().next();
						List<String> path = myMap.getShortestPath(currentPos, nextNode);
						if(!path.isEmpty()) {
							//instructMove(path.get(0));
							goal.incNumOfSteps();
						}
						else System.out.println("Path is EMPTY");
					}
					else System.out.println("Open Nodes is EMPTY");
				}
				else {
					beliefBase.updateBelief("stateTanker", "TANKING");
					goal.resetNumOfSteps();
				}
				break;
			case "TANKING":
				
			
				String treasure = (String)beliefBase.getBelief("TreasureType").getValue();
				
				
				DFAgentDescription template = new DFAgentDescription();
		        ServiceDescription templateSd1 = new ServiceDescription();
	            templateSd1.setType("agentCollector");
	            template.addServices(templateSd1);
	            
	            switch(treasure) {
	            case "ANY":
	            	ServiceDescription templateSd2 = new ServiceDescription();
			        ServiceDescription templateSd3 = new ServiceDescription();
			        templateSd2.setType("agentGold");
		            templateSd3.setType("agentDiamond");
		            template.addServices(templateSd2);
		            template.addServices(templateSd3);
	            	break;
	            case "GOLD":
	            	ServiceDescription templateSd21 = new ServiceDescription();
	            	templateSd21.setType("agentGold");
	            	template.addServices(templateSd21);
	            	break;
	            case "DIAMOND":
	            	ServiceDescription templateSd211 = new ServiceDescription();
	            	templateSd211.setType("agentDiamond");
	            	template.addServices(templateSd211);
	            	break;
	            
	            }
	            //Belief AIDSituated = new TransientBelief("AIDSituated", new AID());
	            AID aid1 = (AID)beliefBase.getBelief("AIDSituated").getValue();
	            
	            String currentPos1 = agPos.get(aid1).getRight();
	            Long start = System.nanoTime();
				Long limit = start + 60*seconds; 
	            //SearchConstraints sc = new SearchConstraints();
	            try{     
	                DFAgentDescription[] results = DFService.search(myAgent, template);	               
	                if(results.length > 0){
	                    for(int i = 0; i < results.length; ++i){
	                        DFAgentDescription dfd = results[i];
	                        AID provider = dfd.getName();
	                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
	                        msg.setOntology("TankingPositionInform");
	                        Couple<String, Long> inf = new Couple<String, Long>(currentPos1, limit);
	                        msg.setContentObject(inf);
	                        msg.addReceiver(provider);
	                        ((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
	                    }
	                 
	                }
	                System.out.println("Agent searched DL correctly.");	                
	            } catch(FIPAException e){
	                setEndState(Plan.EndState.FAILED);
	            } catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            Long timei = System.nanoTime();
	            if (timei > limit) {
	            	List<Couple<Observation, Integer>> backpack = (List<Couple<Observation, Integer>>)beliefBase.getBelief("backPackFreeSpace").getValue();
	            	boolean full = false;
	            	int count = 0;
	            	for(Couple<Observation, Integer> bp : backpack) {
	            		count += bp.getRight();
	            	}
	            	full = count == 0;
	            	if(full) {
	            		beliefBase.updateBelief("stateTanker", "FULL");
	            	}
	            	else beliefBase.updateBelief("stateTanker", "ROAMING");
	            }
				
				break;
			case "FULL":
				
				Set<String> closedNds = (HashSet<String>)beliefBase.getBelief("closedNodes").getValue();
				
				if(!closedNds.isEmpty()) {
					int min = Integer.MAX_VALUE;
					String nextNode = "";
					for(String node : closedNds) {
						Set<String> edges = myMap.getEdges(node);
						int m = edges.size();
						if (m < min) {
							min = m;
							nextNode = node;
						}
					}
					if (nextNode != "") {
						List<String> path = myMap.getShortestPath(currentPos, nextNode);
						if(!path.isEmpty()) {
							//instructMove(path.get(0));
							goal.incNumOfSteps();
						}
						else System.out.println("Path is EMPTY");
					}else System.out.println("nextNode (String) is empty");
					
				}
				else System.out.println("Closed Nodes is EMPTY");
				
				break;		
			}
    	}
    }
    
    
    
    protected void init() {
		System.out.println("BDI AGENT");
		Capability cap = this.getCapability();
		BeliefBase beliefBase = cap.getBeliefBase();
		
	
		String rol = "";
		int lockPickingSkill = -1;
		String TreasureType = "";		
		HashMap<AID, Couple<Long, String>> agentPositions = new HashMap<AID, Couple<Long, String>>();
		HashMap<String, Couple <Long, HashMap<Observation, Integer>>> resourceInformation = new HashMap<String, Couple<Long, HashMap<Observation, Integer>>>();
		Set<String> openNodes = new HashSet<String>();		
		Set<String> closedNodes = new HashSet<String>();
		List<Couple<Observation, Integer>> bp = new ArrayList<Couple<Observation, Integer>>();
		
		
		Belief openN = new TransientBeliefSet("openNodes", openNodes);
		Belief closedN = new TransientBeliefSet("closedNodes", closedNodes);
		Belief AIDSituated = new TransientBelief("AIDSituated", new AID());
		Belief rolSituated = new TransientBelief("rolSituated", rol);
		Belief agPositions = new TransientBelief("agPositions", agentPositions);
		Belief resourceInfo = new TransientBelief("resourceInfo", resourceInformation);
		Belief LockPicking = new TransientBelief("lockPickingSkill", lockPickingSkill);
		Belief TreasureT = new TransientBelief("TreasureType", TreasureType);
		Belief BackPack = new TransientBelief("backPackFreeSpace", bp);		
		Belief stateTanker = new TransientBelief("stateTanker", "");
		Belief capTanker = new TransientBelief("capTanker", -1);
		Belief stateCollect= new TransientBelief("stateCollect","");
		
		beliefBase.addBelief(AIDSituated);
		beliefBase.addBelief(rolSituated);
		beliefBase.addBelief(agPositions);
		beliefBase.addBelief(resourceInfo);
		beliefBase.addBelief(TreasureT);
		beliefBase.addBelief(LockPicking);
		beliefBase.addBelief(stateTanker);
		beliefBase.addBelief(stateCollect);
		beliefBase.addBelief(BackPack);
		beliefBase.addBelief(openN);
		beliefBase.addBelief(closedN);
		
		Plan registerPlan = new DefaultPlan(DFRegisterGoal.class, DFRegisterPlan.class);
        addGoal(new DFRegisterGoal());
        getCapability().getPlanLibrary().addPlan(registerPlan);
        
        Plan synchroPlan = new DefaultPlan(WaitSituatedGoal.class, WaitSituatedPlan.class);
        addGoal(new WaitSituatedGoal());
        getCapability().getPlanLibrary().addPlan(synchroPlan);
    }

}
