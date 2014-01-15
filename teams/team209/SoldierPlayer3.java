package team209;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class SoldierPlayer3 extends Player {

	private RobotController rc;
	private MapLocation toLocation;
	private RobotType type;
	private MapLocation loc;

	public SoldierPlayer3(RobotController rc) throws GameActionException {
		this.rc = rc;
		int[] toLoc = null;
		type = RobotType.values()[rc.readBroadcast(BroadCaster.TYPE_CHANNEL)];
		if (type == RobotType.NOISETOWER || type == RobotType.PASTR) {
			boolean found = false;
			for (int noiseID = 0; noiseID < 5; noiseID++) {
				int msg = rc.readBroadcast(BroadCaster.NOISEPOS_CHANNEL
						+ noiseID);
				if (msg == 0) {
					break;
				}
				int spawned = msg / 100000000;
				if (spawned < 2) {
					found = true;
					toLoc = BroadCaster.fromInt2(msg);
					toLocation = new MapLocation(toLoc[0], toLoc[1]);
					rc.broadcast(BroadCaster.NOISEPOS_CHANNEL + noiseID,
							msg + 100000000);
					BroadCaster.broadCast(rc, BroadCaster.SWARMPOS_CHANNEL,
							toLocation.x, toLocation.y);
					break;
				}
			}
			if (!found) {
				type = RobotType.SOLDIER;
			}
		}
	}

	public void run() throws GameActionException {
		if (!Shooting.tryToShoot(rc)) {
			loc = rc.getLocation();
			switch (type) {
			case NOISETOWER:
			case PASTR:
				if (Util.distance(loc.x, loc.y, toLocation.x, toLocation.y) < (type == RobotType.NOISETOWER ? 1
						: 2)) {
					if (type != RobotType.SOLDIER)
						if (rc.isActive() && !rc.isConstructing())
							rc.construct(type);
				} else {
					pathTo(toLocation);
				}
				break;
			case SOLDIER:
				int[] swarmPos = BroadCaster.fromInt2(rc
						.readBroadcast(BroadCaster.SWARMPOS_CHANNEL));
				pathTo(new MapLocation(swarmPos[0], swarmPos[1]));
				break;
			default:
				break;
			}
		}
	}

	private void pathTo(MapLocation loc2) throws GameActionException {
		dynamicMove(loc, loc2, false);
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
		int countDir = 0;
		boolean right = (Clock.getRoundNum() / 100) % 2 == 0;
		if (!right)
			bestDir += Util.VALID_DIRECTIONS.length;
		while (countDir++ < Util.VALID_DIRECTIONS.length) {
			if (tryToMove(bestDir, sneak))
				return true;
			else
				bestDir += right ? 1 : -1;
		}
		// if (!tryToMove(bestDir, sneak))
		// if (!tryToMove(bestDir + 1, sneak))
		// return tryToMove(bestDir + Util.VALID_DIRECTIONS.length - 1,
		// sneak);
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
