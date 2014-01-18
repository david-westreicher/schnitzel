package team209;

import java.util.List;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class HQPlayer4 extends Player {

	private static final int MAX_ROBOTS = GameConstants.MAX_ROBOTS;
	private RobotController rc;
	private OptimizedPathing p;
	private int[][] map;
	private int width;
	private int height;

	public HQPlayer4(RobotController rc) throws GameActionException {
		this.rc = rc;

		Util.tick();
		// sensing map= 94191 for(60x60)
		// sensing map= 90589 for(60x60)
		// sensing map= 94195 for(60x60)
		// sensing map= 45221 bc for castles
		senseMap();
		Util.tock("sensing map");
		OptimizedGraph.init(map);
		Util.tick();
		// Pathing2 from map= 11479 bc
		// Pathing2 from map= 4661 bc
		p = new OptimizedPathing(map);
		Util.tock("Pathing2 from map");
		Util.tick();
		// path from map= 78707 bc
		// path from map= 77580 bc
		MapLocation from = rc.getLocation();
		MapLocation to = rc.senseEnemyHQLocation();
		MapLocation[] path = p
				.path(rc.getLocation(), rc.senseEnemyHQLocation());
		Util.tock("path from map");
		Util.tick();
		BroadCaster.broadCast(rc, path);
		Util.tock("broadCast from map");
	}

	private void senseMap() {
		RobotController rc = this.rc;
		int width = Math.min(100, rc.getMapWidth());
		int height = Math.min(100, rc.getMapHeight());
		int[][] localmap = new int[width][height];
		for (int i = width; --i >= 0;)
			for (int j = height; --j >= 0;)
				localmap[i][j] = rc.senseTerrainTile(new MapLocation(i, j))
						.ordinal() - 3;
		this.map = localmap;
		this.width = width;
		this.height = width;
	}

	@Override
	public void run() throws GameActionException {
		if (!Shooting.tryToShoot(rc)) {
			tryToSpawn();
		}
	}

	private void tryToSpawn() throws GameActionException {
		if (rc.senseRobotCount() < MAX_ROBOTS)
			for (Direction d : Util.VALID_DIRECTIONS) {
				if (rc.canMove(d) && rc.isActive()) {
					rc.spawn(d);
					break;
				}
			}
	}

}
