package client;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGUtils;
import command.Command;
import util.FileTableModel;
import util.FilesPanel;
import util.FilesTable;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Properties;

public class GuiClient extends JFrame {

    private final static Path USER_PATH = Paths.get(System.getProperty("user.home"));
    private final static Path ROOT_PATH = Paths.get(USER_PATH.toString(), "server");
    private final static String DEFAULT_HOST = "localhost";
    private final static int DEFAULT_PORT = 5678;
    private final static String STATUS_TEMPLATE = "%s files of size %s bytes";
    private final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();

    private JSplitPane files;
    private JTextField serverAddressField;
    private JTextField serverPortField;
    private FilesPanel clientFilesPanel;
    private FilesPanel serverFilesPanel;
    private JPanel adminPanel;
    private FilesTable serverFiles;
    private Client client;
    private JTextField loginField;
    private JPasswordField passwordField;
    private String currentUser;
    private final HashMap<File, String> filePermissions = new HashMap<>();
    private final Properties properties = new Properties();

    /**
     * Class constructor.
     */
    public GuiClient() {
        loadProperties();
        prepareGUI();
    }

    /**
     * Loads properties from the properties file.
     */
    private void loadProperties() {
        try (InputStream in = GuiClient.class.getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(in);
        } catch (IOException e) {
            // TODO: add logging.
        }
    }

