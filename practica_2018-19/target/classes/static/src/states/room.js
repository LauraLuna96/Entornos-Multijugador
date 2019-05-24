Spacewar.roomState = function(game) {

}

Spacewar.roomState.prototype = {

	init : function() {
		if (game.global.DEBUG_MODE) {
			console.log("[DEBUG] Entering **ROOM** state");
		}
	},

	preload : function() {

	},

	create : function() {
		$("#menu-sala").show();
	},

	update : function() {
		//game.state.start('gameState')
	},

	shutdown : function() {
		$("#menu-sala").hide();
	}
}

function updateSalaInfo() {
	console.log("Updated current room info");
	$("#menu-sala-header").html("Sala " + game.global.currentSala.roomName);
}