Spacewar.preloadState = function(game) {

}

Spacewar.preloadState.prototype = {

	init : function() {
		if (game.global.DEBUG_MODE) {
			console.log("[DEBUG] Entering **PRELOAD** state");
		}
	},

	preload : function() {
		game.load.atlas('spacewar', 'assets/atlas/spacewar.png',
				'assets/atlas/spacewar.json',
				Phaser.Loader.TEXTURE_ATLAS_JSON_HASH)
		game.load.atlas('explosion', 'assets/atlas/explosion.png',
				'assets/atlas/explosion.json',
				Phaser.Loader.TEXTURE_ATLAS_JSON_HASH)
		
		// Carga las imágenes de los botones
		/*game.load.image('bLobby','assets/images/placeholders/button.png'); 
        game.load.image('bMatchmaking', 'assets/images/partida1.png'); */
	},

	create : function() {
		game.state.start('menuState')
	},

	update : function() {

	}
}