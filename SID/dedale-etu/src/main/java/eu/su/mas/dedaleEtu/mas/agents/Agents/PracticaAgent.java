package eu.su.mas.dedaleEtu.mas.agents.Agents;


import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.princ.ConfigurationFile;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;

import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.util.Logger;
import jade.wrapper.StaleProxyException;
import jade.core.behaviours.OneShotBehaviour;


import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


import jade.util.Logger;




/**
 * This dummy collector moves randomly, tries all its methods at each time step, store the treasure that match is treasureType 
 * in its backpack and intends to empty its backPack in the Tanker agent. @see {@link RandomWalkExchangeBehaviour}
 * 
 * @author hc
 *
 */
public class PracticaAgent extends AbstractDedaleAgent {


	private Logger myLogger = Logger.getMyLogger(getClass().getName());
	private static final long serialVersionUID = -1784844593772918359L;
	

	private MapRepresentation myMap;
	private HashMap<String, Couple <Long, HashMap<Observation, Integer>>> resourceInformation;
	private HashMap<AID, Couple<Long, String>> agentPositions;
	private AID bdi;
	private String rol;
	private ArrayList<String> instruction;
	
	Set<Couple<Observation,java.lang.Integer>> exp;
	int lockpicking_skill;
	Observation TreasureType;
	
	
	
	protected void setup(){

		super.setup();
		List<Behaviour> lb=new ArrayList<Behaviour>();
		instruction = new ArrayList<String>();
		instruction.add("resend");
		instruction.add("move");
		instruction.add("unlock");
		instruction.add("collect");
		instruction.add("drop");
		instruction.add("requestBackpack");

		lb.add(new initializeBehaviour(this));
		lb.add(new SynchroSendBDIAgent(this));
		lb.add(new SynchroReceiveBDIAgent(this));
		
		addBehaviour(new startMyBehaviours(this,lb));
//		System.out.println("Agent "+this.getLocalName()+ " has been registered in the DF.");
	}



	protected void takeDown(){

	}

	class initializeBehaviour extends OneShotBehaviour {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -7647894424857962196L;
		
		private AbstractDedaleAgent Ag;
		
		public initializeBehaviour (final AbstractDedaleAgent myagent) {
			super(myagent);
			Ag = myagent;
		}
		
		@Override
		public void action() {
			
			exp = getMyExpertise();

			TreasureType = getMyTreasureType();
			


			 DFAgentDescription dfd = new DFAgentDescription();
	         ServiceDescription sd = new ServiceDescription();   

			lockpicking_skill = -1;
			for(Couple<Observation,java.lang.Integer> it : exp) {
				if(it.getLeft().toString().equals("LockPicking")) lockpicking_skill = it.getRight(); 
			}
			
			
	         
			if(lockpicking_skill > 0) {
				
				
				
//				System.out.println("IT'S A COLLECTOR");
		         sd.setType("agentCollect"); 
				Ag.addBehaviour(new DFSBehaviour(Ag, myMap));

		        rol = "collectAgent";
				
				
			}
			else {
				
				
//				System.out.println("IT'S A TANKER");
		         sd.setType("agentTanker"); 
		        rol = "tankerAgent";
			}
			

			 Ag.addBehaviour(new recieveMessagesBehaviour(Ag));
			 ((PracticaAgent)Ag).agentPositions = new HashMap<AID, Couple<Long, String>>();
			 ((PracticaAgent)Ag).resourceInformation = new HashMap<String, Couple <Long, HashMap<Observation, Integer>>>();
			 
	         sd.setName(getName());
	         dfd.setName(getAID());
	         dfd.addServices(sd);
	         try {
	        	 DFService.register(myAgent,dfd);
	        	 List<Behaviour> lb=new ArrayList<Behaviour>();
//	        	 System.out.println("the  agent "+myAgent.getLocalName()+ " is started");       
	         } catch (FIPAException e) {
//	             myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
	             doDelete();
	         }
			

			
		}
		
	}
	
	class SynchroSendBDIAgent extends SimpleBehaviour{
		private static final long serialVersionUID = -5521094927843971108L;
		private boolean ackSend = false;


	/**
	 * 
	 * @param myagent
	 * @param myMap known map of the world the agent is living in
	 * @param agentNames name of the agents to share the map with
	 */
		public SynchroSendBDIAgent(final AbstractDedaleAgent myagent) {
			super(myagent);
		}

