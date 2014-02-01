package team209;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class HQPressure extends Player {

	private static final int HOLD_SOLDIERCOUNT_THRESHOLD = 5;
	private static final int BUILD_PASTR_ROUNDS = 50;

	public enum PathType {
		HOLD_TO_MEETING, ATTACK_PATH, BACK_HOME_PATH
	}

	public static enum States {
		HOLD, MEET, MILK, RAGE_MODE
	}

	private RobotController rc;
	private MapLocation loc;
	private int[][] map;
	private OptimizedPathing p;
	private double[][] bestNoisePos;
	private int bestNoisePosIndex;
	private States currentState = States.HOLD;
	private Team opponent;
	private MapLocation meeting;
	private boolean firstSpawn = true;
	private MapLocation attackLoc;
	private int attackIndex = 0;
	private int preferredSpawnDirection;
	private MapLocation enemyHQ;
	private Team myTeam;

	public HQPressure(RobotController rc) throws GameActionException {
		this.rc = rc;
		this.myTeam = rc.getTeam();
		this.opponent = rc.getTeam().opponent();
		loc = rc.getLocation();
		enemyHQ = rc.senseEnemyHQLocation();
		BroadCaster.broadCastState(rc, currentState);
		calculatePreferredSpawnDirection();
		tryToSpawn();
		map = Analyser.senseMap(rc);
		tryToSpawn();
		OptimizedGraph.init(map);
		tryToSpawn();
		p = new OptimizedPathing(map);
		tryToSpawn();
		Util.tick();
		bestNoisePos = Analyser.findBestNoisePos(rc, map, loc);
		Util.tock("findBestNoisePos");
		bestNoisePosIndex = 0;
		tryToSpawn();
	}

	private void calculatePreferredSpawnDirection() {
		preferredSpawnDirection = Util.getDirectionIndex(loc.directionTo(
				rc.senseEnemyHQLocation()).opposite());
	}

	@Override
	public void run() throws GameActionException {
		if (!Shooting.tryToShoot(rc, RobotType.HQ.attackRadiusMaxSquared)) {
			MapLocation[] enemyPastrs = null;
			if (attackLoc != null)
				checkAttackSuccess();
			switch (currentState) {
			case HOLD:
				if (rc.senseRobotCount() >= HOLD_SOLDIERCOUNT_THRESHOLD)
					changeState(States.MEET);
				break;
			case MEET:
				enemyPastrs = getEnemyPastrLocations();
				if (enemyPastrs == null)
					// enemy has hq pastrs
					changeState(States.MILK);
				else if (rc.senseRobotCount() > HOLD_SOLDIERCOUNT_THRESHOLD
						&& attackLoc == null && enemyPastrs.length >= 1)
					changeState(States.RAGE_MODE);
				break;
			case MILK:
				int myPastrs = rc.sensePastrLocations(myTeam).length;
				int pastrsBuildedOnRadio = rc
						.readBroadcast(BroadCaster.PASTR_BUILDED);
				// rc.setIndicatorString(2, pastrsBuildedOnRadio + "");
				if (myPastrs == 0 && pastrsBuildedOnRadio > 0)
					if (pastrsBuildedOnRadio + BUILD_PASTR_ROUNDS < Clock
							.getRoundNum()) {
						rc.broadcast(BroadCaster.PASTR_BUILDED, 0);
					}
				enemyPastrs = getEnemyPastrLocations();
				rc.setIndicatorString(0, attackLoc + ", "
						+ (enemyPastrs == null ? "null" : enemyPastrs.length));
				if (attackLoc == null && enemyPastrs != null
						&& enemyPastrs.length >= 1)
					changeState(States.RAGE_MODE);
				break;
			case RAGE_MODE:
				// rageMode();
				// enemyPastrs = rc.sensePastrLocations(opponent);
				// if (enemyPastrs.length == 0)
				// changeState(States.MEET);
				changeState(States.MILK);
				break;
			}
			tryToSpawn();
			// rc.setIndicatorString(0, currentState + "");
		}
	}

	private void checkAttackSuccess() throws GameActionException {
		if (rc.readBroadcast(BroadCaster.ATTACK_SWARM_ALIVE) == 1)
			rc.broadcast(BroadCaster.ATTACK_SWARM_ALIVE, 0);
		else {
			attackLoc = null;
			attackIndex = 0;
			System.out.println("swarm died");
			return;
		}
		if (rc.readBroadcast(BroadCaster.ATTACK_SUCCESSFULL) == 1) {
			MapLocation lastAttackLoc = attackLoc;
			boolean newAttackGenerated = generateAttackPath(attackLoc);
			if (newAttackGenerated) {
				BroadCaster.broadCast(rc, BroadCaster.NEW_ATTACK,
						++attackIndex, PathType.ATTACK_PATH.ordinal());
			} else {
				System.out.println("generating path home from: "
						+ lastAttackLoc + ", to: " + meeting);
				MapLocation[] pathToAttack = p.path(lastAttackLoc, meeting);
				if (pathToAttack != null && pathToAttack.length > 0) {
					BroadCaster.broadCast(rc, pathToAttack,
							PathType.ATTACK_PATH);
					BroadCaster.broadCast(rc, BroadCaster.NEW_ATTACK,
							++attackIndex, PathType.BACK_HOME_PATH.ordinal());
					attackLoc = null;
					// attackIndex = 0;
				}
			}
			rc.broadcast(BroadCaster.ATTACK_SUCCESSFULL, 0);
		}
	}

	private void changeState(States s) throws GameActionException {
		// System.out.println("Changing state from " + currentState + " to " +
		// s);
		switch (currentState) {
		case HOLD:
			// HOLD->MEET
			meeting = getNextMeetingPoint();
			if (meeting == null)
				meeting = Analyser.getRandomMeetingPoint();
			int[] head = BroadCaster.fromInt2(rc
					.readBroadcast(BroadCaster.SAUSAGE_HEAD));
			MapLocation[] pathToMeeting = p.path(new MapLocation(head[0],
					head[1]), meeting);
			// TODO what happens if we have no meeting point?
			if (pathToMeeting != null && pathToMeeting.length > 0) {
				rc.broadcast(BroadCaster.PASTR_BUILDED, 0);
				BroadCaster.broadCast(rc, pathToMeeting,
						PathType.HOLD_TO_MEETING);
			}
			break;
		case MEET:
			// MEET->ATTACK
			if (s == States.RAGE_MODE) {
				generateAttackPath(meeting);
				rc.broadcast(BroadCaster.ATTACK_SWARM_ALIVE, 1);
			}
			break;
		case MILK:
			// MILK->RAGE_MODE
			generateAttackPath(meeting);
			rc.broadcast(BroadCaster.ATTACK_SWARM_ALIVE, 1);
			break;
		}
		currentState = s;
		BroadCaster.broadCastState(rc, currentState);
	}

	private boolean generateAttackPath(MapLocation from)
			throws GameActionException {
		MapLocation[] enemyPastrLocations = getEnemyPastrLocations();
		if (enemyPastrLocations == null)
			return false;
		if (from == null)
			from = loc;
		attackLoc = Util.closest(from, enemyPastrLocations);
		// TODO what happens if we have no meeting point?
		if (attackLoc != null) {
			MapLocation[] pathToAttack = p.path(from, attackLoc);
			// TODO what happens if we have no meeting point?
			if (pathToAttack != null && pathToAttack.length > 0) {
				System.out.println("generateAttackPath " + from + ", to "
						+ attackLoc);
				// for (MapLocation loc : pathToAttack)
				// System.out.println("generateAttackPath " + loc);
				BroadCaster.broadCast(rc, pathToAttack, PathType.ATTACK_PATH);
				return true;
			}
		}
		return false;
	}

	private MapLocation[] getEnemyPastrLocations() {
		int enemyPastrsNum = 0;
		MapLocation[] realEnemyPastrs = rc.sensePastrLocations(opponent);
		for (MapLocation loc : realEnemyPastrs)
			if (Util.distance(loc.x, loc.y, enemyHQ.x, enemyHQ.y) > 2)
				enemyPastrsNum++;
		MapLocation[] enemyPastrs = new MapLocation[enemyPastrsNum];
		for (MapLocation loc : realEnemyPastrs)
			if (Util.distance(loc.x, loc.y, enemyHQ.x, enemyHQ.y) > 2)
				enemyPastrs[--enemyPastrsNum] = loc;
		// he has hq pastrs :(
		if (realEnemyPastrs.length > 0 && enemyPastrs.length == 0) {
			return null;
		}
		return enemyPastrs;
	}

	private MapLocation getNextMeetingPoint() {
		MapLocation meeting = null;
		if (bestNoisePos.length > 0) {
			int index = bestNoisePosIndex;
			meeting = new MapLocation((int) bestNoisePos[index][0],
					(int) bestNoisePos[index][1]);
			// System.out.println("generating meeting at " + meeting);
			do {
				bestNoisePosIndex = (bestNoisePosIndex + 1)
						% bestNoisePos.length;
			} while (bestNoisePos[bestNoisePosIndex][2] <= 0);
		}
		return meeting;
	}

	private boolean tryToSpawn() throws GameActionException {
		if (rc.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			for (int i = preferredSpawnDirection; i < preferredSpawnDirection + 8; i++) {
				Direction d = Util.VALID_DIRECTIONS[i
						% Util.VALID_DIRECTIONS.length];
				if (rc.canMove(d) && rc.isActive()) {
					if (firstSpawn) {
						BroadCaster.broadCast(rc, BroadCaster.SAUSAGE_HEAD,
								loc.add(d));
						firstSpawn = false;
					}
					rc.spawn(d);
					return true;
				}
			}
		}
		return false;
	}
}
