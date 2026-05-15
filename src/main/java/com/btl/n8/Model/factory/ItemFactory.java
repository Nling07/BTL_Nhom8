package com.btl.n8.Model.factory;

import com.btl.n8.Model.entity.Card;
import com.btl.n8.Model.entity.Figure;
import com.btl.n8.Model.entity.Item;
import com.btl.n8.Model.entity.Poster;
import com.btl.n8.Model.enums.ItemType;

public class ItemFactory {
    private ItemFactory() {
    }

    public static Item createItem(
            ItemType type,
            int id,
            String name,
            int sellerId,
            byte[] image
    ) {

        switch (type) {

            case FIGURE:
                return new Figure(
                        id,
                        name,
                        sellerId,
                        image
                );

            case POSTER:
                return new Poster(
                        id,
                        name,
                        sellerId,
                        image
                );

            case CARD:
                return new Card(
                        id,
                        name,
                        sellerId,
                        image
                );

            default:
                throw new IllegalArgumentException(
                        "Invalid item type: " + type
                );
        }
    }

    public static Item createItem(
            ItemType type,
            String name,
            int sellerId,
            byte[] image
    ) {

        switch (type) {

            case FIGURE:
                return new Figure(
                        name,
                        sellerId,
                        image
                );

            case POSTER:
                return new Poster(
                        name,
                        sellerId,
                        image
                );

            case CARD:
                return new Card(
                        name,
                        sellerId,
                        image
                );

            default:
                throw new IllegalArgumentException(
                        "Invalid item type: " + type
                );
        }
    }
}
