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
		//$("#btn-leaveRoom").click(function(){leaveRoom();});
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
	console.log(game.global.currentSala.players)
}

function leaveRoom() {
	let msg = new Object();
	msg.event = 'LEAVE ROOM';
	console.log("Enviada petición para salir de la sala");
	game.global.socket.send(JSON.stringify(msg));
	game.state.start('lobbyState');
}