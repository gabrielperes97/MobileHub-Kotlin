package gabrielleopoldino.contextnet.mobilehubsimulator.tecnologies.simulator

import gabrielleopoldino.contextnet.mobilehubsimulator.MessageManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.io.StringReader
import java.lang.Exception
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import javax.json.Json
import javax.json.JsonObject
import javax.json.JsonString
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import kotlin.collections.HashMap

object SimulatorConnection : Tecnology{
    override val mapObjs = HashMap<UUID, MOBJ>()

    val scanInterval = 1000 //ms
    lateinit var socketServer : ServerSocket
    internal val objs = ArrayList<MOBJ>()//TODO Jogar isso em um escopo geral se for aumentar o numero de tecnologias
    var port: Int = 0

    val LOGGER = Logger.getLogger("SimulatorConnection")

    var initialized = false
    var reserved = false


    fun init(port: Int) {
        if (!initialized) {
            reserved = false
            this.port = port
            socketServer = ServerSocket(port)
            println("Starting simulation server at " + Inet4Address.getLocalHost() + ":" + socketServer.localPort)
            Accepter.start()
        }
        else
        {
            error("Simulator Connection already initialized")
        }
    }

    fun init(port: Int, sslContext: SSLContext) {
        if (!initialized) {
            reserved = true
            this.port = port
            val sslServer = sslContext.serverSocketFactory.createServerSocket() as SSLServerSocket
            sslServer.needClientAuth = true
            socketServer = sslServer
            socketServer.bind(InetSocketAddress(port))
            println("Starting simulation server in reserver mode at " + Inet4Address.getLocalHost() + ":" + socketServer.localPort)
            Accepter.start()
        }
        else
        {
            error("Simulator Connection already initialized")
        }
    }

    private fun sendImpl(mobj: MOBJ, message: JsonObject){
        synchronized(mobj) {
            mobj.out.println(message.toString())
            mobj.out.flush()
        }
    }

    override fun sendToMobj(uuid: UUID, message: JsonObject) {
        val mobj = mapObjs[uuid]
        if(mobj != null)
            sendImpl(mobj, message)
    }

    object Accepter: Thread("Simulator socket listener") {

        init {
            //super.setDaemon(true)
        }

        override fun run() {
            while (true)
            {
                if (!socketServer.isClosed) {
                    val obj = socketServer.accept()
                    println("Client connected: ${obj.inetAddress.hostAddress}")
                    val bundler = MOBJ(obj)
                    objs.add(bundler)
                }
            }
        }
    }

    class MOBJ constructor(private val socket: Socket): Thread(socket.inetAddress.hostAddress + " Receiver") {

        lateinit var reader:BufferedReader
        lateinit var out: PrintStream

        private val scanTask : ScheduledExecutorService = Executors.newScheduledThreadPool(1)


        init {
            start()
        }

        override fun start() {
            reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            out = PrintStream(socket.getOutputStream())
            scanTask.scheduleAtFixedRate({
                val json = Json.createObjectBuilder()
                        .add("waitResponse", true)
                        .build()
                out.println(json.toString())
                out.flush()
            }, 1, scanInterval.toLong(), TimeUnit.MILLISECONDS)
            super.start()
        }

        override fun run() {
            while (socket.isConnected){
                try {
                    val str = reader.readLine()
                    if (str != null) {
                        val json = Json.createReader(StringReader(str)).readObject()

                        val uuidStr = json["uuid"]
                        if (uuidStr != null) {
                            try {
                                val uuid = UUID.fromString((uuidStr as JsonString).string)
                                if (!mapObjs.containsKey(uuid)) {
                                    mapObjs.put(uuid, this)
                                }
                            } catch (e: IllegalArgumentException) {
                                LOGGER.severe("UUID string too large: $uuidStr")
                            }
                        }
                        MessageManager.processDataFromMobj(json)
                    }
                }catch (e : Exception)
                {}
            }
        }
    }
}