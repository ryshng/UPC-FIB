package eu.su.mas.dedaleEtu.mas.agents.Agents;

import bdi4jade.plan.planbody.AbstractPlanBody;
import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
//import eu.su.mas.dedaleEtu.mas.knowledge.CustomMapRepresentation;
import eu.su.mas.dedaleEtu.mas.agents.Agents.CustomMapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.agents.Agents.CustomMapRepresentation;
import eu.su.mas.dedaleEtu.mas.agents.Agents.BDIAgent2.TankerGoal;
import bdi4jade.core.*;
import bdi4jade.plan.*;
import bdi4jade.goal.*;
import bdi4jade.plan.planbody.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.core.appender.rolling.action.IfAccumulatedFileCount;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.ui.view.Viewer;

import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import bdi4jade.belief.*;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.util.Logger;

public class BDIAgent extends SingleCapabilityAgent {
	
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
    
    
    private void dedaleResendMessage(ACLMessage msg) {
    	//envia un missatge al agent situat amb msg com a contentObject, el agent situat llavors reenvia msg utilitzant sendMessage(msg)
    	ACLMessage bdiMessage = new ACLMessage(ACLMessage.REQUEST);

		Capability cap = this.getCapability();
		BeliefBase beliefBase = cap.getBeliefBase();
		AID agSituated = (AID)beliefBase.getBelief("AIDSituated").getValue();
		bdiMessage.addReceiver(agSituated);
		bdiMessage.setOntology("resend");
		try {
			bdiMessage.setContentObject(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		send(bdiMessage);
    }
    
    private void instructMove(String pos) {
    	ACLMessage bdiMessage = new ACLMessage(ACLMessage.REQUEST);

		Capability cap = this.getCapability();
		BeliefBase beliefBase = cap.getBeliefBase();
		AID agSituated = (AID)beliefBase.getBelief("AIDSituated").getValue();
		bdiMessage.addReceiver(agSituated);
		bdiMessage.setOntology("move");
		bdiMessage.setContent(pos);
		
		send(bdiMessage);
    }
    
    
    private void instructUnlock(Observation o) {


    	ACLMessage bdiMessage = new ACLMessage(ACLMessage.REQUEST);

		Capability cap = this.getCapability();
		BeliefBase beliefBase = cap.getBeliefBase();
		AID agSituated = (AID)beliefBase.getBelief("AIDSituated").getValue();
		bdiMessage.addReceiver(agSituated);
		bdiMessage.setOntology("unlock");
		try {
			bdiMessage.setContentObject(o);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		send(bdiMessage);
    }
    
    private void instructDrop(String tankerName) {
    	ACLMessage bdiMessage = new ACLMessage(ACLMessage.REQUEST);

		Capability cap = this.getCapability();
		BeliefBase beliefBase = cap.getBeliefBase();
		AID agSituated = (AID)beliefBase.getBelief("AIDSituated").getValue();
		bdiMessage.addReceiver(agSituated);
		bdiMessage.setOntology("drop");
		bdiMessage.setContent(tankerName);
		send(bdiMessage);
    }
    
    private void instructPick(Observation o) {
    	ACLMessage bdiMessage = new ACLMessage(ACLMessage.REQUEST);
    	
		Capability cap = this.getCapability();
		BeliefBase beliefBase = cap.getBeliefBase();
		AID agSituated = (AID)beliefBase.getBelief("AIDSituated").getValue();
		bdiMessage.addReceiver(agSituated);
		bdiMessage.setSender(getAID());
		bdiMessage.setOntology("collect");
		try {
			bdiMessage.setContentObject(o);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		send(bdiMessage);
    }
    
    private void requestBackpackSpace() {
    	ACLMessage bdiMessage = new ACLMessage(ACLMessage.REQUEST);

		Capability cap = this.getCapability();
		BeliefBase beliefBase = cap.getBeliefBase();
		AID agSituated = (AID)beliefBase.getBelief("AIDSituated").getValue();
		bdiMessage.addReceiver(agSituated);
		bdiMessage.setOntology("requestBackpack");

		send(bdiMessage);
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
                    	if(lockPickingSkill > 0) {
                    		rol = "collectAgent";
                    	}
                    	else {
                    		rol = "tankerAgent";
                    	}


                    	System.out.println("AGENT TYPE: " + rol + "   lockPickingSkill: " + lockPickingSkill + "   TreasureType: " + TreasureType);

                    	
                    	AID aid = msg.getSender();
                    	beliefBase.updateBelief("AIDSituated", aid);
                    	beliefBase.updateBelief("rolSituated", rol);
                    	beliefBase.updateBelief("lockPickingSkill", lockPickingSkill);
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
                
                sendInfoBehaviour Sendinf = new sendInfoBehaviour(myAgent,10);
                addBehaviour(Sendinf);
                
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
    			
    			
    			Capability cap = this.getCapability();
        		BeliefBase beliefBase = cap.getBeliefBase();
        		CustomMapRepresentation myMap;
        		
        		HashMap<AID, Couple<Long, String>> agPos = (HashMap<AID, Couple<Long, String>>)beliefBase.getBelief("agPositions").getValue();
        		HashMap<String, Couple <Long, HashMap<Observation, Integer>>> resources = (HashMap<String, Couple <Long, HashMap<Observation, Integer>>>)beliefBase.getBelief("resourceInfo").getValue();
        		


    			 
    			switch (msg.getOntology()){   			
	    			case "":
	    				break;
	    				
	    				
	    			case "observe":
	    				long time = -1;
	    				List<Couple<String,List<Couple<Observation,Integer>>>> lobs = null;
						try {
							
							time = msg.getPostTimeStamp();
							lobs = (List<Couple<String,List<Couple<Observation,Integer>>>>)msg.getContentObject();
						} catch (UnreadableException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					

						if(!beliefBase.hasBelief("map")) {
							myMap = new CustomMapRepresentation();
							Belief map = new TransientBelief("map", myMap);
							beliefBase.addBelief(map);
						}
						myMap = (CustomMapRepresentation)beliefBase.getBelief("map").getValue();
						
						
						Set<String> openNodes = (HashSet<String>)beliefBase.getBelief("openNodes").getValue();
						Set<String> closedNodes = (HashSet<String>)beliefBase.getBelief("closedNodes").getValue();

						
						
						
						Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();

						//check node actual
						
						
						Couple<String,List<Couple<Observation, Integer>>> currentNode = iter.next();
						List<Couple<Observation, Integer>> currentNodeInfo = currentNode.getRight();
						String myPosition = currentNode.getLeft();
						boolean hasWind = false;
						
						HashMap<Observation, Integer> currentNodeHashmap = new HashMap<Observation,Integer>();
						resources.put(myPosition, new Couple<>(time,currentNodeHashmap));
						
						for(int i = 0; i < currentNodeInfo.size(); i++) {
							if(currentNodeInfo.get(i).getLeft() == Observation.WIND) hasWind = true;
							currentNodeHashmap.put(currentNodeInfo.get(i).getLeft(),currentNodeInfo.get(i).getRight());
//							HashMap<String, Couple <Long, HashMap<Observation, Integer>>>
							
						}
//						System.out.println("current Node updated: " + resources.get(myPosition));
						
						
						myMap.addNode(myPosition, MapAttribute.closed);
						closedNodes.add(myPosition);
						openNodes.remove(myPosition);
						
						
						
						//check de nodes adjaçents
						while(!hasWind && iter.hasNext()) {
							String nodeId=iter.next().getLeft();
							if(!myPosition.equals(nodeId)) {
								
								if((!closedNodes.contains(nodeId)) && (!openNodes.contains(nodeId))) {

									myMap.addNode(nodeId,MapAttribute.open);
									openNodes.add(nodeId);
								}
								myMap.addEdge(myPosition,  nodeId);
							}
						}
						//update pos del agent
						
						AID myAID = (AID)beliefBase.getBelief("AIDSituated").getValue();
						
						agPos.put(myAID, new Couple<> (time,myPosition));
						
						

					
					
						

						beliefBase.updateBelief("myPos", myPosition);
						beliefBase.updateBelief("openNodes",openNodes);
						beliefBase.updateBelief("closedNodes",closedNodes);
						beliefBase.updateBelief("map",myMap);
						beliefBase.updateBelief("agPositions",agPos);
						beliefBase.updateBelief("resourceInfo",resources);
						
						
						
                		
						
						String sc =  (String)beliefBase.getBelief("stateCollect").getValue();
						String st =  (String)beliefBase.getBelief("stateCollect").getValue();
						
                		if(sc.equals("") && st.equals("")) {
    						
    						String rol = (String)beliefBase.getBelief("rolSituated").getValue();
                    		if(rol.equals("collectAgent")) {
                    			
                                Plan collectPlan = new DefaultPlan(CollectorGoal.class, CollectorPlan.class);
                                addGoal(new CollectorGoal());
                                getCapability().getPlanLibrary().addPlan(collectPlan);
                                
                                
                                beliefBase.updateBelief("stateCollect", "SEARCH-PICK");
                    		}
                    		else {
                    		
                        		Plan tankPlan = new DefaultPlan(TankerGoal.class, TankerPlan.class);
                                addGoal(new TankerGoal());
                                getCapability().getPlanLibrary().addPlan(tankPlan);
                        		

                                beliefBase.updateBelief("stateTanker", "ROAMING");
    						
                    		}
                			
                		}
						
						
						
						
						
						
						
						
						
	    				break;
	    			
	    			case "mapTopology":
	    				try {
							if(!beliefBase.hasBelief("map")) {
								myMap = new CustomMapRepresentation();
								Belief map = new TransientBelief("map", myMap);
								beliefBase.addBelief(map);
							}
							myMap = (CustomMapRepresentation)beliefBase.getBelief("map").getValue();
							SerializableSimpleGraph<String, MapAttribute> newMap = (SerializableSimpleGraph<String, MapAttribute>)msg.getContentObject();
							myMap.mergeMap(newMap);
							beliefBase.updateBelief("map", myMap);
							
						} catch (UnreadableException e) {
							e.printStackTrace();
						}
	    				break;
	    			case "resourceInformation":
	    				HashMap<String, Couple<Long, HashMap<Observation, Integer>>> newResources;
						try {
							
							System.out.println("Received resourceInformation");
							newResources = (HashMap<String, Couple <Long, HashMap<Observation, Integer>>>) msg.getContentObject();
							for (Entry<String, Couple<Long, HashMap<Observation, Integer>>> entry : newResources.entrySet()) {
								String key = entry.getKey();
								Couple<Long, HashMap<Observation, Integer>> value = entry.getValue();
								if ((!resources.containsKey(key)) || value.getLeft() > resources.get(key).getLeft()) {
									resources.put(key,value); 
								}
							}
							beliefBase.updateBelief("resourceInfo", resources);
						} catch (UnreadableException e) {
							e.printStackTrace();
						}
	    				break;
	    			case "agentPositions":
	    				HashMap<AID, Couple<Long, String>> newAgentPositions;
						System.out.println("Received agentPositions");
						try {
							newAgentPositions = (HashMap<AID, Couple<Long, String>>) msg.getContentObject();
							for (Entry<AID, Couple<Long, String>> entry : newAgentPositions.entrySet()) {
								AID key = entry.getKey();
								Couple<Long,String> value = entry.getValue();
								if ((!agPos.containsKey(key)) || value.getLeft() > agPos.get(key).getLeft()) {
									agPos.put(key,value); 
									System.out.println("agent " + key.getName() + "spotted in " + value + " at time" + agPos.get(key).getLeft());
								}
							}
							beliefBase.updateBelief("agPositions", agPos);
						} catch (UnreadableException e) {
							e.printStackTrace();
						}
	    				break;		
	    			case "informBackpack":
					try {
						List<Couple<Observation, Integer>> bp = (List<Couple<Observation, Integer>>) msg.getContentObject();
						beliefBase.updateBelief("backPackFreeSpace", bp);
						
					} catch (UnreadableException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    			break;
	    			default:
	    				 myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Unexpected message ["+ACLMessage.getPerformative(msg.getPerformative())+"] received from "+msg.getSender().getLocalName());
	    				break;
	    				
	    			case "TankingPositionInform":
					try {
						Couple<String,Long> content = (Couple<String,Long>)msg.getContentObject();
						
						
						Vector<Couple<AID,Long>> tankerList =  (Vector<Couple<AID,Long>>)beliefBase.getBelief("tanker").getValue();
						AID tankerAID = msg.getSender();
						tankerList.add(new Couple<>(tankerAID,content.getRight()));

						agPos.put(tankerAID,new Couple<>(System.nanoTime(),content.getLeft()));

						beliefBase.updateBelief("agPositions",agPos);
						
					} catch (UnreadableException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    				break;
    			}
                
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
							instructMove(path.get(0));
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
							instructMove(path.get(0));
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
    
    public class CollectorGoal implements Goal{
    	/**
		 * 
		 */
		private static final long serialVersionUID = -6467883382894689498L;

		public CollectorGoal(){}
    }
    
    public class CollectorPlan extends AbstractPlanBody{
    	/**
		 * 
		 */
		private static final long serialVersionUID = -4794541365908673342L;

		@Override
    	public void action() {
			
			Capability cap = this.getCapability();
			BeliefBase beliefBase = cap.getBeliefBase();
			
			String state = (String)beliefBase.getBelief("stateCollect").getValue();
			
			switch(state) {
			case "SEARCH-PICK":
				searchPick();
				break;
			case "SEARCH-TANK":
				searchTank();
				break;					
			}
    	}
    }
    
    public class sendInfoBehaviour extends TickerBehaviour {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6171727635468914682L;

		public sendInfoBehaviour(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onTick() {
			
			
			try {
			
			BDIAgent ag = (BDIAgent) myAgent;
			
			Capability cap = ag.getCapability();
    		BeliefBase beliefBase = cap.getBeliefBase();
    		
			CustomMapRepresentation myMap = (CustomMapRepresentation)beliefBase.getBelief("map").getValue();
			AID SituatedAID = (AID)beliefBase.getBelief("AIDSituated").getValue();
			
			
			HashMap<String, Couple <Long, HashMap<Observation, Integer>>> resourceInformation = (HashMap<String, Couple <Long, HashMap<Observation, Integer>>>)beliefBase.getBelief("resourceInfo").getValue();

    		HashMap<AID, Couple<Long, String>> agentPositions = (HashMap<AID, Couple<Long, String>>)beliefBase.getBelief("agPositions").getValue();
    		
    		
//			Belief agPositions = new TransientBelief("agPositions", agentPositions);
			
			
			ACLMessage msgMapTopology = new ACLMessage(ACLMessage.INFORM);
			msgMapTopology.setOntology("mapTopology");
			msgMapTopology.setContentObject(myMap.getSerializableGraph());
			msgMapTopology.setSender(SituatedAID);
	//		msgMapTopology.setContent(myMap.getSerializableGraph()); 
			
			ACLMessage msgResourceInformation = new ACLMessage(ACLMessage.INFORM);
			msgResourceInformation.setOntology("ResourceInformation");
			msgResourceInformation.setContentObject((Serializable) resourceInformation);
			msgResourceInformation.setSender(SituatedAID);
			
			ACLMessage msgAgentPositions = new ACLMessage(ACLMessage.INFORM);
			msgAgentPositions.setOntology("agentPositions");
			

			
			
			msgAgentPositions.setContentObject((Serializable) agentPositions);
			msgAgentPositions.setSender(SituatedAID);
			
	        DFAgentDescription template = new DFAgentDescription();
	        ServiceDescription templateSd = new ServiceDescription();
	        // Busca agents de tipus "player"
	        
	        templateSd.setType("agentCollect");
        


            
            String[] agents = new String[] {"agentCollect","agentTanker","agentExplo"};
            for (String i : agents) {
                templateSd.setType(i);
                template.addServices(templateSd);
                SearchConstraints sc = new SearchConstraints();

                DFAgentDescription[] results = DFService.search(ag, template);
                if(results.length > 0){
                    for(int s = 0; s < results.length; ++s){
                        DFAgentDescription dfd = results[s];
                        String name = dfd.getName().getName();

                        if(!name.equals(SituatedAID.getName())) {
                            AID player = new AID(name,true);
                            msgMapTopology.addReceiver(player);
                            msgResourceInformation.addReceiver(player);
                            msgAgentPositions.addReceiver(player);
                        }
    //                    System.out.println("message sent from " + getAID().getName() + " to " + name);
                    }
                }
            }
            
            
            
            
            
            dedaleResendMessage(msgMapTopology);
            dedaleResendMessage(msgAgentPositions);
            dedaleResendMessage(msgAgentPositions);
			}
			catch(FIPAException e) {
				e.printStackTrace();
		    } 
			catch (IOException e) {
				e.printStackTrace();
			}
			
		}
    	
    }
    
    
    
    
    private void searchTank() {
		Capability cap = this.getCapability();
		BeliefBase beliefBase = cap.getBeliefBase();

		CustomMapRepresentation myMap = (CustomMapRepresentation)beliefBase.getBelief("map").getValue();
		HashMap<AID, Couple<Long, String>> Positions = (HashMap<AID, Couple<Long, String>>)beliefBase.getBelief("agPositions").getValue();
		

		String myPosition = Positions.get((AID)beliefBase.getBelief("AIDSituated").getValue()).getRight(); 
		
		Vector<Couple<AID,Long>> tankers = (Vector<Couple<AID,Long>>)beliefBase.getBelief("tanker").getValue();
		
		Vector<Couple<String,Integer>> candidats = new Vector<Couple<String,Integer>>();
		for (Couple<AID,Long> tank : tankers) {
			if (System.nanoTime() > tank.getRight()) {
				tankers.remove(tank);
				continue;
			}
			
			String Destination = Positions.get(tank.getLeft()).getRight();
			
			List<String> path = myMap.getShortestPath(myPosition,Destination);
			if (path.size() == 1) {
				instructDrop("GOLD");
				instructDrop("DIAMOND");
				return;
			}
			candidats.add(new Couple<String,Integer>(path.get(0), path.size()));
		}
		if (!candidats.isEmpty()) {
			Integer min = candidats.get(0).getRight();
			String dest = candidats.get(0).getLeft();
			for (Couple<String,Integer> i : candidats ) {
				if (i.getRight() < min) {
					min = i.getRight();
					dest = i.getLeft();
				}
			}
			instructMove(dest);
		}
		else {
			explore();
		}
	}

    private void searchPick() {
		Capability cap = this.getCapability();
		BeliefBase beliefBase = cap.getBeliefBase();
		
		
		CustomMapRepresentation myMap = (CustomMapRepresentation)beliefBase.getBelief("map").getValue();
		
		AID myAID = (AID) beliefBase.getBelief("AIDSituated").getValue();
		String myPosition = ((HashMap<AID, Couple<Long, String>>)beliefBase.getBelief("agPositions").getValue()).get(myAID).getRight();
		
		
		String TreasureType = (String) beliefBase.getBelief("TreasureType").getValue();
		int lockPickingSkill = (int) beliefBase.getBelief("lockPickingSkill").getValue();
		Vector<String> materials = new Vector<String>();
		if (TreasureType.equals("Any")) {
			materials.add(Observation.GOLD.toString());
			materials.add(Observation.DIAMOND.toString());
		} else {
			materials.add(TreasureType);
		}
		HashMap<String, Couple <Long, HashMap<Observation, Integer>>> resourceInfo = (HashMap<String, Couple<Long, HashMap<Observation, Integer>>>) beliefBase.getBelief("resourceInfo").getValue();
		
		Vector<Couple<String,Integer>> candidats = new Vector<Couple<String,Integer>>();
		
		
		
		for (Entry<String, Couple<Long, HashMap<Observation, Integer>>> entry : resourceInfo.entrySet()) {

			String Destination = entry.getKey();
			HashMap<Observation, Integer> content = entry.getValue().getRight();
			
			boolean resource = false;
			boolean canLockPick = false;
			Set<Observation> visualitzats = new HashSet<Observation>();
			Iterator<Entry<Observation, Integer>> it = content.entrySet().iterator();
			while(it.hasNext()) {
				Entry<Observation, Integer> obs = it.next();

				if(materials.contains(obs.getKey().toString()) && obs.getValue() > 0) {
					resource = true;
					visualitzats.add(obs.getKey());
					System.out.println(obs);
				}
				if(obs.getKey().toString().equals("LockPicking") && obs.getValue() <= lockPickingSkill)  canLockPick = true;
				

			}

			if (resource && canLockPick) {
				List<String> path = myMap.getShortestPath(myPosition,Destination);
				if (path.size() == 0) {
					for(Observation i : visualitzats) {
						instructUnlock(i);
						instructPick(i);
					}
					
					return;
				}
				else candidats.add(new Couple<String,Integer>(path.get(0), path.size()));
			}
		}
		
		
		if (!candidats.isEmpty()) {
			Integer min = candidats.get(0).getRight();
			String dest = candidats.get(0).getLeft();
			for (Couple<String,Integer> i : candidats ) {
				if (i.getRight() < min) {
					min = i.getRight();
					dest = i.getLeft();
				}
			}
			instructMove(dest);
		}
		else {
			explore();
		}
	}
	


    
    
	private void explore() {
        Capability cap = this.getCapability();
        BeliefBase beliefBase = cap.getBeliefBase();
        String myPosition = ((HashMap<AID, Couple<Long, String>>)beliefBase.getBelief("agPositions").getValue()).get((AID) beliefBase.getBelief("AIDSituated").getValue()).getRight(); 
        Set<String> openNodes = (Set<String>)beliefBase.getBelief("openNodes").getValue();
        CustomMapRepresentation myMap = (CustomMapRepresentation)beliefBase.getBelief("map").getValue();

        Vector<Couple<String,Integer>> candidats = new Vector<Couple<String,Integer>>();
        for (String node : openNodes) {
            List<String> path = myMap.getShortestPath(myPosition,node);
            candidats.add(new Couple<String,Integer>(path.get(0), path.size()));
        }

        if (!candidats.isEmpty()) {
            Integer min = candidats.get(0).getRight();
            String dest = candidats.get(0).getLeft();
            for (Couple<String,Integer> i : candidats ) {
                if (i.getRight() < min) {
                    min = i.getRight();
                    dest = i.getLeft();
                }
            }
            instructMove(dest);
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
		
		Vector<Couple<AID,Long>> tankerList =  new Vector<Couple<AID,Long>>();
				
				
				
		
		Belief tankers = new TransientBelief("tanker", tankerList);
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
		Belief myPos = new TransientBelief("myPos","");
		
		beliefBase.addBelief(tankers);
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
		beliefBase.addBelief(myPos);
		
		Plan registerPlan = new DefaultPlan(DFRegisterGoal.class, DFRegisterPlan.class);
        addGoal(new DFRegisterGoal());
        getCapability().getPlanLibrary().addPlan(registerPlan);
        
        Plan synchroPlan = new DefaultPlan(WaitSituatedGoal.class, WaitSituatedPlan.class);
        addGoal(new WaitSituatedGoal());
        getCapability().getPlanLibrary().addPlan(synchroPlan);
        
        

        

    }

}
