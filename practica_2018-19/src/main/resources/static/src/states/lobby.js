Spacewar.lobbyState = function(game) {

}

Spacewar.lobbyState.prototype = {

	init : function() {
		if (game.global.DEBUG_MODE) {
			console.log("[DEBUG] Entering **LOBBY** state");
		}
	},

	preload : function() {

	},

	create : function() {
		//game.state.start('matchmakingState')
		$("#menu-lobby").show();
		//$("#btn-createRoom").click(function(){createRoom()});
		//$("#btn-leaveLobby").click(function(){leaveLobby()});
	},

	update : function() {

	},

	shutdown : function() {
		$("#menu-lobby").hide();
	}
}

function createRoom() {
	let msg = new Object()
	msg.event = 'NEW ROOM'
	msg.roomName = 'Sala de prueba'
	console.log("Enviada petición de creación de sala: ")
	game.global.socket.send(JSON.stringify(msg))
	game.state.start('roomState')
}

function joinRoom(roomName) {
	let msg = new Object()
	msg.event = 'JOIN ROOM'
	msg.roomName = roomName;
	console.log("Enviada petición de unirse a sala: " + roomName)
	game.global.socket.send(JSON.stringify(msg))
	game.state.start('roomState')
}

function leaveLobby() {
	let msg = new Object()
	msg.event = 'LEAVE LOBBY'
	console.log("Enviada petición de salir del lobby")
	game.global.socket.send(JSON.stringify(msg))
	game.state.start('menuState')
}