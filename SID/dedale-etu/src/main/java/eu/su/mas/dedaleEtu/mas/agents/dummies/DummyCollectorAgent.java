package eu.su.mas.dedaleEtu.mas.agents.dummies;


import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.agents.Agents.PracticaAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dataStructures.tuple.Couple;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.util.Logger;

/**
 * This dummy collector moves randomly, tries all its methods at each time step, store the treasure that match is treasureType 
 * in its backpack and intends to empty its backPack in the Tanker agent. @see {@link RandomWalkExchangeBehaviour}
 * 
 * @author hc
 *
 */
public class DummyCollectorAgent extends AbstractDedaleAgent{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1784844593772918359L;



	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){

		super.setup();

		List<Behaviour> lb=new ArrayList<Behaviour>();
		lb.add(new RandomWalkExchangeBehaviour(this));

		addBehaviour(new startMyBehaviours(this,lb));

		
		
		System.out.println("the  agent "+this.getLocalName()+ " is started");

	}



	/**
	 * This method is automatically called after doDelete()
	 */
	protected void takeDown(){
		
	}
	class RandomWalkExchangeBehaviour extends OneShotBehaviour{
		/**
		 * When an agent choose to move
		 *  
		 */
		private static final long serialVersionUID = 9088209402507795289L;

		public RandomWalkExchangeBehaviour (final AbstractDedaleAgent myagent) {
			super(myagent);
			//super(myagent);
		}

		@Override
		public void action() {

			 DFAgentDescription dfd = new DFAgentDescription();
	         ServiceDescription sd = new ServiceDescription(); 
	         
	         sd.setType("agentCollector"); 
	         sd.setName(getName());
	         dfd.setName(getAID());
	         dfd.addServices(sd);
	         try {
	        	 DFService.register(myAgent,dfd);
	        	 List<Behaviour> lb=new ArrayList<Behaviour>();
 
	         } catch (FIPAException e) {
	             doDelete();
	         }

	 		myAgent.addBehaviour(new recieveMessagesBehaviour((AbstractDedaleAgent)myAgent));
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
			ACLMessage msg = myAgent.receive();
			
			if(msg != null) {			
			}
			else {
				block();
			}
		}
	}
	

	
	
	
	
	
	
	
	
	
	
}