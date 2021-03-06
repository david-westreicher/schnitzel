package seedingteam209;

import team209.OptimizedPathing.PathType;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class BroadCaster {

	public static final int GRAPH_BROADCAST_START = 10;
	public static final boolean SEND_EDGES_TOO = false;
	public static final int NOISEPOS_CHANNEL = 200;
	public static final int SWARMPOS_CHANNEL = 100;
	public static final int HQ_STARTROUND_CHANNEL = 0;
	public static final int TYPE_CHANNEL = 0;
	public static final int CURRENT_FREQUENCY_BAND = 1;
	private static final int FREQUENCY_BAND_SIZE = 30001;
	public static final int MEETING_CONSTRUCTED = 65321;
	public static final int SAUSAGE_HEAD = 65322;
	public static final int HQ_STATE = 0;
	public static final int PASTR_BUILDED = 1;
	private static final int PATH_CHANNEL_START = 2;
	public static final int ATTACK_SUCCESSFULL = 65323;
	public static final int NEW_ATTACK = 65324;
	public static final int ATTACK_SWARM_ALIVE = 65322;

	// MAXINT: 2147483647
	// **********00001111
	public static int toInt2(int i0, int i1) {
		return i1 + i0 * 10000;
	}

	public static int[] fromInt2(int i) {
		int ints[] = new int[2];
		ints[1] = i % 10000;
		ints[0] = (i / 10000) % 10000;
		return ints;
	}

	// MAXINT: 2147483647
	// ********0001112233
	public static int toInt4(int... ints) {
		int num = ints[3];
		num += ints[2] * 100;
		num += ints[1] * 10000;
		num += ints[0] * 10000000;
		return num;
	}

	public static int[] fromInt4(int i) {
		int ints[] = new int[4];
		ints[3] = i % 100;
		ints[2] = (i / 100) % 100;
		ints[1] = (i / 10000) % 1000;
		ints[0] = i / 10000000;
		return ints;
	}

	public static void broadCast(RobotController rc, int i, int loc1, int loc2)
			throws GameActionException {
		rc.broadcast(i, toInt2(loc1, loc2));
	}

	public static void broadCast(RobotController rc, MapLocation[] path,
			int frequencyband, PathType pt) throws GameActionException {
		// 0: action
		// 1: hqlength
		// 2 - 10000: hq -> meetpoint
		// 10001: lastlength
		// 10002 - 20000: last_meetpoint -> new_meetpoint
		// 20001: attacklength
		// 20002 - 30000: meetpoint -> attack
		int startChannel = PATH_CHANNEL_START + (frequencyband % 2)
				* FREQUENCY_BAND_SIZE + pt.ordinal() * 10000 + 1;
		int channel = startChannel + 1;
		for (MapLocation ml : path) {
			rc.broadcast(channel++, toInt2(ml.x, ml.y));
		}
		rc.broadcast(startChannel, path.length);
	}

	public static void broadCast(RobotController rc, MapLocation[] path,
			HQPressure.PathType pt) throws GameActionException {
		int startChannel = PATH_CHANNEL_START + pt.ordinal() * 10000 + 1;
		int channel = startChannel + 1;
		for (MapLocation ml : path) {
			rc.broadcast(channel++, toInt2(ml.x, ml.y));
		}
		rc.broadcast(startChannel, path.length);
	}

	public static MapLocation[] readPath(RobotController rc,
			HQPressure.PathType pt) throws GameActionException {
		int startChannel = PATH_CHANNEL_START + pt.ordinal() * 10000 + 1;
		int pathlength = rc.readBroadcast(startChannel);
		if (pathlength == 0)
			return new MapLocation[0];
		MapLocation[] ml = new MapLocation[pathlength];
		for (int i = 0; i < pathlength; i++) {
			int[] xy = fromInt2(rc.readBroadcast(startChannel + 1 + i));
			ml[i] = new MapLocation(xy[0], xy[1]);
		}
		return ml;
	}

	public static MapLocation[] readPath(RobotController rc, int frequency,
			PathType pt) throws GameActionException {
		int startChannel = PATH_CHANNEL_START + (frequency % 2)
				* FREQUENCY_BAND_SIZE + pt.ordinal() * 10000 + 1;
		int pathlength = rc.readBroadcast(startChannel);
		if (pathlength == 0)
			return new MapLocation[0];
		MapLocation[] ml = new MapLocation[pathlength];
		for (int i = 0; i < pathlength; i++) {
			int[] xy = fromInt2(rc.readBroadcast(startChannel + 1 + i));
			ml[i] = new MapLocation(xy[0], xy[1]);
		}
		return ml;
	}

	public static void broadCast(RobotController rc, int frequency,
			MapLocation add) throws GameActionException {
		broadCast(rc, frequency, add.x, add.y);
	}

	public static HQPressure.States readState(RobotController rc)
			throws GameActionException {
		return HQPressure.States.values()[rc
				.readBroadcast(BroadCaster.HQ_STATE)];
	}

	public static void broadCastState(RobotController rc,
			HQPressure.States currentState) throws GameActionException {
		rc.broadcast(BroadCaster.HQ_STATE, currentState.ordinal());
	}
}
