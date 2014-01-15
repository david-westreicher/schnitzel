package team209;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {
	private static Player p;

	public static void run(RobotController rc) {
		Util.init(rc);
		try {
			if (rc.getType() == RobotType.HQ) {
				p = new HQPlayer3(rc);
			} else if (rc.getType() == RobotType.SOLDIER) {
				p = new SoldierPlayer3(rc);
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
