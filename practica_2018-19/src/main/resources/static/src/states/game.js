Spacewar.gameState = function (game) {
	this.bulletTime
	this.fireBullet
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
		game.global.myPlayer.image.anchor.setTo(0.5, 0.5)
	},

	create: function () {

		////// INTERFAZ QUE MUESTRA DATOS DE LOS JUGADORES //////
		var textGroup = game.add.group();
		for (var i = 0; i < game.global.currentSala.players.length; i++) {
			//textGroup.add(game.make.text(10, 10 + i * 5 , game.global.currentSala.players[i].playerName + " / "+ game.global.currentSala.players[i].life + " / "+ game.global.currentSala.players[i].ammo + " / "+ game.global.currentSala.players[i].propeller + " / "+ game.global.currentSala.players[i].score, { font: "12px Arial", fill: generateHexColor() }));
			textGroup.add(game.make.text(10, 10 + i * 20 , "nombre / 3 / 6 / 2 / 17", { font: "15px Orbitron", fill: generateColor(i) }));
		}
		/*function generateHexColor() { 
			return '#' + ((0.5 + 0.5 * Math.random()) * 0xFFFFFF << 0).toString(16);
		}*/
		function generateColor(n) {
			if (n>=1) {
				n = n*0.1
			}
			return '#' + ((0.5 + 0.5 * n) * 0xFFFFFF << 0).toString(16);
		}
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
		}

		if (game.global.DEBUG_MODE) {
			console.log("[DEBUG] Sending UPDATE MOVEMENT message to server")
		}
		game.global.socket.send(JSON.stringify(msg))
	}
}