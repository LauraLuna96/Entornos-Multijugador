window.onload = function () {

	// Cosas de la interfaz/web

	// Abrimos el modal para la conexión al servidor
	$('#modal').modal({ backdrop: 'static', keyboard: false });

	// Asignamos un evento de keydown a la ventana de input del chat para que se envíe
	// el mensaje si pulsamos "enter" (además de si pulsamos el botón Enviar)
	$("#chatInput").keydown(function (e) {	// Cuando se pulse una tecla sobre el input del chat ...
		if (e.keyCode === 13) {  			// Si la tecla es enter
			submitChatMsg();				// enviamos el mensaje al servidor
		}
	});

	// Creamos el juego
	game = new Phaser.Game(1024, 600, Phaser.AUTO, 'gameDiv')

	// GLOBAL VARIABLES
	game.global = {
		FPS: 30,
		DEBUG_MODE: false,
		socket: null,
		myPlayer: new Object(),
		otherPlayers: [],
		projectiles: [],
		salas: [],
		currentSala: null
	}

	// WEBSOCKET CONFIGURATOR
	//game.global.socket = new WebSocket("ws://127.0.0.1:8080/spacewar/Hulio")
	// El ws se crea y configura en una función a parte

	// PHASER SCENE CONFIGURATOR
	game.state.add('bootState', Spacewar.bootState)
	game.state.add('preloadState', Spacewar.preloadState)
	game.state.add('menuState', Spacewar.menuState)
	game.state.add('lobbyState', Spacewar.lobbyState)
	game.state.add('matchmakingState', Spacewar.matchmakingState)
	game.state.add('roomState', Spacewar.roomState)
	game.state.add('gameState', Spacewar.gameState)
	game.state.add('endState', Spacewar.endState) // Creación del estado end para cuando finaliza una partida

	//game.state.start('bootState')
	// El juego se inicializa cuando el websocket se abre
}

// Conexión con el servidor
function openWebsocket() {
	name = $("#nameInput").val();
	if (name == "" | name.length < 4 | name.length > 20) {
		console.log("[ERROR] Player name is too short or too long!")
		return;
	}
	var splChars = "*|,\":<>[]{}`\';()@&$#% ";
	for (i = 0; i < name.length; i++) {
		if (splChars.indexOf(name.charAt(i)) != -1) {
			// Caracteres no permitidos en el string!
			console.log("[ERROR] Invalid characters in player name!")
			return;
		}
	}
	game.global.socket = new WebSocket("ws://127.0.0.1:8080/spacewar/" + name);
	configWebsocket();
}

// Chat
function submitChatMsg() {
	val = $('#chatInput').val();	// Cogemos el valor del input
	if (val == "") return;			// Si el mensaje está vacío no enviamos nada
	$('#chatInput').val("");		// Reseteamos el valor del input

	let msg = new Object()			// Mensaje a enviar por ws
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
	$("#chatArea").append("<p style='word-break:break-word;'><b>" + name + ":</b> " + text + "</p>")
}

