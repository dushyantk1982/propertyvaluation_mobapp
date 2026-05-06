package com.propval.propertyvaluation
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.widget.ImageView


class CircularImageView  @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        val radius = Math.min(width, height) / 2f
        path.reset()
        path.addCircle(width / 2f, height / 2f, radius, Path.Direction.CCW)
        canvas.clipPath(path)
        super.onDraw(canvas)
    }
}