package com.bibliotech.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailProperties props;

    // ── Welcome email on register ─────────────────────────────────

    @Async
    public void sendWelcome(String to, String prenom) {
        if (!props.isEnabled()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.getFrom());
            msg.setTo(to);
            msg.setSubject("Bienvenue sur " + props.getAppName());
            msg.setText(String.format("""
                    Bonjour %s,

                    Votre compte %s a été créé avec succès.

                    Vous pouvez maintenant :
                    - Parcourir notre catalogue de documents PDF
                    - Télécharger et lire des documents en ligne
                    - Utiliser notre assistant intelligent

                    Connectez-vous sur : http://localhost:3000

                    Cordialement,
                    L'équipe %s
                    """, prenom, props.getAppName(), props.getAppName()));
            mailSender.send(msg);
            log.info("Email de bienvenue envoyé à {}", to);
        } catch (Exception e) {
            log.error("Erreur envoi email bienvenue à {}: {}",
                    to, e.getMessage());
        }
    }

    // ── Download confirmation ─────────────────────────────────────

    @Async
    public void sendDownloadConfirmation(String to,
                                         String prenom,
                                         String documentTitre,
                                         String auteur) {
        if (!props.isEnabled()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.getFrom());
            msg.setTo(to);
            msg.setSubject("Téléchargement confirmé — "
                    + props.getAppName());
            msg.setText(String.format("""
                    Bonjour %s,

                    Vous avez téléchargé le document suivant :

                    Titre  : %s
                    Auteur : %s

                    Bonne lecture !

                    Cordialement,
                    L'équipe %s
                    """, prenom, documentTitre, auteur,
                    props.getAppName()));
            mailSender.send(msg);
            log.info("Email téléchargement envoyé à {} pour \"{}\"",
                    to, documentTitre);
        } catch (Exception e) {
            log.error("Erreur envoi email téléchargement: {}",
                    e.getMessage());
        }
    }

    // ── Account activated/deactivated ────────────────────────────

    @Async
    public void sendAccountStatusChange(String to,
                                        String prenom,
                                        boolean activated) {
        if (!props.isEnabled()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.getFrom());
            msg.setTo(to);
            msg.setSubject((activated ? "Compte activé" : "Compte désactivé")
                    + " — " + props.getAppName());
            msg.setText(String.format("""
                    Bonjour %s,

                    Votre compte %s a été %s par un administrateur.

                    %s

                    Cordialement,
                    L'équipe %s
                    """,
                    prenom,
                    props.getAppName(),
                    activated ? "activé" : "désactivé",
                    activated
                            ? "Vous pouvez maintenant vous connecter."
                            : "Vous ne pouvez plus vous connecter. "
                            + "Contactez un administrateur pour plus d'informations.",
                    props.getAppName()));
            mailSender.send(msg);
            log.info("Email statut compte envoyé à {}: {}",
                    to, activated ? "activé" : "désactivé");
        } catch (Exception e) {
            log.error("Erreur envoi email statut compte: {}",
                    e.getMessage());
        }
    }

    // ── Password changed ──────────────────────────────────────────

    @Async
    public void sendPasswordChanged(String to, String prenom) {
        if (!props.isEnabled()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.getFrom());
            msg.setTo(to);
            msg.setSubject("Mot de passe modifié — "
                    + props.getAppName());
            msg.setText(String.format("""
                    Bonjour %s,

                    Votre mot de passe %s a été modifié avec succès.

                    Si vous n'êtes pas à l'origine de cette modification,
                    contactez immédiatement un administrateur.

                    Cordialement,
                    L'équipe %s
                    """, prenom, props.getAppName(), props.getAppName()));
            mailSender.send(msg);
            log.info("Email changement mot de passe envoyé à {}", to);
        } catch (Exception e) {
            log.error("Erreur envoi email mot de passe: {}",
                    e.getMessage());
        }
    }

    // ── New document added (admin notification) ───────────────────

    @Async
    public void sendNewDocumentNotification(String to,
                                            String documentTitre,
                                            String auteur,
                                            String categorie) {
        if (!props.isEnabled()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.getFrom());
            msg.setTo(to);
            msg.setSubject("Nouveau document disponible — "
                    + props.getAppName());
            msg.setText(String.format("""
                    Bonjour,

                    Un nouveau document est disponible dans %s :

                    Titre      : %s
                    Auteur     : %s
                    Catégorie  : %s

                    Connectez-vous pour le consulter.

                    Cordialement,
                    L'équipe %s
                    """, props.getAppName(), documentTitre,
                    auteur, categorie, props.getAppName()));
            mailSender.send(msg);
            log.info("Email nouveau document envoyé à {} : \"{}\"",
                    to, documentTitre);
        } catch (Exception e) {
            log.error("Erreur envoi email nouveau document: {}",
                    e.getMessage());
        }
    }
}