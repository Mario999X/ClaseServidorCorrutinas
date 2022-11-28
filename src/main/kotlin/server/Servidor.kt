package server

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import models.Alumno
import monitor.DB
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

private const val PUERTO = 6969

fun main() = runBlocking {

    // Preparamos la SC con monitor
    val db = DB()

    // Alumnos base para la lista
    val alumnosBase = listOf(
        Alumno("Mario", 10),
        Alumno("Kratos", 5),
        Alumno("Atreus", 4)
    )
    // Introducimos los alumnos
    repeat(alumnosBase.size) {
        db.put(alumnosBase[it])
    }

    // Dispacher para el servidor IO = Manager
    val selectorManager = SelectorManager(Dispatchers.IO)

    log.debug { "Arrancando servidor..." }
    // Socket TCP
    val serverSocket = aSocket(selectorManager).tcp().bind("localhost", PUERTO)

    while (true) {
        log.debug { "\t--Servidor esperando..." }
        // Espera la llegada de una conexion

        val socket = serverSocket.accept()
        log.debug { "Peticion de cliente -> " + socket.localAddress + " --- " + socket.remoteAddress }

        // Lo desviamos al gestor de clientes, usando una corrutina
        launch {
            GestionClientes(socket, db).run()
            log.debug { "Cliente desconectado" }
        }
    }
}