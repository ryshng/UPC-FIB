package eu.su.mas.dedaleEtu.mas.agents.dummies;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import bdi4jade.plan.Plan;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.ReceiveTreasureTankerBehaviour;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.agents.dummies.DummyCollectorAgent.recieveMessagesBehaviour;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;


/**
 * Dummy Tanker agent. It does nothing more than printing what it observes every 10s and receiving the treasures from other agents. 
 * <br/>
 * Note that this last behaviour is hidden, every tanker agent automatically possess it.
 * 
 * @author hc
 *
 */
public class DummyTankerAgent extends AbstractDedaleAgent{

	/**
	 * 
	 */
	private static final long serialVersionUID = -178484459918359L;



	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){

		super.setup();

		
		System.out.println("the  agent "+this.getLocalName()+ " is started");

 		addBehaviour(new RegisterDFBehaviour(this));
	}

	/**
	 * This method is automatically called after doDelete()
	 */
	protected void takeDown(){

	}
}


/**************************************
 * 
 * 
 * 				BEHAVIOUR
 * 
 * 
 **************************************/

class RegisterDFBehaviour extends OneShotBehaviour{
	/**
	 * When an agent choose to move
	 *  
	 */
	private static final long serialVersionUID = 9088209402507795289L;

	public RegisterDFBehaviour (final AbstractDedaleAgent myagent) {
		super(myagent);
		//super(myagent);
	}

	@Override
	public void action() {

		 DFAgentDescription dfd = new DFAgentDescription();
         ServiceDescription sd = new ServiceDescription(); 
         
         sd.setType("agentTanker"); 
         sd.setName(myAgent.getName());
         dfd.setName(myAgent.getAID());
         dfd.addServices(sd);
         try {
        	 DFService.register(myAgent,dfd);
        	 List<Behaviour> lb=new ArrayList<Behaviour>();

         } catch (FIPAException e) {
             
         }

// 		myAgent.addBehaviour(new RandomTankerBehaviour((AbstractDedaleAgent)myAgent));
	}

}

class RandomTankerBehaviour extends TickerBehaviour{
	/**
	 * When an agent choose to migrate all its components should be serializable
	 *  
	 */
	boolean first;
	private static final long serialVersionUID = 9088209402507795289L;
	public RandomTankerBehaviour (final AbstractDedaleAgent myagent) {
		super(myagent, 100);
		first = true;
	}

	@Override
	public void onTick()  {
		if(first) {
			first = true;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		DFAgentDescription template = new DFAgentDescription();
        ServiceDescription templateSd1 = new ServiceDescription();
        templateSd1.setType("agentCollector");
        template.addServices(templateSd1);
        
        ServiceDescription templateSd2 = new ServiceDescription();
	    ServiceDescription templateSd3 = new ServiceDescription();
	    
//	    templateSd2.setType("agentGold");
//        templateSd3.setType("agentDiamond");
        template.addServices(templateSd2);
        template.addServices(templateSd3);
        
        String currentPos = ((AbstractDedaleAgent) myAgent).getCurrentPosition();
        
        
        //Belief AIDSituated = new TransientBelief("AIDSituated", new AID());
        AID aid = myAgent.getAID();
        
        Long start = System.nanoTime();
		Long limit = start + 1000000000;; 
        //SearchConstraints sc = new SearchConstraints();
        try{
        	
           
            DFAgentDescription[] results = DFService.search(myAgent, template);	          
            if(results.length > 0){
                for(int i = 0; i < results.length; ++i){
                    DFAgentDescription dfd = results[i];
                    AID provider = dfd.getName();
//                    System.out.println(provider);
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setOntology("TankingPositionInform");
                    Couple<String, Long> inf = new Couple<String, Long>(currentPos, limit);
                    msg.setSender(aid);
                    msg.setContentObject(inf);
                    msg.addReceiver(provider);
                    ((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
                }
             
            }
//            System.out.println("Agent searched DL correctly.");	                
        } catch(FIPAException e){
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}