package models

import kotlinx.serialization.Serializable

@Serializable
data class Alumno(
    val nombre: String,
    val nota: Int,
    var id: Int?
) {
    constructor(
        nombre: String,
        nota: Int
    ) : this(nombre, nota, id = null)

    override fun toString(): String {
        return "Alumno('$nombre', nota=$nota, id=$id)"
    }
}
