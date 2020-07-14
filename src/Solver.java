import Enums.Dir;
import Enums.Lane;
import Enums.Value;
import Exceptions.InvalidBoardException;
import Exceptions.InvalidMoveException;
import Exceptions.PuzzleException;

import java.util.ArrayList;

/**
 * Created by Matt on 7/30/2017.
 * The Solver class applies a range of algorithms to a Board in an attempt to solve it. By always trying to simplest
 * algorithms until no more progress can be made, it also determines a difficulty rating for the puzzle. The code is
 * broken down into Difficulties, Strategies, and Components. Each difficulty will apply a subset of appropriately
 * complex strategies. Each strategy is a unique method that may determine the value of tiles on the board, the location
 * of entire ships, or where ships cannot go. Strategies apply to the entire puzzle and typically use a loop to call
 * components, which are more limited in scope.
 */
public class Solver {

	static int solveDynamic(Board board) throws PuzzleException {
		boolean loop = true;
		int level = 1;
		int maxLevel = 1;
		do {
			board.log.reset();
			loop = execute(board, level);
			if (isComplete(board)) loop = false;
			if (board.log.getChanged()) level=1;
			else level++;
			if (level > maxLevel) maxLevel = level;
		} while (loop);
		return maxLevel;
	}

	static boolean execute(Board board, int level) throws PuzzleException {
		switch (level) {
			case 1: strategyFillLanes(board);
					return true;
			case 2: strategyIdentifyTiles(board);
					return true;
			case 3: strategyCompleteShipSizes(board); return true;
			case 4: strategyIdentifyShips(board); return true;
			case 5: strategyFindSharedTiles(board); return true;
			case 6: strategyFillPartialLane(board); return true;
			case 7: strategySimpleLookAhead(board); return true;
			default: return false;
		}
	}

	//==========Difficulties==========//

	public static void solveEasiest(Board board) throws PuzzleException {
		do {
			do {
				board.log.reset();
				strategyFillLanes(board);
			} while (board.log.getChanged());
			board.log.reset();
			strategyIdentifyTiles(board);
		} while (board.log.getChanged());
	}

	public static void solveEasy(Board board) throws PuzzleException {
		do {
			do {
				solveEasiest(board);
				board.log.reset();
				strategyCompleteShipSizes(board);
			} while (board.log.getChanged());
			board.log.reset();
			strategyIdentifyShips(board);
		} while (board.log.getChanged());
	}

	public static void solveNormal(Board board) throws PuzzleException {
		do {
			do {
				do {
					solveEasy(board);
					board.log.reset();
					strategyFindSharedTiles(board);
				} while (board.log.getChanged());
				board.log.reset();
				strategyFillPartialLane(board);
			} while (board.log.getChanged());
			board.log.reset();
			strategySimpleLookAhead(board);
		} while (board.log.getChanged());
	}

	//==========Strategies==========//

	//-----Easiest-----//
	private static void strategyFillLanes(Board board) throws InvalidMoveException {
		for (int i = 0; i < board.SIZE; i++) {
			fillLane(board, Lane.COL, i);
			fillLane(board, Lane.ROW, i);
		}
	}

	private static void strategyIdentifyTiles(Board board) throws PuzzleException {
		for (int i = 0; i < board.SIZE; i++) {
			for (int j = 0; j < board.SIZE; j++) {
				identifyShipUnid(board, board.TILES[i][j]);
				identifyShipMid(board, board.TILES[i][j]);
			}
		}
	}

	//-----Easy-----//
	private static void strategyCompleteShipSizes(Board board) throws PuzzleException {
		for (int i = 1; i <= board.MAX_SHIP_SIZE; i++) {
			ArrayList<Ship> list = board.getShipList(i, false);
			if (getMissingShips(board, i) == list.size()) {
				for (Ship ship : list) {
					ship.confirm();
				}
			}
		}
	}

	private static void strategyIdentifyShips(Board board) throws PuzzleException {
		for (Tile tile : board.getTiles(Value.SHIP_UNID)) {
			identifyShip(board, tile);
		}
	}

