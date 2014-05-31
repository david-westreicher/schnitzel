package team209;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {
	private static Player p;

	public static void run(RobotController rc) {
		Util.init(rc);
		if (rc.getType() == RobotType.HQ) {
			try {
				p = new HQPressure(rc);
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		} else if (rc.getType() == RobotType.SOLDIER) {
			try {
				p = new SoldierPressure(rc);
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		} else if (rc.getType() == RobotType.NOISETOWER) {
			p = new NoiseTower(rc);
		}
		if (p != null)
			while (true) {
				try {
					p.run();
				} catch (GameActionException e) {
					e.printStackTrace();
				}
				rc.yield();
			}
		else
			while (true)
				rc.yield();

	}
}
