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
	private int noiseReach = (int) (Math
			.sqrt(RobotType.NOISETOWER.attackRadiusMaxSquared) / 1.5);
	private RobotType typeOrder[] = new RobotType[] { RobotType.SOLDIER,
			RobotType.NOISETOWER, RobotType.PASTR, RobotType.SOLDIER,
			RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
			RobotType.SOLDIER };
	private int currentType = 0;
	private ArrayList<double[]> possNoisePos;

	public HQPlayer3(RobotController rc) throws GameActionException {
		this.rc = rc;
		initialize();
	}

	private void initialize() throws GameActionException {
		loc = rc.getLocation();
		// findSwarmLocation();
		findBestNoisePos();
	}

	private void findSwarmLocation() throws GameActionException {
		int[] location = new int[] { loc.x, loc.y };
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		int[] enemylocation = new int[] { enemyLoc.x, enemyLoc.y };
		Util.add(location,
				Util.divide(Util.add(enemylocation, location.clone(), -1), 2),
				+1);
		BroadCaster.broadCast(rc, BroadCaster.SWARMPOS_CHANNEL, location[0],
				location[1]);
	}

	private void findBestNoisePos() throws GameActionException {
		width = rc.getMapWidth();
		height = rc.getMapWidth();
		map = new double[width][height];
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				map[i][j] = rc.senseTerrainTile(new MapLocation(i, j))
						.ordinal();
		cowGrowth = rc.senseCowGrowth();
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				if (map[i][j] == 2)
					cowGrowth[i][j] = -1;
		possNoisePos = new ArrayList<double[]>();
		int maxDist = (int) Math.max(width / 2.2, height / 2.2);
		for (int i = noiseReach / 2; i < width; i += noiseReach / 2) {
			for (int j = noiseReach / 2; j < height; j += noiseReach / 2) {
				if (Util.distance(i, j, loc.x, loc.y) >= maxDist)
					continue;
				// cowGrowth[i][j] = -3;
				double newScore = analyse(i, j);
				if (newScore > 0)
					possNoisePos.add(new double[] { i, j, newScore });
			}
		}
		Collections.sort(possNoisePos, new Comparator<double[]>() {
			@Override
			public int compare(double[] o1, double[] o2) {
				return (int) (o2[2] - o1[2]);
			}
		});
		// Util.printMap(cowGrowth);
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
		// System.out.println("analyzed map");
	}

	private double analyse(int x, int y) {
		double score = 0;
		if (cowGrowth[x][y] < 0)
			return 0;
		for (int i = -1; i <= 1; i++)
			for (int j = -1; j <= 1; j++) {
				int posX = x + i;
				int posY = y + j;
				if (posX >= 0 && posX < width && posY >= 0 && posY < height) {
					double val = cowGrowth[posX][posY];
					if (val > 0)
						score += val;
				}
			}
		// System.out.println("analyse " + x + ", " + y);
		for (int i = 0; i < 4; i++) {
			int xMove = (int) (((i / 2) - 0.5f) * 2);
			int yMove = (int) (((i % 2) - 0.5f) * 2);
			int posX = x;
			int posY = y;
			for (int j = 0; j <= noiseReach; j++) {
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