	//-----Normal-----//
	private static void strategyFindSharedTiles(Board board) throws  PuzzleException {
		for (int size = 2; size <= board.MAX_SHIP_SIZE; size++) {
			placeSharedTiles(board, size);
		}
	}

	private static void strategyFillPartialLane(Board board) throws PuzzleException {
		for (int size = 2; size <= board.MAX_SHIP_SIZE; size++) {
			fillPartialLane(board, size);
		}
	}

	private static void strategySimpleLookAhead(Board board) throws PuzzleException {
		for (int size = 2; size <= board.MAX_SHIP_SIZE; size++) {
			simpleLookAhead(board, size);
		}
	}

	//=========Strategy Components==========//

	//-----Easiest-----//
	/**
	 * If possible, fills the remaining blank tiles in a row with WATER or SHIP_UNID
	 * @param board the puzzle Board being solved
	 * @param lane whether the lane to fill is a ROW or COL
	 * @param idx the index of the lane in the board, between 1 and Board.SIZE
	 * @throws InvalidMoveException when filling the lane would cause an invalid move
	 */
	private static void fillLane(Board board, Lane lane, int idx) throws InvalidMoveException {
		// If row is full do nothing
		if (!board.hasBlanks(lane, idx)) return;
		// Else count tiles
		int sumShip, sumWater, sumLane;
		sumShip = board.getSumShip(lane, idx);
		sumWater = board.getSumWater(lane, idx);
		sumLane = board.getSum(lane, idx);
		// Fill in water tiles
		if (sumShip == sumLane) {
			for (int i = 0; i < board.SIZE; i++) {
				Tile tile = board.tile(lane, idx, i);
				if (tile.getValue() == Value.BLANK) tile.setValue(Value.WATER);
			}
		}
		// Fill in ship tiles
		else if (sumWater == board.SIZE - sumLane) {
			for (int i = 0; i < board.SIZE; i++) {
				Tile tile = board.tile(lane, idx, i);
				if (tile.getValue() == Value.BLANK) tile.setValue(Value.SHIP_UNID);
			}
		}
	}

	/**
	 * Identifies a SHIP_MID tile as either SHIP_MID_H or SHIP_MID_V
	 * @param board the puzzle Board being solved
	 * @param tile the tile with a value of SHIP_MID
	 * @throws PuzzleException when identifying the tile would cause an invalid move
	 */
	private static void identifyShipMid(Board board, Tile tile) throws PuzzleException {
		if (tile.getValue() == Value.SHIP_MID) {
			Tile north = tile.getNeighbor(Dir.NORTH);
			Tile south = tile.getNeighbor(Dir.SOUTH);
			Tile east = tile.getNeighbor(Dir.EAST);
			Tile west = tile.getNeighbor(Dir.WEST);
			boolean isHorizontal = false;
			boolean isVertical = false;
			int row = tile.ROW;
			int col = tile.COL;
			int sumShipRow = board.getSumShip(Lane.ROW, row);
			int sumShipCol = board.getSumShip(Lane.COL, col);
			int sumLaneRow = board.getSum(Lane.ROW, row);
			int sumLaneCol = board.getSum(Lane.COL, col);

			if (Tile.isWater(north) || Tile.isWater(south) || Tile.isShip(east) || Tile.isShip(west))
				isHorizontal = true;
			else if (sumShipRow + 2 > sumLaneRow)
				isVertical = true;
			if (Tile.isWater(east) || Tile.isWater(west) || Tile.isShip(north) || Tile.isShip(south))
				isVertical = true;
			else if (sumShipCol + 2 > sumLaneCol)
				isHorizontal = true;

			if (isHorizontal && isVertical)
				throw new InvalidBoardException("Invalid Ship Mid at " + row + ", " + col);
			else if (isHorizontal)
				tile.setValue(Value.SHIP_MID_H);
			else if (isVertical)
				tile.setValue(Value.SHIP_MID_V);
		}
	}

