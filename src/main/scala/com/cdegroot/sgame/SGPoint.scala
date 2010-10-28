package com.cdegroot.sgame

/**
 * This class is a very minimal replacement of java.awt.Point
 */
case class SGPoint(x: Int, y: Int) {
	override def toString = "SGPoint(" + x + "," + y + ")"
	
	// Helper methods to make client code more readable when point is used as dimension
	def width = x
	def height = y
	
}

// Companion object for some syntactic sugar
object SGPoint {
	def apply(x: Double, y: Double): SGPoint = SGPoint(x.toInt, y.toInt)
	def apply(point: SGPoint): SGPoint = SGPoint(point.x, point.y)
	
	val None = SGPoint(0, 0)
}