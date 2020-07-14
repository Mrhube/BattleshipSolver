import Enums.Dir;
import Enums.Value;
import Exceptions.InvalidMoveException;

import java.io.Serializable;

/**
 * Created by Matt on 7/29/2017.
 */
public class Tile implements Serializable {

	final Board BOARD;
	final int ROW;
	final int COL;

	private Value value;

	/**
	 * Creates a blank tile
	 * @param board - The parent Board object
	 * @param row - The row position on the parent Board
	 * @param col - The column position on the parent Board
	 */
	Tile (Board board, int row, int col) {
		this.BOARD = board;
		this.value = Value.BLANK;
		this.ROW = row;
		this.COL = col;
	}

	//========== Static Functions ==========//

	/**
	 * Checks if the Tile has been confirmed to contain a ship piece
	 * @param tile - The instance of Tile to check
	 * @return True if yes, false if no, false if the Tile is null (a Tile would be null if it was outside the bounds)
	 */
	static boolean isShip(Tile tile) {
		return (tile != null) && tile.isShip();
	}

	/**
	 * Checks if all the specified Tiles have been confirmed to contain water
	 * @param tile - The instance of Tile to check
	 * @return True if yes, false if no, true if the Tile is null (a Tile would be null if it was outside the bounds)
	 */
	static boolean isWater(Tile... tile) {
		for (Tile each : tile) {
			if (each != null && !each.isWater()) return false;
		}
		return true;
		//return (tile == null) || (tile.value == Value.WATER);
	}

	/**
	 * Checks if the Tile is blank
	 * @param tile - The instance of Tile to check
	 * @return true if yes, false if no, false if the Tile is null (a Tile would be null if it was outside the bounds)
	 */
	static boolean isBlank(Tile tile) {
		return (tile != null) && tile.isBlank();
	}

	/**
	 * Checks if the Tile is an unidentified ship part
	 * @param tile - The instance of Tile to check
	 * @return true if yes, false if no, false if the Tile is null (a Tile would be null if it was outside the bounds)
	 */
	static boolean isUnid(Tile tile) {
		return (tile != null) && tile.isUnid();
	}

	//========== Methods ==========//

	/**
	 * Check if this Tile has been confirmed to contain a ship piece
	 * @return True if yes, otherwise false
	 */
	boolean isShip() {
		return value != Value.BLANK && value != Value.WATER;
	}

	/**
	 * Check if this Tile has been confirmed to contain water
	 * @return True if yes, otherwise false
	 */
	boolean isWater() {
		return value == Value.WATER;
	}

	/**
	 * Check if this Tile is blank
	 * @return True if yes, otherwise false
	 */
	boolean isBlank() {
		return value == Value.BLANK;
	}

	/**
	 * Check if this Tile is an unidentified ship part
	 * @return True if yes, otherwise false
	 */
	boolean isUnid() {
		return value == Value.SHIP_UNID || value == Value.SHIP_MID;
	}

	/*========== Public Functions ==========*/
	/**
	 * Attempts to change a tile's value and update neighboring tiles appropriately
	 * @param val - The value to change to
	 * @throws InvalidMoveException if the new value is not a valid move
	 */
	void setValue(Value val) throws InvalidMoveException {
		tryValue(val);
		for (Dir dir : getWaterDirections()) {
			Tile tile = getNeighbor(dir);
			if (tile != null) tile.tryValue(Value.WATER);
		}
		for (Dir dir : getShipDirections()) {
			Tile tile = getNeighbor(dir);
			if (tile != null && !Tile.isShip(tile)) tile.setValue(Value.SHIP_UNID);
		}
	}

	/**
	 * Get this Tile's value
	 * @return A Value enum
	 */
	Value getValue() {
		return value;
	}

	/**
	 * Gets the neighboring Tile in the specified direction
	 * @param dir - The direction of the desired neighbor
	 * @return The neighboring Tile if it is within bounds, otherwise null
	 */
	Tile getNeighbor(Dir dir) {
		try {
			switch (dir) {
				case NORTH:
					return BOARD.TILES[ROW - 1][COL];
				case SOUTH:
					return BOARD.TILES[ROW + 1][COL];
				case EAST:
					return BOARD.TILES[ROW][COL + 1];
				case WEST:
					return BOARD.TILES[ROW][COL - 1];
				case NORTHEAST:
					return BOARD.TILES[ROW - 1][COL + 1];
				case NORTHWEST:
					return BOARD.TILES[ROW - 1][COL - 1];
				case SOUTHEAST:
					return BOARD.TILES[ROW + 1][COL - 1];
				case SOUTHWEST:
					return BOARD.TILES[ROW + 1][COL + 1];
			}
			return null;
		} catch	(ArrayIndexOutOfBoundsException e) {return null;}
	}

	/**
	 * Gets an array of neighboring Tiles in the specified directions
	 * @param dir - An array of directions for the desired neighbors
	 * @return An array containing neighboring Tiles or null values
	 */
	Tile[] getNeighbors(Dir[] dir) {
		Tile[] neighbors = new Tile[dir.length];
		for (int i = 0; i < dir.length ; i++) {
			neighbors[i] = getNeighbor(dir[i]);
		}
		return neighbors;
	}

