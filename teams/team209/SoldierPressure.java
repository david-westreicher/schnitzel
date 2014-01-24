package team209;

import team209.HQPressure.States;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class SoldierPressure extends Player {

	private static final int bestDirectionArray[] = new int[] { 0, 1, 7, 2, 6,
			3, 5 };
	private static final int MIN_DISTANCE = 2;
	// TODO if stuck use dynamic move
	private static boolean USE_DYNAMIC_MOVE = false;
	private static final double RETREAT_HEALTH_THRESHOLD = 50;
	private RobotController rc;
	private MapLocation hq;
	private States currentState;
	private States lastState = null;
	private MapLocation lastSquare;
	private MapLocation loc;
	private MapLocation[] currentPath;
	private int currentLocIndex;
	private MapLocation nextLoc;
	private int stuck;
	private int stuckedAt;
	private double hp;

	public SoldierPressure(RobotController rc) {
		this.rc = rc;
		hq = rc.senseHQLocation();
		currentState = States.HOLD;
	}

	@Override
	public void run() throws GameActionException {
		loc = rc.getLocation();
		// try to retreat
		hp = rc.getHealth();
		if (currentState == States.RAGE_MODE)
			if (rc.readBroadcast(BroadCaster.ATTACK_SWARM_ALIVE) == 0)
				rc.broadcast(BroadCaster.ATTACK_SWARM_ALIVE, 1);
		if (hp < RETREAT_HEALTH_THRESHOLD) {
			Direction retreatDirection = Shooting.retreat(rc, hp,
					RobotType.SOLDIER.sensorRadiusSquared);
			if (retreatDirection != null) {
				if (dynamicMove(loc, loc.add(retreatDirection), false))
					return;
			}
		}
		// try to shoot
		if (Shooting.tryToShoot(rc, RobotType.SOLDIER.attackRadiusMaxSquared))
			return;
		States hqState = BroadCaster.readState(rc);
		// rc.setIndicatorString(0, "hqState: " + hqState + ", state: "
		// + currentState);
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
			checkForNewAttack();
			moveAlongPath();
			break;
		}
		lastState = currentState;
	}

	private void checkForNewAttack() throws GameActionException {
		int newAttack = rc.readBroadcast(BroadCaster.NEW_ATTACK);
		if (newAttack == 0)
			return;
		team209.HQPressure.PathType pathType = HQPressure.PathType.values()[newAttack - 1];
		// rc.setIndicatorString(2, "pathType: " + pathType);
		// System.out.println("new attack " + pathType);
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
					// rc.setIndicatorString(0, "MEETING_CONSTRUCTED: "
					// + constructionLevel);
					if (dist == 0) {
						finishedPathing();
					} else if (dist < 2)
						closeToFinishedPathing();
				}
				getNextLoc();
			}
			// rc.setIndicatorString(1, "pathing to: " + nextLoc + ", ["
			// + currentLocIndex + "/" + currentPath.length + "]");
			boolean hasMoved = dynamicMove(loc, nextLoc, false);
			if (currentLocIndex != currentPath.length) {
				if (!USE_DYNAMIC_MOVE && !hasMoved) {
					stuck++;
					if (stuck > 40) {
						USE_DYNAMIC_MOVE = true;
						stuck = 0;
						stuckedAt = Clock.getRoundNum();
					}
				}
				if (stuckedAt + 10 < Clock.getRoundNum()) {
					USE_DYNAMIC_MOVE = false;
				}
			} else
				USE_DYNAMIC_MOVE = false;
			// rc.setIndicatorString(2, "USE_DYNAMIC_MOVE: " +
			// USE_DYNAMIC_MOVE);
		}
	}

	private void closeToFinishedPathing() throws GameActionException {
		switch (currentState) {
		case MILK:
			if (rc.readBroadcast(BroadCaster.PASTR_BUILDED) == 0) {
				construct(RobotType.PASTR);
				rc.broadcast(BroadCaster.PASTR_BUILDED, Clock.getRoundNum());
			}
			break;
		}
	}

	private void finishedPathing() throws GameActionException {
		switch (currentState) {
		case MILK:
		case MEET:
			construct(RobotType.NOISETOWER);
			break;
		case RAGE_MODE:
			if (rc.readBroadcast(BroadCaster.ATTACK_SUCCESSFULL) == 0)
				rc.broadcast(BroadCaster.ATTACK_SUCCESSFULL, 1);
			break;
		}
	}

	private void construct(RobotType type) throws GameActionException {
		if (rc.isActive() && !rc.isConstructing())
			rc.construct(type);
	}

	private void changeState(States newState) throws GameActionException {
		switch (currentState) {
		case HOLD:
			// hold -> meeting
			MapLocation[] pathToMeeting = BroadCaster.readPath(rc,
					HQPressure.PathType.HOLD_TO_MEETING);
			navigate(pathToMeeting);
			break;
		case MEET:
			// meet->milk
			if (newState == States.RAGE_MODE) {
				MapLocation[] pathToAttack = BroadCaster.readPath(rc,
						HQPressure.PathType.ATTACK_PATH);
				navigate(pathToAttack);
			}
			break;
		case MILK:
			// MILK->ATTACK
			MapLocation[] pathToAttack = BroadCaster.readPath(rc,
					HQPressure.PathType.ATTACK_PATH);
			navigate(pathToAttack);
			break;
		}
		currentState = newState;
	}

	private void navigate(MapLocation[] pathToMeeting)
			throws GameActionException {
		if (pathToMeeting != null && pathToMeeting.length > 0) {
			currentPath = pathToMeeting;
			currentLocIndex = 0;
			getNextLoc();
		}
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
		int index = getDirectionIndex(bestDir);
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

	private boolean dynamicMove(MapLocation loc, MapLocation target,
			boolean sneak) throws GameActionException {
		int diffX = -(int) Math.signum(loc.x - target.x);
		int diffY = -(int) Math.signum(loc.y - target.y);
		int bestDir = -1;
		if (diffX == 0)
			if (diffY > 0)
				bestDir = 4;
			else
				bestDir = 0;
		else if (diffX > 0) {
			bestDir = 2 + diffY;
		} else
			bestDir = 6 - diffY;
		if (USE_DYNAMIC_MOVE) {
			int countDir = 0;
			boolean right = (Clock.getRoundNum() / 30) % 2 == 0;
			if (!right)
				bestDir += Util.VALID_DIRECTIONS.length;
			while (countDir++ < Util.VALID_DIRECTIONS.length) {
				if (tryToMove(bestDir, sneak))
					return true;
				else
					bestDir += right ? 1 : -1;
			}
		} else {
			if (!tryToMove(bestDir, sneak))
				if (!tryToMove(bestDir + 1, sneak))
					return tryToMove(
							bestDir + Util.VALID_DIRECTIONS.length - 1, sneak);
		}
		return false;
	}

	private boolean tryToMove(int bestDir, boolean sneak)
			throws GameActionException {
		Direction dir = Util.VALID_DIRECTIONS[bestDir
				% Util.VALID_DIRECTIONS.length];
		if (rc.canMove(dir))
			if (rc.isActive()) {
				if (sneak)
					rc.sneak(dir);
				else
					rc.move(dir);
				return true;
			}
		return false;
	}

	private int getDirectionIndex(Direction retreatDirection) {
		int index = 0;
		for (Direction dir : Util.VALID_DIRECTIONS) {
			if (retreatDirection == dir)
				break;
			index++;
		}
		return index;
	}
}
