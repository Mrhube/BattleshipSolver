package Enums;

public enum Value {

    BLANK('.'),
	WATER('='),
	SHIP_UNID('?'),
	SHIP_SUB('O'),
	SHIP_MID('+'),
	SHIP_MID_H('-'),
	SHIP_MID_V('|'),
	SHIP_NORTH('^'),
	SHIP_SOUTH('v'),
	SHIP_EAST('>'),
	SHIP_WEST('<');

    private char symbol;

	// Constructor
	Value(char symbol) {
		this.symbol = symbol;
	}

	// Methods
	public char toChar() {
		return symbol;
	}

	public static Value fromChar(char c) {
		for (Value val : Value.values()) {
			if (val.symbol == c) { return val; }
		}
		throw new Error("Invalid char: '" + c + "'");
	}

}
