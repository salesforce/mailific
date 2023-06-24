# Overview

This library provides Spring autoconfiguration for the Mailific SMTP listener library . If you're
using Spring, you can add this library to your classpath, add a few Beans to customize the server
behavior, and then autowire in an SMTPServer.

See the `examples/springboot` directory for an example.

# Customizing

To use the SMPT Server, you will probably want to override some properties and Beans. At the very
least, you will need to provide your own MailObjectFactory Bean to do something with received
messages.

## Properties

The properties are all used by Spring configuration -- none of them are accessed directly by the
server library classes. That means that if you override the Beans that read the properties, you may
need to read them yourself and see that the values get injected into the underlying classes.

### Used by ServerConfig Bean

* mailific.server.listenHost (String)
  * Hostname or IP to bind to.
  * Default: `localhost`
* mailific.server.listenPort (int)
  * TCP port to listen on.
  * Default: 2525
* mailific.server.certPath (String)
  * File path to an SSL cert-chain file, in PEM format. If set to empty or null, the server won't offer STARTTLS.
  * Default: empty string
* mailific.server.certKeyPath (String)
  * File path to the key (in PKCS8 format) for the SSL cert specified by mailific.server.certPath
  * Default: empty string
* mailific.server.certPassword (String)
  * Password for the key specified by mailific.server.certKeyPath. Leave null if the key file is not encrypted.
  * Default: null

### Used by ExtensionProvider Bean

* mailific.server.certPath (String)
  * File path to an SSL cert-chain file, in PEM format. If set to empty or null, the server won't offer STARTTLS.
  * Default: empty string


## Beans

All of the Beans listed below are supplied as AutoConfiguration, with a `@ConditionalOnMissingBean`
annotation. This means that they will be created after any Beans that you define in your own
configuration, and only if you do not supply a Bean of the same type. So to override them, you need
merely supply your own Bean of the same type (or, as noted below, with the same name).

I've tried to list them in order of likelihood that you'll want to override them.

### MailObjectFactory

This is the main Bean that you need to override for your server to do anything useful. Write an
implementation of MailObject that does whatever you want to do with messages. Often you don't even
need to write a Factory class: you can implement the Bean like:

```java

    @Bean
    public MailObjectFactory mailObjectFactory() {
        return session -> new MyMailObject(session);
    }

```

### BannerDomainProvider

Bean wrapper for the domain String returned in the banner and ehlo responses. Defaults to the domain
of the host that the server is listening on.

### EhloGreetingProvider

Bean wrapper for the domain String returned in the banner and ehlo responses. Defaults to null.

### ExtensionProvider

Bean wrapper for a collection of extensions. This default implementation adds all of the "harmless
extensions" (ones that just work). If the certPath property is set, it also adds the STARTTLS
extension.

For an example of how to add more extensions, see Config#extensionProvider in the `examples/springboot` directory.

#### Properties

* mailific.server.certPath (String): if non-null, the STARTTLS extension is included

### CommandHandlerProvider

Bean wrapper for the collection of CommandHandlers to use. Depends on BannerDomainProvider,
EhloGreetingProvider, MailObjectFactory, and commandOverrides. The default implementation includes
all the mailific implementations of the standard SMTP commands, but allows you to override or extend
that set via the commandOverrides Bean.

### SmtpServer

This bean will supply a server, using config supplied by a ServerConfig Bean. There's probably no
real reason to override this one.

### ServerConfig

Puts together a ServerConfig based on application properties and a SmtpSessionFactory Bean.

#### Properties

* mailific.server.listenHost (String)
* mailific.server.listenPort (int)
* mailific.server.certPath (String)
* mailific.server.certKeyPath (String)
* mailific.server.certPassword (String)

### SmtpSessionFactory

Builds an SmtpSessionFactory from a LineConsumer Bean named `mailificCommandConsumer` and an
ExtensionProvider Bean. This bean uses the named LineConsumer Bean because LineConsumer is used in
enough places that you might reasonably have more than one in your Config.

### mailificCommandConsumer (LineConsumer)

Builds an SmtpCommandMap from a CommandHandlerProvider Bean. This is the default implementation for
the LineConsumer used to construct the SmtpSessionFactory.

### commandOverrides (Collection<CommandHandler>)

CommandHandlers to add to or replace those in the standard set. (Used by the CommandHandlerProvider
Bean.)
