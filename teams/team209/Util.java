package team209;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Util {

	public static final boolean USE_LOGGING = true;
	public static Random RAND;
	private static int start;
	private static int startRound;
	public static final Direction[] VALID_DIRECTIONS = new Direction[] {
			Direction.NORTH, // 0,-1
			Direction.NORTH_EAST, // 1,-1
			Direction.EAST,// 1,0
			Direction.SOUTH_EAST,// 1,1
			Direction.SOUTH, // 0,1
			Direction.SOUTH_WEST,// -1,1
			Direction.WEST,// -1,0
			Direction.NORTH_WEST // -1,-1
	};

	public static void printMap(int[][] map) {
		if (USE_LOGGING) {
			System.out.println("###########MAP############");
			System.out.print("\n");
			for (int j = 0; j < map[0].length; j++) {
				for (int i = 0; i < map.length; i++)
					System.out.print(valToSymbol(map[i][j]) + " ");
				System.out.print("\n");
			}
		}
	}

	public static Character valToSymbol(int val) {
		char symbol = ' ';
		if (val >= 0)
			symbol = (char) ((val % 80) + 33);
		else
			switch (val) {
			case -1:
				symbol = ' ';
				break;
			case -2:
				symbol = '-';
				break;
			case -3:
				symbol = 'x';
				break;
			}
		return symbol;
	}

	public static void print(int[] rec) {
		if (USE_LOGGING) {
			for (int j = 0; j < rec.length; j++)
				System.out.print(rec[j] + ", ");
			System.out.print("\n");
		}
	}

	public static void init(RobotController rc) {
		RAND = new Random(rc.getRobot().getID());
	}

	public static int distance(int i, int j, int x, int y) {
		return Math.max(Math.abs(i - x), Math.abs(j - y));
	}

	public static float distance(float x, float y) {
		return Math.max(Math.abs(x), Math.abs(y));
	}

	public static MapLocation closest(MapLocation loc, MapLocation[] others) {
		if (others.length == 0)
			return null;
		if (others.length == 1)
			return others[0];
		int min = 100 * 100;
		MapLocation closest = null;
		for (int i = 0; i < Math.min(5, others.length); i++) {
			int dist = distance(loc, others[i]);
			if (dist < min) {
				min = dist;
				closest = others[i];
			}
		}
		return closest;
	}

	private static int distance(MapLocation l1, MapLocation l2) {
		return distance(l1.x, l1.y, l2.x, l2.y);
	}

	public static int[] add(int[] loc1, int[] loc2, int i) {
		loc1[0] += loc2[0] * i;
		loc1[1] += loc2[1] * i;
		return loc1;
	}

	public static int[] divide(int[] add, int i) {
		add[0] /= i;
		add[1] /= i;
		return add;
	}

	public static void printMap(double[][] map) {

		if (USE_LOGGING) {
			System.out.println("###########MAP############");
			System.out.print("\n");
			for (int j = 0; j < map[0].length; j++) {
				for (int i = 0; i < map.length; i++)
					System.out.print(valToSymbol((int) map[i][j]) + " ");
				System.out.print("\n");
			}
		}
	}

	public static void tick() {
		start = Clock.getBytecodeNum();
		startRound = Clock.getRoundNum();
	}

	public static void tock(String string) {
		if (USE_LOGGING)
			System.out.println(string
					+ "= "
					+ ((Clock.getRoundNum() - startRound) * 10000 + (Clock
							.getBytecodeNum() - start)) + " bc");
	}

	public static void printEdges(int[][] edges, int numberOfRecs) {
		for (int i = 0; i < numberOfRecs; i++) {
			for (int j = 1; j <= edges[i][0]; j++) {
			}
		}
	}

	public static MapLocation[] mergePaths(MapLocation[] p1, MapLocation[] p2) {
		MapLocation newpath[] = new MapLocation[p1.length + p2.length];
		for (int i = 0; i < p1.length + p2.length; i++) {
			newpath[i] = (i < p1.length) ? p1[i] : p2[i - p1.length];
		}
		return newpath;

	}

}
