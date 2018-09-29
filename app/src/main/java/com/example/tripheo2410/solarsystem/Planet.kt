package com.example.tripheo2410.solarsystem

import android.content.Context
import android.view.MotionEvent
import android.widget.TextView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable

class Planet(
        private val context: Context,
        private val planetName: String,
        private val planetScale: Float,
        private val planetRenderable: ModelRenderable,
        private val solarSettings: SolarSettings) : Node(), Node.OnTapListener {

    private var infoCard: Node? = null



    init {
        setOnTapListener(this)
    }






    override fun onTap(hitTestResult: HitTestResult?, motionEvent: MotionEvent?) {
        if (infoCard == null) {
            return
        }

        infoCard!!.isEnabled = !infoCard!!.isEnabled
    }

    companion object {

        private val INFO_CARD_Y_POS_COEFF = 0.55f
    }
}