package fr.nozyx.cantinerecovery;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.sshtools.twoslices.Toast;
import com.sshtools.twoslices.ToastType;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    public static final String VERSION = "1.0.0";

    public static boolean shutdownByThis = false;

    public static ImageIcon logo = null;
    public static final boolean headless = GraphicsEnvironment.isHeadless();

    public static void main(String[] args) {
        System.out.println("CantineRecovery v" + VERSION + " démarré!");

        if (headless) System.out.println("Environnement graphique non disponible!\nImpossible de démarrer!");
        else {
            System.out.println("Environnement graphique disponible!");

            Thread shutdownHook = new Thread(() -> {
                if (!shutdownByThis) {
                    for (Frame frame : JFrame.getFrames()) frame.dispose();

                    JOptionPane.showMessageDialog(
                            null,
                            "Votre instance de CantineRecovery v" + VERSION + " a été contrainte de s'éteindre de manière inattendue...\nCela peut être causé par une erreur critique ou simplement que le processus a été interrompu,\nregardez la console pour plus d'informations...",
                            "CantineRecovery Shutdown Hook",
                            JOptionPane.ERROR_MESSAGE
                    );

                    Thread.currentThread().interrupt();
                }
            });

            Runtime.getRuntime().addShutdownHook(shutdownHook);

            logo = new ImageIcon(Main.class.getClassLoader().getResource("cantinerecovery.png"));

            try {
                UIManager.setLookAndFeel(new NimbusLookAndFeel());
            } catch (UnsupportedLookAndFeelException ignored) {}

            if (!new File("cards").exists()) {
                if (!new File("cards").mkdir()) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Une erreur inconnue empêche la création du dossier des cartes...",
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE
                    );

                    shutdownByThis = true;
                    System.exit(1);
                }
            }

            displayNotif("Bienvenue!");

            displayMenu();
        }
    }

    private static void displayMenu() {
        JFrame frame = new JFrame();
        frame.setTitle("Menu - CantineRecovery v" + VERSION);

        if (logo != null) frame.setIconImage(logo.getImage());

        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setSize(300, 100);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JLabel label = new JLabel("Que voulez vous faire ?");
        label.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton exitButton = new JButton("Quitter");
        buttonPanel.add(exitButton);

        JButton listCardsButton = new JButton("Ouvrir la liste des cartes");
        buttonPanel.add(listCardsButton);

        exitButton.addActionListener(e -> {
            frame.dispose();
            displayNotif("Au-revoir!");

            shutdownByThis = true;
            System.exit(0);
        });

        listCardsButton.addActionListener(e -> {
            frame.dispose();
            showCardListFrame();
        });

        frame.add(label, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.dispose();
                displayNotif("Au-revoir!");

                shutdownByThis = true;
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    private static void showCardListFrame() {
        JFrame frame = new JFrame();
        frame.setTitle("Liste des cartes - CantineRecovery v" + VERSION);

        if (logo != null) frame.setIconImage(logo.getImage());

        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(500, 300);
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.dispose();
                displayMenu();
            }
        });

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> cards = new JList<>(model);
        cards.setCellRenderer(new CenteredTextListCellRenderer());
        cards.setSelectionBackground(new Color(65, 165, 247, 155));

        AtomicReference<List<Card>> cardsList = new AtomicReference<>(listCards());
        cardsList.get().forEach(card -> model.addElement("Carte n°" + (card.getCardId() == 0 ? "?" : card.getCardId()) + " (" + (card.getLastname().equals("?") ? "INCONNU" : card.getLastname().toUpperCase()) + " " + (card.getFirstname().equals("?") ? "?" : card.getFirstname().substring(0, 1).toUpperCase() + ".") + ")"));

        if (model.isEmpty()) model.addElement("° Aucune carte enregistrée °");

        cards.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JList<String> list = (JList<String>) e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    String selectedCard = list.getModel().getElementAt(index);
                    if (selectedCard != null) {
                        if (selectedCard.startsWith("°") && selectedCard.endsWith("°")) return;

                        frame.dispose();
                        showCardDetails(cardsList.get().get(index));
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(cards);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        JButton addButton = new JButton("Ajouter");
        addButton.addActionListener((e) -> {
            frame.dispose();
            showAddCardFrame();
        });

        frame.add(scrollPane);
        frame.add(addButton, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private static void showAddCardFrame() {
        JFrame frame = new JFrame();
        frame.setTitle("Ajouter une carte - CantineRecovery v" + VERSION);

        if (logo != null) frame.setIconImage(logo.getImage());

        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setSize(500, 300);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JPanel infoPanel = new JPanel(new GridLayout(5, 2));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField lastnameField = new JTextField();
        JTextField firstnameField = new JTextField();
        JTextField classNameField = new JTextField();
        JTextField codeField = new JTextField();
        JTextField cardIdField = new JTextField();

        infoPanel.add(new JLabel("Nom:"));
        infoPanel.add(lastnameField);

        infoPanel.add(new JLabel("Prénom:"));
        infoPanel.add(firstnameField);

        infoPanel.add(new JLabel("Classe:"));
        infoPanel.add(classNameField);

        infoPanel.add(new JLabel("Contenu du code barre:"));
        infoPanel.add(codeField);

        infoPanel.add(new JLabel("Identifiant de la carte:"));
        infoPanel.add(cardIdField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton confirmButton = new JButton("Confirmer");
        buttonPanel.add(confirmButton);

        JButton importButton = new JButton("Importer depuis un fichier");
        buttonPanel.add(importButton);

        confirmButton.addActionListener(e -> {
            String lastname = lastnameField.getText();
            String firstname = firstnameField.getText();
            String className = classNameField.getText();

            if (lastname.isEmpty()) lastname = "?";
            if (firstname.isEmpty()) firstname = "?";
            if (className.isEmpty()) className = "?";

            Object code = null;
            Object cardId = null;

            if (codeField.getText().isEmpty()) code = 0;
            if (cardIdField.getText().isEmpty()) cardId = 0;

            if (code == null || cardId == null) {
                try {
                    code = Long.parseLong(codeField.getText());
                    cardId = Long.parseLong(cardIdField.getText());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Contenu du code barre ou/et identifiant de la carte invalide!",
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE
                    );

                    return;
                }
            }

            Card newCard = new Card(lastname, firstname, className, (Long) code, (Long) cardId, new File("cards" + File.separator + UUID.randomUUID() + ".crc"));
            newCard.save();

            frame.dispose();
            showCardListFrame();
        });

        importButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setToolTipText("Choisissez un fichier de carte de cantine a importer");
            chooser.setDialogTitle("Importer une carte");
            chooser.setFileFilter(new FileNameExtensionFilter("Carte CantineRecovery", "crc"));

            int result = chooser.showOpenDialog(frame);

            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    Files.copy(chooser.getSelectedFile().toPath(), new File("cards" + File.separator + UUID.randomUUID() + ".crc").toPath());
                } catch (IOException ex) {
                    System.err.println("Erreur lors de l'importation du fichier: " + chooser.getSelectedFile());
                    ex.printStackTrace();

                    JOptionPane.showMessageDialog(
                            null,
                            "Une erreur est survenue lors de l'importation du fichier...\n" + ex.getMessage(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            } else return;

            frame.dispose();
            showCardListFrame();
        });

        frame.add(infoPanel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.dispose();
                showCardListFrame();
            }
        });

        frame.setVisible(true);
    }

    private static void showCardDetails(Card card) {
        JFrame frame = new JFrame();
        frame.setTitle("Carte n°" + card.getCardId() + " - CantineRecovery v" + VERSION);

        if (logo != null) frame.setIconImage(logo.getImage());

        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(500, 300);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JPanel infoPanel = new JPanel(new GridLayout(6, 2));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField lastnameField = new JTextField(card.getLastname());
        lastnameField.setEditable(false);

        JTextField firstnameField = new JTextField(card.getFirstname());
        firstnameField.setEditable(false);

        JTextField classNameField = new JTextField(card.getClassName());
        classNameField.setEditable(false);

        JTextField codeField = new JTextField(String.valueOf(card.getCode()));
        codeField.setEditable(false);

        JTextField cardIdField = new JTextField(String.valueOf(card.getCardId()));
        cardIdField.setEditable(false);

        infoPanel.add(new JLabel("Nom:"));
        infoPanel.add(lastnameField);

        infoPanel.add(new JLabel("Prénom:"));
        infoPanel.add(firstnameField);

        infoPanel.add(new JLabel("Classe:"));
        infoPanel.add(classNameField);

        infoPanel.add(new JLabel("Contenu du code barre:"));
        infoPanel.add(codeField);

        infoPanel.add(new JLabel("Identifiant de la carte:"));
        infoPanel.add(cardIdField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton modifyButton = new JButton("Modifier");

        JButton saveButton = new JButton("Sauvegarder");
        saveButton.setEnabled(false);

        JButton deleteButton = new JButton("Supprimer");

        JButton generateButton = new JButton("Générer un code barre");

        buttonPanel.add(modifyButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(generateButton);

        modifyButton.addActionListener(e -> {
            lastnameField.setEditable(true);
            firstnameField.setEditable(true);
            classNameField.setEditable(true);
            codeField.setEditable(true);
            cardIdField.setEditable(true);

            modifyButton.setEnabled(false);
            saveButton.setEnabled(true);
            deleteButton.setEnabled(false);
            generateButton.setEnabled(false);
        });

        saveButton.addActionListener(e -> {
            String newLastname = lastnameField.getText();
            String newFirstname = firstnameField.getText();
            String newClassName = classNameField.getText();

            long newCode;
            long newCardId;

            try {
                newCode = Long.parseLong(codeField.getText());
                newCardId = Long.parseLong(cardIdField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Contenu du code barre ou/et identifiant de la carte invalide!",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }

            card.setLastname(newLastname);
            card.setFirstname(newFirstname);
            card.setClassName(newClassName);
            card.setCode(newCode);
            card.setCardId(newCardId);
            card.save();

            lastnameField.setEditable(false);
            firstnameField.setEditable(false);
            classNameField.setEditable(false);
            codeField.setEditable(false);
            cardIdField.setEditable(false);

            modifyButton.setEnabled(true);
            saveButton.setEnabled(false);
            deleteButton.setEnabled(true);
            generateButton.setEnabled(true);
        });

        deleteButton.addActionListener(e -> {
            int option = JOptionPane.showConfirmDialog(
                    frame,
                    "Voulez-vous vraiment supprimer cette carte ?",
                    "Confirmation de suppression",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null
            );

            if (option == JOptionPane.YES_OPTION) {
                card.autoDelete();

                frame.dispose();
                showCardListFrame();
            }
        });

        generateButton.addActionListener(e -> {
            BufferedImage image = generateBarcode(card);
            writeImage(image, new File("cards" + File.separator + card.getCardId() + ".png"));
        });

        frame.add(infoPanel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.dispose();
                showCardListFrame();
            }
        });

        frame.setVisible(true);
    }

    private static void displayNotif(String text) {
        try {
            Toast.toast(ToastType.INFO, "CantineRecovery v" + VERSION, text);
        } catch (Throwable th) {
            System.err.println("Erreur lors de l'affichage d'une notification....");
            th.printStackTrace();

            JOptionPane.showMessageDialog(
                null,
                text,
                "CantineRecovery v" + VERSION,
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private static void writeImage(BufferedImage image, File file) {
        try {
            ImageIO.write(image, "PNG", file);

            System.out.println("L'image a correctement été enregistrée à " + file.getAbsolutePath());

            String[] opt = {"Non merci", "Oui"};

            int i = JOptionPane.showOptionDialog(
                    null,
                    "Votre code barre a correctement été généré et enregistré à \n" + file.getAbsolutePath() + "\n\nSouhaitez-vous l'ouvrir?",
                    "Code barre enregistré",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    opt,
                    opt[0]
            );

            if (i == 1) Desktop.getDesktop().open(file);
        } catch (IOException e) {
            System.err.println("Une erreur est survenue lors de l'enregistrement de l'image");
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Une erreur est survenue lors de l'enregistrement de votre code barre...",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static List<Card> listCards() {
        System.out.println("Récupération des cartes enregistrés...");

        List<Card> cards = new ArrayList<>();

        File folder = new File("cards");
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    if (!FilenameUtils.getExtension(file.getName()).equals("crc")) continue;

                    Card card = readCardFromFile(file);
                    if (card != null) cards.add(card);
                }
            }
        }

        return cards;
    }

    private static Card readCardFromFile(File file) {
        Properties properties = new Properties();
        try (FileReader reader = new FileReader(file)) {
            properties.load(reader);

            if (!properties.getProperty("Version").equals(VERSION)) throw new IOException("Ce fichier a été créé a l'aide d'une autre version de CantineRecovery!");

            return Card.generate(properties, file);
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier: " + file.getName());
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Une erreur est survenue lors de la lecture du fichier: " + file.getName() + "\n" + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        return null;
    }

    private static BufferedImage generateBarcode(Card card) {
        try {
            String barcodeText = String.valueOf(card.getCode());

            int imageWidth = 500;
            int imageHeight = 200;
            int barcodeWidth = 500;
            int barcodeHeight = 150;

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 0);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(barcodeText, BarcodeFormat.ITF, barcodeWidth, barcodeHeight, hints);
            BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_BYTE_BINARY);

            int x = 0;
            int y = ((imageHeight - barcodeHeight) / 2);

            for (int i = 0; i < imageWidth; i++) {
                for (int j = 0; j < imageHeight; j++) {
                    if (j < y || j >= y + barcodeHeight) image.setRGB(i, j, 0xFFFFFFFF);
                    else image.setRGB(i, j, bitMatrix.get(i - x, j - y) ? 0 : 0xFFFFFFFF);
                }
            }

            Graphics2D graphics = image.createGraphics();

            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font("Arial", Font.PLAIN, 28));

            FontMetrics metrics = graphics.getFontMetrics();

            int textWidth = metrics.stringWidth(barcodeText + " (By C.R)");
            int textX = (imageWidth - textWidth) / 2;
            int textY = imageHeight - 3;

            graphics.drawString(barcodeText + " (By C.R)", textX, textY);

            textWidth = metrics.stringWidth(card.getLastname() + " " + card.getFirstname() + " " + card.getClassName());
            textX = (imageWidth - textWidth) / 2;
            textY = 23;

            graphics.drawString(card.getLastname() + " " + card.getFirstname() + " " + card.getClassName(), textX, textY);

            graphics.dispose();

            System.out.println("Carte générée avec succès!");

            return image;
        } catch (WriterException e) {
            System.err.println("Erreur lors de la génération du code-barres : " + e.getMessage());
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Une erreur est survenue lors de la création de votre code barre...",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE
            );

            return null;
        }
    }
}
