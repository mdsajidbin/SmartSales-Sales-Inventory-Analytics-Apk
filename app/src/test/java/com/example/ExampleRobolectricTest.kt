package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SmartSales", appName)
  }

  @Test
  fun `product stock status calculations`() {
    val inStockProduct = com.example.model.Product(
      id = "p1",
      name = "Test Item",
      categoryId = "c1",
      price = 20.0,
      stock = 25,
      lowStockThreshold = 5
    )
    assertEquals(com.example.model.StockStatus.IN_STOCK, inStockProduct.getStockStatus())

    val lowStockProduct = inStockProduct.copy(stock = 4)
    assertEquals(com.example.model.StockStatus.LOW_STOCK, lowStockProduct.getStockStatus())

    val outOfStockProduct = inStockProduct.copy(stock = 0)
    assertEquals(com.example.model.StockStatus.OUT_OF_STOCK, outOfStockProduct.getStockStatus())
  }

  @Test
  fun `admin can add stock and stock increases`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.SmartSalesRepository(context)
    val adminUser = com.example.model.User(
      id = "admin1",
      email = "admin@smartsales.com",
      name = "Admin User",
      role = com.example.model.UserRole.ADMIN.value
    )
    repo.setCurrentUser(adminUser)

    val testProduct = com.example.model.Product(
      id = "test_prod_1",
      name = "Test Widget",
      categoryId = "cat1",
      price = 10.0,
      stock = 50,
      lowStockThreshold = 10
    )
    repo.addProduct(testProduct) {}

    var callbackCalled = false
    var resultNewStock = 0
    repo.addStock("test_prod_1", 20, "Admin restock") { result ->
      callbackCalled = true
      result.onSuccess { adj ->
        resultNewStock = adj.newStock
      }
    }
    org.junit.Assert.assertTrue(callbackCalled)
    assertEquals(70, resultNewStock)
    assertEquals(70, repo.products.value.find { it.id == "test_prod_1" }?.stock)
  }

  @Test
  fun `staff cannot add stock`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.SmartSalesRepository(context)
    val staffUser = com.example.model.User(
      id = "staff1",
      email = "staff@smartsales.com",
      name = "Staff User",
      role = com.example.model.UserRole.STAFF.value
    )
    repo.setCurrentUser(staffUser)

    val testProduct = com.example.model.Product(
      id = "test_prod_2",
      name = "Test Gadget",
      categoryId = "cat1",
      price = 15.0,
      stock = 50
    )
    repo.addProduct(testProduct) {}

    var failed = false
    repo.addStock("test_prod_2", 20, "Unauthorized") { result ->
      result.onFailure {
        failed = true
      }
    }
    org.junit.Assert.assertTrue("Staff user must be denied addStock", failed)
    assertEquals(50, repo.products.value.find { it.id == "test_prod_2" }?.stock)
  }

  @Test
  fun `sale automatically reduces stock for admin and staff`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.SmartSalesRepository(context)
    val staffUser = com.example.model.User(
      id = "staff2",
      email = "staff2@smartsales.com",
      name = "Staff Two",
      role = com.example.model.UserRole.STAFF.value
    )
    repo.setCurrentUser(staffUser)

    val testProduct = com.example.model.Product(
      id = "test_prod_3",
      name = "Cold Brew",
      categoryId = "cat1",
      price = 5.0,
      stock = 50
    )
    repo.addProduct(testProduct) {}

    val saleItem = com.example.model.SaleItem(
      productId = "test_prod_3",
      productName = "Cold Brew",
      quantity = 5,
      unitPrice = 5.0,
      lineTotal = 25.0
    )

    var saleSuccess = false
    repo.createSale("guest", "Walk-in Customer", listOf(saleItem)) { result ->
      result.onSuccess {
        saleSuccess = true
      }
    }
    org.junit.Assert.assertTrue("Sale must succeed for staff", saleSuccess)
    assertEquals(45, repo.products.value.find { it.id == "test_prod_3" }?.stock)
  }

  @Test
  fun `sale cannot exceed available stock`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.SmartSalesRepository(context)
    val staffUser = com.example.model.User(
      id = "staff3",
      email = "staff3@smartsales.com",
      name = "Staff Three",
      role = com.example.model.UserRole.STAFF.value
    )
    repo.setCurrentUser(staffUser)

    val testProduct = com.example.model.Product(
      id = "test_prod_4",
      name = "Espresso",
      categoryId = "cat1",
      price = 4.0,
      stock = 10
    )
    repo.addProduct(testProduct) {}

    val saleItem = com.example.model.SaleItem(
      productId = "test_prod_4",
      productName = "Espresso",
      quantity = 15,
      unitPrice = 4.0,
      lineTotal = 60.0
    )

    var saleFailed = false
    repo.createSale("guest", "Walk-in Customer", listOf(saleItem)) { result ->
      result.onFailure {
        saleFailed = true
      }
    }
    org.junit.Assert.assertTrue("Sale exceeding stock must fail", saleFailed)
    assertEquals(10, repo.products.value.find { it.id == "test_prod_4" }?.stock)
  }

  @Test
  fun `sales flow for Organic Arabian Coffee records sale and decrements stock`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.SmartSalesRepository(context)
    val viewModel = com.example.viewmodel.SmartSalesViewModel(repo)

    val coffee = repo.products.value.find { it.id == "prod_arabica_coffee" }
    org.junit.Assert.assertNotNull(coffee)
    val initialStock = coffee!!.stock

    // Record sale of 3 units
    viewModel.recordSingleSale(coffee, 3, "cust_test", "Test Customer")

    val updatedCoffee = repo.products.value.find { it.id == "prod_arabica_coffee" }
    org.junit.Assert.assertNotNull(updatedCoffee)
    assertEquals(initialStock - 3, updatedCoffee!!.stock)
  }

  @Test
  fun `product with unexpected null or extreme values handled safely without crash`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.SmartSalesRepository(context)
    val viewModel = com.example.viewmodel.SmartSalesViewModel(repo)

    val edgeProduct = com.example.model.Product(
      id = "edge_prod",
      name = "",
      categoryId = "",
      price = -5.0,
      stock = 0
    )
    repo.addProduct(edgeProduct) {}

    // Attempting sale on 0 stock should fail gracefully without crashing
    viewModel.recordSingleSale(edgeProduct, 1, "guest", "Walk-in")
    assertEquals(0, repo.products.value.find { it.id == "edge_prod" }?.stock)
  }
}
