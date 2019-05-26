Spacewar.menuState = function (game) {

}

Spacewar.menuState.prototype = {

	init: function () {
		if (game.global.DEBUG_MODE) {
			console.log("[DEBUG] Entering **MENU** state");
		}
	},

	preload: function () {
		// In case JOIN message from server failed, we force it
		/*if (typeof game.global.myPlayer.id == 'undefined') {
			if (game.global.DEBUG_MODE) {
				console.log("[DEBUG] Forcing joining server...");
			}
			let message = {
				event: 'JOIN'
			}
			game.global.socket.send(JSON.stringify(message))
		}*/
	},

	create: function () {
		$("#menu-principal").show();
		//$("#btn-goLobby").click(function(){goLobby()});
		//$("#btn-goMatchmaking").click(function(){goMatchmaking()});
	},

	update : function() {
		/*if (typeof game.global.myPlayer.id !== 'undefined') {
			game.state.start('lobbyState')
		} */
	},

	shutdown : function() {
		$("#menu-principal").hide();
	}
}

function goLobby() {
	let msg = new Object()
	msg.event = 'JOIN LOBBY'
	console.log("Enviada petición de unirse al lobby")
	game.global.socket.send(JSON.stringify(msg))
	game.state.start('lobbyState');
}

function goMatchmaking() {
	game.state.start('matchmakingState');
}
