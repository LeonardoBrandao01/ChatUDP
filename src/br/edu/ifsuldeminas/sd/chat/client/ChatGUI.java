package br.edu.ifsuldeminas.sd.chat.client;

import br.edu.ifsuldeminas.sd.chat.ChatException;
import br.edu.ifsuldeminas.sd.chat.ChatFactory;
import br.edu.ifsuldeminas.sd.chat.MessageContainer;
import br.edu.ifsuldeminas.sd.chat.Sender;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Interface gráfica simples para o Chat UDP/TCP.
 * Implementa {@link MessageContainer} para receber mensagens do receiver.
 */
public class ChatGUI extends JFrame implements MessageContainer {

    // ── Campos de configuração ────────────────────────────────────────────
    private JTextField  fldPortaLocal;
    private JTextField  fldIPRemoto;
    private JTextField  fldPortaRemota;
    private JTextField  fldUsername;

    // ── Seleção de protocolo ──────────────────────────────────────────────
    private JRadioButton radUDP;
    private JRadioButton radTCP;
    private JLabel       lblModo;
    private JComboBox<String> cmbModo; // Servidor / Cliente (TCP)

    // ── Botão e status ────────────────────────────────────────────────────
    private JButton btnIniciar;
    private JLabel  lblStatus;

    // ── Área de chat ──────────────────────────────────────────────────────
    private JTextArea  txtLog;
    private JTextField fldMensagem;
    private JButton    btnEnviar;

    // ── Estado interno ────────────────────────────────────────────────────
    private Sender  sender    = null;
    private boolean connected = false;
    private String  username  = "Usuário";

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // ─────────────────────────────────────────────────────────────────────
    public ChatGUI() {
        super("Chat UDP / TCP");
        buildUI();
        wireListeners();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CONSTRUÇÃO DA INTERFACE
    // ══════════════════════════════════════════════════════════════════════

    private void buildUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(560, 580);
        setMinimumSize(new Dimension(480, 500));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        root.add(buildConfigPanel(), BorderLayout.NORTH);
        root.add(buildLogPanel(),    BorderLayout.CENTER);
        root.add(buildInputPanel(),  BorderLayout.SOUTH);
    }

    // ── Painel de configuração ────────────────────────────────────────────
    private JPanel buildConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Configurações de Conexão",
                TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints g = new GridBagConstraints();
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.insets  = new Insets(4, 6, 4, 6);
        g.weightx = 1.0;

        // Linha 0 — Porta Local | IP Remoto
        g.gridy = 0; g.gridx = 0; g.gridwidth = 1;
        panel.add(new JLabel("Porta Local:"), g);
        g.gridx = 1;
        fldPortaLocal = new JTextField("9001");
        panel.add(fldPortaLocal, g);

        g.gridx = 2;
        panel.add(new JLabel("IP Remoto:"), g);
        g.gridx = 3;
        fldIPRemoto = new JTextField("localhost");
        panel.add(fldIPRemoto, g);

        // Linha 1 — Porta Remota | Seu Nome
        g.gridy = 1; g.gridx = 0;
        panel.add(new JLabel("Porta Remota:"), g);
        g.gridx = 1;
        fldPortaRemota = new JTextField("9002");
        panel.add(fldPortaRemota, g);

        g.gridx = 2;
        panel.add(new JLabel("Seu Nome:"), g);
        g.gridx = 3;
        fldUsername = new JTextField("Usuário");
        panel.add(fldUsername, g);

        // Linha 2 — Protocolo | Modo TCP
        g.gridy = 2; g.gridx = 0;
        panel.add(new JLabel("Protocolo:"), g);

