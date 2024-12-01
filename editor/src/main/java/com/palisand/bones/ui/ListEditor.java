package com.palisand.bones.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

import javax.swing.*;

import lombok.Getter;

public abstract class ListEditor<X> extends JDialog {

	private static final long serialVersionUID = 357627830762461307L;
	
	public static class StringListEditor extends ListEditor<String> {
		
		private static final long serialVersionUID = 1319883864175847127L;
		private final JTextField field = new JTextField();
		
		public StringListEditor(JFrame frame,String title) {
			super(frame,title);
			field.addFocusListener(new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent e) {
					field.selectAll();
				}
			});
		}

		@Override
		public String getValue() {
			return field.getText();
		}

		@Override
		public void setValue(String x) {
			field.setText(x);
		}

		@Override
		public JComponent getEditor() {
			return field;
		}
	}
	
	public static class NumberListEditor extends ListEditor<Number> {
		
		private static final long serialVersionUID = 1319883864175847127L;
		private JSpinner field = new JSpinner();
		
		public NumberListEditor(JFrame frame,String title) {
			super(frame,title);
			field.setModel(new SpinnerNumberModel());
		}

		@Override
		public Number getValue() {
			return (Number)field.getValue();
		}

		@Override
		public void setValue(Number x) {
			field.setValue(x);
		}

		@Override
		public JComponent getEditor() {
			return field;
		}
	}
	
	private JList<X> list = new JList<>();
	private ListListModel<X> listModel;
	private boolean accepted = false;
	
	@SuppressWarnings("unchecked")
	public static <X> ListEditor<X> dialogFor(JFrame frame, String title, Class<X> cls) {
		if (cls == String.class) {
			return (ListEditor<X>)new StringListEditor(frame,title);
		}
		if (Number.class.isAssignableFrom(cls)) {
			return (ListEditor<X>)new NumberListEditor(frame,title);
		}
		return null;
	}
	
	class ListListModel<Y> extends AbstractListModel<Y> {

		private static final long serialVersionUID = 7116486288125008937L;
		@Getter private final List<Y> list;
		
		ListListModel(List<Y> list) {
			this.list = list;
		}

		@Override
		public int getSize() {
			return list.size();
		}

		@Override
		public Y getElementAt(int index) {
			return list.get(index);
		}

		public void doAdd(Y value) {
			if (value != null) {
				list.add(value);
				fireIntervalAdded(this, list.size()-1, list.size()-1);
				setValue(null);
				selectItem(list.size()-1);
				getEditor().requestFocus();
			}
		}

		public void doRemove(int selectedIndex) {
			list.remove(selectedIndex);
			fireIntervalRemoved(this, selectedIndex, selectedIndex);
		}

		public void doUp(int selectedIndex) {
			if (selectedIndex > 0) {
				list.add(selectedIndex-1,list.remove(selectedIndex));
				fireContentsChanged(list, selectedIndex-1, selectedIndex);
				selectItem(selectedIndex-1);
			}
		}

		public void doDown(int selectedIndex) {
			if (selectedIndex != -1 && selectedIndex < list.size()-1) {
				list.add(selectedIndex+1,list.remove(selectedIndex));
				fireContentsChanged(list, selectedIndex, selectedIndex+1);
				selectItem(selectedIndex+1);
			}
		}
		
		private void selectItem(int index) {
			ListEditor.this.list.setSelectedIndex(index);
			ListEditor.this.list.ensureIndexIsVisible(index);
		}

		public void update(Y value, int selectedIndex) {
			if (value != null) {
				list.set(selectedIndex, value);
				fireContentsChanged(this, selectedIndex, selectedIndex);
			}
		}
		
	}
	
	public ListEditor(JFrame parent, String title) {
		super(parent,title,true);
	}
	
	private void init() {
		setLayout(new BoxLayout(getContentPane(),BoxLayout.Y_AXIS));
		getRootPane().setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		JPanel top = new JPanel();
		top.setLayout(new BorderLayout(4,4));
		add(top);
		top.add(new JScrollPane(list),BorderLayout.CENTER);
		list.addListSelectionListener(e -> selectItem());
		JPanel buttons = new JPanel();
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(buttons, BorderLayout.NORTH);
		buttons.setLayout(new GridLayout(5,1));
		top.add(panel,BorderLayout.EAST);
		JButton add = new JButton("+");
		buttons.add(add);
		add.addActionListener(e -> listModel.doAdd(getValue()));
		JButton remove = new JButton("-");
		buttons.add(remove);
		remove.addActionListener(e -> listModel.doRemove(list.getSelectedIndex()));
		JButton update = new JButton("=");
		buttons.add(update);
		update.addActionListener(e -> listModel.update(getValue(),list.getSelectedIndex()));
		JButton up = new JButton("^");
		buttons.add(up);
		up.addActionListener(e -> listModel.doUp(list.getSelectedIndex()));
		JButton down = new JButton("v");
		buttons.add(down);
		down.addActionListener(e -> listModel.doDown(list.getSelectedIndex()));
		top.add(getEditor(),BorderLayout.SOUTH);
		JPanel bottom = new JPanel();
		bottom.setLayout(new GridLayout(1,2));
		add(bottom);
		JButton cancel = new JButton("Cancel");
		bottom.add(cancel);
		cancel.addActionListener(e -> setVisible(false));
		JButton ok = new JButton("OK");
		bottom.add(ok);
		ok.addActionListener(e -> {
			accepted = true;
			setVisible(false);
		});
		setSize(300,260);
		Rectangle rect = getParent().getBounds();
		setLocation((rect.width - getWidth())/2+rect.x,(rect.height-getHeight())/2+rect.y);
	}
	
	public abstract X getValue();
	public abstract void setValue(X x);
	public abstract JComponent getEditor();
	
	private void selectItem() {
		setValue(list.getSelectedValue());
	}
	
	public boolean editData(List<X> data) {
		init();
		listModel = new ListListModel<>(data);
		list.setModel(listModel);
		list.setSelectedIndex(0);
		setVisible(true);
		return accepted;
	}
	
	public List<X> getData() {
		return ((ListListModel<X>)list.getModel()).getList();
	}


}