    /**
     * Prepares application GUI and displays it to the user.
     */
    public void prepareGUI() {
        setWindowParameters();
        setAppIcon();
        addTopPanel();
        addFilesPanel();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                closeChannel();
            }
        });
        setVisible(true);
    }

    /**
     * Sets initial application window parameters.
     */
    private void setWindowParameters() {
        setTitle("File cloud explorer");
        setLayout(new BorderLayout());
        setBounds(600, 300, 800, 600);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMaximizedBounds(env.getMaximumWindowBounds());
    }

    /**
     * Sets application icon.
     */
    private void setAppIcon() {
        setIconImage(FlatSVGUtils.svg2image("/" + properties.getProperty("appIcon"), 16, 16));
    }

    /**
     * Adds the top panel to the main application frame.
     * The panel consists of a connection panel and an administration panel,
     * the latter of which is only visible to users with the 'administrator' flag.
     */
    private void addTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel connectionPanel = getConnectionPanel();
        topPanel.add(connectionPanel, BorderLayout.WEST);

        JPanel adminPanel = getAdminPanel();
        topPanel.add(adminPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
    }

    /**
     * @return Connection panel.
     */
    private JPanel getConnectionPanel() {
        serverAddressField = new JTextField();
        serverAddressField.setToolTipText("Server address");
        serverAddressField.setText(getHost());

        serverPortField = new JTextField();
        serverPortField.setToolTipText("Server port");
        serverPortField.setText(String.valueOf(getPort()));

        loginField = new JTextField();
        loginField.setToolTipText("User name");

        passwordField = new JPasswordField();
        passwordField.setToolTipText("Password");

        JButton connectButton = new JButton();
        connectButton.setToolTipText("Connect to the selected server with the specified credentials");
        connectButton.setIcon(new FlatSVGIcon(properties.getProperty("connectIcon"), 16, 16));
        connectButton.setBackground(new Color(190, 236, 250));
        connectButton.addActionListener(e -> connect());

        JButton disconnectButton = new JButton();
        disconnectButton.setToolTipText("Disconnect from the server");
        disconnectButton.setIcon(new FlatSVGIcon(properties.getProperty("disconnectIcon"), 16, 16));
        disconnectButton.setBackground(new Color(247, 156, 179));
        disconnectButton.addActionListener(e -> disconnect());

        JLabel serverSplitterLabel = new JLabel(":");
        JLabel userSplitterLabel = new JLabel(":");
        JLabel atLabel = new JLabel("@");

        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectionPanel.add(loginField);
        connectionPanel.add(userSplitterLabel);
        connectionPanel.add(passwordField);
        connectionPanel.add(atLabel);
        connectionPanel.add(serverAddressField);
        connectionPanel.add(serverSplitterLabel);
        connectionPanel.add(serverPortField);
        connectionPanel.add(connectButton);
        connectionPanel.add(disconnectButton);

        return connectionPanel;
    }

    /**
     * Returns the administration panel. Invisible by default.
     * Becomes visible only after establishing a connection and only for users with the 'administrator' flag.
     *
     * @return Administration panel.
     */
    private JPanel getAdminPanel() {
        adminPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addUserButton = new JButton();
        addUserButton.setToolTipText("Add new user");
        addUserButton.setIcon(new FlatSVGIcon(properties.getProperty("userIcon"), 16, 16));
        addUserButton.addActionListener(e -> addUser());
        adminPanel.add(addUserButton);

        // Only visible to administrators after logging in. See connect() (not implemented yet).
        adminPanel.setVisible(false);

        return adminPanel;
    }

    /**
     * Adds new user.
     */
    private void addUser() {
        // TODO: implement.
    }

    /**
     * Connects to the server.
     */
    private void connect() {
        InetAddress address = null;
        try {
            address = InetAddress.getByName(getHost());
        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(this, "Wrong server address:\n" + e.getMessage(),
                    "Wrong server address", JOptionPane.ERROR_MESSAGE);
        }
        client = new Client(address, getPort());

        // client.channel may be null because client starts in a separate thread.
        while (!client.channelIsReady()) {
            // TODO: deal with busy-waiting.
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO: add logging.
                e.printStackTrace();
            }
        }
        String authString = String.format("%s %s %s",
                Command.AUTH,
                loginField.getText(),
                new String(passwordField.getPassword()));
        client.sendMessage(authString);

        // TODO: get the server message (OK or fail) and process properly.
        currentUser = loginField.getText();
        adminPanel.setVisible(userIsAdmin());
        // TODO: set to the path given by the server.
        File serverDir = client.getServerDir();
        if (serverDir == null) {
            serverDir = ROOT_PATH.toFile();
        }
        serverFilesPanel.setDir(serverDir);
        serverFilesPanel.setPath(serverDir.getPath());
        serverFilesPanel.updateStatus();
    }

    /**
     * Disconnects from the server.
     */
    private void disconnect() {
        serverFilesPanel.setDir(null);
        serverFilesPanel.setPath("");
        serverFilesPanel.updateStatus();
        closeChannel();
    }

    /**
     * @return true if the user has the 'administrator' flag, otherwise false.
     */
    private boolean userIsAdmin() {
        // TODO: implement after implementing authentication.
        return true;
    }

    /**
     * @return Server address.
     */
    private String getHost() {
        String host = DEFAULT_HOST;
        if (!properties.isEmpty()) {
            String hostProperty = properties.getProperty("host");
            if (hostProperty != null) {
                host = hostProperty;
            }
        }
        final String serverAddressValue = serverAddressField.getText();
        if (!(serverAddressValue.isEmpty() || serverAddressValue.equals(host))) {
            host = serverAddressValue;
        }
        return host;
    }

    /**
     * @return Server port.
     */
    private int getPort() {
        String port = String.valueOf(DEFAULT_PORT);
        if (!properties.isEmpty()) {
            String portProperty = properties.getProperty("port");
            if (portProperty != null) {
                port = portProperty;
            }
        }
        final String portValue = serverPortField.getText();
        if (!(portValue.isEmpty() || portValue.equals(port))) {
            port = portValue;
        }
        return Integer.parseInt(port);
    }

    /**
     * Closes channel to the server.
     */
    private void closeChannel() {
        if (client != null && client.channelIsReady()) {
            client.closeChannel();
        }
    }

    /**
     * Adds files panel to the main application frame.
     */
    private void addFilesPanel() {
        clientFilesPanel = getClientFilesPanel();
        clientFilesPanel.updateStatus();

        serverFilesPanel = getServerFilesPanel();
        serverFilesPanel.updateStatus();

        files = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, clientFilesPanel, serverFilesPanel);
        setDividerPosition();

        files.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setDividerPosition();
            }
        });

        add(files, BorderLayout.CENTER);
    }

    /**
     * Sets divider's default position at 1/2 of app window.
     */
    private void setDividerPosition() {
        files.setDividerLocation(getWidth() / 2);
    }

    /**
     * @return client.Client files panel.
     */
    private FilesPanel getClientFilesPanel() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JComboBox<File> diskList = new JComboBox<>(File.listRoots());
        diskList.setToolTipText("Current disk drive");
        diskList.addActionListener(e -> changeDisk(diskList.getItemAt(diskList.getSelectedIndex())));
        buttons.add(diskList);

        JButton uploadButton = new JButton();
        uploadButton.setToolTipText("Upload the selected file to the current server directory");
        uploadButton.setIcon(new FlatSVGIcon(properties.getProperty("uploadIcon"), 16, 16));
        uploadButton.addActionListener(e -> upload());
        buttons.add(uploadButton);

        FileTableModel filesModel = new FileTableModel(USER_PATH.toFile());
        FilesTable files = new FilesTable(filesModel);

        JLabel status = new JLabel(STATUS_TEMPLATE);

        return new FilesPanel("client.Client files", buttons, files, status);
    }

    /**
     * Changes the disk drive of client's file system.
     *
     * @param newDisk Chosen disk drive.
     */
    private void changeDisk(File newDisk) {
        if (clientFilesPanel.getDir().equals(newDisk)) {
            return;
        }
        clientFilesPanel.setDir(newDisk);
        clientFilesPanel.setPath(String.valueOf(newDisk));
        clientFilesPanel.updateStatus();
    }

    /**
     * Uploads the selected file from the client to the current directory on the server.
     */
    private void upload() {
        if (!maySendCommand()) {
            return;
        }
        File selectedFile = clientFilesPanel.getSelectedFile();
        File currentServerDir = serverFilesPanel.getDir();
        client.upload(selectedFile, currentServerDir);
        File uploadedFile = Paths.get(currentServerDir.getPath(), selectedFile.getName()).toFile();
        filePermissions.put(uploadedFile, "rw");
    }

    /**
     * @return Server files panel.
     */
    private FilesPanel getServerFilesPanel() {

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton downloadButton = new JButton();
        downloadButton.setToolTipText("Download the selected file to the current client directory");
        downloadButton.setIcon(new FlatSVGIcon(properties.getProperty("downloadIcon"), 16, 16));
        downloadButton.addActionListener(e -> download());
        buttons.add(downloadButton);

        JButton copyButton = new JButton();
        copyButton.setToolTipText("Create a copy of the selected file or directory in the current server directory");
        copyButton.setIcon(new FlatSVGIcon(properties.getProperty("copyIcon"), 16, 16));
        copyButton.addActionListener(e -> copy());
        buttons.add(copyButton);

        JButton createDirectoryButton = new JButton();
        createDirectoryButton.setToolTipText("Create new directory in the current server directory");
        createDirectoryButton.setIcon(new FlatSVGIcon(properties.getProperty("newDirIcon"), 16, 16));
        createDirectoryButton.addActionListener(e -> createDirectory());
        buttons.add(createDirectoryButton);

        JButton deleteButton = new JButton();
        deleteButton.setToolTipText("Delete the selected file or directory in the current server directory");
        deleteButton.setIcon(new FlatSVGIcon(properties.getProperty("deleteIcon"), 16, 16));
        deleteButton.addActionListener(e -> delete());
        buttons.add(deleteButton);

        JButton grantPermissionsButton = new JButton();
        grantPermissionsButton.setToolTipText("Grant permissions for the selected file or directory to other users");
        grantPermissionsButton.setIcon(new FlatSVGIcon(properties.getProperty("shareIcon"), 16, 16));
        grantPermissionsButton.addActionListener(e -> grantPermissions());
        buttons.add(grantPermissionsButton);

        // No files shown until connection is established.
        setEmptyServerFiles();

        JLabel status = new JLabel(STATUS_TEMPLATE);

        return new FilesPanel("Server files", buttons, serverFiles, status);
    }

    /**
     * Sets empty file table model for the server files table.
     */
    private void setEmptyServerFiles() {
        FileTableModel filesModel = new FileTableModel(null);
        serverFiles = new FilesTable(filesModel);
    }

    /**
     * Downloads the selected file from the server to the current directory on the client.
     */
    private void download() {
        if (!maySendCommand()) {
            return;
        }
        final File selectedFile = serverFilesPanel.getSelectedFile();
        if (selectedFile == null) {
            return;
        }
        final File currentClientDir = clientFilesPanel.getDir();
        client.download(selectedFile, currentClientDir);
    }

    /**
     * Copies the selected file on the server to the new file in the same directory.
     */
    private void copy() {
        if (!maySendCommand()) {
            return;
        }
        final File selectedFile = serverFilesPanel.getSelectedFile();
        if (selectedFile == null) {
            return;
        }
        final String newFileName = JOptionPane.showInputDialog("Input new file name:");
        if (newFileName.equals(selectedFile.getName())) {
            JOptionPane.showMessageDialog(
                    this,
                    "The name of the new file must be different from the name of the copied file",
                    "Invalid name",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        final File newFile = Paths.get(serverFilesPanel.getDir().getPath(), newFileName).toFile();
        client.copy(selectedFile, newFile);
        filePermissions.put(newFile, "rw");
        refreshServerFiles();
    }

    /**
     * Refreshes the server file list.
     */
    private void refreshServerFiles() {
        TableModel model = serverFiles.getModel();
        if (model == null) {
            return;
        }
        File serverDir = serverFilesPanel.getDir();
        if (serverDir == null) {
            return;
        }
        // TODO: rework, doesn't refresh the files table after changes.
        // Maybe because it happens before any changes are made on the server.
        ((FileTableModel) model).fireTableDataChanged();
    }

    /**
     * Creates a new directory with the specified name on the server.
     */
    private void createDirectory() {
        if (!maySendCommand()) {
            return;
        }
        final File currentServerDir = serverFilesPanel.getDir();
        if (currentServerDir == null) {
            return;
        }
        final String dirName = JOptionPane.showInputDialog("Input new directory name");
        final String dirPath = Paths.get(currentServerDir.getPath(), dirName).toString();
        client.createDirectory(dirPath);
        filePermissions.put(new File(dirPath), "rw");
        refreshServerFiles();
    }

    /**
     * Deletes the selected file on the server.
     */
    private void delete() {
        if (!maySendCommand()) {
            return;
        }
        final File selectedFile = serverFilesPanel.getSelectedFile();
        if (selectedFile == null) {
            return;
        }
        client.delete(selectedFile);
        filePermissions.remove(selectedFile);
        refreshServerFiles();
    }

    /**
     * Modifies users permissions for the selected file.
     */
    private void grantPermissions() {
        if (!maySendCommand()) {
            return;
        }
        final File selectedFile = serverFilesPanel.getSelectedFile();
        final String permissions = filePermissions.getOrDefault(selectedFile, "r");
        client.grantPermissions(currentUser, selectedFile, permissions);
    }

    /**
     * @return The result of checking whether it is possible to send commands to the server.
     */
    private boolean maySendCommand() {
        if (client == null) {
            JOptionPane.showMessageDialog(this,
                    "Connection with the server is not established.",
                    "Connection not established",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (client.channelIsReady()) {
            return true;
        }
        JOptionPane.showMessageDialog(this,
                "Connection with the server is lost.",
                "Connection lost",
                JOptionPane.ERROR_MESSAGE);
        return false;
    }

    public static void main(String[] args) {
        FlatLightLaf.install();
        SwingUtilities.invokeLater(GuiClient::new);
    }
}