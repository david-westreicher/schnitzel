package seedingteam209;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Shooting {
	public static RobotInfo nearbyRobots[] = new RobotInfo[3];

	public static boolean tryToShoot(RobotController rc)
			throws GameActionException {
		return tryToShoot(rc, 10);
	}

	public static boolean tryToShoot(RobotController rc, int attackRadius)
			throws GameActionException {
		Robot[] robots = rc.senseNearbyGameObjects(Robot.class, attackRadius,
				rc.getTeam().opponent());
		int index = 0;
		for (Robot r : robots) {
			RobotInfo ri = rc.senseRobotInfo(r);
			if (ri.type != RobotType.HQ) {
				if (index >= nearbyRobots.length)
					break;
				nearbyRobots[index++] = ri;
			}
		}
		if (index == 0)
			return false;
		double minHealth = 100000;
		RobotInfo minHealthBot = null;
		for (int i = 0; i < index; i++) {
			RobotInfo ri = nearbyRobots[i];
			if (ri.health < minHealth) {
				minHealth = ri.health;
				minHealthBot = ri;
			}
		}
		if (rc.isActive())
			rc.attackSquare(minHealthBot.location);
		return true;
	}

	public static Direction retreat(RobotController rc, double hp,
			int sensorRadius) throws GameActionException {
		Robot[] robots = rc.senseNearbyGameObjects(Robot.class, sensorRadius,
				rc.getTeam().opponent());
		int midPoint[] = new int[2];
		double hpSumOfOthers = 0;
		int index = 0;
		for (Robot r : robots) {
			RobotInfo ri = rc.senseRobotInfo(r);
			if (ri.type != RobotType.HQ) {
				if (index >= nearbyRobots.length)
					break;
				midPoint[0] += ri.location.x;
				midPoint[1] += ri.location.y;
				hpSumOfOthers += ri.health;
				nearbyRobots[index++] = ri;
			}
		}
		if (index == 0 || hpSumOfOthers > hp)
			return null;
		midPoint[0] /= index;
		midPoint[1] /= index;
		Direction dir = rc.getLocation().directionTo(
				new MapLocation(midPoint[0], midPoint[1]));
		return dir.opposite();
	}
}
