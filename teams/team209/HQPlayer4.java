package team209;

import team209.OptimizedPathing.PathType;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class HQPlayer4 extends Player {

	public enum Action {
		DO_NOTHING, NEW_MEETING, ATTACK, WAIT
	}

	private static final int MAX_ROBOTS = GameConstants.MAX_ROBOTS;
	private RobotController rc;
	private OptimizedPathing p;
	private int[][] map;
	private MapLocation loc;
	private MapLocation meeting;
	private int soldierID;
	private double[][] bestNoisePos;
	private int bestNoisePosIndex;
	private MapLocation lastMeeting;
	private int frequencyband = 0;
	private int soldiersAlive;
	private int lastSpawnRound;
	private int lastAttack;

	public HQPlayer4(RobotController rc) throws GameActionException {
		this.rc = rc;
		loc = rc.getLocation();
		BroadCaster.broadCastAction(rc, frequencyband, Action.WAIT);
		tryToSpawn();
		Util.tick();
		// sensing map= 94191 for(60x60)
		// sensing map= 90589 for(60x60)
		// sensing map= 94195 for(60x60)
		// sensing map= 45221 bc for castles
		map = senseMap();
		Util.tock("sensing map");
		OptimizedGraph.init(map);
		Util.tick();
		tryToSpawn();
		// Pathing2 from map= 11479 bc
		// Pathing2 from map= 4661 bc
		p = new OptimizedPathing(map);
		Util.tock("Pathing2 from map");
		tryToSpawn();
		// path from map= 78707 bc
		// path from map= 77580 bc

		// MapLocation to = rc.senseEnemyHQLocation();
		Util.tick();
		// possNoisePos sort= 119061 bc
		// possNoisePos sort= 115933 bc
		// possNoisePos sort= 115545 bc
		// possNoisePos sort= 89488 bc
		bestNoisePos = Analyser.findBestNoisePos(rc, map, loc);
		bestNoisePosIndex = 0;
		Util.tock("findBestNoisePos from map");
		tryToSpawn();
		// Util.tick();
		// generatePathToMeetingPoint();
		// Util.tock("generatePathToMeetingPoint from map");
	}

	private boolean generateAttackMeetingPoint() throws GameActionException {
		MapLocation attackLoc = Util.closest(loc,
				rc.sensePastrLocations(rc.getTeam().opponent()));
		if (attackLoc != null) {
			// System.out.println("generating new attack meeting point");
			MapLocation[] path = Util.mergePaths(p.path(meeting, attackLoc),
					p.path(attackLoc, meeting));
			BroadCaster.broadCast(rc, path, frequencyband - 1,
					PathType.MEETING_TO_ATTACK);
			BroadCaster.broadCastAction(rc, frequencyband - 1, Action.ATTACK);
			return true;
		}
		return false;
	}

	private void generatePathToMeetingPoint() throws GameActionException {
		if (bestNoisePos.length > 0) {
			int index = bestNoisePosIndex;
			lastMeeting = meeting;
			meeting = new MapLocation((int) bestNoisePos[index][0],
					(int) bestNoisePos[index][1]);
			// System.out.println("generating meeting at " + meeting);
			do {
				bestNoisePosIndex = (bestNoisePosIndex + 1)
						% bestNoisePos.length;
			} while (bestNoisePos[bestNoisePosIndex][2] <= 0);
		}
		if (meeting != null) {
			if (lastMeeting != null) {
				// System.out.println("generating nextmeetingpath: " +
				// lastMeeting
				// + ", " + meeting);
				Util.tick();
				MapLocation[] path = p.path(lastMeeting, meeting);
				BroadCaster.broadCast(rc, path, frequencyband - 1,
						PathType.TO_NEXT_MEETING);
				BroadCaster.broadCastAction(rc, frequencyband - 1,
						Action.NEW_MEETING);
				Util.tock("path and broadCast from lastmeeting");
			}
			BroadCaster.broadCastAction(rc, frequencyband, Action.DO_NOTHING);
			Util.tick();
			MapLocation[] pathToMeeting = p.path(loc, meeting);
			Util.tock("path from map");
			Util.tick();
			rc.broadcast(BroadCaster.CURRENT_FREQUENCY_BAND, frequencyband);
			BroadCaster.broadCast(rc, pathToMeeting, frequencyband,
					PathType.HQ_TO_MEETING);
			frequencyband++;
			Util.tock("broadCast from map");
			lastSpawnRound = Clock.getRoundNum();
		}
	}

	private int[][] senseMap() {
		RobotController rc = this.rc;
		int width = Math.min(100, rc.getMapWidth());
		int height = Math.min(100, rc.getMapHeight());
		int[][] localmap = new int[width][height];
		for (int i = width; --i >= 0;)
			for (int j = height; --j >= 0;)
				localmap[i][j] = rc.senseTerrainTile(new MapLocation(i, j))
						.ordinal() - 3;
		MapLocation enemyHQ = rc.senseEnemyHQLocation();
		for (int i = -3; i <= 3; i++) {
			for (int j = -3; j <= 3; j++) {
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

	@Override
	public void run() throws GameActionException {
		if (!Shooting.tryToShoot(rc)) {
			// System.out.println(rc.senseTeamMilkQuantity(rc.getTeam()
			// .opponent()));
			if (rc.isActive() && soldierID % 10 == 0) {
				Util.tick();
				// if attack
				generatePathToMeetingPoint();
				Util.tock("generatePathToMeetingPoint from map");
				rc.broadcast(BroadCaster.MEETING_CONSTRUCTED, 0);
			}
			if (tryToSpawn()) {
				soldierID++;
				soldiersAlive = calculateSoldiersAlive();
				if (soldiersAlive > 8 && lastAttack + 300 < Clock.getRoundNum()) {
					if (generateAttackMeetingPoint()) {
						lastAttack = Clock.getRoundNum();
					}
				}
				// rc.setIndicatorString(0, "soldiers alive: " +
				// soldiersAlive);
			}
		}
	}

	private int calculateSoldiersAlive() {
		int timeToSpawn = Clock.getRoundNum() - lastSpawnRound;
		int soldiersAlive = (int) Math.pow(timeToSpawn
				- GameConstants.HQ_SPAWN_DELAY_CONSTANT_1,
				1.0 / GameConstants.HQ_SPAWN_DELAY_CONSTANT_2);
		lastSpawnRound = Clock.getRoundNum();
		return soldiersAlive + 1;
	}

	private boolean tryToSpawn() throws GameActionException {
		if (rc.senseRobotCount() < MAX_ROBOTS)
			for (Direction d : Util.VALID_DIRECTIONS) {
				if (rc.isActive() && rc.canMove(d)) {
					rc.spawn(d);
					return true;
				}
			}
		return false;
	}

}
