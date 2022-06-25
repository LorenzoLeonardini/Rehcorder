package dev.leonardini.rehcorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.get
import androidx.core.view.marginBottom
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.navigation.NavigationBarView
import dev.leonardini.rehcorder.databinding.ActivityMainBinding
import dev.leonardini.rehcorder.db.Rehearsal
import dev.leonardini.rehcorder.services.RecorderService
import dev.leonardini.rehcorder.ui.RecordingFragment
import dev.leonardini.rehcorder.ui.dialogs.MaterialInfoDialogFragment

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemReselectedListener,
    NavigationBarView.OnItemSelectedListener, View.OnClickListener {

    companion object {
        private const val REQUEST_PERMISSIONS = 42
        private const val PERMISSION_DIALOG_TAG = "PermissionDialog"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var recording: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val topLevelDestinations = HashSet<Int>()
        topLevelDestinations.add(R.id.SongsFragment)
        topLevelDestinations.add(R.id.RehearsalsFragment)
        topLevelDestinations.add(R.id.RecordingFragment)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration.Builder(topLevelDestinations).build()
        setupActionBarWithNavController(navController, appBarConfiguration)

        val bottomNavigation = binding.bottomNavigation
        bottomNavigation.menu[1].isEnabled = false
        bottomNavigation.setOnItemReselectedListener(this)
        bottomNavigation.setOnItemSelectedListener(this)

        binding.fab.setOnClickListener(this)
        val fabMargin = binding.fab.marginBottom
        binding.fab.setOnApplyWindowInsetsListener { v, insets ->
            // Deprecated in API 30, it's fine to use
            (v.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                fabMargin + insets.systemWindowInsetBottom
            v.requestLayout()
            insets
        }

        // Recover state
        if (savedInstanceState != null) {
            recording = savedInstanceState.getBoolean("recording")
        } else if (RecorderService.startTimestamp != -1L) {
            val args = Bundle()
            args.putLong("timestamp", RecorderService.startTimestamp)
            findNavController(R.id.nav_host_fragment_content_main).navigate(
                R.id.to_recording_fragment,
                args
            )
            recording = true
        }
        if (recording) {
            binding.bottomNavigation.selectedItemId = R.id.page_record
            binding.bottomNavigation.menu[0].isEnabled = false
            binding.bottomNavigation.menu[2].isEnabled = false

            binding.fab.setImageResource(R.drawable.ic_stop)
        }
    }

    private var permissionToRecordAccepted: Boolean = false
    private var permissionToGetFineLocationAccepted: Boolean = false
    private var permissionToGetCoarseLocationAccepted: Boolean = false
    private var permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("recording", recording)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (i in permissions.indices) {
            if (requestCode != REQUEST_PERMISSIONS) {
                continue
            }
            val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED
            when (permissions[i]) {
                Manifest.permission.RECORD_AUDIO -> {
                    permissionToRecordAccepted = granted
                }
                Manifest.permission.ACCESS_FINE_LOCATION -> {
                    permissionToGetFineLocationAccepted = granted
                }
                Manifest.permission.ACCESS_COARSE_LOCATION -> {
                    permissionToGetCoarseLocationAccepted = granted
                }
            }

            Log.i("Permissions", "Permission $i : ${permissions[i]} ${grantResults[i]}")
        }
        if (!permissionToRecordAccepted) {
            MaterialInfoDialogFragment(
                R.string.permission_required_title,
                R.string.permission_required
            ).show(supportFragmentManager, PERMISSION_DIALOG_TAG)
            binding.fab.setImageResource(R.drawable.ic_record)
            recording = false
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        if (!permissionToRecordAccepted) return

        binding.fab.setImageResource(R.drawable.ic_stop)
        binding.bottomNavigation.selectedItemId = R.id.page_record
        binding.bottomNavigation.menu[0].isEnabled = false
        binding.bottomNavigation.menu[2].isEnabled = false

        val timestamp = System.currentTimeMillis() / 1000
        val args = Bundle()
        args.putLong("timestamp", timestamp)

        findNavController(R.id.nav_host_fragment_content_main).navigate(
            R.id.to_recording_fragment,
            args
        )
        recording = true

        Thread {
            val (id, file) = Rehearsal.create(
                applicationContext,
                permissionToGetCoarseLocationAccepted,
                permissionToGetFineLocationAccepted
            )

            runOnUiThread {
                val intent = Intent(this, RecorderService::class.java)
                intent.action = "RECORD"
                intent.putExtra("id", id)
                intent.putExtra("file", file)
                intent.putExtra("startTimestamp", timestamp)
                if (Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }.start()
    }

    private fun stopRecording() {
        val intent = Intent(this, RecorderService::class.java)
        intent.action = "STOP"
        startService(intent)

        supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
            ?.let { navFragment ->
                if (navFragment.childFragmentManager.primaryNavigationFragment is RecordingFragment) {
                    (navFragment.childFragmentManager.primaryNavigationFragment as RecordingFragment).stopRecording()
                }
            }

        binding.fab.setImageResource(R.drawable.ic_record)
        recording = false

        binding.bottomNavigation.menu[0].isEnabled = true
        binding.bottomNavigation.menu[2].isEnabled = true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        // needed to avoid reselection
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (recording) {
            return false
        }
        return when (item.itemId) {
            R.id.page_songs -> {
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.to_songs_fragment)
                true
            }
            R.id.page_rehearsals -> {
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.to_rehearsals_fragment)
                true
            }
            else -> {
                false
            }
        }
    }

    override fun onClick(v: View?) {
        if (v == binding.fab) {
            if (recording) {
                stopRecording()
            } else {
                permissionToRecordAccepted = ContextCompat.checkSelfPermission(
                    baseContext,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                permissionToGetCoarseLocationAccepted = ContextCompat.checkSelfPermission(
                    baseContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                permissionToGetFineLocationAccepted = ContextCompat.checkSelfPermission(
                    baseContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!permissionToRecordAccepted || (!permissionToGetCoarseLocationAccepted && !permissionToGetFineLocationAccepted)) {
                    ActivityCompat.requestPermissions(
                        this,
                        permissions,
                        REQUEST_PERMISSIONS
                    )
                } else {
                    startRecording()
                }
            }
        }
    }
}