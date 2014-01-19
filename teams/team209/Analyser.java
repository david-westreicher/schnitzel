package team209;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Analyser {
	private static final int MAX_PASTR_LOCS = 5;
	private static int[][] map;
	private static double[][] cowGrowth;
	private static int NOISE_REACH = (int) (Math
			.sqrt(RobotType.NOISETOWER.attackRadiusMaxSquared) / 1.5);
	private static PriorityQueue<double[]> possNoisePos;
	private static double priorityqueue[][];
	private static int height;
	private static int width;
	static int[][] moves = new int[][] { new int[] { 1, 1 },
			new int[] { 1, -1 }, new int[] { -1, 1 }, new int[] { -1, -1 },
			new int[] { 1, 0 }, new int[] { -1, 0 }, new int[] { 0, 1 },
			new int[] { 0, -1 } };

	public static double[][] findBestNoisePos(RobotController rc, int[][] map,
			MapLocation hqloc) throws GameActionException {
		Analyser.width = map.length;
		Analyser.height = map[0].length;
		priorityqueue = new double[MAX_PASTR_LOCS][3];
		Analyser.map = map;
		cowGrowth = rc.senseCowGrowth();
		// remove voids (walls) from cowgrowth
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				if (map[i][j] == -1)
					cowGrowth[i][j] = -1;

		// TODO
		// remove noisetowers further away from hq than enemyhq
		Util.tick();
		// int maxDist = (int) (Math.max(width, height) / 2.2);
		int noiseReachHalf = NOISE_REACH / 2;
		for (int i = noiseReachHalf; i < width; i += noiseReachHalf) {
			for (int j = noiseReachHalf; j < height; j += noiseReachHalf) {
				// if (Util.distance(i, j, hqloc.x, hqloc.y) >= maxDist)
				// continue;
				// cowGrowth[i][j] = -3;
				double newScore = analyse(i, j);
				if (newScore > priorityqueue[MAX_PASTR_LOCS - 1][2])
					insert(new double[] { i, j, newScore });
				// possNoisePos.add();
			}
		}
		/* Collections.sort(possNoisePos, ); */
		Util.tock("possNoisePos sort");

		return priorityqueue;
		// Util.printMap(cowGrowth);
		// System.out.println("analyzed map");
	}

	private static void insert(double[] newLoc) {
		// printPriorityQueue();
		// System.out.print("inserting: " + newLoc[0] + ", " + newLoc[1] + ", "
		// + newLoc[2] + "\n");
		double lastLoc[] = null;
		for (int i = 0; i < MAX_PASTR_LOCS; i++) {
			double[] loc = priorityqueue[i];
			if (lastLoc != null) {
				double[] tmp = priorityqueue[i];
				priorityqueue[i] = lastLoc;
				lastLoc = tmp;
				// System.out.print("tmp: " + tmp[0] + ", " + tmp[1] + ", "
				// + tmp[2] + "\n");
				// System.out.print("priorityqueue[i]: " + priorityqueue[i][0]
				// + ", " + priorityqueue[i][1] + ", "
				// + priorityqueue[i][2] + "\n");
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
			int xMove = moves[i][0];// (int) (((i / 2) - 0.5f) * 2);
			int yMove = moves[i][1];// (int) (((i % 2) - 0.5f) * 2);
			int posX = x;
			int posY = y;
			for (int j = 0; j <= NOISE_REACH; j++) {
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

}
