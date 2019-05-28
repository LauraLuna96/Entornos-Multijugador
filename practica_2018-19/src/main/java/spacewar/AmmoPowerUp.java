package spacewar;

public class AmmoPowerUp extends GenericPowerUp {

	private final int ammo = 10;

	public AmmoPowerUp(int id) {
		super(id);
	}

	@Override
	public void applyPowerUp(Player player) {
		player.increaseAmmo(ammo);

	}

}
