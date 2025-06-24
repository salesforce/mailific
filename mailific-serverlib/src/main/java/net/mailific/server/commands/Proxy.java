package net.mailific.server.commands;

import net.mailific.server.session.*;

/**
 * Implements the PROXY protocol https://www.haproxy.org/download/1.8/doc/proxy-protocol.txt
 * used by HAProxy and nginx to pass client connection information.
 * nginx supports the PROXY protocol when it proxies a mail server, but to do things like
 * SPF, we need to know the original client IP address.
 *
 * This command collects the client IP address from the PROXY command and stores it in
 * the SESSION_CLIENTIP_PROPERTY property of the session.
 * (Perhaps at some point we should add the ability to change the remote address of the
 * SmtpSession.)
 */
public class Proxy extends BaseHandler {
    public static final String SESSION_CLIENTIP_PROPERTY = "proxied-client.ip";

    @Override
    protected Transition handleValidCommand(SmtpSession session, String commandLine) {
        var parts = commandLine.split(" ");
        // line is of the form (TCP6 is also supported):
        // PROXY TCP4 src_ip dst_ip src_port dst_port
        if (parts.length == 6) {
            var clientIp = parts[2];
            session.setProperty(SESSION_CLIENTIP_PROPERTY, clientIp);
        }
        return new Transition(Reply.DO_NOT_REPLY, SessionState.NO_STATE_CHANGE);
    }

    @Override
    protected boolean validForState(SessionState state) {
        return state == StandardStates.CONNECTED;
    }

    @Override
    public String verb() {
        return "PROXY";
    }
}
