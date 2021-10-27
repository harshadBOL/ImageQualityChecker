package com.example.imgprocess

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    var isOpenCvInit = false
    lateinit var btnChooseImg: AppCompatButton
    lateinit var imgPreview: AppCompatImageView
    lateinit var tvResult: AppCompatTextView
    lateinit var pgBar: ProgressBar
    lateinit var sourceMatImage: Mat
    val SELECT_IMG_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        isOpenCvInit = OpenCVLoader.initDebug()
        initViews()
    }

    private fun initViews() {
        btnChooseImg = findViewById(R.id.btnChooseImg)
        imgPreview = findViewById(R.id.ivPick)
        tvResult = findViewById(R.id.tvResult)
        pgBar = findViewById(R.id.imgQtCheck)
        btnChooseImg.setOnClickListener {
            var intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_IMG_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == SELECT_IMG_CODE && resultCode == -1 && data != null) {
                var uri: Uri = data.data!!
                var thumbnail = MediaStore.Images.Media.getBitmap(
                    getContentResolver(), uri
                )
                imgPreview.setImageBitmap(thumbnail)
                checkLightEffect(thumbnail)
                Log.d(
                    "BIT",
                    "density = ${thumbnail.density} | height = ${thumbnail.height} | width = ${thumbnail.width}"
                )
            }
        } catch (exc: Exception) {
            Toast.makeText(this, "Something went wrong ${exc.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLightEffect(img: Bitmap) {
        var histogram = arrayOfNulls<Int>(256)
        for (i in 0 until histogram.size step 1) {
            histogram[i] = 0;
        }
        pgBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            delay(100)
            for (x in 0 until img.width step 1) {
                for (y in 0 until img.height step 1) {
                    var pixel = img.getPixel(x, y)
                    var r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    var brightness = (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt()
                    histogram[brightness] = histogram[brightness]!! + 1
                }
            }
            var allPixel = img.width * img.height
            var darkPixelCount = 0
            for (i in 0 until 10 step 1) {
                darkPixelCount += histogram[i]!!
            }

            var imgSharpness = getSharpnessScoreFromOpenCV(img)
            showToast(allPixel, darkPixelCount, imgSharpness)
        }
    }

    private fun showToast(allPixel: Int, darkPixelCount: Int, imgBlur: Double) {
        CoroutineScope(Dispatchers.Main).launch {
            pgBar.visibility = View.INVISIBLE
            if (darkPixelCount > allPixel * 0.25) {
                tvResult.text = "Dark Image | Sharpness is $imgBlur"
                //Toast.makeText(baseContext, "Dark picture", Toast.LENGTH_SHORT).show()
            } else {
                tvResult.text = "Light Image | Sharpness is $imgBlur"
                // Toast.makeText(baseContext, "Light picture", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getSharpnessScoreFromOpenCV(bitmap: Bitmap): Double {
        val matGray = Mat()
        sourceMatImage = Mat()
        var destination = Mat()
        Utils.bitmapToMat(bitmap, sourceMatImage)
        Imgproc.cvtColor(sourceMatImage, matGray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.Laplacian(matGray, destination, 3)
        val median = MatOfDouble()
        val std = MatOfDouble()
        Core.meanStdDev(destination, median, std)
        return DecimalFormat("0.00").format(Math.pow(std.get(0, 0)[0], 2.0)).toDouble()
    }
}