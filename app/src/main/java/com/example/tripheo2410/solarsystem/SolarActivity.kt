package com.example.tripheo2410.solarsystem

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable

class SolarActivity : AppCompatActivity() {
    private var installRequested: Boolean = false
    private val solarSettings = SolarSettings()
    private var arSceneView: ArSceneView? = null
    private var loadingMessageSnackbar: Snackbar? = null
    private var sunRenderable: ModelRenderable? = null
    private var mercuryRenderable: ModelRenderable? = null
    private var venusRenderable: ModelRenderable? = null
    private var earthRenderable: ModelRenderable? = null
    private var lunaRenderable: ModelRenderable? = null
    private var marsRenderable: ModelRenderable? = null
    private var jupiterRenderable: ModelRenderable? = null
    private var saturnRenderable: ModelRenderable? = null
    private var uranusRenderable: ModelRenderable? = null
    private var neptuneRenderable: ModelRenderable? = null
    private val solarControlsRenderable: ViewRenderable? = null

    // True once scene is loaded
    private var hasFinishedLoading = false

    // True once the scene has been placed.
    private var hasPlacedSolarSystem = false
    companion object {
        private val RC_PERMISSIONS = 0x123

        // Astronomical units to meters ratio. Used for positioning the planets of the solar system.
        private val AU_TO_METERS = 0.5f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solar)
    }

    override fun onResume() {
        super.onResume()
        if (arSceneView == null) {
            return
        }

        if (arSceneView!!.session == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                val session = DemoUtils.createArSession(this, installRequested)
                if (session == null) {
                    installRequested = DemoUtils.hasCameraPermission(this)
                    return
                } else {
                    arSceneView!!.setupSession(session)
                }
            } catch (e: UnavailableException) {
                DemoUtils.handleSessionException(this, e)
            }

        }

        try {
            arSceneView!!.resume()
        } catch (ex: CameraNotAvailableException) {
            DemoUtils.displayError(this, "Unable to get camera", ex)
            finish()
            return
        }

        if (arSceneView!!.session != null) {
            showLoadingMessage()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (arSceneView != null) {
            arSceneView!!.pause()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (arSceneView != null) {
            arSceneView!!.destroy()
        }
    }

    private fun tryPlaceSolarSystem(tap: MotionEvent?, frame: Frame): Boolean {
        if (tap != null && frame.camera.trackingState == TrackingState.TRACKING) {
            for (hit in frame.hitTest(tap)) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    // Create the Anchor.
                    val anchor = hit.createAnchor()
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(arSceneView!!.scene)
                    val solarSystem = createSolarSystem()
                    anchorNode.addChild(solarSystem)
                    return true
                }
            }
        }

        return false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Standard Android full-screen functionality.
            window
                    .decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun onSingleTap(tap: MotionEvent) {
        if (!hasFinishedLoading) {
            // We can't do anything yet.
            return
        }

        val frame = arSceneView!!.arFrame
        if (frame != null) {
            if (!hasPlacedSolarSystem && tryPlaceSolarSystem(tap, frame)) {
                hasPlacedSolarSystem = true
            }
        }
    }

    private fun createSolarSystem(): Node {
        val base = Node()

        val sun = Node()
        sun.setParent(base)
        sun.localPosition = Vector3(0.0f, 0.5f, 0.0f)

        val sunVisual = Node()
        sunVisual.setParent(sun)
        sunVisual.renderable = sunRenderable
        sunVisual.localScale = Vector3(0.5f, 0.5f, 0.5f)

        val solarControls = Node()
        solarControls.setParent(sun)
        solarControls.renderable = solarControlsRenderable
        solarControls.localPosition = Vector3(0.0f, 0.25f, 0.0f)

        createPlanet("Mercury", sun, 0.4f, 47f, mercuryRenderable, 0.019f)

        createPlanet("Venus", sun, 0.7f, 35f, venusRenderable, 0.0475f)

        val earth = createPlanet("Earth", sun, 1.0f, 29f, earthRenderable, 0.05f)

        createPlanet("Moon", earth, 0.15f, 100f, lunaRenderable, 0.018f)

        createPlanet("Mars", sun, 1.5f, 24f, marsRenderable, 0.0265f)

        createPlanet("Jupiter", sun, 2.2f, 13f, jupiterRenderable, 0.16f)

        createPlanet("Saturn", sun, 3.5f, 9f, saturnRenderable, 0.1325f)

        createPlanet("Uranus", sun, 5.2f, 7f, uranusRenderable, 0.1f)

        createPlanet("Neptune", sun, 6.1f, 5f, neptuneRenderable, 0.074f)

        return base
    }

    private fun createPlanet(
            name: String,
            parent: Node,
            auFromParent: Float,
            orbitDegreesPerSecond: Float,
            renderable: ModelRenderable?,
            planetScale: Float): Node {
        // Orbit is a rotating node with no renderable positioned at the sun.
        // The planet is positioned relative to the orbit so that it appears to rotate around the sun.
        // This is done instead of making the sun rotate so each planet can orbit at its own speed.
        val orbit = RotatingNode(solarSettings, true)
        orbit.setDegreesPerSecond(orbitDegreesPerSecond)
        orbit.setParent(parent)

        // Create the planet and position it relative to the sun.
        val planet = Planet(this, name, planetScale, renderable!!, solarSettings)
        planet.setParent(orbit)
        planet.localPosition = Vector3(auFromParent * AU_TO_METERS, 0.0f, 0.0f)

        return planet
    }

    private fun showLoadingMessage() {
        if (loadingMessageSnackbar != null && loadingMessageSnackbar!!.isShownOrQueued) {
            return
        }

        loadingMessageSnackbar = Snackbar.make(
                this@SolarActivity.findViewById(android.R.id.content),
                R.string.plane_finding,
                Snackbar.LENGTH_INDEFINITE)
        loadingMessageSnackbar!!.view.setBackgroundColor(-0x40cdcdce)
        loadingMessageSnackbar!!.show()
    }

    private fun hideLoadingMessage() {
        if (loadingMessageSnackbar == null) {
            return
        }

        loadingMessageSnackbar!!.dismiss()
        loadingMessageSnackbar = null
    }
}
