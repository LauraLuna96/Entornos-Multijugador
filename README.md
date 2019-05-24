# Entornos Multijugador
Práctica final de entornos multijugador.

# Guía de protocolos de WS implementados
"NEW ROOM"
cliente->servidor: Cuando el cliente crea una sala *antes se llamaba "create room" pero creo que es mejor cambiarle el nombre para que sea consistente con lo demás
servidor->cliente: Cuando un cliente ha creado una sala, se la pasa a los demás

"ROOM INFO"
servidor->cliente: La info de la sala ha cambiado, y el servidor avisa a los jugadores en ella

"JOIN ROOM"
cliente->servidor: Cuando un cliente se quiere unir a una sala específica
servidor->cliente: Cuando un cliente se ha unido a una sala, avisa a los demás (de la sala)

"UPDATE ROOM"
servidor->cliente: Cuando una sala ha tenido algún cambio (ej. se ha unido un jugador) se avisa a los demás clientes del lobby para que actualicen su información visible --> Se usará en el caso de implementar un 2do modo de juego.

"LEAVE ROOM"
cliente->servidor: Cuando quiere salir de la sala donde está
servidor->cliente: Cuando un cliente ha salido de la sala, avisa a los demás (de la sala)

* Si al salir el cliente la sala queda vacía, se borra la sala

"DELETE ROOM"
servidor->cliente: Cuando una sala se ha borrado, avisa a los clientes

"START GAME"
cliente->servidor: Cuando una sala quiere empezar a jugar (si no se cumple el min de jugadores, no se empieza)
servidor->cliente: Cuando comienza el juego (pasar al estado game)
