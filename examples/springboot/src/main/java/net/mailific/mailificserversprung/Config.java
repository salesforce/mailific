package net.mailific.mailificserversprung;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.security.sasl.AuthorizeCallback;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.mailific.mailificserverspringboot.CommandHandlerProvider;
import net.mailific.mailificserverspringboot.ExtensionProvider;
import net.mailific.main.Main;
import net.mailific.server.Line;
import net.mailific.server.LineConsumer;
import net.mailific.server.MailObjectFactory;
import net.mailific.server.SmtpCommandMap;
import net.mailific.server.extension.Extension;
import net.mailific.server.extension.auth.Auth;
import net.mailific.server.extension.auth.AuthCheck;
import net.mailific.server.extension.auth.LoginMechanism;
import net.mailific.server.extension.auth.PlainMechanism;
import net.mailific.server.extension.starttls.StartTls;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;

@Configuration
public class Config {
	
	@Bean
	public ExtensionProvider extensionProvider() {
		List<Extension> extensions = Main.harmlessExtensions();
		extensions.add(new StartTls());
		AuthCheck authCheck = (String authzid, String authcid, byte[] credential) -> {
			AuthorizeCallback result = new AuthorizeCallback(authcid, authzid);
		      result.setAuthorized(new String(credential, StandardCharsets.UTF_8).equals("french fries"));
		      return result;
		};
	    extensions.add(new Auth(
	        List.of(new PlainMechanism(authCheck), new LoginMechanism(authCheck)), "foo"));
	    return () -> extensions;
	}
	
	@Bean
	public MailObjectFactory mailObjectFactory(@Value("${mailific.example.spool:/tmp/mailific-spool}") String spool) {
		return session -> new SpoolDirMailObject(spool);
	}
	
	
	// An example of how to override the LineConsumer used by the SmtpSession. 
	@Bean("mailificCommandConsumer")
	public LineConsumer everythingsOk() {
		return new LineConsumer() {

			@Override
			public Transition consume(SmtpSession session, Line line) {
				// TODO Auto-generated method stub
				return new Transition(Reply._250_OK, SessionState.NO_STATE_CHANGE); 
			}
			
			@Override
			public Transition connect(SmtpSession session) {
				return new Transition(Reply._221_OK, StandardStates.CONNECTED);
			}
			
		};
	}

	// This is just to show that you may have multiple LineConsumer Beans, but the one named mailificCommandConsumer will
	// get used by the autoconfiguration as the main command handler.
	@Bean()
	public LineConsumer nothingsOk() {
		return (session, line) ->  new Transition(Reply._554_SERVER_ERROR, SessionState.NO_STATE_CHANGE); 
	}

}
