package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SmartSalesRepository(private val context: Context) {

    private val TAG = "SmartSalesRepo"

    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseDb: FirebaseDatabase? = null
    private var isFirebaseAvailable: Boolean = false

    private val prefs = context.getSharedPreferences("smart_sales_session", Context.MODE_PRIVATE)
    private val accountsPrefs = context.getSharedPreferences("smart_sales_registered_users", Context.MODE_PRIVATE)

    // Pre-registered accounts: Admin and Staff have completely separate accounts and passwords
    private val defaultAccounts = listOf(
        UserAccount(
            id = "admin_acc_1",
            name = "Admin Sarah",
            email = "admin@gmail.com",
            password = "admin123",
            role = UserRole.ADMIN.value
        ),
        UserAccount(
            id = "staff_acc_1",
            name = "Staff Alex",
            email = "staff@gmail.com",
            password = "staff123",
            role = UserRole.STAFF.value
        )
    )

    private fun getRegisteredAccounts(): List<UserAccount> {
        val list = defaultAccounts.toMutableList()
        val allEntries = accountsPrefs.all
        for ((key, value) in allEntries) {
            if (key.startsWith("user_") && value is String) {
                val parts = value.split("|")
                if (parts.size >= 5) {
                    list.add(
                        UserAccount(
                            id = parts[0],
                            name = parts[1],
                            email = parts[2],
                            password = parts[3],
                            role = parts[4],
                            createdAt = parts.getOrNull(5)?.toLongOrNull() ?: System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        return list
    }

    private fun saveAccount(account: UserAccount) {
        val serialized = "${account.id}|${account.name}|${account.email}|${account.password}|${account.role}|${account.createdAt}"
        accountsPrefs.edit().putString("user_${account.email.lowercase().trim()}", serialized).apply()
    }

    fun findAccount(email: String): UserAccount? {
        val clean = email.trim().lowercase()
        val accounts = getRegisteredAccounts()
        val found = accounts.find { it.email.lowercase() == clean }
        if (found != null) return found
        // Also support aliases for the pre-configured accounts
        if (clean == "admin@smartsales.com") {
            return accounts.find { it.email == "admin@gmail.com" }
        }
        if (clean == "staff@smartsales.com") {
            return accounts.find { it.email == "staff@gmail.com" }
        }
        return null
    }

    fun findAccountRole(email: String): String? {
        return findAccount(email)?.role
    }

    private fun verifyPassword(account: UserAccount, pass: String): Boolean {
        val cleanPass = pass.trim()
        if (account.password == cleanPass) return true
        // Allow backwards-compatible convenience password for seeded accounts
        if ((account.id == "admin_acc_1" || account.id == "staff_acc_1") && cleanPass == "password123") return true
        return false
    }

    private fun loadSavedUser(): User? {
        val uid = prefs.getString("user_uid", null) ?: return null
        val name = prefs.getString("user_name", "User") ?: "User"
        val email = prefs.getString("user_email", "") ?: ""
        val role = prefs.getString("user_role", UserRole.STAFF.value) ?: UserRole.STAFF.value
        val createdAt = prefs.getLong("user_created_at", System.currentTimeMillis())
        return User(uid, name, email, role, createdAt)
    }

    private fun saveUserSession(user: User?) {
        if (user != null) {
            prefs.edit()
                .putString("user_uid", user.id)
                .putString("user_name", user.name)
                .putString("user_email", user.email)
                .putString("user_role", user.role)
                .putLong("user_created_at", user.createdAt)
                .apply()
        } else {
            prefs.edit().clear().apply()
        }
    }

    private val _currentUser = MutableStateFlow<User?>(loadSavedUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    private val _sales = MutableStateFlow<List<Sale>>(emptyList())
    val sales: StateFlow<List<Sale>> = _sales.asStateFlow()

    private val _stockAdjustments = MutableStateFlow<List<StockAdjustment>>(emptyList())
    val stockAdjustments: StateFlow<List<StockAdjustment>> = _stockAdjustments.asStateFlow()

    private val _isOnline = MutableStateFlow<Boolean>(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        initFirebase()
        // If demo mode or local starting state, pre-load realistic initial data
        if (_categories.value.isEmpty()) {
            loadInitialDemoData()
        }
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            firebaseAuth = FirebaseAuth.getInstance()
            firebaseDb = try {
                FirebaseDatabase.getInstance()
            } catch (e: Exception) {
                try {
                    FirebaseDatabase.getInstance("https://smartsales-7d571-default-rtdb.firebaseio.com")
                } catch (e2: Exception) {
                    FirebaseDatabase.getInstance("https://smartsales-7d571.firebaseio.com")
                }
            }
            isFirebaseAvailable = true
            _isOnline.value = true
            setupFirebaseListeners()
            checkCurrentAuthUser()
            Log.d(TAG, "Firebase initialized successfully with project smartsales-7d571")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase setup exception: ${e.message}. Operating in local demo mode.")
            isFirebaseAvailable = false
            _isOnline.value = false
        }
    }

    private fun checkCurrentAuthUser() {
        val auth = firebaseAuth ?: return
        val currentFbUser = auth.currentUser
        if (currentFbUser != null) {
            fetchUserProfile(currentFbUser.uid, currentFbUser.email ?: "")
        }
    }

    private fun fetchUserProfile(uid: String, email: String, onRoleLoaded: ((String?) -> Unit)? = null) {
        val db = firebaseDb
        if (db == null) {
            val localRole = findAccountRole(email) ?: (if (email.contains("admin", ignoreCase = true)) UserRole.ADMIN.value else UserRole.STAFF.value)
            onRoleLoaded?.invoke(localRole)
            return
        }
        db.getReference("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                    val role = snapshot.child("role").getValue(String::class.java) ?: UserRole.STAFF.value
                    val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                    val user = User(uid, name, email, role, createdAt)
                    _currentUser.value = user
                    saveUserSession(user)
                    onRoleLoaded?.invoke(role)
                } else {
                    val role = findAccountRole(email) ?: (if (email.contains("admin", ignoreCase = true)) UserRole.ADMIN.value else UserRole.STAFF.value)
                    val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    val user = User(uid, name, email, role, System.currentTimeMillis())
                    _currentUser.value = user
                    saveUserSession(user)
                    onRoleLoaded?.invoke(role)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "fetchUserProfile error: ${error.message}")
                val role = findAccountRole(email) ?: (if (email.contains("admin", ignoreCase = true)) UserRole.ADMIN.value else UserRole.STAFF.value)
                onRoleLoaded?.invoke(role)
            }
        })
    }

    private fun setupFirebaseListeners() {
        val db = firebaseDb ?: return

        // Categories listener
        db.getReference("categories").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Category>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: ""
                    list.add(Category(id = id, name = name))
                }
                if (list.isNotEmpty()) {
                    _categories.value = list.distinctBy { it.id }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Categories read failed: ${error.message}")
            }
        })

        // Products listener
        db.getReference("products").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Product>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    try {
                        val name = child.child("name").value?.toString() ?: ""
                        val categoryId = child.child("categoryId").value?.toString() ?: ""
                        val rawPrice = child.child("price").value
                        val price = when (rawPrice) {
                            is Number -> rawPrice.toDouble()
                            is String -> rawPrice.replace("$", "").trim().toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }.let { if (it.isNaN() || it.isInfinite() || it < 0.0) 0.0 else it }

                        val rawStock = child.child("stock").value
                        val stock = when (rawStock) {
                            is Number -> rawStock.toInt()
                            is String -> rawStock.trim().toIntOrNull() ?: 0
                            else -> 0
                        }.coerceAtLeast(0)

                        val rawLowStock = child.child("lowStockThreshold").value
                        val lowStockThreshold = when (rawLowStock) {
                            is Number -> rawLowStock.toInt()
                            is String -> rawLowStock.trim().toIntOrNull() ?: 5
                            else -> 5
                        }.coerceAtLeast(0)

                        val imageUrl = child.child("imageUrl").value?.toString() ?: ""
                        val supplier = child.child("supplier").value?.toString() ?: ""
                        list.add(
                            Product(
                                id = id,
                                name = name,
                                categoryId = categoryId,
                                price = price,
                                stock = stock,
                                lowStockThreshold = lowStockThreshold,
                                imageUrl = imageUrl,
                                supplier = supplier
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed parsing product child $id: ${e.message}")
                    }
                }
                if (list.isNotEmpty()) {
                    _products.value = list.distinctBy { it.id }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Products read failed: ${error.message}")
            }
        })

        // Customers listener
        db.getReference("customers").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Customer>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    try {
                        val name = child.child("name").value?.toString() ?: ""
                        val phone = child.child("phone").value?.toString() ?: ""
                        val email = child.child("email").value?.toString() ?: ""
                        val rawCreatedAt = child.child("createdAt").value
                        val createdAt = when (rawCreatedAt) {
                            is Number -> rawCreatedAt.toLong()
                            is String -> rawCreatedAt.trim().toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        list.add(Customer(id, name, phone, email, createdAt))
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed parsing customer child $id: ${e.message}")
                    }
                }
                if (list.isNotEmpty()) {
                    _customers.value = list.distinctBy { it.id }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Customers read failed: ${error.message}")
            }
        })

        // Sales listener
        db.getReference("sales").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Sale>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    try {
                        val customerId = child.child("customerId").value?.toString() ?: ""
                        val rawTotal = child.child("totalAmount").value
                        val totalAmount = when (rawTotal) {
                            is Number -> rawTotal.toDouble()
                            is String -> rawTotal.replace("$", "").trim().toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }.let { if (it.isNaN() || it.isInfinite() || it < 0.0) 0.0 else it }

                        val rawDate = child.child("saleDate").value
                        val saleDate = when (rawDate) {
                            is Number -> rawDate.toLong()
                            is String -> rawDate.trim().toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        val salesPersonId = child.child("salesPersonId").value?.toString() ?: ""

                        // Resolve customer name
                        val custName = _customers.value.find { it.id == customerId }?.name ?: "Customer"

                        val items = mutableListOf<SaleItem>()
                        for (itemSnap in child.child("items").children) {
                            try {
                                val prodId = itemSnap.child("productId").value?.toString() ?: ""
                                val prodName = itemSnap.child("productName").value?.toString()
                                    ?: (_products.value.find { it.id == prodId }?.name ?: "Item")
                                val rawQty = itemSnap.child("quantity").value
                                val quantity = when (rawQty) {
                                    is Number -> rawQty.toInt()
                                    is String -> rawQty.trim().toIntOrNull() ?: 1
                                    else -> 1
                                }.coerceAtLeast(1)

                                val rawUnitPrice = itemSnap.child("unitPrice").value
                                val unitPrice = when (rawUnitPrice) {
                                    is Number -> rawUnitPrice.toDouble()
                                    is String -> rawUnitPrice.replace("$", "").trim().toDoubleOrNull() ?: 0.0
                                    else -> 0.0
                                }.let { if (it.isNaN() || it.isInfinite() || it < 0.0) 0.0 else it }

                                val rawLineTotal = itemSnap.child("lineTotal").value
                                val lineTotal = when (rawLineTotal) {
                                    is Number -> rawLineTotal.toDouble()
                                    is String -> rawLineTotal.replace("$", "").trim().toDoubleOrNull() ?: (quantity * unitPrice)
                                    else -> quantity * unitPrice
                                }
                                items.add(SaleItem(prodId, prodName, quantity, unitPrice, lineTotal))
                            } catch (e: Exception) {
                                Log.w(TAG, "Error parsing sale item: ${e.message}")
                            }
                        }

                        list.add(
                            Sale(
                                id = id,
                                items = items,
                                customerId = customerId,
                                customerName = custName,
                                totalAmount = totalAmount,
                                saleDate = saleDate,
                                salesPersonId = salesPersonId,
                                salesPersonName = if (salesPersonId.contains("admin")) "Admin" else "Sales Staff"
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing sale $id: ${e.message}")
                    }
                }
                if (list.isNotEmpty()) {
                    _sales.value = list.distinctBy { it.id }.sortedByDescending { it.saleDate }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Sales read failed: ${error.message}")
            }
        })

        // Stock adjustments listener
        db.getReference("stock_adjustments").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<StockAdjustment>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    try {
                        val prodId = child.child("productId").value?.toString() ?: ""
                        val prodName = child.child("productName").value?.toString() ?: ""
                        val rawPrev = child.child("previousStock").value
                        val prevStock = when (rawPrev) {
                            is Number -> rawPrev.toInt()
                            is String -> rawPrev.trim().toIntOrNull() ?: 0
                            else -> 0
                        }
                        val rawNew = child.child("newStock").value
                        val newStock = when (rawNew) {
                            is Number -> rawNew.toInt()
                            is String -> rawNew.trim().toIntOrNull() ?: 0
                            else -> 0
                        }
                        val rawChange = child.child("changeAmount").value
                        val changeAmount = when (rawChange) {
                            is Number -> rawChange.toInt()
                            is String -> rawChange.trim().toIntOrNull() ?: 0
                            else -> 0
                        }
                        val reason = child.child("reason").value?.toString() ?: ""
                        val adjustedBy = child.child("adjustedBy").value?.toString() ?: ""
                        val rawTs = child.child("timestamp").value
                        val timestamp = when (rawTs) {
                            is Number -> rawTs.toLong()
                            is String -> rawTs.trim().toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        list.add(StockAdjustment(id, prodId, prodName, prevStock, newStock, changeAmount, reason, adjustedBy, timestamp))
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing stock adjustment $id: ${e.message}")
                    }
                }
                if (list.isNotEmpty()) {
                    _stockAdjustments.value = list.distinctBy { it.id }.sortedByDescending { it.timestamp }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Stock adjustments read failed: ${error.message}")
            }
        })
    }

    private fun loadInitialDemoData() {
        _categories.value = SampleDataSeeder.getDefaultCategories().distinctBy { it.id }
        _products.value = SampleDataSeeder.getDefaultProducts().distinctBy { it.id }
        _customers.value = SampleDataSeeder.getDefaultCustomers().distinctBy { it.id }
        _sales.value = SampleDataSeeder.getDefaultSales().distinctBy { it.id }
        _stockAdjustments.value = SampleDataSeeder.getDefaultStockAdjustments().distinctBy { it.id }
        // Default demo user: Admin
        _currentUser.value = User(
            id = "admin_demo",
            name = "Sarah Manager",
            email = "admin@smartsales.com",
            role = UserRole.ADMIN.value
        )
    }

    // ==========================================
    // AUTHENTICATION
    // ==========================================

    fun login(email: String, pass: String, onResult: (Result<User>) -> Unit) {
        val auth = firebaseAuth
        if (auth != null && isFirebaseAvailable) {
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: ""
                    fetchUserProfile(uid, email)
                    val user = User(uid, email.substringBefore("@"), email, "admin")
                    _currentUser.value = user
                    saveUserSession(user)
                    onResult(Result.success(user))
                }
                .addOnFailureListener { error ->
                    onResult(Result.failure(error))
                }
        } else {
            // Local fallback login
            val role = if (email.contains("staff", ignoreCase = true)) UserRole.STAFF else UserRole.ADMIN
            val user = User(
                id = if (role == UserRole.ADMIN) "admin_demo" else "staff_demo",
                name = if (role == UserRole.ADMIN) "Sarah Manager" else "Alex Staff",
                email = email,
                role = role.value
            )
            _currentUser.value = user
            saveUserSession(user)
            onResult(Result.success(user))
        }
    }

    fun register(name: String, email: String, pass: String, role: UserRole, onResult: (Result<User>) -> Unit) {
        val auth = firebaseAuth
        if (auth != null && isFirebaseAvailable) {
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: UUID.randomUUID().toString()
                    val user = User(uid, name, email, role.value, System.currentTimeMillis())
                    firebaseDb?.getReference("users")?.child(uid)?.setValue(
                        mapOf(
                            "name" to name,
                            "email" to email,
                            "role" to role.value,
                            "createdAt" to user.createdAt
                        )
                    )
                    _currentUser.value = user
                    saveUserSession(user)
                    onResult(Result.success(user))
                }
                .addOnFailureListener { error ->
                    onResult(Result.failure(error))
                }
        } else {
            val user = User(
                id = UUID.randomUUID().toString(),
                name = name,
                email = email,
                role = role.value,
                createdAt = System.currentTimeMillis()
            )
            _currentUser.value = user
            saveUserSession(user)
            onResult(Result.success(user))
        }
    }

    fun resetPassword(email: String, onResult: (Result<Unit>) -> Unit) {
        val auth = firebaseAuth
        if (auth != null && isFirebaseAvailable) {
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener { onResult(Result.success(Unit)) }
                .addOnFailureListener { onResult(Result.failure(it)) }
        } else {
            onResult(Result.success(Unit))
        }
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "logout error: ${e.message}")
        }
        saveUserSession(null)
        _currentUser.value = null
    }

    fun switchDemoRole(role: UserRole) {
        val updated = User(
            id = if (role == UserRole.ADMIN) "admin_demo" else "staff_demo",
            name = if (role == UserRole.ADMIN) "Sarah Manager" else "Alex Staff",
            email = if (role == UserRole.ADMIN) "admin@smartsales.com" else "staff@smartsales.com",
            role = role.value
        )
        _currentUser.value = updated
        saveUserSession(updated)
    }

    fun setCurrentUser(user: User?) {
        _currentUser.value = user
        saveUserSession(user)
    }

    // ==========================================
    // PRODUCTS CRUD (Admin Only for Write/Delete)
    // ==========================================

    fun addProduct(product: Product, onResult: (Result<Product>) -> Unit) {
        val id = if (product.id.isBlank()) "prod_${System.currentTimeMillis()}" else product.id
        val newProduct = product.copy(id = id)

        val currentList = _products.value.filterNot { it.id == newProduct.id }.toMutableList()
        currentList.add(0, newProduct)
        _products.value = currentList.distinctBy { it.id }

        val db = firebaseDb
        if (db != null && isFirebaseAvailable) {
            val data = mapOf(
                "name" to newProduct.name,
                "categoryId" to newProduct.categoryId,
                "price" to newProduct.price,
                "stock" to newProduct.stock,
                "lowStockThreshold" to newProduct.lowStockThreshold,
                "imageUrl" to newProduct.imageUrl,
                "supplier" to newProduct.supplier
            )
            try {
                db.getReference("products").child(id).setValue(data)
            } catch (e: Exception) {
                Log.w(TAG, "addProduct firebase error: ${e.message}")
            }
        }
        onResult(Result.success(newProduct))
    }

    fun updateProduct(product: Product, onResult: (Result<Product>) -> Unit) {
        val db = firebaseDb
        if (db != null && isFirebaseAvailable) {
            val data = mapOf(
                "name" to product.name,
                "categoryId" to product.categoryId,
                "price" to product.price,
                "stock" to product.stock,
                "lowStockThreshold" to product.lowStockThreshold,
                "imageUrl" to product.imageUrl,
                "supplier" to product.supplier
            )
            db.getReference("products").child(product.id).updateChildren(data)
                .addOnSuccessListener {
                    _products.value = _products.value.map { if (it.id == product.id) product else it }
                    onResult(Result.success(product))
                }
                .addOnFailureListener { onResult(Result.failure(it)) }
        } else {
            _products.value = _products.value.map { if (it.id == product.id) product else it }
            onResult(Result.success(product))
        }
    }

    fun deleteProduct(productId: String, onResult: (Result<Unit>) -> Unit) {
        val db = firebaseDb
        if (db != null && isFirebaseAvailable) {
            db.getReference("products").child(productId).removeValue()
                .addOnSuccessListener {
                    _products.value = _products.value.filterNot { it.id == productId }
                    onResult(Result.success(Unit))
                }
                .addOnFailureListener { onResult(Result.failure(it)) }
        } else {
            _products.value = _products.value.filterNot { it.id == productId }
            onResult(Result.success(Unit))
        }
    }

    // ==========================================
    // CATEGORIES CRUD (Admin Only)
    // ==========================================

    fun addCategory(name: String, onResult: (Result<Category>) -> Unit) {
        val id = "cat_${System.currentTimeMillis()}"
        val category = Category(id = id, name = name)

        val db = firebaseDb
        if (db != null && isFirebaseAvailable) {
            db.getReference("categories").child(id).setValue(mapOf("name" to name))
                .addOnSuccessListener {
                    _categories.value = (_categories.value.filterNot { it.id == category.id } + category).distinctBy { it.id }
                    onResult(Result.success(category))
                }
                .addOnFailureListener { onResult(Result.failure(it)) }
        } else {
            _categories.value = (_categories.value.filterNot { it.id == category.id } + category).distinctBy { it.id }
            onResult(Result.success(category))
        }
    }

    fun updateCategory(category: Category, onResult: (Result<Category>) -> Unit) {
        val db = firebaseDb
        if (db != null && isFirebaseAvailable) {
            db.getReference("categories").child(category.id).child("name").setValue(category.name)
                .addOnSuccessListener {
                    _categories.value = _categories.value.map { if (it.id == category.id) category else it }
                    onResult(Result.success(category))
                }
                .addOnFailureListener { onResult(Result.failure(it)) }
        } else {
            _categories.value = _categories.value.map { if (it.id == category.id) category else it }
            onResult(Result.success(category))
        }
    }

    fun deleteCategory(categoryId: String, onResult: (Result<Unit>) -> Unit) {
        // Safety check: Cannot delete category if products are assigned to it
        val assignedProducts = _products.value.count { it.categoryId == categoryId }
        if (assignedProducts > 0) {
            onResult(Result.failure(IllegalStateException("Cannot delete category: $assignedProducts product(s) are assigned to it. Please reassign them first.")))
            return
        }

        val db = firebaseDb
        if (db != null && isFirebaseAvailable) {
            db.getReference("categories").child(categoryId).removeValue()
                .addOnSuccessListener {
                    _categories.value = _categories.value.filterNot { it.id == categoryId }
                    onResult(Result.success(Unit))
                }
                .addOnFailureListener { onResult(Result.failure(it)) }
        } else {
            _categories.value = _categories.value.filterNot { it.id == categoryId }
            onResult(Result.success(Unit))
        }
    }

    // ==========================================
    // CUSTOMERS CRUD
    // ==========================================

    fun addCustomer(customer: Customer, onResult: (Result<Customer>) -> Unit) {
        val id = if (customer.id.isBlank()) "cust_${System.currentTimeMillis()}" else customer.id
        val newCustomer = customer.copy(id = id)

        val db = firebaseDb
        if (db != null && isFirebaseAvailable) {
            val data = mapOf(
                "name" to newCustomer.name,
                "phone" to newCustomer.phone,
                "email" to newCustomer.email,
                "createdAt" to newCustomer.createdAt
            )
            db.getReference("customers").child(id).setValue(data)
                .addOnSuccessListener {
                    _customers.value = (_customers.value.filterNot { it.id == newCustomer.id } + newCustomer).distinctBy { it.id }
                    onResult(Result.success(newCustomer))
                }
                .addOnFailureListener { onResult(Result.failure(it)) }
        } else {
            _customers.value = (_customers.value.filterNot { it.id == newCustomer.id } + newCustomer).distinctBy { it.id }
            onResult(Result.success(newCustomer))
        }
    }

    fun updateCustomer(customer: Customer, onResult: (Result<Customer>) -> Unit) {
        val db = firebaseDb
        if (db != null && isFirebaseAvailable) {
            val data = mapOf(
                "name" to customer.name,
                "phone" to customer.phone,
                "email" to customer.email
            )
            db.getReference("customers").child(customer.id).updateChildren(data)
                .addOnSuccessListener {
                    _customers.value = _customers.value.map { if (it.id == customer.id) customer else it }
                    onResult(Result.success(customer))
                }
                .addOnFailureListener { onResult(Result.failure(it)) }
        } else {
            _customers.value = _customers.value.map { if (it.id == customer.id) customer else it }
            onResult(Result.success(customer))
        }
    }

    // ==========================================
    // INVENTORY MANAGEMENT (Add Stock - Admin Only)
    // ==========================================

    fun addStock(productId: String, quantityToAdd: Int, reason: String = "Restock", onResult: (Result<StockAdjustment>) -> Unit) {
        val user = _currentUser.value
        if (user?.isAdmin() != true) {
            onResult(Result.failure(SecurityException("Access Denied: Only Admin can add stock.")))
            return
        }
        if (quantityToAdd <= 0) {
            onResult(Result.failure(IllegalArgumentException("Quantity to add must be greater than 0.")))
            return
        }

        val product = _products.value.find { it.id == productId }
        if (product == null) {
            onResult(Result.failure(IllegalArgumentException("Product not found.")))
            return
        }

        val prevStock = product.stock
        val newStock = prevStock + quantityToAdd
        val changeAmount = quantityToAdd
        val userName = user.name
        val adjId = "adj_${System.currentTimeMillis()}"
        val adjustment = StockAdjustment(
            id = adjId,
            productId = productId,
            productName = product.name,
            previousStock = prevStock,
            newStock = newStock,
            changeAmount = changeAmount,
            reason = reason,
            adjustedBy = userName,
            timestamp = System.currentTimeMillis()
        )

        // Immediately update local StateFlow
        _products.value = _products.value.map { if (it.id == productId) it.copy(stock = newStock) else it }.distinctBy { it.id }
        _stockAdjustments.value = (listOf(adjustment) + _stockAdjustments.value).distinctBy { it.id }

        val db = firebaseDb
        if (db != null && isFirebaseAvailable) {
            try {
                val stockRef = db.getReference("products").child(productId).child("stock")
                stockRef.setValue(newStock)
                db.getReference("stock_adjustments").child(adjId).setValue(
                    mapOf(
                        "productId" to productId,
                        "productName" to product.name,
                        "previousStock" to prevStock,
                        "newStock" to newStock,
                        "changeAmount" to changeAmount,
                        "reason" to reason,
                        "adjustedBy" to userName,
                        "timestamp" to adjustment.timestamp
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Firebase addStock exception: ${e.message}")
            }
        }
        onResult(Result.success(adjustment))
    }

    fun adjustStock(productId: String, newStock: Int, reason: String, onResult: (Result<StockAdjustment>) -> Unit) {
        val user = _currentUser.value
        if (user?.isAdmin() != true) {
            onResult(Result.failure(SecurityException("Access Denied: Only Admin can adjust stock.")))
            return
        }
        val product = _products.value.find { it.id == productId }
        if (product == null) {
            onResult(Result.failure(IllegalArgumentException("Product not found")))
            return
        }

        val prevStock = product.stock
        val changeAmount = newStock - prevStock
        val userName = user.name
        val adjId = "adj_${System.currentTimeMillis()}"
        val adjustment = StockAdjustment(
            id = adjId,
            productId = productId,
            productName = product.name,
            previousStock = prevStock,
            newStock = newStock,
            changeAmount = changeAmount,
            reason = reason,
            adjustedBy = userName,
            timestamp = System.currentTimeMillis()
        )

        _products.value = _products.value.map { if (it.id == productId) it.copy(stock = newStock) else it }.distinctBy { it.id }
        _stockAdjustments.value = (listOf(adjustment) + _stockAdjustments.value).distinctBy { it.id }

        val db = firebaseDb
        if (db != null && isFirebaseAvailable) {
            try {
                val stockRef = db.getReference("products").child(productId).child("stock")
                stockRef.setValue(newStock).addOnCompleteListener {
                    try {
                        db.getReference("stock_adjustments").child(adjId).setValue(
                            mapOf(
                                "productId" to productId,
                                "productName" to product.name,
                                "previousStock" to prevStock,
                                "newStock" to newStock,
                                "changeAmount" to changeAmount,
                                "reason" to reason,
                                "adjustedBy" to userName,
                                "timestamp" to adjustment.timestamp
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Audit log write error: ${e.message}")
                    }
                    onResult(Result.success(adjustment))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase adjustStock error: ${e.message}")
                onResult(Result.success(adjustment))
            }
        } else {
            onResult(Result.success(adjustment))
        }
    }

    // ==========================================
    // SALES CREATION WITH AUTOMATIC STOCK DECREMENT (Admin & Staff)
    // ==========================================

    /**
     * Records a sale for Admin and Staff, verifies stock availability,
     * automatically decrements stock from available inventory,
     * writes the sale and stock adjustments, and updates state immediately.
     */
    fun createSale(
        customerId: String,
        customerName: String,
        items: List<SaleItem>,
        onResult: (Result<Sale>) -> Unit
    ) {
        if (items.isEmpty()) {
            onResult(Result.failure(IllegalArgumentException("Cart is empty. Please add items to sale.")))
            return
        }

        // 1. Verify that quantity > 0 and does not exceed available stock
        for (item in items) {
            if (item.quantity <= 0) {
                onResult(Result.failure(IllegalArgumentException("Sales quantity for '${item.productName}' must be greater than 0.")))
                return
            }
            val prod = _products.value.find { it.id == item.productId }
            if (prod == null) {
                onResult(Result.failure(IllegalArgumentException("Product '${item.productName}' not found.")))
                return
            }
            if (prod.stock < item.quantity) {
                onResult(Result.failure(IllegalStateException("Insufficient stock for '${prod.name}'. Only ${prod.stock} units available, requested ${item.quantity}.")))
                return
            }
        }

        val totalAmount = items.sumOf { it.lineTotal }
        val saleId = "sale_${System.currentTimeMillis()}"
        val user = _currentUser.value
        val sale = Sale(
            id = saleId,
            items = items,
            customerId = customerId,
            customerName = customerName,
            totalAmount = totalAmount,
            saleDate = System.currentTimeMillis(),
            salesPersonId = user?.id ?: "staff_user",
            salesPersonName = user?.name ?: "Sales Staff"
        )

        // 2. Automatically deduct stock immediately in local state
        val updatedProducts = _products.value.map { prod ->
            val soldItem = items.find { it.productId == prod.id }
            if (soldItem != null) {
                prod.copy(stock = (prod.stock - soldItem.quantity).coerceAtLeast(0))
            } else {
                prod
            }
        }.distinctBy { it.id }
        _products.value = updatedProducts
        _sales.value = (listOf(sale) + _sales.value).distinctBy { it.id }

        // 3. Create stock adjustment audit entries
        val adjustmentEntries = items.map { item ->
            val prevStock = _products.value.find { it.id == item.productId }?.let { it.stock + item.quantity } ?: item.quantity
            val newStk = (prevStock - item.quantity).coerceAtLeast(0)
            StockAdjustment(
                id = "adj_${System.currentTimeMillis()}_${item.productId}",
                productId = item.productId,
                productName = item.productName,
                previousStock = prevStock,
                newStock = newStk,
                changeAmount = -item.quantity,
                reason = "Sale #${sale.id.takeLast(6)}",
                adjustedBy = user?.name ?: "Sales",
                timestamp = System.currentTimeMillis()
            )
        }
        _stockAdjustments.value = (adjustmentEntries + _stockAdjustments.value).distinctBy { it.id }

        // 4. Sync stock changes and sale to Firebase Realtime Database
        val db = firebaseDb
        if (db != null && isFirebaseAvailable) {
            try {
                // Update stock node for each sold item
                for (item in items) {
                    val remaining = updatedProducts.find { it.id == item.productId }?.stock ?: 0
                    db.getReference("products").child(item.productId).child("stock").setValue(remaining)
                }

                // Record sale record
                val saleData = mapOf(
                    "customerId" to sale.customerId,
                    "customerName" to sale.customerName,
                    "totalAmount" to sale.totalAmount,
                    "saleDate" to sale.saleDate,
                    "salesPersonId" to sale.salesPersonId,
                    "salesPersonName" to sale.salesPersonName,
                    "items" to items.map {
                        mapOf(
                            "productId" to it.productId,
                            "productName" to it.productName,
                            "quantity" to it.quantity,
                            "unitPrice" to it.unitPrice,
                            "lineTotal" to it.lineTotal
                        )
                    }
                )
                db.getReference("sales").child(saleId).setValue(saleData)
            } catch (e: Exception) {
                Log.w(TAG, "Firebase sync sale exception: ${e.message}")
            }
        }
        onResult(Result.success(sale))
    }

    // ==========================================
    // SEED SAMPLE DATA (For Live Demo)
    // ==========================================

    fun seedDemoDataToFirebase(onResult: (Result<Unit>) -> Unit) {
        val db = firebaseDb
        if (db != null && isFirebaseAvailable) {
            val categories = SampleDataSeeder.getDefaultCategories()
            val products = SampleDataSeeder.getDefaultProducts()
            val customers = SampleDataSeeder.getDefaultCustomers()
            val sales = SampleDataSeeder.getDefaultSales()

            for (cat in categories) {
                db.getReference("categories").child(cat.id).setValue(mapOf("name" to cat.name))
            }
            for (prod in products) {
                db.getReference("products").child(prod.id).setValue(
                    mapOf(
                        "name" to prod.name,
                        "categoryId" to prod.categoryId,
                        "price" to prod.price,
                        "stock" to prod.stock,
                        "lowStockThreshold" to prod.lowStockThreshold,
                        "imageUrl" to prod.imageUrl,
                        "supplier" to prod.supplier
                    )
                )
            }
            for (cust in customers) {
                db.getReference("customers").child(cust.id).setValue(
                    mapOf(
                        "name" to cust.name,
                        "phone" to cust.phone,
                        "email" to cust.email,
                        "createdAt" to cust.createdAt
                    )
                )
            }
            for (sale in sales) {
                db.getReference("sales").child(sale.id).setValue(
                    mapOf(
                        "customerId" to sale.customerId,
                        "totalAmount" to sale.totalAmount,
                        "saleDate" to sale.saleDate,
                        "salesPersonId" to sale.salesPersonId,
                        "items" to sale.items.map {
                            mapOf(
                                "productId" to it.productId,
                                "productName" to it.productName,
                                "quantity" to it.quantity,
                                "unitPrice" to it.unitPrice,
                                "lineTotal" to it.lineTotal
                            )
                        }
                    )
                )
            }
        }
        loadInitialDemoData()
        onResult(Result.success(Unit))
    }
}
