package org.geogebra.web.html5.main;

import org.geogebra.common.io.MyXMLio;
import org.geogebra.web.html5.util.ArchiveEntry;

/**
 * Class to separate ggbFile to parts.
 *
 * @author laszlo
 */
public class GgbArchive {
	private final ArchiveEntry construction;
	private final ArchiveEntry macros;
	private final ArchiveEntry defaults2d;
	private final ArchiveEntry defaults3d;

	/**
	 *
	 * @param ggbFile the ggb file
	 * @param is3D if app is 3D or not.
	 */
	public GgbArchive(GgbFile ggbFile, boolean is3D) {
		construction = ggbFile.remove(MyXMLio.XML_FILE);
		macros = ggbFile.remove(MyXMLio.XML_FILE_MACRO);
		defaults2d = ggbFile.remove(MyXMLio.XML_FILE_DEFAULTS_2D);
		defaults3d = is3D
				? ggbFile.remove(MyXMLio.XML_FILE_DEFAULTS_3D) : null;

	}

	/**
	 *
	 * @return if archive is valid
	 */
	public boolean isInvalid() {
		return construction == null && macros == null;
	}

	/**
	 *
	 * @return if construction exists
	 */
	public boolean hasConstruction() {
		return construction != null;
	}

	/**
	 *
	 * @return if archive has macros
	 */
	public boolean hasMacros() {
		return macros != null;
	}

	/**
	 *
	 * @return the construction
	 */
	public String getConstruction() {
		return asString(construction);
	}

	private String asString(ArchiveEntry arg) {
		return arg == null ? null : arg.string;
	}

	/**
	 *
	 * @return the macros
	 */
	public String getMacros() {
		return asString(macros);
	}

	/**
	 *
	 * @return defaults2d
	 */
	public String getDefaults2d() {
		return asString(defaults2d);
	}

	/**
	 *
	 * @return defaults3d
	 */
	public String getDefaults3d() {
		return asString(defaults3d);
	}

	/**
	 *
	 * @return if archive has defaults2d
	 */
	public boolean hasDefaults2d() {
		return defaults2d != null;
	}

	/**
	 *
	 * @return if archive has defaults3d
	 */
	public boolean hasDefaults3d() {
		return defaults3d != null;
	}
}
