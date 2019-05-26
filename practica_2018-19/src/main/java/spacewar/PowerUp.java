package spacewar;

// CLASE PADRE DE POWER UPS //

public class PowerUp extends SpaceObject {

	private final int id;
	private static final int POWERUP_COLLISION_FACTOR = 200;

	public PowerUp(int id) {
		this.id = id;
	}

	public int getId() {
		return this.id;
	}
	
	public void applyPowerUp(Player player) {}

}
