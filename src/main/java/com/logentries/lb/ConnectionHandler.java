package com.logentries.lb;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.io.Tcp.*;
import akka.io.TcpMessage;
import akka.util.ByteString;

/**
 * Created by chris on 25/10/14.
 */
public class ConnectionHandler extends UntypedActor {

	private final ActorRef router;
	private ActorRef receiver;

	public ConnectionHandler(ActorRef router) {
		this.router = router;
	}

	@Override
	public void onReceive(Object message) {
		if (message instanceof Received) {
			receiver = sender();
			router.tell((ClientRequest) () -> (Received) message, self());
		} else if (message instanceof ClientResponse) {
			ByteString bs = ((ClientResponse) message).getReceived().data();
			receiver.tell(TcpMessage.write(bs), self());
		} else if (message instanceof ConnectionClosed) {
			getContext().stop(getSelf());
		}
	}
}
