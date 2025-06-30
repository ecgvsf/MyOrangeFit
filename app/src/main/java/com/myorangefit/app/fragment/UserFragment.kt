package com.myorangefit.app.fragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.DatePicker
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import android.transition.Fade
import com.myorangefit.app.R
import com.myorangefit.app.async.WorkoutViewModel
import com.myorangefit.app.compose.MyButton
import com.myorangefit.app.compose.ScaleTheme
import com.myorangefit.app.compose.WeightPicker
import com.myorangefit.app.database.DatabaseHelper
import com.myorangefit.app.database.DatabaseHelperSingleton
import com.myorangefit.app.databinding.FragmentUserBinding
import com.google.android.material.textfield.TextInputEditText
import com.kevalpatel2106.rulerpicker.RulerValuePicker
import com.kevalpatel2106.rulerpicker.RulerValuePickerListener


class UserFragment : Fragment() {

    // ViewBinding
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!
    private lateinit var contx: Context

    private lateinit var viewModel: WorkoutViewModel
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Fade()
        exitTransition  = Fade()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragmentù
        contx = requireContext()
        databaseHelper = DatabaseHelperSingleton.getInstance(contx)
        viewModel = ViewModelProvider(requireActivity())[WorkoutViewModel::class.java]

        _binding = FragmentUserBinding.inflate(inflater,container,false)


        binding.profileName.setOnClickListener { showNameDialog() }
        binding.genderLayout.setOnClickListener { showSexDialog() }
        binding.heightLayout.setOnClickListener { showHeightDialog() }
        binding.weightLayout.setOnClickListener { showWeightDialog() }

        return binding.root
    }

    class WeightPickerDialog(
        private val initialWeight: Float,
        private val onConfirm: (Float) -> Unit
    ) : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_weight)

            dialog.findViewById<ComposeView>(R.id.composeView).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    ScaleTheme {
                        var weight by remember { mutableFloatStateOf(initialWeight) }
                        Column(
                            modifier = Modifier
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            WeightPicker(
                                initialWeight = weight,
                                onWeightChange = { weight = it }
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                MyButton(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .offset(y = 30.dp),
                                    text = "✘",
                                    onClick = {
                                        dismiss()
                                    }
                                )
                                Spacer(modifier = Modifier.width(32.dp))
                                MyButton(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .offset(y = 30.dp),
                                    text = "✔",
                                    onClick = {
                                        onConfirm(weight)
                                        dismiss()
                                    }
                                )
                            }

                        }
                    }
                }
            }

            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg)
            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
            dialog.window?.setGravity(Gravity.BOTTOM)

            return dialog
        }
    }


    private fun showWeightDialog() {
        val initWeight = getNum(binding.weightInfo.text.toString())
        var selectedWeight = initWeight
        val dialog = WeightPickerDialog(initWeight) { weight ->
            selectedWeight = weight
            binding.weightInfo.text = "${selectedWeight.toString()} kg"
        }
        dialog.show(parentFragmentManager, "weight_picker")
    }

    private fun getNum(s: String) : Float {
        val regex = """\d+([.,]\d+)?""".toRegex()
        val match = regex.find(s)
        val number = match?.value?.replace(',', '.')?.toFloat() ?: 0f
        return number
    }

    private fun showHeightDialog() {
        val dialog = Dialog(contx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_height)

        var h = getNum(binding.heightValue.text.toString())
        Log.w("altezza", "$h")

        val ruler = dialog.findViewById<RulerValuePicker>(R.id.ruler_picker)
        var height = dialog.findViewById<TextView>(R.id.height)
        ruler.setIndicatorHeight(0.35f, 0.15f)
        ruler.selectValue(h.toInt())
        ruler.setValuePickerListener(object : RulerValuePickerListener {
            override fun onValueChange(value: Int) {
                //Value changed and the user stopped scrolling the ruler.
                //You can consider this value as final selected value.
                height.text = ruler.currentValue.toString() + " cm"
                h = ruler.currentValue.toFloat()
            }

            override fun onIntermediateValueChange(selectedValue: Int) {
                //Value changed but the user is still scrolling the ruler.
                //This value is not final value. You can utilize this value to display the current selected value.
                height.text = ruler.currentValue.toString() + " cm"
                h = ruler.currentValue.toFloat()
            }
        })

        dialog.findViewById<Button>(R.id.height_button).setOnClickListener {
            //databese.setValue(ruler.currentValue)
            binding.heightValue.text = "${h.toInt()} cm"
            dialog.dismiss()
        }



        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }


    private fun showSexDialog() {
        val dialog = Dialog(contx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_sex)

        var sex = 1
        val gender = binding.genderValue.text.toString()
        if (gender.lowercase() == "male") sex = 0

        if (sex == 0)
            dialog.findViewById<RadioGroup>(R.id.buttons).check(R.id.male_button)
        else if (sex == 1)
            dialog.findViewById<RadioGroup>(R.id.buttons).check(R.id.female_button)

        dialog.findViewById<TextView>(R.id.done).setOnClickListener {
            if (dialog.findViewById<RadioButton>(R.id.male_button).isChecked) {
                //databese.setValue(0)
                binding.genderValue.text = "Male"
            } else {
                //databese.setValue(1)
                binding.genderValue.text = "Female"
            }
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.exit).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }

    private fun showNameDialog(){
        val dialog = Dialog(contx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_name)

        val editName : TextInputEditText = dialog.findViewById(R.id.name_text)
        val editSurname : TextInputEditText = dialog.findViewById(R.id.surname_text)

        val fullName = binding.profileName.text.toString()
        val name = (fullName.substringBefore(" "))
        var surname = (fullName.substringAfter(" "))
        if (name == surname) surname = "" // senza cognome
        editName.text = Editable.Factory.getInstance().newEditable(name)
        editSurname.text = Editable.Factory.getInstance().newEditable(surname)

        dialog.findViewById<TextView>(R.id.done).setOnClickListener{
            //databese.setValue(name)
            //databese.setValue(surname)
            val name = dialog.findViewById<TextInputEditText?>(R.id.name_text).text.toString()
            val surname = dialog.findViewById<TextInputEditText?>(R.id.surname_text).text.toString()
            binding.profileName.text = "${name} ${surname}"
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.exit).setOnClickListener{
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }

    private fun showAgeDialog() {
        val dialog = Dialog(contx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_age)

        val picker = dialog.findViewById<DatePicker>(R.id.date_picker)

        picker.updateDate(1998, 6, 28)
        picker.maxDate = System.currentTimeMillis()

        dialog.findViewById<TextView>(R.id.done).setOnClickListener {
            //databese.("year")
            //databese.("month")
            //databese.("day")
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.exit).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }
}