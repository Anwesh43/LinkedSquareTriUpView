package com.anwesh.uiprojects.squaretriupview

/**
 * Created by anweshmishra on 22/04/19.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.content.Context
import android.app.Activity

val nodes : Int = 5
val squares : Int = 2
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#673AB7")
val backColor : Int = Color.parseColor("#BDBDBD")
val sFactor : Float = 4.5f
val delay : Long = 20

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * scGap * dir

fun Canvas.drawSquareUp(j : Int, sc : Float, gap : Float, size : Float, paint : Paint) {
    val scj : Float = sc.divideScale(j, squares)
    var prevX : Float = 0f
    var prevY : Float = 0f

    if (scj > 0) {
        prevX = gap * j
        prevY = -gap * j
    }
    save()
    translate(prevX + gap * scj, prevY - gap * scj * (1 - 2 * j))
    drawRect(RectF(-size, -size, size, size), paint)
    restore()
}

fun Canvas.drawSTUNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val size : Float = gap / sizeFactor
    val squareSize : Float = size / sFactor
    paint.color = foreColor
    save()
    translate(w / 2, gap * (i + 1))
    for (j in 0..1) {
        save()
        rotate(180f * j * sc2)
        translate(-size, 0f)
        drawRect(RectF(-squareSize, -squareSize, squareSize, squareSize), paint)
        for (k in 0..(squares - 1)) {
            drawSquareUp(k, sc1, size, squareSize, paint)
        }
        restore()
    }
    restore()
}

class SquareTriUpView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, squares, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class STUNode(var i : Int, val state : State = State()) {

        private var next : STUNode?= null
        private var prev : STUNode?= null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = STUNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawSTUNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : STUNode {
            var curr : STUNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class SquareTriUp(var i : Int) {

        private val root : STUNode = STUNode(0)
        private var curr : STUNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : SquareTriUpView) {

        private val animator : Animator = Animator(view)
        private val sup : SquareTriUp = SquareTriUp(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            sup.draw(canvas, paint)
            animator.animate {
                sup.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            sup.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity: Activity) : SquareTriUpView {
            val view : SquareTriUpView = SquareTriUpView(activity)
            activity.setContentView(view)
            return view
        }
    }
}