	/**
	 * Identifies a SHIP_UNID tile
	 * @param board the puzzle Board being solved
	 * @param tile the tile with a value of SHIP_UNID
	 * @throws PuzzleException when identifying the tile would cause an invalid move
	 */
	private static void identifyShipUnid(Board board, Tile tile) throws PuzzleException {
		if (tile.getValue() == Value.SHIP_UNID) {
			Tile north = tile.getNeighbor(Dir.NORTH);
			Tile south = tile.getNeighbor(Dir.SOUTH);
			Tile east = tile.getNeighbor(Dir.EAST);
			Tile west = tile.getNeighbor(Dir.WEST);
			if (Tile.isWater(north,south,east,west)) {
				tile.setValue(Value.SHIP_SUB);
			}
			if (Tile.isWater(north) && Tile.isShip(south)) {
				tile.setValue(Value.SHIP_NORTH);
			}
			if (Tile.isWater(south) && Tile.isShip(north)) {
				tile.setValue(Value.SHIP_SOUTH);
			}
			if (Tile.isWater(east) && Tile.isShip(west)) {
				tile.setValue(Value.SHIP_EAST);
			}
			if (Tile.isWater(west) && Tile.isShip(east)) {
				tile.setValue(Value.SHIP_WEST);
			}
			if (Tile.isShip(north) && Tile.isShip(south)) {
				tile.setValue(Value.SHIP_MID_V);
			}
			if (Tile.isShip(east) && Tile.isShip(west)) {
				tile.setValue(Value.SHIP_MID_H);
			}
		}
	}

	//-----Easy-----//
	/**
	 * If a unidentified tile is only part of 1 incomplete ship, complete the ship
	 * @param board the puzzle Board being solved
	 * @param tile a Tile with a value of SHIP_UNID
	 * @throws InvalidMoveException when completing the ship would cause an invalid move
	 */
	private static void identifyShip(Board board, Tile tile) throws InvalidMoveException {
		if (tile.getValue() == Value.SHIP_UNID) {
			Ship possible = null;
			for (Ship ship : board.getShipList(false)) {
				if (ship.tiles().contains(tile)) {
					if (possible == null) possible = ship;
					else return;
				}
			}
			if (possible != null) {
				possible.confirm();
			}
		}
	}

	//-----Normal-----//
	/**
	 * If the board is missing a ship of a certain size, and all potential locations for that size ship would cause a
	 * tile to be the same value, then that tile must be that value
	 * @param board - The puzzle Board being solved
	 * @param size - The size of the ship to check
	 * @throws PuzzleException if setting that shared Tile to the determined value causes the Board to be invalid
	 */
	private static void placeSharedTiles(Board board, int size) throws PuzzleException {
		ArrayList<Tile> tileList = new ArrayList<>();
		ArrayList<Ship> shipList = board.getShipList(size, false);
		for (Ship ship : shipList) {
			for (Tile tile : ship.tiles()) {
				if (!tileList.contains(tile)) tileList.add(tile);
			}
		}
		boolean shared;
		for (Tile tile : tileList) {
			shared = true;
			for (Ship ship : shipList) {
				if (!ship.tiles().contains(tile)) {
					shared = false;
					break;
				}
			}
			if (shared) tile.setValue(Value.SHIP_UNID);
		}
	}

