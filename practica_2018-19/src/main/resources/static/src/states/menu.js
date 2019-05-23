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
		if (typeof game.global.myPlayer.id == 'undefined') {
			if (game.global.DEBUG_MODE) {
				console.log("[DEBUG] Forcing joining server...");
			}
			let message = {
				event: 'JOIN'
			}
			game.global.socket.send(JSON.stringify(message))
		}
	},

	create: function () {

		//Se crean los botones y se les asignan funciones
		/*button = game.add.button(400, 450, 'bLobby', goLobby);
		button.scale.setTo(0.5,0.5);
		button = game.add.button(600, 450, 'bMatchmaking', goMatchmaking);
		button.scale.setTo(0.5,0.5);*/

	},

	update: function () {
		if (typeof game.global.myPlayer.id !== 'undefined') {
			game.state.start('lobbyState')
		}
	}
}

function goLobby() {
	game.state.start('lobbyState');
}

function goMatchmaking() {
	game.state.start('matchmakingState');
}
