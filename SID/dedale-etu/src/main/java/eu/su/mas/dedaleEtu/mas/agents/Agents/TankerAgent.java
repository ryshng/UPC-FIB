package eu.su.mas.dedaleEtu.mas.agents.Agents;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import bdi4jade.belief.BeliefBase;
import bdi4jade.core.Capability;
import bdi4jade.plan.Plan;
import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.ReceiveTreasureTankerBehaviour;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedale.mas.agent.knowledge.MapRepresentation;
import eu.su.mas.dedale.mas.agent.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.agents.Agents.BDIAgent.WaitSituatedPlan;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.util.Logger;


/**
 * Dummy Tanker agent. It does nothing more than printing what it observes every 10s and receiving the treasures from other agents. 
 * <br/>
 * Note that this last behaviour is hidden, every tanker agent automatically possess it.
 * 
 * @author hc
 *
 */
public class TankerAgent extends AbstractDedaleAgent{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1784844593772918359L;
	private Logger myLogger = Logger.getMyLogger(getClass().getName());
	private MapRepresentation myMap;
	private HashMap<String, Couple <Long, HashMap<Observation, Integer>>> resourceInformation;
	private HashMap<AID, Couple<Long, String>> agentPositions;
	private AID bdi;
	//private String trackPos;

	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){

		super.setup();
		bdi = new AID();
		
		 DFAgentDescription dfd = new DFAgentDescription();
         ServiceDescription sd = new ServiceDescription();   
         sd.setType("Tanker"); 
         sd.setName(getName());
         dfd.setName(getAID());
         dfd.addServices(sd);
         try {
        	 DFService.register(this,dfd);
        	 List<Behaviour> lb=new ArrayList<Behaviour>();
        	 lb.add(new SynchroSendBDIAgent(this));
        	 lb.add(new SynchroReceiveBDIAgent(this));
        	 lb.add(new DFSBehaviour(this, this.myMap));
        	 addBehaviour(new startMyBehaviours(this,lb));
   
        	 System.out.println("the  agent "+this.getLocalName()+ " is started");       
         } catch (FIPAException e) {
             myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
             doDelete();
         }


	}

	/**
	 * This method is automatically called after doDelete()
	 */
	protected void takeDown(){

	}

	

/**************************************
 * 
 * 
 * 				BEHAVIOUR
 * 
 * 
 **************************************/
	
	private void setBDIAgent(AID aid) {
		this.bdi = aid;
	}
	
	private AID getBDIAgent() {
		return this.bdi;
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
            
			System.out.println("Starting synchronization with BDI agent ...");
			
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
                    System.out.println("Sending ack to BDI Agent ...");
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
            		((TankerAgent)this.myAgent).setBDIAgent(msg.getSender());
            		ACLMessage reply = msg.createReply();
            		//String s = msg.getContent() + "\n-BDI Agent found";
            		System.out.println("Confirmation arrived. Sending my name...");
            		reply.setPerformative(ACLMessage.CONFIRM);
            		reply.setSender(myAgent.getAID());
            		reply.setContent("AgentTanker");
            		send(reply);
            		ackReceive = true;            		
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
	
	class ReceiveMessageBehaviour extends CyclicBehaviour{
		private static final long serialVersionUID = 9088209402507795289L;
		private MapRepresentation myMap;
		
		public ReceiveMessageBehaviour (final AbstractDedaleAgent myagent, MapRepresentation myMap) {
			super(myagent);
			this.myMap = myMap;
		}
		@Override
		public void action() {
			
			ACLMessage msg = this.myAgent.receive();
				if(msg != null){
					
				}
				else {
					block();
				}
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
			
			String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
			if (myPosition!=""){
				List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
	//			System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);
	
				this.closedNodes.add(myPosition);
	
	
	
				if(this.myMap == null) {
					this.myMap= new MapRepresentation();
					((TankerAgent) this.myAgent).myMap = this.myMap;
	//				System.out.println("map has been initialized");
				}
	
				this.myMap.addNode(myPosition,MapAttribute.closed);
				
				Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
				
				//check de wind
	
				List<Couple<Observation, Integer>> currentNodeInfo = iter.next().getRight();
				boolean hasWind = false;
				
				for(int i = 0; i < currentNodeInfo.size(); i++) {
					if(currentNodeInfo.get(i).getLeft() == Observation.WIND) hasWind = true;
				}
				
				
				//SEND CURRENT POSITION INFORMATION
				HashMap<Observation, Integer> info = new HashMap<Observation, Integer>();
				for(Couple<Observation, Integer> o:currentNodeInfo) {
					info.put(o.getLeft(), o.getRight());
				}
				Couple<Long, HashMap<Observation, Integer>> newinfo = new Couple <Long, HashMap<Observation, Integer>>(System.nanoTime(), info);
				Couple<String, Couple<Long, HashMap<Observation, Integer>>> res = new Couple<String, Couple<Long, HashMap<Observation, Integer>>>(myPosition, newinfo);				
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setContent("Node-Info");
				//System.out.println("LLEGO2?");
				AID provider = ((TankerAgent) this.myAgent).getBDIAgent();
				//System.out.println("LLEGO3?");
				//System.out.println(provider.getLocalName());
				try {
					msg.setContentObject(res);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				msg.addReceiver(provider);
		        send(msg);
		        
		        
		        
		        
				while(!hasWind && iter.hasNext()) {
					String nodeId=iter.next().getLeft();
					if(!myPosition.equals(nodeId)) {
						
						if((!closedNodes.contains(nodeId)) && (!this.openNodes.contains(nodeId))) {
	
							this.myMap.addNode(nodeId,MapAttribute.open);
							this.openNodes.push(nodeId);
						}
						this.myMap.addEdge(myPosition,  nodeId);
					}
				}
				//System.out.println("LLEGO?");
				
				if(!openNodes.isEmpty()) {
					String Destination = openNodes.peek();
	//				System.out.println("path goes from" + myPosition + "  to   " + Destination);
					List<String> path = this.myMap.getShortestPath(myPosition,Destination);
					if(path.size() != 0) {
						String moveId = path.get(0);
						if(moveId.equals(Destination)) openNodes.pop();
						((AbstractDedaleAgent)this.myAgent).moveTo(moveId);
						//((PracticaAgent) this.myAgent).saveInfoNode(myPosition, System.nanoTime(), currentNodeInfo);
						//((TankerAgent) this.myAgent).sendInfo();
					}
				}
			}	
		}
	}
	
	class recieveMessagesBehaviour extends CyclicBehaviour {
		
		public recieveMessagesBehaviour (final AbstractDedaleAgent myagent) {
			super(myagent);
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public void action() {
			TankerAgent ag = ((TankerAgent) this.myAgent);
			MapRepresentation map = ag.myMap;
			
			
			ACLMessage msg = ag.receive();
			if(msg != null) {
				AID provider = ((TankerAgent) this.myAgent).getAID();
				msg.clearAllReceiver();
				msg.addReceiver(provider);
                send(msg);
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
	//        templateSd.setType("agentTanker");
	//        template.addServices(templateSd);
	//        templateSd.setType("agentExplo");
	//        template.addServices(templateSd);
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
	//                System.out.println("message sent from " + getAID().getName() + " to " + name);
	            }
	        }
	        
	        
	        
	        
	
	        sendMessage(msgMapTopology);
	//        sendMessage(msgResourceInformation);
	        sendMessage(msgAgentPositions);
	
	
	    } catch(FIPAException e) {
			e.printStackTrace();
	    
	    } catch (IOException e) {
			e.printStackTrace();
	    }
	}
}
