package com.palisand.bones.persist.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import lombok.RequiredArgsConstructor;

@SuppressWarnings("serial")
public class SqlQueryTool extends JFrame implements MouseListener {
  private final JTable table = new JTable();
  private final JTree tree = new JTree();
  private final JTextArea sql = new JTextArea();
  private final JButton connect = new JButton("Connect");
  private final JButton execute = new JButton("Execute");
  private final JComboBox<String> url = new JComboBox<>();
  private final JTextField user = new JTextField("ties");
  private final JTextField password = new JTextField("ties");
  private Connection connection = null;
  private final List<ConnectionData> connectionData = new ArrayList<>();

  private record ConnectionData(String url, String user, String password) {

    @Override
    public String toString() {
      return url() + "|" + user() + "|" + password();
    }
  }

  @RequiredArgsConstructor
  private class DataTableModel extends AbstractTableModel {
    private final String[] colNames;
    private final List<Object[]> rows;

    @Override
    public String getColumnName(int columnIndex) {
      return colNames[columnIndex];
    }

    @Override
    public int getRowCount() {
      return rows.size();
    }

    @Override
    public int getColumnCount() {
      return colNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      return rows.get(rowIndex)[columnIndex];
    }

  }

  private class DbTreeModel implements TreeModel {
    private static final String[] DETAILS = {"Columns", "Primary Key", "Foreign Keys", "Indices"};
    private Object root = null;
    private List<StringBuilder> tables = new ArrayList<>();

