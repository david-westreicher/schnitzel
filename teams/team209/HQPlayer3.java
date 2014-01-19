package team209;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class HQPlayer3 extends Player {
	private static final int MAX_ROBOTS = GameConstants.MAX_ROBOTS;
	private RobotController rc;
	private int width;
	private int height;
	private double[][] map;
	private double[][] cowGrowth;
	private MapLocation loc;
	private RobotType typeOrder[] = new RobotType[] { RobotType.SOLDIER,
			RobotType.NOISETOWER, RobotType.PASTR, RobotType.SOLDIER,
			RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
			RobotType.SOLDIER };
	private int currentType = 0;
	private ArrayList<double[]> possNoisePos;

	public HQPlayer3(RobotController rc) throws GameActionException {
		this.rc = rc;
		loc = rc.getLocation();
		//possNoisePos = Analyser.findBestNoisePos(width, height, rc, loc);
		for (int i = 0; i < Math.min(possNoisePos.size(), 4); i++) {
			// System.out.println("bestPos: " + possNoisePos.get(i)[0] + ", "
			// + possNoisePos.get(i)[1]);
			if (i == 0) {
				BroadCaster.broadCast(rc, BroadCaster.SWARMPOS_CHANNEL,
						(int) possNoisePos.get(i)[0],
						(int) possNoisePos.get(i)[1]);
			}
			BroadCaster.broadCast(rc, BroadCaster.NOISEPOS_CHANNEL + i,
					(int) possNoisePos.get(i)[0], (int) possNoisePos.get(i)[1]);
		}
	}

	public void run() throws GameActionException {
		if (!Shooting.tryToShoot(rc)) {
			if (Clock.getRoundNum() % 10 == 0) {
				MapLocation closestPastr = Util.closest(loc,
						rc.sensePastrLocations(rc.getTeam().opponent()));
				if (closestPastr != null)
					BroadCaster.broadCast(rc, BroadCaster.SWARMPOS_CHANNEL,
							closestPastr.x, closestPastr.y);
				else {
					if (possNoisePos.size() > 0) {
						double[] pastrLoc = possNoisePos.get(0);
						BroadCaster.broadCast(rc, BroadCaster.SWARMPOS_CHANNEL,
								(int) pastrLoc[0], (int) pastrLoc[1]);
					}
				}
			}
			tryToSpawn();
		}
	}

	private void tryToSpawn() throws GameActionException {
		if (rc.senseRobotCount() < MAX_ROBOTS)
			for (Direction d : Util.VALID_DIRECTIONS) {
				if (rc.canMove(d) && rc.isActive()) {
					rc.broadcast(BroadCaster.TYPE_CHANNEL,
							typeOrder[currentType % typeOrder.length].ordinal());
					rc.spawn(d);
					currentType++;
				}
			}
	}
}
