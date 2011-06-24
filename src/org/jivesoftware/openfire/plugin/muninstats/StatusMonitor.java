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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * log status information this class collects information on registered users,
 * users online, ressources online, packets in and out and memory information.
 * the information is being written to a status logfile every INTERVAL_TIME
 */
public class StatusMonitor {
	private String statuslogfile;
	private int updateinterval;
	
	private static final String LEGEND_UPDATE = "last_update";
	private static final String LEGEND_REGISTERED = "users_registered";
	private static final String LEGEND_ONLINE = "users_online";
	private static final String LEGEND_USESSIONS = "ressources_online";
	private static final String LEGEND_SSESSIONS = "server_sessions";
	private static final String LEGEND_INCOMING = "packets_in";
	private static final String LEGEND_OUTGOING = "packets_out";
	private static final String LEGEND_MEMMAX = "memory_max";
	private static final String LEGEND_MEMTOTAL = "memory_total";
	private static final String LEGEND_MEMUSED = "memory_used";
	private static final String LEGEND_MEMFREE = "memory_free";

	private EventLogStat event;
	private UserManager userManager;
	private SessionManager sessionManager;
	private PacketMonitor packetMonitor;
	private static final Logger log = LoggerFactory.getLogger(StatusMonitor.class);

	private long lastUpdate;
	private double registeredUsers;
	private double activeUsers;
	private double activeUserSessions;
	private double activeServerSessions;
	private double maxMemory;
	private double totalMemory;
	private double freeMemory;
	private double usedMemory;

	private static StatusMonitor instance = new StatusMonitor();

	/**
	 * @return singleton instance
	 */
	public static StatusMonitor getInstance() {
		return instance;
	}

	/**
	 */
	private StatusMonitor() {
	}

	/**
	 * initialize and start status logger
	 */
	public void init(String statuslogfile, int updateinterval) {
		this.statuslogfile = statuslogfile;
		this.updateinterval = updateinterval;
		
		userManager = UserManager.getInstance();
		sessionManager = SessionManager.getInstance();
		packetMonitor = PacketMonitor.getInstance();

		lastUpdate = 0;
		registeredUsers = 0;
		activeUsers = 0;
		activeUserSessions = 0;
		activeServerSessions = 0;
		maxMemory = 0;
		totalMemory = 0;
		freeMemory = 0;
		usedMemory = 0;

		updateUserStats();
		updateMemStats();

		BackgroundThread backgroundThread = BackgroundThread.getInstance();
		long eventID = backgroundThread.getNewEventID();
		event = new EventLogStat(eventID);
		backgroundThread.addEvent(event);
	}

	/**
	 * destroy status logger
	 */
	public void destroy() {
		BackgroundThread.getInstance().removeEvent(event);
	}

	/**
	 * update memory information
	 */
	public void updateMemStats() {
		Runtime runtime = Runtime.getRuntime();
		maxMemory = (double) runtime.maxMemory() / (1024 * 1024);
		totalMemory = (double) runtime.totalMemory() / (1024 * 1024);
		freeMemory = (double) runtime.freeMemory() / (1024 * 1024);
		usedMemory = totalMemory - freeMemory;
	}

	/**
	 * update user information
	 */
	private void updateUserStats() {
		// count active users and sessions
		Collection<ClientSession> sessions = sessionManager.getSessions();
		Set<String> users = new HashSet<String>(sessions.size());
		activeUserSessions = 0;
		for (ClientSession session : sessions) {
			if (session.getPresence().isAvailable()) {
				++activeUserSessions;
				users.add(session.getAddress().toBareJID());
			}
		}
		activeUsers = users.size();
		registeredUsers = userManager.getUserCount();
	}

	/**
	 * Tracks the number of Server To Server connections taking place in the
	 * server at anyone time. This includes both incoming and outgoing
	 * connections.
	 */
	private void updateServerToServerStats() {
		activeServerSessions = 
			SessionManager.getInstance().getIncomingServers().size() +
			SessionManager.getInstance().getOutgoingServers().size();
	}

	/**
	 * log to status logfile
	 */
	private void log() {
		try {
			FileOutputStream fstream = new FileOutputStream(this.statuslogfile);
			PrintStream out = new PrintStream(fstream);
			out.format("%s %d\n", LEGEND_UPDATE, lastUpdate);
			out.format("%s %d\n", LEGEND_REGISTERED, (int) registeredUsers);
			out.format("%s %d\n", LEGEND_ONLINE, (int) activeUsers);
			out.format("%s %d\n", LEGEND_USESSIONS, (int) activeUserSessions);
			out.format("%s %d\n", LEGEND_SSESSIONS, (int) activeServerSessions);
			out.format("%s %d\n", LEGEND_INCOMING,
					(int) packetMonitor.getPacketsIn());
			out.format("%s %d\n", LEGEND_OUTGOING,
					(int) packetMonitor.getPacketsOut());
			out.format(Locale.ROOT, "%s %.3f\n", LEGEND_MEMMAX, maxMemory);
			out.format(Locale.ROOT, "%s %.3f\n", LEGEND_MEMTOTAL, totalMemory);
			out.format(Locale.ROOT, "%s %.3f\n", LEGEND_MEMUSED, usedMemory);
			out.format(Locale.ROOT, "%s %.3f\n", LEGEND_MEMFREE, freeMemory);
			out.close();
		} catch (IOException e) {
			log.error("Plugin MuninStats: Error writing to statuslogfile\n" + e.toString());
		}
	}

	/**
	 * loop the status logger in background
	 */
	private class EventLogStat extends Event {
		public EventLogStat(long eventID) {
			super(eventID, 0);
		}

		public boolean execute() {
			updateUserStats();
			updateMemStats();
			updateServerToServerStats();
			lastUpdate = System.currentTimeMillis() / 1000L;
			log();

			executionTime = System.currentTimeMillis() + updateinterval * 1000;
			return true;
		}
	}
}