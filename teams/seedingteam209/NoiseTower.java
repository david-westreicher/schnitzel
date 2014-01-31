package seedingteam209;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
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
			.sqrt(RobotType.NOISETOWER.attackRadiusMaxSquared) / 1.5) + 1;
	private int currentDiag = 0;
	private int currentDist = 0;

	public NoiseTower(RobotController rc) {
		this.rc = rc;
		this.width = rc.getMapWidth();
		this.height = rc.getMapHeight();
		distances = new int[8];
		loc = rc.getLocation();
		/*
		 * for (int i = 0; i < 4; i++) { int xMove = (int) (((i / 2) - 0.5f) *
		 * 2); int yMove = (int) (((i % 2) - 0.5f) * 2); int posX = loc.x; int
		 * posY = loc.y; int dist = 1; for (int j = 0; j < noiseReach; j++) {
		 * posX += xMove; posY += yMove; TerrainTile tile =
		 * rc.senseTerrainTile(new MapLocation(posX, posY)); if (tile ==
		 * TerrainTile.NORMAL || tile == TerrainTile.ROAD) { dist++; } else
		 * break; } }
		 */
		int attackLoc[] = new int[2];
		for (int i = 0; i < 8; i++) {
			int xMove = Analyser.moves[i][0];
			int yMove = Analyser.moves[i][1];
			int j = noiseReach;
			do {
				attackLoc[0] = loc.x + xMove * j;
				attackLoc[1] = loc.y + yMove * j;
				TerrainTile tt = rc.senseTerrainTile(new MapLocation(
						attackLoc[0], attackLoc[1]));
				if (tt != TerrainTile.OFF_MAP)
					break;
			} while (j-- > 0);
			distances[i] = Math.min(j + 1, noiseReach);
		}
	}

	@Override
	public void run() throws GameActionException {
		int i = currentDiag;

		int xMove = Analyser.moves[i][0];// (int) (((i / 2) - 0.5f) * 2);
		int yMove = Analyser.moves[i][1];// (int) (((i % 2) - 0.5f) * 2);

		MapLocation attack = new MapLocation(loc.x + xMove
				* (distances[i] - currentDist), loc.y + yMove
				* (distances[i] - currentDist));
		if (rc.isActive()) {
			rc.attackSquare(attack);
			currentDist += DIST_PLUS;
			if (distances[i] - currentDist <= 1) {
				currentDiag = (currentDiag + 1) % 8;
				currentDist = 0;
			}
		}
	}
}
