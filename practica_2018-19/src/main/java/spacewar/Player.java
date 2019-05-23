package spacewar;

import java.util.Random;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class Player extends Spaceship {

	private final WebSocketSession session;
	private final String playerName;
	private final int playerId;
	private final String shipType;
	private Sala sala; // Sala a la que pertenece el jugador, le llega en el constructor

	public Player(int playerId, WebSocketSession session, String playerName) {
		this.playerId = playerId;
		this.session = session;
		this.shipType = this.getRandomShipType();
		this.playerName = playerName;
	}
	
	public Sala getSala() {
		return this.sala;
	}

	public int getPlayerId() {
		return this.playerId;
	}

	public WebSocketSession getSession() {
		return this.session;
	}

	public synchronized void sendMessage(String msg) throws Exception {
		//Si dos hilos quieren llamar a la misma sesión de ws a la vez, la protegemos con EM
		this.session.sendMessage(new TextMessage(msg));
	}

	public String getShipType() {
		return shipType;
	}
	
	public String getPlayerName() {
		return this.playerName;
	}

	private String getRandomShipType() {
		String[] randomShips = { "blue", "darkgrey", "green", "metalic", "orange", "purple", "red" };
		String ship = (randomShips[new Random().nextInt(randomShips.length)]);
		ship += "_0" + (new Random().nextInt(5) + 1) + ".png";
		return ship;
	}
}
