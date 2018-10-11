package gabrielleopoldino.contextnet.mobilehubsimulator

import javax.json.JsonObject

interface Communication{

    fun sendDataToGateway(message: JsonObject)

}