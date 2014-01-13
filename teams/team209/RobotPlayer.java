package team209;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {
	public static Player hq;
	public static Player sp;
	public static RobotController RC;

	public static void run(RobotController rc) {
		Util.init(rc);
		RC = rc;
		if (rc.getType() == RobotType.HQ) {
			try {
				hq = new HQPlayer(rc);
				runHQ();
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		} else if (rc.getType() == RobotType.SOLDIER) {
			try {
				sp = new SoldierPlayer(rc);
				runSoldier();
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		} else {
			while (true) {
				rc.yield();
			}
		}
	}

	private static void runSoldier() {
		while (true) {
			try {
				sp.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
			RC.yield();
		}
	}

	private static void runHQ() {
		while (true) {
			try {
				hq.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
			RC.yield();
		}
	}
}
