package spacewar;

public class PropellerPowerUp extends GenericPowerUp {
	
	private final int propeller = 3;
	public final String type = "PROPELLER";
	
	public PropellerPowerUp(int id) {
		super(id, "PROPELLER");
	}

	@Override
	public void applyPowerUp(Player player) {
		player.increasePropellerUses(propeller);
		player.increaseScore(5);
	}

}
