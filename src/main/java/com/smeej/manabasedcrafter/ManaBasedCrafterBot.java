package com.smeej.manabasedcrafter;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.rest.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

/**
 * The ManaBasedCrafterBot class serves as the main entry point and configuration
 * for the bot application. It initializes the application context, configures
 * the Discord bot client, and ensures the application remains running.
 * <p>
 * This class uses Spring Boot's {@code @SpringBootApplication} annotation to
 * define the configuration and bootstrap the application.
 */
@SpringBootApplication
public class ManaBasedCrafterBot {

	@Value("${token}")
	private String token;

	//Start spring application
	public static void main(String[] args) throws InterruptedException {
		new SpringApplicationBuilder(ManaBasedCrafterBot.class)
				.build()
				.run(args);

		Thread.currentThread().join(); // Prevents bot from stopping
	}

	@Bean
	public GatewayDiscordClient gatewayDiscordClient() {
		return DiscordClientBuilder.create(token).build()
				.gateway()
				.setInitialPresence(ignore -> ClientPresence.online(ClientActivity.listening("to /commands")))
				.login()
				.block();
	}

	@Bean
	public RestClient discordRestClient(GatewayDiscordClient client) {
		return client.getRestClient();
	}
}
