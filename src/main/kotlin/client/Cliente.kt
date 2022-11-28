package client

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.Alumno
import models.mensajes.Request
import models.mensajes.Response
import mu.KotlinLogging

private val log = KotlinLogging.logger { }
private val json = Json

// Varios tipos de request segun la operacion a realizar.
// Una vez se mande, y segun el tipo de Request, el gestor hara lo que deba con el contenido. (menos la opcion 5)
private lateinit var request: Request<Alumno>
private lateinit var requestGenerica: Request<Int>

fun main() = runBlocking {

    // Indicamos el Dispatcher para el cliente
    val selectorManager = SelectorManager(Dispatchers.IO)

    // Datos a usar para los Request que se envien
    var alumno: Alumno
    var nombre: String
    var nota: Int
    var id: Int

    var aviso = 0 // 0 -> Request Generica [Int], 1 -> Request de Alumno

    // Preparamos el bucle para el cliente | La conexion sera tipo HTTP
    var salidaApp = false

    while (!salidaApp) {
        // Preparamos el menu para el cliente
        log.debug {
            """Por favor, seleccione una de las siguientes opciones:
            |1. AGREGAR ALUMNO
            |2. BORRAR ALUMNO
            |3. ACTUALIZAR ALUMNO
            |4. CONSULTAR ALUMNOS
            |5. SALIR
        """.trimMargin()
        }
        val opcion = readln().toIntOrNull()

        val sendRequest = launch {
            when (opcion) {
                1 -> {
                    log.debug { "\tIntroduzca el NOMBRE del alumno: " }
                    nombre = readln()

                    log.debug { "\tIntroduzca la NOTA SIN DECIMALES del alumno: " }
                    nota = readln().toInt()
                    // Las notas van de 0 a 10, punto.
                    if (nota < 0) nota = 0
                    if (nota > 10) nota = 10

                    alumno = Alumno(nombre, nota)
                    request = Request(alumno, Request.Type.ADD)

                    log.debug { "Alumno enviado, esperando respuesta..." }
                    aviso = 1
                }

                2 -> {
                    log.debug { "\tIntroduzca el ID del alumno: " }
                    id = readln().toInt()
                    requestGenerica = Request(id, Request.Type.DELETE)

                    log.debug { "ID enviado, esperando respuesta..." }
                    aviso = 0
                }

                3 -> {
                    log.debug { "\tIntroduzca el nuevo NOMBRE del alumno: " }
                    nombre = readln()

                    log.debug { "\tIntroduzca la nueva NOTA SIN DECIMALES del alumno: " }
                    nota = readln().toInt()
                    // Las notas van de 0 a 10, punto.
                    if (nota < 0) nota = 0
                    if (nota > 10) nota = 10

                    log.debug { "\tIntroduzca el ID del alumno existente: " }
                    id = readln().toInt()

                    request = Request(Alumno(nombre, nota, id), Request.Type.UPDATE)
                    log.debug { "Alumno enviado para actualizar, esperando respuesta..." }
                    aviso = 1
                }

                4 -> {
                    log.debug {
                        """Elija el orden de los alumnos a mostrar:
                    |1. Alfabetico
                    |2. Nota
                    |3. Solo APROBADOS
                    |4. Solo SUSPENSOS
                    |5. Media de NOTAS
                """.trimMargin()
                    }
                    id = readln().toInt() // Reutilizando el codigo
                    if (id == 1) {
                        requestGenerica = Request(id, Request.Type.CONSULT)
                    }
                    if (id == 2) {
                        requestGenerica = Request(id, Request.Type.CONSULT)
                    }
                    if (id == 3) {
                        requestGenerica = Request(id, Request.Type.CONSULT)
                    }
                    if (id == 4) {
                        requestGenerica = Request(id, Request.Type.CONSULT)
                    }
                    if (id == 5) {
                        requestGenerica = Request(id, Request.Type.CONSULT)
                    }
                    if (id < 1 || id > 5) {
                        requestGenerica = Request(id, Request.Type.ERROR)
                    }
                    log.debug { "Esperando listado..." }
                    aviso = 0
                }

                5 -> {
                    log.debug { " Saliendo del programa..." }
                    salidaApp = true
                }

                null -> {
                    log.debug { "OPCION DESCONOCIDA..." }
                }
            }

            // CONEXION CON EL SERVIDOR / VUELTA AL WHEN SI FUE NULL | SALIDA DEL PROGRAMA SIN ENTRAR AL SERVER
            try {
                if (opcion == null || opcion <= 0 || opcion >= 5) {
                    println("-------")
                } else {

                    // Conectamos con el servidor
                    val socket = aSocket(selectorManager).tcp().connect("localhost", 6969)
                    log.debug { "Conectado a ${socket.remoteAddress}" }

                    // Preparamos los canales de lectura-escritura
                    val entrada = socket.openReadChannel()
                    val salida = socket.openWriteChannel(true)

                    // Enviamos el aviso al servidor
                    salida.writeInt(aviso)

                    // Recibimos la respuesta del servidor al aviso -- El mismo numero enviado
                    entrada.readInt()

                    // Ahora, segun la opcion escogida, enviamos un tipo de request
                    if (opcion == 1 || opcion == 3) {
                        salida.writeStringUtf8(json.encodeToString(request) + "\n")
                        log.debug { "$request enviada con exito, esperando respuesta..." }
                    }

                    if (opcion == 2 || opcion == 4) {
                        salida.writeStringUtf8(json.encodeToString(requestGenerica) + "\n")
                        log.debug { "$requestGenerica enviada con exito, esperando respuesta..." }
                    }

                    // Respuesta del servidor
                    val receiveResponse = entrada.readUTF8Line()
                    val response = json.decodeFromString<Response<String>>(receiveResponse!!)
                    log.debug { "Respuesta del servidor: ${response.content}" }

                    // Cerramos la salida y el socket
                    log.debug { "Desconectando del servidor..." }
                    withContext(Dispatchers.IO) {
                        salida.close()
                        socket.close()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        // Recogemos la corrutina
        sendRequest.join()
    }
}