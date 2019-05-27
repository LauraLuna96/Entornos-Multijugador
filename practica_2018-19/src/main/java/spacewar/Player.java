package spacewar;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class Player extends Spaceship {

	private final WebSocketSession session;
	private final String playerName;
	private final int playerId;
	private final String shipType;
	private Sala sala; // Sala a la que pertenece el jugador, le llega en el constructor
	private AtomicInteger life;
	private AtomicInteger ammo; // Munición
	private AtomicInteger score;
	private AtomicBoolean alive;

	public Player(int playerId, WebSocketSession session, String playerName) {
		this.playerId = playerId;
		this.session = session;
		this.shipType = this.getRandomShipType();
		this.playerName = playerName;
		this.life = new AtomicInteger(3);
		this.ammo = new AtomicInteger(20);
		this.score = new AtomicInteger(0);
		this.alive = new AtomicBoolean(true);
	}
	
	public boolean isAlive() {
		return this.alive.get();
	}
	
	public void setAlive(boolean alive) {
		this.alive.set(alive);
	}
	
	public int getScore() {
		return this.score.get();
	}
	
	public void increaseScore(int n) {
		this.score.addAndGet(n);
	}
	
	public int getAmmo() {
		return this.ammo.get();
	}
	
	public void setAmmo(AtomicInteger ammo) {
		this.ammo = ammo;
	}
	
	public void decreaseAmmo() {
		this.ammo.decrementAndGet();
	}

	public int decreaseLife() {
		return this.life.decrementAndGet();
	}

	public int getLife() {
		return this.life.get();
	}

	public void setLife(AtomicInteger life) {
		this.life = life;
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
		// Si dos hilos quieren llamar a la misma sesión de ws a la vez, la protegemos
		// con EM
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
