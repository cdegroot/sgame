package jgame.platform;

import java.awt.Font;
import java.awt.Graphics;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import jgame.JGColor;
import jgame.JGFont;
import jgame.JGObject;
import jgame.JGPoint;
import jgame.JGRectangle;

public class DebugFrame {
	
	static final int BBOX_DEBUG = 1;
	static final int GAMESTATE_DEBUG = 2;
	static final int FULLSTACKTRACE_DEBUG = 4;
	static final int MSGSINPF_DEBUG= 8;

	static int dbgframelog_expiry=80;

	public int debugflags = MSGSINPF_DEBUG;
	public JGFont debugmessage_font = new JGFont("Arial", 0, 12);
	public JGColor debug_auxcolor1 = JGColor.green;
	public JGColor debug_auxcolor2 = JGColor.magenta;
	public Hashtable dbgframelogs = new Hashtable();
	public Hashtable dbgnewframelogs = new Hashtable();
	/** flags indicating messages are new */
	public Hashtable dbgframelogs_new = new Hashtable();
	/** objects that dbgframes correspond to (JGObject) */
	public Hashtable dbgframelogs_obj = new Hashtable();
	/** time that removed objects are dead (Integer) */
	public Hashtable dbgframelogs_dead = new Hashtable();

	public String dbgExceptionToString(Throwable e) {
		ByteArrayOutputStream st = new ByteArrayOutputStream();
		e.printStackTrace(new PrintStream(st));
		if ((debugflags&DebugFrame.FULLSTACKTRACE_DEBUG)!=0) {
			return st.toString();
		} else {
			StringTokenizer toker = new StringTokenizer(st.toString(),"\n");
			String ret = toker.nextToken()+"\n";
			ret       += toker.nextToken()+"\n";
			if (toker.hasMoreTokens())
				ret   += toker.nextToken();
			return ret;
		}
	}
	
	public void dbgShowException(JGEngine jgEngine, String source, Throwable e) {
		ByteArrayOutputStream st = new ByteArrayOutputStream();
		e.printStackTrace(new PrintStream(st));
		if ((debugflags&DebugFrame.FULLSTACKTRACE_DEBUG)!=0) {
			jgEngine.dbgPrint(source,st.toString());
		} else {
			StringTokenizer toker = new StringTokenizer(st.toString(),"\n");
			jgEngine.dbgPrint(source,toker.nextToken());
			jgEngine.dbgPrint(source,toker.nextToken());
			if (toker.hasMoreTokens())
				jgEngine.dbgPrint(source,toker.nextToken());
		}
	}

	public void dbgPrint(JGEngine jgEngine, String source, String msg) {
		if ((debugflags&DebugFrame.MSGSINPF_DEBUG)!=0) {
			Vector log = (Vector)dbgnewframelogs.get(source);
			if (log==null) log = new Vector(5,15);
			if (log.size() < 19) {
				log.add(msg);
			} else if (log.size() == 19) {
				log.add("<messages truncated>");
			}
			dbgnewframelogs.put(source,log);
			JGObject obj = jgEngine.objects.getObject(source);
			if (obj!=null) { // store source object
				dbgframelogs_obj.put(source,obj);
				dbgframelogs_dead.remove(source);
			}
		} else {
			System.out.println(source+": "+msg);
		}
	}
	
