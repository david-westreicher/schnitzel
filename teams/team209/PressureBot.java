package team209;

import team209.HQPlayer4.Action;
import team209.OptimizedPathing.PathType;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class PressureBot extends Player {

	private static final boolean USE_DYNAMIC_MOVE = false;
	private RobotController rc;
	private MapLocation hq;
	private MapLocation lastSquare;
	private MapLocation loc;
	private int moveNum;
	private int frequency;
	private MapLocation[] pathToMeeting;
	private static int bestDirectionArray[] = new int[] { 0, 1, 7, 2, 6, 3, 5 };
	private MapLocation nextLoc;
	private int currentLocIndex;
	private int minDistance = 2;
	private RobotType constructionType;
	private boolean attack;

	public PressureBot(RobotController rc) throws GameActionException {
		this.rc = rc;
		hq = rc.senseHQLocation();
		frequency = this.rc.readBroadcast(BroadCaster.CURRENT_FREQUENCY_BAND);
		lastSquare = new MapLocation(hq.x, hq.y);
	}

	@Override
	public void run() throws GameActionException {
		loc = rc.getLocation();
		if (Shooting.tryToShoot(rc))
			return;
		Action a = BroadCaster.readAction(rc, frequency);
		rc.setIndicatorString(0, "" + a);
		switch (a) {
		case HOLD:
			hold();
			break;
		case MEETING:
			pathToMeeting();
			break;
		case MILKING:
			milking();
			break;
		case ATTACK:
			pathToAttack();
			break;
		}

	}

	private void pathToAttack() throws GameActionException {
		int newAttack = rc.readBroadcast(BroadCaster.NEW_ATTACK);
		rc.setIndicatorString(2, "nonewAttack");
		if (newAttack > 0) {
			rc.setIndicatorString(2, "newAttack");
			pathToMeeting = BroadCaster.readPath(rc, 0,
					PathType.MEETING_TO_ATTACK);
			System.out.println("NEW PATH##############");
			for (MapLocation loc : pathToMeeting)
				System.out.println(loc);
			currentLocIndex = 0;
			if (pathToMeeting != null && pathToMeeting.length > 0) {
				getNextLoc();
			}
			attack = true;
		}
		moveAlongPathing();
	}

	private void milking() {
		// if AUSERWÄHLTER -> make pastr
	}

	private void pathToMeeting() throws GameActionException {
		if (pathToMeeting == null) {
			pathToMeeting = BroadCaster.readPath(rc, 0, PathType.HQ_TO_MEETING);
			if (pathToMeeting != null && pathToMeeting.length > 0) {
				getNextLoc();
			}
		}
		moveAlongPathing();
	}

	private void construct(RobotType type) throws GameActionException {
		this.constructionType = type;
		if (rc.isActive() && !rc.isConstructing())
			rc.construct(type);
	}

	private void moveAlongPathing() throws GameActionException {
		if (nextLoc != null) {
			int dist = Util.distance(nextLoc.x, nextLoc.y, loc.x, loc.y);
			if (dist < minDistance) {
				if (currentLocIndex == pathToMeeting.length) {
					// rc.setIndicatorString(0, "MEETING_CONSTRUCTED: "
					// + constructionLevel);
					if (dist == 0) {
						if (attack) {
							BroadCaster.broadCast(rc,
									BroadCaster.ATTACK_SUCCESSFULL, loc.x,
									loc.y);
						} else {
							rc.broadcast(BroadCaster.MEETING_CONSTRUCTED, 1);
							construct(RobotType.NOISETOWER);
							return;
						}
					}
				}
				getNextLoc();
			}
			dynamicMove(loc, nextLoc, false);
		}
	}

	private boolean dynamicMove(MapLocation loc, MapLocation target,
			boolean sneak) throws GameActionException {
		rc.setIndicatorString(1, "pathing to: " + target);
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
		if (attack) {
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

	private void getNextLoc() throws GameActionException {
		if (currentLocIndex >= pathToMeeting.length)
			return;
		nextLoc = pathToMeeting[currentLocIndex];
		currentLocIndex++;
	}

	private void hold() throws GameActionException {
		Direction dir = loc.directionTo(lastSquare);
		int[] head = BroadCaster.fromInt2(rc
				.readBroadcast(BroadCaster.SAUSAGE_HEAD));
		// rc.setIndicatorString(0, "head: " + head[0] + ", " + head[1]);
		if (!rc.canMove(dir)) {
			moveNum++;
			Direction dirMove = tryToMove(dir.opposite());// Util.RAND.nextBoolean()
															// ? dir
															// .opposite().rotateLeft()
															// :
															// dir.opposite().rotateRight());
			if (dirMove != null) {
				if (head[0] == loc.x && head[1] == loc.y) {
					MapLocation newLoc = loc.add(dirMove);
					BroadCaster.broadCast(rc, BroadCaster.SAUSAGE_HEAD, newLoc);
					// rc.setIndicatorString(1, "newLoc: " + newLoc.x + ", "
					// + newLoc.y);
				}
			}
			lastSquare = loc;
			// rc.setIndicatorString(0, "lastsquare: " + lastSquare);
		}
	}

	private Direction tryToMove(Direction bestDir) throws GameActionException {
		int index = 0;
		// rc.setIndicatorString(1, "bestDir: " + bestDir);
		for (Direction dir : Util.VALID_DIRECTIONS) {
			if (bestDir == dir)
				break;
			index++;
		}
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
