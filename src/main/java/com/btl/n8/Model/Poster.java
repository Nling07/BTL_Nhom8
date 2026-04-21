package com.btl.n8.Model;

import java.math.BigDecimal;

public class Poster extends Item{
    public Poster(int id, String name, int idSeller){
        super(id, name, idSeller, ItemType.POSTER);
    }
    public Poster(){
        super();
    }
}
