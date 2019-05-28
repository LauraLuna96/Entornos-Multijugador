package spacewar;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SpacewarGame {

	// public final static SpacewarGame INSTANCE = new SpacewarGame();
	public Sala sala;

	private final static int FPS = 30;
	private final static long TICK_DELAY = 1000 / FPS;
	public final static boolean DEBUG_MODE = true;
	public final static boolean VERBOSE_MODE = true;
	private boolean isRunning = false;
	private boolean isOver = false; // Se pondrá a true cuando finalice una partida
	private final static int scorePerHit = 10;
	private final static int bonusScorePerHit = 20;

	ObjectMapper mapper = new ObjectMapper();
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	// GLOBAL GAME ROOM
	private Map<String, Player> players = new ConcurrentHashMap<>();
	private Map<Integer, Projectile> projectiles = new ConcurrentHashMap<>();
	// private AtomicInteger numPlayers = new AtomicInteger();

	public SpacewarGame(Sala sala) {
		this.sala = sala;
	}

	public boolean getIsRunning() {
		return isRunning;
	}

	public boolean getIsOver() {
		return isOver;
	}

	public void endGame(String winner) {
		if (!isOver) {
			this.isOver = true;
			sala.setCurrentState(Sala.state.FinPartida);
			System.out.println("[GAME] Game has ended, winner: " + winner);

			ObjectNode msg = mapper.createObjectNode();
			msg.put("event", "END GAME");

			ArrayNode arrayNode = mapper.createArrayNode();
			for (Player p : sala.getPlayers()) {
				if (p.getSession().getId() != winner) {
					ObjectNode jsonPlayer = mapper.createObjectNode();
					jsonPlayer.put("playerName", p.getPlayerName());
					jsonPlayer.put("life", p.getLife());
					jsonPlayer.put("score", p.getScore());

					arrayNode.addPOJO(jsonPlayer);
				} else {
					ObjectNode jsonPlayer = mapper.createObjectNode();
					jsonPlayer.put("playerName", p.getPlayerName());
					jsonPlayer.put("life", p.getLife());
					jsonPlayer.put("score", p.getScore());

					msg.putPOJO("winner", jsonPlayer);
				}
			}

			msg.putPOJO("losers", arrayNode);

			sala.broadcast(msg.toString());
			stopGameLoop();
		}
	}

	public void sendBeginningMessages() throws Exception {
		for (Player p : players.values()) {
			ObjectNode msg = mapper.createObjectNode();
			msg.put("event", "JOIN");
			msg.put("id", p.getPlayerId());
			msg.put("shipType", p.getShipType());
			p.sendMessage(msg.toString());
		}
		ObjectNode msg2 = mapper.createObjectNode();
		msg2.put("event", "START GAME");

		sala.broadcast(msg2.toString());
	}

	public void sendBeginningMessageTo(Player player) throws Exception {
		ObjectNode msg = mapper.createObjectNode();
		msg.put("event", "JOIN");
		msg.put("id", player.getPlayerId());
		msg.put("shipType", player.getShipType());
		player.sendMessage(msg.toString());

		ObjectNode msg2 = mapper.createObjectNode();
		msg2.put("event", "START GAME");
		player.sendMessage(msg2.toString());
	}

	public void addPlayer(Player player) {
		players.put(player.getSession().getId(), player);
		/*
		 * int count = numPlayers.getAndIncrement(); if (count == 0) {
		 * this.startGameLoop(); }
		 */
	}

	public Collection<Player> getPlayers() {
		return players.values();
	}

	public void removePlayer(Player player) {
		players.remove(player.getSession().getId());

		/*
		 * int count = this.numPlayers.decrementAndGet(); if (count == 0) {
		 * this.stopGameLoop(); }
		 */
	}

	public void addProjectile(int id, Projectile projectile) {
		projectiles.put(id, projectile);
	}

	public Collection<Projectile> getProjectiles() {
		return projectiles.values();
	}

	public void removeProjectile(Projectile projectile) {
		projectiles.remove(projectile.getId(), projectile);
	}

	public synchronized void startGameLoop() {
		if (!isRunning) {
			isRunning = true;
			scheduler = Executors.newScheduledThreadPool(1);
			scheduler.scheduleAtFixedRate(() -> tick(), TICK_DELAY, TICK_DELAY, TimeUnit.MILLISECONDS);
		}
	}

	public synchronized void stopGameLoop() {
		if (scheduler != null) {
			isRunning = false;
			scheduler.shutdown();
		}
	}

	/*
	 * public void broadcast(String message) { for (Player player : getPlayers()) {
	 * try { player.getSession().sendMessage(new TextMessage(message.toString())); }
	 * catch (Throwable ex) {
	 * System.err.println("Execption sending message to player " +
	 * player.getSession().getId()); ex.printStackTrace(System.err);
	 * this.removePlayer(player); } } }
	 */

	private void tick() {
		ObjectNode json = mapper.createObjectNode();
		ArrayNode arrayNodePlayers = mapper.createArrayNode();
		ArrayNode arrayNodeProjectiles = mapper.createArrayNode();

		long thisInstant = System.currentTimeMillis();
		Set<Integer> bullets2Remove = new HashSet<>();
		boolean removeBullets = false;

		try {
			String winner = null;
			// Update players
			for (Player player : players.values()) {
				ObjectNode jsonPlayer = mapper.createObjectNode();

				if (player.getLife() > 0) {
					player.calculateMovement();

					jsonPlayer.put("id", player.getPlayerId());
					jsonPlayer.put("playerName", player.getPlayerName());
					jsonPlayer.put("life", player.getLife());
					jsonPlayer.put("propellerUses", player.getPropellerUses());
					jsonPlayer.put("score", player.getScore());
					jsonPlayer.put("ammo", player.getAmmo());
					jsonPlayer.put("shipType", player.getShipType());
					jsonPlayer.put("posX", player.getPosX());
					jsonPlayer.put("posY", player.getPosY());
					jsonPlayer.put("facingAngle", player.getFacingAngle());

					// Si es el único jugador superviviente, gana
					if (players.size() == 1) {
						winner = player.getSession().getId();
					}

				} else if (player.isAlive()) {
					player.setAlive(false);
					jsonPlayer.put("id", player.getPlayerId());
					players.remove(player.getSession().getId());
					System.out.println("[GAME] Player " + player.getPlayerName() + " defeated.");

					// Si por alguna razón no quedan jugadores vivos (se han muerto 2 en la misma
					// frame) cogemos el último que muere como ganador
					if (players.size() == 0) {
						winner = player.getSession().getId();
					}
				}

				jsonPlayer.put("isAlive", player.isAlive());

				arrayNodePlayers.addPOJO(jsonPlayer);
			}

			if (winner != null) {
				players.remove(winner);
				endGame(winner);
			}

			// Update bullets and handle collision
			for (Projectile projectile : getProjectiles()) {
				projectile.applyVelocity2Position();

				// Handle collision
				for (Player player : sala.getPlayers()) {
					if ((projectile.getOwner().getPlayerId() != player.getPlayerId()) && player.intersect(projectile)) {
						// System.out.println("Player " + player.getPlayerId() + " was hit!!!");
						projectile.setHit(true);
						player.decreaseLife();
						// Añade puntuación en cada disparo acertado a un contrincante
						if (player.isAlive()) {
							if (player.getLife() != 0) {
								projectile.getOwner().increaseScore(scorePerHit);
							} else {
								projectile.getOwner().increaseScore(bonusScorePerHit); // El disparo que mata tiene un bonus
							}
						}
						break;
					}
				}

				ObjectNode jsonProjectile = mapper.createObjectNode();
				jsonProjectile.put("id", projectile.getId());

				if (!projectile.isHit() && projectile.isAlive(thisInstant)) {
					jsonProjectile.put("posX", projectile.getPosX());
					jsonProjectile.put("posY", projectile.getPosY());
					jsonProjectile.put("facingAngle", projectile.getFacingAngle());
					jsonProjectile.put("isAlive", true);
				} else {
					removeBullets = true;
					bullets2Remove.add(projectile.getId());
					jsonProjectile.put("isAlive", false);
					if (projectile.isHit()) {
						jsonProjectile.put("isHit", true);
						jsonProjectile.put("posX", projectile.getPosX());
						jsonProjectile.put("posY", projectile.getPosY());
					}
				}
				arrayNodeProjectiles.addPOJO(jsonProjectile);
			}

			if (removeBullets)
				this.projectiles.keySet().removeAll(bullets2Remove);

			json.put("event", "GAME STATE UPDATE");
			json.putPOJO("players", arrayNodePlayers);
			json.putPOJO("projectiles", arrayNodeProjectiles);

			this.sala.broadcast(json.toString());
		} catch (Throwable ex) {

		}
	}

	public void handleCollision() {

	}
}
