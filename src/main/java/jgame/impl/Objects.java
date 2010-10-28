package jgame.impl;

import java.util.Vector;

import jgame.JGObject;
import jgame.JGRectangle;

public class Objects {
	/** Note: objects lock is used to synchronise object updating between
	 * repaint thread and game thread.  The synchronize functions are found in
	 * Engine.doFrameAll and Canvas.paint */
	public SortedArray<JGObject> objects = new SortedArray<JGObject>(40); // needed to lock in JGEngine
	
	private SortedArray<JGObject> objectsToRemove = new SortedArray<JGObject>(40);
	private SortedArray<JGObject> objectsToAdd = new SortedArray<JGObject>(40);
	
	static class ObjectSpec {
		String prefix;
		int cidmask;
		boolean suspended_obj;

		ObjectSpec(String prefix, int cidmask, boolean suspended_obj) {
			this.prefix = prefix;
			this.cidmask = cidmask;
			this.suspended_obj = suspended_obj;
		}
	}
	
	private Vector<ObjectSpec> objectSpecsToRemove = new Vector<ObjectSpec>();
	
	/** indicates when engine is inside a parallel object update (moveObjects,
	 * check*Collision) */
	private boolean inParallelUpdate = false;

	// Stuff to help generate less garbage... Needed? Think not...
	private JGRectangle tmprect1 = new JGRectangle();
	private JGRectangle tmprect2 = new JGRectangle();

	private JGObject [] srcobj = new JGObject[50];
	private JGObject [] dstobj = new JGObject[50];
	
	/** Add new object now.  Old object with the same name is replaced
	 * immediately, and its remove() method called.  
	 * @param obj TODO*/
	void addObject(JGObject obj) {
		int idx = objects.get(obj.getName());
		if (idx >= 0) {
			JGObject old_obj = objects.valueAt(idx);
			// disable object so it doesn't call engine on removal
			old_obj.removeDone();
			// ensure any dispose stuff in the object is called
			old_obj.remove();
		}
		objects.put(obj.getName(),obj);
	}

	/** Actually remove objects in obj_to_remove. */
	void doRemoveList() {
		for (int i=0; i < objectsToRemove.size(); i++) {
			( objectsToRemove.valueAt(i)).removeDone();
		}
		objects.remove(objectsToRemove);
		objectsToRemove.clear();
	}

	/** Add objects marked for addition. Protected.
	*/
	public void flushAddList() {
		// XXX we have to add one by one because we have to call the dispose
		// method of the objects that are replaced
		for (int i=0; i<objectsToAdd.size(); i++) {
			addObject(objectsToAdd.valueAt(i));
		}
		objectsToAdd.clear();
	}

	public boolean existsObject(String index) {
		return objects.get(index) >= 0;
	}

	public JGObject getObject(String index) {
		int idx = objects.get(index);
		if (idx<0) return null;
		return objects.valueAt(idx);
	}

	public void moveObjects(EngineLogic engineLogic, DebugAndErrorHandlerInterface eng, String prefix, int cidmask) {
		if (inParallelUpdate) throw new JGameError("Recursive call",true);
		inParallelUpdate=true;
		int firstidx=getFirstObjectIndex(prefix);
		int lastidx=getLastObjectIndex(prefix);
		for (int i=firstidx; i<lastidx; i++) {
			JGObject o =  objects.valueAt(i);
			o.move(engineLogic, eng, cidmask);
		}
		flushRemoveList();
		inParallelUpdate=false;
	}

	public void markAddObject(JGObject obj) {
		objectsToAdd.put(obj.getName(),obj);
	}

	/** Mark object for removal. 
	 * @param index TODO*/
	void markRemoveObject(String index) {
		int idx = objects.get(index);
		if (idx<0) return;
		objectsToRemove.put(index,objects.valueAt(idx));
	}

	/** Mark object for removal. 
	 * @param obj TODO*/
	void markRemoveObject(JGObject obj) {
		objectsToRemove.put(obj.getName(),obj);
	}

	/** Actually remove object now 
	 * @param obj TODO*/
	void doRemoveObject(JGObject obj) {
		obj.removeDone();
		objects.remove(obj.getName());
	}

	/** Mark all objects with given spec for removal. 
	 * @param prefix TODO
	 * @param cidmask TODO
	 * @param suspended_obj TODO*/
	void markRemoveObjects(String prefix, int cidmask, boolean suspended_obj) {
		objectSpecsToRemove.addElement(new ObjectSpec(prefix, cidmask, suspended_obj));
	}

	/** Actually remove objects with given spec, including those in obj_to_add
	 * list.  Uses obj_to_remove as a temp variable.  If anything is already
	 * in obj_to_remove, it is left there. If do_remove_list is true, the
	 * objects are removed and obj_to_remove is cleared.  Otherwise, the
	 * objects to remove are just added to obj_to_remove. 
	 * @param prefix TODO
	 * @param cidmask TODO
	 * @param suspended_obj TODO
	 * @param do_remove_list TODO*/
	void doRemoveObjects(String prefix, int cidmask, boolean suspended_obj, boolean do_remove_list) {
		int firstidx=getFirstObjectIndex(prefix);
		int lastidx=getLastObjectIndex(prefix);
		for (int i=firstidx; i<lastidx; i++) {
			JGObject o = objects.valueAt(i);
			if (cidmask==0 || (o.colid&cidmask)!=0) {
				if (suspended_obj || !o.is_suspended) {
					objectsToRemove.put(objects.keyAt(i),o);
				}
			}
		}
		if (do_remove_list) doRemoveList();
		// if we enumerate backwards, we can remove elements inline without
		// consistency problems
		for (int i=objectsToAdd.size()-1; i>=0; i--) {
			JGObject o = objectsToAdd.valueAt(i);
			if (prefix==null || objectsToAdd.keyAt(i).startsWith(prefix)) {
				if (cidmask==0 || (o.colid&cidmask)!=0) {
					if (suspended_obj || !o.is_suspended) {
						// Note: remove element inside element enumeration
						objectsToAdd.remove(objectsToAdd.keyAt(i));
					}
				}
			}
		}
	}
	
