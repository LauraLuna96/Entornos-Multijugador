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
        //game.add.text(50, 100, ('Soy un texto! la partida ha finalizado! :D'), {font: '30px Orbitron', fill: '#ffffff'});
		$("#menu-endgame").show();
	},

	update : function() {

	},

	shutdown : function() {
		$("#menu-endgame").hide();
	}
}

function showResults(msg) {
	$("#endgame-winner").html(msg.winner.playerName);
	var losers_str = "";
	for (var player of msg.losers) {
		losers_str += player.playerName + "<br>";
	}
	$("#endgame-losers").html(losers_str);
}