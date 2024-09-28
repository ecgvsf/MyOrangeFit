package com.example.myorangefit.curveMenu

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import com.example.myorangefit.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class CurvedBottomNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {

    private val mPath: Path
    private val mPaint: Paint

    /** the radius represent the radius of the fab button  */
    private var radiusC: Int = 0
    private var radiusB: Int = 0
    // the coordinates of the first curve
    private val mFirstCurveStartPoint = Point()
    private val mFirstCurveEndPoint = Point()
    private val mFirstCurveControlPoint1 = Point()
    private val mFirstCurveControlPoint2 = Point()

    //the coordinates of the second curve
    private var mSecondCurveStartPoint = Point()
    private val mSecondCurveEndPoint = Point()
    private val mSecondCurveControlPoint1 = Point()
    private val mSecondCurveControlPoint2 = Point()

    // Navigation bar bounds (width & height)
    private var mNavigationBarWidth: Int = 0
    private var mNavigationBarHeight: Int = 0

    init {
        // Imposta l'elevation del CurvedBottomNavigationView se necessario
        elevation = 5f // Imposta un valore di elevazione
        // radius of fab button
        radiusB = (256 / 3)
        radiusC = (256 / 2)
        mPath = Path()
        mPaint = Paint()
        with(mPaint) {
            style = Paint.Style.FILL_AND_STROKE
            color = Color.DKGRAY
            setShadowLayer(30f, 0f, 0f, resources.getColor(R.color.opaqueBlack))
        }
        // Imposta il layer software per supportare l'ombra
        setLayerType(LAYER_TYPE_SOFTWARE, mPaint)

        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // get width and height of navigation bar
        mNavigationBarWidth = width
        mNavigationBarHeight = height

        //set var of start point
        val startY = mNavigationBarHeight/3f
        // the coordinates (x,y) of the start point before curve
        mFirstCurveStartPoint.set(mNavigationBarWidth / 2 - radiusB * 2 - radiusB / 3, startY.toInt())
        // the coordinates (x,y) of the end point after curve
        mFirstCurveEndPoint.set(mNavigationBarWidth / 2, mNavigationBarHeight/8)
        // same thing for the second curve
        mSecondCurveStartPoint = mFirstCurveEndPoint
        mSecondCurveEndPoint.set(mNavigationBarWidth / 2 + radiusB * 2 + radiusB / 3, startY.toInt())

        // the coordinates (x,y)  of the 1st control point on a cubic curve
        mFirstCurveControlPoint1.set(
            mFirstCurveStartPoint.x + radiusB + radiusB / 4,
            mFirstCurveStartPoint.y
        )
        // the coordinates (x,y)  of the 2nd control point on a cubic curve
        mFirstCurveControlPoint2.set(
            mFirstCurveEndPoint.x - radiusB * 2 + radiusB,
            mFirstCurveEndPoint.y
        )

        mSecondCurveControlPoint1.set(
            mSecondCurveStartPoint.x + radiusB * 2 - radiusB,
            mSecondCurveStartPoint.y
        )
        mSecondCurveControlPoint2.set(
            mSecondCurveEndPoint.x - (radiusB + radiusB / 4),
            mSecondCurveEndPoint.y
        )

        mPath.apply {
            reset()
            moveTo(0f, startY + radiusC)

            // 1 angolo
            arcTo(
                RectF(
                    0f,
                    startY,
                    radiusC.toFloat(),
                    startY + radiusC
                ), 180f, 90f
            )
            lineTo(mFirstCurveStartPoint.x.toFloat(), mFirstCurveStartPoint.y.toFloat())

            cubicTo(
                mFirstCurveControlPoint1.x.toFloat(), mFirstCurveControlPoint1.y.toFloat(),
                mFirstCurveControlPoint2.x.toFloat(), mFirstCurveControlPoint2.y.toFloat(),
                mFirstCurveEndPoint.x.toFloat(), mFirstCurveEndPoint.y.toFloat()
            )

            cubicTo(
                mSecondCurveControlPoint1.x.toFloat(), mSecondCurveControlPoint1.y.toFloat(),
                mSecondCurveControlPoint2.x.toFloat(), mSecondCurveControlPoint2.y.toFloat(),
                mSecondCurveEndPoint.x.toFloat(), mSecondCurveEndPoint.y.toFloat()
            )

            lineTo(mNavigationBarWidth.toFloat() - radiusC, startY)
            // 2 angolo
            arcTo(
                RectF(
                    mNavigationBarWidth.toFloat() - radiusC,
                    startY,
                    mNavigationBarWidth.toFloat(),
                    startY + radiusC
                ), 270f, 90f
            )

            lineTo(mNavigationBarWidth.toFloat(), (mNavigationBarHeight - radiusC).toFloat())
            // 3 angolo
            arcTo(
                RectF(
                    mNavigationBarWidth.toFloat() - radiusC,
                    mNavigationBarHeight.toFloat() - radiusC,
                    mNavigationBarWidth.toFloat(),
                    mNavigationBarHeight.toFloat()
                ), 0f, 90f
            )

            lineTo(radiusC.toFloat(), mNavigationBarHeight.toFloat())
            // 4 angolo
            arcTo(
                RectF(
                    0f,
                    mNavigationBarHeight.toFloat() - radiusC,
                    radiusC.toFloat(),
                    mNavigationBarHeight.toFloat()
                ), 90f, 90f
            )
            close()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(mPath, mPaint)
    }
}