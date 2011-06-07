/**
 * Copyright (c) 2011, Sven Reissmann
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 	* Redistributions of source code must retain the above copyright 
 * 	  notice, this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright 
 * 	  notice, this list of conditions and the following disclaimer in the 
 * 	  documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.jivesoftware.openfire.plugin.muninstats;

import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.plugin.MuninStats;
import org.jivesoftware.openfire.session.Session;
import org.xmpp.packet.Packet;

/**
 * This class counts the incoming and outgoing packets
 */
public class PacketMonitor implements PacketInterceptor {
	private int packetsIn;
	private int packetsOut;

	private static PacketMonitor instance = new PacketMonitor();

	/** return singleton instance */
	public static PacketMonitor getInstance() {
		return instance;
	}

	private PacketMonitor() {
		packetsIn = 0;
		packetsOut = 0;
	}

	public void init(MuninStats plugin) {
		InterceptorManager.getInstance().addInterceptor(this);
	}

	public void destroy() {
		InterceptorManager.getInstance().removeInterceptor(this);
	}

	public synchronized int getPacketsIn() {
		int tmp = packetsIn;
		packetsIn = 0;
		return tmp;
	}

	public synchronized int getPacketsOut() {
		int tmp = packetsOut;
		packetsOut = 0;
		return tmp;
	}

	public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed)
			throws PacketRejectedException {
		// only count if the packet was processed so we 
		// don't count anything twice
		if (processed) {
			synchronized (this) {
				if (incoming) {
					++packetsIn;
				}
				else {
					++packetsOut;
				}
			}
		}
	}
}
