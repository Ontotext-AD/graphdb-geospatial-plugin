//   Copyright (C) 2019 "Sirma AI" JSC, trading as Ontotext

package com.infomatiq.jsi.rtree;

import com.infomatiq.jsi.Rectangle;
import com.ontotext.trree.plugin.geo.Utils;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TLongObjectProcedure;
import gnu.trove.TLongProcedure;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * An extension to RTree implementation from jsi that invokes the matching node with the rectangle instead of
 * only the id
 * 
 * @author Damyan
 * 
 */
public class RTreeWithCoords extends RTree {
	final static int fileMarker = 0xBAD0BAD0;
	private boolean isInViewMode = false;

	private int size = 0;
	private long rootNodeId = 0;

	private TLongObjectHashMap<Node> nodeMap = new TLongObjectHashMap<Node>();

	/**
	 * @see com.infomatiq.jsi.SpatialIndex#intersects(Rectangle, TLongProcedure)
	 */
	public void intersects(Rectangle r, TLongObjectProcedure<Rectangle> v) {
		Node rootNode = getNode(getRootNodeId());
		intersects(r, v, rootNode);
	}

	/**
	 * Recursively searches the tree for all intersecting entries. Immediately calls execute() on the passed
	 * IntProcedure when a matching entry is found.
	 * 
	 * TODO rewrite this to be non-recursive? Make sure it doesn't slow it down.
	 */
	private boolean intersects(Rectangle r, TLongObjectProcedure<Rectangle> v, Node n) {
		for (int i = 0; i < n.entryCount; i++) {
			if (Utils.intersects(r.minX, r.minY, r.maxX, r.maxY, n.entriesMinX[i], n.entriesMinY[i],
					n.entriesMaxX[i], n.entriesMaxY[i])) {
				if (n.isLeaf()) {
					if (!v.execute(n.ids[i], new Rectangle(n.entriesMinX[i], n.entriesMinY[i],
							n.entriesMaxX[i], n.entriesMaxY[i]))) {
						return false;
					}
				} else {
					Node childNode = getNode(n.ids[i]);
					if (!intersects(r, v, childNode)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * @see com.infomatiq.jsi.SpatialIndex#intersects(Rectangle, TLongProcedure)
	 */
	@Override
	public void intersects(Rectangle r, TLongProcedure v) {
		Node rootNode = getNode(getRootNodeId());
		intersects(r, v, rootNode);
	}

	/**
	 * Recursively searches the tree for all intersecting entries. Immediately calls execute() on the passed
	 * IntProcedure when a matching entry is found.
	 * 
	 * TODO rewrite this to be non-recursive? Make sure it doesn't slow it down.
	 */
	private boolean intersects(Rectangle r, TLongProcedure v, Node n) {
		for (int i = 0; i < n.entryCount; i++) {
			if (Utils.intersects(r.minX, r.minY, r.maxX, r.maxY, n.entriesMinX[i], n.entriesMinY[i],
					n.entriesMaxX[i], n.entriesMaxY[i])) {
				if (n.isLeaf()) {
					if (!v.execute(n.ids[i])) {
						return false;
					}
				} else {
					Node childNode = getNode(n.ids[i]);
					if (!intersects(r, v, childNode)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean isInViewMode() {
		return isInViewMode;
	}

	public void save(DataOutputStream out) throws IOException {
		//store file marker
		out.writeInt(fileMarker);
		//store 12 more zeros
		out.writeInt(0);
		out.writeInt(0);
		out.writeInt(0);
		
		// persist tree properties
		out.writeInt(size());
		out.writeLong(getRootNodeId());
		// persist tree nodes
		for (long id = 0; id <= getHighestUsedNodeId(); id++) {
			Node currentNode = getNode(id);
			if (currentNode != null) {
				// persist node ID
				out.writeLong(id);
				// persist the node properties
				out.writeInt(currentNode.ids.length);
				out.writeInt(currentNode.entryCount);
				out.writeInt(currentNode.level);
				// persist the node surrounding rectangle
				out.writeFloat(currentNode.mbrMinX);
				out.writeFloat(currentNode.mbrMinY);
				out.writeFloat(currentNode.mbrMaxX);
				out.writeFloat(currentNode.mbrMaxY);

				// persist the node data
				for (int entry = 0; entry < currentNode.entryCount; entry++) {
					out.writeLong(currentNode.ids[entry]);
					out.writeFloat(currentNode.entriesMinX[entry]);
					out.writeFloat(currentNode.entriesMinY[entry]);
					out.writeFloat(currentNode.entriesMaxX[entry]);
					out.writeFloat(currentNode.entriesMaxY[entry]);
				}
			}
		}
	}

	public void load(DataInputStream in) throws IOException {
		isInViewMode = true;

		// read the filemarker
		int marker = in.readInt();
		if (marker != fileMarker) {
			throw new IOException("bad index or old version detected. please reindex!");
		}
		// read 12bytes more (enough for future extentions)
		in.readInt();
		in.readInt();
		in.readInt();
		// load tree properties
		size = in.readInt();
		rootNodeId = in.readLong();

		// load tree nodes
		while (true) {
			long id;
			int maxNodeEntries, entryCount, level;

			// attempt to read another node
			try {
				id = in.readLong();
			} catch (EOFException eof) {
				break;
			}

			maxNodeEntries = in.readInt();
			entryCount = in.readInt();
			level = in.readInt();

			Node currentNode = new Node(id, level, maxNodeEntries);
			currentNode.entryCount = entryCount;

			// restore the node surrounding rectangle
			currentNode.mbrMinX = in.readFloat();
			currentNode.mbrMinY = in.readFloat();
			currentNode.mbrMaxX = in.readFloat();
			currentNode.mbrMaxY = in.readFloat();

			// restore the node data
			for (int entry = 0; entry < currentNode.entryCount; entry++) {
				currentNode.ids[entry] = in.readLong();
				currentNode.entriesMinX[entry] = in.readFloat();
				currentNode.entriesMinY[entry] = in.readFloat();
				currentNode.entriesMaxX[entry] = in.readFloat();
				currentNode.entriesMaxY[entry] = in.readFloat();
			}

			nodeMap.put(id, currentNode);
		}
	}

	private void checkModification() {
		if (isInViewMode()) {
			throw new IllegalStateException(
					"Modification not allowed once the index was persisted and restored");
		}
	}

	@Override
	public void add(Rectangle r, long id) {
		checkModification();
		super.add(r, id);
	}

	@Override
	public boolean delete(Rectangle r, long id) {
		checkModification();
		return super.delete(r, id);
	}

	@Override
	public Node getNode(long id) {
		return isInViewMode() ? nodeMap.get(id) : super.getNode(id);
	}

	@Override
	public long getRootNodeId() {
		return isInViewMode() ? rootNodeId : super.getRootNodeId();
	}

	@Override
	public int size() {
		return isInViewMode() ? size : super.size();
	}
}