	public void dbgPrint(JGEngine jgEngine, String msg) { dbgPrint(jgEngine, "MAIN",msg); }
	public void dbgSetDebugColor2(JGColor col) { debug_auxcolor2=col; }
	public void dbgSetDebugColor1(JGColor col) { debug_auxcolor1=col; }
	/** paint the messages 
	 * @param jgEngine TODO
	 * @param g TODO*/
	void paintDbgFrameLogs(JGEngine jgEngine, Graphics g) {
		// we use an absolute font size
		Font dbgfont = new Font(debugmessage_font.name,debugmessage_font.style,
			(int)debugmessage_font.size);
		g.setFont(dbgfont);
		for (Enumeration e=dbgframelogs.keys(); e.hasMoreElements();) {
			String source = (String) e.nextElement();
			Vector log = (Vector) dbgframelogs.get(source);
			if (dbgframelogs_new.containsKey(source)) {
				// new message
				jgEngine.setColor(g,jgEngine.el.fg_color);
			} else {
				// message from previous frame
				jgEngine.setColor(g,debug_auxcolor1);
			}
			JGObject obj = jgEngine.objects.getObject(source);
			if (obj==null) {
				// retrieve dead object
				obj = (JGObject) dbgframelogs_obj.get(source);
				// message from deleted object
				jgEngine.setColor(g,debug_auxcolor2);
				if (obj!=null) {
					// tick dead timer
					int deadtime=0;
					if (dbgframelogs_dead.containsKey(source)) 
						deadtime = ((Integer)dbgframelogs_dead.get(source))
							.intValue();
					if (deadtime < dbgframelog_expiry) {
						dbgframelogs_dead.put(source,new Integer(deadtime+1));
					} else {
						dbgframelogs_obj.remove(source);
						dbgframelogs_dead.remove(source);
					}
				}
			}
			int lineheight = debugmessage_font.getSize()+1;
			if (obj!=null) {
				JGPoint scaled = jgEngine.el.scalePos(obj.x-jgEngine.el.xofs,
					obj.y-jgEngine.el.yofs + lineheight/3,false);
				scaled.y -= lineheight*log.size();
				for (Enumeration f=log.elements(); f.hasMoreElements(); ) {
					g.drawString((String)f.nextElement(),scaled.x,scaled.y);
					scaled.y += lineheight;
				}
			} else {
				if (!source.equals("MAIN")) {
					dbgframelogs.remove(source);
				} else {
					if (dbgframelogs_new.containsKey(source)) {
						// new message
						jgEngine.setColor(g,jgEngine.el.fg_color);
					} else {
						// message from previous frame
						jgEngine.setColor(debug_auxcolor1);
					}
					int ypos = jgEngine.el.scaleYPos(jgEngine.el.viewHeight(),false);
					ypos -= lineheight*log.size();
					for (Enumeration f=log.elements(); f.hasMoreElements(); ) {
						g.drawString((String)f.nextElement(),0,ypos);
						ypos += lineheight;
					}
				}
			}
		}
	}
	public void dbgShowBoundingBox(boolean enabled) {
		if (enabled) debugflags |=  DebugFrame.BBOX_DEBUG;
		else         debugflags &= ~DebugFrame.BBOX_DEBUG;
	}
	/** Refresh message logs for this frame. */
	void refreshDbgFrameLogs() {
		dbgframelogs_new = new Hashtable(); // clear "new" flag
		for (Enumeration e=dbgnewframelogs.keys(); e.hasMoreElements();) {
			String source = (String) e.nextElement();
			Object log = dbgnewframelogs.get(source);
			dbgframelogs.put(source,log);
			dbgframelogs_new.put(source,"yes");
		}
		dbgnewframelogs = new Hashtable();
	}
	
	public void dbgShowGameState(boolean enabled) {
		if (enabled) debugflags |=  DebugFrame.GAMESTATE_DEBUG;
		else         debugflags &= ~DebugFrame.GAMESTATE_DEBUG;
	}
	
	public void dbgShowMessagesInPf(boolean enabled) {
		if (enabled) debugflags |=  DebugFrame.MSGSINPF_DEBUG;
		else         debugflags &= ~DebugFrame.MSGSINPF_DEBUG;
	}
	public void dbgSetMessageExpiry(int ticks) {
		dbgframelog_expiry = ticks;
	}
	
	public void dbgSetMessageFont(JGFont font) { 
		debugmessage_font=font; 
	}

	void dbgDrawObjectBoundingBox(JGEngine jgEngine, Graphics g, JGObject o) {
		jgEngine.setColor(g,jgEngine.el.fg_color);
		JGRectangle bbox = o.getBBox();
		if (bbox!=null) { // bounding box defined
			//bbox.x -= xofs;
			//bbox.y -= yofs;
			bbox = jgEngine.el.scalePos(bbox,true);
			g.drawRect(bbox.x,bbox.y,bbox.width,bbox.height);
		}
		bbox = o.getTileBBox();
		if (bbox!=null) { // tile bounding box defined
			//bbox.x -= xofs;
			//bbox.y -= yofs;
			bbox = jgEngine.el.scalePos(bbox,true);
			g.drawRect(bbox.x,bbox.y,bbox.width,bbox.height);
			jgEngine.setColor(g,debug_auxcolor1);
			bbox = o.getTileBBox();
			bbox = jgEngine.getTiles(bbox);
			bbox.x *= jgEngine.el.tilex;
			bbox.y *= jgEngine.el.tiley;
			//bbox.x -= xofs;
			//bbox.y -= yofs;
			bbox.width *= jgEngine.el.tilex;
			bbox.height *= jgEngine.el.tiley;
			bbox = jgEngine.el.scalePos(bbox,true);
			g.drawRect(bbox.x,bbox.y,bbox.width,bbox.height);
			jgEngine.setColor(g,debug_auxcolor2);
			bbox = o.getCenterTiles();
			bbox.x *= jgEngine.el.tilex;
			bbox.y *= jgEngine.el.tiley;
			//bbox.x -= xofs;
			//bbox.y -= yofs;
			bbox.width *= jgEngine.el.tilex;
			bbox.height *= jgEngine.el.tiley;
			bbox = jgEngine.el.scalePos(bbox,true);
			g.drawRect(bbox.x+2,bbox.y+2,bbox.width-4,bbox.height-4);
		}
	}

}