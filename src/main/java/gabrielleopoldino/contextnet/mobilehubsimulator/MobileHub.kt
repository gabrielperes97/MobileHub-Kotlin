package gabrielleopoldino.contextnet.mobilehubsimulator

import com.beust.jcommander.IStringConverter
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import gabrielleopoldino.contextnet.mobilehubsimulator.tecnologies.simulator.SimulatorConnection
import leopoldino.smrudp.SecurityProfile
import java.io.File
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import java.util.*


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
            SimulatorConnection.start(12346)
        }
        else {
            println("Starting MobileHub in unsecure mode")
            MessageManager.start(GatewayCommunication(InetSocketAddress("127.0.0.1", 5500)))
            SimulatorConnection.start(12346)
        }
    }
}
