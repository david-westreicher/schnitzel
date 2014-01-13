package team209;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class FarmerStrategy {

	private static double[][] cowGrowth;
	private static int height;
	private static int width;

	public static int[] findSpot(RobotController rc, double[][] senseCowGrowth,
			int[][] map, int[] dir) {
		cowGrowth = senseCowGrowth;
		width = map.length;
		height = map[0].length;
		for (int i = 0; i < cowGrowth.length; i++) {
			for (int j = 0; j < cowGrowth[0].length; j++) {
				if (map[i][j] == -1)
					cowGrowth[i][j] = -1;
			}
		}
		// System.out.println("direction to center: " + dir[0] + ", " + dir[1]);
		// printCowGrowth();
		// TODO other orientations
		if (dir[0] > 0) {
			shiftRight();
		} else
			shiftLeft();
		// printCowGrowth();
		if (dir[1] > 0) {
			shiftDown();
		} else
			shiftUp();
		// if (rc.getTeam() == Team.A)
		// printCowGrowth();
		return findMaximum();
	}

	private static void shiftLeft() {
		for (int j = 0; j < height; j++)
			innerLoop: for (int i = 0; i < width; i++) {
				double val = cowGrowth[i][j];
				if (val < 0)
					continue;
				else {
					int savedIndex = i;
					double sum = 0;
					double val2;
					while ((val2 = cowGrowth[i][j]) >= 0) {
						sum += val2;
						cowGrowth[i][j] = 0;
						i++;
						if (i >= width) {
							cowGrowth[savedIndex][j] = sum;
							break innerLoop;
						}
					}
					cowGrowth[savedIndex][j] = sum;
				}
			}
	}

	private static void shiftUp() {
		for (int i = 0; i < width; i++)
			innerLoop: for (int j = 0; j < height; j++) {
				double val = cowGrowth[i][j];
				if (val < 0)
					continue;
				else {
					int savedIndex = j;
					double sum = 0;
					double val2;
					while ((val2 = cowGrowth[i][j]) >= 0) {
						sum += val2;
						cowGrowth[i][j] = 0;
						j++;
						if (j >= height) {
							cowGrowth[i][savedIndex] = sum;
							break innerLoop;
						}
					}
					cowGrowth[i][savedIndex] = sum;
				}
			}
	}

	private static void shiftDown() {
		for (int i = 0; i < width; i++)
			innerLoop: for (int j = height - 1; j >= 0; j--) {
				double val = cowGrowth[i][j];
				if (val < 0)
					continue;
				else {
					int savedIndex = j;
					double sum = 0;
					double val2;
					while ((val2 = cowGrowth[i][j]) >= 0) {
						sum += val2;
						cowGrowth[i][j] = 0;
						j--;
						if (j < 0) {
							cowGrowth[i][savedIndex] = sum;
							break innerLoop;
						}
					}
					cowGrowth[i][savedIndex] = sum;
				}
			}
	}

	private static void shiftRight() {
		for (int j = 0; j < height; j++)
			innerLoop: for (int i = width - 1; i >= 0; i--) {
				double val = cowGrowth[i][j];
				if (val < 0)
					continue;
				else {
					int savedIndex = i;
					double sum = 0;
					double val2;
					while ((val2 = cowGrowth[i][j]) >= 0) {
						sum += val2;
						cowGrowth[i][j] = 0;
						i--;
						if (i < 0) {
							cowGrowth[savedIndex][j] = sum;
							break innerLoop;
						}
					}
					cowGrowth[savedIndex][j] = sum;
				}
			}
	}

	private static void printCowGrowth() {
		System.out.print("\n");
		for (int i = 0; i < cowGrowth.length; i++) {
			for (int j = 0; j < cowGrowth[0].length; j++) {
				String symbol = "" + (int) cowGrowth[j][i];
				if (cowGrowth[j][i] < 0)
					symbol = " ";
				if (cowGrowth[j][i] > 9)
					symbol = "x";
				System.out.print(symbol + " ");
			}
			System.out.print("\n");
		}
	}

	private static int[] findMaximum() {
		int maxLoc[] = new int[2];
		double max = -1;
		for (int i = 0; i < cowGrowth.length; i++) {
			for (int j = 0; j < cowGrowth[0].length; j++) {
				double val = cowGrowth[i][j];
				if (val > max) {
					max = val;
					maxLoc[0] = i;
					maxLoc[1] = j;
				}
			}
		}
		return maxLoc;
	}

}
