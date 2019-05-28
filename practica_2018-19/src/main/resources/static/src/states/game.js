Spacewar.gameState = function (game) {
	this.bulletTime
	this.propellerTime
	this.fireBullet
	this.usePropeller
	this.numStars = 100 // Should be canvas size dependant
	this.maxProjectiles = 800 // 8 per player
}

Spacewar.gameState.prototype = {

	init: function () {
		if (game.global.DEBUG_MODE) {
			console.log("[DEBUG] Entering **GAME** state");
		}
	},

	preload: function () {
		// We create a procedural starfield background
		for (var i = 0; i < this.numStars; i++) {
			let sprite = game.add.sprite(game.world.randomX,
				game.world.randomY, 'spacewar', 'staralpha.png');
			let random = game.rnd.realInRange(0, 0.6);
			sprite.scale.setTo(random, random)
		}

		// We preload the bullets pool
		game.global.proyectiles = new Array(this.maxProjectiles)
		for (var i = 0; i < this.maxProjectiles; i++) {
			game.global.projectiles[i] = {
				image: game.add.sprite(0, 0, 'spacewar', 'projectile.png')
			}
			game.global.projectiles[i].image.anchor.setTo(0.5, 0.5)
			game.global.projectiles[i].image.visible = false
		}

		// we load a random ship
		let random = ['blue', 'darkgrey', 'green', 'metalic', 'orange',
			'purple', 'red']
		let randomImage = random[Math.floor(Math.random() * random.length)]
			+ '_0' + (Math.floor(Math.random() * 6) + 1) + '.png'
		game.global.myPlayer.image = game.add.sprite(0, 0, 'spacewar',
			game.global.myPlayer.shipType)

		// Cargamos los power ups (PRUEBA)
		let lifePowerUp = game.add.sprite(game.world.randomX,
			game.world.randomY, 'LifePowerUp', 'LifePowerUp.png');
		lifePowerUp.scale.setTo(2,2);
		let ammoPowerUp = game.add.sprite(game.world.randomX,
				game.world.randomY, 'AmmoPowerUp', 'AmmoPowerUp.png');
		ammoPowerUp.scale.setTo(2,2);
		let propellerPowerUp = game.add.sprite(game.world.randomX,
			game.world.randomY, 'PropellerPowerUp', 'PropellerPowerUp.png');
		propellerPowerUp.scale.setTo(2,2);	

		game.global.myPlayer.image.anchor.setTo(0.5, 0.5)
		game.global.myPlayer.propellerUses = 3;
		game.global.myPlayer.ammo = 20;
	},

	create: function () {

		game.input.mouse.capture = true;
		game.global.UIText[game.global.myPlayer.id] = game.add.text(10, 10 + game.global.myPlayer.id * 20 , game.global.myPlayer.playerName + " / "+ game.global.myPlayer.life + " / "+ game.global.myPlayer.ammo + " / "+ game.global.myPlayer.propellerUses + " / "+ game.global.myPlayer.score, { font: "16px Orbitron", fill: "#40ffe6" });
		game.global.UIText[game.global.myPlayer.id].fontWeight = 'bold';
		game.global.UIText[game.global.myPlayer.id].stroke = '#000000';
    	game.global.UIText[game.global.myPlayer.id].strokeThickness = 3;
		
		game.global.UIPlayerName[game.global.myPlayer.id] = game.add.text(game.global.myPlayer.image.x, game.global.myPlayer.image.y+20, game.global.myPlayer.playerName, { font: "12px Orbitron", fill: "#40ffe6" });
		game.global.UIPlayerName[game.global.myPlayer.id].anchor.set(0.5, 0.5);
		game.global.UIPlayerName[game.global.myPlayer.id].stroke = '#000000';
    	game.global.UIPlayerName[game.global.myPlayer.id].strokeThickness = 3;
		////// INTERFAZ QUE MUESTRA DATOS DE LOS JUGADORES //////
		/*function generateHexColor() { 
			return '#' + ((0.5 + 0.5 * Math.random()) * 0xFFFFFF << 0).toString(16);
		}*/
		/////////////////////////////////////////////////////////

		this.bulletTime = 0
		this.fireBullet = function () {
			if (game.time.now > this.bulletTime) {
				this.bulletTime = game.time.now + 250;
				// this.weapon.fire()
				return true
			} else {
				return false
			}
		}

		/////////////////////////////////////////////////////////

		this.propellerTime = 0;
		this.usePropeller = function () {
			if (game.global.myPlayer.propellerUses <= 0 && game.time.now > this.propellerTime){
				this.propellerTime = game.time.now + 250;
				console.log("No tienes carga de propulsión.");
				return false
			}
			else if (game.time.now > this.propellerTime) {
				this.propellerTime = game.time.now + 250;
				console.log("PROPULSOR USADO!");
				return true
			} else {
				return false
			}
		}

		// Esto es para que las teclas se puedan usar fuera del canvas (en el chat, por ej)
		/*game.onBlur.add(function () {
			game.input.keyboard.enabled = false;
		});

		game.onFocus.add(function () {
			game.input.keyboard.enabled = true;
		});*/
		$("#gameDiv").mouseenter(function () {
			game.input.enabled = true;
		});

		$("#gameDiv").mouseleave(function () {
			game.input.enabled = false;

		});

		this.wKey = game.input.keyboard.addKey(Phaser.Keyboard.W);
		this.sKey = game.input.keyboard.addKey(Phaser.Keyboard.S);
		this.aKey = game.input.keyboard.addKey(Phaser.Keyboard.A);
		this.dKey = game.input.keyboard.addKey(Phaser.Keyboard.D);
		this.pKey = game.input.keyboard.addKey(Phaser.Keyboard.P);
		this.spaceKey = game.input.keyboard.addKey(Phaser.Keyboard.SPACEBAR);

		// Stop the following keys from propagating up to the browser
		/*game.input.keyboard.addKeyCapture([Phaser.Keyboard.W,
		Phaser.Keyboard.S, Phaser.Keyboard.A, Phaser.Keyboard.D,
		Phaser.Keyboard.SPACEBAR]);*/

		game.camera.follow(game.global.myPlayer.image);
	},

	update: function () {
		
		let msg = new Object()
		msg.event = 'UPDATE MOVEMENT'

		msg.movement = {
			thrust: false,
			brake: false,
			rotLeft: false,
			rotRight: false
		}

		msg.bullet = false
		msg.propeller = false

		if (this.wKey.isDown)
			msg.movement.thrust = true;
		if (this.sKey.isDown)
			msg.movement.brake = true;
		if (this.aKey.isDown)
			msg.movement.rotLeft = true;
		if (this.dKey.isDown)
			msg.movement.rotRight = true;
		if (this.spaceKey.isDown) {
			msg.bullet = this.fireBullet()
			console.log("Shoot!")
		}
		if (this.pKey.isDown) { // Servirá para los propulsores
			msg.propeller = this.usePropeller()
		}

		if (game.global.DEBUG_MODE) {
			console.log("[DEBUG] Sending UPDATE MOVEMENT message to server")
		}
		game.global.socket.send(JSON.stringify(msg))
	},
}

function generateColor(n) {
	if (n%2!=0) {
		return '#40ffe6'
	} else {
		return '#ffffff'
	}
}
function clearGame() {
	game.state.clearCurrentState(); 
	game.global.currentSala.players = [];
	game.global.myPlayer = new Object();
	game.global.otherPlayers = [];
}