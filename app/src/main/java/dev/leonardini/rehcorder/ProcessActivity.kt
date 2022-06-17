package dev.leonardini.rehcorder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import dev.leonardini.rehcorder.databinding.ActivityProcessBinding
import java.io.File

class ProcessActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityProcessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityProcessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (intent.hasExtra("fileName")) {
//            Thread {
            val fileName = intent.getStringExtra("fileName")
            val uri = FileProvider.getUriForFile(
                this,
                "${this.packageName}.provider",
                File("${filesDir.absolutePath}/recordings/$fileName")
            )

            val viewMediaIntent = Intent()
            viewMediaIntent.action = Intent.ACTION_VIEW
            viewMediaIntent.setDataAndType(uri, "audio/*")
            viewMediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(viewMediaIntent)
//            }.start()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}