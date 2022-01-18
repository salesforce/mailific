Mailific is a library for building SMTP listeners, with an implementation built on Netty. It is new,
and hasn't seen much battle testing, but it is usable. In addition to the basic SMTP dialog, it
supports important extensions including STARTTLS, SMTPUTF8, and AUTH.

As of now, it does little checking for standards compliance. In future versions (time allowing),
further compliance checks will be added and will be on by default. So if you depend on lenient
behavior in the current version, you should read the change log and be prepared to to update your
code to specify the lenient behavior.

Goals

* Interoperate with RFC compliant senders.

* Clean separation of SMTP logic from networking implementation.

* Minimal dependencies (other than Netty). No dependency on JavaMail.

Future goals:

* Default behavior should be to not tolerate non-RFC compliant behavior.
* Easy ways to override for more tolerance.
* Add libraries for authentication checking: SPF, DKIM, DMARC.
* Add sending support (as a separate library) for relaying.
* SAX-like message-parsing library (e.g., receive headers and body parts as events)

Known issues:
* No soft shutdown

# HOW TO

This section will walk through some progressively more complicated tasks.

## Start a Server

You can start up a simple demo server by just running `java -jar <mailific jar>`. By default it will
listen on port 2525. Pass `-h` to see some options. This server just accepts any message you send in
and discards it.

To start up a listener in your own app, you need to construct a NettySmtpServer (the included
concrete implementation of SmtpServer) and call start() on it. Let's walk through what you need
for that.

First, you need a MailObjectFactory. We'll talk through MailObjects in detail below, under
[Processing Messages](#processing-messages). To start with, you can just construct a
BaseMailObjectFactory, which creates MailObjects that simply discard all message data.

Next, you need a ServerConfig. You can start from scratch using the ServerConfig.builder(), but for
now, call Main.defaultServerConfigBuilder(MailObjectFactory), which populates all the builder fields
with defaults that you can override.

Finally, create and start a server with `new NettySmtpServer(serverConfig).start()`.

You can shut id down with `SmtpServer.shutdown()`. Be warned that shutdown is not graceful: it kills
any sessions in flight.

## <a name="processing-messages">Processing Messages</a>

A server that just discards messages is not very useful (except maybe in a test environment). To
actually do something with incoming mail, you need to implement `MailObject`. The simplest way is to
extend InMemoryMailObject and do your processing in processFinished(byte[]).

Once you've got your MailObject class, implement a MailObjectFactory that returns it. You can pass
that into Main.defaultServerConfigBuilder() when building your server.


## Handling SMTP Commands

If you want to modify the handling of an SMTP command, you can either create your own implementation
of CommandHandler, extend BaseHandler, or extend the standard handler for the verb you want to
modify. Let's say for example, that you want your server's response to EHLO to include a random
joke. You could create a class called JokeEhlo and extend Ehlo. The only method you would need
to override is getGreeting(), which would need to return your random joke.

OK, now you've got JokeEhlo, what do you do with it? You want to include it in the collection of
CommandHandlers in the ServerConfig. An easy way to override just one or two of the standard
CommandHandlers is to start with Main.baseCommandHandlers(). It returns a Map with all the default
command implementations, keyed by verb. To replace the standard Ehlo command with an
instance of yours, call myCommandMap.put(jokeEhlo.verb(), jokeEhlo). Then, call
serverConfigBuilder.withCommandHandlers(myCommandMap).

Notice that you're passing a single instance of your JokeEhlo into the ServerConfig -- not, say, a
JokeEhloFactory. CommandHandlers have to be stateless, reentrant Singletons, because the same
instance is going to be used concurrently for all incoming SMTP sessions.

The library comes with implementations for most of the standard SMTP commands. I haven't gotten
around to VRFY and EXPN yet.

## TLS

You can turn on StartTLS (as describe in RFC 2487) by supplying the ServerConfig with a TLS
cert-chain file (in PEM format) and the key for that cert in pkcs8 format. If the key is encrypted,
you must also supply the password. As of now, those are the only formats provided.

## Supporting SMTP Extensions

The library includes support for several SMTP extensions, and it's fairly easy to add your own. RFC
1425 doesn't really limit what effects an SMTP extension can have, and some of them alter server
behavior drastically. So it's impossible to write an extension framework that anticipates any
conceivable extension. Hopefully the Extension interface will get you pretty far.

The method Main.harmlessExtensions() returns a collection of extensions that you might as well
include, since they "just work" and improve the server's capabilities. Look in the
net.mailific.server.extension package to see what else is available, and to see some code examples.

In case you're poking around in there, be aware that StartTls is the one supplied extension that
isn't implemented purely as an extension: there's code in the server implementation that interacts
with it.

## Securing the server

### Allow/block by IP

Extend Connect, and override the method shouldAllow(SmtpSession). You can get the remote IP from
SmtpSession.getRemoteAddress().

### Allow/block by Sender or Recipient

There are two ways to go about this. If you are extending BaseMailObject, you probably want to
override mailFrom() and offerRecipient() to return a non-2xx reply code for blocked
senders/recipients. You could also create your own Mail and/or Rcpt CommandHandlers.

### Auth

The library supports SMTP Auth (as detailed in RFC 4954) using either the PLAIN or LOGIN SASL
mechanism. To turn this on, you must

1. implement the AuthCheck interface,
2. use your Authcheck to construct a PlainMechanism and/or LoginMechanism,
3. use your mechanisms to construct an Auth extension, and
4. include the Auth extension in the ServerConfig.

There's an example of all this in the Main.getTestAuthExtension method.

# Logging

Logging is pretty basic. Rather than adding a dependency on one of the million different "last
logging libraries you'll ever need", all logging goes through the SmtpEventLogger interface. The
default implementation for that uses java.util.logger logging, but you can suit yourself.

# Testing

You may note that there are lots of unit tests but no integration tests. I'm using a separate
project to do integration tests. I hope to release that soon, as well.
