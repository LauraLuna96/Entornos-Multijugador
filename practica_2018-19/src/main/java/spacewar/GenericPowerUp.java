package spacewar;

// CLASE PADRE DE POWER UPS //

public abstract class GenericPowerUp extends SpaceObject {

	private final int id;
	private static final int POWERUP_COLLISION_FACTOR = 200;
	private boolean isHit = false;
	public final String type;

	public GenericPowerUp(int id, String type) {
		this.id = id;
		this.type = type;
	}
	
	public boolean getIsHit() {
		return this.isHit;
	}
	
	public void setHit(boolean hit) {
		this.isHit = hit;
	}

	public int getId() {
		return this.id;
	}
	
	public abstract void applyPowerUp(Player player);

}
