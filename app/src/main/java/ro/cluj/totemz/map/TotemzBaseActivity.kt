package ro.cluj.totemz.map

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.View
import android.view.animation.BounceInterpolator
import com.github.salomonbrys.kodein.android.withContext
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.intentFor
import ro.cluj.totemz.*
import ro.cluj.totemz.mqtt.MQTTService
import ro.cluj.totemz.utils.FadePageTransformer
import ro.cluj.totemz.utils.RxBus
import rx.subscriptions.CompositeSubscription
import java.util.*
import kotlin.concurrent.timerTask

class TotemzBaseActivity : BaseActivity(), ViewPager.OnPageChangeListener, OnFragmentActionsListener, TotemzBaseView {

    val SERVICE_CLASSNAME = "ro.cluj.totemz.mqtt.MQTTService"

    var CAMERA_REQUEST = 93

    //Animation properties
    val SCALE_UP = 1f
    val SCALE_DOWN = 0.7f
    val DURATION = 300L
    val TAG = TotemzBaseActivity::class.java.simpleName

    //Subscriptions
    val subscriptions = CompositeSubscription()
    val TAB_CAMERA = 0
    val TAB_MAP = 1
    val TAB_USER = 2
    //Injections
    val presenterTotemzBase: TotemzBasePresenter by instance()
    val activityManager: ActivityManager by withContext(this).instance()
    val rxBus: RxBus by instance()

    @StringRes
    override fun getActivityTitle(): Int {
        return R.string.app_name
    }

    override fun getRootLayout(): View {
        return container_totem
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        presenterTotemzBase.attachView(this)

        /* Instantiate pager adapter and set fragments*/
        val adapter = BaseFragAdapter(supportFragmentManager,
                arrayListOf(FragmentCamera.newInstance(), FragmentMap.newInstance()))

        /*set custom trnasformer for fading text view*/
        pager_menu_switch.setPageTransformer(true, FadePageTransformer())
        pager_menu_switch.adapter = adapter

        /*Set ofscreen page limit for fragment state retention*/
        pager_menu_switch.offscreenPageLimit = 3


        // Set menu click listeners
        img_camera.setOnClickListener {
            pager_menu_switch.currentItem = TAB_CAMERA
            subscriptions.add(
                    presenterTotemzBase.scaleAnimation(arrayListOf(img_camera), SCALE_UP, DURATION,
                            BounceInterpolator()).mergeWith(
                            presenterTotemzBase.scaleAnimation(arrayListOf(img_compass, img_user), SCALE_DOWN,
                                    DURATION, BounceInterpolator()))

                            .subscribe({
                                //                                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//                                startActivityForResult(cameraIntent, CAMERA_REQUEST)
                            }))
            cont_pulse_camera.start()
            cont_pulse_compass.stop()
        }

        img_compass.setOnClickListener {
            pager_menu_switch.currentItem = TAB_MAP
            subscriptions.add(
                    presenterTotemzBase.scaleAnimation(arrayListOf(img_compass), SCALE_UP, DURATION,
                            BounceInterpolator())
                            .mergeWith(
                                    presenterTotemzBase.scaleAnimation(arrayListOf(img_camera, img_user), SCALE_DOWN,
                                            DURATION,
                                            BounceInterpolator()))
                            .subscribe())
            cont_pulse_compass.start()
            if (!serviceIsRunning()) {
                startService(intentFor<MQTTService>())
            }
        }

        img_user.setOnClickListener {
            pager_menu_switch.currentItem = TAB_USER
            subscriptions.add(
                    presenterTotemzBase.scaleAnimation(arrayListOf(img_user), SCALE_UP, DURATION,
                            BounceInterpolator())
                            .mergeWith(
                                    presenterTotemzBase.scaleAnimation(arrayListOf(img_camera, img_compass),
                                            SCALE_DOWN,
                                            DURATION, BounceInterpolator()))
                            .subscribe())
            if (serviceIsRunning()) {
                stopMQTTLocationService()
            }
            cont_pulse_uer.start()
            cont_pulse_compass.stop()
        }


        cont_pulse_compass.start()
        Timer().schedule(timerTask { startService(intentFor<MQTTService>()) }, 2000)
    }

    //TODO USE IT!
    override fun onNextFragment(fragType: FragmentTypes) {
        when (fragType) {
            FragmentTypes.FRAG_CAM -> {

            }
            FragmentTypes.FRAG_MAP -> {

            }
            FragmentTypes.FRAG_USER -> {
            }
        }
    }


    private fun stopMQTTLocationService() {
        val intent = Intent(this, MQTTService::class.java)
        stopService(intent)
    }

    private fun serviceIsRunning(): Boolean {
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (SERVICE_CLASSNAME == service.service.className) {
                Log.i("MQTT", "SERVICE IS RUNNING")
                return true
            }
        }
        Log.i("MQTT", "SERVICE HAS STOPPED")
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CAMERA_REQUEST && resultCode === RESULT_OK) {
//            val photo = data?.extras?.get("data") as Bitmap
        }
    }

    override fun onDestroy() {
        presenterTotemzBase.detachView()
        subscriptions.unsubscribe()
        super.onDestroy()
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {

    }
}

