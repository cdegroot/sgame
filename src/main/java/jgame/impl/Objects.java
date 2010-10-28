package jgame.impl;

import java.util.Enumeration;
import java.util.Vector;

import jgame.JGObject;
import jgame.JGRectangle;

public class Objects {
	/** Note: objects lock is used to synchronise object updating between
	 * repaint thread and game thread.  The synchronize functions are found in
	 * Engine.doFrameAll and Canvas.paint */
	public SortedArray objects = new SortedArray(40);
	public SortedArray obj_to_remove = new SortedArray(40);
	public Vector obj_spec_to_remove = new Vector();
	public SortedArray obj_to_add = new SortedArray(40);
	
	/** indicates when engine is inside a parallel object update (moveObjects,
	 * check*Collision) */


	boolean in_parallel_upd=false;

	JGRectangle tmprect1 = new JGRectangle();
	JGRectangle tmprect2 = new JGRectangle();

	JGObject [] srcobj = new JGObject[50];
	JGObject [] dstobj = new JGObject[50];
	
	/** Add new object now.  Old object with the same name is replaced
	 * immediately, and its remove() method called.  
	 * @param engineLogic TODO
	 * @param obj TODO*/
	void addObject(EngineLogic engineLogic, JGObject obj) {
		int idx = objects.get(obj.getName());
		if (idx >= 0) {
			JGObject old_obj = (JGObject)objects.values[idx];
			// disable object so it doesn't call engine on removal
			old_obj.removeDone();
			// ensure any dispose stuff in the object is called
			old_obj.remove();
		}
		objects.put(obj.getName(),obj);
	}

	/** Actually remove objects in obj_to_remove. 
	 * @param engineLogic TODO*/
	void doRemoveList(EngineLogic engineLogic) {
		for (int i=0; i<obj_to_remove.size; i++) {
			((JGObject)obj_to_remove.values[i]).removeDone();
		}
		objects.remove(obj_to_remove);
		obj_to_remove.clear();
	}

	/** Add objects marked for addition. Protected.
	 * @param engineLogic TODO
	*/
	public void flushAddList(EngineLogic engineLogic) {
		// XXX we have to add one by one because we have to call the dispose
		// method of the objects that are replaced
		for (int i=0; i<obj_to_add.size; i++) {
			addObject(engineLogic, (JGObject)obj_to_add.values[i]);
		}
		obj_to_add.clear();
	}

	public boolean existsObject(EngineLogic engineLogic, String index) {
		return objects.get(index) >= 0;
	}

	public JGObject getObject(EngineLogic engineLogic, String index) {
		int idx = objects.get(index);
		if (idx<0) return null;
		return (JGObject)objects.values[idx];
	}

	public void moveObjects(EngineLogic engineLogic, JGEngineInterface eng, String prefix, int cidmask) {
		if (in_parallel_upd) throw new JGameError("Recursive call",true);
		in_parallel_upd=true;
		int firstidx=engineLogic.objects.getFirstObjectIndex(engineLogic, prefix);
		int lastidx=engineLogic.bg_color.getLastObjectIndex(engineLogic, prefix);
		for (int i=firstidx; i<lastidx; i++) {
			JGObject o = (JGObject) objects.values[i];
			if (cidmask!=0 && (o.colid&cidmask)==0) continue;
			// first, update suspend mode
			if (o.is_suspended) {
				if (o.resume_in_view
				&&o.isInView(engineLogic.offscreen_margin_x,engineLogic.offscreen_margin_y)) o.resume();
			} else {
				if (o.expiry==JGObject.suspend_off_view
				||  o.expiry==JGObject.suspend_off_view_expire_off_pf) {
					if (!o.isInView(engineLogic.offscreen_margin_x,engineLogic.offscreen_margin_y))
						o.suspend();
				}
			}
			// move object
			// we must ensure modulo is correct when object is suspended so
			// that it will unsuspend properly
			//o.moduloPos(); // is inlined below
			if (engineLogic.pf_wrapx) o.x = engineLogic.moduloXPos(o.x);
			if (engineLogic.pf_wrapy) o.y = engineLogic.moduloYPos(o.y);
			if (!o.is_suspended) {
				//o.moduloPos(); // is inlined below
				if (engineLogic.pf_wrapx) o.x = engineLogic.moduloXPos(o.x);
				if (engineLogic.pf_wrapy) o.y = engineLogic.moduloYPos(o.y);
				try {
					o.move();
				} catch (JGameError ex) {
					eng.exitEngine(eng.dbgExceptionToString(ex));
				} catch (Exception ex) {
					eng.dbgShowException(o.getName(),ex);
				}
				o.updateAnimation(engineLogic.gamespeed);
				o.x += o.xdir*o.xspeed*engineLogic.gamespeed;
				o.y += o.ydir*o.yspeed*engineLogic.gamespeed;
				//o.moduloPos(); // is inlined below
				if (engineLogic.pf_wrapx) o.x = engineLogic.moduloXPos(o.x);
				if (engineLogic.pf_wrapy) o.y = engineLogic.moduloYPos(o.y);
			}
			// check expiry; object should not expire when suspended
			if (!o.is_suspended) {
				int expiry = (int) o.expiry;
				if (expiry >= 0) {
					o.expiry -= engineLogic.gamespeed;
					if (o.expiry < 0) o.remove();
				} else {
					if (expiry==JGObject.expire_off_pf
					||  expiry==JGObject.suspend_off_view_expire_off_pf) {
						if (!o.isOnPF(engineLogic.offscreen_margin_x,engineLogic.offscreen_margin_y))
							o.remove();
					}
					if (expiry==JGObject.expire_off_view
					&& !o.isInView(engineLogic.offscreen_margin_x,engineLogic.offscreen_margin_y))
						o.remove();
				}
			}
		}
		flushRemoveList(engineLogic);
		in_parallel_upd=false;
	}

