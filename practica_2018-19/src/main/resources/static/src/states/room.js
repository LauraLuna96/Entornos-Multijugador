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
	var player_str = "<h4>Jugadores en la sala</h4>";
	for (var i = 0; i < game.global.currentSala.players.length; i++) {
		player_str += "<i class='fas fa-user-astronaut'></i> " + game.global.currentSala.players[i].playerName + "<br>";
	}
	$("#area-sala").html(player_str);
	//console.log(game.global.currentSala.players)
}

function leaveRoom() {
	let msg = new Object();
	msg.event = 'LEAVE ROOM';
	console.log("Enviada petición para salir de la sala");
	game.global.socket.send(JSON.stringify(msg));
	game.state.start('lobbyState');
}

function sendStartGame() {
	let msg = new Object();
	msg.event = 'START GAME';
	console.log("Enviada petición para iniciar partida");
	game.global.socket.send(JSON.stringify(msg));
}


function showWaiting() {
	$("#menu-lobby").hide();
	$("#menu-waiting").show();
}

function hideWaiting() {
	$("#menu-waiting").hide();
}

function leaveWaiting() {
	let msg = new Object();
	msg.event = 'LEAVE WAITING';
	console.log("Enviada petición para salir de la espera");
	game.global.socket.send(JSON.stringify(msg));
}