package com.logentries.lb;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.io.Tcp;
import akka.io.Tcp.*;
import akka.io.TcpMessage;
import akka.japi.Procedure;
import scala.Option;

import java.net.InetSocketAddress;

/**
 * Created by chris on 25/10/14.
 */
public class Client extends UntypedActor {

	public static class ClientException extends RuntimeException {
	}

	private class ConnectionHandler implements Procedure<Object> {

		private final ActorRef connection;
		private ActorRef receiver;

		public ConnectionHandler(ActorRef connection) {
			this.connection = connection;
		}

		@Override
		public void apply(Object msg) {
			if (msg instanceof CommandFailed) {
				// OS kernel socket buffer was full
				throw new ClientException();
			} else if (msg instanceof ClientRequest) {
				ClientRequest request = (ClientRequest) msg;
				receiver = sender();
				connection.tell(TcpMessage.write(request.getReceived().data()), self());
			} else if (msg instanceof Received) {
				receiver.tell((ClientResponse) () -> (Received) msg, self());
			} else if (msg instanceof ConnectionClosed) {
				context().stop(self());
			}
		}
	}

	private final ActorRef ioActor;

	public Client() {
		this.ioActor = Tcp.get(context().system()).manager();
	}

	@Override
	public void preStart() {
		ioActor.tell(TcpMessage.connect(new InetSocketAddress("localhost", 9960)), self());
	}

	@Override
	public void preRestart(Throwable err, Option<Object> msg) {
		ioActor.tell(TcpMessage.close(), self());
	}

	@Override
	public void onReceive(Object message) {
		if (message instanceof CommandFailed) {
			throw new ClientException();
		} else if (message instanceof Connected) {
			sender().tell(TcpMessage.register(self()), self());
			getContext().become(new ConnectionHandler(sender()));
		} else if (message instanceof ConnectionClosed) {
			context().stop(self());
		} else {
			unhandled(message);
		}
	}
}