    @Override
    public Object getRoot() {
      if (root == null && connection != null) {
        try {
          root = connection.getSchema();
          if (root == null) {
            root = connection.getCatalog();
          }
          if (root == null) {
            root = url.getSelectedItem();
          }
          DatabaseMetaData md = connection.getMetaData();
          ResultSet rs = md.getTables(connection.getCatalog(), connection.getSchema(), null,
              new String[] {"TABLE"});
          while (rs.next()) {
            tables.add(new StringBuilder(rs.getString("TABLE_NAME")));
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
      return root;
    }

    @Override
    public Object getChild(Object parent, int index) {
      if (parent.equals(root)) {
        return tables.get(index);
      }
      if (parent instanceof StringBuilder) {
        return DETAILS[index];
      }
      return null;
    }

    @Override
    public int getChildCount(Object parent) {
      if (parent.equals(root)) {
        return tables.size();
      }
      if (parent instanceof StringBuilder) {
        return 4;
      }
      return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
      return getChildCount(node) == 0;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
      // ignore
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
      if (parent.equals(root)) {
        return tables.indexOf(child);
      }
      if (parent instanceof StringBuilder) {
        return Arrays.asList(DETAILS).indexOf(child);
      }
      return -1;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
      // ignore
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
      // ignore
    }

  }

  private SqlQueryTool() {
    super("Bones Simple JDBC Query Tool");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    addComponentListener(new ComponentAdapter() {

      @Override
      public void componentResized(ComponentEvent e) {
        saveConfig();
      }

      @Override
      public void componentMoved(ComponentEvent e) {
        saveConfig();
      }

    });
    JSplitPane treePane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    treePane.setDividerLocation(150);
    add(treePane);
    treePane.setLeftComponent(new JScrollPane(tree));
    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitPane.setDividerLocation(150);
    treePane.setRightComponent(splitPane);
    tree.addMouseListener(this);
    tree.setModel(new DbTreeModel());
    JPanel top = new JPanel();
    splitPane.setTopComponent(top);
    top.setLayout(new BorderLayout());
    JPanel connectPanel = new JPanel();
    top.add(connectPanel, BorderLayout.NORTH);
    sql.setRows(10);
    top.add(new JScrollPane(sql), BorderLayout.CENTER);
    connectPanel.setLayout(new BoxLayout(connectPanel, BoxLayout.X_AXIS));
    connectPanel.add(new JLabel("URL"));
    url.setPreferredSize(new Dimension(500, url.getPreferredSize().height));
    connectPanel.add(url);
    url.setEditable(true);
    url.addItemListener(e -> {
      if (e.getID() == ItemEvent.SELECTED) {
        selectUrl((String) e.getItem());
      }
    });
    connectPanel.add(new JLabel("User"));
    connectPanel.add(user);
    connectPanel.add(new JLabel("Password"));
    connectPanel.add(password);
    connectPanel.add(connect);
    connectPanel.add(execute);

    execute.setEnabled(false);
    splitPane.setBottomComponent(new JScrollPane(table));
    table.setFillsViewportHeight(true);
    connect.addActionListener(e -> toggleConnection());
    execute.addActionListener(e -> executeQuery());
    setBounds(100, 100, 800, 600);
    loadConfig();
    setVisible(true);
  }

  private void selectUrl(String data) {
    int i = url.getSelectedIndex();
    ConnectionData cd = connectionData.remove(i);
    user.setText(cd.user());
    password.setText(cd.password());
    connectionData.add(0, cd);
  }

  public void loadConfig() {
    File file = new File("sqlquerytool.properties");
    if (file.exists()) {
      try (FileReader in = new FileReader(file)) {
        Properties properties = new Properties();
        properties.load(in);
        setBounds(Integer.valueOf(properties.getProperty("x", "100")),
            Integer.valueOf(properties.getProperty("y", "100")),
            Integer.valueOf(properties.getProperty("width", "800")),
            Integer.valueOf(properties.getProperty("height", "600")));
        int index = 0;
        for (String data = properties.getProperty("connection." + index); data != null; data =
            properties.getProperty("connection." + (++index))) {
          String[] parts = data.split("\\|");
          if (parts.length == 3) {
            connectionData.add(new ConnectionData(parts[0], parts[1], parts[2]));
          }
        }
        if (connectionData.isEmpty()) {
          url.removeAllItems();
          user.setText("");
          password.setText("");
        } else {
          for (ConnectionData data : connectionData) {
            url.addItem(data.url());
          }
          ConnectionData data = connectionData.get(0);
          url.setSelectedItem(data.url());
          user.setText(data.user());
          password.setText(data.password());
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  public void saveConfig() {
    File file = new File("sqlquerytool.properties");
    try (FileWriter out = new FileWriter(file)) {
      Properties properties = new Properties();
      properties.setProperty("x", Integer.toString(getBounds().x));
      properties.setProperty("y", Integer.toString(getBounds().y));
      properties.setProperty("width", Integer.toString(getBounds().width));
      properties.setProperty("height", Integer.toString(getBounds().height));
      int index = 0;
      for (ConnectionData data : new ArrayList<>(connectionData)) {
        properties.setProperty("connection." + (index++), data.toString());
      }
      properties.store(out, "Simple SQL Query Tool");
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private void loadDriver(String urlstr) throws ClassNotFoundException {
    String className = null;
    if (urlstr.startsWith("jdbc:h2:")) {
      className = "org.h2.Driver";
    } else if (urlstr.startsWith("jdbc:postgresql")) {
      className = "org.postgresql.Driver";
    } else if (urlstr.startsWith("jdbc:oracle:thin:")) {
      className = "oracle.jdbc.driver.OracleDriver";
    } else if (urlstr.startsWith("jdbc:mysql:")) {
      className = "com.mysql.cj.jdbc.Driver";
    }
    Class.forName(className);
  }

  private void toggleConnection() {
    if (connect.getText().equals("Connect")) {
      connect();
    } else {
      disconnect();
    }
  }

  private void connect() {
    try {
      loadDriver((String) url.getSelectedItem());
      connection = DriverManager.getConnection((String) url.getSelectedItem(), user.getText(),
          password.getText());
      connection.setAutoCommit(true);
      connect.setText("Disconnect");
      url.setEnabled(false);
      user.setEnabled(false);
      password.setEnabled(false);
      execute.setEnabled(true);
      tree.setModel(new DbTreeModel());
      if (connectionData.stream().map(cd -> cd.url)
          .noneMatch(str -> str.equals(url.getSelectedItem()))) {
        connectionData.add(
            new ConnectionData((String) url.getSelectedItem(), user.getText(), password.getText()));
        url.insertItemAt((String) url.getSelectedItem(), 0);
        while (connectionData.size() > 5) {
          connectionData.remove(connectionData.size() - 1);
        }
      } else {
        ConnectionData cd = connectionData.remove(url.getSelectedIndex());
        connectionData.add(0, cd);
        url.removeAllItems();
        for (ConnectionData data : connectionData) {
          url.addItem(data.url());
        }
      }
      saveConfig();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private void disconnect() {
    try {
      connection = null;
      connect.setText("Connect");
      url.setEnabled(true);
      user.setEnabled(true);
      password.setEnabled(true);
      execute.setEnabled(false);
      tree.setModel(null);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private void executeQuery() {
    try {
      if (sql.getText().trim().toLowerCase().startsWith("select")) {
        try (ResultSet rs = connection.createStatement().executeQuery(sql.getText())) {
          showResult(rs);
          rs.close();
        }
      } else {
        int count = connection.createStatement().executeUpdate(sql.getText());
        table.setModel(new DataTableModel(new String[] {"Rows affected"},
            Collections.singletonList(new Object[] {count})));
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
      List<Object[]> rows = new ArrayList<>();
      while (ex != null) {
        rows.add(new Object[] {ex.toString()});
        Throwable cause = ex.getCause();
        while (cause != null) {
          rows.add(new Object[] {cause.toString()});
          cause = cause.getCause();
        }
        ex = ex.getNextException();
      }
      table.setModel(new DataTableModel(new String[] {"ERROR"}, rows));
    }
  }

  private void showResult(ResultSet rs) throws SQLException {
    List<Object[]> rows = new ArrayList<>();
    ResultSetMetaData md = rs.getMetaData();
    String[] colNames = new String[md.getColumnCount()];
    for (int i = 0; i < colNames.length; ++i) {
      colNames[i] = md.getColumnName(i + 1);
    }
    while (rs.next()) {
      if (colNames == null) {
        md = rs.getMetaData();
        colNames = new String[md.getColumnCount()];
        for (int i = 0; i < colNames.length; ++i) {
          colNames[i] = md.getColumnName(i + 1);
        }
      }
      Object[] row = new Object[md.getColumnCount()];
      for (int i = 1; i <= md.getColumnCount(); ++i) {
        row[i - 1] = rs.getObject(i);
        if (rs.wasNull()) {
          row[i - 1] = null;
        }
      }
      rows.add(row);
      if (rows.size() >= 100) {
        break;
      }
    }
    table.setModel(new DataTableModel(colNames, rows));
  }


  public static void main(String... args) {
    new SqlQueryTool();
  }

  @Override
  public void mouseClicked(MouseEvent e) {}

  @Override
  public void mousePressed(MouseEvent e) {
    if (e.getClickCount() >= 2) {
      TreePath selection = tree.getPathForLocation(e.getX(), e.getY());
      if (!selection.getLastPathComponent().equals(tree.getModel().getRoot())) {
        sql.setText("");
        if (selection.getLastPathComponent() instanceof StringBuilder) {
          sql.setText("SELECT * FROM " + selection.getLastPathComponent());
          execute.doClick();
        } else if (selection.getLastPathComponent().equals(DbTreeModel.DETAILS[0])) {
          try {
            showResult(connection.getMetaData().getColumns(connection.getCatalog(),
                connection.getSchema(), selection.getPath()[1].toString(), null));
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        } else if (selection.getLastPathComponent().equals(DbTreeModel.DETAILS[2])) {
          try {
            showResult(connection.getMetaData().getExportedKeys(connection.getCatalog(),
                connection.getSchema(), selection.getPath()[1].toString()));
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        } else if (selection.getLastPathComponent().equals(DbTreeModel.DETAILS[1])) {
          try {
            showResult(connection.getMetaData().getImportedKeys(connection.getCatalog(),
                connection.getSchema(), selection.getPath()[1].toString()));
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        } else if (selection.getLastPathComponent().equals(DbTreeModel.DETAILS[3])) {
          try {
            showResult(connection.getMetaData().getIndexInfo(connection.getCatalog(),
                connection.getSchema(), selection.getPath()[1].toString(), false, false));
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      }
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseEntered(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseExited(MouseEvent e) {
    // TODO Auto-generated method stub

  }

}
