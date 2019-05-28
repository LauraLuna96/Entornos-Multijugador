package spacewar;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
	// private AtomicInteger playerId = new AtomicInteger(0); Cada sala tiene su
	// propia cuenta de playerIds
	private AtomicInteger projectileId = new AtomicInteger(0);

	// Salas y jugadores
	private Map<String, Player> globalPlayers = new ConcurrentHashMap<>(); // Mapa de jugadores global
	private Map<String, Player> lobbyPlayers = new ConcurrentHashMap<>(); // Mapa de jugadores en el lobby
	private Map<String, Sala> salas = new ConcurrentHashMap<>(); // Mapa de salas existentes

	// Matchmaking
	private BlockingQueue<Player> awaitingMatchmaking = new LinkedBlockingQueue<Player>();

	// Waiting list
	private ScheduledExecutorService waitingListScheduler = Executors.newScheduledThreadPool(1);
	public BlockingQueue<Player> lastAddedToWaitingLists = new LinkedBlockingQueue<Player>();

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {

		// Sacamos el nombre del jugador de la Uri con la que se ha conectado del
		// websocket,
		// que será de la forma ip:puerto/spacewars/{playerName}
		String[] uri = session.getUri().toString().split("/");
		String playerName = uri[uri.length - 1];

		if (!globalPlayers.containsKey(playerName)) {
			Player player = new Player(session, playerName);

			System.out.println("[SYS] New player " + playerName + " created.");
			session.getAttributes().put(PLAYER_ATTRIBUTE, player);

			/*
			 * ObjectNode msg = mapper.createObjectNode(); msg.put("event", "JOIN");
			 * msg.put("id", player.getPlayerId()); msg.put("shipType",
			 * player.getShipType()); player.sendMessage(msg.toString());
			 */

			globalPlayers.put(player.getPlayerName(), player); // Añade el jugador al mapa de jugadores global

			ObjectNode msg = mapper.createObjectNode();
			msg.put("event", "CONFIRMATION");
			msg.put("type", "CORRECT NAME");
			session.sendMessage(new TextMessage(msg.toString()));
			// game.addPlayer(player);
		} else {
			// El nombre del jugador ya existe!
			System.out.println(
					"[SYS] Player name " + playerName + " is already taken. Rejecting new player with same name.");
			ObjectNode msg = mapper.createObjectNode();
			msg.put("event", "ERROR");
			msg.put("type", "PLAYER NAME TAKEN ERROR");
			session.sendMessage(new TextMessage(msg.toString()));
		}
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

			// Un jugador quiere usar el matchmaking automático
			case "JOIN MATCHMAKING":
				awaitingMatchmaking.add(player);
				msg.put("event", "JOIN MATCHMAKING");
				player.sendMessage(msg.toString());
				break;

			// Un jugador quiere salir del matchmaking automático
			case "LEAVE MATCHMAKING":
				awaitingMatchmaking.remove(player);
				msg.put("event", "CONFIRMATION");
				msg.put("event", "LEAVE MATCHMAKING");
				player.sendMessage(msg.toString());
				break;

			case "LEAVE WAITING":
				if (lastAddedToWaitingLists.remove(player)) {
					removePlayerFromWaitingList(player);
				}
				msg.put("event", "LEAVE WAITING");
				player.sendMessage(msg.toString());
				sendGetRoomsMessage(player);
				break;

			// Un jugador ha entrado al lobby
			case "JOIN LOBBY":
				lobbyPlayers.put(player.getPlayerName(), player); // Añade el jugador al lobby
				sendGetRoomsMessage(player);
				break;

			// Un jugador ha salido del lobby
			case "LEAVE LOBBY":
				lobbyPlayers.remove(player.getPlayerName()); // Quita al jugador del lobby
				break;

			// Un jugador se ha unido a una sala
			case "JOIN ROOM":

				String roomName = node.get("roomName").asText(); // Sacamos el nombre de la sala
				Sala s = salas.get(roomName); // y con él, la sala del mapa

				addPlayerToRoom(player, s);
				break;

			// Un jugador ha creado una sala
			case "NEW ROOM":
				lobbyPlayers.remove(player.getPlayerName()); // Quita al jugador del lobby
				Sala room = new Sala(node.get("roomName").asText()); // Crea la sala

				System.out.println("[ROOM] New room " + room.getName() + " created.");
				salas.put(room.getName(), room); // Guarda la sala en el mapa

				System.out.println("[ROOM] Player " + player.getPlayerName() + " joined the room " + room.getName());
				addPlayerToRoom(player, room);
				break;

			// Algo ha cambiado en la info. de una sala
			/*
			 * case "UPDATE ROOM": msg.put("event", "ROOM INFO"); msg.put("roomName",
			 * sala.getName()); ArrayNode arrayNodePlayers3 = mapper.createArrayNode(); for
			 * (Player p : sala.getPlayers()) { ObjectNode jsonPlayer =
			 * mapper.createObjectNode(); jsonPlayer.put("playerName", p.getPlayerName());
			 * jsonPlayer.put("life", p.getLife()); arrayNodePlayers3.addPOJO(jsonPlayer); }
			 * msg.putPOJO("players", arrayNodePlayers3);
			 * player.sendMessage(msg.toString()); break;
			 */

			// Un jugador ha salido de la sala donde estaba
			case "LEAVE ROOM":
				sala.removePlayer(player);
				System.out.println("[ROOM] Player " + player.getPlayerName() + " left the room " + sala.getName());

				// Comprobamos si hay algún player en la waiting room
				addPlayerToRoomFromWaitingList(sala);

				// Comprobamos si queda algún jugador más
				if (sala.getNumPlayers() <= 0) {

					System.out.println("[ROOM] Room " + sala.getName() + " is empty. Deleting it now.");
					msg.put("event", "DELETE ROOM");
					msg.put("roomName", sala.getName());
					sendMessageToAllInLobby(msg.toString());
					salas.remove(sala.getName());

				} else {
					msg.put("event", "LEAVE ROOM");
					msg.put("playerName", player.getPlayerName());
					sala.broadcast(msg.toString());

					if (sala.getCurrentState() == "Partida") {
						ObjectNode msg2 = mapper.createObjectNode();
						msg2.put("event", "REMOVE PLAYER");
						msg2.put("id", player.getPlayerId());
						sala.broadcast(msg2.toString());
					}
				}

				session.getAttributes().remove(ROOM_ATTRIBUTE);
				lobbyPlayers.put(player.getPlayerName(), player); // Añade el jugador al lobby

				sendGetRoomsMessageAll();
				break;

			//////////////////////////////////////////////////////
			// PARTIDA

			// Un jugador se ha unido a la partida (ahora este mensaje lo envia el servidor al cliente)
			case "JOIN":
				/*
				 * msg.put("event", "JOIN"); msg.put("id", player.getPlayerId());
				 * msg.put("shipType", player.getShipType());
				 * player.sendMessage(msg.toString());
				 */
				break;

			// Un jugador quiere iniciar la partida manualmente (solo si hay más de 1
			// jugador en la sala, pero aún no se ha llegado al nº de jugadores maximo)
			case "START GAME":
				if (sala.tryStartGame())
					sendGetRoomsMessageAll();
				break;

			// Actualizar posición
			case "UPDATE MOVEMENT":
				player.loadMovement(node.path("movement").get("thrust").asBoolean(),
						node.path("movement").get("brake").asBoolean(),
						node.path("movement").get("rotLeft").asBoolean(),
						node.path("movement").get("rotRight").asBoolean(), node.path("propeller").asBoolean());
				if (node.path("bullet").asBoolean()) {
					Projectile projectile = new Projectile(player, this.projectileId.incrementAndGet());
					// Gestiona el número de balas
					if (projectile.getOwner().getAmmo() > 0) {
						projectile.getOwner().decreaseAmmo();
					}
					if (projectile.getOwner().getAmmo() > 0) {
						sala.getGame().addProjectile(projectile.getId(), projectile);
					}
				}
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
		if (player == null)
			return;
		globalPlayers.remove(player.getPlayerName());

		Sala sala = (Sala) session.getAttributes().get(ROOM_ATTRIBUTE);
		if (sala != null) {
			sala.removePlayer(player);
			ObjectNode msg = mapper.createObjectNode();
			msg.put("event", "LEAVE ROOM");
			msg.put("playerName", player.getPlayerName());
			sala.broadcast(msg.toString());

			addPlayerToRoomFromWaitingList(sala);
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

	public void sendGetRoomsMessage(Player player) throws Exception {

		ObjectNode msg = mapper.createObjectNode();
		msg.put("event", "GET ROOMS");

		ArrayNode arrayNode = mapper.createArrayNode();
		for (Sala sa : salas.values()) {
			ObjectNode jsonSala = mapper.createObjectNode();
			jsonSala = sa.getSalaAsObjectNode(jsonSala);
			arrayNode.addPOJO(jsonSala);
		}
		msg.putPOJO("salas", arrayNode);

		player.sendMessage(msg.toString());
	}

	public void sendGetRoomsMessageAll() throws Exception {

		ObjectNode msg = mapper.createObjectNode();
		msg.put("event", "GET ROOMS");

		ArrayNode arrayNode = mapper.createArrayNode();
		for (Sala sa : salas.values()) {
			ObjectNode jsonSala = mapper.createObjectNode();
			jsonSala = sa.getSalaAsObjectNode(jsonSala);
			arrayNode.addPOJO(jsonSala);
		}
		msg.putPOJO("salas", arrayNode);

		sendMessageToAllInLobby(msg.toString());
	}

	public void sendRoomInfoMessage(Sala sala) {

		ObjectNode msg = mapper.createObjectNode();

		msg.put("event", "ROOM INFO");
		msg.put("roomName", sala.getName());

		ArrayNode arrayNode = mapper.createArrayNode();
		for (Player p : sala.getPlayers()) {
			ObjectNode jsonPlayer = mapper.createObjectNode();
			jsonPlayer.put("playerName", p.getPlayerName());
			jsonPlayer.put("life", p.getLife());
			jsonPlayer.put("ammo", p.getAmmo());
			jsonPlayer.put("propeller", p.getPropellerUses());
			jsonPlayer.put("score", p.getScore());

			arrayNode.addPOJO(jsonPlayer);
		}
		msg.putPOJO("players", arrayNode);
		sala.broadcast(msg.toString());
	}

	public void removeFromWaitingList() {
		Player player = lastAddedToWaitingLists.poll();
		if (player == null) {
			System.out.println(
					"[ERROR] Error while removing player from waiting list (they might have cancelled the wait manually)");
		}
		Sala sala = (Sala) player.getSession().getAttributes().get(ROOM_ATTRIBUTE);
		if (sala.waitingList.remove(player)) {
			player.getSession().getAttributes().remove(ROOM_ATTRIBUTE);
			lobbyPlayers.put(player.getPlayerName(), player); // Añade el jugador al lobby
			ObjectNode msg = mapper.createObjectNode();
			msg.put("event", "LEAVE WAITING");
			System.out.println("[ROOM] Player " + player.getPlayerName() + " removed from waiting list.");
			try {
				player.sendMessage(msg.toString());
				sendGetRoomsMessage(player);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void removePlayerFromWaitingList(Player player) {
		Sala sala = (Sala) player.getSession().getAttributes().get(ROOM_ATTRIBUTE);
		if (sala.waitingList.remove(player)) {
			player.getSession().getAttributes().remove(ROOM_ATTRIBUTE);
			lobbyPlayers.put(player.getPlayerName(), player); // Añade el jugador al lobby
			ObjectNode msg = mapper.createObjectNode();
			msg.put("event", "LEAVE WAITING");
			System.out.println("[ROOM] Player " + player.getPlayerName() + " removed from waiting list.");
			try {
				player.sendMessage(msg.toString());
				sendGetRoomsMessage(player);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public boolean checkMatchmaking(Sala sala) {
		Player addPlayer = awaitingMatchmaking.poll();
		if (addPlayer == null) {
			return false;
		} else {
			try {
				addPlayerToRoom(addPlayer, sala);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}
	}

	public void addPlayerToRoom(Player player, Sala s) throws Exception {
		ObjectNode msg = mapper.createObjectNode();
		String result = s.addPlayer(player);
		switch (result) {
		case "joined":
			lobbyPlayers.remove(player.getPlayerName()); // Quita al jugador del lobby
			player.getSession().getAttributes().put(ROOM_ATTRIBUTE, s); // Guardamos la sala en la sesión de ws
			System.out.println("[ROOM] Player " + player.getPlayerName() + " joined the room " + s.getName());

			msg.put("event", "JOIN ROOM");
			msg.put("roomName", s.getName());
			player.sendMessage(msg.toString());
			sendRoomInfoMessage(s);
			sendGetRoomsMessageAll();

			// Si la partida ya estaba iniciada, mandamos el mensaje de partida
			if (s.getCurrentState() == "Partida") {
				try {
					s.getGame().sendBeginningMessageTo(player);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			// Ahora comprobamos si la sala está llena, y si tenemos que empezar el juego
			// Es importante que esto se envíe después del mensaje de JOIN ROOM
			s.startGameIfFull();
			break;
		case "waiting":
			lobbyPlayers.remove(player.getPlayerName()); // Quita al jugador del lobby
			player.getSession().getAttributes().put(ROOM_ATTRIBUTE, s); // Guardamos la sala en la sesión de ws
			System.out.println("[ROOM] Player " + player.getPlayerName() + " is waiting to enter " + s.getName());
			msg.put("event", "WAITING ROOM");
			msg.put("roomName", s.getName());
			player.sendMessage(msg.toString());
			lastAddedToWaitingLists.put(player);
			waitingListScheduler.schedule(() -> removeFromWaitingList(), 5, TimeUnit.SECONDS);
			break;
		case "error":
			msg.put("event", "ERROR");
			msg.put("type", "JOIN ROOM ERROR");
			player.sendMessage(msg.toString());
			break;
		}
	}

	public void addPlayerToRoomFromWaitingList(Sala sala) throws Exception {
		ObjectNode msg = mapper.createObjectNode();
		Player addPlayer = sala.tryAddPlayerFromWaitingRoom();
		if (addPlayer != null) {

			msg.put("event", "JOIN ROOM");
			msg.put("roomName", sala.getName());
			addPlayer.sendMessage(msg.toString());
			sendRoomInfoMessage(sala);
			sendGetRoomsMessageAll();
			sala.startGameIfFull();

			// Si la partida ya estaba iniciada, mandamos el mensaje de partida
			if (sala.getCurrentState() == "Partida") {
				try {
					sala.getGame().sendBeginningMessageTo(addPlayer);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
