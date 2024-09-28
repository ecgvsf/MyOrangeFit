package com.example.myorangefit.model

import android.os.Parcel
import android.os.Parcelable

data class Workout(
    val id: Int,
    var name: String,
    val bodyPart: String,
    val type: Int, //0 tempo, 1 peso
    val image: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(bodyPart)
        parcel.writeInt(type)
        parcel.writeString(image)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Workout> {
        override fun createFromParcel(parcel: Parcel): Workout {
            return Workout(parcel)
        }

        override fun newArray(size: Int): Array<Workout?> {
            return arrayOfNulls(size)
        }
    }
}


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