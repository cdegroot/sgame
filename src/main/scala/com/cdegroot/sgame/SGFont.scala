package com.cdegroot.sgame

// TODO fix comments
/** A generic font specification for fonts on the different
* platforms.  It is based on the JRE platform, other platforms should do their
* best to translate it to something equivalent or use sensible defaults if
* there is nothing comparable.
* 
* The font name can be a logical font name or a font face name. A
	 * logical name must be either: Dialog, DialogInput, Monospaced, Serif, or
	 * SansSerif. If name is null, the name of the new Font is set to the name
	 * "Default". */

class SGFont(size: Double, name: String = "Default", style: SGFontStyle.Value = SGFontStyle.Plain) 

/**
 * Font styles
 */
object SGFontStyle extends Enumeration {
	val Plain, Bold, Italic = Value
}
