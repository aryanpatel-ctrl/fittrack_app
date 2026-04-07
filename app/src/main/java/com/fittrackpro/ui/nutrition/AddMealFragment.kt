package com.fittrackpro.ui.nutrition

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fittrackpro.databinding.FragmentAddMealBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class AddMealFragment : Fragment() {

    private var _binding: FragmentAddMealBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMealViewModel by viewModels()

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var barcodeScanner: BarcodeScanner? = null
    private var isScanning = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startBarcodeScanner()
        } else {
            Toast.makeText(requireContext(), "Camera permission required for barcode scanning", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mealType = arguments?.getString("mealType") ?: "breakfast"
        viewModel.setMealType(mealType)

        cameraExecutor = Executors.newSingleThreadExecutor()
        setupBarcodeScanner()
        setupUI()
        observeViewModel()
    }

    private fun setupBarcodeScanner() {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39
            )
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        val mealTypes = arrayOf("Breakfast", "Lunch", "Dinner", "Snack")
        binding.spinnerMealType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mealTypes)

        binding.etSearchFood.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()
                if (!query.isNullOrEmpty() && query.length >= 2) viewModel.searchFood(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnScanBarcode.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                startBarcodeScanner()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnCloseCamera.setOnClickListener {
            stopBarcodeScanner()
        }

        binding.btnQuickAdd.setOnClickListener {
            val name = binding.etFoodName.text.toString().trim()
            val calories = binding.etCalories.text.toString().toIntOrNull() ?: 0
            val protein = binding.etProtein.text.toString().toFloatOrNull() ?: 0f
            val carbs = binding.etCarbs.text.toString().toFloatOrNull() ?: 0f
            val fat = binding.etFat.text.toString().toFloatOrNull() ?: 0f
            val quantity = binding.etQuantity.text.toString().toFloatOrNull() ?: 1f
            val mealType = binding.spinnerMealType.selectedItem.toString().lowercase()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a food name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.addMealEntry(name, calories, protein, carbs, fat, quantity, mealType)
        }

        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            binding.rvSearchResults.visibility = if (results.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.selectedFood.observe(viewLifecycleOwner) { food ->
            food?.let {
                binding.etFoodName.setText(it.name)
                binding.etCalories.setText(it.calories.toString())
                binding.etProtein.setText(it.protein.toString())
                binding.etCarbs.setText(it.carbs.toString())
                binding.etFat.setText(it.fat.toString())
                binding.rvSearchResults.visibility = View.GONE
            }
        }

        viewModel.mealAdded.observe(viewLifecycleOwner) { added ->
            if (added) {
                Toast.makeText(requireContext(), "Meal logged!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        viewModel.barcodeLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarcode.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.barcodeError.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearBarcodeError()
            }
        }
    }

    private fun startBarcodeScanner() {
        binding.cardCameraPreview.visibility = View.VISIBLE
        isScanning = true

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                    if (isScanning) {
                        isScanning = false
                        requireActivity().runOnUiThread {
                            onBarcodeDetected(barcode)
                        }
                    }
                })
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            Log.e("AddMealFragment", "Camera binding failed", e)
            Toast.makeText(requireContext(), "Failed to start camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onBarcodeDetected(barcode: String) {
        stopBarcodeScanner()
        Toast.makeText(requireContext(), "Barcode: $barcode", Toast.LENGTH_SHORT).show()
        viewModel.lookupBarcode(barcode)
    }

    private fun stopBarcodeScanner() {
        isScanning = false
        cameraProvider?.unbindAll()
        binding.cardCameraPreview.visibility = View.GONE
    }

    private inner class BarcodeAnalyzer(
        private val onBarcodeDetected: (String) -> Unit
    ) : ImageAnalysis.Analyzer {

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                barcodeScanner?.process(image)
                    ?.addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { value ->
                                onBarcodeDetected(value)
                                return@addOnSuccessListener
                            }
                        }
                    }
                    ?.addOnFailureListener {
                        // Silently ignore scan failures
                    }
                    ?.addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        barcodeScanner?.close()
        _binding = null
    }
}
