package com.example.myorangefit.async

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myorangefit.database.DatabaseHelper
import com.example.myorangefit.model.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class WorkoutViewModel(private val databaseHelper: DatabaseHelper) : ViewModel() {

    private val _workoutsForDate = MutableLiveData<Map<LocalDate, MutableList<Workout>>>()
    val workoutsForDate: LiveData<Map<LocalDate, MutableList<Workout>>> get() = _workoutsForDate

    private val _allWorkouts = MutableLiveData<MutableMap<LocalDate, MutableSet<String>>>()
    val allWorkouts: LiveData<MutableMap<LocalDate, MutableSet<String>>> get() = _allWorkouts

    private val _streakWorkout = MutableLiveData(0)
    val streakWorkout get() = _streakWorkout

    private val _weekWorkout = MutableLiveData(0)
    val weekWorkout get() = _weekWorkout

    private val _weekDatesList = MutableLiveData<MutableList<String>>()
    val weekDatesList get() = _weekDatesList

    private val _selectedDate = MutableLiveData(LocalDate.now())
    val selectedData get() = _selectedDate

    private val _nItems = MutableLiveData(0)
    val nItems get() = _nItems

    fun getWeekDates(today: LocalDate) {
        viewModelScope.launch(Dispatchers.IO){
            val list = databaseHelper.getWeekWorkout(today)
            withContext(Dispatchers.Main) {
                _weekDatesList.value = list
            }
        }
    }

    fun setNItems(num: Int) {
        viewModelScope.launch(Dispatchers.IO){
            withContext(Dispatchers.Main) {
                _nItems.value = num
            }
        }
    }

    fun updateData(data: LocalDate){
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _selectedDate.value = data
            }
        }
    }

    fun getStreak() {
        viewModelScope.launch(Dispatchers.IO) {
            val streak = databaseHelper.getStreak(LocalDate.now())
            withContext(Dispatchers.Main) {
                _streakWorkout.value = streak
            }
        }
    }

    fun getWeekWorkout() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = databaseHelper.getConutWeekWorkout(LocalDate.now())
            withContext(Dispatchers.Main) {
                _weekWorkout.value = count
            }
        }
    }

    fun loadWorkoutsForDate(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            val workoutData = databaseHelper.getWorkoutsIdForDate(date.toString()).mapNotNull{ id ->
                databaseHelper.getWorkoutById(id)
            }.toMutableList()
            val workoutsForSpecificDate = mapOf(date to workoutData)
            withContext(Dispatchers.Main) {
                _workoutsForDate.value = workoutsForSpecificDate
            }
        }
    }


    fun reloadWorkoutsForDate(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentWorkouts = _workoutsForDate.value ?: emptyMap()

            // Verifica se la data è presente nella mappa
            if (!currentWorkouts.containsKey(date)) {
                // Recupera i nuovi allenamenti per la data specifica
                val workoutData = databaseHelper.getWorkoutsIdForDate(date.toString()).mapNotNull { id ->
                    databaseHelper.getWorkoutById(id)
                }.toMutableList()

                // Crea una nuova mappa aggiornando solo il valore per la data specifica
                val updatedWorkouts = currentWorkouts.toMutableMap().apply {
                    this[date] = workoutData
                }

                withContext(Dispatchers.Main) {
                    _workoutsForDate.value = updatedWorkouts
                }
            } else {
                withContext(Dispatchers.Main) {
                    _workoutsForDate.value = currentWorkouts
                }
            }
        }
    }

    fun loadAllWorkouts() {
        viewModelScope.launch(Dispatchers.IO) {
            val workoutCalendar = databaseHelper.getAllWorkoutCalendar()
            val dateBodyPartMap = mutableMapOf<LocalDate, MutableSet<String>>()

            for (w in workoutCalendar) {
                val workout = databaseHelper.getWorkoutById(w.idWorkout)
                val date = LocalDate.parse(w.date)
                dateBodyPartMap.getOrPut(date) { mutableSetOf() }.add(workout?.bodyPart ?: "")
            }

            withContext(Dispatchers.Main) {
                _allWorkouts.value = dateBodyPartMap
            }
        }
    }


    fun reloadDayAllWorkout(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            // Ottieni gli ID degli allenamenti per la data specificata
            val workoutIdsForDate = databaseHelper.getWorkoutsIdForDate(date.toString())

            // Crea una nuova mappa per la data specifica
            val updatedBodyPartsForDate = workoutIdsForDate.mapNotNull { id ->
                databaseHelper.getWorkoutById(id)?.bodyPart // Ottieni il bodyPart dell'allenamento
            }.toMutableSet() // Utilizziamo un MutableSet per evitare duplicati


            withContext(Dispatchers.Main) {
                // Aggiorna solo l'entry corrispondente alla data nella mappa allWorkouts
                val currentMap = _allWorkouts.value?.toMutableMap() ?: mutableMapOf()

                // Modifica o aggiungi la voce per quella specifica data
                currentMap[date] = updatedBodyPartsForDate

                // Imposta il nuovo valore nella MutableLiveData
                _allWorkouts.value = currentMap
            }
        }
    }
}