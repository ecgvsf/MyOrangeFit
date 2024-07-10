package com.example.myorangefit

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Workout(
    val id: Int,
    val name: String,
    val bodyPart: String,
    val type: Int, //0 tempo, 1 peso
    val image: String
)


/*
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Identificatore univoco dell'allenamento (può essere generato automaticamente dal database)
    val date: String, // Data dell'allenamento nel formato "yyyy-MM-dd"
    val bodyPart: String, // Parte del corpo allenata (es. spalle, schiena, petto, braccia, gambe)
    val repetitions: Int, // Numero di ripetizioni eseguite durante l'allenamento
    val weight: Float, // Peso utilizzato durante l'allenamento (in kg)
    val notes: String?, // Descrizione dell'esercizio, opzionale
    val imagePath: String? // Percorso dell'immagine associata all'allenamento, opzionale
)

 */