package com.example.myorangefit.database

import android.content.ContentValues

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import android.util.Log
import com.example.myorangefit.model.BodyPart
import com.example.myorangefit.model.Serie
import com.example.myorangefit.model.Workout
import com.example.myorangefit.model.WorkoutCalendar
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE BodyPart (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE);")
        db.execSQL("CREATE TABLE Workout (id INTEGER PRIMARY KEY AUTOINCREMENT, body_part_id INTEGER NOT NULL, name TEXT NOT NULL, type INTEGER NOT NULL, image TEXT NOT NULL, FOREIGN KEY(body_part_id) REFERENCES BodyPart(id));")
        db.execSQL("CREATE TABLE WorkoutCalendar (id_workout INTEGER NOT NULL, date DATE NOT NULL, notes TEXT, PRIMARY KEY(id_workout, date), FOREIGN KEY(id_workout) REFERENCES Workout(id));")
        db.execSQL("CREATE TABLE Series (id INTEGER PRIMARY KEY AUTOINCREMENT,workout_id INTEGER NOT NULL, date DATE NOT NULL, series_number INTEGER NOT NULL, rep INTEGER , peso INTEGER, tempo TEXT, FOREIGN KEY (workout_id) REFERENCES workoutcalendar(id));")

        val bodyParts = listOf("Chest", "Back", "Legs", "Arms", "Shoulders", "Abs")
        bodyParts.forEach {
            db.execSQL("INSERT INTO BodyPart (name) VALUES ('$it');")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS WorkoutCalendar")
        db.execSQL("DROP TABLE IF EXISTS Workout")
        db.execSQL("DROP TABLE IF EXISTS BodyPart")
        db.execSQL("DROP TABLE IF EXISTS Series")
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

    fun getWorkoutById(id: Int): Workout? {
        val db = readableDatabase

        val cursor = db.query(
            "Workout",
            arrayOf("body_part_id", "name", "type", "image"),
            "id = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                val bodyPartId = it.getString(it.getColumnIndexOrThrow("body_part_id"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val type = it.getInt(it.getColumnIndexOrThrow("type"))
                val image = it.getString(it.getColumnIndexOrThrow("image"))
                return Workout(id, name, bodyPartId, type, image)
            }
        }

        return null
    }


    fun getNameByIdWorkout(id: Int): String{
        val db = readableDatabase
        //PRENDO IL NOME DALL'ID
        var cursor = db.query(
            "Workout",
            arrayOf("name"),
            "id = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        var name: String = ""
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
        }

        cursor.close()
        return name
    }

    fun getTypeByIdWorkout(id: Int): Int {
        val db = readableDatabase
        val cursor = db.query(
            "Workout",
            arrayOf("type"),
            "id = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        var type = -1
        if (cursor.moveToFirst()) {
            type = cursor.getInt(cursor.getColumnIndexOrThrow("type"))
        }

        cursor.close()
        return type
    }

    fun getAllWorkoutCalendar(): List<WorkoutCalendar> {
        val workoutCalendarList = mutableListOf<WorkoutCalendar>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM WorkoutCalendar", null)
        while (cursor.moveToNext()) {
            val idWorkout = cursor.getInt(cursor.getColumnIndexOrThrow("id_workout"))
            val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
            val notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"))
            workoutCalendarList.add(WorkoutCalendar(idWorkout, date, notes))
        }
        cursor.close()
        return workoutCalendarList
    }

    fun getWorkoutsIdForDate(currentDay: String): List<Int> {
        val workoutIds = mutableListOf<Int>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id_workout FROM WorkoutCalendar WHERE date = ?",
            arrayOf(currentDay)
        )
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id_workout"))
            workoutIds.add(id)
        }
        cursor.close()


        return workoutIds
    }

    fun getWorkoutsCalendarByMonths(currentYear: String, currentMonth: String): List<WorkoutCalendar> {
        val db = readableDatabase
        val workoutCalendarList = mutableListOf<WorkoutCalendar>()
        val currentMonthFormatted = "%$currentYear-%$currentMonth%"

        // Esegui la query con le date calcolate
        val cursor = db.rawQuery(
            """
        SELECT * FROM WorkoutCalendar
        WHERE (date BETWEEN ? AND ?)
           /*OR (date BETWEEN ? AND ?)
           OR (date BETWEEN ? AND ?)*/;
        """, arrayOf(
                "$currentYear-${"%02d".format(currentMonth.toInt())}-01", // inizio mese corrente
                "$currentYear-${"%02d".format(currentMonth.toInt())}-31", // fine mese corrente
                /*"$previousYear-$previousMonthStart-01", // inizio mese precedente
                "$previousYear-$previousMonthEnd", // fine mese precedente
                "$nextYear-$nextMonthStart-01", // inizio mese successivo
                "$nextYear-$nextMonthEnd" // fine mese successivo*/
            )
        )

        while (cursor.moveToNext()) {
            val idWorkout = cursor.getInt(cursor.getColumnIndexOrThrow("id_workout"))
            val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
            val notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"))

            workoutCalendarList.add(WorkoutCalendar(idWorkout, date, notes))
        }
        cursor.close()
        return workoutCalendarList
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

    fun insertWorkoutInCalendar(workoutId: Int, date: String, notes: String?) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id_workout", workoutId)
            put("date", date)
            put("notes", notes)
        }
        db.insert("WorkoutCalendar", null, values)
    }

    fun insertSeries(workoutId: Int, date: String, series: Int, rep: Int?, peso: Float?, tempo: Int?) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("workout_id", workoutId)
            put("date", date)
            put("series_number", series)
            put("rep", rep)
            put("peso", peso)
            put("tempo", tempo)
        }
        db.insert("Series", null, values)
    }

    fun deleteWorkoutCalendar(id: Int, date: String): Boolean {
        val db = writableDatabase
        deleteSeries(id, date)
        val deletedRows = db.delete("WorkoutCalendar", "id_workout=? and date=?", arrayOf(id.toString(), date))
        return deletedRows > 0
    }

    private fun deleteSeries(id: Int, date: String): Boolean {
        val db = writableDatabase
        val deletedRows = db.delete("Series", "workout_id=? and date=?", arrayOf(id.toString(), date))
        return deletedRows > 0
    }

    // Metodo per eliminare un esercizio dal database
    fun deleteWorkout(workoutId: Int): Boolean {
        val db = writableDatabase
        val deletedRows = db.delete("workout", "id=?", arrayOf(workoutId.toString()))
        return deletedRows > 0
    }

    fun updateWorkout(id: Int, bodyPartId: Int, name: String, type: Int, imagePath: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("body_part_id", bodyPartId)
            put("name", name)
            put("type", type)
            put("image", imagePath)
        }

        val affectedRows = db.update(
            "Workout",
            values,
            "id = ?",
            arrayOf(id.toString())
        )

        return affectedRows > 0
    }

    fun getSeries(workoutId: Int, date: String): MutableList<Serie> {
        val db = readableDatabase
        val seriesList = mutableListOf<Serie>()
        val query = """
            SELECT rep, peso, tempo FROM Series
            WHERE workout_id = ? and date = ?
            ORDER by series_number
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(workoutId.toString(), date))

        while (cursor.moveToNext()) {
            val rep = cursor.getInt(cursor.getColumnIndexOrThrow("rep"))
            val peso = cursor.getInt(cursor.getColumnIndexOrThrow("peso"))
            val tempo = cursor.getInt(cursor.getColumnIndexOrThrow("tempo"))

            seriesList.add(Serie(rep, peso, tempo))
        }
        cursor.close()

        return seriesList
    }


    fun getSeriesByWorkoutnDate(workoutId: Int, date: LocalDate): Triple<Int, Int, Int> {
        val db = readableDatabase
        val formattedDate = date.toString() // Converti la data nel formato stringa

        val query = """
        SELECT COUNT(*), AVG(rep), AVG(peso)
        FROM Series
        WHERE workout_id = ? AND date = ?
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(workoutId.toString(), formattedDate))

        var count = 0
        var avgRep = 0
        var avgPeso = 0

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
            avgRep = cursor.getInt(1)
            avgPeso = cursor.getInt(2)
        }

        cursor.close()
        return Triple(count, avgRep, avgPeso)
    }

    fun getWeekWorkout(date: LocalDate): MutableList<String> {
        val db = readableDatabase
        var datesList = mutableListOf<String>()

        // Converte la data in stringa per usarla nella query
        val startOfWeek = date.minusDays(date.dayOfWeek.value.toLong() - 1).toString() // Inizio della settimana (lunedì)
        val endOfWeek = date.plusDays(7 - date.dayOfWeek.value.toLong()).toString() // Fine della settimana (domenica)

        val query = """
            SELECT DISTINCT date 
            FROM WorkoutCalendar wc
            WHERE date >= ? -- Inizio della settimana
              AND date <= ?; -- Fine della settimana
        """

        val cursor = db.rawQuery(query, arrayOf(startOfWeek, endOfWeek))
        while (cursor.moveToNext()) {
            val data = cursor.getString(cursor.getColumnIndexOrThrow("date"))
            datesList.add(data)
        }
        cursor.close()

        return datesList
    }

    // Metodo per constare gli allenamenti settimanali
    fun getConutWeekWorkout(date: LocalDate): Int {
        val db = readableDatabase
        var countWorkout = 0

        // Converte la data in stringa per usarla nella query
        val startOfWeek = date.minusDays(date.dayOfWeek.value.toLong() - 1).toString() // Inizio della settimana (lunedì)
        val endOfWeek = date.plusDays(7 - date.dayOfWeek.value.toLong()).toString() // Fine della settimana (domenica)

        val query = """
            SELECT COUNT(DISTINCT date) AS workout_count
            FROM WorkoutCalendar wc
            WHERE date >= ? -- Inizio della settimana
              AND date <= ?; -- Fine della settimana
        """

        val cursor = db.rawQuery(query, arrayOf(startOfWeek, endOfWeek))
        if (cursor.moveToFirst()) {
            countWorkout = cursor.getInt(cursor.getColumnIndexOrThrow("workout_count"))
        }
        cursor.close()

        return countWorkout
    }


    // Metodo per contare la streak più recente
    fun getStreak(date: LocalDate): Int {
        val db = readableDatabase
        var today = 0

        // Converte la data in stringa per usarla nella query
        val dateString = date.toString()

        // Query per vedere se oggi mi sono allenato
            var query = """
            SELECT COUNT(DISTINCT date) as c
            FROM WorkoutCalendar wc
            WHERE date = ?; -- Data passata
        """

        // Eseguo la query
        var cursor = db.rawQuery(query, arrayOf(dateString))

        // Recupero il risultato dalla query
        if (cursor.moveToFirst()) {
            today = cursor.getInt(cursor.getColumnIndexOrThrow("c"))
        }
        cursor.close()

        //controllo se la streak può iniziare (se "ieri" c'è un allenamento)
        val yesterday = date.minusDays(1).toString()

        // Verifica se ieri c'è stato un allenamento
        val checkYesterdayQuery = """
            SELECT COUNT(DISTINCT date) as c
            FROM WorkoutCalendar
            WHERE date = ?;
        """
        val yesterdayCursor = db.rawQuery(checkYesterdayQuery, arrayOf(yesterday))
        var yesterdayWorkout = if (yesterdayCursor.moveToFirst()) {
            yesterdayCursor.getInt(yesterdayCursor.getColumnIndexOrThrow("c"))
        } else {
            0
        }
        yesterdayCursor.close()

        if (yesterdayWorkout != 0) {
            // La query per contare la serie più recente di giorni consecutivi con workout
            query = """
                WITH RECURSIVE recent_streak AS (
                    SELECT date(?, '-1 day') AS date
                    UNION ALL
                    SELECT date(date, '-1 day')
                    FROM recent_streak
                    WHERE EXISTS (
                        SELECT 1 
                        FROM WorkoutCalendar wc
                        WHERE wc.date = date(recent_streak.date, '-1 day')
                    )
                )
                SELECT COUNT(*) AS consecutive_days_count
                FROM recent_streak
                WHERE date IN (SELECT date FROM WorkoutCalendar);
            """

            // Eseguo la query
            cursor = db.rawQuery(query, arrayOf(dateString))

            // Recupero il risultato dalla query
            if (cursor.moveToFirst()) {
                yesterdayWorkout =
                    cursor.getInt(cursor.getColumnIndexOrThrow("consecutive_days_count"))
            }

            cursor.close()
        }

        return yesterdayWorkout + today
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

