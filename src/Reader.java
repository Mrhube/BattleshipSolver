import Enums.Value;
import Exceptions.InvalidMoveException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

/**
 * Created by Matt on 6/25/2016.
 * The Reader class is used to convert the data in Puzzles.txt into instances of Board that can be solved
 */
class Reader {

	/**
	 * Converts a single puzzle from Puzzles.txt into a Board object
	 * @param id - The identifier of the desired puzzle
	 * @return An instance of Board ready to be solved
	 */
	static Board readPuzzle(int id) {

		try {

			Scanner scanner = new Scanner(new FileReader("src/FreePuzzles.txt"));
			scanner.useDelimiter("\\D");

			while (true) {
				String line = scanner.nextLine();
				if (line.equals(id + ")")) {
					break;
				}
			}
			// Read tile data
			String[] data = new String[10];
			for (int i = 0; i < 10; i++) {
				data[i] = scanner.nextLine().replaceAll("\\s","");
			}
			// Read row sum data
			int[] rowSum = new int[10];
			for (int i = 0; i < 10; i++) {
				while (scanner.hasNext()) {
					if (scanner.hasNextInt()) {
						rowSum[i] = scanner.nextInt();
						break;
					}
					scanner.next();
				}
			}
			// Read col sum data
			int[] colSum = new int[10];
			for (int i = 0; i < 10; i++) {
				while (scanner.hasNext()) {
					if (scanner.hasNextInt()) {
						colSum[i] = scanner.nextInt();
						break;
					}
					scanner.next();
				}
			}
			scanner.close();
			Board board = new Board("Puzzle " + id,10,4,rowSum,colSum);

			for (int i = 0; i < 10; i++) {
				for (int j = 0; j < 10; j++) {
					if (data[i].charAt(j) != '.') {
						board.TILES[i][j].setValue(Value.fromChar(data[i].charAt(j)));
					}
				}
			}
			board.log.clear();
			return board;
		} catch(FileNotFoundException e) {
			throw new Error();
		} catch(InvalidMoveException e) {
			System.out.println(e);
			return null;
		}
	}
}
