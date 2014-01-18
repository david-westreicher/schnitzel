package team209;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class SoldierPlayer4 extends Player {

	private RobotController rc;
	private MapLocation[] path;
	private MapLocation nextLoc;
	private int currentLocIndex;
	private MapLocation loc;
	private MapLocation hq;
	private int minDistance;
	private boolean goBack = false;
	private int step;

	public SoldierPlayer4(RobotController rc) throws GameActionException {
		this.rc = rc;
		hq = rc.senseHQLocation();
		path = BroadCaster.readPath(rc);
		if (path.length > 0) {
			getNextLoc();
		}
	}

	private void getNextLoc() {
		currentLocIndex = Math.min(path.length - 1,
				Math.max(0, currentLocIndex));
		nextLoc = path[currentLocIndex];
		currentLocIndex += goBack ? -1 : 1;
		if (Util.distance(nextLoc.x, nextLoc.y, hq.x, hq.y) == 0)
			minDistance = 2;
		else
			minDistance = 2;
	}

	@Override
	public void run() throws GameActionException {
		loc = rc.getLocation();
		rc.setIndicatorString(0, rc.getHealth() + "");
		if (!goBack && rc.getHealth() <= 30) {
			// goBack = true;
			// currentLocIndex -= 2;
			// getNextLoc();
		}
		if (goBack || !Shooting.tryToShoot(rc)) {
			if (nextLoc != null) {
				if (Util.distance(nextLoc.x, nextLoc.y, loc.x, loc.y) < minDistance) {
					getNextLoc();
				}
				dynamicMove(loc, nextLoc, false);
			}
		}
	}

	private void getLastLoc() {
		// TODO Auto-generated method stub

	}

	private boolean dynamicMove(MapLocation loc, MapLocation target,
			boolean sneak) throws GameActionException {
		if (step++ > 10 && (Clock.getRoundNum() / 300) % 2 == 0)
			return true;
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
		int countDir = 0;
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
