package com.smeej.manabasedcrafter;

import com.smeej.manabasedcrafter.mtgservices.SearchCardByIdService;

public class TestMain {

    public static void main(String[] args) {
        SearchCardByIdService searchCardByIdService = new SearchCardByIdService();

        System.out.println(searchCardByIdService.formatCardDetails());
    }
}
