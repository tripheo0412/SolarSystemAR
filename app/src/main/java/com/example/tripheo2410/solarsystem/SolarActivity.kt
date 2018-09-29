package com.example.tripheo2410.solarsystem

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable

class SolarActivity : AppCompatActivity() {
    private val solarSettings = SolarSettings()
    companion object {
        private val RC_PERMISSIONS = 0x123

        // Astronomical units to meters ratio. Used for positioning the planets of the solar system.
        private val AU_TO_METERS = 0.5f
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solar)
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
}
