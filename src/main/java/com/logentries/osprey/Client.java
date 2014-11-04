package com.logentries.osprey;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.io.Tcp;
import akka.io.Tcp.*;
import akka.io.TcpMessage;
import akka.japi.Procedure;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import scala.Option;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * Forwards messages onto a backend.
 */
class Client extends UntypedActor {

	/**
	 * Exception used to signify the failure of a backend connection.
	 *
	 * <p>This indicates to a supervising actor/router that it should restart this client.</p>
	 */
	static class ClientException extends RuntimeException implements Serializable {
	}

	private static final String BACKEND_PORT = "backend.port";

	private final DynamicIntProperty backendPort;

	/**
	 * A connected client assumes the behaviour of this connection handler
	 * when a connection is successfully established with its backend.
	 */
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
				throw new ClientException();
			}
		}
	}

	private final ActorRef ioActor;

	public Client() {
		this.backendPort = DynamicPropertyFactory.getInstance().getIntProperty(BACKEND_PORT, 0);
		this.ioActor = Tcp.get(context().system()).manager();
	}

	@Override
	public void preStart() {
		ioActor.tell(TcpMessage.connect(new InetSocketAddress("localhost", backendPort.get())), self());
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
			throw new ClientException();
		} else {
			unhandled(message);
		}
	}
}
