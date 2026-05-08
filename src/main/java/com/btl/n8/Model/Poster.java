package com.btl.n8.Model;

import java.math.BigDecimal;

public class Poster extends Item{
    public Poster(int id, String name, int idSeller, byte[] image){
        super(id, name, idSeller, ItemType.POSTER, image);
    }
    public Poster(String name, int idSeller, byte[] image){
        super(name, idSeller, ItemType.POSTER, image);
    }
    public Poster(){
        super();
    }
}
