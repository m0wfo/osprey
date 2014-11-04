package com.logentries.osprey;

import akka.io.Tcp;

import java.io.Serializable;

/**
 * Differentiates a TCP received message coming from a back-end response.
 */
interface ClientResponse extends Serializable {
	public Tcp.Received getReceived();
}
