package com.logentries.lb;

import akka.io.Tcp;

import java.io.Serializable;

/**
 * Created by chris on 25/10/14.
 */
public interface ClientResponse extends Serializable {
	public Tcp.Received getReceived();
}
