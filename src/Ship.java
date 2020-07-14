import Enums.Dir;
import Enums.Lane;
import Enums.Value;
import Exceptions.InvalidMoveException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Matt on 5/28/2017.
 * The Ship class represents one or more adjacent Tiles in a Board, that together comprise a ship. These tiles may
 * already be confirmed to contain the ship, or they could be unidentified and represent where a ship could potentially
 * fit on the board.
 */
class Ship implements Serializable {

	/*
	TODO:
	 Make size a final int
	 Store Start/End tiles as final Tile properties
	 */

	//========== State ==========//
	final private Board board;					// The parent instance of Board to which this Ship belongs
	final private ArrayList<Tile> tileList;		// An array list of Tiles that compose this Ship
	private ArrayList<Tile> waterTileList;		// An array list of Tiles around this Ship that must be water

	//========== Constructors ==========//

	/**
	 * Creates a new instance of Ship, should be used for ships that occupy a single tile
	 * @param board - The parent instance of Board to which this ship belongs
	 * @param start - The Tile occupied by this Ship
	 */
	Ship(Board board, Tile start) {
		this.board = board;
		tileList = new ArrayList<>();
		tileList.add(start);
	}

	/**
	 * Creates a new instance of Ship, should be used for ships that occupy multiple tiles
	 * @param board - The parent instance of Board to which this Ship belongs
	 * @param start - The northern-most or western-most Tile occupied by this Ship
	 * @param end - The southern-most or eastern-most Tile occupied by this Ship
	 */
	Ship(Board board, Tile start, Tile end) {
		this.board = board;
		Dir dir;
		Tile tile;
		if (start.ROW == end.ROW) dir = Dir.EAST;
		else dir = Dir.SOUTH;
		tile = start.getNeighbor(dir);
		tileList = new ArrayList<>();
		tileList.add(start);
		while (tile != end) {
			tileList.add(tile);
			tile = tile.getNeighbor(dir);
		}
		tileList.add(end);
	}

	//========== Methods ==========//

	/**
	 * Gets the northern-most or western-most Tile occupied by this Ship
	 * @return An instance of Tile
	 */
	private Tile start() {
		return tileList.get(0);
	}

	/**
	 * Gets the southern-most or eastern-most Tile occupied by this Ship
	 * @return An instance of Tile
	 */
	private Tile end() {
		return tileList.get(tileList.size() - 1);
	}

	/**
	 * Gets the number of Tiles that compose this Ship
	 * @return The size of the Ship as an integer
	 */
	public int size() {
		return tileList.size();
	}

	/**
	 * Gets the type of lane occupied by this Ship, either a row or a column
	 * @return A value from the Lane enum
	 */
	public Lane lane() {
		if (size() == 1) return null;
		if (start().ROW == end().ROW) return Lane.ROW;
		else return Lane.COL;
	}

	/**
	 * Gets the list of Tiles that compose this Ship
	 * @return An array list of Tiles
	 */
	public ArrayList<Tile> tiles() {
		return tileList;
	}

	/**
	 * Checks whether or not the Ship is completely identified
	 * @return True if every Tile occupied by this Ship is identified, otherwise false
	 */
	public Boolean isConfirmed() {
		// Check subs
		if (size() == 1) return tileList.get(0).getValue() == Value.SHIP_SUB;
		// Check ships
		else {
			Value start;
			Value end;
			if (lane() == Lane.ROW) {
				start = Value.SHIP_WEST;
				end = Value.SHIP_EAST;
			} else {
				start = Value.SHIP_NORTH;
				end = Value.SHIP_SOUTH;
			}
			for (int i = 0; i < size(); i++) {
				if (i == 0 && start().getValue() != start) return false;
				else if (i == size() - 1 && end().getValue() != end) return false;
				else if (!Tile.isShip(tileList.get(i))) return false;
			}
			return true;
		}
	}

