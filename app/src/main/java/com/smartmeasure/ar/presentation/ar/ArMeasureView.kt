package com.smartmeasure.ar.presentation.ar

import android.app.Activity
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.Surface
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt

class ArMeasureView(
    private val activity: Activity,
    private val listener: Listener,
) : GLSurfaceView(activity) {
    interface Listener {
        fun onSessionReady(depthEnabled: Boolean)
        fun onTrackingChanged(tracking: Boolean)
        fun onPointCaptured(pointCount: Int, distanceMeters: Double?)
        fun onNoSurface()
        fun onSessionError()
    }

    private val renderer = ArRenderer()
    private var session: Session? = null
    private var sessionResumed = false

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun onHostResume() {
        if (sessionResumed) return

        try {
            val activeSession = session ?: Session(activity).also { newSession ->
                val config = newSession.config.apply {
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                }
                val depthEnabled = newSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
                if (depthEnabled) config.depthMode = Config.DepthMode.AUTOMATIC
                newSession.configure(config)
                session = newSession
                post { listener.onSessionReady(depthEnabled) }
            }

            activeSession.resume()
            sessionResumed = true
            super.onResume()
        } catch (_: Exception) {
            post { listener.onSessionError() }
        }
    }

    fun onHostPause() {
        if (!sessionResumed) return
        super.onPause()
        session?.pause()
        sessionResumed = false
    }

    fun close() {
        onHostPause()
        renderer.detachAnchors()
        session?.close()
        session = null
    }

    fun capturePoint() {
        renderer.captureRequested.set(true)
    }

    fun resetMeasurement() {
        queueEvent { renderer.resetMeasurement() }
    }

    private inner class ArRenderer : Renderer {
        val captureRequested = AtomicBoolean(false)
        private val backgroundRenderer = CameraBackgroundRenderer()
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var firstAnchor: Anchor? = null
        private var secondAnchor: Anchor? = null
        private var lastTracking: Boolean? = null

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            backgroundRenderer.createOnGlThread()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            surfaceWidth = width
            surfaceHeight = height
            GLES20.glViewport(0, 0, width, height)
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            val activeSession = session ?: return
            if (!sessionResumed || backgroundRenderer.textureId == 0) return

            try {
                activeSession.setCameraTextureName(backgroundRenderer.textureId)
                activeSession.setDisplayGeometry(
                    // View#getDisplay works from API 17; Context#getDisplay would need API 30.
                    this@ArMeasureView.display?.rotation ?: Surface.ROTATION_0,
                    surfaceWidth,
                    surfaceHeight,
                )

                val frame = activeSession.update()
                backgroundRenderer.draw(frame)

                val tracking = frame.camera.trackingState == TrackingState.TRACKING
                if (tracking != lastTracking) {
                    lastTracking = tracking
                    post { listener.onTrackingChanged(tracking) }
                }

                if (captureRequested.getAndSet(false)) {
                    captureCenter(frame, tracking)
                }
            } catch (_: Exception) {
                post { listener.onSessionError() }
            }
        }

        private fun captureCenter(frame: Frame, tracking: Boolean) {
            if (!tracking || surfaceWidth <= 0 || surfaceHeight <= 0) {
                post { listener.onNoSurface() }
                return
            }

            val hit = frame.hitTest(surfaceWidth / 2f, surfaceHeight / 2f)
                .firstOrNull { result ->
                    when (val trackable = result.trackable) {
                        is Plane -> trackable.isPoseInPolygon(result.hitPose)
                        is Point -> trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
                        is DepthPoint -> true
                        else -> false
                    }
                }

            if (hit == null) {
                post { listener.onNoSurface() }
                return
            }

            if (firstAnchor != null && secondAnchor != null) {
                detachAnchors()
            }

            if (firstAnchor == null) {
                firstAnchor = hit.createAnchor()
                post { listener.onPointCaptured(1, null) }
                return
            }

            secondAnchor = hit.createAnchor()
            val distance = distanceBetween(firstAnchor!!, secondAnchor!!)
            post { listener.onPointCaptured(2, distance) }
        }

        fun resetMeasurement() {
            detachAnchors()
            post { listener.onPointCaptured(0, null) }
        }

        fun detachAnchors() {
            firstAnchor?.detach()
            secondAnchor?.detach()
            firstAnchor = null
            secondAnchor = null
        }

        private fun distanceBetween(first: Anchor, second: Anchor): Double {
            val a = first.pose
            val b = second.pose
            val dx = (a.tx() - b.tx()).toDouble()
            val dy = (a.ty() - b.ty()).toDouble()
            val dz = (a.tz() - b.tz()).toDouble()
            return sqrt(dx * dx + dy * dy + dz * dz)
        }
    }
}

private class CameraBackgroundRenderer {
    var textureId: Int = 0
        private set

    private var program = 0
    private var positionAttribute = 0
    private var texCoordAttribute = 0
    private var textureUniform = 0

    private val quadCoords = floatBufferOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f,
    )
    private val transformedTexCoords = floatBufferOf(
        0f, 0f,
        0f, 0f,
        0f, 0f,
        0f, 0f,
    )

    fun createOnGlThread() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE,
        )

        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionAttribute = GLES20.glGetAttribLocation(program, "a_Position")
        texCoordAttribute = GLES20.glGetAttribLocation(program, "a_TexCoord")
        textureUniform = GLES20.glGetUniformLocation(program, "u_Texture")
    }

    fun draw(frame: Frame) {
        if (frame.hasDisplayGeometryChanged()) {
            quadCoords.position(0)
            transformedTexCoords.position(0)
            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                quadCoords,
                Coordinates2d.TEXTURE_NORMALIZED,
                transformedTexCoords,
            )
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glUseProgram(program)

        quadCoords.position(0)
        transformedTexCoords.position(0)
        GLES20.glVertexAttribPointer(positionAttribute, 2, GLES20.GL_FLOAT, false, 0, quadCoords)
        GLES20.glVertexAttribPointer(
            texCoordAttribute,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            transformedTexCoords,
        )
        GLES20.glEnableVertexAttribArray(positionAttribute)
        GLES20.glEnableVertexAttribArray(texCoordAttribute)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(textureUniform, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionAttribute)
        GLES20.glDisableVertexAttribArray(texCoordAttribute)
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        return GLES20.glCreateProgram().also { programId ->
            GLES20.glAttachShader(programId, vertexShader)
            GLES20.glAttachShader(programId, fragmentShader)
            GLES20.glLinkProgram(programId)
            val status = IntArray(1)
            GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, status, 0)
            check(status[0] == GLES20.GL_TRUE) {
                "Camera shader link failed: ${GLES20.glGetProgramInfoLog(programId)}"
            }
        }
    }

    private fun compileShader(type: Int, source: String): Int =
        GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val status = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
            check(status[0] == GLES20.GL_TRUE) {
                "Camera shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}"
            }
        }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES u_Texture;
            varying vec2 v_TexCoord;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """
    }
}

private fun floatBufferOf(vararg values: Float): FloatBuffer =
    ByteBuffer.allocateDirect(values.size * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(values)
            position(0)
        }
