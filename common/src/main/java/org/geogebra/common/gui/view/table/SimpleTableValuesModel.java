package org.geogebra.common.gui.view.table;

import java.util.ArrayList;
import java.util.List;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoList;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.kernelND.GeoEvaluatable;

import com.google.j2objc.annotations.Weak;

/**
 * TableValuesModel implementation. Uses caching to store values.
 */
class SimpleTableValuesModel implements TableValuesModel {

	@Weak
	private Kernel kernel;

	private List<TableValuesListener> listeners;
	private List<TableValuesColumn> columns;
	private GeoList values;

	private boolean batchUpdate;

	/**
	 * Construct a SimpleTableValuesModel.
	 *
	 * @param kernel kernel
	 */
	SimpleTableValuesModel(Kernel kernel) {
		this.kernel = kernel;
		this.listeners = new ArrayList<>();

		this.columns = new ArrayList<>();
		this.values = new GeoList(kernel.getConstruction());

		this.batchUpdate = false;

		initializeModel();
	}

	@Override
	public void registerListener(TableValuesListener listener) {
		listeners.add(listener);
	}

	@Override
	public void unregisterListener(TableValuesListener listener) {
		listeners.remove(listener);
	}

	@Override
	public int getRowCount() {
		return values.size();
	}

	@Override
	public int getColumnCount() {
		return columns.size();
	}

	@Override
	public int getRealColumnCount() {
		return columns.size() + 1;
	}

	@Override
	public String getCellAt(int row, int column) {
		return columns.get(column).getCellAt(row);
	}

	/**
	 * @param row
	 *            row index
	 * @param column
	 *            column index
	 * @return function value
	 */
	double getValueAt(int row, int column) {
		return columns.get(column).getValueAt(row);
	}

	@Override
	public String getHeaderAt(int column) {
		return columns.get(column).getHeaderName();
	}

	@Override
	public void setCell(int row, int column) {
		TableValuesColumn col = columns.get(column);
		if (col.isModifiable()) {
			col.setCell(row);
			if (column == 0) {
				updateAllEvaluatables();
			}
			notifyDatasetChanged();
		}
	}

	/**
	 * Add an evaluatable to the model.
	 *
	 * @param evaluatable evaluatable
	 */
	void addEvaluatable(GeoEvaluatable evaluatable) {
		if (!evaluatables.contains(evaluatable)) {
			int idx = 0;
			while (idx < evaluatables.size() && evaluatables.get(idx)
					.getTableColumn() < evaluatable.getTableColumn()) {
				idx++;
			}
			evaluatables.add(idx, evaluatable);
			ensureIncreasingIndices(idx);
			int column = idx;
			columns.add(column, new String[values.size()]);
			doubleColumns.add(column, new Double[values.size()]);
			header.add(column, getHeaderName(evaluatable));
			notifyColumnAdded(evaluatable, column);
		}
	}

	private void ensureIncreasingIndices(int idx) {
		int lastColumn = evaluatables.get(idx).getTableColumn();
		for (int i = idx + 1; i < evaluatables.size(); i++) {
			if (evaluatables.get(i).getTableColumn() <= lastColumn) {
				lastColumn++;
				evaluatables.get(i).setTableColumn(lastColumn);
			}
		}
	}

	/**
	 * Remove an evaluatable from the model.
	 *
	 * @param evaluatable evaluatable
	 */
	void removeEvaluatable(GeoEvaluatable evaluatable) {
		if (evaluatables.contains(evaluatable)) {
			if (!kernel.getConstruction().isRemovingGeoToReplaceIt()) {
				evaluatable.setTableColumn(-1);
			}
			int index = evaluatables.indexOf(evaluatable);
			evaluatables.remove(evaluatable);
			int column = index + 1;
			columns.remove(column);
			doubleColumns.remove(column);
			for (int i = 0; i < evaluatables.size(); i++) {
				evaluatables.get(i).setTableColumn(i + 1);
			}
			notifyColumnRemoved(evaluatable, column);
		}
	}

	private void updateAllEvaluatables() {
		for (TableValuesColumn column : columns) {
			updateEvaluatable(column.getEvaluatable());
		}
	}

