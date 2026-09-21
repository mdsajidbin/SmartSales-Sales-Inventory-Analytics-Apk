package com.example.model

enum class UserRole(val value: String, val displayName: String) {
    ADMIN("admin", "Admin"),
    STAFF("staff", "Sales Staff");

    companion object {
        fun fromString(role: String?): UserRole {
            return if (role.equals("admin", ignoreCase = true)) ADMIN else STAFF
        }
    }
}

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = UserRole.STAFF.value,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isAdmin(): Boolean = role.equals(UserRole.ADMIN.value, ignoreCase = true)
    fun isStaff(): Boolean = !isAdmin()
}

data class UserAccount(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val role: String = UserRole.STAFF.value,
    val createdAt: Long = System.currentTimeMillis()
)
