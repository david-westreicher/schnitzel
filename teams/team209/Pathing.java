package team209;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

import team209.Graph.Edge;
import team209.Graph.Node;
import battlecode.common.MapLocation;

public class Pathing {

	private static final int MAX_PATH_LENGTH = 500;

	private static class MetaNode {
		public int id;
		private ArrayList<Integer> path = new ArrayList<Integer>();
		private int pathLength = 0;
		private int position[] = new int[] { 0, 0 };
		public int f;

		public MetaNode(int id) {
			this.id = id;
		}
	}

	private HashMap<Integer, HashMap<Integer, Edge>> edges;
	private ArrayList<MetaNode> allNodes;

	public Pathing(ArrayList<Node> nodes,
			HashMap<Integer, HashMap<Integer, Edge>> edges) {
		this.edges = edges;
		this.allNodes = new ArrayList<MetaNode>();
		for (Node n : nodes)
			allNodes.add(new MetaNode(n.id));
	}

	public ArrayList<MapLocation> path(MapLocation start, MapLocation target,
			Graph g) {
		MetaNode startNode = allNodes.get(g.getNode(start).id);
		MetaNode targetNode = allNodes.get(g.getNode(target).id);
		LinkedList<MetaNode> queue = new LinkedList<MetaNode>();
		ArrayList<Integer> shortestPath = new ArrayList<Integer>();
		startNode.position[0] = start.x;
		startNode.position[1] = start.y;
		targetNode.position[0] = target.x;
		targetNode.position[1] = target.y;
		queue.add(startNode);
		startNode.path.clear();
		startNode.path.add(startNode.id);
		// System.out.println("starting pathing");
		int rectPathLength = 0;
		loop: while (rectPathLength++ < MAX_PATH_LENGTH && queue.size() > 0) {
			// System.out.print("queue: ");
			// for (MetaNode n : queue)
			// System.out.print(Util.valToSymbol(n.id) + ", ");
			// System.out.print("\n");
			MetaNode n = queue.removeLast();
			// System.out.println("pathing from: " + Util.valToSymbol(n.id)
			// + ", pos: " + n.position[0] + ", " + n.position[1]);
			HashMap<Integer, Edge> nodeEdges = g.edges.get(n.id);
			for (Integer child : nodeEdges.keySet()) {
				MetaNode childNode = allNodes.get(child);
				if (childNode.path.size() == 0) {
					childNode.path.addAll(n.path);
					childNode.path.add(child);
					Edge edge = nodeEdges.get(child);
					childNode.pathLength = n.pathLength
							+ Util.distance(n.position[0], n.position[1],
									edge.x, edge.y);
					childNode.f = childNode.pathLength
							+ Util.distance(edge.x, edge.y, target.x, target.y);
					// System.out.println("pathing to: "
					// + Util.valToSymbol(childNode.id) + " ("
					// + childNode.f + "), edge: " + edge.x + ", "
					// + edge.y);
					childNode.position[0] = edge.x;
					childNode.position[1] = edge.y;
					if (childNode == targetNode) {
						shortestPath.addAll(childNode.path);
						break loop;
					}
					addToSortedQueue(queue, childNode);
				}
			}
		}
		ArrayList<MapLocation> locPath = new ArrayList<MapLocation>();
		for (int i = 0; i < shortestPath.size() - 1; i++) {
			int beginNode = shortestPath.get(i);
			int endNode = shortestPath.get(i + 1);
			Edge edge = edges.get(beginNode).get(endNode);
			locPath.add(new MapLocation(edge.x, edge.y));
			edge = edges.get(endNode).get(beginNode);
			locPath.add(new MapLocation(edge.x, edge.y));
			// System.out.print(Util.valToSymbol(beginNode) + " -> ");
		}
		locPath.add(new MapLocation(target.x, target.y));
		// System.out.print("\n");
		// System.out.println("finished pathing");
		for (MetaNode n : allNodes) {
			n.pathLength = 0;
			n.f = 0;
			n.path.clear();
		}
		return locPath;
	}

	private void addToSortedQueue(LinkedList<MetaNode> queue, MetaNode childNode) {
		int index = 0;
		while (index < queue.size() && queue.get(index).f > childNode.f)
			index++;
		queue.add(index, childNode);
	}
}
