# Entornos Multijugador
Práctica final de entornos multijugador.

# Guía de protocolos de WS implementados

## Extra

* "ERROR"  
**Servidor -> Cliente:_** Ha habido algún error con las acciones realizadas por el cliente. Tiene un atributo "type" que permite gestionar los diferentes errores en su propia función.

* "CONFIRMATION"  
**Servidor -> Cliente:** El servidor ha confirmado alguna acción realizada por el cliente. Igual que el mensaje de error, tiene un atributo "type" que permite gestionar las diferentes confirmaciones.

## Salas

* "JOIN LOBBY"  
**Cliente -> Servidor:** El cliente quiere entrar al lobby (selección de sala)

* "LEAVE LOBBY"  
**Cliente -> Servidor:** El cliente quiere salir del lobby (al menú principal)

* "NEW ROOM"  
**Cliente -> Servidor:** El cliente quiere crear una sala

* "JOIN ROOM"  
**Cliente -> Servidor:** El cliente quiere unirse a una sala existente

* "GET ROOMS"  
**Servidor -> Cliente:** Se ha actualizado la lista de salas disponibles

* "ROOM INFO"  
**Servidor -> Cliente:** Se le envía la información de la sala donde está el cliente (cuando se une alguien, etc)

* "LEAVE ROOM"  
**Cliente -> Servidor:** El cliente quiere salir de la sala

## Juego

* "START GAME"  
**Servidor -> Cliente:** El juego ha comenzado

* "UPDATE MOVEMENT"  
**Cliente -> Servidor:** El cliente ha realizado algún input

* "GAME STATE UPDATE"  
**Servidor -> Cliente:** Actualización del estado del juego

* "END GAME"  
**Servidor -> Cliente:** El juego ha acabado y hay un ganador

## Chat

* "CHAT MSG"  
**Cliente -> Servidor:** El cliente quiere enviar un mensaje al chat
