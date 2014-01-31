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
	private static final int HQ_ATTACK_DISTANCE = 25;
	private static final Direction validDirs[] = new Direction[] {
			Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
			Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST,
			Direction.WEST, Direction.NORTH_WEST };

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
		while (size * size < RobotType.SOLDIER.attackRadiusMaxSquared)
			size++;
		size *= 2;
		size++;
		attackSquare = new int[size][size];
		int attackSquareMid = size / 2;
		for (int i = 0; i < size; i++)
			for (int j = 0; j < size; j++) {
				int diffX = i - attackSquareMid;
				int diffY = j - attackSquareMid;
				if (diffX * diffX + diffY * diffY <= RobotType.SOLDIER.attackRadiusMaxSquared) {
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
		if (id == 0 && Clock.getRoundNum() % 100 == 0)
			Util.tick();
		loc = rc.getLocation();
		senseRobots();
		if (enemyRobotsCount == 0) {
			if (buildingsCount > 0) {
				if (attackBuilding())
					return;
			}
			hold();
		} else if (!outnumbering()) {
			retreat();
		} else if (isSafe()) {
			MapLocation friendToHelp = canHelpFriend();
			if (friendToHelp != null) {
				helpFriend(friendToHelp);
			} else {
				attack();
			}
		} else {
			shoot();
		}
		if (id == 0 && Clock.getRoundNum() % 100 == 0)
			Util.tock("run");
	}

	private boolean attackBuilding() throws GameActionException {
		for (int i = 0; i < buildingsCount; i++) {
			if (buildings[i].location.distanceSquaredTo(loc) <= RobotType.SOLDIER.attackRadiusMaxSquared) {
				if (rc.isActive()) {
					rc.attackSquare(buildings[i].location);
					return true;
				}
			}
		}
		return false;
	}

	private void attack() throws GameActionException {
		rc.setIndicatorString(1, "attack");
		RobotInfo ri = enemyRobots[0];
		tryToMove(loc.directionTo(ri.location), true, 5);
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
		rc.setIndicatorString(1, "hold");
		if (loc.distanceSquaredTo(enemyHq) <= HQ_ATTACK_DISTANCE) {
			tryToMove(enemyHq.directionTo(loc), true, 5);
		} else
			tryToMove(loc.directionTo(toLoc), false, 3);
	}

	private void helpFriend(MapLocation friendToHelp)
			throws GameActionException {
		rc.setIndicatorString(1, "helpFriend " + friendToHelp);
		tryToMove(loc.directionTo(friendToHelp), true);
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
		rc.setIndicatorString(1, "retreat");
		Direction dir = enemyRobots[0].location.directionTo(loc);
		tryToMove(dir, true, 5);
	}

	private void shoot() throws GameActionException {
		rc.setIndicatorString(1, "shoot");
		double minHP = 100000;
		double minID = 10000000;
		RobotInfo minHpRobot = null;
		for (int i = 0; i < enemyRobotsCount; i++) {
			RobotInfo ri = enemyRobots[i];
			if (ri.health <= minHP)
				if (ri.location.distanceSquaredTo(loc) <= RobotType.SOLDIER.attackRadiusMaxSquared) {
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
		if (minHpRobot != null && rc.isActive())
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
			if (enemy.location.distanceSquaredTo(loc) <= RobotType.SOLDIER.attackRadiusMaxSquared)
				return true;
		}
		return false;
	}

	private void senseRobots() throws GameActionException {
		Robot[] gos = rc.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared);
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
				} else if (ri.type == RobotType.NOISETOWER
						|| ri.type == RobotType.PASTR) {
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

	private boolean tryToMove(Direction dir, boolean attack)
			throws GameActionException {
		return tryToMove(dir, attack, 5);
	}

	private boolean tryToMove(Direction dir, boolean attack, int howDirect)
			throws GameActionException {
		int index = Util.getDirectionIndex(dir);
		for (int i = 0; i < howDirect; i++) {
			dir = validDirs[(index + niceMove[i]) % validDirs.length];
			if (!attack
					&& loc.add(dir).distanceSquaredTo(enemyHq) <= HQ_ATTACK_DISTANCE)
				continue;
			if (!attack)
				if (isEnemyInRange(loc.add(dir)))
					continue;
			if (rc.canMove(dir) && rc.isActive()) {
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
		this.toLoc = toLoc;
		this.sneak = sneak;
		run();
	}
}
