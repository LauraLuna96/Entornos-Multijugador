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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WebsocketGameHandler extends TextWebSocketHandler {

	// private SpacewarGame game = SpacewarGame.INSTANCE; Cada sala tendrá su propia
	// instancia
	private static final String PLAYER_ATTRIBUTE = "PLAYER";
	private static final String ROOM_ATTRIBUTE = "ROOM";
	private ObjectMapper mapper = new ObjectMapper();
	private AtomicInteger playerId = new AtomicInteger(0);
	private AtomicInteger projectileId = new AtomicInteger(0);
	private Map<String, Player> globalPlayers = new ConcurrentHashMap<>(); // Mapa de jugadores global
	private Map<String, Player> lobbyPlayers = new ConcurrentHashMap<>(); // Mapa de jugadores en el lobby
	private Map<String, Sala> salas = new ConcurrentHashMap<>(); // Mapa de salas existentes

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {

		// Sacamos el nombre del jugador de la Uri con la que se ha conectado del
		// websocket,
		// que será de la forma ip:puerto/spacewars/{playerName}
		String[] uri = session.getUri().toString().split("/");
		String playerName = uri[uri.length - 1];
		Player player = new Player(playerId.incrementAndGet(), session, playerName);

		System.out.println("[SYS] New player " + playerName + " created.");
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
			Sala sala = (Sala) session.getAttributes().get(ROOM_ATTRIBUTE);

			switch (node.get("event").asText()) {
			//////////////////////////////////////////////////////
			// SALAS

			// Un jugador ha entrado al lobby
			case "JOIN LOBBY":
				lobbyPlayers.put(player.getPlayerName(), player); // Añade el jugador al lobby
				
				msg.put("event", "GET ROOMS");
				ArrayNode arrayNode = mapper.createArrayNode();
				for (Sala s : salas.values()) {
					ObjectNode jsonSala = mapper.createObjectNode();
					jsonSala.put("roomName", s.getName());
					jsonSala.put("numPlayers", s.getNumPlayers());
					jsonSala.put("maxPlayers", 2);
					arrayNode.addPOJO(jsonSala);
				}
				msg.putPOJO("salas", arrayNode);
				player.sendMessage(msg.toString());
				break;

			// Un jugador ha salido del lobby
			case "LEAVE LOBBY":
				lobbyPlayers.remove(player.getPlayerName()); // Quita al jugador del lobby
				break;

			// Un jugador se ha unido a una sala
			case "JOIN ROOM":
				lobbyPlayers.remove(player.getPlayerName()); // Quita al jugador del lobby

				String roomName = node.get("roomName").asText(); // Sacamos el nombre de la sala
				Sala s = salas.get(roomName); // y con él, la sala del mapa
				session.getAttributes().put(ROOM_ATTRIBUTE, s); // y la huardamos en la sesión de ws
				s.addPlayer(player);

				System.out.println("[ROOM] Player " + player.getPlayerName() + " joined the room " + s.getName());
				
				msg.put("event", "ROOM INFO");					// Después, enviamos al jugador un msg
				msg.put("roomName", roomName);					// con la info de la sala a la que ha entrado
				//msg.put("players", s.playerString());
				ArrayNode arrayNodePlayers = mapper.createArrayNode();
				for (Player p : s.getPlayers()) {
					ObjectNode jsonPlayer = mapper.createObjectNode();
					jsonPlayer.put("playerName", p.getPlayerName());
					jsonPlayer.put("life", p.getLife());
					arrayNodePlayers.addPOJO(jsonPlayer);
				}
				msg.putPOJO("players", arrayNodePlayers);
				player.sendMessage(msg.toString());
				break;

			// Un jugador ha creado una sala
			case "NEW ROOM":
				lobbyPlayers.remove(player.getPlayerName()); // Quita al jugador del lobby
				Sala room = new Sala(node.get("roomName").asText()); // Crea la sala

				System.out.println("[ROOM] New room " + room.getName() + " created.");

				salas.put(room.getName(), room); // Guarda la sala en el mapa
				msg.put("event", "NEW ROOM"); // y avisa a los demás jugadores del lobby
				msg.put("roomName", room.getName()); // de que se ha creado una sala nueva
				sendMessageToAllInLobby(msg.toString()); // para que la muestren en la lista

				room.addPlayer(player);

				System.out.println("[ROOM] Player " + player.getPlayerName() + " joined the room " + room.getName());
				
				session.getAttributes().put(ROOM_ATTRIBUTE, room);	// Guardamos la sala en la sesión de ws
				ObjectNode msg2 = mapper.createObjectNode();	// y mandamos el mensaje al jugador
				msg2.put("event", "ROOM INFO");					// con la info de la sala a la que ha
				msg2.put("roomName", room.getName());			// entrado (nombre, otros jugadores, etc)
				ArrayNode arrayNodePlayers2 = mapper.createArrayNode();
				for (Player p : sala.getPlayers()) {
					ObjectNode jsonPlayer = mapper.createObjectNode();
					jsonPlayer.put("playerName", p.getPlayerName());
					jsonPlayer.put("life", p.getLife());
					arrayNodePlayers2.addPOJO(jsonPlayer);
				}
				msg2.putPOJO("players", arrayNodePlayers2);
				player.sendMessage(msg2.toString());
				
				msg.put("event", "GET ROOMS");
				ArrayNode arrayNode2 = mapper.createArrayNode();
				for (Sala sa : salas.values()) {
					ObjectNode jsonSala = mapper.createObjectNode();
					jsonSala.put("roomName", sa.getName());
					jsonSala.put("numPlayers", sa.getNumPlayers());
					jsonSala.put("maxPlayers", 2);
					arrayNode2.addPOJO(jsonSala);
				}
				msg.putPOJO("salas", arrayNode2);
				sendMessageToAllInLobby(msg.toString());
				break;

			// Algo ha cambiado en la info. de una sala
			case "UPDATE ROOM":
				msg.put("event", "ROOM INFO");
				msg.put("roomName", sala.getName());
				ArrayNode arrayNodePlayers3 = mapper.createArrayNode();
				for (Player p : sala.getPlayers()) {
					ObjectNode jsonPlayer = mapper.createObjectNode();
					jsonPlayer.put("playerName", p.getPlayerName());
					jsonPlayer.put("life", p.getLife());
					arrayNodePlayers3.addPOJO(jsonPlayer);
				}
				msg.putPOJO("players", arrayNodePlayers3);
				player.sendMessage(msg.toString());
				break;

			// Un jugador ha salido de la sala donde estaba
			case "LEAVE ROOM":
				sala.removePlayer(player);
				System.out.println("[ROOM] Player " + player.getPlayerName() + " left the room " + sala.getName());
				if (sala.getNumPlayers() <= 0) {
					System.out.println("[ROOM] Room " + sala.getName() + " is empty. Deleting it now.");
					msg.put("event", "DELETE ROOM");
					msg.put("roomName", sala.getName());
					sendMessageToAllInLobby(msg.toString());
					salas.remove(sala.getName());
					session.getAttributes().remove(ROOM_ATTRIBUTE);
					
					msg.put("event", "GET ROOMS");
					ArrayNode arrayNode3 = mapper.createArrayNode();
					for (Sala sa : salas.values()) {
						ObjectNode jsonSala = mapper.createObjectNode();
						jsonSala.put("roomName", sa.getName());
						jsonSala.put("numPlayers", sa.getNumPlayers());
						jsonSala.put("maxPlayers", 2);
						arrayNode3.addPOJO(jsonSala);
					}
					msg.putPOJO("salas", arrayNode3);
					sendMessageToAllInLobby(msg.toString());
				} else {
					msg.put("event", "LEAVE ROOM");
					msg.put("playerName", player.getPlayerName());
					sala.broadcast(msg.toString());
				}
				lobbyPlayers.put(player.getPlayerName(), player); // Añade el jugador al lobby
				msg.put("event", "GET ROOMS");
				ArrayNode arrayNode3 = mapper.createArrayNode();
				for (Sala sa : salas.values()) {
					ObjectNode jsonSala = mapper.createObjectNode();
					jsonSala.put("roomName", sa.getName());
					jsonSala.put("numPlayers", sa.getNumPlayers());
					jsonSala.put("maxPlayers", 2);
					arrayNode3.addPOJO(jsonSala);
				}
				msg.putPOJO("salas", arrayNode3);
				player.sendMessage(msg.toString());
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

			case "TAKE HIT":
				msg.put("event", "TAKE HIT");
				msg.put("playerName", player.getPlayerName());
				msg.put("life", player.decreaseLife());
				sala.broadcastExcept(msg.toString(),player.getPlayerName());
				break;

			//////////////////////////////////////////////////////
			// CHAT

			case "CHAT MSG":
				String text = node.get("text").asText();
				System.out.println("[CHAT] Message received [" + player.getPlayerName() + "]: " + text);
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

		Sala sala = (Sala) session.getAttributes().get(ROOM_ATTRIBUTE);
		if (sala != null) {
			sala.removePlayer(player);
			ObjectNode msg = mapper.createObjectNode();
			msg.put("event", "LEAVE ROOM");
			msg.put("playerName", player.getPlayerName());
			sala.broadcast(msg.toString());
		}

		System.out.println("[SYS] Player " + player.getPlayerName() + " disconnected.");

		// ObjectNode msg = mapper.createObjectNode();
		// msg.put("event", "REMOVE PLAYER");
		// msg.put("id", player.getPlayerId());
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
