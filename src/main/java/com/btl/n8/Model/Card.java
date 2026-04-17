package com.btl.n8.Model;

public class Card extends Item{
    public Card(int id, String name, double startingPrice, int idSeller){
        super(id, name, startingPrice, idSeller);
    }

    public Card(){super();}
}
