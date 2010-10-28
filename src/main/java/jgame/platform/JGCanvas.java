package jgame.platform;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Toolkit;

import javax.swing.ListCellRenderer;

import jgame.JGObject;
import jgame.impl.JGameError;

/** JGCanvas is internally used by JGEngine for updating and drawing objects
 * and tiles, and handling keyboard/mouse events. 
 */
class JGCanvas extends Canvas {

	/**
	 * 
	 */
	private final JGEngine jgEngine;

	// part of the "official" method of handling keyboard focus
	public boolean isFocusTraversable() { return true; }


	/*====== init stuff ======*/

	public JGCanvas (JGEngine jgEngine, int winwidth, int winheight) {
		super();
		this.jgEngine = jgEngine;
		setSize(winwidth,winheight);
	}


	/** Determines whether repaint will show the game graphics or do
	 * nothing. */
	boolean is_initialised=false;
	/** paint interface that is used when the canvas is not initialised (for
	 * displaying status info while starting up, loading files, etc. */
	private ListCellRenderer initpainter=null;
	String progress_message="Please wait, loading files .....";
	String author_message="JGame 3.4";
	/** for displaying progress bar, value between 0.0 - 1.0 */
	double progress_bar=0.0;

	void setInitialised() {
		is_initialised=true; 
		initpainter=null;
	}
	void setInitPainter(ListCellRenderer painter) {
		initpainter=painter;
	}
	void setProgressBar(double pos) {
		progress_bar=pos;
		if (!is_initialised && initpainter!=null) repaint(100);
	}
	void setProgressMessage(String msg) {
		progress_message=msg;
		if (!is_initialised && initpainter!=null) repaint(100);
	}
	void setAuthorMessage(String msg) {
		author_message=msg;
		if (!is_initialised && initpainter!=null) repaint(100);
	}

	/*====== paint ======*/

	/** Don't call directly. Use repaint().
	*/
	public void update(Graphics g) { paint(g); }

