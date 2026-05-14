package com.btl.n8.Model.entity;

import com.btl.n8.Model.enums.ItemType;

public class Poster extends Item {
    public Poster(int id, String name, int idSeller, byte[] image){
        super(id, name, idSeller, ItemType.POSTER, image);
    }
    public Poster(String name, int idSeller, byte[] image){
        super(name, idSeller, ItemType.POSTER, image);
    }
    public Poster(){
        super();
    }

    @Override
    public String getDescription() {
        return "Limited edition poster";
    }
}
