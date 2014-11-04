package com.logentries.osprey;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.io.Tcp.*;
import akka.io.TcpMessage;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteString;

/**
 * Handles connection state changes within a client for a single connection.
 */
class ConnectionHandler extends AbstractActor {

	private ActorRef receiver;

	public ConnectionHandler(ActorRef router) {
		receive(ReceiveBuilder.match(Received.class, msg -> {
			// we have a connection! keep track of the receiver (client)
			receiver = sender();
			router.tell((ClientRequest) () -> msg, self());
		}).match(ClientResponse.class, msg -> {
			// send the message back to the client
			ByteString bs = msg.getReceived().data();
			receiver.tell(TcpMessage.write(bs), self());
		}).match(ConnectionClosed.class, msg -> getContext().stop(self()))
				.matchAny(this::unhandled).build());
	}
}
