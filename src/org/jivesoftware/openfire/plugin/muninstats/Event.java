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

/**
 * This is the baseclass for all events.
 * Events are Objects which want to be executed at a specific time.
 * @see BackgroundThread
 * based on code by Martin Wuest
 */
public abstract class Event implements Comparable<Event> {
	private long eventID;
	protected long executionTime;

	/** 
	 * create an event.
	 * if creating an new event, use EventManager.getNewEventID() to generate an new unique eventID. 
	 */
	public Event(long eventID, long executionTime) {
		this.eventID = eventID;
		this.executionTime = executionTime;
	}

	/**
	 * get eventID
	 * @return eventID
	 */
	public long getEventID() {
		return eventID;
	}

	/**
	 * get executionTime
	 * @return executionTime
	 */
	public long getExecutionTime() {
		return executionTime;
	}

	/** 
	 * compares two events by executionTime. 
	 * if executionTime is equal, the objects are compared by eventID. 
	 */
	public int compareTo(Event other) {
		if (other == null) { 
			throw new NullPointerException(); 
		}
		if (this.executionTime < other.executionTime) { 
			return -1; 
		} else if (this.executionTime > other.executionTime) { 
			return  1; 
		} else if (this.eventID < other.eventID) { 
			return -1; 
		} else if (this.eventID > other.eventID) { 
			return  1; 
		} else { 
			return 0; 
		}
	}

	/** 
	 * two events are equal, if the eventID is equal. 
	 */
	public boolean equals(Object other) {
		if (other instanceof Event) {
			return this.eventID == ((Event)other).eventID;
		} else {
			return false;
		}
	}	
	
	/** 
	 * this method will be called by EventManager if executionTime is reached.
	 * You can set executionTime to a new value and return true, if you want 
	 * to be called again. Otherwise you have to return false. 
	 */
	public abstract boolean execute();
} 
