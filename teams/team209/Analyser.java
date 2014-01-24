package team209;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Analyser {
	private static final int MAX_PASTR_LOCS = 5;
	// the number of samples skipped per horizontal or lateral line
	// most accurate score (SAMPLE_RATE = 1), worse accuracy but faster
	// (SAMPLE_RATE>1)
	private static final int SAMPLE_RATE = 2;
	private static double[][] cowGrowth;
	private static int NOISE_REACH = (int) (Math
			.sqrt(RobotType.NOISETOWER.attackRadiusMaxSquared) / 1.5) + 1;
	private static double priorityqueue[][];
	private static int height;
	private static int width;
	static int[][] moves = new int[][] { new int[] { 1, 1 },
			new int[] { 1, 0 }, new int[] { 1, -1 }, new int[] { 0, -1 },
			new int[] { -1, -1 }, new int[] { -1, 0 }, new int[] { -1, 1 },
			new int[] { 0, 1 } };
	private static MapLocation hqLoc;
	private static float[] midNormal;
	private static float[] mid;
	private static float amp[] = new float[2];

	public static double[][] findBestNoisePos(RobotController rc, int[][] map,
			MapLocation hqloc) throws GameActionException {
		Analyser.width = map.length;
		Analyser.height = map[0].length;
		priorityqueue = new double[MAX_PASTR_LOCS][3];
		Analyser.hqLoc = hqloc;
		cowGrowth = rc.senseCowGrowth();
		calculateMidNormal();
		// remove voids (walls) from cowgrowth
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				if (map[i][j] == -1)
					cowGrowth[i][j] = -1;
		// don't build pastrs near hq
		for (int i = -2; i <= 2; i++) {
			for (int j = -2; j <= 2; j++) {
				int x = hqLoc.x + i;
				int y = hqLoc.y + j;
				if (x >= 0 && x < width && y >= 0 && y < height) {
					cowGrowth[x][y] = -1;
				}
			}
		}

		// possNoisePos sort= 182078 bc
		Util.tick();
		// int maxDist = (int) (Math.max(width, height) / 2.2);
		int noiseReachHalf = NOISE_REACH / 2;
		for (int i = 0; i < width; i += noiseReachHalf) {
			for (int j = 0; j < height; j += noiseReachHalf) {
				if (cowGrowth[i][j] < 0)
					continue;
				float dist = distanceToMidLine(i, j);
				if (dist <= 0)
					continue;
				// cowGrowth[i][j] = -3;
				double score = analyse(i, j);
				double newScore = score * 2 + dist;
				// System.out.println(i + "," + j + ": " + dist + " : " +
				// score);
				if (newScore > priorityqueue[MAX_PASTR_LOCS - 1][2])
					insert(new double[] { i, j, newScore });
			}
		}
		Util.tock("possNoisePos sort");
		// printPriorityQueue();
		// Util.printMap(cowGrowth);
		// System.out.println("analyzed map");
		return priorityqueue;
	}

	private static void calculateMidNormal() {
		mid = new float[] { width / 2, height / 2 };
		float normal[] = new float[] { hqLoc.x - mid[0], hqLoc.y - mid[1] };
		float length = (float) Math.sqrt(normal[0] * normal[0] + normal[1]
				* normal[1]);
		// TODO what happens if hq at midpoint
		normal[0] /= length;
		normal[1] /= length;
		// System.out.println(mid[0] + ", " + mid[1]);
		// System.out.println(normal[0] + ", " + normal[1]);
		// flip normal by 90 degrees
		// float tmp = normal[0];
		// normal[0] = normal[1];
		// normal[1] = -tmp;
		Analyser.midNormal = normal;
	}

	// check
	// http://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line#Vector_formulation
	private static float distanceToMidLine(int i, int j) {
		float dot = (mid[0] - i) * midNormal[0] + (mid[1] - j) * midNormal[1];
		return -dot;
	}

	private static void insert(double[] newLoc) {
		double lastLoc[] = null;
		for (int i = 0; i < MAX_PASTR_LOCS; i++) {
			double[] loc = priorityqueue[i];
			if (lastLoc != null) {
				double[] tmp = priorityqueue[i];
				priorityqueue[i] = lastLoc;
				lastLoc = tmp;
			} else if (loc[2] < newLoc[2]) {
				lastLoc = loc;
				priorityqueue[i] = newLoc;
			}
		}
		// printPriorityQueue();
	}

	private static void printPriorityQueue() {
		System.out.println("PRIORITY QUEUE+##########");
		for (double loc[] : priorityqueue)
			System.out.print(loc[0] + ", " + loc[1] + ", " + loc[2] + "\n");
	}

	private static double analyse(int x, int y) {
		double score = cowGrowth[x][y];
		if (score < 0)
			return 0;
		// include factory milk gain for adjacent cells
		/*
		 * for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) { int
		 * posX = x + i; int posY = y + j; if (posX >= 0 && posX < width && posY
		 * >= 0 && posY < height) { double val = cowGrowth[posX][posY]; if (val
		 * > 0) score += val; } }
		 */
		// 0 ->00->-0.5, -0.5f->-1 -1
		// 1 -> 01->-0.5, 0.5f->-1 1
		// 2 -> 10-> 1 -1
		// 3 -> 11 1 1
		//
		// System.out.println("analyse " + x + ", " + y);
		for (int i = 0; i < 8; i++) {
			int xMove = moves[i][0] * SAMPLE_RATE;// (int) (((i / 2) - 0.5f) *
													// 2);
			int yMove = moves[i][1] * SAMPLE_RATE;// (int) (((i % 2) - 0.5f) *
													// 2);
			int posX = x;
			int posY = y;
			for (int j = 0; j <= NOISE_REACH; j += SAMPLE_RATE) {
				posX += xMove;
				posY += yMove;
				if (posX >= 0 && posX < width && posY >= 0 && posY < height
						&& cowGrowth[posX][posY] >= 0) {
					score += cowGrowth[posX][posY];
					// System.out.println("analysestar " + posX + ", " + posY);
				} else
					break;
			}
		}
		return score;
	}

	public static int[][] senseMap(RobotController rc) {
		int width = Math.min(100, rc.getMapWidth());
		int height = Math.min(100, rc.getMapHeight());
		int[][] localmap = new int[width][height];
		for (int i = width; --i >= 0;)
			for (int j = height; --j >= 0;)
				localmap[i][j] = rc.senseTerrainTile(new MapLocation(i, j))
						.ordinal() - 3;
		MapLocation enemyHQ = rc.senseEnemyHQLocation();
		for (int i = -2; i <= 2; i++) {
			for (int j = -2; j <= 2; j++) {
				int x = enemyHQ.x + i;
				int y = enemyHQ.y + j;
				if (x >= 0 && x < width && y >= 0 && y < height) {
					localmap[x][y] = -1;
				}
			}
		}
		// Util.printMap(localmap);
		return localmap;
	}

}
