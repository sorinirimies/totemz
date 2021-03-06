package ro.cluj.totemz

/* ktlint-disable no-wildcard-imports */

import android.app.NotificationManager
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.LazyKodeinAware
import com.github.salomonbrys.kodein.provider
import com.google.firebase.auth.FirebaseAuth

/**
 * Created by Sorin Albu-Irimies on 8/27/2016.
 */
abstract class BaseActivity : AppCompatActivity(), LazyKodeinAware {

    abstract fun getActivityTitle(): Int

    override val kodein = LazyKodein(appKodein)

    val notificationManager: () -> NotificationManager by withContext(this).provider()
    val sharedPrefs: () -> SharedPreferences by withContext(this).provider()
    val firebaseAuth: () -> FirebaseAuth by provider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun setTitle(title: CharSequence) {
        if (getActivityTitle() != 0) {
            super.setTitle(getActivityTitle())
        }
    }
}
