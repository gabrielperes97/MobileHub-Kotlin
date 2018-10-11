package gabrielleopoldino.contextnet.mobilehubsimulator.attachdata.simulator

import java.util.*
import javax.json.Json
import javax.json.JsonObjectBuilder

class GPS {
    companion object {
        fun randomGpsData(): JsonObjectBuilder {
            val json = Json.createObjectBuilder()
                    .add("latitude", Random().nextInt(180))
                    .add("longitude", Random().nextInt(180))
            return json
        }
    }
}