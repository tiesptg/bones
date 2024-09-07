package com.palisand.bones.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

public class TableLayout implements LayoutManager2 {
	private int[] widths;
	private int[] heights;
	private int[] preferredWidths;
	private int[] preferredHeights;
	private int gap = 2;
	private int margin = 2;
	private ArrayList<Pair> components = new ArrayList<>();
	private Container parent = null;
	private TableListener tableListener = new TableListener();
	private boolean fillSpace = true;
	
	@FunctionalInterface
	public interface Sizer {
		Dimension getDimension(Component component);
	}
	
	private class Pair {
		final Component component;
		final Constraints constraints;
		int left;
		int top;
		Dimension preferredSize;
		
		Pair(Component comp, Constraints constraints) {
			this.component = comp;
			this.constraints = constraints;
		}
	}
	
	
	public static class Constraints {
		private final int columns;
		private final int rows;
		
		public Constraints(int rows, int columns) {
			this.columns = columns;
			this.rows = rows;
		}
		
		public int getColumns() {
			return columns;
		}
		
		public int getRows() {
			return rows;
		}
		
		public int getNrOfCells() {
			return columns * rows;
		}
	}
	
	private class TableListener extends MouseMotionAdapter implements MouseListener {

		private Point start = null;
		private int column = -1;

