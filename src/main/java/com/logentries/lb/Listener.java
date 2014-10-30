package com.logentries.lb;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.io.Tcp;
import akka.io.TcpMessage;
import scala.concurrent.duration.Duration;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by chris on 25/10/14.
 */
public class Listener extends UntypedActor {

	private static final SupervisorStrategy STRATEGY = SupervisorStrategy.stoppingStrategy();

	private final ActorRef ioActor, router;

	public Listener(InetSocketAddress remote) {
		this.clients = new ArrayList<>();
		this.ioActor = Tcp.get(context().system()).manager();
		this.router = context().actorOf(Props.create(Client.class));
//		this.router = new Router(new RoundRobinRoutingLogic());
//		context().watch(router);
		ioActor.tell(TcpMessage.bind(self(), remote, 512), self());
	}

	@Override
	public void preStart() {
	}

	@Override
	public SupervisorStrategy supervisorStrategy() {
		return STRATEGY;
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Tcp.Bound) {
			ioActor.tell(msg, getSelf());
		} else if (msg instanceof Tcp.CommandFailed) {
			getContext().stop(getSelf());
		} else if (msg instanceof Tcp.Connected) {
			// a ConnectionHandler actor handles all further I/O for this connection
			ActorRef handler = context().actorOf(Props.create(ConnectionHandler.class, router));
			getSender().tell(TcpMessage.register(handler), self());
		} else if (msg instanceof Terminated) {
			ActorPath victim = ((Terminated) msg).actor().path();
			context().system().scheduler().scheduleOnce(Duration.create(500, TimeUnit.MILLISECONDS),
					(Runnable) () -> self().tell(victim, ActorRef.noSender()),
					context().dispatcher());
		} else if (msg instanceof ActorPath) {

		}
	}
}
