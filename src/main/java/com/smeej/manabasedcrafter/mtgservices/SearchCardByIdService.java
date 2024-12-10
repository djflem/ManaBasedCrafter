package com.smeej.manabasedcrafter.mtgservices;

import io.magicthegathering.javasdk.api.CardAPI;
import io.magicthegathering.javasdk.resource.Card;

public class SearchCardByIdService {
    // sol ring multiverseId = 247533

    int multiverseId = 247533;
    Card card = CardAPI.getCard(multiverseId);

    public String formatCardDetails() {
        return SearchCardService.formatCardDetails(card);
    }
}
