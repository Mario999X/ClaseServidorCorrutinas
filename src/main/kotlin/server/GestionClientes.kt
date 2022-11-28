package server

import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.Alumno
import models.mensajes.Request
import models.mensajes.Request.Type.*
import models.mensajes.Response
import monitor.DB
import mu.KotlinLogging

private val log = KotlinLogging.logger {}
private val json = Json

private lateinit var response: Response<String>
private lateinit var request: Request<Alumno>
private lateinit var requestGenerica: Request<Int>

class GestionClientes(private val s: Socket, private val db: DB) {

    // Canal de entrada y de salida
    private val entrada = s.openReadChannel()
    private val salida = s.openWriteChannel(true) // true, para que se envíe el dato al instante

    suspend fun run() = withContext(Dispatchers.IO) {

        // Recibimos el aviso del cliente
        log.debug { "Obteniendo el aviso del cliente" }
        val aviso = entrada.readInt()

        // Enviamos la respuesta al cliente
        salida.writeInt(aviso)
        // Analizamos el request del cliente (teniendo en cuenta la señal recibida)
        val sendResponse = launch {
            if (aviso == 1) {

                val receiveRequest = entrada.readUTF8Line()

                receiveRequest?.let {
                    request = json.decodeFromString<Request<Alumno>>(receiveRequest)
                    log.debug { "Recibido: $request" }

                    // Recogemos el contenido y lo casteamos a Alumno
                    val alumno = request.content as Alumno

                    when (request.type) {
                        ADD -> {
                            log.debug { "Alumno: $alumno" }
                            db.put(alumno)
                            log.debug { "Alumno agregado" }
                            response = Response("Operacion realizada", Response.Type.OK)
                        }

                        UPDATE -> {
                            log.debug { "Alumno: $alumno" }
                            val existe = alumno.id?.let { db.update(alumno.id!!, alumno) }
                            response = if (!existe!!) {
                                Response("Alumno no existe", Response.Type.OK)
                            } else Response("Alumno actualizado", Response.Type.OK)
                        }

                        else -> {
                            response = Response("Error | signal recibido: $aviso ", Response.Type.ERROR)
                        }
                    }
                }

                // Request Generica
            } else {
                val receiveRequest = entrada.readUTF8Line()

                receiveRequest?.let {
                    requestGenerica = json.decodeFromString<Request<Int>>(receiveRequest)
                    log.debug { "Recibido: $requestGenerica" }

                    // Recogemos el contenido y lo casteamos a Alumno
                    val numOpcion = requestGenerica.content as Int

                    when (requestGenerica.type) {
                        DELETE -> {

                            log.debug { "ID: $numOpcion" }
                            val existe = numOpcion.let { db.delete(it) }
                            response = if (!existe) {
                                Response("Alumno no existe", Response.Type.OK)
                            } else Response("Alumno eliminado", Response.Type.OK)

                        }

                        CONSULT -> {

                            val listaAlumnos = db.getAll().toSortedMap()
                            log.debug { "Obteniendo lista en orden pedido" }
                            var orden = listOf<Alumno>()

                            if (numOpcion == 1) {
                                orden = listaAlumnos.values.sortedBy { it.nombre }
                            }
                            if (numOpcion == 2) {
                                orden = listaAlumnos.values.sortedByDescending { it.nota }
                            }
                            if (numOpcion == 3) {
                                orden = listaAlumnos.values.filter { it.nota >= 5 }
                            }
                            if (numOpcion == 4) {
                                orden = listaAlumnos.values.filter { it.nota < 5 }
                            }
                            response = Response(orden.toString(), Response.Type.OK)

                            if (numOpcion == 5) {
                                val alumnos = db.getAll().values.toList()
                                val media = alumnos.stream().mapToInt { it.nota }.average()

                                response = Response(media.toString(), Response.Type.OK)
                            }
                        }

                        else -> {
                            response = Response("Error | signal recibido: $aviso ", Response.Type.ERROR)
                        }
                    }
                }

            }
            // Como los Response son mandados como un String, se puede reutilizar asi
            // Lo enviamos
            salida.writeStringUtf8(json.encodeToString(response) + "\n")
        }
        sendResponse.join()

        log.debug { "Cerrando conexion" }
        withContext(Dispatchers.IO) {
            salida.close()
            s.close()
        }
    }
}