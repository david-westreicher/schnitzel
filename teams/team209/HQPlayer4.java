package team209;

import team209.OptimizedPathing.PathType;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class HQPlayer4 extends Player {

	public enum Action {
		DO_NOTHING, NEW_MEETING
	}

	private static final int MAX_ROBOTS = GameConstants.MAX_ROBOTS;
	private RobotController rc;
	private OptimizedPathing p;
	private int[][] map;
	private MapLocation loc;
	private MapLocation lastClosestPastr;
	private MapLocation meeting;
	private int soldierType;
	private double[][] bestNoisePos;
	private int bestNoisePosIndex;
	private MapLocation lastMeeting;
	private int frequencyband = 0;

	public HQPlayer4(RobotController rc) throws GameActionException {
		this.rc = rc;
		loc = rc.getLocation();
		Util.tick();
		// sensing map= 94191 for(60x60)
		// sensing map= 90589 for(60x60)
		// sensing map= 94195 for(60x60)
		// sensing map= 45221 bc for castles
		map = senseMap();
		Util.tock("sensing map");
		OptimizedGraph.init(map);
		Util.tick();
		// Pathing2 from map= 11479 bc
		// Pathing2 from map= 4661 bc
		p = new OptimizedPathing(map);
		Util.tock("Pathing2 from map");
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
		// Util.tick();
		// generatePathToMeetingPoint();
		// Util.tock("generatePathToMeetingPoint from map");
	}

	private void generatePathToMeetingPoint() throws GameActionException {
		if (bestNoisePos.length > 0) {
			int index = bestNoisePosIndex;
			lastMeeting = meeting;
			meeting = new MapLocation((int) bestNoisePos[index][0],
					(int) bestNoisePos[index][1]);
			System.out.println("generating meeting at " + meeting);
			bestNoisePosIndex = (bestNoisePosIndex + 1) % bestNoisePos.length;
		}
		if (meeting != null) {
			if (lastMeeting != null) {
				// System.out.println("generating nextmeetingpath: " +
				// lastMeeting
				// + ", " + meeting);
				Util.tick();
				BroadCaster.broadCast(rc, p.path(lastMeeting, meeting),
						frequencyband - 1, PathType.TO_NEXT_MEETING);
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
		}
	}

	private void generatePathToAttack() throws GameActionException {
		MapLocation closestPastr = Util.closest(loc,
				rc.sensePastrLocations(rc.getTeam().opponent()));
		if (closestPastr != null) {
			MapLocation[] pathToAttack = p.path(meeting, closestPastr);
			BroadCaster.broadCast(rc, pathToAttack, frequencyband,
					PathType.MEETING_TO_ATTACK);
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
			if (rc.isActive()) {
				if (soldierType % 8 == 0) {
					Util.tick();
					generatePathToMeetingPoint();
					Util.tock("generatePathToMeetingPoint from map");
					rc.broadcast(BroadCaster.MEETING_CONSTRUCTED, 0);
				}
				if (tryToSpawn()) {
					soldierType++;
				}
			}
		}
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
