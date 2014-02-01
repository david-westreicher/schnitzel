package team209;

public class OptimizedGraph {
	public static final int MAX_RECTS = 2000;
	private static final int MAX_EDGES_PER_NODE = 20;
	public static int[][] map;
	private static int height;
	private static int width;
	public int[] recs;
	public static int[][] edgeInfo = new int[MAX_RECTS * MAX_RECTS][2];
	public static int[][] edges = new int[MAX_RECTS][MAX_EDGES_PER_NODE + 1];
	public static int numberOfRecs;

	private static void makeEdges(int[] recs, int[][] map) {
		int nodeMap[][] = map;
		// System.out.println("numberOfRecs" + numberOfRecs);
		for (int k = 0; k < numberOfRecs; k++) {
			int recIndex = k * 4;
			int startX = recs[recIndex + 0];
			int startY = recs[recIndex + 1];
			int endX = recs[recIndex + 2];
			int endY = recs[recIndex + 3];
			int n = nodeMap[startX][startY];
			int x = 0;
			int y = 0;
			// System.out.print(Util.valToSymbol(k));
			// for (int j = 0; j < 4; j++)
			// System.out.print(recs[recIndex + j] + ",");
			// System.out.print("\n");
			// diagonal
			if (startX > 0) {
				if (startY > 0) {
					x = startX - 1;
					y = startY - 1;
					int diagVal = nodeMap[x][y];
					if (diagVal >= 0 && nodeMap[x + 1][y] != diagVal
							&& nodeMap[x][y + 1] != diagVal) {
						addEdgeDiag(n, diagVal, x, y, false);
					}
				}
				if (endY < height - 1) {
					x = startX - 1;
					y = endY + 1;
					int diagVal = nodeMap[x][y];
					if (diagVal >= 0 && nodeMap[x + 1][y] != diagVal
							&& nodeMap[x][y - 1] != diagVal) {
						addEdgeDiag(n, diagVal, x, y, false);
					}
				}
			}
			// upward scan
			if (startY > 0)
				for (int i = startX; i <= endX; i++) {
					x = i;
					y = startY - 1;
					int n2 = nodeMap[x][y];
					if (n2 >= 0) {
						int otherEnd = recs[n2 * 4 + 2];
						x += Math.min(endX, otherEnd);
						x /= 2;
						addEdge(n, n2, x, y, false);
						i = otherEnd + 1;
					}
				}

			// left scan
			if (startX > 0)
				for (int i = startY; i <= endY; i++) {
					// System.out.print(i + ",");
					x = startX - 1;
					y = i;
					int n2 = nodeMap[x][y];
					if (n2 >= 0) {
						int otherEnd = recs[n2 * 4 + 3];
						y += Math.min(endY, otherEnd);
						y /= 2;
						addEdge(n, n2, x, y, true);
						i = otherEnd + 1;
					}
				}

		}
		// System.out.println("numberOfEdges" + numberOfEdges);
	}

	private static void addEdgeDiag(int n1num, int n2num, int x, int y,
			boolean b) {
		// System.out.println("add diagonal edge from " + Util.valToSymbol(n)
		// + " to " + Util.valToSymbol(n2));
		addEdgeReal(n1num, n2num, x, y);
		addEdgeReal(n2num, n1num, x, y);
	}

	private static void addEdge(int n1num, int n2num, int x, int y,
			boolean horizontal) {
		addEdgeReal(n1num, n2num, (x + (horizontal ? 1 : 0)),
				(y + (!horizontal ? 1 : 0)));
		addEdgeReal(n2num, n1num, x, y);
	}

	private static void addEdgeReal(int n1num, int n2num, int x, int y) {
		if (edges[n1num][0] >= MAX_EDGES_PER_NODE)
			return;
		int index = ++edges[n1num][0];
		edges[n1num][index] = n2num;
		edgeInfo[n1num * MAX_RECTS + n2num][0] = x;
		edgeInfo[n1num * MAX_RECTS + n2num][1] = y;
	}

	public static int[] makeRects(int[][] rectMap) {
		int[] recs = new int[MAX_RECTS * 4];
		int recsIndex = 0;
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++) {
				int val = rectMap[i][j];
				if (val < -1) {
					int newJ = generateRec(val, rectMap, i, j, recsIndex, recs);
					// System.out.println("Rectangle for pos " + i + "," + j +
					// ": ");
					// Util.print(rec);
					recsIndex++;
					j = newJ;
				}
			}
		// Util.printMap(rectMap);
		numberOfRecs = recsIndex;
		// System.out.println("Rectangles: " + recs.size());
		return recs;
	}

	private static int generateRec(int val, int[][] rectMap, int x, int y,
			int fillValue, int[] recs) {
		int startX = x;
		int startY = y;
		int endX = x;
		int endY = y;
		rectMap[startX][startY] = fillValue;
		while (++endX < width && rectMap[endX][y] < -1) {
			rectMap[endX][y] = fillValue;
		}
		endX--;
		boolean extend = true;
		while (extend) {
			endY++;
			if (endY > height - 1)
				break;
			for (int i = startX; i <= endX; i++) {
				if (rectMap[i][endY] >= -1) {
					extend = false;
					break;
				}
			}
			if (extend)
				for (int i = startX; i <= endX; i++)
					rectMap[i][endY] = fillValue;
		}
		endY--;
		int startIndex = fillValue * 4;
		recs[startIndex + 0] = startX;
		recs[startIndex + 1] = startY;
		recs[startIndex + 2] = endX;
		recs[startIndex + 3] = endY;
		// System.out.println("generateRec" + val + ", with "
		// + Util.valToSymbol(fillValue) + ", " + startX + "," + startY
		// + ", " + endX + ", " + endY);
		return endY;
	}

	public static void init(int[][] map) {
		width = map.length;
		height = map[0].length;
		// Util.tick();
		// makeRects from map= 73308 bc for castles
		// makeRects from map= 59704 bc
		// makeRects from map= 54629 bc (ignore roads)
		int[] rects = makeRects(map);
		// Util.tock("makeRects from map");
		// Util.tick();
		// makeEdges from map= 24516 bc for castles
		// makeEdges from map= 22150 bc
		// makeEdges from map= 20822 bc
		// makeEdges from map= 10920 bc (ignore roads)
		makeEdges(rects, map);
		// Util.tock("makeEdges from map");
		// Util.printMap(map);
		// Util.printEdges(edges,numberOfRecs);
	}
}
