package gabrielleopoldino.contextnet.mobilehubsimulator.tecnologies.simulator

import java.util.*
import javax.json.JsonObject

interface Tecnology {
    val mapObjs: Map<UUID, Any>

    fun sendToMobj(uuid: UUID, message: JsonObject)
}