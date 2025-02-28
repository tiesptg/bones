package com.palisand.bones.ui;

import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.w3c.dom.Node;

import com.palisand.bones.tt.Rules.ConstraintViolation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProblemsModel extends AbstractTableModel {
	private static final long serialVersionUID = 8958409337125976381L;
	private List<ConstraintViolation> problems = Collections.emptyList();

	@Override
	public int getColumnCount() {
		return 4;
	}
	
	@Override
	public String getColumnName(int columnIndex) {
		switch (columnIndex) {
		case 0: return "Severity";
		case 1: return "Object";
		case 2: return "Field";
		case 3: return "Message";
		}
		return null;
		
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 1) {
			return Node.class;
		}
		return String.class;
	}

	@Override
	public int getRowCount() {
		return problems.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
		ConstraintViolation violation = problems.get(rowIndex);
		switch (columnIndex) {
		case 0: return violation.severity();
		case 1: return violation.node();
		case 2: return violation.field();
		case 3: return violation.message();
		}
		return null;
	}

}
