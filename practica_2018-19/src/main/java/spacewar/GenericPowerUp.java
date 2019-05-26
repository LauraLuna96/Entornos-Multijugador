package spacewar;

// CLASE PADRE DE POWER UPS //

public abstract class GenericPowerUp extends SpaceObject {

	private final int id;
	private static final int POWERUP_COLLISION_FACTOR = 200;

	public GenericPowerUp(int id) {
		this.id = id;
	}

	public int getId() {
		return this.id;
	}
	
	public abstract void applyPowerUp(Player player);

}
