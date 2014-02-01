package team209;

import team209.HQPressure.States;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class SoldierPressure extends Player {

	private static final int bestDirectionArray[] = new int[] { 0, 1, 7, 2, 6,
			3, 5 };
	private static final int MIN_DISTANCE = 2;
	private static final double RETREAT_HEALTH_THRESHOLD = 50;
	private static final int MAX_RAGEMODE_TIME = 500;
	private RobotController rc;
	private MapLocation hq;
	private States currentState;
	private States lastState = null;
	private MapLocation lastSquare;
	private MapLocation loc;
	private MapLocation[] currentPath;
	private int currentLocIndex;
	private MapLocation nextLoc;
	private double hp;
	private int attackIndex = 1;
	private int niceMoveRIGHT[] = new int[] { 0, 1, 7, 2, 6, 3, 5, 4 };
	private int niceMoveLEFT[] = new int[] { 0, 7, 1, 6, 2, 5, 3, 4 };
	private Micro m;
	private int timeInRageMode;

	public SoldierPressure(RobotController rc) throws GameActionException {
		this.rc = rc;
		hq = rc.senseHQLocation();
		currentState = States.HOLD;
		attackIndex = BroadCaster.fromInt2(rc
				.readBroadcast(BroadCaster.NEW_ATTACK))[0] + 1;
		m = new Micro(rc);
	}

	@Override
	public void run() throws GameActionException {
		loc = rc.getLocation();
		// try to retreat
		hp = rc.getHealth();
		if (currentState == States.RAGE_MODE)
			if (rc.readBroadcast(BroadCaster.ATTACK_SWARM_ALIVE) == 0)
				rc.broadcast(BroadCaster.ATTACK_SWARM_ALIVE, 1);
		// try to shoot
		States hqState = BroadCaster.readState(rc);
		rc.setIndicatorString(0, "hqState: " + hqState + ", state: "
				+ currentState);
		switch (currentState) {
		case HOLD:
			hold();
			if (hqState == HQPressure.States.MEET) {
				changeState(HQPressure.States.MEET);
			}
			if (hqState == HQPressure.States.MILK)
				changeState(HQPressure.States.MEET);
			break;
		case MEET:
			moveAlongPath();
			if (hqState == HQPressure.States.MILK)
				changeState(HQPressure.States.MILK);
			if (hqState == HQPressure.States.RAGE_MODE)
				changeState(HQPressure.States.RAGE_MODE);
			break;
		case MILK:
			moveAlongPath();
			if (hqState == HQPressure.States.RAGE_MODE)
				changeState(HQPressure.States.RAGE_MODE);
			break;
		case RAGE_MODE:
			if (timeInRageMode++ > MAX_RAGEMODE_TIME)
				changeState(HQPressure.States.MILK);
			checkForNewAttack();
			moveAlongPath();
			break;
		}
		lastState = currentState;
	}

	private void checkForNewAttack() throws GameActionException {
		int[] newAttack = BroadCaster.fromInt2(rc
				.readBroadcast(BroadCaster.NEW_ATTACK));
		rc.setIndicatorString(2, "no new pathType");
		if (newAttack[0] != attackIndex)
			return;
		timeInRageMode = 0;
		attackIndex++;
		team209.HQPressure.PathType pathType = HQPressure.PathType.values()[newAttack[1]];
		rc.setIndicatorString(2, "pathType: " + pathType + ", attackIndex: "
				+ attackIndex);
		System.out.println("new attack " + pathType + ", " + newAttack[0]
				+ ", " + attackIndex);
		switch (pathType) {
		case ATTACK_PATH:
			MapLocation[] newAttackPath = BroadCaster.readPath(rc,
					HQPressure.PathType.ATTACK_PATH);
			currentPath = Util.mergePaths(currentPath, newAttackPath);
			break;
		case BACK_HOME_PATH:
			MapLocation[] backHomePath = BroadCaster.readPath(rc,
					HQPressure.PathType.ATTACK_PATH);
			currentPath = Util.mergePaths(currentPath, backHomePath);
			currentState = States.MEET;
			// attackIndex = 1;
			break;
		default:
			break;
		}
	}

	private void moveAlongPath() throws GameActionException {
		if (nextLoc != null) {
			int dist = Util.distance(nextLoc.x, nextLoc.y, loc.x, loc.y);
			if (dist < MIN_DISTANCE) {
				if (currentLocIndex == currentPath.length) {
					if (dist == 0) {
						if (finishedPathing())
							return;
					} else if (dist < 2)
						if (closeToFinishedPathing())
							return;
				}
				getNextLoc();
			}
			rc.setIndicatorString(2, "pathing to: " + nextLoc + ", ["
					+ currentLocIndex + "/" + currentPath.length + "], "
					+ timeInRageMode);
			boolean reachedMeeting = false;
			if (currentLocIndex == currentPath.length
					&& (currentState == States.MEET || currentState == States.MILK)
					&& dist < 4) {
				reachedMeeting = true;
			}
			if (reachedMeeting) {
				if (Clock.getRoundNum() % 5 == 0)
					m.move(rc, nextLoc, true);
			} else
				m.move(rc, nextLoc, false);
		}
	}

	private boolean closeToFinishedPathing() throws GameActionException {
		switch (currentState) {
		case MILK:
			if (rc.readBroadcast(BroadCaster.PASTR_BUILDED) == 0) {
				if (construct(RobotType.PASTR)) {
					rc.broadcast(BroadCaster.PASTR_BUILDED, Clock.getRoundNum());
					return true;
				}
			}
			break;
		}
		return false;
	}

	private boolean finishedPathing() throws GameActionException {
		switch (currentState) {
		case MILK:
		case MEET:
			return construct(RobotType.NOISETOWER);
		case RAGE_MODE:
			if (rc.readBroadcast(BroadCaster.ATTACK_SUCCESSFULL) == 0) {
				rc.broadcast(BroadCaster.ATTACK_SUCCESSFULL, 1);
			}
			break;
		}
		return false;
	}

	private boolean construct(RobotType type) throws GameActionException {
		if (rc.senseNearbyGameObjects(Robot.class,
				RobotType.SOLDIER.sensorRadiusSquared, rc.getTeam().opponent()).length < rc
				.senseNearbyGameObjects(Robot.class,
						RobotType.SOLDIER.sensorRadiusSquared, rc.getTeam()).length)
			if (!rc.isConstructing() && rc.isActive()) {
				rc.construct(type);
				return true;
			}
		return false;
	}

	private void changeState(States newState) throws GameActionException {
		switch (currentState) {
		case HOLD:
			// hold -> meeting
			MapLocation[] pathToMeeting = BroadCaster.readPath(rc,
					HQPressure.PathType.HOLD_TO_MEETING);
			currentPath = pathToMeeting;
			getNextLoc();
			break;
		case MEET:
			// meet->ATTACK
			if (newState == States.RAGE_MODE) {
				timeInRageMode = 0;
				MapLocation[] newAttackPath = BroadCaster.readPath(rc,
						HQPressure.PathType.ATTACK_PATH);
				currentPath = Util.mergePaths(currentPath, newAttackPath);
			}
			break;
		case MILK:
			// MILK->ATTACK
			timeInRageMode = 0;
			MapLocation[] newAttackPath = BroadCaster.readPath(rc,
					HQPressure.PathType.ATTACK_PATH);
			currentPath = Util.mergePaths(currentPath, newAttackPath);
			break;
		}
		currentState = newState;
	}

	private void getNextLoc() throws GameActionException {
		if (currentLocIndex >= currentPath.length)
			return;
		nextLoc = currentPath[currentLocIndex];
		currentLocIndex++;
	}

	// HOLD-> spawn soldiers out with pressure, SAUSAGE_HEAD = start for pathing
	// (farthest hold position)
	private void hold() throws GameActionException {
		if (lastState != currentState) {
			lastSquare = new MapLocation(hq.x, hq.y);
		}
		Direction dir = loc.directionTo(lastSquare);
		int[] head = BroadCaster.fromInt2(rc
				.readBroadcast(BroadCaster.SAUSAGE_HEAD));
		if (!rc.canMove(dir)) {
			Direction dirMove = holdMove(dir.opposite());
			if (dirMove != null) {
				if (head[0] == loc.x && head[1] == loc.y) {
					MapLocation newLoc = loc.add(dirMove);
					BroadCaster.broadCast(rc, BroadCaster.SAUSAGE_HEAD, newLoc);
				}
			}
			lastSquare = loc;
			// rc.setIndicatorString(0, "lastsquare: " + lastSquare);
		}
	}

	private Direction holdMove(Direction bestDir) throws GameActionException {
		int index = Util.getDirectionIndex(bestDir);
		index += Util.VALID_DIRECTIONS.length;
		index += Util.RAND.nextBoolean() ? -1 : +1;
		for (int i = 0; i < bestDirectionArray.length; i++) {
			Direction dir = Util.VALID_DIRECTIONS[(bestDirectionArray[i] + index)
					% Util.VALID_DIRECTIONS.length];
			if (rc.isActive() && rc.canMove(dir)) {
				rc.move(dir);
				return dir;
			}
		}
		return null;
	}
}