	/** Don't call directly. Use repaint().
	*/
	public void paint(Graphics g) { try {
		if (this.jgEngine.el.is_exited) {
			this.jgEngine.paintExitMessage(g);
			return;
		}
		if (!is_initialised) {
			if (initpainter!=null) {
				//if (buffer==null) {
				//	buffer=createImage(width,height);
				//}
				//if (incremental_repaint) {
					//initpainter.getListCellRendererComponent(null,
				//			buffer.getGraphics(),0,true,false);
				//	g.drawImage(buffer,0,0,this);
				//} else {
					initpainter.getListCellRendererComponent(null,
							getGraphics(),0,false,false);
				//}
			}
			return;
		}
		/* Each frame before the paint operation, we check if our possibly
		 * volatile bg and buffer are still ok.  If so, we don't re-validate
		 * every time, but leave them as persistent images until their
		 * contents are eventually destroyed.  Then we always recreate them
		 * even if their status is RESTORED.  Note that bg is indeed
		 * persistent, and things are incrementally drawn on it during the
		 * course of doFrames and paints.  If the buffer to be rendered to
		 * screen is invalid when we render it to screen, we give up for this
		 * frame and don't retry until the next frame. */
		if (this.jgEngine.background==null||!JREImage.isScratchImageValid(this.jgEngine.background)) {
			this.jgEngine.background=JREImage.createScratchImage(
				this.jgEngine.el.width+3*this.jgEngine.el.scaledtilex,this.jgEngine.el.height+3*this.jgEngine.el.scaledtiley );
			this.jgEngine.el.invalidateBGTiles();
		}
		if (this.jgEngine.buffer==null||!JREImage.isScratchImageValid(this.jgEngine.buffer)) {
			this.jgEngine.buffer=JREImage.createScratchImage(this.jgEngine.el.width,this.jgEngine.el.height);
		}
		if (this.jgEngine.buffer!=null && this.jgEngine.background!=null) {
			// block update thread
			synchronized (this.jgEngine.objects.objects) {
				// paint any part of bg which is not yet defined
				this.jgEngine.el.repaintBG(this.jgEngine);
				/* clear buffer */
				Graphics bufg = this.jgEngine.buffer.getGraphics();
				this.jgEngine.buf_gfx = bufg; // enable objects to draw on buffer gfx.
				//bufg.setColor(getBackground());
				//draw background to buffer
				//bufg.drawImage(background,-scaledtilex,-scaledtiley,this);
				int tilexshift=this.jgEngine.el.moduloFloor(this.jgEngine.el.tilexofs+1,this.jgEngine.el.viewnrtilesx+3);
				int tileyshift=this.jgEngine.el.moduloFloor(this.jgEngine.el.tileyofs+1,this.jgEngine.el.viewnrtilesy+3);
				int sx1 = tilexshift+1;
				int sy1 = tileyshift+1;
				int sx2 = this.jgEngine.el.viewnrtilesx+3;
				int sy2 = this.jgEngine.el.viewnrtilesy+3;
				if (sx2-sx1 > this.jgEngine.el.viewnrtilesx) sx2 = sx1 + this.jgEngine.el.viewnrtilesx;
				if (sy2-sy1 > this.jgEngine.el.viewnrtilesy) sy2 = sy1 + this.jgEngine.el.viewnrtilesy;
				int bufmidx = sx2-sx1;
				int bufmidy = sy2-sy1;
				this.jgEngine.copyBGToBuf(bufg,sx1,sy1, sx2,sy2, 0,0);
				sx1 = 0;
				sy1 = 0;
				sx2 = tilexshift-1;
				sy2 = tileyshift-1;
				this.jgEngine.copyBGToBuf(bufg,sx1,sy1, sx2,sy2, bufmidx,bufmidy);
				sx1 = 0;
				sy1 = tileyshift+1;
				sx2 = tilexshift-1;
				sy2 = this.jgEngine.el.viewnrtilesy+3;
				if (sy2-sy1 > this.jgEngine.el.viewnrtilesy) sy2 = sy1 + this.jgEngine.el.viewnrtilesy;
				this.jgEngine.copyBGToBuf(bufg,sx1,sy1, sx2,sy2, bufmidx,0);
				sx1 = tilexshift+1;
				sy1 = 0;
				sx2 = this.jgEngine.el.viewnrtilesx+3;
				sy2 = tileyshift-1;
				if (sx2-sx1 > this.jgEngine.el.viewnrtilesx) sx2 = sx1 + this.jgEngine.el.viewnrtilesx;
				this.jgEngine.copyBGToBuf(bufg,sx1,sy1, sx2,sy2, 0,bufmidy);
				//Color defaultcolour=g.getColor();
				///* sort objects */
				//ArrayList sortedkeys = new ArrayList(objects.keySet());
				//Collections.sort(sortedkeys);
				//for (Iterator i=sortedkeys.iterator(); i.hasNext(); ) {
				for (int i=0; i<this.jgEngine.objects.objects.size(); i++) {
					this.jgEngine.drawObject(bufg, (JGObject)this.jgEngine.objects.objects.valueAt(i));
				}
				this.jgEngine.buf_gfx = null; // we're finished with the object drawing
				/* draw status */
				if (bufg!=null) this.jgEngine.paintFrame(bufg);
				//}/*synchronized */
				/* draw buffer */
				g.drawImage(this.jgEngine.buffer,0,0,this);
				//g.setColor(defaultcolour);
			}
			// don't block the update thread while waiting for sync
			Toolkit.getDefaultToolkit().sync();
		}
	} catch (JGameError e) {
		this.jgEngine.exitEngine("Error during paint:\n"
				+this.jgEngine.dbgExceptionToString(e) );
	} }


	public void dbgShowFullStackTrace(JGEngine jgEngine, boolean enabled) {
		if (enabled) jgEngine.debugFrame.debugflags |=  DebugFrame.FULLSTACKTRACE_DEBUG;
		else         jgEngine.debugFrame.debugflags &= ~DebugFrame.FULLSTACKTRACE_DEBUG;
	}

}