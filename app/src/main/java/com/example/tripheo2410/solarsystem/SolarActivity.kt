package com.example.tripheo2410.solarsystem

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable

class SolarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solar)
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
