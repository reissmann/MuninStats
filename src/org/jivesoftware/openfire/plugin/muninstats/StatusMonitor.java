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

import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.plugin.muninstats.BackgroundThread;
import org.jivesoftware.openfire.plugin.muninstats.Event;
import org.jivesoftware.openfire.plugin.muninstats.StatusMonitor;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.user.UserManager;

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * log status information
 * this class collects information on registered users, users online, 
 * ressources online, packets in and out and memory information. the 
 * information is being written to a status logfile every INTERVAL_TIME
 */
public class StatusMonitor {
	private static final long INTERVAL_TIME = 5 * 60;
	private static final String STATUS_LOGFILE = "/opt/openfire/resources/statistics/status.log";
	private static final String LEGEND_UPDATE = "last_update";
	private static final String LEGEND_REGISTERED = "users_registered";
	private static final String LEGEND_ONLINE = "users_online";
	private static final String LEGEND_USESSIONS = "ressources_online";
	private static final String LEGEND_SSESSIONS = "server_sessions";
	private static final String LEGEND_INCOMING = "packets_in";
	private static final String LEGEND_OUTGOING = "packets_out";
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
	private double totalMemory;
	private double freeMemory;
	private double usedMemory;

	private static StatusMonitor instance = new StatusMonitor();

	/** 
	 * @return singleton instance 
	 */
	public static StatusMonitor getInstance () {
		return instance;
	}

	/**
	 * initialize logging
	 */
	private StatusMonitor () {
	}

	/**
	 * initialize and start status logger
	 */
	public void init () {
		userManager = UserManager.getInstance ();
		sessionManager = SessionManager.getInstance ();
		packetMonitor = PacketMonitor.getInstance ();

		lastUpdate = 0;
		registeredUsers = 0;
		activeUsers = 0;
		activeUserSessions = 0;
		activeServerSessions = 0;
		totalMemory = 0;
		freeMemory = 0;
		usedMemory = 0;

		updateUserStats ();
		updateMemStats ();

		BackgroundThread backgroundThread = BackgroundThread.getInstance ();
		long eventID = backgroundThread.getNewEventID ();
		event = new EventLogStat (eventID);
		backgroundThread.addEvent (event);
	}

	/**
	 * destroy status logger
	 */
	public void destroy () {
		BackgroundThread.getInstance ().removeEvent (event);
	}

	/**
	 * update memory information
	 */
	public void updateMemStats () {
		Runtime runtime = Runtime.getRuntime();
	    totalMemory = (double) runtime.totalMemory () / (1024 * 1024);
	    freeMemory = (double) runtime.freeMemory () / (1024 * 1024);
	    usedMemory = totalMemory - freeMemory;
	}
	
	/**
	 * update user information
	 */
	private void updateUserStats () {
		// count active users and sessions
		Collection<ClientSession> sessions = sessionManager.getSessions ();
		Set<String> users = new HashSet<String> (sessions.size ());
		activeUserSessions = 0;
		for (ClientSession session : sessions) {
			if (session.getPresence ().isAvailable ()) {
				++activeUserSessions;
				users.add (session.getAddress ().toBareJID ());
			}
		}
		activeUsers = users.size();
		registeredUsers = userManager.getUserCount();
	}

    /**
     * Tracks the number of Server To Server connections taking place in the server at anyone time.
     * This includes both incoming and outgoing connections.
     */
    private void updateServerToServerStats () {
    	activeServerSessions = 
    		SessionManager.getInstance().getIncomingServers().size() + 
    		SessionManager.getInstance().getOutgoingServers().size();
    }
    
	/**
	 * log to status logfile
	 */
	private void log() {
		DecimalFormat df = new DecimalFormat("#.###");
		try {
			FileWriter fstream = new FileWriter (STATUS_LOGFILE);
			BufferedWriter out = new BufferedWriter (fstream);
			out.write (LEGEND_UPDATE + " " + (int) lastUpdate + "\n");
			out.write (LEGEND_REGISTERED + " " + (int) registeredUsers + "\n");
			out.write (LEGEND_ONLINE + " " + (int) activeUsers + "\n");
			out.write (LEGEND_USESSIONS + " " + (int) activeUserSessions + "\n");
			out.write (LEGEND_SSESSIONS + " " + (int) activeServerSessions + "\n");
			out.write (LEGEND_INCOMING + " " + (int) packetMonitor.getPacketsIn() + "\n");
			out.write (LEGEND_OUTGOING + " " + (int) packetMonitor.getPacketsOut() + "\n");
			out.write (LEGEND_MEMTOTAL + " " + df.format (totalMemory) + "\n");
			out.write (LEGEND_MEMUSED + " " + df.format (usedMemory) + "\n");
			out.write (LEGEND_MEMFREE + " " + df.format (freeMemory) + "\n");
			out.close ();
		} catch (IOException e) {
			log.error("Error writing to status logfile\n" + e.toString());
		}
	}

	/**
	 * loop the status logger in background
	 */
	private class EventLogStat extends Event {
		public EventLogStat(long eventID) {
			super(eventID, 0);
		}

		public boolean execute () {
			updateUserStats();
			updateMemStats();
			updateServerToServerStats();
			lastUpdate = (new Date()).getTime() / 1000L;
			log();

			executionTime = System.currentTimeMillis() + INTERVAL_TIME * 1000;
			return true;
		}
	}
} 