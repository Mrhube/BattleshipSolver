import Exceptions.PuzzleException;

import java.util.Scanner;

/**
 * Created by Matt on 7/29/2017.
 */
public class Main {

	public static void main(String[] args) {
		String inputStr;
		Scanner in = new Scanner(System.in);
		do {
			System.out.print("Solve all puzzles? (Y/N) ");
			inputStr = in.nextLine().toUpperCase();
		} while (!inputStr.equals("Y") && !inputStr.equals("N"));
		if (inputStr.equals("Y")) {
			solveAll();
		} else {
			System.out.print("Solve which puzzle? ");
			while (!in.hasNextInt()) {
				System.out.print("You must enter a number: ");
				in.next();
			}
			solveSingle(in.nextInt());
		}
	}

	/**
	 * Attempt to solve a single specified puzzle from Puzzles.txt
	 * @param i - the ID of the puzzle to be solved
	 */
	private static void solveSingle(int i) {
		Board board = Reader.readPuzzle(i);

		try {
			int level = Solver.solveDynamic(board);
			System.out.println(board);
			for (Ship ship : board.getShipList(false)) {
				System.out.println(ship);
			}
			System.out.println("Difficulty Level: " + level);
		} catch (PuzzleException e) {
			System.out.println(e.getMessage());
		}

	}

	/**
	 * Attempt to solve all puzzles from Puzzles.txt
	 */
	private static void solveAll() {
		try {
			for (int i = 1; i <= 12; i++) {
				Board board = Reader.readPuzzle(i);
				int level = Solver.solveDynamic(board);
				if (Solver.isComplete(board)) {
					System.out.println("Puzzle " + i + ": Solved (Difficulty " + level + ")");
				} else {
					System.out.println("Puzzle " + i + ": \u001B[31mFailed\u001B[0m");
				}
			}
		} catch (PuzzleException e) {
			System.out.println(e.getMessage());
		}
	}
}
