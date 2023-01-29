package net.mailific.mailificserversprung;

import java.nio.charset.StandardCharsets;

import net.mailific.server.Line;
import net.mailific.server.LineConsumer;
import net.mailific.server.commands.Rcpt;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.Transition;

/**
 * 
 * Example line consumer that rewrites RCPT commands. It would likely be better 
 * to do this in the mail object, but I'm struggling for a simple example of wrapping the 
 * SmtpCommandMap.
 * 
 * @author jhumphreys
 *
 */
public class RcptRewriteLineConsumer implements LineConsumer {

	private final LineConsumer delegate;
	
	public RcptRewriteLineConsumer(LineConsumer delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public Transition consume(SmtpSession session, Line line) {
		if (Rcpt.RCPT.equalsIgnoreCase(line.getVerb())) {
			line.setLine("RCPT TO:<joe@example.com>\r\n".getBytes(StandardCharsets.UTF_8));
		}
		return delegate.consume(session, line);
	}

}
