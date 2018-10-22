package gabrielleopoldino.contextnet.mobilehubsimulator

import lac.cnclib.net.NodeConnection
import lac.cnclib.net.NodeConnectionListener
import lac.cnclib.sddl.message.ApplicationMessage
import lac.cnclib.sddl.message.ClientLibProtocol
import lac.cnclib.sddl.message.Message
import leopoldino.secureclientlib.net.mrudp.SmrUdpNodeConnection
import leopoldino.smrudp.SecurityProfile
import java.io.StringReader
import java.lang.Exception
import java.net.SocketAddress
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.*
import java.util.logging.Logger
import javax.json.Json
import javax.json.JsonObject
import kotlin.system.exitProcess

class SecureGatewayCommunication constructor(val gatewayAddress: SocketAddress, val trustKeyStore: KeyStore, trustPassword: String, val identity: KeyStore, identityPassword: String) : Communication {
    val connection : SmrUdpNodeConnection
    val LOGGER = Logger.getLogger("SecureGatewayCommunication")
    val signer = Signature.getInstance("SHA1withRSA")

    init{
        val enumeration = identity.aliases()
        var uuid : UUID? = null
        var privateKey: PrivateKey? = null
        while (enumeration.hasMoreElements()) {
            val alias = enumeration.nextElement() as String
            val certificate = identity.getCertificate(alias)
            val subj = (certificate as X509Certificate).subjectDN.name
            val result = "CN=(([a-z]|\\d|-)*),?".toRegex().find(subj)
            if (result != null)
            {
                uuid = UUID.fromString(result.groupValues[1])
                privateKey = identity.getKey(identity.aliases().nextElement(), identityPassword.toCharArray()) as PrivateKey
                break
            }
        }

        if (uuid == null)
        {
            error("UUID not found at identity certificate")
            exitProcess(1)
        }

        println("Using predefined uuid $uuid")

        signer.initSign(privateKey)

        connection = SmrUdpNodeConnection(uuid, SecurityProfile.getInstance(trustKeyStore, identity, identityPassword))
        connection.addNodeConnectionListener(Listener())
        connection.connect(gatewayAddress)
    }

    override fun sendDataToGateway(message: JsonObject) {

        val signedMessage = signJson(message)

        val sddlMessage = ApplicationMessage()
        sddlMessage.setPayloadType(ClientLibProtocol.PayloadSerialization.JSON)
        sddlMessage.contentObject = signedMessage.toString()
        connection.sendMessage(sddlMessage)

        LOGGER.finer("Send: $signedMessage")
    }


    private inner class Listener: NodeConnectionListener {
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
                    val signedJson = signJson(json)
                    MessageManager.processDataToMobj(signedJson)
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

    fun signJson(message: JsonObject): JsonObject {

        signer.update(message.toString().toByteArray())
        val signBytes = signer.sign()
        return Json.createObjectBuilder(message)
                .add("MobileHub Signature", Arrays.toString(signBytes))
                .build()

    }
}