package com.example.spendwise.domain.model

data class Currency(
    val code: String,
    val symbol: String,
    val name: String
) {
    companion object {
        val INR = Currency("INR", "₹", "Indian Rupee")
        val USD = Currency("USD", "$", "US Dollar")
        val EUR = Currency("EUR", "€", "Euro")
        val GBP = Currency("GBP", "£", "British Pound")
        val JPY = Currency("JPY", "¥", "Japanese Yen")
        val CAD = Currency("CAD", "CA$", "Canadian Dollar")
        val AUD = Currency("AUD", "AU$", "Australian Dollar")

        val ALL = listOf(INR, USD, EUR, GBP, JPY, CAD, AUD)

        fun fromCode(code: String): Currency {
            return ALL.find { it.code.equals(code, ignoreCase = true) } ?: INR
        }
    }
}
