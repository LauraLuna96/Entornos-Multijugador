package spacewar;

public class PropellerPowerUp extends GenericPowerUp {
	
	private final int propeller = 3;
	
	public PropellerPowerUp(int id) {
		super(id);
	}

	@Override
	public void applyPowerUp(Player player) {
		player.increasePropellerUses(propeller);
	}

}
