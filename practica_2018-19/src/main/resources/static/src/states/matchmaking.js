Spacewar.matchmakingState = function(game) {

}

Spacewar.matchmakingState.prototype = {

	init : function() {
		if (game.global.DEBUG_MODE) {
			console.log("[DEBUG] Entering **MATCH-MAKING** state");
		}
	},

	preload : function() {
		if (game.global.DEBUG_MODE) {
			console.log("[DEBUG] Joining room...");
		}
	},

	create : function() {
		$("#menu-matchmaking").show();
	},

	update : function() {
		if (typeof game.global.myPlayer.room !== 'undefined') {
			if (game.global.DEBUG_MODE) {
				console.log("[DEBUG] Joined room " + game.global.myPlayer.room);
			}
			game.state.start('roomState')
		}
	},

	shutdown : function() {
		$("#menu-matchmaking").hide();
	}
}

function leaveMatchmaking() {
	let msg = new Object()
	msg.event = 'LEAVE MATCHMAKING'
	console.log("Enviada petición de salid del matchmaking")
	game.global.socket.send(JSON.stringify(msg))
}