package com.example.data.model

data class ContactItem(
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val initials: String = ""
)
