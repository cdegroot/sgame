package com.cdegroot.sgame

/**
 * Minimal replacement for AWT Color class
 */
case class SGColor(red: Int, green: Int, blue: Int, alpha: Int = 255)


/**
 * Some predefined colors
 */
object SGColor {	
	val Black  = SGColor(0,0,0)
	val White  = SGColor(255,255,255)
	val Yellow = SGColor(255,255,0)
	val Green  = SGColor(0,255,0)
	val Cyan   = SGColor(0,255,255)
	val Blue   = SGColor(0,0,255)
	val Magenta= SGColor(255,0,255)
	val Red    = SGColor(255,0,0)
	val Pink   = SGColor(255,140,140)
	val Orange = SGColor(255,140,0)

	/**
	 * Alternative constructor with Double values between 0.0 and 1.0
	 */
	def apply(r: Double, g: Double, b: Double, a: Double): SGColor = new SGColor(
			(r * 255.95).toInt, 
			(g * 255.95).toInt, 
			(b * 255.95).toInt, 
			(a * 255.95).toInt)
	
	def apply(r: Double, g: Double, b: Double): SGColor = apply(r, g, b, 1.0)
	
}
