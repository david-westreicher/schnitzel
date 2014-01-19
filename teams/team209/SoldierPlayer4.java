package team209;

import team209.HQPlayer4.Action;
import team209.OptimizedPathing.PathType;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class SoldierPlayer4 extends Player {

	private RobotController rc;
	private MapLocation[] path;
	private MapLocation nextLoc;
	private int currentLocIndex;
	private MapLocation loc;
	private MapLocation hq;
	private int minDistance = 2;
	private int frequency;
	private RobotType constructionType = null;

	public SoldierPlayer4(RobotController rc) throws GameActionException {
		this.rc = rc;
		frequency = this.rc.readBroadcast(BroadCaster.CURRENT_FREQUENCY_BAND);
		hq = rc.senseHQLocation();
		path = BroadCaster.readPath(rc, frequency, PathType.HQ_TO_MEETING);
		if (path != null && path.length > 0) {
			getNextLoc();
		}
	}

	private void getNextLoc() throws GameActionException {
		if (currentLocIndex >= path.length)
			return;
		nextLoc = path[currentLocIndex];
		currentLocIndex++;
		rc.setIndicatorString(2, "currentLocIndex: " + currentLocIndex + "/"
				+ path.length);
		// currentLocIndex = Math.min(path.length - 1,
		// Math.max(0, currentLocIndex));
		// if (goBack && currentLocIndex == 0) {
		// goBack = false;
		// step = 0;
		// }
		// if (Util.distance(nextLoc.x, nextLoc.y, hq.x, hq.y) == 0)
		// minDistance = 2;
		// else
		// minDistance = 2;
	}

	private void construct(RobotType type) throws GameActionException {
		this.constructionType = type;
		if (rc.isActive() && !rc.isConstructing())
			rc.construct(type);
	}

	@Override
	public void run() throws GameActionException {
		if (constructionType != null) {
			construct(constructionType);
			return;
		}
		loc = rc.getLocation();
		Action a = BroadCaster.readAction(rc, frequency);
		if (a == Action.NEW_MEETING) {
			MapLocation[] newpath = BroadCaster.readPath(rc, frequency,
					PathType.TO_NEXT_MEETING);
			path = mergePaths(path, newpath);
			frequency++;
			// currentLocIndex = 0;
		}
		if (!Shooting.tryToShoot(rc)) {
			if (nextLoc != null) {
				int dist = Util.distance(nextLoc.x, nextLoc.y, loc.x, loc.y);
				if (dist < minDistance) {
					if (currentLocIndex == path.length) {
						int constructionLevel = rc
								.readBroadcast(BroadCaster.MEETING_CONSTRUCTED);
						rc.setIndicatorString(0, "MEETING_CONSTRUCTED: "
								+ constructionLevel);
						if (dist == 0) {
							rc.broadcast(BroadCaster.MEETING_CONSTRUCTED, 1);
							construct(RobotType.NOISETOWER);
							return;
						} else if (dist == 1 && constructionLevel == 1) {
							rc.broadcast(BroadCaster.MEETING_CONSTRUCTED, 2);
							construct(RobotType.PASTR);
							return;
						}
					}
					getNextLoc();
				}
				dynamicMove(loc, nextLoc, false);
			}
		}
	}

	private MapLocation[] mergePaths(MapLocation[] p1, MapLocation[] p2) {
		MapLocation newpath[] = new MapLocation[p1.length + p2.length];
		for (int i = 0; i < p1.length + p2.length; i++) {
			newpath[i] = (i < p1.length) ? p1[i] : p2[i - p1.length];
		}
		return newpath;

	}

	private boolean dynamicMove(MapLocation loc, MapLocation target,
			boolean sneak) throws GameActionException {
		rc.setIndicatorString(1, "pathing to: " + target);
		int diffX = -(int) Math.signum(loc.x - target.x);
		int diffY = -(int) Math.signum(loc.y - target.y);
		int bestDir = -1;
		if (diffX == 0)
			if (diffY > 0)
				bestDir = 4;
			else
				bestDir = 0;
		else if (diffX > 0) {
			bestDir = 2 + diffY;
		} else
			bestDir = 6 - diffY;
		// int countDir = 0;
		// boolean right = (Clock.getRoundNum() / 30) % 2 == 0;
		// if (!right)
		// bestDir += Util.VALID_DIRECTIONS.length;
		// while (countDir++ < Util.VALID_DIRECTIONS.length) {
		// if (tryToMove(bestDir, sneak))
		// return true;
		// else
		// bestDir += right ? 1 : -1;
		// }
		if (!tryToMove(bestDir, sneak))
			if (!tryToMove(bestDir + 1, sneak))
				return tryToMove(bestDir + Util.VALID_DIRECTIONS.length - 1,
						sneak);
		return false;
	}

	private boolean tryToMove(int bestDir, boolean sneak)
			throws GameActionException {
		Direction dir = Util.VALID_DIRECTIONS[bestDir
				% Util.VALID_DIRECTIONS.length];
		if (rc.canMove(dir))
			if (rc.isActive()) {
				if (sneak)
					rc.sneak(dir);
				else
					rc.move(dir);
				return true;
			}
		return false;
	}
}
