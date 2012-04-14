package org.jigsawrenaissance;

public class Constants {
	/** App name for log messages. */
	static final String TAG = "PollService";
	/** How long to wait (in milliseconds) between attempts to fetch commands. */
	public static final int POLL_TIME = 10*1000;  // ten seconds
	/** Should we get commands? */
	public static final boolean GET_COMMAND = true;
	/** Should we upload data? */
	public static final boolean SEND_DATA = false;
	/** Staging directory within /sdcard. @ToDo: Replace with ...? */
	public static final String STAGING_DIRECTORY = "poll_staging";
	/** Size of buffer for file I/O. */
	public static final int BUFSIZ = 1024;
}
