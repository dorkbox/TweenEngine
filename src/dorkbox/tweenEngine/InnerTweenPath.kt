package dorkbox.tweenEngine

class InnerTweenPath(val computeFunction: (Float, FloatArray, Int) -> Float) : TweenPath {
    override fun compute(tweenValue: Float, points: FloatArray, pointsCount: Int): Float {
        return computeFunction(tweenValue, points, pointsCount)
    }
}
