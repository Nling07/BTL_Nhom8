package com.btl.n8.Model;

import java.math.BigDecimal;

public class Card extends Item{
    public Card(int id, String name, int idSeller){
        super(id, name, idSeller, ItemType.CARD);
    }

    public Card(String name, int idSeller){
        super(name, idSeller, ItemType.CARD);
    }

    public Card(){super();}
}
