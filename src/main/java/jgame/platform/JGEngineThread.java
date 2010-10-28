package jgame.platform;

import jgame.impl.JGameError;

/** Engine thread, executing game action. */
class JGEngineThread implements Runnable {
	/**
	 * 
	 */
	private final JGEngine jgEngine;
	private long target_time=0; /* time at which next frame should start */
	private int frames_skipped=0;
	public JGEngineThread (JGEngine jgEngine) {
		this.jgEngine = jgEngine;}
	public void run() { try {
		try {
			this.jgEngine.initGame();
		} catch (Exception e) {
			e.printStackTrace();
			throw new JGameError("Exception during initGame(): "+e);
		}
		this.jgEngine.canvas.setInitialised();
		target_time = System.currentTimeMillis()+(long)(1000.0/this.jgEngine.el.fps);
		while (!this.jgEngine.el.is_exited) {
			if ((this.jgEngine.debugFrame.debugflags&DebugFrame.MSGSINPF_DEBUG)!=0) this.jgEngine.debugFrame.refreshDbgFrameLogs();
			long cur_time = System.currentTimeMillis();
			if (!this.jgEngine.running) {
				// wait in portions of 1/2 sec until running is set;
				// reset target time
				Thread.sleep(500);
				target_time = cur_time+(long)(1000.0/this.jgEngine.el.fps);
			} else if (cur_time < target_time+(long)(500.0/this.jgEngine.el.fps)) {
				// we lag behind less than 1/2 frame -> do full frame.
				// This empirically produces the smoothest animation
				synchronized (this.jgEngine.objects.objects) {
					this.jgEngine.doFrameAll();
					this.jgEngine.el.updateViewOffset();
				}
				this.jgEngine.canvas.repaint();
				frames_skipped=0;
				if (cur_time+3 < target_time) {
					//we even have some time left -> sleep it away
					Thread.sleep(target_time-cur_time);
				} else {
					// we don't, just yield to give input handler and
					// painter some time
					Thread.yield();
				}
				target_time += (1000.0/this.jgEngine.el.fps);
			//} else if (cur_time >
			//target_time + (long)(1000.0*el.maxframeskip/el.fps)) {
			//	// we lag behind more than the max # frames ->
			//	// draw full frame and reset target time
			//	synchronized (objects) {
			//		doFrameAll();
			//		el.updateViewOffset();
			//	}
			//	canvas.repaint();
			//	frames_skipped=0;
			//	// yield to give input handler + painter some time
			//	Thread.yield();
			//	target_time=cur_time + (long)(1000.0/el.fps);
			} else {
				// we lag behind a little -> frame skip
				synchronized (this.jgEngine.objects.objects) {
					this.jgEngine.doFrameAll();
					this.jgEngine.el.updateViewOffset();
				}
				// if we skip too many frames in succession, draw a frame
				if ((++frames_skipped) > this.jgEngine.el.maxframeskip) {
					this.jgEngine.canvas.repaint();
					frames_skipped=0;
					target_time=cur_time + (long)(1000.0/this.jgEngine.el.fps);
				} else {
					target_time += (long)(1000.0/this.jgEngine.el.fps);
				}
				// yield to give input handler some time
				Thread.yield();
			}
		}
	} catch (InterruptedException e) {
		/* exit thread when interrupted */
		System.out.println("JGame thread exited.");
	} catch (Exception e) {
		this.jgEngine.dbgShowException("MAIN",e);
	} catch (JGameError e) {
		this.jgEngine.exitEngine("Error in main:\n"+this.jgEngine.dbgExceptionToString(e));
	} }
}