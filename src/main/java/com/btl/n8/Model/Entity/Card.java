package com.btl.n8.Model.Entity;

import com.btl.n8.Model.Enums.ItemType;

public class Card extends Item {
    public Card(int id, String name, int idSeller, byte[] image){
        super(id, name, idSeller, ItemType.CARD, image);
    }
    public Card(String name, int idSeller, byte[] image){
        super(name, idSeller, ItemType.CARD, image);
    }
    public Card(){
        super();
    }

    @Override
    public String getDescription() {
        return "Rare trading card";
    }
}