	public void markAddObject(EngineLogic engineLogic, JGObject obj) {
		obj_to_add.put(obj.getName(),obj);
	}

	/** Mark object for removal. 
	 * @param engineLogic TODO
	 * @param index TODO*/
	void markRemoveObject(EngineLogic engineLogic, String index) {
		int idx = objects.get(index);
		if (idx<0) return;
		obj_to_remove.put(index,(JGObject)objects.values[idx]);
	}

	/** Mark object for removal. 
	 * @param engineLogic TODO
	 * @param obj TODO*/
	void markRemoveObject(EngineLogic engineLogic, JGObject obj) {
		obj_to_remove.put(obj.getName(),obj);
	}

	/** Actually remove object now 
	 * @param engineLogic TODO
	 * @param obj TODO*/
	void doRemoveObject(EngineLogic engineLogic, JGObject obj) {
		obj.removeDone();
		objects.remove(obj.getName());
	}

	/** Mark all objects with given spec for removal. 
	 * @param engineLogic TODO
	 * @param prefix TODO
	 * @param cidmask TODO
	 * @param suspended_obj TODO*/
	void markRemoveObjects(EngineLogic engineLogic, String prefix, int cidmask, boolean suspended_obj) {
		obj_spec_to_remove.addElement(prefix);
		obj_spec_to_remove.addElement(new Integer(cidmask));
		obj_spec_to_remove.addElement(new Boolean(suspended_obj));
	}

	/** Actually remove objects with given spec, including those in obj_to_add
	 * list.  Uses obj_to_remove as a temp variable.  If anything is already
	 * in obj_to_remove, it is left there. If do_remove_list is true, the
	 * objects are removed and obj_to_remove is cleared.  Otherwise, the
	 * objects to remove are just added to obj_to_remove. 
	 * @param engineLogic TODO
	 * @param prefix TODO
	 * @param cidmask TODO
	 * @param suspended_obj TODO
	 * @param do_remove_list TODO*/
	void doRemoveObjects(EngineLogic engineLogic, String prefix, int cidmask, boolean suspended_obj, boolean do_remove_list) {
		int firstidx=engineLogic.objects.getFirstObjectIndex(engineLogic, prefix);
		int lastidx=engineLogic.bg_color.getLastObjectIndex(engineLogic, prefix);
		for (int i=firstidx; i<lastidx; i++) {
			JGObject o = (JGObject) objects.values[i];
			if (cidmask==0 || (o.colid&cidmask)!=0) {
				if (suspended_obj || !o.is_suspended) {
					obj_to_remove.put(objects.keys[i],o);
				}
			}
		}
		if (do_remove_list) doRemoveList(engineLogic);
		// if we enumerate backwards, we can remove elements inline without
		// consistency problems
		for (int i=obj_to_add.size-1; i>=0; i--) {
			JGObject o = (JGObject) obj_to_add.values[i];
			if (prefix==null || obj_to_add.keys[i].startsWith(prefix)) {
				if (cidmask==0 || (o.colid&cidmask)!=0) {
					if (suspended_obj || !o.is_suspended) {
						// Note: remove element inside element enumeration
						obj_to_add.remove(obj_to_add.keys[i]);
					}
				}
			}
		}
	}

