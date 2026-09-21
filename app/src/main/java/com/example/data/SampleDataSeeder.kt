package com.example.data

import com.example.model.*
import java.util.Calendar

object SampleDataSeeder {

    fun getDefaultCategories(): List<Category> = listOf(
        Category(id = "cat_beverages", name = "Beverages"),
        Category(id = "cat_snacks", name = "Snacks & Food"),
        Category(id = "cat_stationery", name = "Stationery"),
        Category(id = "cat_electronics", name = "Electronics & Cables"),
        Category(id = "cat_personal_care", name = "Personal Care")
    )

    fun getDefaultProducts(): List<Product> = listOf(
        Product(
            id = "prod_arabica_coffee",
            name = "Organic Arabian Coffee (250g)",
            categoryId = "cat_beverages",
            price = 14.50,
            stock = 24,
            lowStockThreshold = 8,
            imageUrl = "",
            supplier = "Highland Roast Co."
        ),
        Product(
            id = "prod_matcha_tea",
            name = "Ceremonial Matcha Green Tea",
            categoryId = "cat_beverages",
            price = 19.99,
            stock = 4, // Low stock!
            lowStockThreshold = 6,
            imageUrl = "",
            supplier = "Kyoto Imports"
        ),
        Product(
            id = "prod_oat_milk",
            name = "Barista Oat Milk (1L)",
            categoryId = "cat_beverages",
            price = 4.25,
            stock = 38,
            lowStockThreshold = 10,
            imageUrl = "",
            supplier = "Valley Fresh Dairy"
        ),
        Product(
            id = "prod_dark_chocolate",
            name = "72% Artisanal Dark Chocolate Bar",
            categoryId = "cat_snacks",
            price = 3.50,
            stock = 15,
            lowStockThreshold = 5,
            imageUrl = "",
            supplier = "CacaoCraft"
        ),
        Product(
            id = "prod_granola_bars",
            name = "Honey Almond Protein Bars (Box 6)",
            categoryId = "cat_snacks",
            price = 8.75,
            stock = 2, // Low stock!
            lowStockThreshold = 5,
            imageUrl = "",
            supplier = "NatureFuel"
        ),
        Product(
            id = "prod_sparkling_water",
            name = "Sparkling Lime Essence Water (6-pack)",
            categoryId = "cat_beverages",
            price = 6.20,
            stock = 0, // Out of stock!
            lowStockThreshold = 5,
            imageUrl = "",
            supplier = "PureSprings"
        ),
        Product(
            id = "prod_notebook",
            name = "A5 Dot-Grid Hardcover Journal",
            categoryId = "cat_stationery",
            price = 12.00,
            stock = 30,
            lowStockThreshold = 8,
            imageUrl = "",
            supplier = "PaperCraft Press"
        ),
        Product(
            id = "prod_gel_pens",
            name = "Quick-Dry Fine Gel Pens (Set of 5)",
            categoryId = "cat_stationery",
            price = 7.50,
            stock = 18,
            lowStockThreshold = 5,
            imageUrl = "",
            supplier = "PaperCraft Press"
        ),
        Product(
            id = "prod_usb_c_cable",
            name = "Braided USB-C to USB-C Fast Cable (2m)",
            categoryId = "cat_electronics",
            price = 15.99,
            stock = 12,
            lowStockThreshold = 5,
            imageUrl = "",
            supplier = "VoltTech Labs"
        ),
        Product(
            id = "prod_power_adapter",
            name = "65W GaN Dual USB Wall Charger",
            categoryId = "cat_electronics",
            price = 29.99,
            stock = 7,
            lowStockThreshold = 4,
            imageUrl = "",
            supplier = "VoltTech Labs"
        ),
        Product(
            id = "prod_hand_sanitizer",
            name = "Aloe Vera Hand Sanitizer Spray (100ml)",
            categoryId = "cat_personal_care",
            price = 3.25,
            stock = 45,
            lowStockThreshold = 10,
            imageUrl = "",
            supplier = "CleanCare Labs"
        )
    )

    fun getDefaultCustomers(): List<Customer> = listOf(
        Customer(
            id = "cust_1",
            name = "Sophia Martinez",
            phone = "+1 (555) 234-5678",
            email = "sophia.m@example.com"
        ),
        Customer(
            id = "cust_2",
            name = "David Chen",
            phone = "+1 (555) 345-6789",
            email = "david.chen@example.com"
        ),
        Customer(
            id = "cust_3",
            name = "Elena Rostova",
            phone = "+1 (555) 456-7890",
            email = "elena.r@example.com"
        ),
        Customer(
            id = "cust_4",
            name = "Marcus Aurelius Williams",
            phone = "+1 (555) 567-8901",
            email = "marcus.w@example.com"
        ),
        Customer(
            id = "cust_5",
            name = "Walk-in Guest",
            phone = "+1 (555) 000-0000",
            email = ""
        )
    )

