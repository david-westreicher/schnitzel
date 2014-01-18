package team209;

import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Shooting {
	public static RobotInfo nearbyRobots[] = new RobotInfo[3];

	public static boolean tryToShoot(RobotController rc)
			throws GameActionException {
		Robot[] robots = rc.senseNearbyGameObjects(Robot.class, 10, rc
				.getTeam().opponent());
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
}