	/**
	 * Gets the neighboring tiles in the cardinal directions
	 * @return An array of tiles or null values
	 */
	Tile[] getAdjacentNeighbors() {
		return new Tile[] {this.getNeighbor(Dir.NORTH),this.getNeighbor(Dir.SOUTH),this.getNeighbor(Dir.EAST),this.getNeighbor(Dir.WEST)};
	}

	/**
	 * Gets an array of neighboring Tiles that are known to contain water based on the value of this Tile
	 * @return An array of Tiles or null values
	 */
	Tile[] getWaterNeighbors() {
		Dir[] waterDir = getWaterDirections();
		Tile[] waterTiles = new Tile[waterDir.length];
		for (int i = 0; i < waterDir.length; i++) {
			waterTiles[i] = getNeighbor(waterDir[i]);
		}
		return waterTiles;
	}

	/**
	 * Gets the direction of neighboring Tiles that must contain water based on this Tile's value
	 * @return An array of directions
	 */
	Dir[] getWaterDirections() {
		switch (value) {
			case SHIP_UNID:
				return new Dir[] {Dir.NORTHEAST, Dir.NORTHWEST, Dir.SOUTHEAST, Dir.SOUTHWEST};
			case SHIP_MID:
				return new Dir[] {Dir.NORTHEAST, Dir.NORTHWEST, Dir.SOUTHEAST, Dir.SOUTHWEST};
			case SHIP_MID_H:
				return new Dir[] {Dir.NORTH, Dir.SOUTH, Dir.NORTHEAST, Dir.NORTHWEST, Dir.SOUTHEAST, Dir.SOUTHWEST};
			case SHIP_MID_V:
				return new Dir[] {Dir.EAST, Dir.WEST, Dir.NORTHEAST, Dir.NORTHWEST, Dir.SOUTHEAST, Dir.SOUTHWEST};
			case SHIP_SUB:
				return new Dir[] {Dir.NORTH, Dir.SOUTH, Dir.EAST, Dir.WEST, Dir.NORTHEAST, Dir.NORTHWEST, Dir.SOUTHEAST, Dir.SOUTHWEST};
			case SHIP_NORTH:
				return new Dir[] {Dir.NORTH, Dir.EAST, Dir.WEST, Dir.NORTHEAST, Dir.NORTHWEST, Dir.SOUTHEAST, Dir.SOUTHWEST};
			case SHIP_SOUTH:
				return new Dir[] {Dir.SOUTH, Dir.EAST, Dir.WEST, Dir.NORTHEAST, Dir.NORTHWEST, Dir.SOUTHEAST, Dir.SOUTHWEST};
			case SHIP_EAST:
				return new Dir[] {Dir.NORTH, Dir.SOUTH, Dir.EAST, Dir.NORTHEAST, Dir.NORTHWEST, Dir.SOUTHEAST, Dir.SOUTHWEST};
			case SHIP_WEST:
				return new Dir[] {Dir.NORTH, Dir.SOUTH, Dir.WEST, Dir.NORTHEAST, Dir.NORTHWEST, Dir.SOUTHEAST, Dir.SOUTHWEST};
			default:
				return new Dir[0];
		}
	}

	/*========== Private Functions ==========*/

	/**
	 * Gets the direction of neighboring Tiles that must contain ship pieces
	 * @return An array of directions
	 */
	private Dir[] getShipDirections() {
		switch (value) {
			case SHIP_NORTH:
				return new Dir[] {Dir.SOUTH};
			case SHIP_SOUTH:
				return new Dir[] {Dir.NORTH};
			case SHIP_EAST:
				return new Dir[] {Dir.WEST};
			case SHIP_WEST:
				return new Dir[] {Dir.EAST};
			case SHIP_MID_H:
				return new Dir[] {Dir.EAST, Dir.WEST};
			case SHIP_MID_V:
				return new Dir[] {Dir.NORTH, Dir.SOUTH};
			default:
				return new Dir[0];
		}
	}

	/**
	 * Attempts to change this Tile to a new value
	 * @param val - the value to change this Tile to
	 * @throws InvalidMoveException if the new value is not a valid move
	 */
	private void tryValue(Value val) throws InvalidMoveException {
		boolean valid = true;
		boolean overwrite = true;
		switch (this.value) {
			case BLANK:
				break;
			case WATER:
				if (val != Value.WATER) valid = false;
				overwrite = false;
				break;
			case SHIP_UNID:
				if (val == Value.WATER || val == Value.BLANK) valid = false;
				if (val == Value.SHIP_UNID) overwrite = false;
				break;
			case SHIP_MID:
				if (val != Value.SHIP_MID_H && val != Value.SHIP_MID_V) valid = false;
				break;
			default:
				if (this.value != val && val != Value.SHIP_UNID) valid = false;
				overwrite = false;
		}
		if (!valid) throw new InvalidMoveException("Invalid move at (" + ROW + "," + COL + "): Change value from " + val + " to " + val);
		if (overwrite) {
			if (BOARD.log != null) {
				BOARD.log.setChanged();
				BOARD.log.record(String.format("Changed tile at %s from %s to %s\n",this,value,val));
			}
			this.value = val;
		}
	}

	/*========== Override Functions ==========*/
	public String toString() {
		return "[" + ROW + "," + COL + "]";
	}
}
