package ro.cluj.totemz.screens.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.Button
import com.github.salomonbrys.kodein.instance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import com.squareup.picasso.Picasso
import io.reactivex.disposables.CompositeDisposable
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_user_login.*
import kotlinx.android.synthetic.main.frag_user_profile.*
import org.jetbrains.anko.support.v4.intentFor
import ro.cluj.totemz.BaseFragment
import ro.cluj.totemz.BasePresenter
import ro.cluj.totemz.R
import ro.cluj.totemz.model.FragmentTypes
import ro.cluj.totemz.mqtt.MQTTService
import ro.cluj.totemz.realm.UserInfoRealm
import ro.cluj.totemz.screens.camera.CameraPresenter
import ro.cluj.totemz.screens.camera.FragmentCamera
import ro.cluj.totemz.utils.fadeInOutAnimation
import ro.cluj.totemz.utils.query
import ro.cluj.totemz.utils.queryFirst
import timber.log.Timber

/**
 * Created by sorin on 11.10.16.
 *
 * Copyright (c) 2016 moovel GmbH<br>
 *
 * All rights reserved<br>
<p></p>
 */
class FragmentUser : BaseFragment(), ViewFragmentUser {

    private var isLoggedIn = false
    private val disposables = CompositeDisposable()
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    val TAG = FragmentCamera::class.java.simpleName

    val presenter: PresenterFragmentUser by instance()

    companion object {
        fun newInstance(): FragmentUser {
            val fragment = FragmentUser()
            return fragment
        }
    }

    override fun getFragType(): FragmentTypes {
        return FragmentTypes.FRAG_USER
    }

    override fun getPresenter(): BasePresenter<*> {
        return presenter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Timber.i("User is logged in")
                //USer signed in
                isLoggedIn = true
                user.setupLoggedIn()
            } else {
                // User is signed out
                isLoggedIn = false
                Timber.i("onAuthStateChanged:signed_out")
                intentFor<UserLoginActivity>()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.frag_user_profile, container, false)
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btn_logout.signOutListener()
    }

    override fun onStart() {
        super.onStart()
        authStateListener?.let {
            firebaseAuth.addAuthStateListener(it)
        }
    }

    override fun onStop() {
        super.onStop()
        authStateListener?.let {
            firebaseAuth.removeAuthStateListener(it)
        }
    }

    override fun onDetach() {
        super.onDetach()
        disposables.clear()
    }

    fun FirebaseUser.setupLoggedIn() {
        // User is signed in
        Timber.i("onAuthStateChanged:signed_in:" + this.uid)
        tv_email.text = this.email
        tv_id.text = this.uid
        tv_user_name.text = this.displayName
        Picasso.with(activity)
                .load(this.photoUrl)
                .error(R.drawable.vector_profle)
                .transform(CropCircleTransformation())
                .into(img_logged_in)
    }

    fun Button.signOutListener() {
        this.setOnClickListener {
            if (isLoggedIn) {
                FirebaseAuth.getInstance().signOut()
                activity.stopService(intentFor<MQTTService>())
                startActivity(intentFor<UserLoginActivity>())
            }
        }
    }
}
