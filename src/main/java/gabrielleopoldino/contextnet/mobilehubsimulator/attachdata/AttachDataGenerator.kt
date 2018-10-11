package gabrielleopoldino.contextnet.mobilehubsimulator.attachdata

import javax.json.JsonObjectBuilder

interface AttachDataGenerator {
    fun generateData(): JsonObjectBuilder
}