        g.gridx = 1;
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        radUDP = new JRadioButton("UDP", true);
        radTCP = new JRadioButton("TCP", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(radUDP);
        bg.add(radTCP);
        radioPanel.add(radUDP);
        radioPanel.add(radTCP);
        panel.add(radioPanel, g);

        g.gridx = 2;
        lblModo = new JLabel("Modo TCP:");
        lblModo.setEnabled(false);
        panel.add(lblModo, g);

        g.gridx = 3;
        cmbModo = new JComboBox<>(new String[]{"Servidor (escutar)", "Cliente (conectar)"});
        cmbModo.setEnabled(false);
        panel.add(cmbModo, g);

        // Linha 3 — Status + botão
        g.gridy = 3; g.gridx = 0; g.gridwidth = 2;
        lblStatus = new JLabel("● Desconectado");
        lblStatus.setForeground(Color.RED);
        panel.add(lblStatus, g);

        g.gridx = 2; g.gridwidth = 2;
        btnIniciar = new JButton("Iniciar Chat");
        panel.add(btnIniciar, g);

        return panel;
    }

    // ── Área de mensagens ─────────────────────────────────────────────────
    private JScrollPane buildLogPanel() {
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        txtLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtLog.setBorder(new EmptyBorder(6, 8, 6, 8));

        JScrollPane scroll = new JScrollPane(txtLog);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Mensagens",
                TitledBorder.LEFT, TitledBorder.TOP));
        return scroll;
    }

    // ── Barra de envio ────────────────────────────────────────────────────
    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBorder(new EmptyBorder(4, 0, 0, 0));

        fldMensagem = new JTextField();
        fldMensagem.setEnabled(false);
        fldMensagem.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        btnEnviar = new JButton("Enviar");
        btnEnviar.setEnabled(false);

        panel.add(fldMensagem, BorderLayout.CENTER);
        panel.add(btnEnviar,   BorderLayout.EAST);
        return panel;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LISTENERS
    // ══════════════════════════════════════════════════════════════════════

    private void wireListeners() {
        // Habilita combo de modo apenas quando TCP estiver marcado
        radTCP.addItemListener(e -> {
            boolean tcp = radTCP.isSelected();
            lblModo.setEnabled(tcp);
            cmbModo.setEnabled(tcp);
        });

        btnIniciar.addActionListener(e -> handleConnect());

        ActionListener enviar = e -> handleSend();
        btnEnviar.addActionListener(enviar);
        fldMensagem.addActionListener(enviar); // Enter também envia
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LÓGICA DE CONEXÃO
    // ══════════════════════════════════════════════════════════════════════

    private void handleConnect() {
        if (connected) return;

        // Valida porta local
        int localPort;
        try {
            localPort = Integer.parseInt(fldPortaLocal.getText().trim());
            if (localPort <= 1024 || localPort > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showErr("Porta Local inválida. Use um valor entre 1025 e 65535.");
            return;
        }

        boolean isTCP      = radTCP.isSelected();
        boolean serverMode = isTCP && cmbModo.getSelectedIndex() == 0;

        String remoteIP   = fldIPRemoto.getText().trim();
        int    remotePort = 0;

        // Cliente TCP e UDP precisam de IP/porta remotos
        if (!serverMode) {
            if (remoteIP.isEmpty()) {
                showErr("O campo 'IP Remoto' não pode estar vazio.");
                return;
            }
            try {
                remotePort = Integer.parseInt(fldPortaRemota.getText().trim());
                if (remotePort <= 0 || remotePort > 65535) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showErr("Porta Remota inválida. Use um valor entre 1 e 65535.");
                return;
            }
        }

        username = fldUsername.getText().trim();
        if (username.isEmpty()) {
            showErr("O campo 'Seu Nome' não pode estar vazio.");
            return;
        }

        // Bloqueia os campos antes de conectar
        setFieldsEnabled(false);
        btnIniciar.setEnabled(false);

        try {
            if (isTCP) {
                if (serverMode) {
                    // Aguarda conexão em thread separada (não bloqueia a EDT)
                    logSystem("Aguardando conexão TCP na porta " + localPort + "…");
                    final int lp = localPort;
                    new Thread(() -> {
                        try {
                            Sender s = TCPReceiver.createServerMode(lp, this);
                            SwingUtilities.invokeLater(() ->
                                    onConnected(s, "TCP", localPort, "—", lp));
                        } catch (ChatException ex) {
                            SwingUtilities.invokeLater(() -> {
                                showErr("Erro ao abrir servidor TCP: "
                                        + ex.getCause().getMessage());
                                setFieldsEnabled(true);
                                btnIniciar.setEnabled(true);
                            });
                        }
                    }).start();
                    return; // onConnected será chamado pela thread
                } else {
                    Sender s = TCPReceiver.createClientMode(remoteIP, remotePort, this);
                    onConnected(s, "TCP", localPort, remoteIP, remotePort);
                }
            } else {
                Sender s = ChatFactory.build(remoteIP, remotePort, localPort, this);
                onConnected(s, "UDP", localPort, remoteIP, remotePort);
            }
        } catch (ChatException ex) {
            String detalhe = ex.getCause() != null
                    ? ex.getCause().getMessage() : ex.getMessage();
            showErr("Falha ao iniciar o chat: " + detalhe);
            setFieldsEnabled(true);
            btnIniciar.setEnabled(true);
        }
    }

    private void onConnected(Sender s, String protocolo,
                             int localPort, String remoteIP, int remotePort) {
        this.sender    = s;
        this.connected = true;

        fldMensagem.setEnabled(true);
        btnEnviar.setEnabled(true);
        fldMensagem.requestFocusInWindow();

        lblStatus.setText("● Conectado  [" + protocolo + "]");
        lblStatus.setForeground(new Color(0, 130, 0));

        logSystem(String.format("Chat iniciado — protocolo: %s | porta local: %d | destino: %s:%d",
                protocolo, localPort, remoteIP, remotePort));
    }

    private void setFieldsEnabled(boolean enabled) {
        fldPortaLocal.setEnabled(enabled);
        fldIPRemoto.setEnabled(enabled);
        fldPortaRemota.setEnabled(enabled);
        fldUsername.setEnabled(enabled);
        radUDP.setEnabled(enabled);
        radTCP.setEnabled(enabled);
        cmbModo.setEnabled(enabled && radTCP.isSelected());
        lblModo.setEnabled(enabled && radTCP.isSelected());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LÓGICA DE ENVIO
    // ══════════════════════════════════════════════════════════════════════

    private void handleSend() {
        if (!connected || sender == null) return;
        String texto = fldMensagem.getText().trim();
        if (texto.isEmpty()) return;

        String payload = texto + MessageContainer.FROM + username;
        try {
            sender.send(payload);
            log("[" + now() + "] Eu (" + username + "): " + texto);
            fldMensagem.setText("");
        } catch (ChatException ex) {
            showErr("Erro ao enviar mensagem: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MessageContainer
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void newMessage(String message) {
        if (message == null) return;
        String msg = message.trim().replace("\u0000", "");
        if (msg.isEmpty()) return;

        // Mensagens de sistema inseridas pelo TCPReceiver
        if (msg.startsWith("[ TCP ]")) {
            SwingUtilities.invokeLater(() -> logSystem(msg));
            return;
        }

        String remetente = "?";
        String texto     = msg;
        if (msg.contains(MessageContainer.FROM)) {
            String[] partes = msg.split(MessageContainer.FROM, 2);
            texto     = partes[0].trim();
            remetente = partes.length > 1 ? partes[1].trim() : "?";
        }

        final String r = remetente;
        final String t = texto;
        SwingUtilities.invokeLater(() ->
                log("[" + now() + "] " + r + ": " + t));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UTILITÁRIOS
    // ══════════════════════════════════════════════════════════════════════

    private void log(String linha) {
        txtLog.append(linha + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    private void logSystem(String msg) {
        log("--- " + msg + " ---");
    }

    private String now() {
        return LocalTime.now().format(TIME_FMT);
    }

    private void showErr(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MAIN
    // ══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new ChatGUI().setVisible(true);
        });
    }
}
