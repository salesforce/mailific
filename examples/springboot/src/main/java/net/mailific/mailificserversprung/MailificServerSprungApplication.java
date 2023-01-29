package net.mailific.mailificserversprung;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import net.mailific.server.SmtpServer;

@SpringBootApplication
public class MailificServerSprungApplication implements ApplicationRunner {

	@Autowired
	private SmtpServer smtpServer;
	
	public static void main(String[] args) {
		SpringApplication.run(MailificServerSprungApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		smtpServer.start();
	}
}
