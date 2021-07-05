package org.geogebra.common.gui.view.table;

import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.geos.GeoList;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.kernelND.GeoEvaluatable;

public class TableValuesColumn {

	private final GeoEvaluatable evaluatable;
	private final GeoList xValues;

	private String headerName;
	private final Double[] doubleValues;
	private final String[] stringValues;

	public TableValuesColumn(GeoEvaluatable evaluatable, GeoList xValues) {
		this.evaluatable = evaluatable;
		this.xValues = xValues;

		doubleValues = new Double[xValues.size()];
		stringValues = new String[xValues.size()];
	}

	boolean isModifiable() {
		return evaluatable instanceof GeoList;
	}

	GeoEvaluatable getEvaluatable() {
		return evaluatable;
	}

	String getCellAt(int row) {
		String value = stringValues[row];
		if (value == null) {
			double doubleValue = getValueAt(row);
			value = format(doubleValue);
			stringValues[row] = value;
		}
		return value;
	}

	double getValueAt(int row) {
		Double value = doubleValues[row];
		if (value == null) {
			value = evaluateAt(row);
			doubleValues[row] = value;
		}
		return value;
	}

	String getHeaderName() {
		return headerName;
	}

	void updateHeaderName() {
		String labelSimple = evaluatable.getLabelSimple();
		headerName = evaluatable.isGeoList() ? labelSimple : labelSimple + "(x)";
	}

	public void setCell(int row) {
		stringValues[row] = null;
		doubleValues[row] = null;
		GeoNumeric cell = (GeoNumeric) ((GeoList) evaluatable).get(row);
		cell.setValue(cell.getValue() + 1); //TODO
	}

	private double evaluateAt(int row) {
		if (evaluatable.isGeoList()) {
			return evaluatable.value(row);
		}
		double x = xValues.get(row).evaluateDouble();
		return evaluatable.value(x);
	}

	private String format(double x) {
		return evaluatable.getKernel().format(x, StringTemplate.defaultTemplate);
	}
}
