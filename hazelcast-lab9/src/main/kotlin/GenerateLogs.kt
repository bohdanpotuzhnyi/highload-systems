import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

fun generateRandomIpAddress(): String {
    return (1..4).joinToString(".") { Random.nextInt(1, 255).toString() }
}

fun generateLogLine(): String {
    val ipAddress = generateRandomIpAddress()
    val userIdentifier = "-"
    val userId = "-"
    val dateFormat = SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z")
    val currentTime = dateFormat.format(Date())
    val method = "GET"
    val resources = listOf("/index.html", "/contact.html", "/about.html", "/products.html", "/api/data")
    val resource = resources.random()
    val protocol = "HTTP/1.1"
    val statuses = listOf(200, 404, 500, 301, 403)
    val status = statuses.random()
    val size = (100..1024).random()

    return "$ipAddress $userIdentifier $userId [$currentTime] \"$method $resource $protocol\" $status $size\n"
}

fun main() {
    val file = File("home/data/web-server.log")
    val executor = Executors.newSingleThreadScheduledExecutor()

    executor.scheduleAtFixedRate({
        file.appendText(generateLogLine())
        println("Log entry added.")
    }, 0, 1, TimeUnit.SECONDS)
}
