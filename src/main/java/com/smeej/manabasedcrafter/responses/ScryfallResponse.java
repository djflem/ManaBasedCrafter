package com.smeej.manabasedcrafter.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Represents the response from a card search operation. This class encapsulates
 * the details of a card retrieved from a data source, including its object type,
 * unique identifier, name, and image URIs.
 * <p>
 * The class handles JSON deserialization using Jackson annotations, allowing it
 * to map JSON fields to corresponding class fields. Unknown fields in the JSON
 * response are ignored to prevent deserialization errors.
 * <p>
 * Primary fields include:
 * - object: The type of object (e.g., "card").
 * - id: The unique identifier for the card.
 * - name: The name of the card.
 * - imageUris: A map containing image URIs for the card, categorized by quality or size keys (e.g., "normal").
 * - manaCost: A mana value of a card. For making color count charts.
 * <p>
 * This class is commonly used in external API integrations, such as interacting
 * with Magic: The Gathering card databases, to parse and provide access to card information.
 * <p>
 * The DTO can be expanded in the future if more fields are needed.
 */
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unrecognized fields
public class ScryfallResponse {

    @JsonProperty("object")
    private String object;

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("mana_cost")
    private String manaCost;

    @JsonProperty("image_uris")
    private Map<String, String> imageUris;

    // Getters and Setters
    public String getObject() {
        return object;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getManaCost() {
        return manaCost;
    }

    public Map<String, String> getImageUris() {
        return imageUris;
    }
}
