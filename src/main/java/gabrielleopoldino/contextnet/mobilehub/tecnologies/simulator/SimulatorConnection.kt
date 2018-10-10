package gabrielleopoldino.contextnet.mobilehub.tecnologies.simulator

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.io.StringReader
import java.net.Inet4Address
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.json.Json

object SimulatorConnection {
    val scanInterval = 1000 //ms
    val socketServer = ServerSocket(12346)
    internal val objs = ArrayList<Bundler>()

    init {
        println("Starting server at "+Inet4Address.getLocalHost()+":"+ socketServer.localPort)
        Accepter.start()
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
                    val bundler = Bundler(obj, ClientListener(obj))
                    objs.add(bundler)
                }
            }
        }
    }

    internal class ClientListener constructor(private val socket: Socket): Thread(socket.inetAddress.hostAddress + " Receiver") {

        private lateinit var reader:BufferedReader
        private lateinit var out: PrintStream

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
            }, 1, 1, TimeUnit.SECONDS)
            super.start()
        }

        override fun run() {
            while (socket.isConnected){
                val str = reader.readLine()
                if (str != null) {
                    val json = Json.createReader(StringReader(str)).readObject()
                    println(json.toString())
                }
            }
        }
    }

    internal class Bundler constructor(val socket: Socket, val receiverListener: ClientListener)
}