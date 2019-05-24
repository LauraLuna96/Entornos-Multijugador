package spacewar;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WebsocketGameHandler extends TextWebSocketHandler {

	// private SpacewarGame game = SpacewarGame.INSTANCE;
	private static final String PLAYER_ATTRIBUTE = "PLAYER";
	private ObjectMapper mapper = new ObjectMapper();
	private AtomicInteger playerId = new AtomicInteger(0);
	private AtomicInteger projectileId = new AtomicInteger(0);
	private Map<String, Player> globalPlayers = new ConcurrentHashMap<>(); // Mapa de jugadores global
	private Map<String, Player> lobbyPlayers = new ConcurrentHashMap<>(); // Mapa de jugadores en el lobby
	private Map<String, Sala> salas = new ConcurrentHashMap<>(); // Mapa de salas existentes

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		String[] uri = session.getUri().toString().split("/");
		String playerName = uri[uri.length - 1];
		Player player = new Player(playerId.incrementAndGet(), session, playerName);
		System.out.println("Created player with name: " + player.getPlayerName());
		session.getAttributes().put(PLAYER_ATTRIBUTE, player);

		/*
		 * ObjectNode msg = mapper.createObjectNode(); msg.put("event", "JOIN");
		 * msg.put("id", player.getPlayerId()); msg.put("shipType",
		 * player.getShipType()); player.sendMessage(msg.toString());
		 */

		globalPlayers.put(player.getPlayerName(), player); // Añade el jugador al mapa de jugadores global
		// game.addPlayer(player);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		try {
			JsonNode node = mapper.readTree(message.getPayload());
			ObjectNode msg = mapper.createObjectNode();
			Player player = (Player) session.getAttributes().get(PLAYER_ATTRIBUTE);

			switch (node.get("event").asText()) {
			//////////////////////////////////////////////////////
			// SALAS

			// Un jugador ha entrado al lobby
			case "JOIN LOBBY":
				lobbyPlayers.put(player.getPlayerName(), player); // Añade el jugador al lobby
				break;

			// Un jugador ha salido del lobby
			case "LEAVE LOBBY":
				lobbyPlayers.remove(player.getPlayerName()); // Quita al jugador del lobby
				break;

			// Un jugador se ha unido a una sala
			case "JOIN ROOM":
				lobbyPlayers.remove(player.getPlayerName()); // Quita al jugador del lobby
				msg.put("event", "NEW ROOM");
				msg.put("room", "GLOBAL");
				player.sendMessage(msg.toString());
				break;

			// Un jugador ha creado una sala
			case "NEW ROOM":
				lobbyPlayers.remove(player.getPlayerName()); // Quita al jugador del lobby
				Sala room = new Sala(node.get("name").asText()); // Crea la sala
				salas.put(room.getName(), room); // Guarda la sala
				msg.put("event", "NEW ROOM");
				msg.put("name", room.getName());
				sendMessageToAll(msg.toString());
				break;

			// Algo ha cambiado en la info. de una sala
			case "UPDATE ROOM":
				break;

			// Un jugador ha salido de la sala donde estaba
			case "LEAVE ROOM":
				lobbyPlayers.put(player.getPlayerName(), player); // Añade el jugador al lobby
				break;

			// Un jugador quiere recibir la info. de todas las salas disponibles
			case "GET ROOMS":
				break;

			//////////////////////////////////////////////////////
			// PARTIDA

			// Un jugador se ha unido a la partida
			case "JOIN":
				msg.put("event", "JOIN");
				msg.put("id", player.getPlayerId());
				msg.put("shipType", player.getShipType());
				player.sendMessage(msg.toString());
				break;

			// Actualizar posición
			case "UPDATE MOVEMENT":
				player.loadMovement(node.path("movement").get("thrust").asBoolean(),
						node.path("movement").get("brake").asBoolean(),
						node.path("movement").get("rotLeft").asBoolean(),
						node.path("movement").get("rotRight").asBoolean());
				if (node.path("bullet").asBoolean()) {
					Projectile projectile = new Projectile(player, this.projectileId.incrementAndGet());
					// game.addProjectile(projectile.getId(), projectile);
				}
				break;

			//////////////////////////////////////////////////////
			// CHAT

			case "CHAT MSG":
				String text = node.get("text").asText();
				System.out.println("Chat message received: " + text);
				msg.put("event", "CHAT MSG");
				msg.put("text", text);
				msg.put("player", player.getPlayerName());
				sendMessageToAll(msg.toString());
				break;
			default:
				break;
			}

		} catch (Exception e) {
			System.err.println("Exception processing message " + message.getPayload());
			e.printStackTrace(System.err);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		Player player = (Player) session.getAttributes().get(PLAYER_ATTRIBUTE);
		globalPlayers.remove(player.getPlayerName());

		ObjectNode msg = mapper.createObjectNode();
		msg.put("event", "REMOVE PLAYER");
		msg.put("id", player.getPlayerId());
		// game.broadcast(msg.toString());
	}

	// Método que envia un msg a todos los jugadores
	public void sendMessageToAll(String msg) throws Exception {
		Collection<Player> copy = globalPlayers.values();
		for (Player p : copy) {
			p.sendMessage(msg);
		}
	}

	// Método que envia un msg a todos los jugadores menos a uno
	public void sendMessageToAllExcept(String msg, String playername) throws Exception {
		Collection<Player> copy = globalPlayers.values();
		for (Player p : copy) {
			if (p.getPlayerName() != playername)
				p.sendMessage(msg);
		}
	}

	// Método que envia un msg a todos los jugadores en el lobby (fuera de sala)
	public void sendMessageToAllInLobby(String msg) throws Exception {
		Collection<Player> copy = lobbyPlayers.values();
		for (Player p : copy) {
			p.sendMessage(msg);
		}
	}
}
