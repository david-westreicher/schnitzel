package seedingteam209;

import java.util.LinkedList;

import battlecode.common.MapLocation;

public class OptimizedPathing {
	public static enum PathType {
		HQ_TO_MEETING, MEETING_TO_ATTACK, TO_NEXT_MEETING

	}

	private static class MetaNode {
		private int id;
		private int pathLength = 0;
		private int position[] = new int[] { 0, 0 };
		public int f;
		public int comingFrom;
		public int rectLength;

		public MetaNode(int id) {
			this.id = id;
		}
	}

	private static final int MAX_PORTALS = OptimizedGraph.MAX_RECTS;
	private static final int MAX_PATH_LENGTH = 500;

	private int[][] edges;
	private int[][] map;
	private int[][] edgeInfo;
	private MetaNode[] allNodes;

	public OptimizedPathing(int[][] map) {
		this.map = map;
		this.edges = OptimizedGraph.edges;
		this.edgeInfo = OptimizedGraph.edgeInfo;
		int recNum = OptimizedGraph.numberOfRecs;
		allNodes = new MetaNode[recNum];
		for (int i = 0; i < recNum; i++) {
			allNodes[i] = new MetaNode(i);
		}
	}

	public MapLocation[] path(MapLocation start, MapLocation target) {
		int startNodeID = getNode(start);
		int targetNodeID = getNode(target);
		if (startNodeID == -1 || targetNodeID == -1) {
			return new MapLocation[0];
		}
		MetaNode startNode = allNodes[startNodeID];
		MetaNode targetNode = allNodes[targetNodeID];
		LinkedList<MetaNode> queue = new LinkedList<MetaNode>();
		startNode.position[0] = start.x;
		startNode.position[1] = start.y;
		startNode.comingFrom = -1;
		startNode.pathLength = 0;
		startNode.rectLength = 0;
		targetNode.position[0] = target.x;
		targetNode.position[1] = target.y;
		queue.add(startNode);
		startNode.f = 1;
		// System.out.println("starting pathing from "
		// + Util.valToSymbol(startNode.id) + " to: "
		// + Util.valToSymbol(targetNode.id));
		int rectPathLength = 0;
		loop: while (rectPathLength++ < MAX_PATH_LENGTH && queue.size() > 0) {
			MetaNode n = queue.removeLast();
			// System.out.print("queue: ");
			// for (MetaNode n2 : queue)
			// System.out.print(Util.valToSymbol(n2.id) + ", ");
			// System.out.print("\n");
			// System.out.println("pathing from: " + Util.valToSymbol(n.id)
			// + ", pos: " + n.position[0] + ", " + n.position[1]);
			int id = n.id;
			int[] children = edges[id];
			for (int i = 1; i <= children[0]; i++) {
				int child = children[i];
				MetaNode childNode = allNodes[child];
				if (childNode.f == 0) {
					int edgei[] = edgeInfo[id * MAX_PORTALS + child];
					int edgeX = edgei[0];
					int edgeY = edgei[1];
					childNode.comingFrom = id;
					childNode.pathLength = n.pathLength
							+ Util.distance(n.position[0], n.position[1],
									edgeX, edgeY);
					childNode.f = childNode.pathLength
							+ Util.distance(edgeX, edgeY, target.x, target.y);
					childNode.rectLength = n.rectLength + 1;
					// childNode.f = n.f + 1;
					// System.out.println("pathing to: "
					// + Util.valToSymbol(childNode.id) + " ("
					// + childNode.f + ", " + childNode.pathLength
					// + "), edge: " + edgeX + ", " + edgeY);
					childNode.position[0] = edgeX;
					childNode.position[1] = edgeY;
					if (childNode == targetNode) {
						break loop;
					}
					addToSortedQueue(queue, childNode);
				}
			}
		}
		MetaNode lastNode = targetNode;
		int index = lastNode.rectLength * 2;
		MapLocation locPath[] = new MapLocation[lastNode.rectLength * 2 + 1];
		locPath[index--] = new MapLocation(target.x, target.y);
		while (lastNode.comingFrom != -1) {
			int beginNode = lastNode.id;
			int endNode = lastNode.comingFrom;
			int edgei[] = edgeInfo[beginNode * MAX_PORTALS + endNode];
			int edgeX = edgei[0];
			int edgeY = edgei[1];
			locPath[index--] = new MapLocation(edgeX, edgeY);
			edgei = edgeInfo[endNode * MAX_PORTALS + beginNode];
			edgeX = edgei[0];
			edgeY = edgei[1];
			locPath[index--] = new MapLocation(edgeX, edgeY);
			lastNode = allNodes[lastNode.comingFrom];
			// System.out.print(Util.valToSymbol(beginNode) + " -> ");
		}
		// System.out.print("\n");
		// for (MapLocation ml : locPath) {
		// System.out.println(ml.x + ", " + ml.y);
		// }
		// System.out.println("finished pathing");
		for (MetaNode n : allNodes) {
			n.f = 0;
		}
		return locPath;
	}

	private void addToSortedQueue(LinkedList<MetaNode> queue, MetaNode childNode) {
		int index = 0;
		while (index < queue.size() && queue.get(index).f > childNode.f)
			index++;
		queue.add(index, childNode);
	}

	private int getNode(MapLocation target) {
		return map[target.x][target.y];
	}
}