	/**
	 * Update the column for the Evaluatable object.
	 *
	 * @param evaluatable object to update in table
	 */
	void updateEvaluatable(GeoEvaluatable evaluatable) {
		if (evaluatables.contains(evaluatable)) {
			int index = evaluatables.indexOf(evaluatable);
			columns.set(index, new String[values.size()]);
			doubleColumns.set(index, new Double[values.size()]);
			notifyColumnChanged(evaluatable, index);
		}
	}

	/**
	 * Returns the index of the evaluatable in the model
	 * or -1 if it's not in the model.
	 *
	 * @param evaluatable object to check
	 * @return index of the object, -1 if it's not present
	 */
	int getEvaluatableIndex(GeoEvaluatable evaluatable) {
		return evaluatables.indexOf(evaluatable);
	}

	/**
	 * Get the evaluatable from the model.
	 *
	 * @param index index of the object
	 * @return evaluatable if present in the model
	 */
	GeoEvaluatable getEvaluatable(int index) {
		if (index < evaluatables.size() && index > -1) {
			return evaluatables.get(index);
		}
		return null;
	}

	/**
	 * Update the name of the Evaluatable object (if it has any)
	 *
	 * @param evaluatable the evaluatable object
	 */
	void updateEvaluatableName(GeoEvaluatable evaluatable) {
		if (evaluatables.contains(evaluatable)) {
			int index = evaluatables.indexOf(evaluatable);
			columns.get(index).updateHeaderName();
			notifyColumnHeaderChanged(evaluatable, index + 1);
		}
	}

	/**
	 * Set the x-values of the model.
	 *
	 * @param values x-values
	 */
	void setValues(double[] values) {
		this.values.clear();
		for (int i = 0; i < columns.size(); i++) {
			columns.set(i, new String[values.length]);
		}
		Double[] valuesColumn = new Double[values.length];
		for (int i = 0; i < values.length; i++) {
			valuesColumn[i] = values[i];
			this.values.add(new GeoNumeric(kernel.getConstruction(), values[i]));
		}
		doubleColumns.set(0, valuesColumn);
		for (int i = 1; i < doubleColumns.size(); i++) {
			doubleColumns.set(i, new Double[values.length]);
		}
		notifyDatasetChanged();
	}

	private void initializeModel() {
		columns.add(new TableValuesColumn(values, null));
	}

	/**
	 * Clears and initializes the model.
	 */
	void clearModel() {
		columns.clear();
		initializeModel();
	}

	/**
	 * Starts batch update.
	 * This batch update call cannot be nested.
	 */
	void startBatchUpdate() {
		batchUpdate = true;
	}

	/**
	 * Ends the batch update.
	 * Calls {@link TableValuesListener#notifyDatasetChanged(TableValuesModel)}.
	 */
	void endBatchUpdate() {
		batchUpdate = false;
		notifyDatasetChanged();
	}

	/**
	 * Get the x-values of the model.
	 *
	 * @return x-values
	 */
	GeoList getValues() {
		return values;
	}

	private void notifyColumnRemoved(GeoEvaluatable evaluatable, int column) {
		if (!batchUpdate) {
			for (TableValuesListener listener : listeners) {
				listener.notifyColumnRemoved(this, evaluatable, column);
			}
		}
	}

	private void notifyColumnAdded(GeoEvaluatable evaluatable, int column) {
		if (!batchUpdate) {
			for (TableValuesListener listener : listeners) {
				listener.notifyColumnAdded(this, evaluatable, column);
			}
		}
	}

	private void notifyColumnChanged(GeoEvaluatable evaluatable, int column) {
		if (!batchUpdate) {
			for (TableValuesListener listener : listeners) {
				listener.notifyColumnChanged(this, evaluatable, column);
			}
		}
	}

	private void notifyColumnHeaderChanged(GeoEvaluatable evaluatable, int column) {
		if (!batchUpdate) {
			for (TableValuesListener listener : listeners) {
				listener.notifyColumnHeaderChanged(this, evaluatable, column);
			}
		}
	}

	private void notifyDatasetChanged() {
		if (!batchUpdate) {
			for (TableValuesListener listener : listeners) {
				listener.notifyDatasetChanged(this);
			}
		}
	}
}
