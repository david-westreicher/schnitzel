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
	private States currentState;
	private Team opponent;
	private MapLocation meeting;
	private boolean firstSpawn = true;
	private MapLocation attackLoc;

	public HQPressure(RobotController rc) throws GameActionException {
		this.rc = rc;
		this.opponent = rc.getTeam().opponent();
		loc = rc.getLocation();
		currentState = States.HOLD;
		BroadCaster.broadCastState(rc, currentState);
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

	@Override
	public void run() throws GameActionException {
		if (!Shooting.tryToShoot(rc, RobotType.HQ.attackRadiusMaxSquared)) {
			MapLocation[] enemyPastrs = null;
			if (attackLoc != null)
				checkAttackSuccess();
			switch (currentState) {
			case HOLD:
				hold();
				if (rc.senseRobotCount() > HOLD_SOLDIERCOUNT_THRESHOLD)
					changeState(States.MEET);
				break;
			case MEET:
				meet();
				enemyPastrs = rc.sensePastrLocations(opponent);
				if (enemyPastrs.length == 1)
					changeState(States.MILK);
				if (rc.senseRobotCount() > HOLD_SOLDIERCOUNT_THRESHOLD
						&& attackLoc == null && enemyPastrs.length > 1)
					changeState(States.RAGE_MODE);
				break;
			case MILK:
				milk();
				int myPastrs = rc.sensePastrLocations(rc.getTeam()).length;
				int pastrsBuildedOnRadio = rc
						.readBroadcast(BroadCaster.PASTR_BUILDED);
				// rc.setIndicatorString(2, pastrsBuildedOnRadio + "");
				if (myPastrs == 0 && pastrsBuildedOnRadio > 0)
					if (pastrsBuildedOnRadio + BUILD_PASTR_ROUNDS < Clock
							.getRoundNum()) {
						rc.broadcast(BroadCaster.PASTR_BUILDED, 0);
					}
				enemyPastrs = rc.sensePastrLocations(opponent);
				if (attackLoc == null && enemyPastrs.length > 1)
					changeState(States.RAGE_MODE);
				break;
			case RAGE_MODE:
				// rageMode();
				// enemyPastrs = rc.sensePastrLocations(opponent);
				// if (enemyPastrs.length == 0)
				// changeState(States.MEET);
				changeState(States.MEET);
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
			// System.out.println("swarm died");
			return;
		}
		if (rc.readBroadcast(BroadCaster.NEW_ATTACK) != 0)
			rc.broadcast(BroadCaster.NEW_ATTACK, 0);
		if (rc.readBroadcast(BroadCaster.ATTACK_SUCCESSFULL) == 1) {
			MapLocation lastAttackLoc = attackLoc;
			boolean newAttackGenerated = generateAttackPath(attackLoc);
			if (newAttackGenerated) {
				rc.broadcast(BroadCaster.NEW_ATTACK,
						PathType.ATTACK_PATH.ordinal() + 1);
			} else {
				// System.out.println("generating path home from: "
				// + lastAttackLoc + ", to: " + meeting);
				MapLocation[] pathToAttack = p.path(lastAttackLoc, meeting);
				if (pathToAttack != null && pathToAttack.length > 0) {
					BroadCaster.broadCast(rc, pathToAttack,
							PathType.ATTACK_PATH);
					attackLoc = null;
				}
				rc.broadcast(BroadCaster.NEW_ATTACK,
						PathType.BACK_HOME_PATH.ordinal() + 1);
			}
			rc.broadcast(BroadCaster.ATTACK_SUCCESSFULL, 0);
		}
	}

	private void rageMode() {

	}

	private void meet() {

	}

	private void milk() {

	}

	private void changeState(States s) throws GameActionException {
		// System.out.println("Changing state from " + currentState + " to " +
		// s);
		switch (currentState) {
		case HOLD:
			// HOLD->MEET
			meeting = getNextMeetingPoint();
			// TODO what happens if we have no meeting point?
			if (meeting != null) {
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
		MapLocation[] enemyPastrLocations = rc.sensePastrLocations(rc.getTeam()
				.opponent());
		if (from == null)
			from = loc;
		attackLoc = Util.closest(from, enemyPastrLocations);
		// TODO what happens if we have no meeting point?
		if (attackLoc != null) {
			// System.out.println("generateAttackPath");
			MapLocation[] pathToAttack = p.path(from, attackLoc);
			// TODO what happens if we have no meeting point?
			if (pathToAttack != null && pathToAttack.length > 0) {
				BroadCaster.broadCast(rc, pathToAttack, PathType.ATTACK_PATH);
				return true;
			}
		}
		return false;
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

	private void hold() throws GameActionException {
	}

	private boolean tryToSpawn() throws GameActionException {
		if (rc.senseRobotCount() < GameConstants.MAX_ROBOTS)
			for (Direction d : Util.VALID_DIRECTIONS) {
				if (rc.isActive() && rc.canMove(d)) {
					if (firstSpawn) {
						BroadCaster.broadCast(rc, BroadCaster.SAUSAGE_HEAD,
								loc.add(d));
						firstSpawn = false;
					}
					rc.spawn(d);
					return true;
				}
			}
		return false;
	}
}
