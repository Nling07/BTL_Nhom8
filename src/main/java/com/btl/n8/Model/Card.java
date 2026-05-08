package com.btl.n8.Model;

import java.math.BigDecimal;

public class Card extends Item{
    public Card(int id, String name, int idSeller, byte[] image){
        super(id, name, idSeller, ItemType.CARD, image);
    }
    public Card(String name, int idSeller, byte[] image){
        super(name, idSeller, ItemType.CARD, image);
    }
    public Card(){
        super();
    }
}
