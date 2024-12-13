A tool to help create manabases for MtG decks, EDH specifically. After inviting the bot to your discord guild, the member can use slash commands to search for cards or upload decks (with a txt/csv list of cards work in progress). You may then access a number of stastical graphs to assist with deck building and landbase refining. 

I intend to implement stats such as: probability of drawing a land, % of land to all cards, how many basic lands suggested for each color, and more...

Currently the cards are searched using Scryfall web API. The bot also uses Discord4j and Spring Boot dependencies for the command language and internal server (not yet hosted and requires your IDE to be running). The bot will be deployed after completion.
