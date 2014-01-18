package team209;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameActionExceptionType;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class SoldierPlayer3 extends Player {

	private RobotController rc;
	private MapLocation toLocation;
	private RobotType type;
	private MapLocation loc;
	private double[][] map;
	private MapLocation currentPathingLocation;
	private boolean bugging;
	private int buggingDir;

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

		// Bytecodes for map sensing= 101449
		// Bytecodes for map sensing= 97789
		// Bytecodes for map sensing= 90586
		int width = rc.getMapWidth();
		int height = rc.getMapWidth();
		System.out.println("size: " + width + ", " + height);
		Util.tick();
		TerrainTile[][] localmap = new TerrainTile[width][height];
		for (int i = width; --i >= 0;)
			for (int j = height; --j >= 0;)
				localmap[i][j] = rc.senseTerrainTile(new MapLocation(i, j));
		// this.map = localmap;
		Util.tock("Bytecodes for map sensing");
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
		/*
		 * if (!loc2.equals(currentPathingLocation)) { int start =
		 * Clock.getBytecodeNum(); currentPathingLocation = loc2;
		 * generatePath(loc2);
		 * System.out.println("Bytecodes for generating path = " +
		 * (Clock.getBytecodeNum() - start)); }
		 */
		bugMove(loc, loc2);
		// dynamicMove(loc, loc2, false);
	}

	private void bugMove(MapLocation fromLoc, MapLocation toLoc)
			throws GameActionException {
		double[][] localMap = map;
		int width = localMap.length;
		int height = localMap[0].length;
		int[] currPos = new int[] { fromLoc.x, fromLoc.y };
		int[] mov = new int[] { (int) Math.signum(toLoc.x - fromLoc.x),
				(int) Math.signum(toLoc.y - fromLoc.y) };
		currPos[0] += mov[0];
		currPos[1] += mov[1];
		currPos[0] = Math.max(0, Math.min(width - 1, currPos[0]));
		currPos[1] = Math.max(0, Math.min(height - 1, currPos[1]));
		if (bugging)
			return;
		if (localMap[currPos[0]][currPos[1]] != 2) {
			Direction dir = Util.VALID_DIRECTIONS[getDirection(mov[0], mov[1])];
			rc.setIndicatorString(0, "moving to " + dir);
			tryToMove(getDirection(mov[0], mov[1]), false);
		} else {
			currPos[0] -= mov[0];
			currPos[1] -= mov[1];
			int directionToWall = getDirection(mov[0], mov[1]);
			bugging = true;
			if (Math.abs(mov[0]) + Math.abs(mov[1]) == 2) {
				// diagonal move->check if lateral move to block necessary
				if (localMap[currPos[0]][currPos[1] + mov[1]] != 2
						&& localMap[currPos[0] + mov[0]][currPos[1]] != 2) {
					tryToMove(getDirection(mov[0], 0), false);
				}
			}
			buggingDir = getDirection(mov[0], mov[1]);
			rc.setIndicatorString(0, "currPos: " + currPos[0] + ", "
					+ currPos[1] + " , "
					+ Util.VALID_DIRECTIONS[directionToWall]);
		}
	}

	private void generatePath(MapLocation loc2) {
		double[][] localMap = map;
		int width = localMap.length;
		int height = localMap[0].length;
		ArrayList<int[]> path = new ArrayList<int[]>();
		int[] currPos = new int[] { loc.x, loc.y };
		int[] mov = new int[] { (int) Math.signum(loc2.x - loc.x),
				(int) Math.signum(loc2.y - loc.y) };
		int moves = 0;
		boolean followBlockRight = Util.RAND.nextBoolean();
		while (moves++ < 100) {
			if (localMap[currPos[0]][currPos[1]] != 2) {
				currPos[0] += mov[0];
				currPos[1] += mov[1];
				mov[0] = (int) Math.signum(currPos[0] - loc2.x);
				mov[1] = (int) Math.signum(currPos[1] - loc2.y);
				currPos[0] = Math.max(0, Math.min(width - 1, currPos[0]));
				currPos[1] = Math.max(0, Math.min(height - 1, currPos[1]));
			} else {
				currPos[0] -= mov[0];
				currPos[1] -= mov[1];
				int directionToWall = getDirection(mov[0], mov[1]);
				rc.setIndicatorString(0, "currPos: " + currPos[0] + ", "
						+ currPos[1] + " , "
						+ Util.VALID_DIRECTIONS[directionToWall]);
			}
		}
	}

	private int getDirection(int diffX, int diffY) {
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
		return bestDir;
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
