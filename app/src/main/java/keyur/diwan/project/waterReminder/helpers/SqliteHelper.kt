package keyur.diwan.project.waterReminder.helpers

// Import ContentValues to store data for database operations (e.g., insert, update)
import android.content.ContentValues
// Import Context to access the app’s context for database creation
import android.content.Context
// Import Cursor to handle database query results
import android.database.Cursor
// Import SQLiteDatabase for direct database operations (e.g., raw queries)
import android.database.sqlite.SQLiteDatabase
// Import SQLiteOpenHelper as the base class for managing the SQLite database
import android.database.sqlite.SQLiteOpenHelper

// SqliteHelper class manages the app’s SQLite database for storing water intake statistics
class SqliteHelper(val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME, null,
    DATABASE_VERSION
) {
    // Companion object to define database constants (e.g., table name, column names)
    companion object {
        // Database version, incremented when the schema changes
        private val DATABASE_VERSION = 1
        // Database name ("Aqua")
        private val DATABASE_NAME = "Aqua"
        // Table name for storing water intake statistics
        private val TABLE_STATS = "stats"
        // Column for the primary key (auto-incremented ID)
        private val KEY_ID = "id"
        // Column for the date (unique, format: YYYY-MM-DD)
        private val KEY_DATE = "date"
        // Column for the current water intake (in ml)
        private val KEY_INTOOK = "intook"
        // Column for the total intake goal (in ml)
        private val KEY_TOTAL_INTAKE = "totalintake"
    }

    // onCreate is called when the database is created for the first time
    override fun onCreate(db: SQLiteDatabase?) {
        // SQL query to create the stats table with columns: id, date (unique), intook, totalintake
        val CREATE_STATS_TABLE = ("CREATE TABLE " + TABLE_STATS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_DATE + " TEXT UNIQUE,"
                + KEY_INTOOK + " INT," + KEY_TOTAL_INTAKE + " INT" + ")")
        // Execute the query to create the table
        db?.execSQL(CREATE_STATS_TABLE)
    }

    // onUpgrade is called when the database version is incremented
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Drop the existing stats table if it exists
        db!!.execSQL("DROP TABLE IF EXISTS " + TABLE_STATS)
        // Recreate the table with the new schema
        onCreate(db)
    }

    // Function to add a new entry for a given date if it doesn’t already exist
    fun addAll(date: String, intook: Int, totalintake: Int): Long {
        // Check if an entry for the given date already exists
        if (checkExistance(date) == 0) {
            // Create ContentValues to store the data
            val values = ContentValues()
            // Add the date to the values
            values.put(KEY_DATE, date)
            // Add the initial intake (usually 0) to the values
            values.put(KEY_INTOOK, intook)
            // Add the total intake goal to the values
            values.put(KEY_TOTAL_INTAKE, totalintake)
            // Get a writable database instance
            val db = this.writableDatabase
            // Insert the new entry into the stats table
            val response = db.insert(TABLE_STATS, null, values)
            // Close the database connection
            db.close()
            // Return the result of the insert operation (row ID or -1 if failed)
            return response
            // Called by MainActivity.kt in onStart to initialize the database for the current date
        }
        // If the date already exists, return -1 to indicate no insertion was performed
        return -1
    }

    // Function to retrieve the current water intake for a given date
    fun getIntook(date: String): Int {
        // SQL query to select the intook column for the given date
        val selectQuery = "SELECT $KEY_INTOOK FROM $TABLE_STATS WHERE $KEY_DATE = ?"
        // Get a readable database instance
        val db = this.readableDatabase
        // Execute the query with the date as a parameter, using a try-with-resources block
        db.rawQuery(selectQuery, arrayOf(date)).use {
            // Check if the query returned a result
            if (it.moveToFirst()) {
                // Get the column index of KEY_INTOOK
                val index = it.getColumnIndexOrThrow(KEY_INTOOK)
                // Return the intake value from the column
                return it.getInt(index)
            }
        }
        // If no entry is found for the date, return 0
        return 0
        // Called by MainActivity.kt in updateValues to get the current intake
        // Also called by NotificationHelper.kt to calculate intake percentage for notifications
    }

    // Function to add a specified amount to the current intake for a given date
    fun addIntook(date: String, selectedOption: Int): Int {
        // Get the current intake for the date
        val intook = getIntook(date)
        // Get a writable database instance
        val db = this.writableDatabase
        // Create ContentValues to update the intake
        val contentValues = ContentValues()
        // Calculate the new intake by adding the selected amount
        contentValues.put(KEY_INTOOK, intook + selectedOption)

        // Update the stats table with the new intake value for the given date
        val response = db.update(TABLE_STATS, contentValues, "$KEY_DATE = ?", arrayOf(date))
        // Close the database connection
        db.close()
        // Return the number of rows affected (should be 1 if successful)
        return response
        // Called by MainActivity.kt in the fabAdd click listener to add water intake
    }

    // Function to check if an entry exists for a given date
    fun checkExistance(date: String): Int {
        // SQL query to select the intook column for the given date
        val selectQuery = "SELECT $KEY_INTOOK FROM $TABLE_STATS WHERE $KEY_DATE = ?"
        // Get a readable database instance
        val db = this.readableDatabase
        // Execute the query with the date as a parameter, using a try-with-resources block
        db.rawQuery(selectQuery, arrayOf(date)).use {
            // Check if the query returned a result
            if (it.moveToFirst()) {
                // Return the count of rows (1 if the date exists, 0 otherwise)
                return it.count
            }
        }
        // If no entry is found, return 0
        return 0
        // Called by addAll to prevent duplicate entries for the same date
    }

    // Function to retrieve all entries from the stats table
    fun getAllStats(): Cursor {
        // SQL query to select all columns from the stats table
        val selectQuery = "SELECT * FROM $TABLE_STATS"
        // Get a readable database instance
        val db = this.readableDatabase
        // Execute the query and return the Cursor containing the results
        return db.rawQuery(selectQuery, null)
        // Called by StatsActivity.kt to display water intake statistics
    }

    // Function to update the total intake goal for a given date
    fun updateTotalIntake(date: String, totalintake: Int): Int {
        // Get the current intake for the date (not used, but ensures the entry exists)
        val intook = getIntook(date)
        // Get a writable database instance
        val db = this.writableDatabase
        // Create ContentValues to update the total intake
        val contentValues = ContentValues()
        // Set the new total intake value
        contentValues.put(KEY_TOTAL_INTAKE, totalintake)

        // Update the stats table with the new total intake for the given date
        val response = db.update(TABLE_STATS, contentValues, "$KEY_DATE = ?", arrayOf(date))
        // Close the database connection
        db.close()
        // Return the number of rows affected (should be 1 if successful)
        return response
        // Called by other parts of the app (e.g., InitUserInfoActivity.kt) to update the total intake
    }

    // Function to reset the water intake to 0 for a given date
    fun resetIntook(date: String): Int {
        // Get a writable database instance
        val db = this.writableDatabase
        // Create ContentValues to set the intake to 0
        val contentValues = ContentValues()
        // Set the intook column to 0
        contentValues.put(KEY_INTOOK, 0)

        // Update the stats table to set the intake to 0 for the given date
        val response = db.update(TABLE_STATS, contentValues, "$KEY_DATE = ?", arrayOf(date))
        // Close the database connection
        db.close()
        // Return the number of rows affected (should be 1 if successful)
        return response
        // Called by MainActivity.kt in the fabAdd click listener when intake exceeds total intake
    }
}