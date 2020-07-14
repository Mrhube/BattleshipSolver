import Enums.Dir;
import Enums.Lane;
import Enums.Value;

import java.io.*;
import java.util.ArrayList;

/*
 TODO
  Add error checking to Board constructor array sizes
 */

/**
 * The Board class holds all the state related to the Battleship puzzle
 */
public class Board implements Serializable {

	//========== State ==========//

	String NAME;							// The puzzle identifier
	Logger log;								// An instance of Logger to record actions
	final int SIZE;							// The size of the puzzle grid, both height and width
	final int MAX_SHIP_SIZE;				// The length of the largest size ship in the puzzle
	private final int[] ROW_SUM;			// An array indicating how many tiles in each row must contain a ship
	private final int[] COL_SUM;			// An array indicating how many tiles in each column must contain a ship
	final Tile[][] TILES;					// A 2D-array of Tile objects representing the puzzle grid
	private ArrayList<Ship>[][] shipList;	// A list of potential ship locations in the puzzle
	private ArrayList<Ship> shipBlackList;	// A list of ship locations that have been proven invalid

	//========== Constructor ==========//

	/**
	 * Creates a board filled with blank tiles
	 * @param name String identifier
	 * @param size The number of rows and columns
	 * @param maxShipSize The largest ship on the board
	 * @param rowSum Array of sums of ship tiles in each row
	 * @param colSum Array of sums of ship tiles in each column
	 */
	Board(String name, int size, int maxShipSize, int[] rowSum, int[] colSum) {
		this.NAME = name;
		this.log = new Logger();
		this.SIZE = size;
		this.MAX_SHIP_SIZE = maxShipSize;
		this.ROW_SUM = rowSum;
		this.COL_SUM = colSum;
		TILES = new Tile[size][size];
		shipBlackList = new ArrayList<>();
		shipList = new ArrayList[MAX_SHIP_SIZE][2];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				TILES[i][j] = new Tile(this, i, j);
			}
		}
	}

	//========== Public Methods ==========//

	/**
	 * Checks if a specific lane contains any blank tiles
	 * @param lane - The type or lane, row or column
	 * @param idx - The lane index, between 0 and SIZE-1
	 * @return True if there is at least one blank tile, otherwise false
	 */
	boolean hasBlanks(Lane lane, int idx) {
		for (int i = 0; i < SIZE; i++) {
			if (Tile.isBlank(tile(lane, idx, i))) return true;
		}
		return false;
	}

	/**
	 * Checks if a specific lane contains an unidentified ship tiles
	 * @param lane - The type or lane, row or column
	 * @param idx - The lane index, between 0 and SIZE-1
	 * @return True if there is at least one unidentified ship tile, otherwise false
	 */
	boolean hasUnid(Lane lane, int idx) {
		for (int i = 0; i < SIZE; i++) {
			if (Tile.isUnid(tile(lane, idx, i))) return true;
		}
		return false;
	}

	/**
	 * Gets the number of tiles in a specific lane that must contain a ship
	 * @param lane - The type or lane, row or column
	 * @param idx - The lane index, between 0 and SIZE-1
	 * @return The number of tiles as an integer
	 */
	int getSum(Lane lane, int idx) {
		if (lane == Lane.COL) {
			return COL_SUM[idx];
		} else {
			return ROW_SUM[idx];
		}
	}

	/**
	 * Gets the number of tiles in a specific lane that currently contain a ship
	 * @param lane - The type or lane, row or column
	 * @param idx - The lane index, between 0 and SIZE-1
	 * @return The number of tiles as an integer
	 */
	int getSumShip(Lane lane, int idx) {
		int count = 0;
		for (int i = 0; i < SIZE; i++) {
			if (Tile.isShip(tile(lane, idx, i))) count++;
		}
		return count;
	}

	/**
	 * Gets the number of tiles in a specific lane that currently contain water
	 * @param lane - The type or lane, row or column
	 * @param idx - The lane index, between 0 and SIZE-1
	 * @return The number of tiles as an integer
	 */
	int getSumWater(Lane lane, int idx) {
		int count = 0;
		for (int i = 0; i < SIZE; i++) {
			if (Tile.isWater(tile(lane, idx, i))) count++;
		}
		return count;
	}

	/**
	 * Gets the number of tiles in a specific lane that are currently blank
	 * @param lane - The type or lane, row or column
	 * @param idx - The lane index, between 0 and SIZE-1
	 * @return The number of tiles as an integer
	 */
	int getSumBlank(Lane lane, int idx) {
		int count = 0;
		for (int i = 0; i < SIZE; i++) {
			if (Tile.isBlank(tile(lane, idx, i))) count++;
		}
		return count;
	}

	/**
	 * Get the number of ships of the specified size that have not yet been fully identified on the board
	 * @param size - The length of the ship in tiles, between 1 and MAX_SHIP_SIZE
	 * @return The number of ships as an integer
	 */
	int getMissingShips(int size) {
		return MAX_SHIP_SIZE - size + 1 - getShipList(size, true).size();
	}

	/**
	 * Gets an array list of all the tiles in the board with a specified value
	 * @param val - The desired tile value
	 * @return An array list of Tile objects
	 */
	ArrayList<Tile> getTiles(Value val) {
		Tile tile;
		ArrayList<Tile> result = new ArrayList<>();
		for (int i = 1; i < SIZE; i++) {
			for (int j = 1; j < SIZE; j++) {
				tile = TILES[i][j];
				if (tile.getValue() == val) result.add(tile);
			}
		}
		return result;
	}

	/**
	 * Gets the tile at the specified position in the specified lane
	 * @param lane - The type or lane, row or column
	 * @param laneNum - The lane index, between 0 and SIZE-1
	 * @param tileNum - The tile index, between 0 and SIZE-1
	 * @return An instance of Tile
	 */
	Tile tile(Lane lane, int laneNum, int tileNum) {
		if (lane == Lane.COL) {
			return TILES[tileNum][laneNum];
		} else {
			return TILES[laneNum][tileNum];
		}
	}

	/**
	 * Gets a list of either confirmed or unconfirmed ships in the board
	 * @param confirmed - Whether the returned list of ships should be confirmed or just potential locations of ships
	 * @return An array list of Ship objects
	 */
	ArrayList<Ship> getShipList(boolean confirmed) {
		generateShips();
		ArrayList<Ship> result = new ArrayList<>();
		for (int i = 0; i < MAX_SHIP_SIZE; i++) {
			result.addAll(getShipList(i + 1, confirmed));
		}
		return result;
	}

	/**
	 * Gets a list of either confirmed or unconfirmed ships of the specified size in the board
	 * @param size - The length of the ship in tiles, between 1 and MAX_SHIP_SIZE
	 * @param confirmed - Whether the returned list of ships should be confirmed or just potential locations of ships
	 * @return An array list of Ship objects
	 */
	ArrayList<Ship> getShipList(int size, boolean confirmed) {
		generateShips();
		return shipList[size - 1][confirmed? 1 : 0];
	}

	/**
	 * Creates a deep copy of the board
	 * @return A new instance of Board
	 */
	Board cloneBoard() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(this);
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);
			Board clone = (Board) ois.readObject();
			clone.NAME = "Clone of " + clone.NAME + " (" + (int)(Math.random()*10000) + ")";
			clone.log = new Logger();
			return clone;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Adds a ship to the boards blacklist
	 * @param ship - The instance of Ship to blacklist
	 */
	void blacklist(Ship ship) {
		shipBlackList.add(ship);
		if (log != null) {
			log.record("Blacklisted: " + ship);
			log.setChanged();
		}
	}

	//========== Private Methods ==========//

	/**
	 * Gets the tile at the specified row and column, if within bounds
	 * @param row - The row index
	 * @param col - The column index
	 * @return An instance of Tile if the row and column are between 0 and SIZE-1, otherwise null
	 */
	private Tile tile(int row, int col) {
		if (row < this.SIZE && col < this.SIZE) return TILES[row][col];
		else return null;
	}

	/**
	 * Populates shipList with all the potential locations of ships on the board, excluding any blacklisted locations
	 */
	private void generateShips() {

		for (int i = 0; i < MAX_SHIP_SIZE; i++) {
			shipList[i][0] = new ArrayList<>(); // Confirmed ships
			shipList[i][1] = new ArrayList<>(); // Unconfirmed ships
		}

		for (int i = 0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++) {
				Tile start = TILES[i][j];
				if (start.getValue() == Value.BLANK || start.getValue() == Value.SHIP_UNID || start.getValue() == Value.SHIP_NORTH || start.getValue() == Value.SHIP_WEST) {
					// Create horizontal
					if (!Tile.isShip(start.getNeighbor(Dir.WEST))) {
						int sum = 0;
						for (int k = 0; k < MAX_SHIP_SIZE && j + k < SIZE; k++) {
							Tile cur = tile(i, j + k);
							Tile next = tile(i, j + k + 1);
							if (Tile.isWater(cur)) break;
							if (!Tile.isShip(cur)) sum++;
							if (k != 0 && !Tile.isShip(next) && getSum(Lane.ROW,i) - getSumShip(Lane.ROW,i) >= sum) {
								Ship ship = new Ship(this,start,cur);
								if (!shipBlackList.contains(ship)) {

									if (ship.isConfirmed()) shipList[ship.size()-1][1].add(ship);
									else shipList[ship.size()-1][0].add(ship);
								}
							}
						}
					}
					// Create vertical
					if (!Tile.isShip(start.getNeighbor(Dir.NORTH))) {
						int sum = 0;
						for (int k = 0; k < MAX_SHIP_SIZE && i + k < SIZE; k++) {
							Tile cur = tile(i + k, j);
							Tile next = tile(i + k + 1, j);
							if (Tile.isWater(cur)) break;
							if (!Tile.isShip(cur)) sum++;
							if (k != 0 && !Tile.isShip(next) && getSum(Lane.COL,j) - getSumShip(Lane.COL,j) >= sum) {
								Ship ship = new Ship(this,start,cur);
								if (!shipBlackList.contains(ship)) {

									if (ship.isConfirmed()) shipList[ship.size()-1][1].add(ship);
									else shipList[ship.size()-1][0].add(ship);
								}
							}
						}
					}
				}
				// Create sub
				if (start.getValue() == Value.BLANK || start.getValue() == Value.SHIP_UNID || start.getValue() == Value.SHIP_SUB) {
					boolean valid = true;
					for (Tile neighbor : start.getAdjacentNeighbors()) {
						if (Tile.isShip(neighbor)) {
							valid = false;
							break;
						}
					}
					if (valid) {
						Ship ship = new Ship(this,start);
						if (!shipBlackList.contains(ship)) {

							if (ship.isConfirmed()) shipList[ship.size()-1][1].add(ship);
							else shipList[ship.size()-1][0].add(ship);
						}
					}
				}
			}
		}
		// Check if size is complete
		for (int i = 0; i < MAX_SHIP_SIZE; i++) {
			if (shipList[i][1].size() >= MAX_SHIP_SIZE - i) {
				shipList[i][0].clear();
			}
		}
	}

	//========== Override Methods ==========//

	public String toString() {
		String result = "";
		for (int i = 0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++) {
				result += TILES[i][j].getValue().toChar() + " ";
			}
			result += "| " + ROW_SUM[i] + "\n";
		}
		result += "_ _ _ _ _ _ _ _ _ _\n";
		for (int i = 0; i < SIZE; i++) {
			result += COL_SUM[i] + " ";
		}
		return result;
	}

	//========== Unused ==========//

	ArrayList<Ship> getShipList() {
		generateShips();
		ArrayList<Ship> result = new ArrayList<>();
		for (int i = 0; i < MAX_SHIP_SIZE; i++) {
			result.addAll(getShipList(i + 1, true));
			result.addAll(getShipList(i + 1, false));
		}
		return result;
	}
	ArrayList<Ship> getShipList(int size) {
		generateShips();
		ArrayList<Ship> result = new ArrayList<>();
		result.addAll(getShipList(size, true));
		result.addAll(getShipList(size, false));
		return result;
	}

}
