package com.smeej.manabasedcrafter.scryfallservices;

import java.util.Map;

public class ScryfallCardResponse {
    private Map<String, String> image_uris;

    public Map<String, String> getImageUris() {
        return image_uris;
    }

    public void setImageUris(Map<String, String> image_uris) {
        this.image_uris = image_uris;
    }
}
