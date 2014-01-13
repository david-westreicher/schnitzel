package team209;

import java.util.ArrayList;
import java.util.HashMap;

import team209.Graph.Edge;
import team209.Graph.Node;
import battlecode.common.MapLocation;

public class Graph {

	public static final int MAX_NODES = 10000;
	public ArrayList<Node> nodes;
	public HashMap<Integer, HashMap<Integer, Edge>> edges;
	private int width;
	private int height;
	private Node[][] nodeMap;

	public Graph(int[][] map) {
		width = map.length;
		height = map[0].length;
		ArrayList<int[]> rects = makeRects(map);
		nodes = new ArrayList<Node>();
		int id = 0;
		for (int[] rect : rects) {
			Node n = new Node(id, rect);
			nodes.add(n);
			id++;
		}
		initNodeMap(nodes);
		// System.out.println("nodes: " + id);
		buildEdges();
	}

	private void buildEdges() {
		edges = new HashMap<Integer, HashMap<Integer, Edge>>();
		for (Node n : nodes) {
			addEdges(n);
		}
		Util.printEdges(edges);
		// System.out.println("finished building graph");
	}

	public Graph(int width, int height, ArrayList<Node> nodes) {
		this.width = width;
		this.height = height;
		this.nodes = nodes;
		// for (Node n : nodes)
		// System.out.println("node " + n.id + ": " + n.rect[0] + ", "
		// + n.rect[1] + ", " + n.rect[2] + ", " + n.rect[3]);
		// System.out.println("nodes: " + nodes.size());
		initNodeMap(nodes);
		buildEdges();
	}

	public Graph(int width, int height, ArrayList<Node> nodes,
			HashMap<Integer, HashMap<Integer, Edge>> edges) {
		this.width = width;
		this.height = height;
		this.nodes = nodes;
		this.edges = edges;
		//Util.printEdges(edges);
		initNodeMap(nodes);
	}

	private void initNodeMap(ArrayList<Node> nodes) {
		this.nodeMap = new Node[width][height];
		for (Node n : nodes) {
			int[] rec = n.rect;
			for (int i = rec[0]; i <= rec[2]; i++)
				for (int j = rec[1]; j <= rec[3]; j++) {
					nodeMap[i][j] = n;
				}
		}
	}

	private ArrayList<int[]> makeRects(int[][] rectMap) {
		// Util.printMap(rectMap);
		ArrayList<int[]> recs = new ArrayList<int[]>();
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++) {
				int val = rectMap[i][j];
				if (val < -1) {
					int rec[] = generateRec(val, rectMap, i, j);
					// System.out.print("Rectangle for pos " + i + "," + j +
					// ": ");
					// Util.print(rec);
					fill(rectMap, rec, recs.size());
					recs.add(rec);
				}
			}
		Util.printMap(rectMap);
		// System.out.println("Rectangles: " + recs.size());
		return recs;
	}

	private void fill(int[][] rectMap, int[] rec, int val) {
		for (int i = rec[0]; i <= rec[2]; i++)
			for (int j = rec[1]; j <= rec[3]; j++) {
				rectMap[i][j] = val;
			}
	}

	private int[] generateRec(int val, int[][] rectMap, int x, int y) {
		int startX = x;
		int startY = y;
		int endX = x;
		int endY = y;
		while (endX + 1 < width && rectMap[endX + 1][y] == val)
			endX++;
		boolean extend = true;
		while (extend) {
			endY++;
			if (endY > height - 1)
				break;
			for (int i = startX; i <= endX; i++) {
				if (rectMap[i][endY] != val) {
					extend = false;
					break;
				}
			}
		}
		endY--;
		return new int[] { startX, startY, endX, endY };
	}

	private void addEdges(Node n) {
		int[] rect = n.rect;
		int x = 0;
		int y = 0;
		if (rect[1] > 0)
			for (int i = rect[0]; i <= rect[2]; i++) {
				x = i;
				y = rect[1] - 1;
				Node nmap = nodeMap[x][y];
				if (nmap != null) {
					Node n2 = nodes.get(nmap.id);
					addEdge(n, n2, x, y, false);
					i = n2.rect[2];
				}
			}
		if (rect[0] > 0)
			for (int i = rect[1]; i <= rect[3]; i++) {
				x = rect[0] - 1;
				y = i;
				Node nmap = nodeMap[x][y];
				if (nmap != null) {
					Node n2 = nodes.get(nmap.id);
					addEdge(n, n2, x, y, true);
					i = n2.rect[3];
				}
			}
	}

	private void addEdge(Node n1, Node n2, int x, int y, boolean horizontal) {
		int n1num = n1.id;
		int n2num = n2.id;
		addEdgeReal(n1num, n2num, x + (horizontal ? 1 : 0), y
				+ (!horizontal ? 1 : 0));
		addEdgeReal(n2num, n1num, x, y);
	}

	private void addEdgeReal(int n1, int n2, int x, int y) {
		HashMap<Integer, Edge> nodeEdges = edges.get(n1);
		if (nodeEdges == null) {
			nodeEdges = new HashMap<Integer, Edge>();
			edges.put(n1, nodeEdges);
		}
		nodeEdges.put(n2, new Edge(x, y));
	}

	public static class Edge {

		public int x;
		public int y;

		public Edge(int x, int y) {
			this.x = x;
			this.y = y;
		}

	}

	public static class Node {
		public int id;
		public int[] rect;

		public Node(int id, int[] rect) {
			this.id = id;
			this.rect = rect;
		}
	}

	public Node getNode(MapLocation start) {
		return nodeMap[start.x][start.y];
	}

}