	void doRemoveObjects(ObjectSpec objectSpec, boolean do_remove_list) {
		doRemoveObjects(objectSpec.prefix, objectSpec.cidmask, objectSpec.suspended_obj, do_remove_list);
	}

	/** protected, remove objects marked for removal. */
	public void flushRemoveList() {
		for (ObjectSpec objectSpec : objectSpecsToRemove) {
			doRemoveObjects(objectSpec, false);
		}
		objectSpecsToRemove.removeAllElements();
		doRemoveList();
	}

	public void moveObjects(EngineLogic engineLogic, DebugAndErrorHandlerInterface eng) {
		moveObjects(engineLogic, eng,null,0); 
	}

	public void checkCollision(DebugAndErrorHandlerInterface eng, int srccid, int dstcid) {
		if (inParallelUpdate) throw new JGameError("Recursive call",true);
		inParallelUpdate=true;
		if (objects.size() > srcobj.length) {
			// grow arrays to make objects fit
			srcobj = new JGObject[objects.size()+50];
			dstobj = new JGObject[objects.size()+50];
		}
		int srcsize = 0;
		int dstsize = 0;
		/* get all matching objects */
		JGRectangle sr = tmprect1;
		JGRectangle dr = tmprect2;
		for (int i=0; i<objects.size(); i++) {
			JGObject o  = objects.valueAt(i);
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
		flushRemoveList();
		inParallelUpdate=false;
	}

	public int checkCollision(int cidmask, JGObject obj) {
		JGRectangle bbox = obj.getBBox();
		if (bbox==null) return 0;
		int retcid=0;
		JGRectangle obj_bbox = tmprect1;
		for (int i=0; i<objects.size(); i++) {
			JGObject o = objects.valueAt(i);
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

	public void checkBGCollision(EngineLogic engineLogic, DebugAndErrorHandlerInterface eng, int tilecid, int objcid) {
		if (inParallelUpdate) throw new JGameError("Recursive call",true);
		inParallelUpdate=true;
		if (objects.size() > srcobj.length) {
			// grow arrays to make objects fit
			srcobj = new JGObject[objects.size()+50];
		}
		int srcsize = 0;
		JGRectangle r = tmprect1;
		/* get all matching objects */
		for (int i=0; i<objects.size(); i++) {
			JGObject o  = objects.valueAt(i);
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
		flushRemoveList();
		inParallelUpdate=false;
	}

	public Vector<JGObject> getObjects(String prefix, int cidmask, boolean suspended_obj, JGRectangle bbox) {
		Vector<JGObject> objects_v = new Vector<JGObject>(50,100);
		JGRectangle obj_bbox = tmprect1;
		int firstidx=getFirstObjectIndex(prefix);
		int lastidx=getLastObjectIndex(prefix);
		for (int i=firstidx; i<lastidx; i++) {
			JGObject obj  = objects.valueAt(i);
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

	public void removeObject(JGObject obj) {
		if (inParallelUpdate) { // queue remove
			markRemoveObject(obj);
		} else { // do remove immediately
			doRemoveObject(obj);
		}
	}

	public void removeObjects(String prefix, int cidmask, boolean suspended_obj) {
		if (inParallelUpdate) {
			markRemoveObjects(prefix, cidmask,suspended_obj);
		} else {
			doRemoveObjects(prefix, cidmask,suspended_obj,true);
		}
	}

	public int countObjects(String prefix, int cidmask) {
		return countObjects(prefix, cidmask,true);
	}

	public int countObjects(String prefix, int cidmask, boolean suspended_obj) {
		int nr_obj=0;
		int firstidx=getFirstObjectIndex(prefix);
		int lastidx=getLastObjectIndex(prefix);
		for (int i=firstidx; i<lastidx; i++) {
			JGObject obj = objects.valueAt(i);
			if (cidmask==0 || (obj.colid&cidmask)!=0) {
				if (suspended_obj || !obj.is_suspended) {
					nr_obj++;
				}
			}
		}
		return nr_obj;
	}
	
	public int getLastObjectIndex(String prefix) {
		if (prefix==null) return objects.size();
		// XXX theoretically there may be strings with prefix
		// lexicographically below this one
		return -1 - objects.get(prefix+'\uffff');
	}


	int getFirstObjectIndex(String prefix) {
		if (prefix==null) return 0;
		int firstidx = objects.get(prefix);
		if (firstidx<0) firstidx = -1-firstidx;
		return firstidx;
	}

	/** Do final update actions on objects after all frame updates finished.
	* Protected. */
	public void frameFinished() {
		for (int i=0; i<objects.size(); i++) {
			(objects.valueAt(i)).frameFinished();
		}
	}

	public void removeObjects(String prefix, int cidmask) {
		removeObjects(prefix, cidmask,true);
	}


}