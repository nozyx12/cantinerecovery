package fr.nozyx.cantinerecovery;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class Card {
    private final File from;

    private String firstname;
    private String lastname;
    private String className;
    private long code;
    private long cardId;

    public Card(String lastname, String firstname, String className, long code, long cardId, File from) {
        this.lastname = lastname;
        this.firstname = firstname;
        this.className = className;
        this.code = code;
        this.cardId = cardId;
        this.from = from;
    }

    public void save() {
        Properties properties = new Properties();
        try {
            if (!this.from.exists()) this.from.createNewFile();

            FileReader reader = new FileReader(this.from);

            properties.load(reader);

            properties.setProperty("Version", Main.VERSION);
            properties.setProperty("Lastname", this.lastname);
            properties.setProperty("Firstname", this.firstname);
            properties.setProperty("Class", this.className);
            properties.setProperty("Code", String.valueOf(this.code));
            properties.setProperty("CardId", String.valueOf(this.cardId));

            properties.store(new FileWriter(this.from), null);
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde de la carte dans le fichier: " + this.from.getName());
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Une erreur est survenue lors de la sauvegarde de la carte dans le fichier: " + this.from.getName() + "\n" + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public void autoDelete() {
        if (!this.from.delete()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Une erreur inconnue empêche la suppression de cette carte...",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public static Card generate(Properties prop, File from) {
        return new Card(prop.getProperty("Lastname"), prop.getProperty("Firstname"), prop.getProperty("Class"), Long.parseLong(prop.getProperty("Code")), Long.parseLong(prop.getProperty("CardId")), from);
    }

    public String getFirstname() {
        return this.firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return this.lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getClassName() {
        return this.className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public long getCode() {
        return this.code;
    }

    public void setCode(long code) {
        this.code = code;
    }

    public long getCardId() {
        return this.cardId;
    }

    public void setCardId(long cardId) {
        this.cardId = cardId;
    }
}
