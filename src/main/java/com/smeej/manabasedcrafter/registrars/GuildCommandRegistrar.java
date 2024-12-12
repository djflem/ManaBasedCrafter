package com.smeej.manabasedcrafter.registrars;

import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import discord4j.rest.RestClient;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The GuildCommandRegistrar is a Spring Component that implements ApplicationRunner to handle
 * the registration of Discord Guild Slash Commands on application startup. This class uses a
 * provided RestClient to communicate with Discord's API for bulk overwriting application commands.
 * It is designed to parse JSON command definitions from resource files and register them
 * automatically with the target guild.
 *
 * Responsibilities:
 * - Reads command definitions from JSON files located in the resources/commands directory.
 * - Constructs and registers these commands with the specified Discord guild.
 * - Utilizes Discord4J classes and tools, such as JacksonResources, for JSON parsing.
 *
 * Initialization:
 * - The GuildCommandRegistrar is instantiated by Spring with a pre-configured RestClient.
 * - The guildId property is injected from the application configuration.
 *
 * Behavior:
 * - The run method is triggered once during application startup and performs the following:
 *   1. Reads command data from JSON files.
 *   2. Converts the data into a list of ApplicationCommandRequest objects.
 *   3. Uses Discord's Bulk Overwrite API to register the commands with the configured guild.
 * - The bulk overwrite process is idempotent, ensuring safe re-registration even if only a
 *   single command has been added, changed, or removed.
 * - Logs the success or failure of the command registration process for debugging purposes.
 *
 * Additional Commands:
 * - This class can utilize methods to add/update/delete guild commands. Please refer to the
 *   Discord4J documentation.
 */
@Component
public class GuildCommandRegistrar implements ApplicationRunner {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final RestClient client;

    @Value("${guildId}")
    private long guildId;

    //Use the rest client provided by our Bean
    public GuildCommandRegistrar(RestClient client) {
        this.client = client;
    }

    //This method will run only once on each start up and is automatically called with Spring so blocking is okay.
    @Override
    public void run(ApplicationArguments args) throws IOException {
        //Create an ObjectMapper that supported Discord4J classes
        final JacksonResources d4jMapper = JacksonResources.create();

        // Convenience variables for the sake of easier to read code below.
        PathMatchingResourcePatternResolver matcher = new PathMatchingResourcePatternResolver();
        final ApplicationService applicationService = client.getApplicationService();
        final long applicationId = client.getApplicationId().block();

        //Get our commands json from resources as command data
        List<ApplicationCommandRequest> commands = new ArrayList<>();
        for (Resource resource : matcher.getResources("commands/*.json")) {
            ApplicationCommandRequest request = d4jMapper.getObjectMapper()
                    .readValue(resource.getInputStream(), ApplicationCommandRequest.class);

            commands.add(request);
        }

        /* Bulk overwrite commands. This is now idempotent, so it is safe to use this even when only 1 command
        is changed/added/removed
        */
        applicationService.bulkOverwriteGuildApplicationCommand(applicationId, guildId, commands)
                .doOnNext(ignore -> LOGGER.debug("Successfully registered Global Commands"))
                .doOnError(e -> LOGGER.error("Failed to register global commands", e))
                .subscribe();
    }
}
