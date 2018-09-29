package com.example.tripheo2410.solarsystem

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import java.util.concurrent.CompletableFuture

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
        if (!DemoUtils.checkIsSupportedDeviceOrFinish(this)) {
            // Not a supported device.
            return
        }
        setContentView(R.layout.activity_solar)
        arSceneView = findViewById(R.id.ar_scene_view)

        // Build all the planet models.
        val sunStage : CompletableFuture<ModelRenderable> = ModelRenderable.builder().setSource(this, Uri.parse("Sol.sfb")).build()
        val mercuryStage : CompletableFuture<ModelRenderable> = ModelRenderable.builder().setSource(this, Uri.parse("Mercury.sfb")).build()
        val venusStage : CompletableFuture<ModelRenderable> = ModelRenderable.builder().setSource(this, Uri.parse("Venus.sfb")).build()
        val earthStage : CompletableFuture<ModelRenderable> = ModelRenderable.builder().setSource(this, Uri.parse("Earth.sfb")).build()
        val lunaStage : CompletableFuture<ModelRenderable> = ModelRenderable.builder().setSource(this, Uri.parse("Luna.sfb")).build()
        val marsStage : CompletableFuture<ModelRenderable> = ModelRenderable.builder().setSource(this, Uri.parse("Mars.sfb")).build()
        val jupiterStage : CompletableFuture<ModelRenderable> = ModelRenderable.builder().setSource(this, Uri.parse("Jupiter.sfb")).build()
        val saturnStage : CompletableFuture<ModelRenderable> = ModelRenderable.builder().setSource(this, Uri.parse("Saturn.sfb")).build()
        val uranusStage : CompletableFuture<ModelRenderable> = ModelRenderable.builder().setSource(this, Uri.parse("Uranus.sfb")).build()
        val neptuneStage : CompletableFuture<ModelRenderable> = ModelRenderable.builder().setSource(this, Uri.parse("Neptune.sfb")).build()

        // Build a renderable from a 2D View.

        CompletableFuture.allOf(
                sunStage,
                mercuryStage,
                venusStage,
                earthStage,
                lunaStage,
                marsStage,
                jupiterStage,
                saturnStage,
                uranusStage,
                neptuneStage)
                .handle<Any> { notUsed, throwable ->
                    // When you build a Renderable, Sceneform loads its resources in the background while
                    // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                    // before calling get().

                    if (throwable != null) {
                        DemoUtils.displayError(this, "Unable to load renderable", throwable)
                        return@handle null
                    }

                    try {
                        sunRenderable = sunStage.get()
                        mercuryRenderable = mercuryStage.get()
                        venusRenderable = venusStage.get()
                        earthRenderable = earthStage.get()
                        lunaRenderable = lunaStage.get()
                        marsRenderable = marsStage.get()
                        jupiterRenderable = jupiterStage.get()
                        saturnRenderable = saturnStage.get()
                        uranusRenderable = uranusStage.get()
                        neptuneRenderable = neptuneStage.get()
                        // Everything finished loading successfully.
                        hasFinishedLoading = true

                    } catch (ex: InterruptedException) {
                        DemoUtils.displayError(this, "Unable to load renderable", ex)
                    }

                    null
                }

        // Set up a tap gesture detector.
        gestureDetector = GestureDetector(
                this,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        onSingleTap(e)
                        return true
                    }

                    override fun onDown(e: MotionEvent): Boolean {
                        return true
                    }
                })

        // Set a touch listener on the Scene to listen for taps.
        arSceneView!!
                .scene
                .setOnTouchListener { hitTestResult: HitTestResult, event: MotionEvent ->
                    // If the solar system hasn't been placed yet, detect a tap and then check to see if
                    // the tap occurred on an ARCore plane to place the solar system.
                    if (!hasPlacedSolarSystem) {
                        return@setOnTouchListener gestureDetector !!. onTouchEvent (event)
                    }

                    // Otherwise return false so that the touch event can propagate to the scene.
                    false
                }

        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        arSceneView!!
                .scene
                .addOnUpdateListener { frameTime ->
                    if (loadingMessageSnackbar == null) {
                        return@addOnUpdateListener
                    }

                    val frame = arSceneView!!.arFrame
                    if (frame == null) {
                        return@addOnUpdateListener
                    }

                    if (frame!!.camera.trackingState != TrackingState.TRACKING) {
                        return@addOnUpdateListener
                    }

                    for (plane in frame.getUpdatedTrackables<Plane>(Plane::class.java)) {
                        if (plane.trackingState == TrackingState.TRACKING) {
                            hideLoadingMessage()
                        }
                    }
                }

        // Lastly request CAMERA permission which is required by ARCore.
        DemoUtils.requestCameraPermission(this, RC_PERMISSIONS)
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

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, results: IntArray) {
        if (!DemoUtils.hasCameraPermission(this)) {
            if (!DemoUtils.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                DemoUtils.launchPermissionSettings(this)
            } else {
                Toast.makeText(
                        this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                        .show()
            }
            finish()
        }
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
