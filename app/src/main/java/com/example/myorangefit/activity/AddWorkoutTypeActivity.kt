package com.example.myorangefit.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myorangefit.R
import com.example.myorangefit.database.DatabaseHelper
import com.example.myorangefit.database.DatabaseHelperSingleton
import com.example.myorangefit.databinding.ActivityAddWorkoutTypeBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

const val IMAGE_PICK_CODE = 1
const val BODY_PART_REQUEST_CODE = 2

class AddWorkoutTypeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddWorkoutTypeBinding
    private lateinit var databaseHelper: DatabaseHelper
    private var imagePath: String? = null  // Percorso dell'immagine selezionata
    private var previousImagePath: String? = null  // Percorso dell'immagine precedente
    private var bodyPart: String? = null  // Percorso dell'immagine precedente

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityManager.add(this)
        binding = ActivityAddWorkoutTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelperSingleton.getInstance(this)

        val editFlag = intent.getIntExtra("editFlag", -1)
        val id = intent.getIntExtra("id", -1)
        if (editFlag != -1) {
            val workout = databaseHelper.getWorkoutById(id)

            binding.change.visibility = View.VISIBLE
            if (workout != null) {
                binding.nameEditText.setText(workout.name)
                if (workout.type == 0)
                    binding.radio.check(R.id.time_button)
                if (workout.type == 1)
                    binding.radio.check(R.id.weight_button)
                loadImageWithGlide(workout.image)

                imagePath = workout.image // Salva il percoso dell'immagine
                previousImagePath = workout.image // Salva il percorso dell'immagine precedente
            }
        }

        bodyPart = intent.getStringExtra("bodyPart").orEmpty()
        binding.bodyPart.text = bodyPart

        binding.imageCard.setOnClickListener {
            val galleryIntent = Intent()
            galleryIntent.action = Intent.ACTION_GET_CONTENT
            galleryIntent.type = "image/*"
            startActivityForResult(galleryIntent, IMAGE_PICK_CODE)
        }

        binding.change.setOnClickListener {
            val intent = Intent(this, BodyPartActivity::class.java)
            intent.putExtra("flag", 2)
            startActivityForResult(intent, BODY_PART_REQUEST_CODE)
        }

        binding.saveButton.setOnClickListener {
            val bodyPartId = databaseHelper.getBodyPart(bodyPart!!)

            val workoutName = binding.nameEditText.text.toString()
            val type = when {
                binding.timeButton.isChecked -> 0
                binding.weightButton.isChecked -> 1
                else -> -1
            }

            // Controlla che tutti i campi siano stati inseriti correttamente
            if (workoutName.isEmpty()) {
                Toast.makeText(this, "Inserisci un nome per l'esercizio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (type == -1) {
                Toast.makeText(this, "Seleziona un tipo di esercizio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("AddWorkoutTypeActivity", "Body Part: $bodyPart e Id: $bodyPart")
            Log.d("AddWorkoutTypeActivity", "Image path: $imagePath")
            if (imagePath == null)
                imagePath = ""
            Log.d("AddWorkoutTypeActivity", "Image path: $imagePath")

            if (editFlag != -1) {

                Log.d("imageP", "$imagePath")
                Log.d("imageP", "$previousImagePath")
                // Aggiorna l'esercizio nel database
                databaseHelper.updateWorkout(id, bodyPartId, workoutName, type, imagePath!!)
                // Elimina l'immagine precedente se è stata cambiata
                if (previousImagePath != null && previousImagePath != imagePath) {
                    deleteImageFile(previousImagePath!!)
                }
            } else {
                databaseHelper.insertWorkout(bodyPartId, workoutName, type, imagePath!!)
            }
            ActivityManager.finishAll()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            val selectedImage = data.data
            try {
                val imageStream: InputStream? = contentResolver?.openInputStream(selectedImage!!)
                val bitmap = BitmapFactory.decodeStream(imageStream)
                if (imageStream != null) {
                    // Elimina l'immagine precedente se esiste e se è stata caricata una nuova immagine
                    if (previousImagePath != null) {
                        deleteImageFile(previousImagePath!!)
                        previousImagePath = null // Resetta il percorso dell'immagine precedente
                    }
                    imagePath = saveImageToAppDirectory(bitmap, imageStream)
                    loadImageWithGlide(imagePath)
                }
            } catch (exception: IOException) {
                exception.printStackTrace()
            }
        } else if (requestCode == BODY_PART_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val selectedBodyPart = data.getStringExtra("bodyPart").orEmpty()
            binding.bodyPart.text = selectedBodyPart
            bodyPart = selectedBodyPart
        }

    }

    private fun saveImageToAppDirectory(bitmap: Bitmap, inputStream: InputStream): String {
        val imageFileName = "IMG_${System.currentTimeMillis()}.jpg"
        val storageDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "MyAppImages")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val imageFile = File(storageDir, imageFileName)
        try {
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream) // Salva l'immagine nel file
            outputStream.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return imageFile.absolutePath
    }

    private fun deleteImageFile(path: String) {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun loadImageWithGlide(imagePath: String?) {
        Glide.with(this)
            .load(imagePath)
            .centerCrop()
            .into(binding.image)
        val layoutParams = binding.image.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.setMargins(0, 0, 0, 0)
        binding.image.layoutParams = layoutParams
        binding.image.setColorFilter(Color.TRANSPARENT)
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.remove(this)
    }
}
