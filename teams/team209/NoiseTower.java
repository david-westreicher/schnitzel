package team209;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class NoiseTower extends Player {

	private static final int DIST_PLUS = 1;
	private RobotController rc;
	private int[] distances;
	private MapLocation loc;
	private int height;
	private int width;
	private int noiseReach = (int) (Math
			.sqrt(RobotType.NOISETOWER.attackRadiusMaxSquared) / 1.5);
	private int currentDiag = 0;
	private int currentDist = 0;
	private MapLocation center;

	public NoiseTower(RobotController rc) {
		this.rc = rc;
		this.width = rc.getMapWidth();
		this.height = rc.getMapHeight();
		distances = new int[8];
		loc = rc.getLocation();
		updateCenter();
	}

	private void calcDistances(MapLocation center) {
		for (int i = 0; i < 8; i++) {
			int xMove = Analyser.moves[i][0];
			int yMove = Analyser.moves[i][1];
			int reach = 4;
			while (true) {
				int x = center.x + xMove * reach;
				int y = center.y + yMove * reach;
				MapLocation m = new MapLocation(x, y);
				int distance = m.distanceSquaredTo(loc);
				if (distance >= RobotType.NOISETOWER.attackRadiusMaxSquared) {
					reach--;
					break;
				} else if (rc.senseTerrainTile(m) == TerrainTile.OFF_MAP)
					break;
				else
					reach++;
			}
			distances[i] = reach;
		}
	}

	@Override
	public void run() throws GameActionException {
		int i = currentDiag;

		int xMove = Analyser.moves[i][0];// (int) (((i / 2) - 0.5f) * 2);
		int yMove = Analyser.moves[i][1];// (int) (((i % 2) - 0.5f) * 2);

		MapLocation attack = new MapLocation(center.x + xMove
				* (distances[i] - currentDist), center.y + yMove
				* (distances[i] - currentDist));
		if (rc.isActive()) {
			rc.attackSquare(attack);
			updateCenter();
			currentDist += DIST_PLUS;
			if (distances[i] - currentDist < 4) {
				currentDiag = (currentDiag + 1) % 8;
				currentDist = 0;
			}
		}
		if (Clock.getRoundNum() % 5 == 0)
			checkForSelfDestruct();
	}

	private void checkForSelfDestruct() throws GameActionException {
		Robot[] enemies = rc.senseNearbyGameObjects(Robot.class,
				RobotType.NOISETOWER.sensorRadiusSquared, rc.getTeam()
						.opponent());
		Robot[] friends = rc.senseNearbyGameObjects(Robot.class,
				RobotType.NOISETOWER.sensorRadiusSquared, rc.getTeam());
		if (friends.length == 0 && enemies.length > 1)
			rc.selfDestruct();
	}

	private void updateCenter() {
		MapLocation[] pastrLocations = rc.sensePastrLocations(rc.getTeam());
		if (pastrLocations.length == 0) {
			if (center != loc) {
				calcDistances(loc);
			}
			center = loc;
			return;
		}
		for (MapLocation pastr : pastrLocations) {
			if (pastr.distanceSquaredTo(loc) <= 2) {
				if (center == null || !pastr.equals(center)) {
					calcDistances(pastr);
				}
				center = pastr;
				break;
			}
		}

	}
}
