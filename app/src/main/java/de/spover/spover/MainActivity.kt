package de.spover.spover

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import de.spover.spover.fragments.SettingsFragment


class MainActivity : AppCompatActivity() {

    companion object {
        private var TAG = MainActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initSettingsFragment(savedInstanceState)
    }

    private fun initSettingsFragment(savedInstanceState: Bundle?) {
        // The fragment container has already been replaced by a fragment
        if (findViewById<FrameLayout>(R.id.fragmentContainer) == null) {
            return
        }

        // If we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return
        }

        val settingsFragment = SettingsFragment()

        // In case this activity was started with special instructions from an Intent,
        // pass the Intent's extras to the fragment as arguments
        settingsFragment.arguments = intent.extras

        val transaction = supportFragmentManager.beginTransaction()
        val settingsFragmentTag = "settingsFragmentTag"
        transaction.add(R.id.fragmentContainer, settingsFragment, settingsFragmentTag)
        transaction.commit()
    }
}