	/** protected, remove objects marked for removal. 
	 * @param engineLogic TODO*/
	public void flushRemoveList(EngineLogic engineLogic) {
		//for (Enumeration e=obj_to_remove.elements(); e.hasMoreElements();) {
		//	String name = (String)e.nextElement();
		//	JGObject o = (JGObject)objects.get(name);
		//	if (o!=null) { // object might have been removed already
		//		doRemoveObject(o);
		//	}
		//}
		// add all query results from object specs to obj_to_remove
		// don't enumerate when no elements (which is about 90% of the time)
		if (obj_spec_to_remove.size()!=0) {
			for (Enumeration e=obj_spec_to_remove.elements();
			e.hasMoreElements(); ) {
				String prefix = (String) e.nextElement();
				int cid = ((Integer)e.nextElement()).intValue();
				boolean suspended_obj=((Boolean)e.nextElement()).booleanValue();
				doRemoveObjects(engineLogic, prefix,cid,suspended_obj,false);
			}
			obj_spec_to_remove.removeAllElements();
		}
		// remove everything in one go
		doRemoveList(engineLogic);
	}

	public void moveObjects(EngineLogic engineLogic, JGEngineInterface eng) {
		moveObjects(engineLogic, eng,null,0); 
	}

	public void checkCollision(EngineLogic engineLogic, JGEngineInterface eng, int srccid, int dstcid) {
		if (in_parallel_upd) throw new JGameError("Recursive call",true);
		in_parallel_upd=true;
		if (objects.size > srcobj.length) {
			// grow arrays to make objects fit
			srcobj = new JGObject[objects.size+50];
			dstobj = new JGObject[objects.size+50];
		}
		int srcsize = 0;
		int dstsize = 0;
		/* get all matching objects */
		JGRectangle sr = tmprect1;
		JGRectangle dr = tmprect2;
		for (int i=0; i<objects.size; i++) {
			JGObject o  = (JGObject)objects.values[i];
			if (o.is_suspended) continue;
			if (!o.getBBox(sr)) continue;
			if ((o.colid & srccid) != 0) {
				srcobj[srcsize++] = o;
			}
			if ((o.colid & dstcid) != 0) {
				dstobj[dstsize++] = o;
			}
		}
		/* check collision */
		for (int si=0; si<srcsize; si++) {
			JGObject srco = srcobj[si];
			if (!srco.getBBox(sr)) continue;
			for (int di=0; di<dstsize; di++) {
				JGObject dsto = dstobj[di];
				if (dsto == srco) continue;
				if (!dsto.getBBox(dr)) continue;
				if (sr.intersects(dr)) {
					try {
						dsto.hit(srco);
					} catch (JGameError ex) {
						eng.exitEngine(eng.dbgExceptionToString(ex));
					} catch (Exception ex) {
						eng.dbgShowException(dsto.getName(),ex);
					}
				}
			}
		}
		flushRemoveList(engineLogic);
		in_parallel_upd=false;
	}

	public int checkCollision(EngineLogic engineLogic, int cidmask, JGObject obj) {
		JGRectangle bbox = obj.getBBox();
		if (bbox==null) return 0;
		int retcid=0;
		JGRectangle obj_bbox = tmprect1;
		for (int i=0; i<objects.size; i++) {
			JGObject o  = (JGObject)objects.values[i];
			if (o==obj) continue;
			if (!o.is_suspended) {
				if (cidmask==0 || (o.colid&cidmask)!=0) {
					if (!o.getBBox(obj_bbox)) continue;
					if (bbox.intersects(obj_bbox)) {
						retcid |= o.colid;
					}
				}
			}
		}
		return retcid;
	}

