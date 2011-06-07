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

import org.jivesoftware.openfire.plugin.MuninStats;
import org.jivesoftware.openfire.plugin.muninstats.BackgroundThread;
import org.jivesoftware.openfire.plugin.muninstats.Event;

import org.slf4j.Logger;
import java.util.*;

/**
 * This class manages the background-thread
 * based on code by Martin Wuest
 */
public class BackgroundThread implements Runnable {
	private MuninStats plugin;
	private Thread thread;
	private boolean running;
	public static Logger log;

	private long incrementalEventID;
	private PriorityQueue<Event> eventQueue;

	private static BackgroundThread instance = new BackgroundThread();

	private BackgroundThread() {
		running = false;
		thread = null;
		eventQueue = new PriorityQueue<Event>();
	}

	public void init(MuninStats plugin) {
		this.plugin = plugin;
	}
	
	/** 
	 * return singleton instance of the BackgroundThread
	 * @return singleton instance
	 */
	public static BackgroundThread getInstance() {
		return instance;
	}
	
	/**
	 * get a new event id (just the next increment)
	 * @return event id
	 */
	public synchronized long getNewEventID() {
		return incrementalEventID++;
	}

	/** 
	 * start the internal thread 
	 */
	public void start() {
		if (running) {
			stop();
		}
		running = true;
		thread = new Thread(this);
		thread.start();
	}

	/** 
	 * stop the internal thread 
	 */
	public void stop() {
		running = false;
		if (thread == null) {
			return;
		}
		try {
			thread.join();
		} catch (InterruptedException e) {
			log.error("Error joining internal thread\n" + e.toString());
		}
		thread = null;
	}

	/** 
	 * check whether the internal thread is running
	 * @return true if the thread is running, otherwise false
	 */
	public boolean isRunning() {
		return running;
	}

	/** 
	 * process the eventloop 
	 */
	public void run() {
		try {
			thread.setPriority(Thread.MIN_PRIORITY);
			while (running) {
				Event event;
				synchronized (this) {
					event = eventQueue.peek();
					if ((event != null) && 
						(System.currentTimeMillis() > event.getExecutionTime())) {
						// remove event from queue
						eventQueue.poll();
					} else {
						event = null;
					}
				}

				if (event != null) {
					if (event.execute()) {
						// event want's to be called again
						synchronized (this) {
							eventQueue.add(event);
						}
					}
					Thread.yield();
				} else {
					Thread.sleep(500);
				}
			}
		} catch (Exception e) {
			log.error("Error running BackgroundThread\n" + e.toString());
		}
	}

	/**
	 * add an event to the eventloop
	 * @param event to be added
	 */
	public void addEvent(Event event) {
		if (event == null) {
			throw new NullPointerException();
		}
		synchronized (this) {
			eventQueue.add(event);
		}
	}

	/**
	 * remove an event from the eventloop
	 * @param event to be removed
	 */
	public void removeEvent(Event event) {
		if (event == null) {
			throw new NullPointerException();
		}
		synchronized (this) {
			eventQueue.remove(event);
		}
	}
}