package com.gametux.console

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.gametux.console.databinding.ActivityMainBinding
import com.gametux.console.service.ConsoleService

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var consoleService: ConsoleService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ConsoleService.ConsoleBinder
            consoleService = binder.getService()
            isBound = true

            consoleService?.startDiscovery {
                runOnUiThread {
                    binding.tvStatus.text = "Connected to TV ✅"
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startAndBindService()
        setupUI()
    }

    private fun startAndBindService() {
        val intent = Intent(this, ConsoleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun setupUI() {
        binding.btnAddRom.setOnClickListener {
            // For MVP, just simulate loading a ROM
            showController()
        }

        setupControllerButtons()
    }

    private fun showController() {
        binding.romSelectionLayout.visibility = View.GONE
        binding.controllerLayout.root.visibility = View.VISIBLE
    }

    private fun setupControllerButtons() {
        val buttons = listOf(
            binding.controllerLayout.btnUp,
            binding.controllerLayout.btnDown,
            binding.controllerLayout.btnLeft,
            binding.controllerLayout.btnRight,
            binding.controllerLayout.btnA,
            binding.controllerLayout.btnB,
            binding.controllerLayout.btnStart,
            binding.controllerLayout.btnSelect
        )

        buttons.forEach { button ->
            button.setOnTouchListener { view, event ->
                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    // TODO: Send input to emulator
                }
                false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