    fun getDefaultSales(adminUid: String = "admin_demo", staffUid: String = "staff_demo"): List<Sale> {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000
        val cal = Calendar.getInstance()

        return listOf(
            Sale(
                id = "sale_101",
                customerId = "cust_1",
                customerName = "Sophia Martinez",
                totalAmount = 43.49,
                saleDate = now - (2 * 60 * 60 * 1000), // Today 2 hours ago
                salesPersonId = staffUid,
                salesPersonName = "Alex Staff",
                items = listOf(
                    SaleItem("prod_arabica_coffee", "Organic Arabian Coffee (250g)", 2, 14.50, 29.00),
                    SaleItem("prod_oat_milk", "Barista Oat Milk (1L)", 1, 4.25, 4.25),
                    SaleItem("prod_dark_chocolate", "72% Artisanal Dark Chocolate Bar", 2, 3.50, 7.00),
                    SaleItem("prod_hand_sanitizer", "Aloe Vera Hand Sanitizer Spray (100ml)", 1, 3.24, 3.24)
                )
            ),
            Sale(
                id = "sale_102",
                customerId = "cust_2",
                customerName = "David Chen",
                totalAmount = 45.98,
                saleDate = now - (5 * 60 * 60 * 1000), // Today 5 hours ago
                salesPersonId = adminUid,
                salesPersonName = "Sarah Admin",
                items = listOf(
                    SaleItem("prod_power_adapter", "65W GaN Dual USB Wall Charger", 1, 29.99, 29.99),
                    SaleItem("prod_usb_c_cable", "Braided USB-C to USB-C Fast Cable (2m)", 1, 15.99, 15.99)
                )
            ),
            Sale(
                id = "sale_103",
                customerId = "cust_3",
                customerName = "Elena Rostova",
                totalAmount = 31.50,
                saleDate = now - (1 * oneDay), // Yesterday
                salesPersonId = staffUid,
                salesPersonName = "Alex Staff",
                items = listOf(
                    SaleItem("prod_notebook", "A5 Dot-Grid Hardcover Journal", 2, 12.00, 24.00),
                    SaleItem("prod_gel_pens", "Quick-Dry Fine Gel Pens (Set of 5)", 1, 7.50, 7.50)
                )
            ),
            Sale(
                id = "sale_104",
                customerId = "cust_4",
                customerName = "Marcus Aurelius Williams",
                totalAmount = 54.48,
                saleDate = now - (3 * oneDay), // 3 days ago
                salesPersonId = adminUid,
                salesPersonName = "Sarah Admin",
                items = listOf(
                    SaleItem("prod_matcha_tea", "Ceremonial Matcha Green Tea", 2, 19.99, 39.98),
                    SaleItem("prod_arabica_coffee", "Organic Arabian Coffee (250g)", 1, 14.50, 14.50)
                )
            ),
            Sale(
                id = "sale_105",
                customerId = "cust_5",
                customerName = "Walk-in Guest",
                totalAmount = 19.50,
                saleDate = now - (6 * oneDay), // 6 days ago
                salesPersonId = staffUid,
                salesPersonName = "Alex Staff",
                items = listOf(
                    SaleItem("prod_granola_bars", "Honey Almond Protein Bars (Box 6)", 2, 8.75, 17.50),
                    SaleItem("prod_dark_chocolate", "72% Artisanal Dark Chocolate Bar", 1, 2.00, 2.00)
                )
            ),
            Sale(
                id = "sale_106",
                customerId = "cust_1",
                customerName = "Sophia Martinez",
                totalAmount = 67.97,
                saleDate = now - (14 * oneDay), // 14 days ago
                salesPersonId = adminUid,
                salesPersonName = "Sarah Admin",
                items = listOf(
                    SaleItem("prod_power_adapter", "65W GaN Dual USB Wall Charger", 1, 29.99, 29.99),
                    SaleItem("prod_arabica_coffee", "Organic Arabian Coffee (250g)", 2, 14.50, 29.00),
                    SaleItem("prod_granola_bars", "Honey Almond Protein Bars (Box 6)", 1, 8.98, 8.98)
                )
            ),
            Sale(
                id = "sale_107",
                customerId = "cust_2",
                customerName = "David Chen",
                totalAmount = 36.49,
                saleDate = now - (25 * oneDay), // 25 days ago
                salesPersonId = staffUid,
                salesPersonName = "Alex Staff",
                items = listOf(
                    SaleItem("prod_matcha_tea", "Ceremonial Matcha Green Tea", 1, 19.99, 19.99),
                    SaleItem("prod_usb_c_cable", "Braided USB-C to USB-C Fast Cable (2m)", 1, 15.99, 15.99),
                    SaleItem("prod_hand_sanitizer", "Aloe Vera Hand Sanitizer Spray (100ml)", 1, 0.51, 0.51)
                )
            )
        )
    }

    fun getDefaultStockAdjustments(adminUid: String = "admin_demo"): List<StockAdjustment> {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000
        return listOf(
            StockAdjustment(
                id = "adj_1",
                productId = "prod_arabica_coffee",
                productName = "Organic Arabian Coffee (250g)",
                previousStock = 10,
                newStock = 24,
                changeAmount = 14,
                reason = "Weekly restock delivery from Highland Roast",
                adjustedBy = "Sarah Admin",
                timestamp = now - (2 * oneDay)
            ),
            StockAdjustment(
                id = "adj_2",
                productId = "prod_sparkling_water",
                productName = "Sparkling Lime Essence Water (6-pack)",
                previousStock = 2,
                newStock = 0,
                changeAmount = -2,
                reason = "Damaged during handling in stockroom",
                adjustedBy = "Sarah Admin",
                timestamp = now - (4 * oneDay)
            )
        )
    }
}