// Configuración de websocket (eventos onclose/onopen, etc)
function configWebsocket() {
	game.global.socket.onopen = () => {
		if (game.global.DEBUG_MODE) {
			console.log('[DEBUG] WebSocket connection opened.')
		}
		//game.state.start('bootState');
		//$(".modal").modal("hide")
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

			///// MENSAJES DE ERROR Y CONFIRMACIÓN
			// Estos mensajes sirven para enviar información sobre diversos errores o confirmaciones
			// de acciones realizadas, y se gestionan en métodos a parte
			case 'ERROR':
				handleError(msg);
				break
			case 'CONFIRMATION':
				handleConfirmation(msg);
				break

			///// MENSAJES DEL CHAT
			// Mensajes para la gestión del chat
			case 'CHAT MSG':
				showChatMsg(msg.text, msg.player);
				break

			///// MENSAJES DE SALAS
			// Mensajes para la gestión de las salas
			case 'JOIN ROOM':
				console.log("Joined room " + msg.roomName)
				if (game.global.currentSala == null) game.global.currentSala = new Object();
				game.global.currentSala.roomName = msg.roomName;
				game.global.currentSala.players = msg.players;
				game.state.start('roomState')
				break
			case 'GET ROOMS':
				updateRoomList(msg.salas);
				break
			case 'NEW ROOM':	// No se usa (se pasa la info de todas las salas a la vez con get rooms)
				if (game.global.DEBUG_MODE) {
					console.log('[DEBUG] NEW ROOM message received')
					console.dir(msg)
				}
				/*game.global.myPlayer.room = {
					name: msg.room
				}*/
				break
			case 'ROOM INFO':
				if (game.global.DEBUG_MODE) {
					console.log('[DEBUG] ROOM INFO message received')
					console.dir(msg)
				}
				if (game.global.currentSala == null) game.global.currentSala = new Object();
				game.global.currentSala.roomName = msg.roomName;
				game.global.currentSala.players = msg.players;
				console.log(msg.players);
				updateSalaInfo();
				break
			case 'LEAVE ROOM':
				if (game.global.DEBUG_MODE) {
					console.log('[DEBUG] LEAVE ROOM message received')
					console.dir(msg)
				}
				var pos = -1;
				for (i = 0; i < game.global.currentSala.players.length; i++) {
					if (game.global.currentSala.players[i] == msg.playerName) {
						pos = i;
					}
				}
				if (pos >= 0) game.global.currentSala.players.remove(pos);
				updateSalaInfo();
				break;

			///// MENSAJES DE LA PARTIDA
			// Mensajes durante la partida
			case 'START GAME':
				console.log("Received start game message")
				game.state.start('gameState')
				break
			case 'JOIN':
				if (game.global.DEBUG_MODE) {
					console.log('[DEBUG] JOIN message received')
					console.dir(msg)
				}
				game.global.myPlayer.id = msg.id
				game.global.myPlayer.shipType = msg.shipType
				if (game.global.DEBUG_MODE) {
					console.log('[DEBUG] ID assigned to player: ' + game.global.myPlayer.id)
				}
				break
			case 'GAME STATE UPDATE':
				if (game.global.DEBUG_MODE) {
					console.log('[DEBUG] GAME STATE UPDATE message received')
					console.dir(msg)
				}
				if (typeof game.global.myPlayer.image !== 'undefined') {
					for (var player of msg.players) {
						if (game.global.myPlayer.id == player.id) {
							if (!player.isAlive) {
								console.log("Tas morio :(")
							} else {
								game.global.myPlayer.image.x = player.posX
								game.global.myPlayer.image.y = player.posY
								game.global.myPlayer.image.angle = player.facingAngle
								game.global.myPlayer.life = player.life
								game.global.myPlayer.isAlive = player.isAlive
								game.global.myPlayer.ammo = player.ammo
								game.global.myPlayer.propeller = player.propeller
							}
							//console.log("MyPlayer life: " + game.global.myPlayer.life);

						} else {

							if (typeof game.global.otherPlayers[player.id] == 'undefined') {
								game.global.otherPlayers[player.id] = {
									image: game.add.sprite(player.posX, player.posY, 'spacewar', player.shipType)
								}
								game.global.otherPlayers[player.id].image.anchor.setTo(0.5, 0.5)
							} else {
								if (!player.isAlive) {
									console.log("OtherPlayer[" + player.id + "] se ha morido :)")
								} else {
									game.global.otherPlayers[player.id].image.x = player.posX
									game.global.otherPlayers[player.id].image.y = player.posY
									game.global.otherPlayers[player.id].image.angle = player.facingAngle
									game.global.otherPlayers[player.id].life = player.life
									game.global.otherPlayers[player.id].isAlive = player.isAlive
								}
								//console.log("OtherPlayer["+ player.id +"] life: " + game.global.otherPlayers[player.id].life);
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
								// we load explosion :)
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
			case 'END GAME':
				console.log("Player " + msg.winner.playerName + " won! Exiting game state.")
				clearGame();
				game.state.start('endState');
				showResults(msg);
				break
			case 'REMOVE PLAYER':
				if (game.global.DEBUG_MODE) {
					console.log('[DEBUG] REMOVE PLAYER message received')
					console.dir(msg.players)
				}
				game.global.otherPlayers[msg.id].image.destroy()
				delete game.global.otherPlayers[msg.id]
				break
			default:
				console.dir(msg)
				break
		}
	}
}

function handleError(error) {
	switch (error.type) {
		case 'JOIN ROOM ERROR':
			game.state.start('lobbyState')
			console.log("[ERROR] There was an error while joining the room, you've been sent back to the lobby");
			break
		case 'PLAYER NAME TAKEN ERROR':
			console.log("[ERROR] The username was already taken! Choose a diferent one");
			game.global.socket.close();
			break
		default:
			console.log("[ERROR] Unknown error received, type: " + error.type);
			break
	}
}

function handleConfirmation(confirm) {
	switch (confirm.type) {
		case 'CORRECT NAME':
			game.state.start('bootState');
			$(".modal").modal("hide")
			break
		default:
			console.log("[CONFIRM] Unknown confirmation received, type: " + confirm.type);
			break
	}
}