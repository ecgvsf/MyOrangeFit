package com.example.myorangefit

import android.content.ContentValues

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE BodyPart (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE);")
        db.execSQL("CREATE TABLE Workout (id INTEGER PRIMARY KEY AUTOINCREMENT, body_part_id INTEGER NOT NULL, name TEXT NOT NULL, type INTEGER NOT NULL, image TEXT NOT NULL, FOREIGN KEY(body_part_id) REFERENCES BodyPart(id));")
        db.execSQL("CREATE TABLE WorkoutCalendar (id_workout INTEGER NOT NULL, date TEXT NOT NULL, notes TEXT, rep INTEGER NOT NULL, series INTEGER NOT NULL, peso INTEGER, tempo TEXT, PRIMARY KEY(id_workout, date), FOREIGN KEY(id_workout) REFERENCES Workout(id));")

        val bodyParts = listOf("Chest", "Back", "Legs", "Arms", "Shoulders", "Abs")
        bodyParts.forEach {
            db.execSQL("INSERT INTO BodyPart (name) VALUES ('$it');")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS WorkoutCalendar")
        db.execSQL("DROP TABLE IF EXISTS Workout")
        db.execSQL("DROP TABLE IF EXISTS BodyPart")
        onCreate(db)
    }

    fun getBodyPart(bodypart: String): Int {
        val db = readableDatabase
        //PRENDO L'ID DELLA PARTE DEL CORPO
        var cursor = db.query(
            "BodyPart",
            arrayOf("id"),
            "name = ?",
            arrayOf(bodypart),
            null, null, null
        )

        var bodyPartId: Int = -1
        if (cursor.moveToFirst()) {
            bodyPartId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
        }

        cursor.close()
        return bodyPartId
    }

    fun getAllBodyParts(): List<BodyPart> {
        val bodyParts = mutableListOf<BodyPart>()
        val db = readableDatabase
        val cursor = db.query("BodyPart", arrayOf("id", "name"), null, null, null, null, null)
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            bodyParts.add(BodyPart(id, name))
        }
        cursor.close()
        return bodyParts
    }

    fun getAllWorkouts(): List<Workout> {
        val workouts = mutableListOf<Workout>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT Workout.id, Workout.name, Workout.image, Workout.type, BodyPart.name AS bodyPart FROM Workout INNER JOIN BodyPart ON Workout.body_part_id = BodyPart.id", null)
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val image = cursor.getString(cursor.getColumnIndexOrThrow("image"))
            val type = cursor.getInt(cursor.getColumnIndexOrThrow("type"))
            val bodyPart = cursor.getString(cursor.getColumnIndexOrThrow("bodyPart"))
            workouts.add(Workout(id, name, bodyPart, type, image ))
        }
        cursor.close()
        return workouts
    }

    fun getAllWorkoutsByBodyPart(): Map<String, List<Workout>> {
        val workoutsByBodyPart = mutableMapOf<String, List<Workout>>()
        val bodyParts = getAllBodyParts()
        val db = readableDatabase
        bodyParts.forEach { bodyPart ->
            val workouts = mutableListOf<Workout>()
            val cursor = db.rawQuery("SELECT Workout.id, Workout.name, Workout.image, Workout.type FROM Workout WHERE body_part_id = ?", arrayOf(bodyPart.id.toString()))
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val image = cursor.getString(cursor.getColumnIndexOrThrow("image"))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow("type"))
                workouts.add(Workout(id, name, bodyPart.name, type, image))
            }
            cursor.close()
            workoutsByBodyPart[bodyPart.name] = workouts
        }
        return workoutsByBodyPart
    }

    fun getWorkoutsByBodyPart(bodypart: String): List<Workout> {
        val db = readableDatabase
        val workouts = mutableListOf<Workout>()
        //PRENDO L'ID DELLA PARTE DEL CORPO
        var cursor = db.query(
            "BodyPart",
            arrayOf("id"),
            "name = ?",
            arrayOf(bodypart),
            null, null, null
        )

        var bodyPartId: Int? = null
        if (cursor.moveToFirst()) {
            bodyPartId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
        }

        cursor = db.query(
            "Workout",
            arrayOf("id", "body_part_id", "name", "type", "image"),
            "body_part_id = ?",
            arrayOf(bodyPartId.toString()), // Sostituzione del segnaposto "?" con il valore di 'bodyPartId'
            null, null, null
        )

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val image = cursor.getString(cursor.getColumnIndexOrThrow("image"))
            val type = cursor.getInt(cursor.getColumnIndexOrThrow("type"))
            workouts.add(Workout(id, bodypart, name, type, image))
        }

        cursor.close()
        return workouts
    }

    fun insertWorkout(bodyPartId: Int, name: String, type: Int, imagePath: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("body_part_id", bodyPartId)
                put("name", name)
                put("type", type)
                put("image", imagePath)
            }
            val result = db.insert("Workout", null, values)
            if (result != -1L) {
                db.setTransactionSuccessful() // Segna la transazione come riuscita
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction() // Completa la transazione
        }
    }

    fun insertWorkoutInCalendar(workoutId: Int, date: String, notes: String?, rep: Int, series: Int, peso: Int, tempo: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id_workout", workoutId)
            put("date", date)
            put("notes", notes)
            put("rep", rep)
            put("series", series)
            put("peso", peso)
            put("tempo", tempo)
        }
        db.insert("WorkoutCalendar", null, values)
    }

    // Metodo per eliminare un esercizio dal database
    fun deleteWorkout(workoutId: Int): Boolean {
        val db = writableDatabase
        val deletedRows = db.delete("workout", "id=?", arrayOf(workoutId.toString()))
        db.close()
        return deletedRows > 0
    }

    // Metodo per eseguire il backup del database
    fun backupDatabase(context: Context): Boolean {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val backupFile = File(Environment.getExternalStorageDirectory(), "workout_backup.db")

        return try {
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }


    companion object {
        private const val DATABASE_NAME = "workout.db"
        private const val DATABASE_VERSION = 1
    }
}

