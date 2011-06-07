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
package org.jivesoftware.openfire.plugin;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.plugin.muninstats.BackgroundThread;
import org.jivesoftware.openfire.plugin.muninstats.StatusMonitor;
import org.jivesoftware.openfire.plugin.muninstats.PacketMonitor;

import org.slf4j.Logger;
import java.io.File;

/**
 * An Openfire Monitoring plugin.
 * This plugin writes statistics data to a plain textfile from where it can 
 * be parsed by Munin or any other external monitoring tool. Statistics that 
 * will be exported include:
 * 		- users (registered users, online users, online ressources)
 * 		- throughput (incoming and outgoing packets)
 * 		- memory (available, free and used memory)
 * 		- server2server connections
 */
public class MuninStats implements Plugin {	
	private StatusMonitor statusMonitor;
	private BackgroundThread backgroundThread;
	private PacketMonitor packetMonitor;
	private static Logger log;
	
	public MuninStats() {
	}

	/**
	 * initialize the plugin
	 * start the background thread and initialize the monitors
	 */
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		backgroundThread = BackgroundThread.getInstance();
		backgroundThread.init(this);
		backgroundThread.start();
		statusMonitor = StatusMonitor.getInstance();
		statusMonitor.init();
		packetMonitor = PacketMonitor.getInstance();
		packetMonitor.init(this);

		log.info("Plugin MuninStats initialized");
	}

	/**
	 * destroy the plugin
	 */
	public void destroyPlugin() {
		packetMonitor.destroy();
		statusMonitor.destroy();
		backgroundThread.stop();
		log.info ("Plugin MuninStats destroyed");
	}
}