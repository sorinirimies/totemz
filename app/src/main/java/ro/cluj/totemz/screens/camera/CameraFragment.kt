package ro.cluj.totemz.screens.camera

/* ktlint-disable no-wildcard-imports */

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ro.cluj.totemz.BaseFragment
import ro.cluj.totemz.BasePresenter
import ro.cluj.totemz.R
import ro.cluj.totemz.models.FragmentTypes

class CameraFragment : BaseFragment(), CameraView {
    var CAMERA_REQUEST = 93
    lateinit var presenter: CameraPresenter
    val TAG = CameraFragment::class.java.simpleName

    companion object {
        @JvmStatic
        fun newInstance(): CameraFragment = CameraFragment()
    }

    override fun getFragType(): FragmentTypes {
        return FragmentTypes.FRAG_CAM
    }

    override fun getPresenter(): BasePresenter<*> {
        return presenter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.frag_totem_camera, container, false)
        presenter = CameraPresenter()
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CAMERA_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            val photo = data?.extras?.get("data") as Bitmap
        }
    }
}
