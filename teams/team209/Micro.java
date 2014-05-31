package team209;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Micro {

	private RobotController rc;
	private MapLocation loc;
	private MapLocation enemyHq;
	private int id;
	private int niceMoveRIGHT[] = new int[] { 0, 1, 7, 2, 6, 3, 5, 4 };
	private int niceMoveLEFT[] = new int[] { 0, 7, 1, 6, 2, 5, 3, 4 };
	private int[] niceMove;
	private int[][] attackSquare;
	private RobotInfo[] enemyRobots;
	private RobotInfo[] friendlyRobots;
	private MapLocation toLoc;
	private int enemyRobotsCount;
	private int friendlyRobotsCount;
	private double enemyHealth;
	private double friendHealth;
	private RobotInfo[] buildings;
	private int buildingsCount;
	private boolean sneak;
	private boolean potentialHQWrongMove;
	private boolean stuck;
	private MapLocation lastToLoc;
	private int stuckTimer;
	private int randomDir = (int) Math.random() ;
	private static final int HQ_ATTACK_DISTANCE = 25;
	private static final Direction validDirs[] = new Direction[] {
			Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
			Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST,
			Direction.WEST, Direction.NORTH_WEST };
	private static final int soldierSensorRadius = RobotType.SOLDIER.sensorRadiusSquared;
	private static final int soldierAttackRadius = RobotType.SOLDIER.attackRadiusMaxSquared;
	private static final int hqAttackRadius = RobotType.HQ.attackRadiusMaxSquared;

	public Micro(RobotController rc) throws GameActionException {
		this.rc = rc;
		loc = rc.getLocation();
		enemyHq = rc.senseEnemyHQLocation();
		id = rc.readBroadcast(BroadCaster.SOLDIER_ID);
		niceMove = (id % 2) == 0 ? niceMoveLEFT : niceMoveRIGHT;
		rc.broadcast(BroadCaster.SOLDIER_ID, id + 1);
		initLocalMap();
	}

	private void initLocalMap() {
		int size = 1;
		while (size * size < soldierAttackRadius)
			size++;
		size *= 2;
		size++;
		attackSquare = new int[size][size];
		int attackSquareMid = size / 2;
		for (int i = 0; i < size; i++)
			for (int j = 0; j < size; j++) {
				int diffX = i - attackSquareMid;
				int diffY = j - attackSquareMid;
				if (diffX * diffX + diffY * diffY <= soldierAttackRadius) {
					attackSquare[i][j] = 1;
				}
			}
		// if (id == 0) {
		// System.out.println(RobotType.HQ.attackRadiusMaxSquared);
		// printMap(hqSquare);
		// }
		// rc.setIndicatorString(1, "" + id);
	}

	private void run() throws GameActionException {
		potentialHQWrongMove = loc.distanceSquaredTo(enemyHq) <= HQ_ATTACK_DISTANCE;
		senseRobots();
		if (enemyRobotsCount == 0) {
			if (!attackBuilding())
				hold();
		} else if (!outnumbering()) {
			retreat();
			// kamikaze();
		} else if (isSafe()) {
			MapLocation friendToHelp = canHelpFriend();
			if (friendToHelp != null) {
				helpFriend(friendToHelp);
			} else {
				if (!attackBuilding())
					attack();
			}
		} else {
			if (!attackBuilding())
				shoot();
		}
	}

	private void kamikaze() throws GameActionException {
		double minDistance = 100000;
		RobotInfo minDistRobot = null;
		for (int i = 0; i < enemyRobotsCount; i++) {
			RobotInfo ri = enemyRobots[i];
			int dist = ri.location.distanceSquaredTo(loc);
			if (dist < minDistance) {
				minDistance = dist;
				minDistRobot = ri;
			}
		}
		if (minDistRobot != null)
			if (minDistance <= 2)
				rc.selfDestruct();
			else
				tryToMove(directionTo(loc, minDistRobot.location), true, 5);
	}

	private boolean attackBuilding() throws GameActionException {
		if (buildingsCount == 0)
			return false;
		// rc.setIndicatorString(1, "attackBuilding");
		for (int i = 0; i < buildingsCount; i++) {
			if (buildings[i].type == RobotType.PASTR)
				if (buildings[i].location.distanceSquaredTo(loc) <= soldierAttackRadius) {
					rc.attackSquare(buildings[i].location);
					return true;
				}
		}
		/*
		 * for (int i = 0; i < buildingsCount; i++) { if (buildings[i].type ==
		 * RobotType.NOISETOWER) if
		 * (buildings[i].location.distanceSquaredTo(loc) <= soldierAttackRadius)
		 * { if (rc.isActive()) { rc.attackSquare(buildings[i].location); return
		 * true; } } }
		 */
		return false;
	}

	private void attack() throws GameActionException {
		// rc.setIndicatorString(1, "attack");
		RobotInfo ri = enemyRobots[0];
		tryToMove(directionTo(loc, ri.location), true, 5);
	}

	private boolean outnumbering() {
		int friends = friendlyRobotsCount + 1;
		int enemies = enemyRobotsCount;
		if (friends == enemies
				&& friendHealth + rc.getHealth() > enemyHealth * 2)
			return true;
		return friends > enemies;
	}

	private void hold() throws GameActionException {
		// rc.setIndicatorString(1, "hold");
		if (loc.distanceSquaredTo(enemyHq) <= HQ_ATTACK_DISTANCE) {
			tryToMove(directionTo(enemyHq, loc), true, 5);
		} else
			tryToMove(directionTo(loc, toLoc), false, 8);
	}

	private Direction directionTo(MapLocation loc1, MapLocation loc2) {
		int diffX = signum(loc2.x - loc1.x);
		int diffY = signum(loc2.y - loc1.y);
		switch (diffX) {
		case 1:
			switch (diffY) {
			case -1:
				return Direction.NORTH_EAST;
			case 0:
				return Direction.EAST;
			case 1:
				return Direction.SOUTH_EAST;
			}
		case 0:
			switch (diffY) {
			case -1:
				return Direction.NORTH;
			case 0:
				return Direction.NONE;
			case 1:
				return Direction.SOUTH;
			}
		case -1:
			switch (diffY) {
			case -1:
				return Direction.NORTH_WEST;
			case 0:
				return Direction.WEST;
			case 1:
				return Direction.SOUTH_WEST;
			}
		}
		return Direction.NONE;
	}

	private int signum(int i) {
		return i > 0 ? 1 : (i == 0 ? 0 : -1);
	}

	private void helpFriend(MapLocation friendToHelp)
			throws GameActionException {
		// rc.setIndicatorString(1, "helpFriend " + friendToHelp);
		// TODO was set to 5
		tryToMove(directionTo(loc, friendToHelp), true, 8);
	}

	private MapLocation canHelpFriend() {
		for (int i = 0; i < friendlyRobotsCount; i++) {
			RobotInfo friend = friendlyRobots[i];
			if (isEnemyInRange(friend.location))
				return friend.location;
		}
		return null;
	}

	private void retreat() throws GameActionException {
		// rc.setIndicatorString(1, "retreat");
		Direction dir = directionTo(loc, toLoc);// directionTo(enemyRobots[0].location,
												// loc);
		if (!tryToMove(dir, false, 8)) {
			tryToMove(directionTo(enemyRobots[0].location, loc), true, 5);
		}
	}

	private void shoot() throws GameActionException {
		double minHP = 100000;
		double minID = 10000000;
		RobotInfo minHpRobot = null;
		for (int i = 0; i < enemyRobotsCount; i++) {
			RobotInfo ri = enemyRobots[i];
			if (ri.health <= minHP)
				if (ri.location.distanceSquaredTo(loc) <= soldierAttackRadius) {
					if (ri.health < minHP
							|| (ri.health == minHP && ri.robot.getID() < minID)) {
						minHP = ri.health;
						minID = ri.robot.getID();
						minHpRobot = ri;
						if (minHP <= 10)
							break;
					}
				}
		}
		// rc.setIndicatorString(1,
		// "shoot " + minHpRobot + ", " + Clock.getRoundNum());
		if (minHpRobot != null)
			rc.attackSquare(minHpRobot.location);
	}

	private boolean isSafe() {
		return !isEnemyInRange(loc);
	}

	private boolean isEnemyInRange(MapLocation loc) {
		if (enemyRobotsCount == 0)
			return false;
		for (int i = 0; i < enemyRobotsCount; i++) {
			RobotInfo enemy = enemyRobots[i];
			if (enemy.location.distanceSquaredTo(loc) <= soldierAttackRadius)
				return true;
		}
		return false;
	}

	private void senseRobots() throws GameActionException {
		Robot[] gos = rc.senseNearbyGameObjects(Robot.class,
				soldierSensorRadius);
		enemyRobots = new RobotInfo[gos.length];
		friendlyRobots = new RobotInfo[gos.length];
		buildings = new RobotInfo[gos.length];
		Team team = rc.getTeam();
		int enemyIndex = 0;
		int friendlyIndex = 0;
		int buildingIndex = 0;
		enemyHealth = 0;
		friendHealth = 0;
		for (Robot r : gos) {
			if (r.getTeam() != team) {
				RobotInfo ri = rc.senseRobotInfo(r);
				if (ri.type == RobotType.SOLDIER) {
					enemyRobots[enemyIndex++] = ri;
					enemyHealth += ri.health;
				} else if (// ri.type == RobotType.NOISETOWER ||
				ri.type == RobotType.PASTR) {
					buildings[buildingIndex++] = ri;
				}
			}
		}
		if (enemyIndex > 0)
			for (Robot r : gos) {
				if (r.getTeam() == team) {
					RobotInfo ri = rc.senseRobotInfo(r);
					if (ri.type == RobotType.SOLDIER) {
						friendlyRobots[friendlyIndex++] = ri;
						friendHealth += ri.health;
					}
				}
			}
		enemyRobotsCount = enemyIndex;
		friendlyRobotsCount = friendlyIndex;
		buildingsCount = buildingIndex;
		// rc.setIndicatorString(0, enemyIndex + "");
	}

	private boolean tryToMove(Direction dir, boolean attack, int howDirect)
			throws GameActionException {
		if (sneak && Clock.getRoundNum() % 5 != 0)
			return true;
		if (dir == Direction.NONE)
			return true;
		int index = Util.getDirectionIndex(dir);
		if (stuck)
			return tryToMoveCircular(index, attack);
		// rc.setIndicatorString(0, "tryToMove " + dir + ", " + index + ", " +
		// loc
		// + ", " + toLoc);

		for (int i = 0; i < howDirect; i++) {
			dir = validDirs[(index + niceMove[i]) % validDirs.length];
			MapLocation newLoc = loc.add(dir);
			if (potentialHQWrongMove
					&& newLoc.distanceSquaredTo(enemyHq) <= hqAttackRadius)
				continue;
			if (!attack && isEnemyInRange(newLoc))
				continue;

			if (rc.canMove(dir)) {
				if (sneak)
					rc.sneak(dir);
				else
					rc.move(dir);
				return true;
			}
		}
		return false;
	}

	private boolean tryToMoveCircular(int index, boolean attack)
			throws GameActionException {
		index += 8;
		int right = ((((Clock.getRoundNum() / 50 + randomDir)) % 2 == 0) ? -1
				: 1);
		for (int i = 0; i < 8; i++) {
			Direction dir = validDirs[(index + i * right) % validDirs.length];
			MapLocation newLoc = loc.add(dir);
			if (potentialHQWrongMove
					&& newLoc.distanceSquaredTo(enemyHq) <= hqAttackRadius)
				continue;
			if (!attack && isEnemyInRange(newLoc))
				continue;

			if (rc.canMove(dir)) {
				if (sneak)
					rc.sneak(dir);
				else
					rc.move(dir);
				return true;
			}
		}
		return false;
	}

	public void move(RobotController rc, MapLocation toLoc, boolean sneak)
			throws GameActionException {
		this.rc = rc;
		this.lastToLoc = this.toLoc;
		this.toLoc = toLoc;
		if (lastToLoc != null)
			checkForStuck();
		loc = rc.getLocation();
		this.sneak = sneak;
		if (rc.isActive())
			run();
	}

	private void checkForStuck() {
		if (lastToLoc.x == toLoc.x && lastToLoc.y == toLoc.y) {
			stuckTimer++;
			if (stuckTimer > 50) {
				stuckTimer = 0;
				randomDir = Util.RAND.nextInt();
				stuck = !stuck;
			}
		} else
			stuck = false;
	}
}
