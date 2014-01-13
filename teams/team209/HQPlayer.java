package team209;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.TerrainTile;

public class HQPlayer extends Player {
	private static final int MAX_ROBOTS = GameConstants.MAX_ROBOTS;
	private RobotController rc;
	private int width;
	private int height;
	private Graph graph;
	private int[][] map;
	private int[] maxLoc;
	private int[] directionToCenter;

	public HQPlayer(RobotController rc) throws GameActionException {
		this.rc = rc;
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		initialize();
		MapLocation loc = rc.getLocation();
		directionToCenter = new int[] { (int) Math.signum(loc.x - width / 2),
				(int) Math.signum(loc.y - height / 2) };
		if (directionToCenter[0] == 0)
			directionToCenter[0] = 1;
		if (directionToCenter[1] == 0)
			directionToCenter[1] = 1;
		int startTime = Clock.getRoundNum();
		maxLoc = FarmerStrategy.findSpot(rc, rc.senseCowGrowth(), map,
				directionToCenter);
		System.out.println("Need " + (Clock.getRoundNum() - startTime)+" rounds to find best spot");
		broadCastStrategy();
	}

	public void broadCastStrategy() throws GameActionException {
		int[] cleanerLoc = findMaxMove(maxLoc[0], maxLoc[1], 0,
				-directionToCenter[1]);
		if (directionToCenter[1] > 0)
			cleanerLoc[1] = Math.max(cleanerLoc[1], maxLoc[1] - height / 2);
		else
			cleanerLoc[1] = Math.min(cleanerLoc[1], maxLoc[1] + height / 2);
		// System.out.println("cleaner loc: " + cleanerLoc[0] + ", "
		// + cleanerLoc[1]);
		ArrayList<int[]> farmerLocs = new ArrayList<int[]>();
		// System.out.println("maxLoc: " + maxLoc[0] + ", " + maxLoc[1]);
		// System.out.println("cleanerLoc: " + cleanerLoc[0] + ", "
		// + cleanerLoc[1]);
		for (int i = 0; i < 5; i++) {
			int y = maxLoc[1] - directionToCenter[1] * i
					* GameConstants.MOVEMENT_SCARE_RANGE / 2;
			if (y < 0 || y >= height)
				break;
			int[] farmerLoc = findMaxMove(maxLoc[0], y, -directionToCenter[0],
					0);
			farmerLocs.add(farmerLoc);
			// System.out.println("farmerLoc: " + farmerLoc[0] + ", "
			// + farmerLoc[1]);
		}
		int offset = 0;
		BroadCaster.broadCast(rc, offset++, maxLoc[0], maxLoc[1]);
		BroadCaster.broadCast(rc, offset++, cleanerLoc[0], cleanerLoc[1]);
		for (int[] farmerLoc : farmerLocs)
			BroadCaster.broadCast(rc, offset++, farmerLoc[0], farmerLoc[1]);
	}

	private int[] findMaxMove(int x, int y, int movX, int movY) {
		while (x + movX >= 0 && x + movX < width && y + movY >= 0
				&& y + movY < height && map[x + movX][y + movY] >= 0) {
			x += movX;
			y += movY;
		}
		return new int[] { x, y };
	}

	public void run() throws GameActionException {
		if (!Shooting.tryToShoot(rc))
			tryToSpawn();
	}

	private void tryToSpawn() throws GameActionException {
		if (rc.senseRobotCount() < MAX_ROBOTS)
			for (Direction d : Util.VALID_DIRECTIONS)
				if (rc.canMove(d) && rc.isActive())
					rc.spawn(d);
	}

	private void initialize() throws GameActionException {
		map = new int[width][height];
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				map[i][j] = rc.senseTerrainTile(new MapLocation(i, j))
						.ordinal() - 3;

		int startTime = Clock.getRoundNum();
		graph = new Graph(map);
		System.out.println("Need " + (Clock.getRoundNum() - startTime)+" rounds to build graph");
		rc.setIndicatorString(0, "nodes: " + graph.nodes.size() + ", edges: "
				+ graph.edges.size());
		BroadCaster.broadCast(rc, graph);
	}

}
