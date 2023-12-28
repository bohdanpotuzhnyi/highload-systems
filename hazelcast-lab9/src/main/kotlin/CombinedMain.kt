import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.jet.datamodel.Tuple2
import com.hazelcast.jet.pipeline.Pipeline
import com.hazelcast.jet.pipeline.Sinks
import com.hazelcast.jet.pipeline.Sources
import com.hazelcast.jet.aggregate.AggregateOperations
import com.hazelcast.jet.pipeline.WindowDefinition
import org.slf4j.LoggerFactory
import kotlinx.coroutines.runBlocking
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun main() = runBlocking {
    System.setProperty("hazelcast.logging.type", "slf4j")
    val logger = LoggerFactory.getLogger("PipelineLogger")

    val config = Config()
    config.jetConfig.isEnabled = true
    val hzInstance = Hazelcast.newHazelcastInstance(config)

    val pipeline = Pipeline.create()

    val sourceStage = pipeline.readFrom(Sources.fileWatcher("home/data/"))

    val timestampedStage = sourceStage
        .withTimestamps({ line ->
            val timestampPart = Regex("\\[(.*?)\\]").find(line)?.groups?.get(1)?.value
            val formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
            ZonedDateTime.parse(timestampPart, formatter)
                .withZoneSameInstant(ZoneId.of("UTC")).toInstant().toEpochMilli()
        }, 0)
        .map { line ->
            val parts = line.split(" ")
            val quoteIndices = parts.indices.filter { "\"" in parts[it] }
            val url = if (quoteIndices.size >= 2) {
                val requestParts = parts.subList(quoteIndices[0], quoteIndices[1] + 1)
                requestParts.joinToString(" ").split(" ")[1]
            } else {
                ""
            }
            val statusCode = parts.subList(quoteIndices[1] + 1, parts.size)
                .firstOrNull { it.toIntOrNull() != null }?.toInt() ?: 0
            Tuple2.tuple2(url, statusCode)
        }

    val commonStage = sourceStage.withoutTimestamps().map { line ->
        val parts = line.split(" ")
        val quoteIndices = parts.indices.filter { "\"" in parts[it] }
        val url = if (quoteIndices.size >= 2) {
            val requestParts = parts.subList(quoteIndices[0], quoteIndices[1] + 1)
            requestParts.joinToString(" ").split(" ")[1]
        } else {
            ""
        }
        val statusCode = parts.subList(quoteIndices[1] + 1, parts.size)
            .firstOrNull { it.toIntOrNull() != null }?.toInt() ?: 0
        if(statusCode == 200)
            logger.info("Procesing URL: ${url}, Status: ${statusCode}")
        Tuple2.tuple2(url, statusCode)
    }

    commonStage.filter { it.f1() == 200 }
        .map { Tuple2.tuple2(it.f0(), 1) }
        .writeTo(Sinks.mapWithMerging(
            "requestsCountMap",
            { it.f0() },
            { 1 },
            { oldValue: Int, value: Int ->
                logger.info("Updating URL - Old Value: $oldValue, New Value: ${oldValue + value}")
                oldValue + value
            }
        ))

    timestampedStage.filter { it.f1() == 200 }
        .window(WindowDefinition.sliding(30000, 10000))
        .aggregate(AggregateOperations.counting())
        .writeTo(Sinks.logger())

    println("Starting the pipeline")
    hzInstance.jet.newJob(pipeline).join()
}
