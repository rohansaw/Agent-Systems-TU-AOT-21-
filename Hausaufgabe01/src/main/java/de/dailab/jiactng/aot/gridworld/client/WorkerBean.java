package de.dailab.jiactng.aot.gridworld.client;


import de.dailab.jiactng.aot.gridworld.messages.WorkerConfirm;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;


/**
 * You can use this stub as a starting point for your worker bean or start from scratch.
 */



public class WorkerBean extends AbstractAgentBean {
	/*
	 * this bean represents one of your Worker agents (i.e. each Worker Agent you initialize with this bean
	 * will have a separate copy); it's structure will be similar to your Broker agent's
	 *
	 *
	 * note that the number of workers may vary from grid to grid, but the number of worker
	 * agents will always be the same (specified in the client.xml); you will have to have your Broker somehow tell
	 * the worker agents which of them are currently needed and who may idle
	 *
	 * you could, theoretically, also control all your Workers from a single worker agent (and
	 * bean), or even implement both the Broker and the Worker in the same bean, but that would
	 * of course defeat the purpose of this exercise and may not be possible in "real life"
	 */




	@Override
	public void doStart() throws Exception {
		/*
		 * this will be called once when the agent starts and can be used for initialization work
		 * note that when this method is executed, (a) it is not guaranteed that all the other
		 * agents are already started and/or their actions are known, and (b) the agent's execution
		 * has not yet started, so do not wait for any actions to be completed in this method (you
		 * can invoke actions, though, if they are already known to the agent)
		 *
		 *
		 * You can use a SpaceObserver to listen to messages, but you can also check messages in execute()
		 * and only temporarily attach a SpaceObserver for specific purposes
		 *
		 * As an example it is added here at the beginning.
		 */
		memory.attach(new MessageObserver(), new JiacMessage());

		log.info("starting...");
	}

	@Override
	public void execute() {
		/*
		 * this is executed periodically by the agent; check the BrokerBean.java for an example.
		 */
	}




	/*
	 * You can implement some functions and helper methods here.
	 */



	/** This is an example of using the SpaceObeserver for message processing. */
	@SuppressWarnings({"serial", "rawtypes"})
	class MessageObserver implements SpaceObserver<IFact> {

		@Override
		public void notify(SpaceEvent<? extends IFact> event) {
			if (event instanceof WriteCallEvent) {
				JiacMessage message = (JiacMessage) ((WriteCallEvent) event).getObject();

				if (message.getPayload() instanceof WorkerConfirm) {
					/* do something */
				}
			}
		}
	}


}