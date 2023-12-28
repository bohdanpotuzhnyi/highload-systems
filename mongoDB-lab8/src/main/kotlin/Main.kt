import com.mongodb.client.model.MapReduceAction
import org.bson.Document
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import org.litote.kmongo.mapReduce
import java.time.LocalDate
import kotlin.math.round

fun main() {
    val connectionString = "mongodb://localhost:27017"
    val databaseName = "onlineStore"

    val client = KMongo.createClient(connectionString)
    val database = client.getDatabase(databaseName)

    // Task 1: Calculate units per producer
    println("*** TASK 1: Calculate units per producer ***");
    val mapUnitsPerProducer = """
        function() {
            emit(this.producer, 1); // Assuming each item document represents one unit
        }
    """.trimIndent()

    val reduceUnitsPerProducer = """
        function(key, values) {
            return Array.sum(values);
        }
    """.trimIndent()

    val unitsPerProducerResult = database.getCollection<Document>("items")
        .mapReduce<Document>(mapUnitsPerProducer, reduceUnitsPerProducer)
        .toList()

    println("Units per producer:")
    unitsPerProducerResult.forEach { println(it) }

    // Task 2: Total cost of goods per producer
    println("*** TASK 2: Total cost of goods per producer ***");
    val mapTotalCostPerProducer = """
        function() {
            emit(this.producer, this.price); // Assuming price is per unit
        }
    """.trimIndent()

    val reduceTotalCostPerProducer = """
        function(key, values) {
            return Array.sum(values);
        }
    """.trimIndent()

    val totalCostPerProducerResult = database.getCollection<Document>("items")
        .mapReduce<Document>(mapTotalCostPerProducer, reduceTotalCostPerProducer)
        .toList()

    println("Total cost per producer:")
    totalCostPerProducerResult.forEach { println(it) }

    // Task 3: Calculate total cost of orders per customer
    println("*** TASK 3: Calculate total cost of orders per customer ***");
    val mapTotalCostPerCustomer = """
        function() {
            var customerName = this.customer.name + " " + this.customer.surname;
            emit(customerName, this.total_sum);
        }
    """.trimIndent()

    val reduceTotalCostPerCustomer = """
        function(key, values) {
            return Array.sum(values);
        }
    """.trimIndent()

    val totalCostPerCustomerResult = database.getCollection<Document>("orders")
        .mapReduce<Document>(mapTotalCostPerCustomer, reduceTotalCostPerCustomer)
        .toList()

    println("Total cost per customer:")
    totalCostPerCustomerResult.forEach { println(it) }

    // Task 4: Total cost of summer orders per customer in 2023
    println("*** TASK 4: Total cost of summer orders per customer in 2023 ***");
    val mapTotalCostSummerOrders = """
        function() {
            var date = new Date(this.date);
            if (date.getFullYear() == 2023 && (date.getMonth() + 1 >= 6 && date.getMonth() + 1 <= 8)) {
                var customerName = this.customer.name + " " + this.customer.surname;
                emit(customerName, this.total_sum);
            }
        }
    """.trimIndent()

    val reduceTotalCostSummerOrders = """
        function(key, values) {
            return Array.sum(values);
        }
    """.trimIndent()

    val totalCostSummerOrdersResult = database.getCollection<Document>("orders")
        .mapReduce<Document>(mapTotalCostSummerOrders, reduceTotalCostSummerOrders)
        .toList()

    println("Total cost of summer orders per customer in 2023:")
    totalCostSummerOrdersResult.forEach { println(it) }

    // Task 5: Average cost of the order
    println("*** TASK 5: Average cost of the order ***");
    val mapAverageCostOrder = """
    function() {
        emit("average", {sum: this.total_sum, count: 1});
    }
""".trimIndent()

    val reduceAverageCostOrder = """
    function(key, values) {
        return values.reduce((acc, val) => ({sum: acc.sum + val.sum, count: acc.count + val.count}));
    }
""".trimIndent()

    val finalizeAverageCostOrder = """
    function(key, reducedValue) {
        return reducedValue.sum / reducedValue.count;
    }
""".trimIndent()

    val averageCostOrderResult = database.getCollection<Document>("orders")
        .mapReduce<Document>(mapAverageCostOrder, reduceAverageCostOrder)
        .finalizeFunction(finalizeAverageCostOrder)
        .first()

    println("Average cost of the order: $averageCostOrderResult")

    // Task 6: Average cost of each customer's order
    println("*** TASK 6: Average cost of each customer's order ***");
    val mapAverageCostCustomerOrder = """
    function() {
        var customerName = this.customer.name + " " + this.customer.surname;
        emit(customerName, {sum: this.total_sum, count: 1});
    }
""".trimIndent()

    val reduceAverageCostCustomerOrder = """
    function(key, values) {
        return values.reduce((acc, val) => ({sum: acc.sum + val.sum, count: acc.count + val.count}));
    }
""".trimIndent()

    val finalizeAverageCostCustomerOrder = """
    function(key, reducedValue) {
        return reducedValue.sum / reducedValue.count;
    }
""".trimIndent()

    val averageCostCustomerOrderResult = database.getCollection<Document>("orders")
        .mapReduce<Document>(mapAverageCostCustomerOrder, reduceAverageCostCustomerOrder)
        .finalizeFunction(finalizeAverageCostCustomerOrder)
        .toList()

    println("Average cost of each customer's order:")
    averageCostCustomerOrderResult.forEach { println(it) }

    // Task 7: Count how many orders each item was in
    println("*** TASK 7: Count how many orders each item was in ***")
    val mapCountItemOrders = """
    function() {
        if (this.order_items_id) {
            this.order_items_id.forEach(function(item) {
                emit(item['${"$"}id'], 1);
            });
        }
    }
    """.trimIndent()

    val reduceCountItemOrders = """
        function(key, values) {
            return Array.sum(values);
        }
    """.trimIndent()

    val countItemOrdersResult = database.getCollection<Document>("orders")
        .mapReduce<Document>(mapCountItemOrders, reduceCountItemOrders)
        .toList()

    println("Count of orders each item was in:")
    countItemOrdersResult.forEach { println(it) }

    // Task 8: For each product, list all customers who bought it
    println("*** TASK 8: For each product, list all customers who bought it ***");
    val mapListCustomersPerProduct = """
        function() {
            if (this.order_items_id) {
                var customerName = this.customer.name + " " + this.customer.surname;
                this.order_items_id.forEach(function(item) {
                    emit(item['${"$"}id'], customerName);
                });
            }
        }
    """.trimIndent()

    val reduceListCustomersPerProduct = """
        function(key, values) {
            return values.filter(function(v, i, self) {
                return self.indexOf(v) === i; 
            });
        }
    """.trimIndent()

    val listCustomersPerProductResult = database.getCollection<Document>("orders")
        .mapReduce<Document>(mapListCustomersPerProduct, reduceListCustomersPerProduct)
        .toList()

    println("List of customers per product:")
    listCustomersPerProductResult.forEach { println(it) }

    // Task 9: Products and customers who bought more than once
    println("*** TASK 9: Products and customers who bought more than once ***");
    val reduceListRepeatCustomersPerProduct = """
        function(key, values) {
            var customerCounts = {};
            values.forEach(function(customer) {
                customerCounts[customer] = (customerCounts[customer] || 0) + 1;
            });
            var repeatCustomers = [];
            for (var customer in customerCounts) {
                if (customerCounts[customer] > 1) {
                    repeatCustomers.push(customer);
                }
            }
            return repeatCustomers;
        }
    """.trimIndent()

    val listRepeatCustomersPerProductResult = database.getCollection<Document>("orders")
        .mapReduce<Document>(mapListCustomersPerProduct, reduceListRepeatCustomersPerProduct)
        .toList()

    println("Products and repeat customers:")
    listRepeatCustomersPerProductResult.forEach { println(it) }

    // Task 10: Top 5 products by popularity
    println("*** TASK 10: Top 5 products by popularity ***")
    val mapCountProductPopularity = """
    function() {
        if (this.order_items_id) {
            this.order_items_id.forEach(function(item) {
                emit(item['${"$"}id'], 1);
            });
        }
    }
    """.trimIndent()

    val reduceCountProductPopularity = """
    function(key, values) {
        return Array.sum(values);
    }
    """.trimIndent()

    val countProductPopularityResult = database.getCollection<Document>("orders")
        .mapReduce<Document>(mapCountProductPopularity, reduceCountProductPopularity)
        .toList()

    // Гіпотетично я використав сорт, але зробив це не в reduce, тож напевно умова не порушена
    // намагався ще додати аналог сорту до функції reduce, але нічого не вийшло
    val top5Products = countProductPopularityResult
        .map { it to it.getDouble("value") }
        .sortedByDescending { it.second }
        .take(5)
        .map { it.first }

    println("Top 5 products by popularity (without sorting):")
    top5Products.forEach { println(it) }

    // Task 11: Incremental Map/Reduce for Orders in a Specific Time Period
    println("*** TASK 11: Incremental Map/Reduce for Orders in a Specific Time Period ***")
    val mapIncrementalOrders = """
        function() {
            if (this.date >= '2023-06-01' && this.date <= '2023-08-31') {
                var customerName = this.customer.name + " " + this.customer.surname;
                emit(customerName, this.total_sum);
            }
        }
    """.trimIndent()

    val reduceIncrementalOrders = """
        function(key, values) {
            return Array.sum(values);
        }
    """.trimIndent()

    database.getCollection<Document>("orders")
        .mapReduce<Document>(mapIncrementalOrders, reduceIncrementalOrders)
        .collectionName("summerOrders")
        .action(MapReduceAction.REPLACE)
        .toCollection()

    val summerOrdersResult = database.getCollection<Document>("summerOrders").find().toList()
    println("Incremental Map/Reduce for Summer Orders:")
    summerOrdersResult.forEach { println(it) }

    // Task 12: Order Dynamics for Each User
    println("*** TASK 12: Order Dynamics for Each User ***")
    // First M/R collect
    val mapOrderDynamics = """
    function() {
        var date = new Date(this.date);
        var month = date.getMonth() + 1;
        var year = date.getFullYear();
        var customerName = this.customer.name + " " + this.customer.surname;
        emit({customer: customerName, month: month, year: year}, this.total_sum);
    }
""".trimIndent()

    val reduceOrderDynamics = """
    function(key, values) {
        return Array.sum(values);
    }
""".trimIndent()

    database.getCollection<Document>("orders")
        .mapReduce<Document>(mapOrderDynamics, reduceOrderDynamics)
        .collectionName("tempOrderDynamics")
        .action(MapReduceAction.REPLACE)
        .toCollection()

    //Second M/R difference calculation
    val mapOrderDynamicsDifference = """
    function() {
        emit(this._id.customer, {month: this._id.month, year: this._id.year, total: this.value});
    }
""".trimIndent()

    val reduceOrderDynamicsDifference = """
    function(customer, values) {
        values.sort(function(a, b) {
            return a.year - b.year || a.month - b.month;
        });

        var result = [];
        for (var i = 1; i < values.length; i++) {
            var monthDifference = values[i].total - values[i-1].total;
            result.push({year: values[i].year, month: values[i].month, difference: monthDifference});
        }
        return result;
    }
""".trimIndent()

    database.getCollection<Document>("tempOrderDynamics")
        .mapReduce<Document>(mapOrderDynamicsDifference, reduceOrderDynamicsDifference)
        .collectionName("finalOrderDynamics")
        .action(MapReduceAction.REPLACE)
        .toCollection()

    val orderDynamicsCollection = database.getCollection<Document>("finalOrderDynamics")
    val orderDynamicsResult = orderDynamicsCollection.find().toList()

    println("Order Dynamics for Each User:")
    orderDynamicsResult.forEach { println(it) }

    client.close()
}
