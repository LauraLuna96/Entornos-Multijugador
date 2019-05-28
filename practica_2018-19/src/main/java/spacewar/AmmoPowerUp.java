package spacewar;

public class AmmoPowerUp extends GenericPowerUp {

	private final int ammo = 10;
	public final String type = "AMMO";

	public AmmoPowerUp(int id) {
		super(id, "AMMO");
	}

	@Override
	public void applyPowerUp(Player player) {
		player.increaseAmmo(ammo);
		player.increaseScore(5);
	}

}
