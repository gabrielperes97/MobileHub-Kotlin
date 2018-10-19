package gabrielleopoldino.contextnet.mobilehubsimulator

import lac.cnclib.net.NodeConnection
import lac.cnclib.net.NodeConnectionListener
import lac.cnclib.net.mrudp.MrUdpNodeConnection
import lac.cnclib.sddl.message.ApplicationMessage
import lac.cnclib.sddl.message.ClientLibProtocol
import lac.cnclib.sddl.message.Message
import java.io.StringReader
import java.lang.Exception
import java.net.SocketAddress
import java.util.logging.Logger
import javax.json.Json
import javax.json.JsonObject

class GatewayCommunication constructor(val gatewayAddress: SocketAddress) : Communication{

    val connection = MrUdpNodeConnection()
    val LOGGER = Logger.getLogger("GatewayCommunication")

    init{
        connection.addNodeConnectionListener(Listener())
        connection.connect(gatewayAddress)
    }

    override fun sendDataToGateway(message: JsonObject) {
        val sddlMessage = ApplicationMessage()
        sddlMessage.setPayloadType(ClientLibProtocol.PayloadSerialization.JSON)
        sddlMessage.contentObject = message.toString()
        connection.sendMessage(sddlMessage)

        LOGGER.info("Send: $message")
    }


    private inner class Listener: NodeConnectionListener{
        override fun connected(remoteCon: NodeConnection?) {
            LOGGER.info("Connected to gateway")
        }

        override fun reconnected(remoteCon: NodeConnection?, endPoint: SocketAddress?, wasHandover: Boolean, wasMandatory: Boolean) {
            LOGGER.info("Reconnecting to gateway")
        }

        override fun disconnected(remoteCon: NodeConnection?) {
            LOGGER.info("Disconnected to gateway")
        }

        override fun newMessageReceived(remoteCon: NodeConnection?, message: Message?) {
            val content = message?.contentObject
            if (content != null) {
                if (content is String) {
                    val str = message.contentObject as String
                    val json = Json.createReader(StringReader(str)).readObject()
                    MessageManager.processDataToMobj(json)
                }
                else
                    LOGGER.severe("Content is not a String")
            }
            else
                LOGGER.severe("Content is null")
        }

        override fun unsentMessages(remoteCon: NodeConnection?, unsentMessages: MutableList<Message>?) {
            LOGGER.info("Have unsent messages")
        }

        override fun internalException(remoteCon: NodeConnection?, e: Exception?) {
            LOGGER.severe("Error: ${e.toString()}")
        }

    }
}