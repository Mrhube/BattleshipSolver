import java.io.Serializable;

/**
 * Created by Matt on 8/1/2017.
 * The Logger class records changes made to a Board as it is solved. It also contains a "changed" flag that is used to
 * track if applying a specific solving strategy made any progress in solving the Board.
 */
public class Logger implements Serializable {

	//========== State ==========//

	public boolean changed;	// Indicates whether the state of the parent Board has changed since the last reset
	private String log;		// A multi-line string of actions taken on the board

	//========== Constructor ==========//

	Logger() {
		this.changed = false;
		this.log = "";
	}

	//========== Package Methods ==========//

	public String getLog() {
		return log;
	}

	void record(String s) {
		log += s + "\n";
	}

	void setChanged() {
		changed = true;
	}

	boolean getChanged() {
		return changed;
	}

	void reset() {
		changed = false;
	}

	void clear() {
		log = "";
	}

}