	/**
	 * If a lane must contain all the remaining ships of one size, set any blanks tiles in that lane to water if they
	 * aren't contained in the incomplete ships
	 * @param board - The puzzle Board being solved
	 * @param size - The size of the ship to check
	 * @throws InvalidMoveException
	 */
	private static void fillPartialLane(Board board, int size) throws InvalidMoveException {
		ArrayList<Ship> shipList = board.getShipList(size, false);
		if (shipList.size() == 0) return;
		ArrayList<Tile> tileList = new ArrayList<>();
		for (Ship ship : shipList) {
			for (Tile tile : ship.tiles()) {
				if (!tileList.contains(tile)) tileList.add(tile);
			}
		}
		boolean sameRow = true;
		boolean sameCol = true;
		for (Tile tile : tileList) {
			if (tile.ROW != tileList.get(0).ROW) sameRow = false;
			if (tile.COL != tileList.get(0).COL) sameCol = false;
		}
		if (!sameRow && !sameCol) return;
		Lane lane;
		int idx;
		if (sameRow) {
			lane = Lane.ROW;
			idx = tileList.get(0).ROW;
		} else {
			lane = Lane.COL;
			idx = tileList.get(0).COL;
		}
		int count = 0;
		Tile tile;
		ArrayList<Tile> blankList = new ArrayList<>();
		for (int i = 0; i < board.SIZE; i++) {
			tile = board.tile(lane, idx, i);
			if (!tileList.contains(tile)) {
				if (tile.isBlank()) blankList.add(tile);
				if (tile.isShip()) count++;
			}
		}
		if (board.getSum(lane, idx) - count - size * getMissingShips(board, size) == 0) {
			for (Tile blank : blankList) {
				blank.setValue(Value.WATER);
			}
		}

	}

	/**
	 * If all the ships of a specific size have not been placed yet, look at the potential locations where ships of that
	 * size could still fit and apply the easiest strategies to determine if any of those locations would be invalid
	 * @param board
	 * @param size
	 * @throws PuzzleException
	 */
	private static void simpleLookAhead (Board board, int size) throws PuzzleException {
		if (getMissingShips(board,size) <= 2) {
			Board clone;
			ArrayList<Ship> shipList = board.getShipList(size,false);
			if (shipList.size() <= 4) {
				for (Ship ship : shipList) {
					clone = board.cloneBoard();
					clone.getShipList(size, false).get(clone.getShipList(size, false).indexOf(ship)).confirm();
					try {
						solveEasiest(clone);
						validateLaneCount(clone);
						validateShipCount(clone);
					} catch (PuzzleException e) {
						board.blacklist(ship);
					}
				}
			}
		}
	}

	//=========Helper Functions==========//

	public static boolean isComplete(Board board) {
		for (int i = 0; i < board.SIZE; i++) {
			if (board.hasBlanks(Lane.COL, i)) return false;
			if (board.hasBlanks(Lane.ROW, i)) return false;
			if (board.hasUnid(Lane.COL,i)) return false;
			if (board.hasUnid(Lane.ROW,i)) return false;
			if (board.getSumShip(Lane.COL, i) != board.getSum(Lane.COL, i)) return false;
			if (board.getSumShip(Lane.ROW, i) != board.getSum(Lane.ROW, i)) return false;
		}
		return true;
	}

	public static int getMissingShips(Board board, int size) {
		return board.MAX_SHIP_SIZE - size + 1 - board.getShipList(size,true).size();
	}

	private static void validateLaneCount(Board board) throws PuzzleException {
		int sumShip, sumWater, sumLane;
		Lane[] lanes = {Lane.ROW, Lane.COL};
		for (int idx = 0; idx < board.SIZE; idx++) {
			for (Lane lane : lanes) {
				sumShip = board.getSumShip(lane, idx);
				sumWater = board.getSumWater(lane, idx);
				sumLane = board.getSum(lane, idx);
				if ((sumShip > sumLane) | (board.SIZE - sumWater < sumLane)) {
					throw new InvalidBoardException("Invalid Lane: " + lane + " " + idx + "\n" + board.toString());
				}
			}
		}
	}

	private static void validateShipCount(Board board) throws PuzzleException {
		for (int size = 1; size <= board.MAX_SHIP_SIZE; size++) {
			if (board.getShipList(size,true).size() > (board.MAX_SHIP_SIZE - size + 1)) {
				throw new InvalidBoardException("Invalid Ship Total: size " + size + "\n" + board.toString());
			}
		}
	}

}

/** Possible performance improvements:
	Log which rows/columns are changed and only verify them
*/

// Check validity
//if ((sumShip > sumLane) | (board.SIZE - sumWater < sumLane)) {
//	throw new InvalidBoardException("Fill Lane: " + lane + " " + idx + "\n" + board.toString());
//}