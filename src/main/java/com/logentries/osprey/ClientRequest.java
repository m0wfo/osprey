package com.logentries.osprey;

import akka.io.Tcp;

import java.io.Serializable;

/**
 * Distinguishes a received TCP message coming from a front-end request.
 */
interface ClientRequest extends Serializable {
	public Tcp.Received getReceived();
}