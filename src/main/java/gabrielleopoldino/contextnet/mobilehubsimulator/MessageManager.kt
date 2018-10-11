package gabrielleopoldino.contextnet.mobilehubsimulator

import gabrielleopoldino.contextnet.mobilehubsimulator.attachdata.AttachDataGenerator
import gabrielleopoldino.contextnet.mobilehubsimulator.tecnologies.simulator.Tecnology
import java.util.*
import javax.json.Json
import javax.json.JsonObject

object MessageManager {

    val attachDataGenerators = arrayListOf<AttachDataGenerator>()
    val tecnologies = LinkedList<Tecnology>()
    lateinit var communicationManager : Communication

    fun start(communication: Communication)
    {
        this.communicationManager = communication
    }
    
    fun processDataFromMobj(message: JsonObject) {
        communicationManager.sendDataToGateway(message)
    }

    fun attachData(message: JsonObject): JsonObject {
        val newJson = Json.createObjectBuilder(message)
        attachDataGenerators.forEach {
            newJson.addAll(it.generateData())
        }
        return newJson.build()
    }

    fun processDataToMobj(message: JsonObject) {
        val uuid_str = message["uuid"]
        if (uuid_str != null)
        {
            val uuid = UUID.fromString(uuid_str.toString())
            tecnologies.forEach{
                if(it.mapObjs.containsKey(uuid))
                {
                    it.sendToMobj(uuid, message)
                }
            }
        }
    }



}