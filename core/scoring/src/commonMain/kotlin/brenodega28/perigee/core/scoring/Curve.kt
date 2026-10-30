package brenodega28.perigee.core.scoring

class Curve(
    val budget: Double,
    val floor: Double,
) {
    fun score(value: Double): Double {
        if (budget == floor) return if (value == budget) 1.0 else 0.0
        val position = (value - floor) / (budget - floor)
        return position.coerceIn(0.0, 1.0)
    }

    companion object {
        fun of(spec: CurveSpec): Curve = Curve(spec.budget, spec.floor)
    }
}
