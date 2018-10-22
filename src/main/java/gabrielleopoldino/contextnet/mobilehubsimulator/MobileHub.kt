package gabrielleopoldino.contextnet.mobilehubsimulator

import com.beust.jcommander.IStringConverter
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import gabrielleopoldino.contextnet.mobilehubsimulator.tecnologies.simulator.SimulatorConnection
import leopoldino.smrudp.SecurityProfile
import java.io.File
import java.net.InetSocketAddress
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory


object MobileHub {

    @Parameters(commandNames = ["secure"] , commandDescription = "Enable secure mobile Hub")
    private class CommandSecure {

        @Parameter(names = ["-i", "--identity"], description = "the identity certificate", converter = FileConverter::class)
        var identityCert : File? = null

        @Parameter(names = ["-ip", "--identity-password"], description = "the password for the identity certificate")
        var identityPassword : String = ""

        @Parameter(names = ["-t", "--trusted"], description = "the trusted ca certificate", converter = FileConverter::class)
        var trustedCert : File? = null

        @Parameter(names = ["-tp", "--trust-password"], description = "the password for the trust keystore")
        var  trustPassword : String = ""

        @Parameter(names = ["-r", "--reserved"], description = "Use reserved mode (Scenario 1)")
        var reservedMode : Boolean = false

    }

    private class FileConverter : IStringConverter<File> {
        override fun convert(path: String?): File {
            return File(path)
        }
    }


    @JvmStatic
    fun main(args: Array<String>) {
        val mh = MobileHub
        val commandSecure = CommandSecure()
        val jc = JCommander.newBuilder()
                .addObject(mh)
                .addCommand(commandSecure)
                .build()
        jc.parse(*args)

        if(jc.parsedCommand == "secure")
        {
            val trust = SecurityProfile.loadKeyStoreFromFile(commandSecure.trustedCert, commandSecure.trustPassword, "JKS")
            val identity = SecurityProfile.loadKeyStoreFromFile(commandSecure.identityCert, commandSecure.identityPassword, "PKCS12")

            println("Starting MobileHub in secure mode")
            MessageManager.start(SecureGatewayCommunication(InetSocketAddress("127.0.0.1", 5500), trust, commandSecure.trustPassword, identity, commandSecure.identityPassword))
            if (commandSecure.reservedMode) {
                val context = SSLContext.getInstance("TLSv1.2")
                val keyFact = KeyManagerFactory.getInstance("SunX509")
                keyFact.init(identity, commandSecure.identityPassword.toCharArray())
                val trustFact = TrustManagerFactory.getInstance("SunX509")
                //Yes, verify with the identity
                trustFact.init(identity)

                context.init(keyFact.keyManagers, trustFact.trustManagers, SecureRandom())
                SimulatorConnection.init(12346, context)
                println("Simulator connection in reserved mode")
            }
            else
                SimulatorConnection.init(12346)
        }
        else {
            println("Starting MobileHub in unsecure mode")
            MessageManager.start(GatewayCommunication(InetSocketAddress("127.0.0.1", 5500)))
            SimulatorConnection.init(12346)
        }
    }
}
