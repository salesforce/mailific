package net.mailific.mailificserversprung;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import javax.security.sasl.AuthorizeCallback;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.mailific.mailificserverspringboot.BannerDomainProvider;
import net.mailific.mailificserverspringboot.CommandHandlerProvider;
import net.mailific.mailificserverspringboot.EhloGreetingProvider;
import net.mailific.mailificserverspringboot.ExtensionProvider;
import net.mailific.main.Main;
import net.mailific.server.Line;
import net.mailific.server.LineConsumer;
import net.mailific.server.MailObjectFactory;
import net.mailific.server.SmtpCommandMap;
import net.mailific.server.commands.CommandHandler;
import net.mailific.server.extension.Extension;
import net.mailific.server.extension.auth.Auth;
import net.mailific.server.extension.auth.AuthCheck;
import net.mailific.server.extension.auth.LoginMechanism;
import net.mailific.server.extension.auth.PlainMechanism;
import net.mailific.server.extension.starttls.StartTls;
import net.mailific.server.session.LineConsumerChain;
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
	// This creates a chain that filters for the "evil" command before passing the line
	// on to the standard set of command handlers. 
	@Bean("mailificCommandConsumer")
	public LineConsumer hearNoEvil(CommandHandlerProvider commands) {
		LineConsumerChain chain = new LineConsumerChain();
		chain.addLineConsumer("standard", new SmtpCommandMap(commands.commandHandlers(), commands.connectHandler()));
		chain.addLineConsumer("stupid", 
				new LineConsumer() {

			@Override
			public Transition consume(SmtpSession session, Line line) {
				if (line.getStripped().equalsIgnoreCase("evil")) {
					return new Transition(new Reply(500, "I don't hear you."), SessionState.NO_STATE_CHANGE);
				}
				return Transition.UNHANDLED;
			}
			
			@Override
			public Transition connect(SmtpSession session) {
				return Transition.UNHANDLED;
			}
		});
		return chain;
	}

	// This is just to show that you may have multiple LineConsumer Beans, but the one named mailificCommandConsumer will
	// get used by the autoconfiguration as the main command handler.
	@Bean()
	public LineConsumer nothingsOk() {
		return (session, line) ->  new Transition(Reply._554_SERVER_ERROR, SessionState.NO_STATE_CHANGE); 
	}

	// An example of the easy way to override or add a couple of extensions. Makes NOOP return a rude reply,
	// and adds "X-DO-NOTHING"
    @Bean(name = "commandOverrides")
    Collection<CommandHandler> commandOverrides(
      BannerDomainProvider domain,
      EhloGreetingProvider greeting,
      MailObjectFactory mailObjectFactory) {
    	
    	return List.of(new CommandHandler() {
			
			@Override
			public String verb() {
				return "NOOP";
			}
			
			@Override
			public Transition handleCommand(SmtpSession smtpSession, Line line) {
				return new Transition(new Reply(250, "PFFT"), SessionState.NO_STATE_CHANGE);
			}
			}, new CommandHandler() {
			
			@Override
			public String verb() {
				return "X-DO-NOTHING";
			}
			
			@Override
			public Transition handleCommand(SmtpSession smtpSession, Line line) {
				return new Transition(new Reply(250, "Nothing doing."), SessionState.NO_STATE_CHANGE);
			}
		});
    	
    	
  }
}