		@Override
		public void action() {
            
//			System.out.println("Starting synchronization with BDI agent ...");
			
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription templateSd = new ServiceDescription();
            // Search agents of type "BDI"
            templateSd.setType("BDI");
            template.addServices(templateSd);
            
            try{
                // Array results contain all (in this case only 1) agents on the platform of type "BDI"
                DFAgentDescription[] results = DFService.search(myAgent, template);
                if(results.length > 0){
                    DFAgentDescription dfd= results[0];
                    AID provider = dfd.getName();
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    //String s = "-Send ACK";
                    msg.addReceiver(provider);
//                    System.out.println("Sending ack to BDI Agent ...");
                    send(msg);
                    ackSend = true;
                }
            } catch(FIPAException e){
                
            }
		}

		@Override
		public boolean done() {
			return ackSend;
		}
		
	}
	
	class SynchroReceiveBDIAgent extends SimpleBehaviour{
		private static final long serialVersionUID = -1545472147329608150L;
		private boolean ackReceive = false;


	/**
	 * 
	 * @param myagent
	 * @param myMap known map of the world the agent is living in
	 * @param agentNames name of the agents to share the map with
	 */
		public SynchroReceiveBDIAgent(final AbstractDedaleAgent myagent) {
			super(myagent);
		}

		@Override
		public void action() {
            ACLMessage msg= myAgent.receive();
            if(msg != null) {
            	if(msg.getPerformative() == ACLMessage.CONFIRM) {
            		//((TankerAgent) this.myAgent)
            		((PracticaAgent)this.myAgent).setBDIAgent(msg.getSender());
            		ACLMessage reply = msg.createReply();
            		//String s = msg.getContent() + "\n-BDI Agent found";
//            		System.out.println("Confirmation arrived. Sending my name...");
            		reply.setPerformative(ACLMessage.CONFIRM);
            		reply.setSender(myAgent.getAID());
            		
            		

//            		Set<Couple<Observation,java.lang.Integer>> exp;
//            		Observation TreasureType;
//            		
            		String lps = String.valueOf(lockpicking_skill);
            		String TT = TreasureType.toString();
            		ArrayList<String> info = new ArrayList<String>();
            		info.add(lps);
            		info.add(TT);
//            		System.out.println(info);
            		try {
						reply.setContentObject(info);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            		send(reply);
            		ackReceive = true;  
            		
        			List<Couple<String, List<Couple<Observation, Integer>>>> o = observe();
        			((PracticaAgent)this.myAgent).sendObservation(System.nanoTime(), o);
        			
        			System.out.println("first observation sent");
//        			System.out.println("observationSent");
            		
            		
            	}  	
            }
            else {
            	block();
            }      
		}

		@Override
		public boolean done() {
			return ackReceive;
		}	
	}
	
	class DFSBehaviour extends TickerBehaviour{
		
		/**
		 * When an agent choose to move
		 *  
		 */
		private static final long serialVersionUID = 9088209402507795289L;
		
		
		private MapRepresentation myMap;
		private AbstractDedaleAgent ag;
		private Stack<String> openNodes;

		private Set<String> closedNodes;
		
		public DFSBehaviour (final AbstractDedaleAgent myagent, MapRepresentation myMap) {
			super(myagent, 100);
			this.myMap = myMap;
			this.openNodes=new Stack<String>();
			this.closedNodes=new HashSet<String>();
		}

		@Override
		public void onTick() {
				
		}

	}
	
