package com.example.spendwise.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.spendwise.data.local.dao.BudgetDao
import com.example.spendwise.data.local.dao.CategoryDao
import com.example.spendwise.data.local.dao.PersonalExpenseDao
import com.example.spendwise.data.local.entity.Category
import com.example.spendwise.data.local.entity.MonthlyBudget
import com.example.spendwise.data.local.entity.PersonalExpense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [PersonalExpense::class, Category::class, MonthlyBudget::class],
    version = 1,
    exportSchema = false
)
abstract class SpendWiseDatabase : RoomDatabase() {

    abstract fun personalExpenseDao(): PersonalExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: SpendWiseDatabase? = null

        val DEFAULT_CATEGORIES = listOf(
            // Food categories
            Category(id = 1, name = "Food & Meals", icon = "🍔", isDefault = true, isActive = true, displayOrder = 1),
            Category(id = 2, name = "Dining Out", icon = "🍽️", isDefault = true, isActive = true, displayOrder = 2),
            Category(id = 3, name = "Groceries", icon = "🥦", isDefault = true, isActive = true, displayOrder = 3),
            Category(id = 4, name = "Coffee & Snacks", icon = "☕", isDefault = true, isActive = true, displayOrder = 4),
            Category(id = 5, name = "Fast Food", icon = "🍕", isDefault = true, isActive = true, displayOrder = 5),
            Category(id = 6, name = "Food Delivery", icon = "🥡", isDefault = true, isActive = true, displayOrder = 6),
            
            // Shopping categories
            Category(id = 7, name = "Shopping", icon = "🛍️", isDefault = true, isActive = true, displayOrder = 7),
            Category(id = 8, name = "Clothes & Wear", icon = "👗", isDefault = true, isActive = true, displayOrder = 8),
            Category(id = 9, name = "Electronics", icon = "💻", isDefault = true, isActive = true, displayOrder = 9),
            Category(id = 10, name = "Footwear", icon = "👟", isDefault = true, isActive = true, displayOrder = 10),
            Category(id = 11, name = "Home & Kitchen", icon = "🏠", isDefault = true, isActive = true, displayOrder = 11),
            Category(id = 12, name = "Beauty & Care", icon = "💄", isDefault = true, isActive = true, displayOrder = 12),
            
            // Travel & Bills
            Category(id = 13, name = "Commute & Transit", icon = "🚕", isDefault = true, isActive = true, displayOrder = 13),
            Category(id = 14, name = "Fuel & Petrol", icon = "⛽", isDefault = true, isActive = true, displayOrder = 14),
            Category(id = 15, name = "Bills & Utilities", icon = "🧾", isDefault = true, isActive = true, displayOrder = 15),
            Category(id = 16, name = "Entertainment", icon = "🎬", isDefault = true, isActive = true, displayOrder = 16),
            Category(id = 17, name = "Health & Medicine", icon = "💊", isDefault = true, isActive = true, displayOrder = 17),
            Category(id = 18, name = "Education & Books", icon = "📚", isDefault = true, isActive = true, displayOrder = 18),
            Category(id = 19, name = "Subscriptions", icon = "📱", isDefault = true, isActive = true, displayOrder = 19),
            Category(id = 20, name = "Personal Care", icon = "🧘", isDefault = true, isActive = true, displayOrder = 20),
            Category(id = 21, name = "Other", icon = "📦", isDefault = true, isActive = true, displayOrder = 21)
        )

        fun getInstance(context: Context): SpendWiseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpendWiseDatabase::class.java,
                    "spendwise_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default categories on DB creation
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).categoryDao().insertAll(DEFAULT_CATEGORIES)
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
