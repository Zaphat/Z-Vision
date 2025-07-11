package com.zteam.zvision.ui.screens.qrCreation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.zteam.zvision.ui.features.qrCreation.QrCreationViewModel
import com.zteam.zvision.ui.features.qrCreation.QRContent
import com.zteam.zvision.ui.features.qrCreation.TextQR
import com.zteam.zvision.ui.features.qrCreation.UrlQR
import com.zteam.zvision.ui.features.qrCreation.QRGenerator
import com.zteam.zvision.domain.QrUsecase
import com.zteam.zvision.data.repository.QrRepository
import com.zteam.zvision.data.local.AppDatabase
import com.zteam.zvision.data.model.QrModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*
import android.graphics.Bitmap
import android.widget.ImageView

class QrCreationFragment : Fragment() {
    // For demonstration, manual ViewModel instantiation. Use DI in production.
    private val viewModel: QrCreationViewModel by lazy {
        val context = requireContext().applicationContext
        val db = AppDatabase.getInstance(context)
        val repo = QrRepository(db.qrDao())
        val usecase = QrUsecase(repo)
        QrCreationViewModel(usecase)
    }

    private lateinit var nameEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var typeSpinner: Spinner
    private lateinit var favoriteCheckBox: CheckBox
    private lateinit var generateButton: Button
    private lateinit var saveButton: Button
    private lateinit var qrImageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)

            nameEditText = EditText(context).apply { hint = "QR Name" }
            addView(nameEditText)

            typeSpinner = Spinner(context)
            val types = arrayOf("URL", "Text") // Add WiFi if needed
            typeSpinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, types)
            addView(typeSpinner)

            contentEditText = EditText(context).apply { hint = "Content" }
            addView(contentEditText)

            favoriteCheckBox = CheckBox(context).apply { text = "Favorite" }
            addView(favoriteCheckBox)

            generateButton = Button(context).apply { text = "Generate QR" }
            addView(generateButton)

            qrImageView = ImageView(context)
            addView(qrImageView)

            saveButton = Button(context).apply { text = "Save QR" }
            addView(saveButton)
        }
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generateButton.setOnClickListener {
            val content = getQrContentFromInput()
            if (content != null) {
                viewModel.generateQrBitmap(content)
            } else {
                Toast.makeText(requireContext(), "Invalid content", Toast.LENGTH_SHORT).show()
            }
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val content = getQrContentFromInput()
            val favorite = favoriteCheckBox.isChecked
            if (name.isNotBlank() && content != null) {
                viewModel.createAndSaveQr(name, content, favorite)
                Toast.makeText(requireContext(), "QR saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe generated QR bitmap
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.generatedBitmap.collectLatest { bitmap ->
                qrImageView.setImageBitmap(bitmap)
            }
        }
    }

    private fun getQrContentFromInput(): QRContent? {
        val type = typeSpinner.selectedItem.toString()
        val content = contentEditText.text.toString()
        return when (type) {
            "URL" -> if (content.isNotBlank()) UrlQR(content) else null
            "Text" -> if (content.isNotBlank()) TextQR(content) else null
            // Add WiFiQR input handling if needed
            else -> null
        }
    }
}

