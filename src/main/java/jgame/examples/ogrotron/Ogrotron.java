package jgame.examples.ogrotron;
import jgame.*;
import jgame.platform.*;
/** Simple robotron clone, made to work well with all platforms:
* midp, jre, and jogl. Supports touch screen. */
public class Ogrotron extends StdGame {
	public static void main(String[]args) {new Ogrotron(parseSizeArgs(args,0));}
	public Ogrotron() { initEngineApplet(); }
	public Ogrotron(JGPoint size) { initEngine(size.x,size.y); }
	public void initCanvas() { setCanvasSettings(15,20,16,16,null,null,null); }
	public void initGame() {
		defineMedia("ogrotron.tbl");
		setSmoothing(false);
		setVideoSyncedUpdate(true);
		if (isMidlet()) {
			setFrameRate(22,1);
			setGameSpeed(2.0);
		} else {
			setFrameRate(45,1);
			title_font = new JGFont("Arial",0,11);
			highscore_title_font = new JGFont("Arial",0,11);
			highscore_font = new JGFont("Arial",0,11);
		}
		leveldone_ingame=true;
		startgame_ingame=true;
		lifelost_ingame=true;
		setHighscores(10,new Highscore(0,"nobody"),15);
	}
	public void doFrameTitle() {
		if (getMouseButton(1)) {
			clearMouseButton(1);
			startGame();
		}
	}
	Player player=null;
	public void defineLevel() {
		removeObjects(null,0);
		fillBG("");
		player=new Player(pfWidth()/2,pfHeight()/2);
		for (int i=0; i<8+level*2; i++)
			new Ogre(random(0,pfWidth()-8),random(0,pfWidth()-8),
				random(0.45,0.9) );
		for (int i=0; i<8+level; i++)
			setTile(random(0,pfTilesX(),1),random(0,pfTilesY(),1),
				level%2==0 ? "$" : "%");
			//new JGObject("bonus",true, random(0,pfWidth()-8),
			//	random(0,pfWidth()-8), 8, "bonus",-1);
	}
	public void initNewLife() {
		player=new Player(pfWidth()/2,pfHeight()/2);
	}
	public void startGameOver() { removeObjects(null,0); }
	public void doFrameInGame() {
		moveObjects();
		checkCollision(4+8,1); // ogre, bonus hit player
		checkBGCollision(1,1); // bonus hits player
		checkCollision(2,4);   // bullet hits ogre
		if (countObjects("ogre",0)==0) levelDone();
		//if (checkTime(0,800,20-level))
		//	new JGObject("pod",true,pfWidth(),random(0,pfHeight()-16),
		//		4, "pod", 0,0,14,14,  (-3.0-level), 0.0, -2);
	}
	public void incrementLevel() {
		if (level<8) level++;
		stage++;
	}
	JGFont scoring_font = new JGFont("Arial",0,8);
	class Ogre extends JGObject {
		Ogre(double x,double y, double speed) {
			super("ogre",true,x,y,4,stage%2==0 ? "ogre":"ogre2",speed,speed,-1);
		}
		double newdirtimer=0;
		public void move() {
			if (newdirtimer<=0) {
				newdirtimer=random(15,35);
				if (player!=null) {
					if (random(0,1) > 0.5) {
						if (player.x > x) xdir=1; else xdir=-1;
						ydir = random(-1,1,1);
					} else {
						if (player.y > y) ydir=1; else ydir=-1;
						xdir = random(-1,1,1);
					}
				}
				if (x<0 && xdir<=0) xdir=1;
				if (x>pfwidth && xdir>=0) xdir=-1;
				if (y<0 && ydir<=0) ydir=1;
				if (y>pfheight && ydir>=0) ydir=-1;
			} else {
				newdirtimer -= gamespeed;
			}
		}
		public void hit(JGObject obj) {
			score += 10;
			remove();
			obj.remove();
			new JGObject("explo",true, x,y, 0,"explo_u",0,-4, EXPIRE_OFF_PF);
			new JGObject("explo",true, x,y, 0,"explo_d",0,4, EXPIRE_OFF_PF);
		}
	}
	public class Player extends JGObject {
		public Player(double x,double y) {
			super("player",false,x,y,1,"player", 1.0,1.0, -1);
		}
		double bullettimer=0;
		double invultimer=100;
		double bulxspeed=0,bulyspeed=0;
		public void move() {
			if (getMouseButton(1)) {
				double angle = atan2(getMouseX() - x, getMouseY() - y);
				xspeed = 2.0*Math.sin(angle);
				yspeed = 2.0*Math.cos(angle);
				xdir = 1;
				ydir = 1;
				bulxspeed = -xspeed*xdir;
				bulyspeed = -yspeed*ydir;
				if (xspeed < 0) setGraphic("player_l");
				else setGraphic("player_r");
			} else {
				xspeed = 2.0;
				yspeed = 2.0;
				setDir(0,0);
				if (getKey(key_up)    && y > yspeed)       ydir=-1;
				if (getKey(key_down)  && y < pfHeight()-8) ydir=1;
				if (getKey(key_left)  && x > xspeed) {
					xdir=-1;
					setGraphic("player_l");
				}
				if (getKey(key_right) && x < pfWidth()-8) {
					xdir=1;
					setGraphic("player_r");
				}
				if (xdir!=0||ydir!=0) {
					bulxspeed = xspeed*xdir;
					bulyspeed = yspeed*ydir;
				}
			}
			if (bullettimer<=0) {
				if (bulxspeed!=0 || bulyspeed!=0) {
					bullettimer=8;
					new JGObject("bullet",true, x,y, 2, "bullet",
						-2.0*bulxspeed, -2.0*bulyspeed, EXPIRE_OFF_PF);
				}
			} else {
				bullettimer -= gamespeed;
			}
			invultimer -= gamespeed;
		}
		public void hit_bg(int tilecid,int tilex,int tiley) {
			setTile(tilex,tiley,"");
			score+=25;
			new JGObject("pts",true,tilex*tilewidth,tiley*tileheight,0,"pts",
				0,-0.25, 80);
		}
		public void hit(JGObject obj) {
			if (invultimer<0) {
				remove();
				lifeLost();
				new JGObject("explo",true,x,y, 0,"explo_u",0,-3, EXPIRE_OFF_PF);
				new JGObject("explo",true,x,y, 0,"explo_d",0,3, EXPIRE_OFF_PF);
				new JGObject("explo",true,x,y,0,"explo_u",0,-1.5,EXPIRE_OFF_PF);
				new JGObject("explo",true,x,y,0,"explo_d",0, 1.5,EXPIRE_OFF_PF);

			}
			//new StdScoring("pts",obj.x,obj.y,0,-1.0,40,"5 pts",scoring_font,
			//	new JGColor [] { JGColor.red,JGColor.yellow },2);
		}
	}
}
