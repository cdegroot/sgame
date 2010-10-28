package jgame.impl;

import jgame.JGColor;
import jgame.JGFont;

public interface DebugAndErrorHandlerInterface {

	/** Show bounding boxes around the objects: the image bounding box
	 * (getBBox) , the tile span (getTiles), and the center tiles
	 * (getCenterTiles).  */
	public abstract void dbgShowBoundingBox(boolean enabled);

	/** Show the game state in the bottom right corner of the screen. The
	 * message font and foreground colour are used to draw the text. */
	public abstract void dbgShowGameState(boolean enabled);

	/** Indicates whether to show full exception stack traces or just the
	 * first lines.  Default is false.  */
	public abstract void dbgShowFullStackTrace(boolean enabled);

	/** Output messages on playfield instead of console. Default is true.
	 * Messages printed by an object are displayed close to that object.
	 * Messages printed by the main program are shown at the bottom of the
	 * screen.  The debug message font is used to display the messages.
	 * <p>A message that is generated in this frame is shown in the foreground
	 * colour at the appropriate source.  If the source did not generate a
	 * message, the last printed message remains visible, and is shown in
	 * debug colour 1.  If an object prints a message, and then dies, the
	 * message will remain for a period of time after the object is gone.
	 * These messages are shown in debug colour 2.
	 */
	public abstract void dbgShowMessagesInPf(boolean enabled);

	/** Set the number of frames a debug message of a removed object should
	 * remain on the playfield. */
	public abstract void dbgSetMessageExpiry(int ticks);

	/** Set the font for displaying debug messages. */
	public abstract void dbgSetMessageFont(JGFont font);

	/** Set debug color 1, used for printing debug information. */
	public abstract void dbgSetDebugColor1(JGColor col);

	/** Set debug color 2, used for printing debug information. */
	public abstract void dbgSetDebugColor2(JGColor col);

	/** Print a debug message, with the main program being the source. */
	public abstract void dbgPrint(String msg);

	/** Print a debug message from a specific source, which is either the main
	 * program or a JGObject.
	 * @param source  may be object ID or "MAIN" for the main program. */
	public abstract void dbgPrint(String source, String msg);

	/** Print the relevant information of an exception as a debug message.
	 * @param source  may be object ID or "MAIN" for the main program. */
	public abstract void dbgShowException(String source, Throwable e);

	/**Convert the relevant information of an exception to a multiline String.*/
	public abstract String dbgExceptionToString(Throwable e);

	/** Exit, optionally reporting an exit message.  The exit message can be
	 * used to report fatal errors.  In case of an application or midlet, the
	 * program exits.  In case of an applet, destroy is called, and the exit
	 * message is displayed on the playfield.
	 * @param msg an exit message, null means none */
	public abstract void exitEngine(String msg);

}