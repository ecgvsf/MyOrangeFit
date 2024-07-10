package com.example.myorangefit

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myorangefit.databinding.ActivityAddWorkoutTypeBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

const val IMAGE_PICK_CODE = 1

class AddWorkoutTypeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddWorkoutTypeBinding
    private lateinit var databaseHelper: DatabaseHelper
    private var imagePath: String? = null  // Percorso dell'immagine selezionata
    private var imageUri: Uri? = null       // Uri dell'immagine selezionata

    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onActivityResult(IMAGE_PICK_CODE, result.resultCode, result.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityManager.add(this)
        binding = ActivityAddWorkoutTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelperSingleton.getInstance(this)

        val bodyPart = intent.getStringExtra("bodyPart").orEmpty()
        binding.bodyPart.text = bodyPart

        binding.imageCard.setOnClickListener {
            val galleryIntent = Intent()
            galleryIntent.action = Intent.ACTION_GET_CONTENT
            galleryIntent.type = "image/*"
            startActivityForResult(galleryIntent, IMAGE_PICK_CODE)
        }

        binding.saveButton.setOnClickListener {
            val bodyPartId = databaseHelper.getBodyPart(bodyPart)

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

            databaseHelper.insertWorkout(bodyPartId, workoutName, type, imagePath!!)
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
                    imagePath = saveImageToAppDirectory(bitmap, imageStream)
                    loadImageWithGlide(imagePath)
                }
            } catch (exception: IOException) {
                exception.printStackTrace()
            }

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

