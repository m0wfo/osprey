package com.logentries.osprey;

import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.cluster.Cluster;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import scala.collection.JavaConverters;

import java.util.ArrayList;
import java.util.List;


/**
 * The entry point to the osprey process.
 */
public final class App {

	private static final String PEER_PORT = "peer.port";
	private static final String PEER_SEEDS = "peer.seeds";

	private App() {
	}

	public static void main(String[] args) {
		System.setProperty("archaius.dynamicPropertyFactory.registerConfigWithJMX", "true");

		List<Address> addresses = new ArrayList<>();

		DynamicIntProperty peerPort = DynamicPropertyFactory.getInstance().getIntProperty(PEER_PORT, 0);
		DynamicStringProperty seeds = DynamicPropertyFactory.getInstance().getStringProperty(PEER_SEEDS, "");

		if (seeds.get().equals("")) {
			throw new IllegalArgumentException("No seeds specified!");
		}

		if (peerPort.get() < 0 || peerPort.get() > 65536) {
			throw new IllegalArgumentException("Invalid peer port!");
		}

		// bootstrap cluster assembly by adding the local actor system as a seed
		addresses.add(Address.apply("akka.tcp", "osprey", "localhost", peerPort.get()));

		// add the rest of the seeds
		String[] hosts = seeds.get().split(",");
		for (String arg : hosts) {
			String host = arg.trim();
			addresses.add(Address.apply("akka.tcp", "osprey", host, peerPort.get()));
		}

		// boot the local actor system
		ActorSystem system = ActorSystem.create("osprey");
		// assemble the cluster based on the list of seeds
		Cluster.get(system).joinSeedNodes(JavaConverters.asScalaBufferConverter(addresses).asScala().toList());
		// start up a listener
		system.actorOf(Props.create(Listener.class), "listener");
	}
}
