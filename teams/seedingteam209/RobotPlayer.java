package seedingteam209;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {
	private static Player p;

	public static void run(RobotController rc) {
		Util.init(rc);
		try {
			if (rc.getType() == RobotType.HQ) {
				p = new HQPressure(rc);
			} else if (rc.getType() == RobotType.SOLDIER) {
				p = new SoldierPressure(rc);
			} else if (rc.getType() == RobotType.NOISETOWER) {
				p = new NoiseTower(rc);
			}
			if (p != null)
				while (true) {
					p.run();
					rc.yield();
				}
			else
				while (true)
					rc.yield();
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}
}
