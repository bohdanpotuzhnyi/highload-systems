import org.bson.Document
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import java.util.UUID
import java.time.LocalDate
import kotlin.random.Random

/**
 * Main function to manage MongoDB data operations.
 * It connects to a MongoDB database, clears specified collections,
 * generates new data, and then inserts this data into the collections.
 * Finally, it closes the database connection.
 */
fun main() {
    val connectionString = "mongodb://localhost:27017"
    val databaseName = "onlineStore"

    val client = KMongo.createClient(connectionString)
    val database = client.getDatabase(databaseName)

    // Clear collections
    clearCollections(database, "items", "reviews", "orders")

    // Generate and insert data
    val items = generateItems(20)
    database.getCollection<Document>("items").insertMany(items.map { Document(it as Map<String, Any>) })

    val reviews = generateReviews(50)
    database.getCollection<Document>("reviews").insertMany(reviews.map { Document(it as Map<String, Any>) })

    val names = listOf("Andrii", "Ivan", "Olena", "Anna", "Petro")
    val surnames = listOf("Smith", "Brown", "Johnson", "Williams", "Jones")
    val customerData = generateCustomerData(names, surnames)

    val orders = generateOrders(100, items.map { it["_id"] as String }, customerData)
    database.getCollection<Document>("orders").insertMany(orders.map { Document(it as Map<String, Any>) })

    client.close()
}

/**
 * Clears the specified collections in the MongoDB database.
 *
 * @param database The MongoDB database instance.
 * @param collectionNames Vararg parameter specifying the names of the collections to be cleared.
 */
fun clearCollections(database: com.mongodb.client.MongoDatabase, vararg collectionNames: String) {
    collectionNames.forEach { collectionName ->
        val collection = database.getCollection<Document>(collectionName)
        collection.deleteMany(Document())
    }
}

/**
 * Generates a map of customer data with consistent `cardId` and phone numbers for each unique name-surname pair.
 *
 * @param names List of first names to be used in generating customer data.
 * @param surnames List of surnames to be used in generating customer data.
 * @return A map where the key is the full name (name and surname) and the value is a Pair of cardId and phone numbers.
 */
fun generateCustomerData(names: List<String>, surnames: List<String>): Map<String, Pair<String, List<Long>>> {
    val customerData = mutableMapOf<String, Pair<String, List<Long>>>()

    for (name in names) {
        for (surname in surnames) {
            val fullName = "$name $surname"
            val cardId = Random.nextLong(10000000, 99999999).toString()
            val phones = listOf(Random.nextLong(1000000, 9999999), Random.nextLong(1000000, 9999999))
            customerData[fullName] = Pair(cardId, phones)
        }
    }

    return customerData
}

/**
 * Generates a list of item data for insertion into the 'items' collection.
 *
 * @param count The number of item data entries to generate.
 * @return A list of maps, each representing an item with fields such as _id, category, model, producer, price, and feature.
 */
fun generateItems(count: Int): List<Map<String, Any>> {
    val categories = listOf("Phone", "Laptop", "Tablet")
    val models = mapOf(
        "Phone" to listOf("iPhone 6", "Galaxy S10", "Pixel 4"),
        "Laptop" to listOf("MacBook Pro", "Dell XPS", "Lenovo ThinkPad"),
        "Tablet" to listOf("iPad Pro", "Samsung Galaxy Tab", "Microsoft Surface")
    )
    val producers = listOf("Apple", "Samsung", "Google", "Dell", "Lenovo", "Microsoft")
    val features = listOf("Water Resistant", "High Performance", "Long Battery Life")

    return List(count) {
        val category = categories.random()
        mapOf(
            "_id" to UUID.randomUUID().toString(),
            "category" to category,
            "model" to (models[category]?.random() ?: "Unknown Model"),
            "producer" to producers.random(),
            "price" to Random.nextInt(500, 2000),
            "feature" to features.random()
        )
    }
}

/**
 * Generates a list of review data for insertion into the 'reviews' collection.
 *
 * @param count The number of review data entries to generate.
 * @return A list of maps, each representing a review with fields such as _id and review text.
 */
fun generateReviews(count: Int): List<Map<String, Any>> {
    return List(count) {
        mapOf(
            "_id" to UUID.randomUUID().toString(),
            "review" to "Review ${Random.nextInt(1, 100)}"
        )
    }
}

/**
 * Generates a list of order data for insertion into the 'orders' collection.
 * Each order includes customer information and items ordered.
 *
 * @param count The number of order data entries to generate.
 * @param itemIds A list of item IDs to be used for generating order items.
 * @param customerData A map of customer data for generating consistent customer information in orders.
 * @return A list of maps, each representing an order with fields such as _id, order_number, date, total_sum, customer, payment, and order_items_id.
 */
fun generateOrders(count: Int, itemIds: List<String>, customerData: Map<String, Pair<String, List<Long>>>): List<Map<String, Any>> {
    return List(count) {
        val customer = customerData.entries.random()
        val itemCount = Random.nextInt(1, 5)
        mapOf(
            "_id" to UUID.randomUUID().toString(),
            "order_number" to Random.nextInt(100000, 999999),
            "date" to LocalDate.now().minusDays(Random.nextLong(0, 365)).toString(),
            "total_sum" to Random.nextInt(1000, 5000),
            "customer" to mapOf(
                "name" to customer.key.split(" ")[0],
                "surname" to customer.key.split(" ")[1],
                "phones" to customer.value.second,
                "address" to "Some Address"
            ),
            "payment" to mapOf(
                "card_owner" to customer.key,
                "cardId" to customer.value.first
            ),
            "order_items_id" to List(itemCount) { mapOf("\$ref" to "items", "\$id" to itemIds.random()) }
        )
    }
}
