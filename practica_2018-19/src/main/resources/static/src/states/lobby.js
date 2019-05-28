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
	var roomName = $('#new-roomName').val();
	if (roomName == "") {
		$("#create-room-error").html("El nombre no puede estar vacío");
		$("#create-room-error").show();
		return;
	}
	var splChars = "*|,\":<>[]{}`\';()@&$#% ";
	for (i = 0; i < roomName.length; i++) {
		if (splChars.indexOf(roomName.charAt(i)) != -1) {
			// Caracteres no permitidos en el string!
			console.log("[ERROR] Invalid characters in room name!")
			$("#create-room-error").html("¡El nombre de la sala es inválido!");
			$("#create-room-error").show();
			return;
		}
	}
	$("#create-room-error").hide();
	$('#new-roomName').val("");
	let msg = new Object()
	msg.event = 'NEW ROOM'
	msg.roomName = roomName;
	console.log("Enviada petición de creación de sala: ")
	game.global.socket.send(JSON.stringify(msg))
}

function joinRoom(btn) {
	console.log(btn);
	var roomName = btn.data('roomname');
	console.log(roomName);
	let msg = new Object();
	msg.event = 'JOIN ROOM';
	msg.roomName = roomName;
	console.log("Enviada petición para entrar a la sala " + roomName);
	game.global.socket.send(JSON.stringify(msg));
}

function leaveLobby() {
	let msg = new Object()
	msg.event = 'LEAVE LOBBY'
	console.log("Enviada petición de salir del lobby")
	game.global.socket.send(JSON.stringify(msg))
	game.state.start('menuState')
}

function updateRoomList(rooms) {
	$("#area-lobby").html("");
	for (i = 0; i < rooms.length; i++) {
		if (rooms[i].state != "FinPartida"){
			$("#area-lobby").append('<div class="sala-row"><button type="button" data-roomname="'+rooms[i].roomName+'" onclick="joinRoom($(this));" class="btn btn-spacewars btn-sala"><i class="fas fa-sign-in-alt"></i> '+ rooms[i].roomName + '</button> '+ rooms[i].numPlayers+'/'+rooms[i].maxPlayers+' jugadores</div>')
		} else {
			$("#area-lobby").append('<div class="sala-row"><button type="button" data-roomname="'+rooms[i].roomName+'" class="btn btn-spacewars btn-sala btn-sala-unavailable" disabled><i class="fas fa-sign-in-alt"></i> '+ rooms[i].roomName + '</button> <b>[ PARTIDA ACABADA ]</b>  '+ rooms[i].numPlayers+'/'+rooms[i].maxPlayers+' jugadores</div>')
		}
	}
}