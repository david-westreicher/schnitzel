package team209;

import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Shooting {

	public static boolean tryToShoot(RobotController rc)
			throws GameActionException {
		Robot[] robots = rc.senseNearbyGameObjects(Robot.class, 10, rc
				.getTeam().opponent());
		for (Robot r : robots) {
			RobotInfo ri = rc.senseRobotInfo(r);
			if (ri.type != RobotType.HQ) {
				if (rc.isActive())
					rc.attackSquare(ri.location);
				return true;
			}
		}
		return false;
	}

}
