package com.mismayilov.radar

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.lottie.LottieAnimationView
import com.mismayilov.radar.databinding.ActivityMainBinding
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothDevice: BluetoothDevice
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream
    private val mHandler: Handler = Handler()

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("MissingPermission", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animateRadar()
        loadTempData()

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) return


        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        if (pairedDevices != null && pairedDevices.isNotEmpty()) {
            for (device in pairedDevices) {
                if (device.name == "HC-05") {
                    bluetoothDevice = device
                    break
                }
            }
        }
        if (!::bluetoothDevice.isInitialized) {
            Toast.makeText(this, "HC-05 cihazı tapılmadı", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            bluetoothSocket =
                bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            bluetoothSocket.connect()
            inputStream = bluetoothSocket.inputStream
            outputStream = bluetoothSocket.outputStream
            listenForData()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        binding.btnBack.setOnLongClickListener { sendData("B"); true }
        binding.btnFront.setOnLongClickListener { sendData("F"); true }
        binding.btnLeft.setOnLongClickListener { sendData("L"); true }
        binding.btnRight.setOnLongClickListener { sendData("R"); true }


        binding.btnBack.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == android.view.MotionEvent.ACTION_UP) {
                sendData("S")
            }
            false
        }
        binding.btnFront.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == android.view.MotionEvent.ACTION_UP) {
                sendData("S")
            }
            false
        }
        binding.btnLeft.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == android.view.MotionEvent.ACTION_UP) {
                sendData("S")
            }
            false
        }
        binding.btnRight.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == android.view.MotionEvent.ACTION_UP) {
                sendData("S")
            }
            false
        }
    }

    private fun listenForData() {
        Thread {
            while (true) {
                try {
                    val buffer = ByteArray(1024)
                    val bytes: Int = inputStream.read(buffer) ?: -1
                    if (bytes != -1) {
                        val dataList = String(buffer, 0, bytes)
                        val data = dataList.split(",")
                        val list = data[0].split(":")
                        mHandler.post {
                            Log.e("TAG", list[0] ?: "No data")
                            try {
                                if (list[0].contains("null")) return@post
                                loadTempData(list[0].toDouble(), list[1].toDouble())
                            } catch (e: Exception) {
                                Log.e("TAG", "Error parsing data", e)
                            }
                        }
                    }
                    Thread.sleep(1000)
                } catch (e: IOException) {
                    Log.e("TAG", "Error reading from input stream", e)
                    break
                }
            }
        }.start()
    }

    private fun sendData(data: String) {
        val msgBuffer = data.toByteArray()
        try {
            outputStream?.write(msgBuffer)
        } catch (e: IOException) {
            Log.e("TAG", "Error sending data", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun animateRadar() {
        val mImgRadarBack: ImageView = findViewById(R.id.mImgRadarBack)
        val rotation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.rotate)
        rotation.fillAfter = true
        mImgRadarBack.startAnimation(rotation)
    }

    private fun loadTempData(distance: Double? = null, rotation: Double = 0.0) {
        binding.myConstraint.removeAllViews()
        if (distance == null) return
        val lotieAnimation = LottieAnimationView(this)
        lotieAnimation.setAnimation(R.raw.radar_object)
        lotieAnimation.loop(true)
        lotieAnimation.playAnimation()
        lotieAnimation.layoutParams = ConstraintLayout.LayoutParams(
            500,
            500
        ).apply {
            topToTop = R.id.myConstraint
            topMargin = 350-distance.roundToInt()
        }

        binding.myConstraint.rotation = 100- if (rotation <180) rotation.toFloat() else (360-rotation).toFloat()
        binding.myConstraint.addView(lotieAnimation)

    }
}