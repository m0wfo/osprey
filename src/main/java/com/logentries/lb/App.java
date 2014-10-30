package com.logentries.lb;

import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.cluster.Cluster;
import scala.collection.JavaConverters;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by chris on 25/10/14.
 */
public class App {

	public static void main(String[] args) {
		List<Address> addresses = new ArrayList<>();
		addresses.add(Address.apply("lb", "tcp", "localhost", 2551));
		for (String arg : args) {
			String[] parts = arg.split(":");
			addresses.add(Address.apply("lb", "tcp", parts[0], Integer.valueOf(parts[1])));
		}

		ActorSystem system = ActorSystem.create("lb");
//		Cluster.get(system).joinSeedNodes(JavaConverters.asScalaBufferConverter(addresses).asScala().toList());
		system.actorOf(Props.create(Listener.class, new InetSocketAddress("0.0.0.0", 8080)), "listener");
		system.actorOf(Props.create(Watcher.class));
	}
}
