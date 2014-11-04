package com.logentries.osprey;

import akka.actor.*;
import akka.io.Tcp;
import akka.io.Tcp.*;
import akka.io.TcpMessage;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.FromConfig;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import scala.concurrent.duration.Duration;

import java.net.InetSocketAddress;

/**
 * The osprey 'front-end'.
 *
 * <p>This listens for connections from public clients and forwards them on to available back-ends via an
 * internal configuration-driven router.</p>
 */
class Listener extends AbstractActor {

	private static final String LISTEN_PORT = "listen.port";

	private static final SupervisorStrategy STRATEGY = new OneForOneStrategy(-1, Duration.Inf(),
			DeciderBuilder.matchAny(e -> SupervisorStrategy.restart()).build());

	private final DynamicIntProperty listenPort;
	private final ActorRef ioActor, router;

	public Listener() {
		this.listenPort = DynamicPropertyFactory.getInstance().getIntProperty(LISTEN_PORT, 0);
		this.ioActor = Tcp.get(context().system()).manager();
		this.router = context().actorOf(FromConfig.getInstance().props(Props.create(Client.class)), "backend");

		receive(ReceiveBuilder
				.match(Bound.class, msg -> ioActor.tell(msg, self()))
				.match(CommandFailed.class, msg -> context().stop(self()))
				.match(Connected.class, msg -> {
					// a ConnectionHandler actor handles all further I/O for this connection
					ActorRef handler = context().actorOf(Props.create(ConnectionHandler.class, router));
					sender().tell(TcpMessage.register(handler), self());
				}).matchAny(this::unhandled).build());
	}

	@Override
	public SupervisorStrategy supervisorStrategy() {
		return STRATEGY;
	}

	@Override
	public void preStart() {
		InetSocketAddress remote = new InetSocketAddress("0.0.0.0", listenPort.get());
		ioActor.tell(TcpMessage.bind(self(), remote, 1024), self());
	}
}
