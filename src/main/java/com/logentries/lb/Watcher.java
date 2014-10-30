package com.logentries.lb;

import akka.actor.UntypedActor;
import akka.cluster.Cluster;

/**
 * Created by chris on 25/10/14.
 */
public class Watcher extends UntypedActor {

	private final Cluster cluster;

	public Watcher() {
		this.cluster = Cluster.get(context().system());
	}

	@Override
	public void preStart() {
		cluster.subscribe(self());
	}

	@Override
	public void postStop() {
		cluster.unsubscribe(self());
	}

	@Override
	public void onReceive(Object message) throws Exception {
		String x = "";
	}
}
