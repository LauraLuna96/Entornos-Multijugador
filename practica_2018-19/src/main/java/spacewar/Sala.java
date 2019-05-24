package spacewar;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Sala {

	private SpacewarGame game;
	private Map<String, Player> players = new ConcurrentHashMap<>();
	private String name;
	private AtomicInteger numPlayers = new AtomicInteger(0);
	
	Sala(String name) {
		this.name = name;
		this.game = new SpacewarGame(this);
	}
	
	public int getNumPlayers() {
		return numPlayers.get();
	}

	public SpacewarGame getGame() {
		return game;
	}

	public Map<String, Player> getPlayersMap() {
		return players;
	}
	
	public Collection<Player> getPlayers() {
		return players.values();
	}

	public String getName() {
		return name;
	}
	
	public void addPlayer(Player player) {
		players.put(player.getSession().getId(), player);

		int count = numPlayers.getAndIncrement();
		if (count == 0) {
			game.startGameLoop();
		}
	}
	
	public void removePlayer(Player player) {
		players.remove(player.getSession().getId());

		int count = this.numPlayers.decrementAndGet();
		if (count == 0) {
			game.stopGameLoop();
		}
	}
	
	// Método que manda un mensaje específico a TODOS los jugadores (de la sala)
	public void broadcast(String message) {
		for (Player player : getPlayers()) {
			try {
				player.sendMessage(message.toString()); // Usamos el método sendMessage del player porque está protegido por EM
			} catch (Throwable ex) {
				System.err.println("Execption sending message to player " + player.getSession().getId());
				ex.printStackTrace(System.err);
				this.removePlayer(player);
			}
		}
	}
	
	// Método que devuelve la colección de jugadores escrita en JSON
	public String playerString() {
		String result = "";
		for (Player player : getPlayers()) {
			result+=player.getPlayerName()+",";
		}
		result=result.substring(0,result.length()-1);
		return result;
	}

}
