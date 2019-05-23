window.onload = function () {

	// Cosas de la interfaz/web
	$('#modal').modal({ backdrop: 'static', keyboard: false });
	$("#chatInput").keydown(function (e) {	// Cuando se pulse una tecla sobre el input del chat ...
		if (e.keyCode === 13) {  			// Si la tecla es enter
			submitChatMsg();					// enviamos el mensaje al servidor
		}
	});

	game = new Phaser.Game(1024, 600, Phaser.AUTO, 'gameDiv')

	// GLOBAL VARIABLES
	game.global = {
		FPS: 30,
		DEBUG_MODE: false,
		socket: null,
		myPlayer: new Object(),
		otherPlayers: [],
		projectiles: []
	}

	// WEBSOCKET CONFIGURATOR
	//game.global.socket = new WebSocket("ws://127.0.0.1:8080/spacewar/Hulio")

	// El ws se configura en una función a parte


	// PHASER SCENE CONFIGURATOR
	game.state.add('bootState', Spacewar.bootState)
	game.state.add('preloadState', Spacewar.preloadState)
	game.state.add('menuState', Spacewar.menuState)
	game.state.add('lobbyState', Spacewar.lobbyState)
	game.state.add('matchmakingState', Spacewar.matchmakingState)
	game.state.add('roomState', Spacewar.roomState)
	game.state.add('gameState', Spacewar.gameState)

	//game.state.start('bootState')

}

// Conexión con el servidor
function openWebsocket() {
	name = $("#nameInput").val();
	game.global.socket = new WebSocket("ws://127.0.0.1:8080/spacewar/" + name);
	configWebsocket();
}

// Chat
function submitChatMsg() {
	val = $('#chatInput').val();
	if (val == "") return;	// Si el mensaje está vacío no enviamos nada
	let msg = new Object()
	msg.event = 'CHAT MSG'
	msg.text = val;
	console.log("Chat msg sent: " + msg.text);
	game.global.socket.send(JSON.stringify(msg));
}

function showChatMsg(text, name) {
	maxMsgs = 40;
	if ($("#chatArea").children().length >= maxMsgs) {
		$("#chatArea").find(':first-child').remove();
	}
	$("#chatArea").append("<p><b>" + name + ":</b> " + text + "</p>")
}

function configWebsocket() {
	game.global.socket.onopen = () => {
		$(".modal").modal("hide")
		if (game.global.DEBUG_MODE) {
			console.log('[DEBUG] WebSocket connection opened.')
		}
	}

	game.global.socket.onclose = (e) => {
		if (game.global.DEBUG_MODE) {
			console.log('[DEBUG] WebSocket connection closed.')
		}

		if (e.code == 404) {
			console.log("Error 404");
		}
	}

	game.global.socket.onmessage = (message) => {
		var msg = JSON.parse(message.data)

		switch (msg.event) {
			case 'JOIN':
				if (game.global.DEBUG_MODE) {
					console.log('[DEBUG] JOIN message recieved')
					console.dir(msg)
				}
				game.global.myPlayer.id = msg.id
				game.global.myPlayer.shipType = msg.shipType
				if (game.global.DEBUG_MODE) {
					console.log('[DEBUG] ID assigned to player: ' + game.global.myPlayer.id)
				}
				break
			case 'NEW ROOM':
				if (game.global.DEBUG_MODE) {
					console.log('[DEBUG] NEW ROOM message recieved')
					console.dir(msg)
				}
				game.global.myPlayer.room = {
					name: msg.room
				}
				break
			case 'GAME STATE UPDATE':
				if (game.global.DEBUG_MODE) {
					console.log('[DEBUG] GAME STATE UPDATE message recieved')
					console.dir(msg)
				}
				if (typeof game.global.myPlayer.image !== 'undefined') {
					for (var player of msg.players) {
						if (game.global.myPlayer.id == player.id) {
							game.global.myPlayer.image.x = player.posX
							game.global.myPlayer.image.y = player.posY
							game.global.myPlayer.image.angle = player.facingAngle
						} else {
							if (typeof game.global.otherPlayers[player.id] == 'undefined') {
								game.global.otherPlayers[player.id] = {
									image: game.add.sprite(player.posX, player.posY, 'spacewar', player.shipType)
								}
								game.global.otherPlayers[player.id].image.anchor.setTo(0.5, 0.5)
							} else {
								game.global.otherPlayers[player.id].image.x = player.posX
								game.global.otherPlayers[player.id].image.y = player.posY
								game.global.otherPlayers[player.id].image.angle = player.facingAngle
							}
						}
					}

					for (var projectile of msg.projectiles) {
						if (projectile.isAlive) {
							game.global.projectiles[projectile.id].image.x = projectile.posX
							game.global.projectiles[projectile.id].image.y = projectile.posY
							if (game.global.projectiles[projectile.id].image.visible === false) {
								game.global.projectiles[projectile.id].image.angle = projectile.facingAngle
								game.global.projectiles[projectile.id].image.visible = true
							}
						} else {
							if (projectile.isHit) {
								// we load explosion
								let explosion = game.add.sprite(projectile.posX, projectile.posY, 'explosion')
								explosion.animations.add('explosion')
								explosion.anchor.setTo(0.5, 0.5)
								explosion.scale.setTo(2, 2)
								explosion.animations.play('explosion', 15, false, true)
							}
							game.global.projectiles[projectile.id].image.visible = false
						}
					}
				}
				break
			case 'REMOVE PLAYER':
				if (game.global.DEBUG_MODE) {
					console.log('[DEBUG] REMOVE PLAYER message recieved')
					console.dir(msg.players)
				}
				game.global.otherPlayers[msg.id].image.destroy()
				delete game.global.otherPlayers[msg.id]
				break
			case 'CHAT MSG':
				showChatMsg(msg.text, msg.player);
				break
			default:
				console.dir(msg)
				break
		}
	}

	// Una vez que hemos configurado el WS, empezamos el juego
	game.state.start('bootState');
}