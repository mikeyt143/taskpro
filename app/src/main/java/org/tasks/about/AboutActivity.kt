package org.tasks.about

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import org.tasks.R

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
    val aboutText = findViewById<TextView>(R.id.about_text)
    val btnSource = findViewById<android.widget.Button>(R.id.button_source)
    val btnLicense = findViewById<android.widget.Button>(R.id.button_license)
        val credits = getString(R.string.about_credits)
        val showPrivacy = intent.getBooleanExtra("show_privacy", false)
        val showLicense = intent.getBooleanExtra("show_license", false)
    when {
            showPrivacy -> {
                val assetText = try {
                    assets.open("privacy_policy.txt").bufferedReader().use { it.readText() }
                } catch (e: Exception) { "" }
                aboutText.text = "$credits\n\n$assetText"
            }
            showLicense -> {
                val licenseText = try {
                    assets.open("LICENSE_NOTICE.txt").bufferedReader().use { it.readText() }
                } catch (e: Exception) { "" }
                aboutText.text = "$credits\n\n$licenseText"
            }
            else -> aboutText.text = credits
        }

        btnSource.setOnClickListener {
            val url = getString(R.string.source_code_url)
            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }
        btnLicense.setOnClickListener {
            val licenseText = try {
                assets.open("LICENSE_NOTICE.txt").bufferedReader().use { it.readText() }
            } catch (e: Exception) { "" }
            aboutText.text = "$credits\n\n$licenseText"
        }
    }
}