	/**
	 * Checks if this Ship and the other specified Ship can both exist without violating any Battleship puzzle rules
	 * @param other - The other instance of Ship
	 * @return True if the existence of both Ships would not make the puzzle invalid, otherwise false
	 */
	boolean conflicts(Ship other) {
		// Check if ships occupy the same tiles, accounting for water
		int r1 = this.start().ROW - 1;
		int c1 = this.start().COL - 1;
		int r2 = this.end().ROW + 1;
		int c2 = this.end().COL + 1;
		int r3 = other.start().ROW;
		int c3 = other.start().COL;
		int r4 = other.end().ROW;
		int c4 = other.end().COL;
		if (((r1 <= r4) && (r3 <= r2)) && ((c1 <= c4) && ( c3 <= c2))) return true;
		// Check count of ships
		if ((this.size() == other.size()) && (board.getMissingShips(this.size()) < 2)) return true;
		// Check if ships exceed lane sum
		Map<Integer,Integer> rowMap = new HashMap<>();
		Map<Integer,Integer> colMap = new HashMap<>();
		int shipSum;
		int laneSum;
		for (Tile tile1 : this.tiles()) {
			if (!tile1.isShip()) {
				if (rowMap.containsKey(tile1.ROW)) {
					rowMap.put(tile1.ROW,rowMap.get(tile1.ROW)+1);
				} else rowMap.put(tile1.ROW,1);
				if (colMap.containsKey(tile1.COL)) {
					colMap.put(tile1.COL,colMap.get(tile1.COL)+1);
				} else colMap.put(tile1.COL,1);
			}
		}
		for (Tile tile2 : other.tiles()) {
			if (!tile2.isShip()) {
				if (rowMap.containsKey(tile2.ROW)) {
					rowMap.put(tile2.ROW,rowMap.get(tile2.ROW)+1);
				} else rowMap.put(tile2.ROW,1);
				if (colMap.containsKey(tile2.COL)) {
					colMap.put(tile2.COL,colMap.get(tile2.COL)+1);
				} else colMap.put(tile2.COL,1);
			}
		}
		for (int key : rowMap.keySet()) {
			shipSum = board.getSumShip(Lane.ROW, key) + rowMap.get(key);
			laneSum = board.getSum(Lane.ROW, key);
			if (shipSum > laneSum) return true;
		}
		for (int key : colMap.keySet()) {
			shipSum = board.getSumShip(Lane.COL, key) + colMap.get(key);
			laneSum = board.getSum(Lane.COL, key);
			if (shipSum > laneSum) return true;
		}
		return false;
	}

	//========== Mutators ==========//

	/**
	 * Change the values of the Tiles occupied by this Ship so that they are all identified ship parts
	 * @throws InvalidMoveException - If changing the value of the tiles causes the board to become invalid
	 */
	void confirm() throws InvalidMoveException {
		if (size() == 1) {
			start().setValue(Value.SHIP_SUB);
		} else {
			if (lane() == Lane.ROW) {
				start().setValue(Value.SHIP_WEST);
				end().setValue(Value.SHIP_EAST);
				for (int i = 1; i < size() - 1; i++) {
					tileList.get(i).setValue(Value.SHIP_MID_H);
				}
			} else {
				start().setValue(Value.SHIP_NORTH);
				end().setValue(Value.SHIP_SOUTH);
				for (int i = 1; i < size() - 1; i++) {
					tileList.get(i).setValue(Value.SHIP_MID_V);
				}
			}
		}
	}

	//========== Override ==========//
	public String toString() {
		return "Ship: (" + start().ROW + "," + start().COL + ") (Size:" + size() + ") (Lane:" + lane() + ") (Confirmed:" + isConfirmed() + ")";
	}

	public boolean equals(Object o) {
		Ship other = (Ship) o;
		return this.size() == other.size() && this.start().ROW == other.start().ROW && this.start().COL == other.start().COL && this.lane() == other.lane();
	}

	//========== Unused ==========//

	public ArrayList<Tile> waterTiles() {
		// Return if already calculated
		if (waterTileList != null) return waterTileList;
		// Subs
		if (this.size() == 1) {
			for (Tile tile : this.start().getAdjacentNeighbors()) {
				if (tile != null) waterTileList.add(tile);
			}
			// Horizontal ships
		} else if (this.lane() == Lane.ROW) {
			for (int i = 0; i < this.size(); i++) {
				// Add west end tiles
				if (i == 0) {
					for (Tile tile : tileList.get(i).getNeighbors(new Dir[]{Dir.NORTH, Dir.NORTHWEST, Dir.WEST, Dir.SOUTHWEST, Dir.SOUTH})) {
						if (tile != null) waterTileList.add(tile);
					}
					// Add east end tiles
				} else if (i == this.size() - 1) {
					for (Tile tile : tileList.get(i).getNeighbors(new Dir[]{Dir.NORTH, Dir.NORTHEAST, Dir.EAST, Dir.SOUTHEAST, Dir.SOUTH})) {
						if (tile != null) waterTileList.add(tile);
					}
					// Add middle tiles
				} else {
					for (Tile tile : tileList.get(i).getNeighbors(new Dir[]{Dir.NORTH, Dir.SOUTH})) {
						if (tile != null) waterTileList.add(tile);
					}
				}
			}
			// Vertical ships
		} else {
			for (int i = 0; i < this.size(); i++) {
				// Add north end tiles
				if (i == 0) {
					for (Tile tile : tileList.get(i).getNeighbors(new Dir[]{Dir.EAST, Dir.NORTHEAST, Dir.NORTH, Dir.NORTHWEST, Dir.WEST})) {
						if (tile != null) waterTileList.add(tile);
					}
					// Add south end tiles
				} else if (i == this.size() - 1) {
					for (Tile tile : tileList.get(i).getNeighbors(new Dir[]{Dir.EAST, Dir.SOUTHEAST, Dir.SOUTH, Dir.SOUTHWEST, Dir.WEST})) {
						if (tile != null) waterTileList.add(tile);
					}
					// Add middle tiles
				} else {
					for (Tile tile : tileList.get(i).getNeighbors(new Dir[] {Dir.EAST, Dir.WEST})) {
						if (tile != null) waterTileList.add(tile);
					}
				}
			}
		}
		return waterTileList;
	}

}