	public void checkBGCollision(EngineLogic engineLogic, JGEngineInterface eng, int tilecid, int objcid) {
		if (in_parallel_upd) throw new JGameError("Recursive call",true);
		in_parallel_upd=true;
		if (objects.size > srcobj.length) {
			// grow arrays to make objects fit
			srcobj = new JGObject[objects.size+50];
		}
		int srcsize = 0;
		JGRectangle r = tmprect1;
		/* get all matching objects */
		for (int i=0; i<objects.size; i++) {
			JGObject o  = (JGObject)objects.values[i];
			if (o.is_suspended) continue;
			if (!o.getTileBBox(r)) continue;
			if ((o.colid & objcid) != 0) {
				srcobj[srcsize++] = o;
			}
		}
		/* check collision */
		JGRectangle tiler = tmprect2;
		for (int i=0; i<srcsize; i++) {
			JGObject o = srcobj[i];
			// tile bbox is always defined
			o.getTileBBox(r);
			// fast equivalent of cid=checkBGCollision(r)
			engineLogic.getTiles(tiler,r);
			int cid=engineLogic.getTileCid(tiler);
			if ((cid & tilecid) != 0) {
				try {
					o.hit_bg(cid);
					o.hit_bg(cid,tiler.x,tiler.y,tiler.width,tiler.height);
					// XXX this might be slow, check its performance
					for (int y=0; y<tiler.height; y++) {
						for (int x=0; x<tiler.width; x++) {
							int thiscid = engineLogic.getTileCid(tiler.x+x, tiler.y+y);
							if ( (thiscid&tilecid) != 0)
								o.hit_bg(thiscid, tiler.x+x, tiler.y+y);
						}
					}
				} catch (JGameError ex) {
					eng.exitEngine(eng.dbgExceptionToString(ex));
				} catch (Exception ex) {
					eng.dbgShowException(o.getName(),ex);
				}
			}
		}
		flushRemoveList(engineLogic);
		in_parallel_upd=false;
	}

	public Vector getObjects(EngineLogic engineLogic, String prefix, int cidmask, boolean suspended_obj, JGRectangle bbox) {
		Vector objects_v = new Vector(50,100);
		int nr_obj=0;
		JGRectangle obj_bbox = tmprect1;
		int firstidx=engineLogic.objects.getFirstObjectIndex(engineLogic, prefix);
		int lastidx=engineLogic.bg_color.getLastObjectIndex(engineLogic, prefix);
		for (int i=firstidx; i<lastidx; i++) {
			JGObject obj  = (JGObject)objects.values[i];
			if (cidmask==0 || (obj.colid&cidmask)!=0) {
				if (suspended_obj || !obj.is_suspended) {
					if (bbox!=null) {
						if (!obj.getBBox(obj_bbox)) continue;
						if (bbox.intersects(obj_bbox)) {
							objects_v.addElement(obj);
						}
					} else {
						objects_v.addElement(obj);
					}
				}
			}
		}
		return objects_v;
	}

	public void removeObject(EngineLogic engineLogic, JGObject obj) {
		if (in_parallel_upd) { // queue remove
			markRemoveObject(engineLogic, obj);
		} else { // do remove immediately
			doRemoveObject(engineLogic, obj);
		}
	}

	public void removeObjects(EngineLogic engineLogic, String prefix, int cidmask, boolean suspended_obj) {
		if (in_parallel_upd) {
			markRemoveObjects(engineLogic, prefix,cidmask,suspended_obj);
		} else {
			doRemoveObjects(engineLogic, prefix,cidmask,suspended_obj,true);
		}
	}

	public int countObjects(EngineLogic engineLogic, String prefix, int cidmask) {
		return engineLogic.objects.countObjects(engineLogic, prefix,cidmask,true);
	}

	public int countObjects(EngineLogic engineLogic, String prefix, int cidmask, boolean suspended_obj) {
		int nr_obj=0;
		int firstidx=engineLogic.objects.getFirstObjectIndex(engineLogic, prefix);
		int lastidx=engineLogic.bg_color.getLastObjectIndex(engineLogic, prefix);
		for (int i=firstidx; i<lastidx; i++) {
			JGObject obj = (JGObject) objects.values[i];
			if (cidmask==0 || (obj.colid&cidmask)!=0) {
				if (suspended_obj || !obj.is_suspended) {
					nr_obj++;
				}
			}
		}
		return nr_obj;
	}

	int getFirstObjectIndex(EngineLogic engineLogic, String prefix) {
		if (prefix==null) return 0;
		int firstidx = objects.get(prefix);
		if (firstidx<0) firstidx = -1-firstidx;
		return firstidx;
	}

	/** Do final update actions on objects after all frame updates finished.
	* Protected. 
	 * @param engineLogic TODO*/
	public void frameFinished(EngineLogic engineLogic) {
		for (int i=0; i<objects.size; i++) {
			((JGObject)objects.values[i]).frameFinished();
		}
	}


}