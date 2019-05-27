Spacewar.endState = function(game) {

}

Spacewar.endState.prototype = {

	init : function() {
		if (game.global.DEBUG_MODE) {
			console.log("[DEBUG] Entering **END** state");
		}
	},

	preload : function() {
		
	},

	create : function() {
        game.add.text(50, 100, ('Soy un texto! la partida ha finalizado! :D'), {font: '50px Orbitron', fill: '#c6f9ac'});
	},

	update : function() {

	}
}