	class recieveMessagesBehaviour extends CyclicBehaviour {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public recieveMessagesBehaviour (final AbstractDedaleAgent myagent) {
			super(myagent);
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public void action() {
			PracticaAgent ag = ((PracticaAgent) this.myAgent);
			MapRepresentation map = ag.myMap;
			
			
			ACLMessage msg = ag.receive();
			ArrayList<String> ins = ((PracticaAgent)this.myAgent).getInstructions();
			
			if(msg != null) {
				if(msg.getOntology() != "") {
					if(!ins.contains(msg.getOntology())) {
				
						AID provider = ((PracticaAgent) this.myAgent).getBDIAgent();
						msg.clearAllReceiver();
						msg.addReceiver(provider);
		                send(msg);
					}
					else if(ins.contains(msg.getOntology())) {
						List<Couple<String,List<Couple<Observation,Integer>>>> lobs;
						switch(msg.getOntology()) {
						case "resend":
							ACLMessage msg2send;
							try {
								msg2send = (ACLMessage)msg.getContentObject();
								((AbstractDedaleAgent)this.myAgent).sendMessage(msg2send);
							} catch (UnreadableException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							break;
						case "move":
							String nodePos = msg.getContent();
							((AbstractDedaleAgent)this.myAgent).moveTo(nodePos);
							lobs=((AbstractDedaleAgent)this.myAgent).observe();
							((PracticaAgent)this.myAgent).sendObservation(System.nanoTime(), lobs);
							break;
						case "unlock":
							try {
								Observation obs = (Observation)msg.getContentObject();
								boolean open = ((AbstractDedaleAgent)this.myAgent).openLock(obs);
								lobs=((AbstractDedaleAgent)this.myAgent).observe();
								((PracticaAgent)this.myAgent).sendObservation(System.nanoTime(), lobs);
							} catch (UnreadableException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}							
							break;
						case "collect":
							System.out.println("REBUT COLLECT");
							((AbstractDedaleAgent)this.myAgent).pick();
							lobs=((AbstractDedaleAgent)this.myAgent).observe();
							((PracticaAgent)this.myAgent).sendObservation(System.nanoTime(), lobs);
							break;
						case "drop":
							String tankName = msg.getContent();
							((AbstractDedaleAgent)this.myAgent).emptyMyBackPack(tankName);
							break;
						case "requestBackpack":							
							try {
								ACLMessage msg2bdi = new ACLMessage(ACLMessage.INFORM);
								msg2bdi.setOntology("informBackpack");
								List<Couple<Observation, Integer>> space = ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace();
								msg2bdi.setContentObject((Serializable) space);
								msg2bdi.addReceiver(((PracticaAgent)this.myAgent).getBDIAgent());
								send(msg2bdi);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}				
			}
			else {
				block();
			}
		}
	}
	
	public void sendInfo() {
		
		
		try{
		
			ACLMessage msgMapTopology = new ACLMessage(ACLMessage.INFORM);
			msgMapTopology.setOntology("mapTopology");
			msgMapTopology.setContentObject(this.myMap.getSerializableGraph());
			msgMapTopology.setSender(getAID());
	//		msgMapTopology.setContent(myMap.getSerializableGraph()); 
			
			ACLMessage msgResourceInformation = new ACLMessage(ACLMessage.INFORM);
			msgResourceInformation.setOntology("ResourceInformation");
			msgResourceInformation.setContentObject((Serializable) this.resourceInformation);
			msgResourceInformation.setSender(getAID());
			
			ACLMessage msgAgentPositions = new ACLMessage(ACLMessage.INFORM);
			msgAgentPositions.setOntology("agentPositions");
			
			Couple<Long, String> c = new Couple<Long,String>(System.nanoTime(),getCurrentPosition());
			
			
			agentPositions.put(getAID(),c);
			
			
			msgAgentPositions.setContentObject((Serializable) this.agentPositions);
			msgAgentPositions.setSender(getAID());
			
	        DFAgentDescription template = new DFAgentDescription();
	        ServiceDescription templateSd = new ServiceDescription();
	        // Busca agents de tipus "player"
	        templateSd.setType("agentCollect");
	        template.addServices(templateSd);
//	        templateSd.setType("agentTanker");
//	        template.addServices(templateSd);
//	        templateSd.setType("agentExplo");
//	        template.addServices(templateSd);
	        SearchConstraints sc = new SearchConstraints();
        
            DFAgentDescription[] results = DFService.search(this, template);
            int count = 0;
            if(results.length > 0){
                for(int i = 0; i < results.length; ++i){
                    DFAgentDescription dfd = results[i];
                    String name = dfd.getName().getName();
                    
                    if(!name.equals(getAID().getName())) {
	                    AID player = new AID(name,true);
	                    msgMapTopology.addReceiver(player);
	                    msgResourceInformation.addReceiver(player);
	                    msgAgentPositions.addReceiver(player);
                    }
//                    System.out.println("message sent from " + getAID().getName() + " to " + name);
                }
            }
            
            
            
            

            sendMessage(msgMapTopology);
            sendMessage(msgResourceInformation);
            sendMessage(msgAgentPositions);


        } catch(FIPAException e) {
			e.printStackTrace();
        
        } catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void setBDIAgent(AID aid) {
		this.bdi = aid;
	}
	
	private AID getBDIAgent() {
		return this.bdi;
	}
	
	private ArrayList<String> getInstructions() {
		return this.instruction;
	}
	
	private void sendMapTopology() {
		
		
		ACLMessage msg = new ACLMessage(ACLMessage.PROPAGATE);
		msg.setOntology("mapTopology");
		try {
			msg.setContentObject(myMap.getSerializableGraph());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		msg.setSender(getAID());
		msg.addReceiver(bdi);
		send(msg);
	}
	
	@SuppressWarnings("unused")
	private void sendObservation(Long time, List<Couple<String,List<Couple<Observation,Integer>>>> observation) {
		
		

		try {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setOntology("observe");
			msg.setPostTimeStamp(time);
			
			
			msg.setContentObject((Serializable) observation);
			msg.setSender(getAID());
			msg.addReceiver(bdi);
			send(msg);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
}
