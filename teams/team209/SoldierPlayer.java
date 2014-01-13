package team209;

import java.util.ArrayList;

import team209.Graph.Edge;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class SoldierPlayer extends Player {

	public enum Type {
		FACTORY, CLEANER, FARMER, SOLDIER
	}

	private RobotController rc;
	private Pathing pathing;
	private ArrayList<MapLocation> path;
	private Graph graph;
	private MapLocation currentTargetLoc;
	private MapLocation toLoc;
	private Type type = Type.SOLDIER;
	private boolean constructed;
	private boolean reached;
	private MapLocation maxLoc;
	private boolean sweeping = true;

	public SoldierPlayer(RobotController rc) throws GameActionException {
		this.rc = rc;
		for (int i = 0; i < 6; i++) {
			int msg = rc.readBroadcast(i);
			if (i == 0) {
				int[] loc = BroadCaster.fromInt2(msg);
				maxLoc = new MapLocation(loc[0], loc[1]);
			}
			if (msg / 100000000 == 0) {
				if (i == 0)
					type = Type.FACTORY;
				else if (i == 1)
					type = Type.CLEANER;
				else
					type = Type.FARMER;
				int[] loc = BroadCaster.fromInt2(msg);
				toLoc = new MapLocation(loc[0], loc[1]);
				rc.broadcast(i, msg + 100000000);
				break;
			}
		}
		if (toLoc == null) {
			toLoc = Util.closest(rc.getLocation(),
					rc.sensePastrLocations(rc.getTeam().opponent()));
			if (toLoc == null)
				toLoc = rc.senseEnemyHQLocation();
		}
		rc.setIndicatorString(0, type == null ? "no type" : type.toString());
		initialize();
	}

	private void initialize() throws GameActionException {
		// System.out.println("start receiving graph");
		graph = BroadCaster.receiveGraph(rc);
		// System.out.println("end receiving graph");
		// System.out.println("start building path");
		pathing = new Pathing(graph.nodes, graph.edges);
		// System.out.println("end building path");
		// System.out.println("start generating path");
		path = pathing.path(rc.getLocation(), toLoc, graph);
		// System.out.println("end generating path");
		if (path.size() > 0) {
			currentTargetLoc = path.remove(0);
		} else
			System.out.println("no path found");

	}

	public void run() throws GameActionException {
		if (constructed) {
			if (rc.isActive() && !rc.isConstructing())
				rc.construct(RobotType.PASTR);
			return;
		}
		rc.setIndicatorString(2, reached ? "reached" : "not reached");
		MapLocation loc = rc.getLocation();
		if (Shooting.tryToShoot(rc))
			return;
		if (!reached)
			pathToNextLocation(loc);
		else {
			rc.setIndicatorString(2, sweeping ? "sweeping" : "not sweeping");
			if (type == Type.CLEANER) {
				if (sweeping) {
					if (Util.distance(loc.x, loc.y, maxLoc.x, maxLoc.y) > 1) {
						dynamicMove(loc, maxLoc, false);
					} else
						sweeping = false;
				} else {
					if (Util.distance(loc.x, loc.y, toLoc.x, toLoc.y) > 0) {
						dynamicMove(loc, toLoc, true);
					} else
						sweeping = true;
				}
			}
			if (type == Type.FARMER) {
				if (sweeping) {
					if (Util.distance(loc.x, loc.y, maxLoc.x, loc.y) > 1) {
						dynamicMove(loc, new MapLocation(maxLoc.x, loc.y),
								false);
					} else
						sweeping = false;
				} else {
					if (Util.distance(loc.x, loc.y, toLoc.x, toLoc.y) > 0) {
						dynamicMove(loc, toLoc, true);
					} else
						sweeping = true;
				}
			}
			if (type == Type.SOLDIER) {
				reached = false;
				toLoc = Util.closest(rc.getLocation(),
						rc.sensePastrLocations(rc.getTeam().opponent()));
				if (toLoc == null)
					toLoc = rc.senseEnemyHQLocation();
				path = pathing.path(rc.getLocation(), toLoc, graph);
			}
		}
	}

	private void pathToNextLocation(MapLocation loc) throws GameActionException {
		if (currentTargetLoc == null
				|| Util.distance(currentTargetLoc.x, currentTargetLoc.y, loc.x,
						loc.y) < 2) {
			if (path.size() != 0)
				currentTargetLoc = path.remove(0);
			else {
				if (type == Type.FACTORY) {
					constructed = true;
				} else {
					reached = true;
					return;
				}
			}
		}
		dynamicMove(loc, currentTargetLoc, false);
		// Direction toDir = loc.directionTo(currentTargetLoc);
		// if (rc.canMove(toDir)) {
		// if (rc.isActive())
		// rc.move(toDir);
		// } else {
		// rc.setIndicatorString(0, "can't move to: " + toDir + ", currLoc: "
		// + loc + ", targetLoc: " + currentTargetLoc);
		// dynamicMove(loc, currentTargetLoc);
		// }
	}

	private void dynamicMove(MapLocation loc, MapLocation target, boolean sneak)
			throws GameActionException {
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
		if (!tryToMove(bestDir, sneak))
			if (!tryToMove(bestDir + 1, sneak))
				tryToMove(bestDir + Util.VALID_DIRECTIONS.length - 1, sneak);
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
}
