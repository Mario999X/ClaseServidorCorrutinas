package monitor

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import models.Alumno
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger

private val log = KotlinLogging.logger {}

class DB {

    private val listaAlumnos = mutableMapOf<Int, Alumno>()

    private var numAlumno = AtomicInteger(1)

    // Lock
    private val lock = Mutex()

    // Obtenemos el listado de alumnos
    suspend fun getAll(): Map<Int, Alumno> {
        lock.withLock {
            val mapa = listaAlumnos

            log.debug { "\tSe envia el listado de alumnos..." }

            return mapa
        }
    }

    // Introducimos un alumno con ID fijo y en aumento
    suspend fun put(item: Alumno) {
        lock.withLock {
            log.debug { "\tAlumno -> $numAlumno / $item agregado" }
            item.id = numAlumno.toInt()
            listaAlumnos[numAlumno.toInt()] = item
            numAlumno.incrementAndGet()
        }
    }

    // Actualizamos a un alumno (Nombre y Nota) segun su ID
    suspend fun update(item: Int, alumno: Alumno): Boolean {
        lock.withLock {
            var existe = false
            if (listaAlumnos.containsKey(item)) {
                log.debug { "\tAlumno -> ${listaAlumnos[item]} antiguo" }
                listaAlumnos[item] = alumno
                log.debug { "\tAlumno -> ${listaAlumnos[item]} actualizado" }
                existe = true
            } else {
                log.debug { "\tID NO EXISTE -> $item" }
            }
            return existe
        }
    }

    // Borramos a un alumno segun su ID
    suspend fun delete(item: Int): Boolean {
        lock.withLock {
            var existe = false
            if (listaAlumnos.containsKey(item)) {
                log.debug { "\tAlumno -> ${listaAlumnos[item]} eliminado" }
                listaAlumnos.remove(item)
                existe = true
            } else {
                log.debug { "\tID NO EXISTE -> $item" }
            }
            return existe
        }
    }

}