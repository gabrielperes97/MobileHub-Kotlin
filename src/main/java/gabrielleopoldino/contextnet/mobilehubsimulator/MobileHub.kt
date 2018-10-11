package gabrielleopoldino.contextnet.mobilehubsimulator

import gabrielleopoldino.contextnet.mobilehubsimulator.tecnologies.simulator.SimulatorConnection
import java.net.InetSocketAddress

object MobileHub {

    @JvmStatic
    fun main(args: Array<String>) {
        MessageManager.start(GatewayCommunication(InetSocketAddress("127.0.0.1", 5500)))
        SimulatorConnection.start(12346)
    }
}