		@Override
		public void mousePressed(MouseEvent e) {
			start = e.getPoint();
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			if (start != null && column != -1) {
				double diff = e.getX() - start.getX();
				if (fillSpace && diff > widths[column+1]) diff = widths[column+1];
				else if (diff < -widths[column]) diff = -widths[column];
				widths[column] += diff;
				if (fillSpace) widths[column +1] -= diff;
				start = e.getPoint();
				
				parent.invalidate();
				parent.validate();
				parent.repaint();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (start != null) {
				mouseDragged(e);
				start = null;
				column = -1;
				parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			if (start == null) {
				Insets insets = parent.getInsets();
				int left = insets.left + margin - gap/2;
				int len = fillSpace ? widths.length : widths.length + 1;
				for (int i = 1; i < len; ++i) {
					left += widths[i-1] + gap;
					if (e.getX() >= left - gap && e.getX() <= left + gap) {
						column = i-1;
						parent.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
						return;
					}
				}
				parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
		
		@Override
	    public void mouseMoved(MouseEvent e) {
	    	mouseEntered(e);
	    }


		@Override
		public void mouseExited(MouseEvent e) {
			if (start == null) {
				parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
		}

	}
	
	public int getColumnCount() {
		return widths.length;
	}

	
	public TableLayout(int nrOfColumns) {
		widths = new int[nrOfColumns];
		preferredWidths = new int[nrOfColumns];
	}
	
	public TableLayout(int nrOfColumns, int gap, int margin) {
		this(nrOfColumns);
		this.margin = margin;
		this.gap = gap;
	}
	
	private void setResizeListener(Component comp) {
		if (parent == null) {
			parent = comp.getParent();
			parent.addMouseListener(tableListener);
			parent.addMouseMotionListener(tableListener);
		}
	}

	@Override
	public void addLayoutComponent(String name, Component comp) {
		addLayoutComponent(comp, null);
	}

	@Override
	public void removeLayoutComponent(Component comp) {
		int row = -1;
		for (int i = 0; i < components.size(); ++i) {
			Pair pair = components.get(i);
			if (pair.component == comp) {
				row = pair.top;
				components.remove(i);
				break;
			}
		}
		// remove the whole row.
		if (row != -1) {
			for (int i = 0; i < components.size(); ++i) {
				Pair pair = components.get(i);
				if (pair.top == row) {
					components.remove(i);
				} else if (pair.top > row) {
					break;
				}
			}
			if (heights != null) heights = null;
			if (preferredHeights != null) preferredHeights = null;
		}
	}
	
	public void setFillSpace(boolean value) {
		fillSpace = value;
	}
	
	public boolean isSpaceFilled() {
		return fillSpace;
	}
	
	private Dimension layoutSize(Container parent, Sizer sizer) {
		ArrayList<Integer> hs = new ArrayList<>();
		int col = 0;
		int rowHeight = 0;
		Insets insets = parent.getInsets();
		ArrayList<Pair> moreCols = new ArrayList<>();
		ArrayList<Pair> moreRows = new ArrayList<>();
		for (Pair pair: components) {
			pair.preferredSize = pair.component.getPreferredSize();
			pair.left = col;
			pair.top = hs.size();
			if (pair.constraints.columns > 1) {
				moreCols.add(pair);
			} else if (pair.preferredSize.width > preferredWidths[col]){
				preferredWidths[col] = pair.preferredSize.width;
			}
			if (pair.constraints.rows > 1) {
				moreRows.add(pair);
			} else if (pair.preferredSize.height > rowHeight) {
				rowHeight = pair.preferredSize.height;
			}
			col += pair.constraints.columns;
			if (col >= preferredWidths.length) {
				col = 0;
				hs.add(rowHeight);
				rowHeight = 0;
			}
		}
		preferredHeights = hs.stream().flatMapToInt(num -> IntStream.of(num)).toArray();
		if (heights == null || heights.length != preferredHeights.length) {
			heights = new int[preferredHeights.length];
		}
		spread(preferredWidths,moreCols,pair -> pair.preferredSize.width,pair -> pair.left,pair -> pair.constraints.columns);
		spread(preferredHeights,moreRows,pair -> pair.preferredSize.height,pair -> pair.top,pair -> pair.constraints.rows);
		return new Dimension(insets.left + insets.right + sum(preferredWidths) + (preferredWidths.length-1) * gap + 2 * margin
				, insets.bottom + insets.top + sum(preferredHeights) + (preferredHeights.length-1) * gap + 2 * margin);
	}
	
	private int sum(int[] data) {
		int result = 0;
		for (int d: data) {
			result += d;
		}
		return result;
	}
	
	private void spread(int[] size, List<Pair> more,Function<Pair,Integer> getSize,Function<Pair,Integer> getStart, Function<Pair,Integer> getNr) {
		for (Pair pair: more) {
			int value = 0;
			int end = getStart.apply(pair) + getNr.apply(pair);
			for (int i = getStart.apply(pair); i < end; ++i) {
				value += size[i];
			}
			if (getSize.apply(pair) > value) {
				int extra = (getSize.apply(pair) - value) / getNr.apply(pair); 
				
				for (int i = getStart.apply(pair); i < end; ++i) {
					size[i] += extra;
				}
			}
		}
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		return layoutSize(parent,component -> component.getPreferredSize());
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		return layoutSize(parent,component -> component.getMinimumSize());
	}
	
	private void adjustSizes(int[] size,int[] preferredSize,int total,boolean fill) {
		int now = sum(size);
		int pref = sum(preferredSize);
		if (fill && now <= total) {
			// first make sure all have at least preferredsize
			for (int i = 0; i < size.length; ++i) {
				if (size[i] < preferredSize[i]) {
					size[i] = preferredSize[i];
				}
			}
			now = sum(size);
			if (now > total) {
				// when too large take it from the parts that have more space then preferred
				int diff = now - total;
				if (total > pref) {
					while (diff > 0) {
						int nf = 0;
						int smallest = Integer.MAX_VALUE;
						for (int i = 0; i < size.length; ++i) {
							int space = size[i] - preferredSize[i];
							if (space > 0) {
								nf++;
								if (smallest > space) {
									smallest = space;
								}
							}
						}
						if (smallest * nf > diff) {
							smallest = diff / nf;
						}
						for (int i = 0; i < size.length; ++i) {
							int space = size[i] - preferredSize[i];
							if (space > 0) {
								size[i] -= smallest;
							}
						}
						now = sum(size);
						diff = now - total;
					}
				} else {
					// not enough space for preferred
					diff = (pref - total + 1 /* needed otherwise rounding may lead to too many pixels */) / size.length;
					for (int i = 0; i < size.length; ++i) {
						size[i] = preferredSize[i] - diff;
					}
				}
				// other case just single pixel adjustments are done at the end
			} else if (now < total - size.length) {
				int diff = (total - now) / size.length;
				for (int i = 0; i < size.length; ++i) {
					size[i] += diff;
				}
			}
		} else if (now < total) {
			// first make sure all have at least preferredsize
			for (int i = 0; i < size.length; ++i) {
				if (size[i] < preferredSize[i]) {
					size[i] = preferredSize[i];
				}
			}
		} else if (now > total){
			// when too large take it from the parts that have more space then preferred
			int diff = now - total;
			if (total > pref) {
				while (diff > size.length) {
					int nf = 0;
					int smallest = Integer.MAX_VALUE;
					for (int i = 0; i < size.length; ++i) {
						int space = size[i] - preferredSize[i];
						if (space > 0) {
							nf++;
							if (smallest > space) {
								smallest = space;
							}
						}
					}
					if (smallest * nf > diff) {
						smallest = diff / nf;
					}
					for (int i = 0; i < size.length; ++i) {
						int space = size[i] - preferredSize[i];
						if (space > 0) {
							size[i] -= smallest;
						}
					}
					now = sum(size);
					diff = now - total;
				}
			} else {
				// not enough space for preferred
				diff = (pref - total + 1 /* needed otherwise rounding may lead to too many pixels */) / size.length;
				for (int i = 0; i < size.length; ++i) {
					size[i] = preferredSize[i] - diff;
				}
			}
		}
		int left = total - sum(size);
		if (fill && now < total) {
			// add left over pixels
			while (left-- > 0) {
				size[left]++;
			}
		}
	}

	@Override
	public void layoutContainer(Container parent) {
		synchronized (parent.getTreeLock()) {
			if (heights == null || preferredHeights == null) {
				preferredLayoutSize(parent);
			}
			Insets insets = parent.getInsets();
			Dimension size = parent.getSize();
			size.height -= insets.left + insets.right + 2 * margin;
			size.width -= insets.top + insets.bottom + 2 * margin;
			adjustSizes(widths,preferredWidths,size.width,fillSpace);
			adjustSizes(heights,preferredHeights,size.height,false);
			int[] lefts = new int[widths.length+1];
			int[] tops = new int[heights.length+1];
			lefts[0] = insets.left + margin;
			for (int i = 1; i <= widths.length; ++i) {
				lefts[i] = lefts[i-1] + widths[i-1] + gap;
			}
			tops[0] = insets.top + margin;
			for (int i = 1; i <= heights.length; ++i) {
				tops[i] = tops[i-1] + heights[i-1] + gap;
			}
			for (Pair pair: components) {
				pair.component.setBounds(lefts[pair.left],tops[pair.top]
						,lefts[pair.left + pair.constraints.columns] - lefts[pair.left] - gap
						,tops[pair.top + pair.constraints.rows] - tops[pair.top] - gap);
			}
		}
			
	}

	@Override
	public void addLayoutComponent(Component comp, Object constraints) {
		setResizeListener(comp);
		Constraints c = (Constraints)constraints;
		if (c == null) {
			c = new Constraints(1,1);
		}
		Pair pair = new Pair(comp,c);
		for (int i = parent.getComponentCount()-1; i >= 0 ; --i) {
			if (parent.getComponent(i) == comp) {
				components.add(i,pair);
				return;
			}
		}
		components.add(pair);
		if (heights != null) heights = null;
		if (preferredHeights != null) preferredHeights = null;
	}
	
	public static Constraints span(int rows, int cols) {
		return new Constraints(rows,cols);
	}

	@Override
	public Dimension maximumLayoutSize(Container target) {
		return new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE);
	}

	@Override
	public float getLayoutAlignmentX(Container target) {
		return 0.5f;
	}

	@Override
	public float getLayoutAlignmentY(Container target) {
		return 0.5f;
	}

	@Override
	public void invalidateLayout(Container target) {
	}

}
