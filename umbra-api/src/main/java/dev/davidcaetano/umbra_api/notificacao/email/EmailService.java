package dev.davidcaetano.umbra_api.notificacao.email;

import dev.davidcaetano.umbra_api.notificacao.properties.EnvioDeAvisosProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(prefix = "umbra.notificacao", name = "envio-habilitado", havingValue = "true")
public class EmailService {

    private static final String NOME_DO_REMETENTE = "Umbra";

    private final JavaMailSender mailSender;
    private final String remetente;

    public EmailService(JavaMailSender mailSender, EnvioDeAvisosProperties propriedades) {
        this.mailSender = mailSender;
        this.remetente = propriedades.remetente();
    }

    public void enviar(String destinatario, MensagemDeEmail mensagem) {
        MimeMessage email = mailSender.createMimeMessage();
        try {
            MimeMessageHelper montagem = new MimeMessageHelper(email, true, StandardCharsets.UTF_8.name());
            montagem.setFrom(remetente, NOME_DO_REMETENTE);
            montagem.setTo(destinatario);
            montagem.setSubject(mensagem.assunto());
            montagem.setText(mensagem.texto(), mensagem.html());
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("Email de aviso não pôde ser montado", e);
        }
        mailSender.send(email);
    }
}
