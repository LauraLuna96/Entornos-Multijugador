package spacewar;

// CLASE PADRE DE POWER UPS //

public class PowerUp {
	private final int posX;
	private final int posY;
	private final int id;
	private static final int POWERUP_COLLISION_FACTOR = 200;

	PowerUp(int posX, int posY, int id) {
		this.posX = posX;
		this.posY = posY;
		this.id = id;
	}

	public int getPosX() {
		return this.posX;
	}

	public int getPosY() {
		return this.posY;
	}

	public int getId() {
		return this.id;
	}

}
